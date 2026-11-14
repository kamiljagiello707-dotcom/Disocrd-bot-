using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Shapes;
using Liquid.Services;

namespace Liquid.Views
{
    public partial class SpecsView : UserControl
    {
        private SystemSpecs _specs;
        private bool _loading;

        private static readonly Dictionary<string, string> Icons = new Dictionary<string, string>
        {
            ["cpu"] = "IconChip", ["gpu"] = "IconGpu", ["ram"] = "IconRam", ["board"] = "IconBoard",
            ["disk"] = "IconDisk", ["os"] = "IconWin", ["display"] = "IconMonitor", ["net"] = "IconNet"
        };

        public SpecsView()
        {
            InitializeComponent();
            var spin = new DoubleAnimation(0, 360, TimeSpan.FromSeconds(1)) { RepeatBehavior = RepeatBehavior.Forever };
            Spin.BeginAnimation(RotateTransform.AngleProperty, spin);
            Loaded += (s, e) => { if (_specs == null && !_loading) Scan(false); };
        }

        private async void Scan(bool manual)
        {
            _loading = true;
            RefreshBtn.IsEnabled = false;
            Loader.Visibility = Visibility.Visible;
            CardsGrid.Visibility = Visibility.Collapsed;
            VerdictCard.Visibility = Visibility.Collapsed;
            try
            {
                var specs = await Task.Run(HardwareInfo.Collect);
                var verdict = await Task.Run(() => SetupAdvisor.Evaluate(specs));
                _specs = specs;
                Render(specs, verdict);
                CopyBtn.IsEnabled = true;
                Toast.Show(manual ? "Skanowanie zakończone" : "Parametry odczytane",
                    $"Twój setup: {verdict.Rating} ({verdict.Score}/100)", ToastKind.Success);
            }
            catch (Exception ex)
            {
                Toast.Show("Błąd skanowania", ex.Message, ToastKind.Error);
            }
            finally
            {
                _loading = false;
                RefreshBtn.IsEnabled = true;
                Loader.Visibility = Visibility.Collapsed;
            }
        }

        private void Render(SystemSpecs s, Verdict v)
        {
            ColLeft.Children.Clear();
            ColRight.Children.Clear();
            int i = 0;
            double hl = 0, hr = 0;
            foreach (var sec in s.Sections)
            {
                var card = BuildCard(sec);
                // prosty "masonry": dokładaj do niższej kolumny
                double est = 90 + sec.Rows.Count * 30;
                if (hl <= hr) { ColLeft.Children.Add(card); hl += est; } else { ColRight.Children.Add(card); hr += est; }
                Animate(card, i++);
            }
            CardsGrid.Visibility = Visibility.Visible;

            ScoreGauge.Value = v.Score;
            ScoreGauge.Text = v.Score.ToString();
            VerdictTitle.Text = v.Rating;
            VerdictText.Text = v.Message;
            PartBars.Children.Clear();
            PartBars.Children.Add(PartBar("Procesor", v.Cpu, Verdict.CpuMax));
            PartBars.Children.Add(PartBar("Karta graficzna", v.Gpu, Verdict.GpuMax));
            PartBars.Children.Add(PartBar("Pamięć RAM", v.Ram, Verdict.RamMax));
            PartBars.Children.Add(PartBar("Dysk", v.Disk, Verdict.DiskMax));
            ProsList.Children.Clear();
            ConsList.Children.Clear();
            if (v.Pros.Count == 0) v.Pros.Add("Brak wyraźnych atutów — ale każdy setup da się rozbudować!");
            if (v.Cons.Count == 0) v.Cons.Add("Nic istotnego — Twój komputer jest dobrze skonfigurowany.");
            foreach (var p in v.Pros) ProsList.Children.Add(Bullet(p, true));
            foreach (var c in v.Cons) ConsList.Children.Add(Bullet(c, false));
            VerdictCard.Visibility = Visibility.Visible;
            Animate(VerdictCard, i);
        }

        private static void Animate(FrameworkElement el, int index)
        {
            el.Opacity = 0;
            var tt = new TranslateTransform(0, 16);
            el.RenderTransform = tt;
            var delay = TimeSpan.FromMilliseconds(60 * index);
            var ease = new CubicEase { EasingMode = EasingMode.EaseOut };
            el.BeginAnimation(OpacityProperty, new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(350)) { BeginTime = delay });
            tt.BeginAnimation(TranslateTransform.YProperty, new DoubleAnimation(16, 0, TimeSpan.FromMilliseconds(420)) { BeginTime = delay, EasingFunction = ease });
        }

        private FrameworkElement BuildCard(SpecSection sec)
        {
            var card = new Border { Style = (Style)FindResource("Card"), Margin = new Thickness(8) };
            var stack = new StackPanel();
            card.Child = stack;

            var head = new DockPanel { Margin = new Thickness(0, 0, 0, 14) };
            var iconBox = new Border { Width = 42, Height = 42, CornerRadius = new CornerRadius(11), Background = Brushes.White };
            iconBox.Child = new Path
            {
                Style = (Style)FindResource("IconPath"), Stroke = Brushes.Black, Width = 21, Height = 21,
                Data = (Geometry)FindResource(Icons.TryGetValue(sec.Key, out var ic) ? ic : "IconChip")
            };
            DockPanel.SetDock(iconBox, Dock.Left);
            head.Children.Add(iconBox);
            var titles = new StackPanel { Margin = new Thickness(14, 0, 0, 0), VerticalAlignment = VerticalAlignment.Center };
            titles.Children.Add(new TextBlock { Text = sec.Title.ToUpperInvariant(), Style = (Style)FindResource("Caption") });
            titles.Children.Add(new TextBlock
            {
                Text = string.IsNullOrWhiteSpace(sec.Headline) ? "Brak danych" : sec.Headline,
                Style = (Style)FindResource("H2"), TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 2, 0, 0)
            });
            head.Children.Add(titles);
            stack.Children.Add(head);

            bool first = true;
            foreach (var r in sec.Rows)
            {
                var row = new Grid { Margin = new Thickness(0, 0, 0, 0) };
                row.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(0.9, GridUnitType.Star) });
                row.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1.3, GridUnitType.Star) });
                var line = new Border
                {
                    BorderBrush = (Brush)FindResource("LineBrush"),
                    BorderThickness = new Thickness(0, first ? 0 : 1, 0, 0),
                    Padding = new Thickness(0, 8, 0, 8),
                    Child = row
                };
                first = false;
                row.Children.Add(new TextBlock { Text = r.Key, Foreground = (Brush)FindResource("SubBrush"), FontSize = 13, TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 0, 10, 0) });
                var val = new TextBlock { Text = r.Value, Foreground = Brushes.White, FontSize = 13, FontWeight = FontWeights.SemiBold, TextWrapping = TextWrapping.Wrap };
                Grid.SetColumn(val, 1);
                row.Children.Add(val);
                stack.Children.Add(line);
            }
            if (sec.Rows.Count == 0)
                stack.Children.Add(new TextBlock { Text = "Nie udało się odczytać szczegółów.", Style = (Style)FindResource("Sub") });
            return card;
        }

        private static FrameworkElement PartBar(string name, int pts, int max)
        {
            var sp = new StackPanel { Margin = new Thickness(0, 0, 16, 0) };
            var top = new DockPanel();
            var val = new TextBlock { Text = $"{pts}/{max}", FontSize = 12, FontWeight = FontWeights.Bold, Foreground = Brushes.Black };
            DockPanel.SetDock(val, Dock.Right);
            top.Children.Add(val);
            top.Children.Add(new TextBlock { Text = name, FontSize = 12, Foreground = new SolidColorBrush(Color.FromRgb(0x44, 0x44, 0x44)) });
            sp.Children.Add(top);
            var track = new Grid { Height = 6, Margin = new Thickness(0, 6, 0, 0) };
            track.Children.Add(new Border { CornerRadius = new CornerRadius(3), Background = new SolidColorBrush(Color.FromRgb(0xE2, 0xE2, 0xE2)) });
            var fill = new Border { CornerRadius = new CornerRadius(3), Background = Brushes.Black, HorizontalAlignment = HorizontalAlignment.Left, Width = 0 };
            track.Children.Add(fill);
            track.SizeChanged += (s, e) =>
            {
                double target = e.NewSize.Width * pts / Math.Max(1, max);
                fill.BeginAnimation(WidthProperty, new DoubleAnimation(target, TimeSpan.FromMilliseconds(700)) { EasingFunction = new CubicEase { EasingMode = EasingMode.EaseOut } });
            };
            sp.Children.Add(track);
            return sp;
        }

        private static FrameworkElement Bullet(string text, bool good)
        {
            var dp = new DockPanel { Margin = new Thickness(0, 5, 0, 0) };
            var mark = new Border
            {
                Width = 18, Height = 18, CornerRadius = new CornerRadius(9), VerticalAlignment = VerticalAlignment.Top, Margin = new Thickness(0, 1, 10, 0),
                Background = good ? Brushes.Black : Brushes.White, BorderBrush = Brushes.Black, BorderThickness = new Thickness(1.5),
                Child = new TextBlock
                {
                    Text = good ? "✓" : "!", FontSize = 10, FontWeight = FontWeights.Bold,
                    Foreground = good ? Brushes.White : Brushes.Black,
                    HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center,
                    FontFamily = new FontFamily("Segoe UI Symbol, Segoe UI")
                }
            };
            DockPanel.SetDock(mark, Dock.Left);
            dp.Children.Add(mark);
            dp.Children.Add(new TextBlock { Text = text, FontSize = 13, Foreground = new SolidColorBrush(Color.FromRgb(0x22, 0x22, 0x22)), TextWrapping = TextWrapping.Wrap });
            return dp;
        }

        private void Refresh_Click(object sender, RoutedEventArgs e) { if (!_loading) Scan(true); }

        private void Copy_Click(object sender, RoutedEventArgs e)
        {
            if (_specs == null) return;
            try
            {
                Clipboard.SetText(_specs.ToText());
                Toast.Show("Skopiowano", "Specyfikacja jest w schowku — wklej ją np. na Discordzie.", ToastKind.Success);
            }
            catch (Exception ex) { Toast.Show("Nie udało się skopiować", ex.Message, ToastKind.Error); }
        }
    }
}
