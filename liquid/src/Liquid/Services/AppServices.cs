using System.Windows;

namespace Liquid.Services
{
    public static class AppServices
    {
        public static AppSettings Settings;
        public static SensorService Sensors;
        public static FpsService Fps;
        public static FpsHub Hub;
        public static TrayService Tray;
        public static MainWindow Main;

        public static void ShowMain()
        {
            var m = Main;
            if (m == null) return;
            if (!m.IsVisible) m.Show();
            if (m.WindowState == WindowState.Minimized) m.WindowState = WindowState.Normal;
            m.Activate();
            m.Topmost = true; m.Topmost = false;
            m.Focus();
        }
    }
}
