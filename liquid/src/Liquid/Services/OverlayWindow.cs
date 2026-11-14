using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Threading;

namespace Liquid.Services
{
    /// <summary>Przezroczysty, "przeklikiwalny" overlay z licznikiem FPS wyświetlany nad grą.</summary>
    public sealed class OverlayWindow : Window
    {
        private readonly Border _box;
        private readonly TextBlock _fps, _fpsLabel, _game;
        private readonly StackPanel _rows;
        private readonly ScaleTransform _scale = new ScaleTransform(1, 1);
        private readonly DispatcherTimer _topmost;
        private IntPtr _hwnd;
        private static readonly FontFamily Mono = new FontFamily("Consolas, Segoe UI");
        private static readonly FontFamily Ui = new FontFamily("Segoe UI Variable Display, Segoe UI");
        private static readonly Brush Dim = MakeBrush(0xA0, 0xA0, 0xA0);

        private static Brush MakeBrush(byte r, byte g, byte b) { var x = new SolidColorBrush(Color.FromRgb(r, g, b)); x.Freeze(); return x; }

        public OverlayWindow()
        {
            WindowStyle = WindowStyle.None;
            AllowsTransparency = true;
            Background = Brushes.Transparent;
            Topmost = true;
            ShowInTaskbar = false;
            ShowActivated = false;
            Focusable = false;
            IsHitTestVisible = false;
            ResizeMode = ResizeMode.NoResize;
            SizeToContent = SizeToContent.WidthAndHeight;
            Title = "Liquid Overlay";

            _fpsLabel = new TextBlock { Text = "FPS", FontFamily = Ui, FontSize = 11, FontWeight = FontWeights.Bold, Foreground = Dim, VerticalAlignment = VerticalAlignment.Bottom, Margin = new Thickness(6, 0, 0, 5) };
            _fps = new TextBlock { Text = "—", FontFamily = Ui, FontSize = 30, FontWeight = FontWeights.Bold, Foreground = Brushes.White };
            var top = new StackPanel { Orientation = Orientation.Horizontal };
            top.Children.Add(_fps);
            top.Children.Add(_fpsLabel);
            _game = new TextBlock { FontFamily = Ui, FontSize = 10, Foreground = Dim, Margin = new Thickness(0, -2, 0, 4) };
            _rows = new StackPanel();
            var stack = new StackPanel();
            stack.Children.Add(top);
            stack.Children.Add(_game);
            stack.Children.Add(_rows);

            _box = new Border
            {
                CornerRadius = new CornerRadius(12),
                Padding = new Thickness(14, 8, 16, 10),
                BorderThickness = new Thickness(1),
                BorderBrush = new SolidColorBrush(Color.FromArgb(0x40, 0xFF, 0xFF, 0xFF)),
                Child = stack,
                LayoutTransform = _scale,
                MinWidth = 120
            };
            Content = _box;
            ApplySettings();

            SizeChanged += (s, e) => Reposition();
            _topmost = new DispatcherTimer { Interval = TimeSpan.FromSeconds(1.5) };
            _topmost.Tick += (s, e) =>
            {
                if (_hwnd != IntPtr.Zero)
                    Native.SetWindowPos(_hwnd, Native.HWND_TOPMOST, 0, 0, 0, 0, Native.SWP_NOMOVE | Native.SWP_NOSIZE | Native.SWP_NOACTIVATE);
            };
            _topmost.Start();
            Closed += (s, e) => _topmost.Stop();
        }

        protected override void OnSourceInitialized(EventArgs e)
        {
            base.OnSourceInitialized(e);
            _hwnd = new WindowInteropHelper(this).Handle;
            Native.AddExStyle(_hwnd, Native.WS_EX_TRANSPARENT | Native.WS_EX_LAYERED | Native.WS_EX_TOOLWINDOW | Native.WS_EX_NOACTIVATE);
        }

        public void ApplySettings()
        {
            var st = AppServices.Settings;
            byte a = (byte)Math.Round(Math.Max(0, Math.Min(1, st.OverlayOpacity)) * 255);
            _box.Background = new SolidColorBrush(Color.FromArgb(a, 0, 0, 0));
            _scale.ScaleX = _scale.ScaleY = Math.Max(0.6, Math.Min(2.0, st.OverlayScale));
            Reposition();
        }

        public void Reposition()
        {
            double w = ActualWidth, h = ActualHeight;
            if (w <= 0) return;
            double sw = SystemParameters.PrimaryScreenWidth, sh = SystemParameters.PrimaryScreenHeight, m = 14;
            switch (AppServices.Settings.OverlayCorner)
            {
                case "TopRight": Left = sw - w - m; Top = m; break;
                case "BottomLeft": Left = m; Top = sh - h - m - 40; break;
                case "BottomRight": Left = sw - w - m; Top = sh - h - m - 40; break;
                default: Left = m; Top = m; break;
            }
        }

        private TextBlock Row(string label, string value)
        {
            var tb = new TextBlock { FontFamily = Mono, FontSize = 12.5, Foreground = Brushes.White, Margin = new Thickness(0, 1, 0, 0) };
            tb.Inlines.Add(new System.Windows.Documents.Run(label.PadRight(5)) { Foreground = Dim, FontWeight = FontWeights.Bold });
            tb.Inlines.Add(new System.Windows.Documents.Run(value));
            return tb;
        }

        public void UpdateData(FpsStats f, SensorSnapshot s)
        {
            var st = AppServices.Settings;
            bool active = f != null && f.Active;
            _fps.Text = active ? $"{f.Fps:0}" : "—";
            _game.Text = f != null && f.Pid > 0 ? f.ProcessName : "czekam na grę…";
            _rows.Children.Clear();
            if (st.OvFrametime && active) _rows.Children.Add(Row("FT", $"{f.FrameTime:0.0} ms"));
            if (st.OvLows && active && f.Frames > 50) _rows.Children.Add(Row("LOW", $"1% {f.Low1:0} · 0.1% {f.Low01:0}"));
            if (st.OvCpu && s != null) _rows.Children.Add(Row("CPU", $"{Fmt(s.CpuTemp, "°C")} · {Fmt(s.CpuLoad, "%")}"));
            if (st.OvGpu && s != null) _rows.Children.Add(Row("GPU", $"{Fmt(s.GpuTemp, "°C")} · {Fmt(s.GpuLoad, "%")}"));
            if (st.OvRam && s != null) _rows.Children.Add(Row("RAM", Fmt(s.RamLoad, "%") + (s.RamUsedGb != null ? $" · {s.RamUsedGb:0.0} GB" : "")));
            if (st.OvClock) _rows.Children.Add(Row("TIME", DateTime.Now.ToString("HH:mm")));
        }

        private static string Fmt(float? v, string unit) => v.HasValue ? $"{v.Value:0}{unit}" : "—";
    }
}
