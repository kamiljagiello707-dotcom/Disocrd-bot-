using System;
using System.Diagnostics;
using System.Linq;
using System.Windows.Threading;

namespace Liquid.Services
{
    /// <summary>Koordynuje licznik FPS: wybór gry, overlay, benchmark.</summary>
    public sealed class FpsHub
    {
        private readonly DispatcherTimer _timer;
        private OverlayWindow _overlay;
        private DateTime _lastLookup = DateTime.MinValue;
        private int _namedPid;
        private DateTime _benchEnd;
        private int _benchSeconds;
        private string _lastAnnounced = "";

        public bool CounterOn { get; private set; }
        public bool OverlayOn => _overlay != null;
        public bool BenchmarkRunning { get; private set; }
        public double BenchmarkProgress => BenchmarkRunning ? Math.Max(0, Math.Min(1, 1 - (_benchEnd - DateTime.Now).TotalSeconds / _benchSeconds)) : 0;
        public FpsStats Stats { get; private set; } = new FpsStats();

        public event Action Changed;
        public event Action<FpsStats> Updated;
        public event Action<FpsStats, int> BenchmarkFinished;

        public FpsHub()
        {
            _timer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(500) };
            _timer.Tick += (s, e) => Tick();
        }

        public bool StartCounter(bool quiet = false)
        {
            if (CounterOn) return true;
            if (!AppServices.Fps.Start())
            {
                Toast.Show("Nie udało się włączyć licznika", AppServices.Fps.LastError ?? "Nieznany błąd", ToastKind.Error);
                return false;
            }
            CounterOn = true;
            _lastAnnounced = "";
            _timer.Start();
            if (!quiet) Toast.Show("Licznik FPS włączony", Target == "" ? "Tryb automatyczny — wejdź do gry, a Liquid sam ją wykryje." : $"Śledzę grę: {Target}", ToastKind.Success);
            Changed?.Invoke();
            return true;
        }

        public void StopCounter()
        {
            if (!CounterOn) return;
            if (OverlayOn) HideOverlay(true);
            BenchmarkRunning = false;
            AppServices.Fps.Stop();
            CounterOn = false;
            _timer.Stop();
            Stats = new FpsStats();
            Updated?.Invoke(Stats);
            Toast.Show("Licznik FPS wyłączony", "Pomiar został zatrzymany.", ToastKind.Info);
            Changed?.Invoke();
        }

        public string Target
        {
            get => AppServices.Settings.TargetProcess ?? "";
            set
            {
                AppServices.Settings.TargetProcess = value ?? "";
                AppServices.Settings.Save();
                _lastLookup = DateTime.MinValue;
                _lastAnnounced = "";
                AppServices.Fps.ResetStats();
            }
        }

        public void ToggleOverlay()
        {
            if (OverlayOn) HideOverlay(false); else ShowOverlay();
        }

        public void ShowOverlay()
        {
            if (OverlayOn) return;
            if (!CounterOn && !StartCounter(true)) return;
            _overlay = new OverlayWindow();
            _overlay.Show();
            _overlay.UpdateData(Stats, AppServices.Sensors.Latest);
            Toast.Show("Overlay włączony", "Licznik FPS jest widoczny nad grą. Skrót: Ctrl + Shift + F10", ToastKind.Success);
            Changed?.Invoke();
        }

        public void HideOverlay(bool quiet)
        {
            if (!OverlayOn) return;
            try { _overlay.Close(); } catch { }
            _overlay = null;
            if (!quiet) Toast.Show("Overlay wyłączony", "Licznik nadal działa w aplikacji.", ToastKind.Info);
            Changed?.Invoke();
        }

        public void RefreshOverlaySettings() => _overlay?.ApplySettings();

        public void StartBenchmark(int seconds)
        {
            if (!CounterOn && !StartCounter(true)) return;
            if (!Stats.Active)
            {
                Toast.Show("Brak aktywnej gry", "Uruchom grę (lub wybierz ją z listy), a potem włącz benchmark.", ToastKind.Warning);
                return;
            }
            AppServices.Fps.ResetStats();
            _benchSeconds = seconds;
            _benchEnd = DateTime.Now.AddSeconds(seconds);
            BenchmarkRunning = true;
            Toast.Show("Benchmark wystartował", $"Mierzę FPS w {Stats.ProcessName} przez {seconds} s. Graj normalnie!", ToastKind.Info);
            Changed?.Invoke();
        }

        private int ResolveTarget()
        {
            var fps = AppServices.Fps;
            string name = Target;
            int self = Environment.ProcessId;
            if (!string.IsNullOrEmpty(name))
            {
                if ((DateTime.Now - _lastLookup).TotalSeconds > 2 || _namedPid == 0 || !fps.IsPresenting(_namedPid))
                {
                    _lastLookup = DateTime.Now;
                    try
                    {
                        var procs = Process.GetProcessesByName(name);
                        _namedPid = procs.Select(p => p.Id).FirstOrDefault(fps.IsPresenting);
                        if (_namedPid == 0) _namedPid = procs.Select(p => p.Id).FirstOrDefault();
                    }
                    catch { _namedPid = 0; }
                }
                return _namedPid;
            }
            int fg = Native.ForegroundProcessId();
            if (fg != 0 && fg != self && fps.IsPresenting(fg)) return fg;
            int cur = fps.TargetPid;
            if (cur != 0 && fps.IsPresenting(cur)) return cur;
            return fps.BusiestPid();
        }

        private void Tick()
        {
            if (!CounterOn) return;
            try
            {
                AppServices.Fps.TargetPid = ResolveTarget();
                Stats = AppServices.Fps.GetStats();

                if (Stats.Active && Stats.ProcessName != _lastAnnounced)
                {
                    _lastAnnounced = Stats.ProcessName;
                    Toast.Show("Wykryto grę", $"Liczę FPS dla: {Stats.ProcessName}", ToastKind.Success);
                }

                _overlay?.UpdateData(Stats, AppServices.Sensors.Latest);
                Updated?.Invoke(Stats);

                if (BenchmarkRunning && DateTime.Now >= _benchEnd)
                {
                    BenchmarkRunning = false;
                    var r = Stats;
                    Toast.Show("Benchmark zakończony", $"Średnio {r.Avg:0} FPS · 1% low {r.Low1:0} · 0.1% low {r.Low01:0}", ToastKind.Success);
                    BenchmarkFinished?.Invoke(r, _benchSeconds);
                    Changed?.Invoke();
                }
            }
            catch { }
        }
    }
}
