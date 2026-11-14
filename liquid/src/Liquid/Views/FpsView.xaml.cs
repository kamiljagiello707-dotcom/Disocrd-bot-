using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using Liquid.Services;

namespace Liquid.Views
{
    public partial class FpsView : UserControl
    {
        private bool _init = true;
        private static readonly HashSet<string> Ignored = new HashSet<string>(StringComparer.OrdinalIgnoreCase)
        {
            "explorer", "Liquid", "ApplicationFrameHost", "TextInputHost", "SystemSettings", "ShellExperienceHost",
            "SearchHost", "StartMenuExperienceHost", "LockApp", "NVIDIA Share", "dwm", "taskmgr"
        };

        public FpsView()
        {
            InitializeComponent();
            var st = AppServices.Settings;
            OvFt.IsChecked = st.OvFrametime;
            OvLows.IsChecked = st.OvLows;
            OvCpu.IsChecked = st.OvCpu;
            OvGpu.IsChecked = st.OvGpu;
            OvRam.IsChecked = st.OvRam;
            OvClock.IsChecked = st.OvClock;
            ScaleSlider.Value = st.OverlayScale;
            OpacitySlider.Value = st.OverlayOpacity;
            ScaleText.Text = $"{st.OverlayScale * 100:0}%";
            OpacityText.Text = $"{st.OverlayOpacity * 100:0}%";
            foreach (ComboBoxItem it in Corner.Items)
                if ((string)it.Tag == st.OverlayCorner) Corner.SelectedItem = it;
            if (Corner.SelectedIndex < 0) Corner.SelectedIndex = 0;
            LoadGames();
            _init = false;

            AppServices.Hub.Updated += OnStats;
            AppServices.Hub.Changed += UpdateState;
            AppServices.Hub.BenchmarkFinished += OnBenchmark;
            UpdateState();
        }

        private void LoadGames()
        {
            bool prev = _init;
            _init = true;
            GameBox.Items.Clear();
            GameBox.Items.Add(new ComboBoxItem { Content = "Automatycznie — aktywne okno", Tag = "" });
            string target = AppServices.Hub.Target;
            bool found = false;
            try
            {
                var procs = Process.GetProcesses()
                    .Where(p => { try { return p.MainWindowHandle != IntPtr.Zero && !string.IsNullOrWhiteSpace(p.MainWindowTitle); } catch { return false; } })
                    .Where(p => !Ignored.Contains(p.ProcessName))
                    .GroupBy(p => p.ProcessName, StringComparer.OrdinalIgnoreCase).Select(g => g.First())
                    .OrderBy(p => p.MainWindowTitle);
                foreach (var p in procs)
                {
                    string title = p.MainWindowTitle.Length > 42 ? p.MainWindowTitle.Substring(0, 42) + "…" : p.MainWindowTitle;
                    var item = new ComboBoxItem { Content = $"{title}  ({p.ProcessName}.exe)", Tag = p.ProcessName };
                    GameBox.Items.Add(item);
                    if (string.Equals(p.ProcessName, target, StringComparison.OrdinalIgnoreCase)) { GameBox.SelectedItem = item; found = true; }
                }
            }
            catch { }
            if (!string.IsNullOrEmpty(target) && !found)
            {
                var item = new ComboBoxItem { Content = $"{target}.exe  (nieuruchomiona)", Tag = target };
                GameBox.Items.Add(item);
                GameBox.SelectedItem = item;
            }
            if (GameBox.SelectedIndex < 0) GameBox.SelectedIndex = 0;
            _init = prev;
        }

        private void RefreshGames_Click(object sender, RoutedEventArgs e)
        {
            LoadGames();
            Toast.Show("Lista odświeżona", $"Znaleziono {GameBox.Items.Count - 1} programów z oknem.", ToastKind.Info);
        }

        private void GameBox_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (_init || GameBox.SelectedItem is not ComboBoxItem it) return;
            string name = it.Tag as string ?? "";
            AppServices.Hub.Target = name;
            Toast.Show("Wybrano grę", name == "" ? "Tryb automatyczny — liczę FPS aktywnego okna." : $"Licznik FPS będzie śledził {name}.exe", ToastKind.Success);
            UpdateState();
        }

        private void Toggle_Click(object sender, RoutedEventArgs e)
        {
            if (AppServices.Hub.CounterOn) AppServices.Hub.StopCounter();
            else AppServices.Hub.StartCounter();
        }

        private void UpdateState()
        {
            var hub = AppServices.Hub;
            ToggleText.Text = hub.CounterOn ? "Wyłącz licznik FPS" : "Włącz licznik FPS";
            ToggleBtn.Background = hub.CounterOn ? new SolidColorBrush(Color.FromRgb(0x16, 0x16, 0x16)) : Brushes.White;
            ToggleBtn.Foreground = hub.CounterOn ? Brushes.White : Brushes.Black;
            ToggleBtn.BorderBrush = hub.CounterOn ? new SolidColorBrush(Color.FromRgb(0x44, 0x44, 0x44)) : Brushes.White;
            ToggleBtn.BorderThickness = new Thickness(1);
            ToggleIcon.Stroke = hub.CounterOn ? Brushes.White : Brushes.Black;
            OvOn.IsChecked = hub.OverlayOn;
            if (!hub.CounterOn) TargetInfo.Text = "Licznik jest wyłączony — kliknij „Włącz licznik FPS”.";
            if (!hub.BenchmarkRunning) BenchBar.Width = 0;
        }

        private void OnStats(FpsStats s)
        {
            var hub = AppServices.Hub;
            if (!hub.CounterOn)
            {
                BigFps.Text = "—"; BigGame.Text = "licznik wyłączony";
                SAvg.Text = SLow1.Text = SLow01.Text = SMinMax.Text = SFt.Text = STemps.Text = "—";
                Graph.SetData(Array.Empty<double>());
                FramesInfo.Text = "";
                return;
            }
            BigFps.Text = s.Active ? $"{s.Fps:0}" : "—";
            BigGame.Text = s.Pid > 0 ? $"{s.ProcessName}.exe" : "czekam na grę…";
            TargetInfo.Text = s.Active ? $"Śledzę: {s.ProcessName}.exe (PID {s.Pid})"
                : hub.Target == "" ? "Wejdź do gry — zostanie wykryta automatycznie." : $"Czekam, aż {hub.Target}.exe zacznie renderować…";
            bool enough = s.Frames > 30;
            SAvg.Text = enough ? $"{s.Avg:0}" : "—";
            SLow1.Text = enough ? $"{s.Low1:0}" : "—";
            SLow01.Text = s.Frames > 300 ? $"{s.Low01:0}" : "—";
            SMinMax.Text = s.Max > 0 ? $"{s.Min:0} / {s.Max:0}" : "—";
            SFt.Text = s.Active ? $"{s.FrameTime:0.0} ms" : "—";
            var sn = AppServices.Sensors.Latest;
            STemps.Text = $"{(sn.CpuTemp.HasValue ? $"{sn.CpuTemp:0}°" : "—")} / {(sn.GpuTemp.HasValue ? $"{sn.GpuTemp:0}°" : "—")}";
            Graph.SetData(s.Recent);
            FramesInfo.Text = s.Frames > 0 ? $"{s.Frames:N0} klatek zmierzonych" : "";

            if (hub.BenchmarkRunning)
            {
                BenchBar.Width = BenchTrack.ActualWidth * hub.BenchmarkProgress;
                BenchStatus.Text = $"Trwa pomiar… {hub.BenchmarkProgress * 100:0}%";
            }
        }

        private void OnBenchmark(FpsStats r, int seconds)
        {
            NoResults.Visibility = Visibility.Collapsed;
            BenchBar.Width = BenchTrack.ActualWidth;
            BenchStatus.Text = "Pomiar zakończony.";
            var row = new Border
            {
                Background = Brushes.White, CornerRadius = new CornerRadius(10), Padding = new Thickness(14, 10, 14, 10), Margin = new Thickness(0, 0, 0, 6)
            };
            var dp = new DockPanel();
            var avg = new TextBlock { Text = $"{r.Avg:0} FPS", FontSize = 20, FontWeight = FontWeights.Black, Foreground = Brushes.Black, VerticalAlignment = VerticalAlignment.Center };
            DockPanel.SetDock(avg, Dock.Right);
            dp.Children.Add(avg);
            var info = new StackPanel();
            info.Children.Add(new TextBlock { Text = $"{r.ProcessName}.exe · {seconds} s · {DateTime.Now:HH:mm}", FontSize = 12, FontWeight = FontWeights.SemiBold, Foreground = Brushes.Black });
            info.Children.Add(new TextBlock { Text = $"1% low {r.Low1:0} · 0.1% low {r.Low01:0} · min {r.Min:0} · max {r.Max:0}", FontSize = 12, Foreground = new SolidColorBrush(Color.FromRgb(0x55, 0x55, 0x55)) });
            dp.Children.Add(info);
            row.Child = dp;
            BenchResults.Children.Insert(1, row);
            while (BenchResults.Children.Count > 6) BenchResults.Children.RemoveAt(BenchResults.Children.Count - 1);
        }

        private void Bench_Click(object sender, RoutedEventArgs e)
        {
            if (sender is FrameworkElement fe && int.TryParse(fe.Tag as string, out int sec))
            {
                AppServices.Hub.StartBenchmark(sec);
                if (AppServices.Hub.BenchmarkRunning) BenchStatus.Text = "Trwa pomiar… 0%";
            }
        }

        private void OvOn_Click(object sender, RoutedEventArgs e)
        {
            if (OvOn.IsChecked == true) AppServices.Hub.ShowOverlay(); else AppServices.Hub.HideOverlay(false);
            OvOn.IsChecked = AppServices.Hub.OverlayOn;
        }

        private void OvOpt_Click(object sender, RoutedEventArgs e)
        {
            var st = AppServices.Settings;
            st.OvFrametime = OvFt.IsChecked == true;
            st.OvLows = OvLows.IsChecked == true;
            st.OvCpu = OvCpu.IsChecked == true;
            st.OvGpu = OvGpu.IsChecked == true;
            st.OvRam = OvRam.IsChecked == true;
            st.OvClock = OvClock.IsChecked == true;
            st.Save();
            if (sender is CheckBox cb)
                Toast.Show(cb.IsChecked == true ? "Dodano do overlayu" : "Usunięto z overlayu", cb.Content?.ToString() ?? "", ToastKind.Info);
        }

        private void Corner_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (_init || Corner.SelectedItem is not ComboBoxItem it) return;
            AppServices.Settings.OverlayCorner = it.Tag as string ?? "TopLeft";
            AppServices.Settings.Save();
            AppServices.Hub.RefreshOverlaySettings();
            Toast.Show("Pozycja overlayu", it.Content?.ToString() ?? "", ToastKind.Info);
        }

        private void Scale_Changed(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            if (_init || ScaleText == null) return;
            AppServices.Settings.OverlayScale = Math.Round(e.NewValue, 2);
            ScaleText.Text = $"{e.NewValue * 100:0}%";
            AppServices.Settings.Save();
            AppServices.Hub.RefreshOverlaySettings();
        }

        private void Opacity_Changed(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            if (_init || OpacityText == null) return;
            AppServices.Settings.OverlayOpacity = Math.Round(e.NewValue, 2);
            OpacityText.Text = $"{e.NewValue * 100:0}%";
            AppServices.Settings.Save();
            AppServices.Hub.RefreshOverlaySettings();
        }
    }
}
