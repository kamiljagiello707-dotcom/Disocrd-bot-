using System;
using System.ComponentModel;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Animation;
using Liquid.Services;
using Liquid.Views;

namespace Liquid
{
    public partial class MainWindow : Window
    {
        private const int HotkeyOverlay = 0x4C51;
        private const int HotkeyCounter = 0x4C52;
        private bool _reallyClose;
        private HomeView _home;
        private SpecsView _specs;
        private TempsView _temps;
        private FpsView _fps;
        private SettingsView _settings;

        public MainWindow()
        {
            InitializeComponent();
            VersionText.Text = "v" + (typeof(MainWindow).Assembly.GetName().Version?.ToString(3) ?? "1.0.0");
            StateChanged += (s, e) =>
            {
                RootBorder.Padding = WindowState == WindowState.Maximized ? new Thickness(7) : new Thickness(0);
                MaxButton.Content = WindowState == WindowState.Maximized ? "" : "";
            };
            Loaded += (s, e) => { if (PageHost.Content == null) NavHome.IsChecked = true; };
            AppServices.Hub.Changed += UpdateStatus;
        }

        protected override void OnSourceInitialized(EventArgs e)
        {
            base.OnSourceInitialized(e);
            Native.ApplyDarkRoundedFrame(new WindowInteropHelper(this).Handle);
        }

        public void InitHotkeys()
        {
            var hwnd = new WindowInteropHelper(this).EnsureHandle();
            var src = HwndSource.FromHwnd(hwnd);
            src?.AddHook(WndProc);
            bool ok = Native.RegisterHotKey(hwnd, HotkeyOverlay, Native.MOD_CONTROL | Native.MOD_SHIFT | Native.MOD_NOREPEAT, 0x79); // F10
            Native.RegisterHotKey(hwnd, HotkeyCounter, Native.MOD_CONTROL | Native.MOD_SHIFT | Native.MOD_NOREPEAT, 0x78);          // F9
            if (!ok) Toast.Show("Skrót zajęty", "Ctrl+Shift+F10 jest używany przez inny program — overlay włączysz z zakładki Licznik FPS.", ToastKind.Warning);
        }

        private IntPtr WndProc(IntPtr hwnd, int msg, IntPtr wParam, IntPtr lParam, ref bool handled)
        {
            if (msg == Native.WM_HOTKEY)
            {
                int id = wParam.ToInt32();
                if (id == HotkeyOverlay) { AppServices.Hub.ToggleOverlay(); handled = true; }
                else if (id == HotkeyCounter)
                {
                    if (AppServices.Hub.CounterOn) AppServices.Hub.StopCounter(); else AppServices.Hub.StartCounter();
                    handled = true;
                }
            }
            return IntPtr.Zero;
        }

        public void Navigate(string page)
        {
            switch (page)
            {
                case "specs": NavSpecs.IsChecked = true; break;
                case "temps": NavTemps.IsChecked = true; break;
                case "fps": NavFps.IsChecked = true; break;
                case "settings": NavSettings.IsChecked = true; break;
                default: NavHome.IsChecked = true; break;
            }
        }

        private void Nav_Checked(object sender, RoutedEventArgs e)
        {
            if (PageHost == null) return;
            UIElement view;
            if (sender == NavSpecs) view = _specs ??= new SpecsView();
            else if (sender == NavTemps) view = _temps ??= new TempsView();
            else if (sender == NavFps) view = _fps ??= new FpsView();
            else if (sender == NavSettings) view = _settings ??= new SettingsView();
            else view = _home ??= new HomeView();

            PageHost.Content = view;
            var ease = new CubicEase { EasingMode = EasingMode.EaseOut };
            PageHost.BeginAnimation(OpacityProperty, new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(260)));
            ((TranslateTransform)PageHost.RenderTransform).BeginAnimation(TranslateTransform.YProperty,
                new DoubleAnimation(14, 0, TimeSpan.FromMilliseconds(320)) { EasingFunction = ease });
        }

        private void UpdateStatus()
        {
            var hub = AppServices.Hub;
            if (hub.CounterOn)
            {
                StatusDot.Fill = Brushes.White;
                StatusText.Text = hub.OverlayOn ? "Licznik FPS + overlay aktywne" : "Licznik FPS aktywny";
            }
            else
            {
                StatusDot.Fill = new SolidColorBrush(Color.FromRgb(0x5A, 0x5A, 0x5A));
                StatusText.Text = "Licznik FPS wyłączony";
            }
        }

        private void Min_Click(object sender, RoutedEventArgs e) => WindowState = WindowState.Minimized;
        private void Max_Click(object sender, RoutedEventArgs e) =>
            WindowState = WindowState == WindowState.Maximized ? WindowState.Normal : WindowState.Maximized;
        private void Close_Click(object sender, RoutedEventArgs e) => Close();

        protected override void OnClosing(CancelEventArgs e)
        {
            if (!_reallyClose && AppServices.Settings.MinimizeToTray)
            {
                e.Cancel = true;
                Hide();
                if (!AppServices.Settings.TrayHintShown)
                {
                    AppServices.Settings.TrayHintShown = true;
                    AppServices.Settings.Save();
                }
                Toast.Show("Liquid działa w tle", "Znajdziesz go w zasobniku obok zegara. Zamknięcie: prawy klik → Zamknij.", ToastKind.Info);
                return;
            }
            base.OnClosing(e);
            if (!e.Cancel) Application.Current.Shutdown();
        }

        public void Quit()
        {
            _reallyClose = true;
            try { Close(); } catch { }
            Application.Current.Shutdown();
        }
    }
}
