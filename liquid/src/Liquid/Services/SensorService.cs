using System;
using System.Collections.Generic;
using System.Linq;
using System.Management;
using System.Threading;
using LibreHardwareMonitor.Hardware;
using Microsoft.Win32;

namespace Liquid.Services
{
    public sealed class SensorReading
    {
        public string Id;
        public string Name;
        public SensorType Type;
        public float? Value, Min, Max;
    }

    public sealed class HardwareGroup
    {
        public string Id;
        public string Name;
        public HardwareType Type;
        public List<SensorReading> Sensors = new List<SensorReading>();
    }

    public sealed class SensorSnapshot
    {
        public List<HardwareGroup> Groups = new List<HardwareGroup>();
        public float? CpuTemp, CpuLoad, CpuPower, CpuClock;
        public float? GpuTemp, GpuHotSpot, GpuLoad, GpuPower, GpuClock, VramUsedMb, VramTotalMb;
        public float? RamLoad, RamUsedGb, RamTotalGb;
        public string CpuName = "Procesor", GpuName = "Karta graficzna";
        public string CpuTempSource = "";
    }

    /// <summary>Odczyt czujników (LibreHardwareMonitor) w tle co 1 s.</summary>
    public sealed class SensorService : IDisposable
    {
        private Computer _pc;
        private Thread _thread;
        private volatile bool _run;
        private volatile bool _restart;
        private DateTime _lastAcpi = DateTime.MinValue;
        private float? _acpiTemp;

        public volatile SensorSnapshot Latest = new SensorSnapshot();
        public volatile bool Ready;
        public string Error;
        public event Action Updated;

        public static bool PawnIoInstalled
        {
            get
            {
                try
                {
                    using var k = Registry.LocalMachine.OpenSubKey(@"SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\PawnIO");
                    return k != null;
                }
                catch { return false; }
            }
        }

        public void Start()
        {
            if (_run) return;
            _run = true;
            _thread = new Thread(Loop) { IsBackground = true, Name = "Liquid.Sensors", Priority = ThreadPriority.BelowNormal };
            _thread.Start();
        }

        public void Restart() => _restart = true;

        private void OpenComputer()
        {
            try { _pc?.Close(); } catch { }
            _pc = new Computer
            {
                IsCpuEnabled = true,
                IsGpuEnabled = true,
                IsMemoryEnabled = true,
                IsMotherboardEnabled = true,
                IsStorageEnabled = true,
                IsControllerEnabled = false,
                IsNetworkEnabled = false
            };
            _pc.Open();
        }

        private void Loop()
        {
            try { OpenComputer(); Ready = true; }
            catch (Exception ex) { Error = ex.Message; }

            while (_run)
            {
                try
                {
                    if (_restart)
                    {
                        _restart = false;
                        OpenComputer();
                    }
                    if (_pc != null)
                    {
                        Latest = Read();
                        if (!Ready) { Ready = true; Error = null; }
                        Updated?.Invoke();
                    }
                }
                catch (Exception ex) { Error = ex.Message; }
                Thread.Sleep(1000);
            }
        }

        private static readonly SensorType[] Shown =
        {
            SensorType.Temperature, SensorType.Load, SensorType.Clock, SensorType.Fan, SensorType.Power
        };

        private SensorSnapshot Read()
        {
            var snap = new SensorSnapshot();
            IHardware cpu = null, gpu = null, mem = null;

            foreach (var hw in _pc.Hardware)
            {
                try
                {
                    hw.Update();
                    foreach (var sub in hw.SubHardware) sub.Update();
                }
                catch { }

                var g = new HardwareGroup { Id = hw.Identifier.ToString(), Name = hw.Name, Type = hw.HardwareType };
                AddSensors(g, hw.Sensors);
                foreach (var sub in hw.SubHardware) AddSensors(g, sub.Sensors);
                if (g.Sensors.Count > 0) snap.Groups.Add(g);

                switch (hw.HardwareType)
                {
                    case HardwareType.Cpu: if (cpu == null) cpu = hw; break;
                    case HardwareType.GpuNvidia:
                    case HardwareType.GpuAmd:
                        if (gpu == null || gpu.HardwareType == HardwareType.GpuIntel) gpu = hw; break;
                    case HardwareType.GpuIntel: if (gpu == null) gpu = hw; break;
                    case HardwareType.Memory: if (mem == null && hw.Sensors.Any(x => x.SensorType == SensorType.Load && x.Name == "Memory")) mem = hw; break;
                }
            }

            if (cpu != null)
            {
                snap.CpuName = cpu.Name;
                var temps = cpu.Sensors.Where(s => s.SensorType == SensorType.Temperature && s.Value.HasValue && s.Value > 0).ToList();
                string[] pref = { "CPU Package", "Core (Tctl/Tdie)", "Core (Tctl)", "Tctl/Tdie", "Core (Tdie)", "Core Average", "Core Max" };
                foreach (var p in pref)
                {
                    var s = temps.FirstOrDefault(x => x.Name == p);
                    if (s != null) { snap.CpuTemp = s.Value; break; }
                }
                if (snap.CpuTemp == null && temps.Count > 0) snap.CpuTemp = temps.Max(x => x.Value);
                if (snap.CpuTemp != null) snap.CpuTempSource = "sensor";
                snap.CpuLoad = Find(cpu, SensorType.Load, "CPU Total");
                snap.CpuPower = Find(cpu, SensorType.Power, "CPU Package") ?? Find(cpu, SensorType.Power, "Package");
                var clocks = cpu.Sensors.Where(s => s.SensorType == SensorType.Clock && s.Name.StartsWith("Core") && s.Value.HasValue && s.Value > 0).ToList();
                if (clocks.Count > 0) snap.CpuClock = clocks.Average(s => s.Value.Value);
            }

            if (snap.CpuTemp == null)
            {
                // Zapasowy odczyt z ACPI (mniej dokładny)
                if ((DateTime.Now - _lastAcpi).TotalSeconds > 5)
                {
                    _lastAcpi = DateTime.Now;
                    _acpiTemp = ReadAcpi();
                }
                if (_acpiTemp != null) { snap.CpuTemp = _acpiTemp; snap.CpuTempSource = "acpi"; }
            }

            if (gpu != null)
            {
                snap.GpuName = gpu.Name;
                snap.GpuTemp = Find(gpu, SensorType.Temperature, "GPU Core") ?? First(gpu, SensorType.Temperature);
                snap.GpuHotSpot = Find(gpu, SensorType.Temperature, "GPU Hot Spot");
                snap.GpuLoad = Find(gpu, SensorType.Load, "GPU Core") ?? Find(gpu, SensorType.Load, "D3D 3D") ?? First(gpu, SensorType.Load);
                snap.GpuPower = Find(gpu, SensorType.Power, "GPU Package") ?? First(gpu, SensorType.Power);
                snap.GpuClock = Find(gpu, SensorType.Clock, "GPU Core");
                snap.VramUsedMb = Find(gpu, SensorType.SmallData, "GPU Memory Used") ?? Find(gpu, SensorType.SmallData, "D3D Dedicated Memory Used");
                snap.VramTotalMb = Find(gpu, SensorType.SmallData, "GPU Memory Total") ?? Find(gpu, SensorType.SmallData, "D3D Dedicated Memory Total");
            }

            if (mem != null)
            {
                snap.RamLoad = Find(mem, SensorType.Load, "Memory");
                snap.RamUsedGb = Find(mem, SensorType.Data, "Memory Used");
                var avail = Find(mem, SensorType.Data, "Memory Available");
                if (snap.RamUsedGb != null && avail != null) snap.RamTotalGb = snap.RamUsedGb + avail;
            }
            return snap;
        }

        private static void AddSensors(HardwareGroup g, IEnumerable<ISensor> sensors)
        {
            foreach (var s in sensors)
            {
                if (!Shown.Contains(s.SensorType) || !s.Value.HasValue) continue;
                if (s.SensorType == SensorType.Temperature && (s.Value <= 0 || s.Value > 150)) continue;
                g.Sensors.Add(new SensorReading { Id = s.Identifier.ToString(), Name = s.Name, Type = s.SensorType, Value = s.Value, Min = s.Min, Max = s.Max });
            }
        }

        private static float? Find(IHardware hw, SensorType type, string name)
        {
            foreach (var s in hw.Sensors)
                if (s.SensorType == type && s.Name == name && s.Value.HasValue) return s.Value;
            foreach (var sub in hw.SubHardware)
                foreach (var s in sub.Sensors)
                    if (s.SensorType == type && s.Name == name && s.Value.HasValue) return s.Value;
            return null;
        }

        private static float? First(IHardware hw, SensorType type)
        {
            foreach (var s in hw.Sensors)
                if (s.SensorType == type && s.Value.HasValue && s.Value > 0) return s.Value;
            return null;
        }

        private static float? ReadAcpi()
        {
            try
            {
                using var searcher = new ManagementObjectSearcher(@"root\WMI", "SELECT CurrentTemperature FROM MSAcpi_ThermalZoneTemperature");
                float best = 0;
                foreach (ManagementBaseObject o in searcher.Get())
                {
                    float c = Convert.ToSingle(o["CurrentTemperature"]) / 10f - 273.15f;
                    if (c > best && c < 120) best = c;
                }
                return best > 5 ? best : (float?)null;
            }
            catch { return null; }
        }

        public void Dispose()
        {
            _run = false;
            try { _thread?.Join(1500); } catch { }
            try { _pc?.Close(); } catch { }
        }
    }
}
