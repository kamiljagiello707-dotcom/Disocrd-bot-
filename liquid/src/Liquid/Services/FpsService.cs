using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading;
using Microsoft.Diagnostics.Tracing;
using Microsoft.Diagnostics.Tracing.Session;

namespace Liquid.Services
{
    public sealed class FpsStats
    {
        public bool Active;
        public int Pid;
        public string ProcessName = "";
        public double Fps, FrameTime, Avg, Low1, Low01, Min, Max;
        public int Frames;
        public double[] Recent = Array.Empty<double>();
    }

    /// <summary>
    /// Licznik FPS oparty na ETW (to samo źródło co PresentMon): zdarzenia Present z DXGI (DX10/11/12, większość Vulkan/OpenGL przez DXGI) i D3D9.
    /// </summary>
    public sealed class FpsService : IDisposable
    {
        private static readonly Guid DxgiProvider = new Guid("CA11C036-0102-4A2D-A6AD-F03CFED5D3C9");
        private static readonly Guid D3D9Provider = new Guid("783ACA0A-790E-4D7F-8451-AA850511C6B9");
        private const string SessionName = "LiquidFpsSession";

        private TraceEventSession _session;
        private Thread _thread;
        private readonly object _lock = new object();
        private readonly Dictionary<int, ProcFrames> _procs = new Dictionary<int, ProcFrames>();
        private readonly List<double> _frameTimes = new List<double>(4096);
        private readonly List<double> _fpsSamples = new List<double>(512);
        private readonly int _selfPid = Environment.ProcessId;
        private volatile int _targetPid;

        public bool IsRunning { get; private set; }
        public string LastError { get; private set; }

        private sealed class ProcFrames
        {
            public readonly List<double> Times = new List<double>(1024);
            public DateTime LastSeen;
            public double LastTs = -1;
        }

        public int TargetPid
        {
            get => _targetPid;
            set
            {
                if (_targetPid == value) return;
                _targetPid = value;
                ResetStats();
            }
        }

        public bool Start()
        {
            if (IsRunning) return true;
            try
            {
                if (!(TraceEventSession.IsElevated() ?? false))
                {
                    LastError = "Licznik FPS wymaga uprawnień administratora.";
                    return false;
                }
                _session = new TraceEventSession(SessionName) { StopOnDispose = true };
                _session.EnableProvider(DxgiProvider, TraceEventLevel.Verbose, ulong.MaxValue);
                _session.EnableProvider(D3D9Provider, TraceEventLevel.Verbose, ulong.MaxValue);
                _session.Source.AllEvents += OnEvent;
                _thread = new Thread(() =>
                {
                    try { _session.Source.Process(); } catch (Exception ex) { LastError = ex.Message; }
                }) { IsBackground = true, Name = "Liquid.FPS" };
                _thread.Start();
                IsRunning = true;
                ResetStats();
                return true;
            }
            catch (Exception ex)
            {
                LastError = ex.Message;
                Stop();
                return false;
            }
        }

        public void Stop()
        {
            IsRunning = false;
            try { _session?.Dispose(); } catch { }
            _session = null;
            lock (_lock) { _procs.Clear(); }
        }

        public void ResetStats()
        {
            lock (_lock) { _frameTimes.Clear(); _fpsSamples.Clear(); }
        }

        private void OnEvent(TraceEvent e)
        {
            int id = (int)e.ID;
            bool present = (id == 42 && e.ProviderGuid == DxgiProvider) || (id == 1 && e.ProviderGuid == D3D9Provider);
            if (!present) return;
            int pid = e.ProcessID;
            if (pid == _selfPid || pid <= 4) return;
            double ts = e.TimeStampRelativeMSec;

            lock (_lock)
            {
                if (!_procs.TryGetValue(pid, out var pf)) _procs[pid] = pf = new ProcFrames();
                pf.LastSeen = DateTime.UtcNow;
                if (pf.LastTs >= 0 && pid == _targetPid)
                {
                    double dt = ts - pf.LastTs;
                    if (dt > 0.05 && dt < 1000)
                    {
                        _frameTimes.Add(dt);
                        if (_frameTimes.Count > 60000) _frameTimes.RemoveRange(0, 10000);
                    }
                }
                pf.LastTs = ts;
                pf.Times.Add(ts);
                // przytnij do ostatnich 2 s
                double cutoff = ts - 2000;
                int remove = 0;
                while (remove < pf.Times.Count && pf.Times[remove] < cutoff) remove++;
                if (remove > 0) pf.Times.RemoveRange(0, remove);
            }
        }

        /// <summary>PID procesu, który aktualnie renderuje najwięcej klatek (fallback dla trybu auto).</summary>
        public int BusiestPid()
        {
            lock (_lock)
            {
                var now = DateTime.UtcNow;
                return _procs.Where(p => (now - p.Value.LastSeen).TotalSeconds < 2)
                             .OrderByDescending(p => p.Value.Times.Count)
                             .Select(p => p.Key).FirstOrDefault();
            }
        }

        public bool IsPresenting(int pid)
        {
            lock (_lock)
                return _procs.TryGetValue(pid, out var p) && (DateTime.UtcNow - p.LastSeen).TotalSeconds < 2.5;
        }

        /// <summary>Wywoływane z UI ~2x/s.</summary>
        public FpsStats GetStats()
        {
            var st = new FpsStats { Pid = _targetPid };
            if (st.Pid > 0)
            {
                try { st.ProcessName = Process.GetProcessById(st.Pid).ProcessName; } catch { st.ProcessName = "?"; }
            }
            lock (_lock)
            {
                if (st.Pid > 0 && _procs.TryGetValue(st.Pid, out var pf) && (DateTime.UtcNow - pf.LastSeen).TotalSeconds < 2.5 && pf.Times.Count > 1)
                {
                    st.Active = true;
                    double last = pf.Times[pf.Times.Count - 1];
                    int n = 0;
                    for (int i = pf.Times.Count - 1; i >= 0 && pf.Times[i] > last - 1000; i--) n++;
                    st.Fps = n;
                    _fpsSamples.Add(st.Fps);
                    if (_fpsSamples.Count > 7200) _fpsSamples.RemoveAt(0);
                }

                int count = _frameTimes.Count;
                st.Frames = count;
                if (count > 0)
                {
                    int recentN = Math.Min(count, 240);
                    st.Recent = _frameTimes.GetRange(count - recentN, recentN).ToArray();
                    int lastN = Math.Min(count, 30);
                    double sumLast = 0;
                    for (int i = count - lastN; i < count; i++) sumLast += _frameTimes[i];
                    st.FrameTime = sumLast / lastN;

                    double mean = _frameTimes.Average();
                    st.Avg = mean > 0 ? 1000.0 / mean : 0;
                    var sorted = _frameTimes.ToArray();
                    Array.Sort(sorted);
                    st.Low1 = 1000.0 / WorstAverage(sorted, 0.01);
                    st.Low01 = 1000.0 / WorstAverage(sorted, 0.001);
                }
                if (_fpsSamples.Count > 0)
                {
                    st.Min = _fpsSamples.Min();
                    st.Max = _fpsSamples.Max();
                }
            }
            return st;
        }

        private static double WorstAverage(double[] sortedAsc, double fraction)
        {
            int n = Math.Max(1, (int)Math.Ceiling(sortedAsc.Length * fraction));
            double sum = 0;
            for (int i = sortedAsc.Length - n; i < sortedAsc.Length; i++) sum += sortedAsc[i];
            double avg = sum / n;
            return avg <= 0 ? 1 : avg;
        }

        public void Dispose() => Stop();
    }
}
