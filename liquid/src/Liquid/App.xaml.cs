using System;
using System.IO;
using System.Linq;
using System.Threading;
using System.Windows;
using System.Windows.Interop;
using System.Windows.Threading;
using Liquid.Services;

namespace Liquid
{
    public partial class App : Application
    {
        private Mutex _mutex;
        private EventWaitHandle _showEvent;
        private DateTime _lastCpuWarn = DateTime.MinValue, _lastGpuWarn = DateTime.MinValue;

        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);

            _mutex = new Mutex(true, "Liquid_SingleInstance_7F3A", out bool created);
            if (!created)
            {
                try { EventWaitHandle.OpenExisting("Liquid_Show_7F3A").Set(); } catch { }
                Shutdown();
                return;
            }
            _showEvent = new EventWaitHandle(false, EventResetMode.AutoReset, "Liquid_Show_7F3A");
            new Thread(() =>
            {
                while (true)
                {
                    try { _showEvent.WaitOne(); } catch { return; }
                    Dispatcher.BeginInvoke(new Action(AppServices.ShowMain));
                }
            }) { IsBackground = true }.Start();

            DispatcherUnhandledException += OnUiError;
            AppDomain.CurrentDomain.UnhandledException += (s, ex) => Log(ex.ExceptionObject as Exception);

            AppServices.Settings = AppSettings.Load();
            AppServices.Sensors = new SensorService();
            AppServices.Sensors.Updated += CheckTemps;
            AppServices.Sensors.Start();
            AppServices.Fps = new FpsService();
            AppServices.Hub = new FpsHub();

            var main = new MainWindow();
            AppServices.Main = main;
            MainWindow = main;
            AppServices.Tray = new TrayService();

            bool minimized = e.Args.Any(a => a.Equals("--minimized", StringComparison.OrdinalIgnoreCase));
            if (minimized) new WindowInteropHelper(main).EnsureHandle();
            else main.Show();
            main.InitHotkeys();

            if (minimized)
                Toast.Show("Liquid działa w tle", "Kliknij ikonę w zasobniku, aby otworzyć.", ToastKind.Info);
            else
                Toast.Show("Witaj w Liquid!", "Wszystko gotowe. Wybierz sekcję z menu po lewej.", ToastKind.Success);
        }

        private void CheckTemps()
        {
            var st = AppServices.Settings;
            if (st == null || !st.TempWarnings) return;
            var s = AppServices.Sensors.Latest;
            var now = DateTime.Now;
            if (s.CpuTemp >= st.CpuWarnTemp && (now - _lastCpuWarn).TotalMinutes > 2)
            {
                _lastCpuWarn = now;
                Toast.Show("Procesor się grzeje!", $"{s.CpuTemp:0}°C (próg {st.CpuWarnTemp}°C). Sprawdź chłodzenie i przepływ powietrza.", ToastKind.Warning);
            }
            if (s.GpuTemp >= st.GpuWarnTemp && (now - _lastGpuWarn).TotalMinutes > 2)
            {
                _lastGpuWarn = now;
                Toast.Show("Karta graficzna się grzeje!", $"{s.GpuTemp:0}°C (próg {st.GpuWarnTemp}°C). Rozważ wyższe obroty wentylatorów.", ToastKind.Warning);
            }
        }

        private void OnUiError(object sender, DispatcherUnhandledExceptionEventArgs e)
        {
            Log(e.Exception);
            e.Handled = true;
            Toast.Show("Ups, coś poszło nie tak", e.Exception.Message, ToastKind.Error);
        }

        private static void Log(Exception ex)
        {
            if (ex == null) return;
            try
            {
                Directory.CreateDirectory(AppSettings.Folder);
                File.AppendAllText(Path.Combine(AppSettings.Folder, "error.log"), $"[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] {ex}\r\n\r\n");
            }
            catch { }
        }

        protected override void OnExit(ExitEventArgs e)
        {
            try { AppServices.Fps?.Dispose(); } catch { }
            try { AppServices.Sensors?.Dispose(); } catch { }
            try { AppServices.Tray?.Dispose(); } catch { }
            try { AppServices.Settings?.Save(); } catch { }
            try { _mutex?.ReleaseMutex(); } catch { }
            base.OnExit(e);
        }
    }
}
