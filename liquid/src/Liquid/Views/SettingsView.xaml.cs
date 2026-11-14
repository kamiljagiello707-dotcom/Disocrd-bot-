using System.Windows;
using System.Windows.Controls;
using Liquid.Services;

namespace Liquid.Views
{
    public partial class SettingsView : UserControl
    {
        private bool _init = true;

        public SettingsView()
        {
            InitializeComponent();
            var s = AppServices.Settings;
            Notif.IsChecked = s.Notifications;
            Tray.IsChecked = s.MinimizeToTray;
            Autostart.IsChecked = s.StartWithWindows;
            Warn.IsChecked = s.TempWarnings;
            CpuWarn.Value = s.CpuWarnTemp;
            GpuWarn.Value = s.GpuWarnTemp;
            CpuWarnText.Text = $"{s.CpuWarnTemp}°C";
            GpuWarnText.Text = $"{s.GpuWarnTemp}°C";
            AboutTitle.Text = "Liquid v" + (typeof(SettingsView).Assembly.GetName().Version?.ToString(3) ?? "1.0.0");
            SettingsPath.Text = "Ustawienia: " + AppSettings.Folder;
            _init = false;
        }

        private void Changed(object sender, RoutedEventArgs e)
        {
            if (_init) return;
            var s = AppServices.Settings;
            s.Notifications = Notif.IsChecked == true;
            s.MinimizeToTray = Tray.IsChecked == true;
            s.TempWarnings = Warn.IsChecked == true;
            s.Save();
            if (sender is CheckBox cb)
                Toast.Show(cb.IsChecked == true ? "Włączono" : "Wyłączono", cb.Content?.ToString() ?? "", cb.IsChecked == true ? ToastKind.Success : ToastKind.Info, force: true);
        }

        private void Autostart_Click(object sender, RoutedEventArgs e)
        {
            bool want = Autostart.IsChecked == true;
            if (StartupTask.Set(want))
            {
                AppServices.Settings.StartWithWindows = want;
                AppServices.Settings.Save();
                Toast.Show(want ? "Autostart włączony" : "Autostart wyłączony",
                    want ? "Liquid uruchomi się w tle po zalogowaniu." : "Liquid nie będzie startował z Windows.", ToastKind.Success);
            }
            else
            {
                Autostart.IsChecked = !want;
                Toast.Show("Nie udało się zmienić autostartu", "Harmonogram zadań odrzucił zmianę.", ToastKind.Error);
            }
        }

        private void Warn_Changed(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            if (_init || CpuWarnText == null || GpuWarnText == null) return;
            var s = AppServices.Settings;
            s.CpuWarnTemp = (int)CpuWarn.Value;
            s.GpuWarnTemp = (int)GpuWarn.Value;
            CpuWarnText.Text = $"{s.CpuWarnTemp}°C";
            GpuWarnText.Text = $"{s.GpuWarnTemp}°C";
            s.Save();
        }

        private void Test_Click(object sender, RoutedEventArgs e)
        {
            Toast.Show("Tak wyglądają powiadomienia", "Liquid poinformuje Cię o ważnych zdarzeniach.", ToastKind.Success, force: true);
        }
    }
}
