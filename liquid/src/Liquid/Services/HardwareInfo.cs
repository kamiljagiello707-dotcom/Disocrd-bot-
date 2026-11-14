using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Management;
using System.Net.NetworkInformation;
using Microsoft.Win32;

namespace Liquid.Services
{
    public sealed class SpecSection
    {
        public string Key;          // cpu, gpu, ram, board, disk, os, display, net
        public string Title;
        public string Headline;
        public List<KeyValuePair<string, string>> Rows = new List<KeyValuePair<string, string>>();
        public void Add(string k, string v) { if (!string.IsNullOrWhiteSpace(v)) Rows.Add(new KeyValuePair<string, string>(k, v.Trim())); }
    }

    public sealed class GpuInfo { public string Name; public double VramGb; public bool Integrated; }
    public sealed class DiskInfo { public string Name; public string Type; public string Bus; public double SizeGb; public bool IsSystem; }

    public sealed class SystemSpecs
    {
        public List<SpecSection> Sections = new List<SpecSection>();
        public string CpuName = "";
        public int Cores, Threads;
        public double CpuMaxGhz;
        public List<GpuInfo> Gpus = new List<GpuInfo>();
        public double RamGb;
        public int RamModules, RamSpeed;
        public string RamType = "";
        public List<DiskInfo> Disks = new List<DiskInfo>();
        public string OsName = "";
        public int RefreshRate;
        public string Resolution = "";

        public string ToText()
        {
            var sb = new System.Text.StringBuilder();
            sb.AppendLine("=== LIQUID — specyfikacja komputera ===");
            foreach (var s in Sections)
            {
                sb.AppendLine();
                sb.AppendLine($"[{s.Title}] {s.Headline}");
                foreach (var r in s.Rows) sb.AppendLine($"  {r.Key}: {r.Value}");
            }
            return sb.ToString();
        }
    }

    public static class HardwareInfo
    {
        private static List<ManagementBaseObject> Q(string query, string scope = @"root\CIMV2")
        {
            var list = new List<ManagementBaseObject>();
            try
            {
                using var s = new ManagementObjectSearcher(scope, query);
                foreach (ManagementBaseObject o in s.Get()) list.Add(o);
            }
            catch { }
            return list;
        }

        private static string S(ManagementBaseObject o, string prop)
        {
            try { return o[prop]?.ToString()?.Trim() ?? ""; } catch { return ""; }
        }

        private static long L(ManagementBaseObject o, string prop)
        {
            try { var v = o[prop]; return v == null ? 0 : Convert.ToInt64(v); } catch { return 0; }
        }

        private static string Gb(double gb) => gb >= 1000 ? $"{gb / 1024:0.##} TB" : $"{gb:0.#} GB";

        public static SystemSpecs Collect()
        {
            var sp = new SystemSpecs();

            // ---------- CPU ----------
            var cpu = new SpecSection { Key = "cpu", Title = "Procesor" };
            var cpus = Q("SELECT * FROM Win32_Processor");
            if (cpus.Count > 0)
            {
                var c = cpus[0];
                sp.CpuName = S(c, "Name");
                sp.Cores = cpus.Sum(x => (int)L(x, "NumberOfCores"));
                sp.Threads = cpus.Sum(x => (int)L(x, "NumberOfLogicalProcessors"));
                sp.CpuMaxGhz = L(c, "MaxClockSpeed") / 1000.0;
                cpu.Headline = sp.CpuName;
                cpu.Add("Producent", S(c, "Manufacturer").Replace("GenuineIntel", "Intel").Replace("AuthenticAMD", "AMD"));
                cpu.Add("Rdzenie / wątki", $"{sp.Cores} / {sp.Threads}");
                cpu.Add("Taktowanie bazowe", $"{sp.CpuMaxGhz:0.00} GHz");
                cpu.Add("Aktualne taktowanie", $"{L(c, "CurrentClockSpeed") / 1000.0:0.00} GHz");
                long l2 = L(c, "L2CacheSize"), l3 = L(c, "L3CacheSize");
                if (l2 > 0) cpu.Add("Cache L2", l2 >= 1024 ? $"{l2 / 1024.0:0.#} MB" : $"{l2} KB");
                if (l3 > 0) cpu.Add("Cache L3", l3 >= 1024 ? $"{l3 / 1024.0:0.#} MB" : $"{l3} KB");
                cpu.Add("Gniazdo", S(c, "SocketDesignation"));
                cpu.Add("Architektura", L(c, "AddressWidth") == 64 ? "x64" : S(c, "AddressWidth"));
                cpu.Add("Wirtualizacja", L(c, "VirtualizationFirmwareEnabled") != 0 || S(c, "VirtualizationFirmwareEnabled") == "True" ? "Włączona" : "Wyłączona / brak danych");
            }
            sp.Sections.Add(cpu);

            // ---------- GPU ----------
            var gpu = new SpecSection { Key = "gpu", Title = "Karta graficzna" };
            var vramMap = ReadVramFromRegistry();
            int gi = 0;
            foreach (var g in Q("SELECT * FROM Win32_VideoController"))
            {
                string name = S(g, "Name");
                if (string.IsNullOrEmpty(name) || name.Contains("Basic Display") || name.Contains("Remote")) continue;
                double vram = vramMap.TryGetValue(name, out var v) ? v : L(g, "AdapterRAM") / 1073741824.0;
                bool integrated = IsIntegrated(name);
                sp.Gpus.Add(new GpuInfo { Name = name, VramGb = vram, Integrated = integrated });
                string p = gi == 0 ? "" : $" #{gi + 1}";
                if (gi == 0) gpu.Headline = name;
                else gpu.Add("Karta" + p, name);
                gpu.Add("Pamięć VRAM" + p, vram > 0 ? Gb(vram) : "brak danych");
                gpu.Add("Typ" + p, integrated ? "Zintegrowana" : "Dedykowana");
                gpu.Add("Sterownik" + p, S(g, "DriverVersion"));
                try
                {
                    var d = S(g, "DriverDate");
                    if (d.Length >= 8) gpu.Add("Data sterownika" + p, ManagementDateTimeConverter.ToDateTime(d).ToString("dd.MM.yyyy"));
                }
                catch { }
                long w = L(g, "CurrentHorizontalResolution"), h = L(g, "CurrentVerticalResolution"), hz = L(g, "CurrentRefreshRate");
                if (w > 0 && h > 0)
                {
                    gpu.Add("Rozdzielczość" + p, $"{w} × {h} @ {hz} Hz");
                    if (string.IsNullOrEmpty(sp.Resolution)) { sp.Resolution = $"{w} × {h}"; sp.RefreshRate = (int)hz; }
                }
                gi++;
            }
            // najmocniejsza karta jako pierwsza do oceny
            sp.Gpus = sp.Gpus.OrderBy(x => x.Integrated).ThenByDescending(x => x.VramGb).ToList();
            if (sp.Gpus.Count > 0 && gpu.Headline != sp.Gpus[0].Name && !string.IsNullOrEmpty(gpu.Headline))
                gpu.Headline = sp.Gpus[0].Name + "  (+ " + (sp.Gpus.Count - 1) + ")";
            sp.Sections.Add(gpu);

            // ---------- RAM ----------
            var ram = new SpecSection { Key = "ram", Title = "Pamięć RAM" };
            var mods = Q("SELECT * FROM Win32_PhysicalMemory");
            double total = mods.Sum(m => L(m, "Capacity")) / 1073741824.0;
            if (total <= 0)
            {
                var cs = Q("SELECT TotalPhysicalMemory FROM Win32_ComputerSystem");
                if (cs.Count > 0) total = L(cs[0], "TotalPhysicalMemory") / 1073741824.0;
            }
            sp.RamGb = Math.Round(total);
            sp.RamModules = mods.Count;
            if (mods.Count > 0)
            {
                sp.RamSpeed = (int)mods.Max(m => Math.Max(L(m, "ConfiguredClockSpeed"), L(m, "Speed")));
                long t = L(mods[0], "SMBIOSMemoryType");
                sp.RamType = t switch { 20 => "DDR", 21 => "DDR2", 24 => "DDR3", 26 => "DDR4", 34 => "DDR5", 35 => "LPDDR5", 30 => "LPDDR4", _ => "" };
            }
            ram.Headline = $"{sp.RamGb:0} GB {sp.RamType}".Trim() + (sp.RamSpeed > 0 ? $" @ {sp.RamSpeed} MT/s" : "");
            ram.Add("Pojemność", $"{sp.RamGb:0} GB");
            ram.Add("Typ", sp.RamType);
            if (sp.RamSpeed > 0) ram.Add("Prędkość", $"{sp.RamSpeed} MT/s");
            var arr = Q("SELECT MemoryDevices FROM Win32_PhysicalMemoryArray");
            long slots = arr.Sum(a => L(a, "MemoryDevices"));
            ram.Add("Moduły / sloty", slots > 0 ? $"{mods.Count} / {slots}" : $"{mods.Count}");
            ram.Add("Tryb pracy", mods.Count >= 2 ? "Dual Channel (prawdopodobnie)" : mods.Count == 1 ? "Single Channel" : "");
            int mi = 1;
            foreach (var m in mods)
            {
                string part = S(m, "PartNumber");
                string man = S(m, "Manufacturer");
                ram.Add($"Kość {mi++} ({S(m, "DeviceLocator")})", $"{L(m, "Capacity") / 1073741824.0:0} GB {man} {part}".Trim());
            }
            sp.Sections.Add(ram);

            // ---------- Płyta główna ----------
            var board = new SpecSection { Key = "board", Title = "Płyta główna" };
            var bb = Q("SELECT * FROM Win32_BaseBoard");
            if (bb.Count > 0)
            {
                board.Headline = $"{S(bb[0], "Manufacturer")} {S(bb[0], "Product")}".Trim();
                board.Add("Producent", S(bb[0], "Manufacturer"));
                board.Add("Model", S(bb[0], "Product"));
                board.Add("Wersja", S(bb[0], "Version"));
            }
            var bios = Q("SELECT * FROM Win32_BIOS");
            if (bios.Count > 0)
            {
                board.Add("BIOS", $"{S(bios[0], "Manufacturer")} {S(bios[0], "SMBIOSBIOSVersion")}".Trim());
                try
                {
                    var d = S(bios[0], "ReleaseDate");
                    if (d.Length >= 8) board.Add("Data BIOS", ManagementDateTimeConverter.ToDateTime(d).ToString("dd.MM.yyyy"));
                }
                catch { }
            }
            var csys = Q("SELECT Manufacturer, Model FROM Win32_ComputerSystem");
            if (csys.Count > 0) board.Add("Komputer", $"{S(csys[0], "Manufacturer")} {S(csys[0], "Model")}".Trim());
            sp.Sections.Add(board);

            // ---------- Dyski ----------
            var disk = new SpecSection { Key = "disk", Title = "Dyski" };
            string sysLetter = Path.GetPathRoot(Environment.SystemDirectory)?.Substring(0, 1) ?? "C";
            var sysDiskNumbers = new HashSet<string>();
            foreach (var p in Q("SELECT DiskNumber, DriveLetter FROM MSFT_Partition", @"root\Microsoft\Windows\Storage"))
            {
                string dl = S(p, "DriveLetter");
                if (dl.Length > 0 && string.Equals(dl.Substring(0, 1), sysLetter, StringComparison.OrdinalIgnoreCase))
                    sysDiskNumbers.Add(S(p, "DiskNumber"));
            }
            foreach (var d in Q("SELECT * FROM MSFT_PhysicalDisk", @"root\Microsoft\Windows\Storage"))
            {
                long media = L(d, "MediaType"), bus = L(d, "BusType");
                string type = media switch { 3 => "HDD", 4 => "SSD", 5 => "SCM", _ => "" };
                string busName = bus switch { 17 => "NVMe", 11 => "SATA", 7 => "USB", 8 => "RAID", 10 => "SAS", 1 => "SCSI", 3 => "ATA", _ => "" };
                if (bus == 17 && type == "") type = "SSD";
                var info = new DiskInfo
                {
                    Name = S(d, "FriendlyName"), Type = type, Bus = busName,
                    SizeGb = L(d, "Size") / 1073741824.0,
                    IsSystem = sysDiskNumbers.Contains(S(d, "DeviceId"))
                };
                sp.Disks.Add(info);
                string label = $"{info.Type} {info.Bus}".Trim();
                disk.Add(info.Name + (info.IsSystem ? "  ★ systemowy" : ""), $"{Gb(info.SizeGb)}" + (label.Length > 0 ? $" · {label}" : ""));
            }
            if (sp.Disks.Count == 0)
            {
                foreach (var d in Q("SELECT Model, Size, MediaType FROM Win32_DiskDrive"))
                {
                    var info = new DiskInfo { Name = S(d, "Model"), SizeGb = L(d, "Size") / 1073741824.0, Type = S(d, "Model").ToUpper().Contains("SSD") ? "SSD" : "" };
                    sp.Disks.Add(info);
                    disk.Add(info.Name, Gb(info.SizeGb));
                }
            }
            foreach (var di in DriveInfo.GetDrives())
            {
                try
                {
                    if (!di.IsReady || di.DriveType != DriveType.Fixed) continue;
                    double tot = di.TotalSize / 1073741824.0, free = di.AvailableFreeSpace / 1073741824.0;
                    disk.Add($"Partycja {di.Name.TrimEnd('\\')} {di.VolumeLabel}".Trim(), $"wolne {Gb(free)} z {Gb(tot)} ({(tot > 0 ? free / tot * 100 : 0):0}%)");
                }
                catch { }
            }
            double sumDisk = sp.Disks.Sum(x => x.SizeGb);
            disk.Headline = $"{sp.Disks.Count} dysk(i) · łącznie {Gb(sumDisk)}";
            sp.Sections.Add(disk);

            // ---------- System ----------
            var os = new SpecSection { Key = "os", Title = "System" };
            var o = Q("SELECT * FROM Win32_OperatingSystem");
            if (o.Count > 0)
            {
                sp.OsName = S(o[0], "Caption").Replace("Microsoft ", "");
                os.Headline = sp.OsName;
                string display = "";
                try
                {
                    using var k = Registry.LocalMachine.OpenSubKey(@"SOFTWARE\Microsoft\Windows NT\CurrentVersion");
                    display = k?.GetValue("DisplayVersion")?.ToString() ?? "";
                }
                catch { }
                os.Add("Wersja", $"{display} (kompilacja {S(o[0], "BuildNumber")})".Trim());
                os.Add("Architektura", S(o[0], "OSArchitecture"));
                try { os.Add("Data instalacji", ManagementDateTimeConverter.ToDateTime(S(o[0], "InstallDate")).ToString("dd.MM.yyyy")); } catch { }
                try
                {
                    var up = DateTime.Now - ManagementDateTimeConverter.ToDateTime(S(o[0], "LastBootUpTime"));
                    os.Add("Czas pracy", $"{(int)up.TotalDays} d {up.Hours} h {up.Minutes} min");
                }
                catch { }
            }
            os.Add("Nazwa komputera", Environment.MachineName);
            os.Add("Użytkownik", Environment.UserName);
            sp.Sections.Add(os);

            // ---------- Ekran ----------
            var disp = new SpecSection { Key = "display", Title = "Monitor" };
            var mons = Q("SELECT * FROM WmiMonitorID", @"root\WMI");
            int mn = 1;
            foreach (var m in mons)
            {
                string name = DecodeWmiString(m, "UserFriendlyName");
                string man = DecodeWmiString(m, "ManufacturerName");
                disp.Add($"Monitor {mn++}", $"{man} {name}".Trim());
            }
            disp.Headline = string.IsNullOrEmpty(sp.Resolution) ? $"{mons.Count} monitor(y)" : $"{sp.Resolution} @ {sp.RefreshRate} Hz";
            disp.Add("Liczba ekranów", System.Windows.Forms.Screen.AllScreens.Length.ToString());
            foreach (var scr in System.Windows.Forms.Screen.AllScreens)
                disp.Add(scr.Primary ? "Ekran główny" : "Ekran", $"{scr.Bounds.Width} × {scr.Bounds.Height}");
            sp.Sections.Add(disp);

            // ---------- Sieć ----------
            var net = new SpecSection { Key = "net", Title = "Sieć" };
            try
            {
                foreach (var ni in NetworkInterface.GetAllNetworkInterfaces())
                {
                    if (ni.OperationalStatus != OperationalStatus.Up) continue;
                    if (ni.NetworkInterfaceType == NetworkInterfaceType.Loopback || ni.NetworkInterfaceType == NetworkInterfaceType.Tunnel) continue;
                    if (ni.Description.Contains("Virtual") || ni.Description.Contains("Hyper-V") || ni.Description.Contains("VPN")) continue;
                    string kind = ni.NetworkInterfaceType == NetworkInterfaceType.Wireless80211 ? "Wi-Fi" : "Ethernet";
                    string speed = ni.Speed > 0 ? (ni.Speed >= 1_000_000_000 ? $"{ni.Speed / 1e9:0.#} Gb/s" : $"{ni.Speed / 1e6:0} Mb/s") : "";
                    net.Add(ni.Description, $"{kind} · {speed}".Trim(' ', '·'));
                    if (net.Headline == null) net.Headline = $"{kind} · {speed}".Trim(' ', '·');
                }
            }
            catch { }
            if (net.Headline == null) net.Headline = "Brak aktywnego połączenia";
            sp.Sections.Add(net);

            return sp;
        }

        private static string DecodeWmiString(ManagementBaseObject o, string prop)
        {
            try
            {
                if (o[prop] is ushort[] arr) return new string(arr.TakeWhile(c => c != 0).Select(c => (char)c).ToArray());
            }
            catch { }
            return "";
        }

        private static bool IsIntegrated(string name)
        {
            string n = name.ToLowerInvariant();
            if (n.Contains("intel") && (n.Contains("uhd") || n.Contains("iris") || n.Contains("hd graphics") || n == "intel(r) graphics" || n.Contains("intel(r) arc(tm) graphics"))) return true;
            if (n.Contains("amd") && n.Contains("radeon") && !n.Contains("rx") && !n.Contains("pro") && (n.Contains("graphics") || n.Contains("vega"))) return true;
            return false;
        }

        private static Dictionary<string, double> ReadVramFromRegistry()
        {
            var map = new Dictionary<string, double>();
            try
            {
                using var cls = Registry.LocalMachine.OpenSubKey(@"SYSTEM\CurrentControlSet\Control\Class\{4d36e968-e325-11ce-bfc1-08002be10318}");
                if (cls == null) return map;
                foreach (var sub in cls.GetSubKeyNames())
                {
                    if (sub.Length != 4) continue;
                    try
                    {
                        using var k = cls.OpenSubKey(sub);
                        var desc = k?.GetValue("DriverDesc")?.ToString();
                        var qw = k?.GetValue("HardwareInformation.qwMemorySize");
                        if (desc == null || qw == null) continue;
                        long bytes = qw is byte[] b ? (b.Length >= 8 ? BitConverter.ToInt64(b, 0) : 0) : Convert.ToInt64(qw);
                        double gb = bytes / 1073741824.0;
                        if (!map.ContainsKey(desc) || map[desc] < gb) map[desc] = gb;
                    }
                    catch { }
                }
            }
            catch { }
            return map;
        }
    }
}
