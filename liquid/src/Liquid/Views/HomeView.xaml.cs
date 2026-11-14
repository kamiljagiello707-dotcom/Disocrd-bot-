using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Threading;
using Liquid.Services;

namespace Liquid.Views
{
    public partial class HomeView : UserControl
    {
        private readonly DispatcherTimer _timer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(1) };

        public HomeView()
        {
            InitializeComponent();
            int h = DateTime.Now.Hour;
            Greeting.Text = h < 5 ? "Dobrej nocy" : h < 12 ? "Dzień dobry" : h < 18 ? "Cześć" : "Dobry wieczór";
            Greeting.Text += ", witaj w Liquid";
            _timer.Tick += (s, e) => Refresh();
            Loaded += (s, e) => { Refresh(); _timer.Start(); };
            Unloaded += (s, e) => _timer.Stop();
        }

        private static string F(float? v, string u) => v.HasValue ? $"{v.Value:0}{u}" : "—";

        private void Refresh()
        {
            var s = AppServices.Sensors.Latest;
            QCpuTemp.Text = F(s.CpuTemp, "°C");
            QGpuTemp.Text = F(s.GpuTemp, "°C");
            QCpuName.Text = s.CpuName;
            QGpuName.Text = s.GpuName;
            QLoad.Text = $"{F(s.CpuLoad, "%")} / {F(s.GpuLoad, "%")}";
            QRam.Text = F(s.RamLoad, "%");
            QRamSub.Text = s.RamUsedGb.HasValue && s.RamTotalGb.HasValue ? $"{s.RamUsedGb:0.0} / {s.RamTotalGb:0} GB w użyciu" : "w użyciu";
            if (!AppServices.Sensors.Ready) QCpuTemp.Text = "…";
            var hub = AppServices.Hub;
            FpsCardStatus.Text = hub.CounterOn ? (hub.Stats.Active ? $"Aktywny · {hub.Stats.Fps:0} FPS" : "Aktywny · czekam na grę") : "Otwórz";
        }

        private void Go_Click(object sender, RoutedEventArgs e)
        {
            if (sender is FrameworkElement fe) AppServices.Main?.Navigate(fe.Tag as string);
        }
    }
}
