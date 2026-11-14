using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Threading;
using LibreHardwareMonitor.Hardware;
using Liquid.Services;

namespace Liquid.Views
{
    public partial class TempsView : UserControl
    {
        private readonly DispatcherTimer _timer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(1) };
        private readonly Dictionary<string, (TextBlock val, TextBlock minmax, Border bar, Grid track, SensorType type)> _rows =
            new Dictionary<string, (TextBlock, TextBlock, Border, Grid, SensorType)>();
        private readonly HashSet<string> _groups = new HashSet<string>();
        private readonly Dictionary<string, TextBlock> _tiles = new Dictionary<string, TextBlock>();
        private readonly List<double> _histCpu = new List<double>(), _histGpu = new List<double>();
        private double _leftH, _rightH;

        public TempsView()
        {
            InitializeComponent();
            HistCpu.UnitSuffix = "°"; HistGpu.UnitSuffix = "°";
            HistCpu.MinScale = 60; HistGpu.MinScale = 60;
            foreach (var (key, title) in new[] { ("cpupw", "MOC CPU"), ("cpuclk", "ZEGAR CPU"), ("gpupw", "MOC GPU"), ("hot", "GPU HOT SPOT"), ("vram", "VRAM"), ("ram", "RAM") })
            {
                var b = new Border { Style = (Style)FindResource("Card"), Margin = new Thickness(8), Padding = new Thickness(16, 14, 16, 14) };
                var sp = new StackPanel();
                sp.Children.Add(new TextBlock { Text = title, Style = (Style)FindResource("Caption") });
                var v = new TextBlock { Text = "—", FontSize = 17, FontWeight = FontWeights.Bold, Foreground = Brushes.White, Margin = new Thickness(0, 6, 0, 0), TextTrimming = TextTrimming.CharacterEllipsis };
                sp.Children.Add(v);
                b.Child = sp;
                Tiles.Children.Add(b);
                _tiles[key] = v;
            }
            _timer.Tick += (s, e) => Refresh();
            Loaded += (s, e) => { Refresh(); _timer.Start(); };
            Unloaded += (s, e) => _timer.Stop();
        }

        private static string F(float? v, string fmt, string u) => v.HasValue ? v.Value.ToString(fmt) + u : "—";

        private void Refresh()
        {
            var svc = AppServices.Sensors;
            var s = svc.Latest;
            bool warnOn = AppServices.Settings.TempWarnings;

            GCpuT.Value = s.CpuTemp ?? 0; GCpuT.Text = F(s.CpuTemp, "0", "");
            GCpuT.Warn = warnOn && s.CpuTemp >= AppServices.Settings.CpuWarnTemp;
            GGpuT.Value = s.GpuTemp ?? 0; GGpuT.Text = F(s.GpuTemp, "0", "");
            GGpuT.Warn = warnOn && s.GpuTemp >= AppServices.Settings.GpuWarnTemp;
            GCpuL.Value = s.CpuLoad ?? 0; GCpuL.Text = F(s.CpuLoad, "0", "");
            GGpuL.Value = s.GpuLoad ?? 0; GGpuL.Text = F(s.GpuLoad, "0", "");
            LCpuT.Text = s.CpuTempSource == "acpi" ? "odczyt ACPI (przybliżony)" : Short(s.CpuName);
            LGpuT.Text = Short(s.GpuName);
            LCpuL.Text = s.CpuClock.HasValue ? $"{s.CpuClock / 1000:0.00} GHz" : "—";
            LGpuL.Text = s.GpuClock.HasValue ? $"{s.GpuClock:0} MHz" : "—";

            _tiles["cpupw"].Text = F(s.CpuPower, "0", " W");
            _tiles["cpuclk"].Text = s.CpuClock.HasValue ? $"{s.CpuClock / 1000:0.00} GHz" : "—";
            _tiles["gpupw"].Text = F(s.GpuPower, "0", " W");
            _tiles["hot"].Text = F(s.GpuHotSpot, "0", "°C");
            _tiles["vram"].Text = s.VramUsedMb.HasValue ? (s.VramTotalMb.HasValue ? $"{s.VramUsedMb / 1024:0.0}/{s.VramTotalMb / 1024:0} GB" : $"{s.VramUsedMb / 1024:0.0} GB") : "—";
            _tiles["ram"].Text = s.RamUsedGb.HasValue ? $"{s.RamUsedGb:0.0}/{s.RamTotalGb:0} GB" : F(s.RamLoad, "0", "%");

            if (svc.Ready)
            {
                Push(_histCpu, s.CpuTemp); Push(_histGpu, s.GpuTemp);
                HistCpu.SetData(_histCpu.ToArray());
                HistGpu.SetData(_histGpu.ToArray());
                HistLegend.Text = _histCpu.Count > 0 ? $"CPU max {_histCpu.Max():0}°C   ·   GPU max {(_histGpu.Count > 0 ? _histGpu.Max() : 0):0}°C" : "";
            }

            bool noCpuTemp = svc.Ready && s.CpuTemp == null;
            DriverBanner.Visibility = noCpuTemp && !SensorService.PawnIoInstalled ? Visibility.Visible : Visibility.Collapsed;

            if (!svc.Ready)
            {
                Waiting.Text = svc.Error != null ? "Błąd czujników: " + svc.Error : "Uruchamiam czujniki… (pierwszy odczyt może potrwać kilka sekund)";
                Waiting.Visibility = Visibility.Visible;
                return;
            }
            Waiting.Visibility = s.Groups.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
            if (s.Groups.Count == 0) Waiting.Text = "Brak dostępnych czujników.";

            foreach (var g in s.Groups) UpdateGroup(g);
        }

        private static void Push(List<double> list, float? v)
        {
            if (!v.HasValue) return;
            list.Add(v.Value);
            if (list.Count > 120) list.RemoveAt(0);
        }

        private static string Short(string n) =>
            System.Text.RegularExpressions.Regex.Replace(n ?? "", @"\(R\)|\(TM\)| CPU|Processor|\d+-Core|NVIDIA |AMD ", "").Trim();

        private static string Unit(SensorType t) => t switch
        {
            SensorType.Temperature => "°C", SensorType.Load => "%", SensorType.Clock => " MHz",
            SensorType.Fan => " RPM", SensorType.Power => " W", _ => ""
        };

        private static string TypeName(SensorType t) => t switch
        {
            SensorType.Temperature => "Temperatury", SensorType.Load => "Obciążenie", SensorType.Clock => "Zegary",
            SensorType.Fan => "Wentylatory", SensorType.Power => "Pobór mocy", _ => t.ToString()
        };

        private static double Scale(SensorType t, float v) => t switch
        {
            SensorType.Temperature => v / 110.0, SensorType.Load => v / 100.0, SensorType.Fan => v / 3000.0,
            SensorType.Power => v / 350.0, SensorType.Clock => v / 6000.0, _ => 0
        };

        private static string HwLabel(HardwareType t) => t switch
        {
            HardwareType.Cpu => "PROCESOR", HardwareType.GpuNvidia => "KARTA GRAFICZNA", HardwareType.GpuAmd => "KARTA GRAFICZNA",
            HardwareType.GpuIntel => "GRAFIKA", HardwareType.Memory => "PAMIĘĆ", HardwareType.Motherboard => "PŁYTA GŁÓWNA",
            HardwareType.Storage => "DYSK", _ => t.ToString().ToUpperInvariant()
        };

        private void UpdateGroup(HardwareGroup g)
        {
            if (!_groups.Contains(g.Id))
            {
                _groups.Add(g.Id);
                var card = new Border { Style = (Style)FindResource("Card"), Margin = new Thickness(8) };
                var stack = new StackPanel();
                card.Child = stack;
                stack.Children.Add(new TextBlock { Text = HwLabel(g.Type), Style = (Style)FindResource("Caption") });
                stack.Children.Add(new TextBlock { Text = g.Name, Style = (Style)FindResource("H2"), Margin = new Thickness(0, 2, 0, 8), TextWrapping = TextWrapping.Wrap });

                foreach (var grp in g.Sensors.GroupBy(x => x.Type).OrderBy(x => Order(x.Key)))
                {
                    stack.Children.Add(new TextBlock { Text = TypeName(grp.Key).ToUpperInvariant(), Style = (Style)FindResource("Caption"), Foreground = (Brush)FindResource("MutedBrush"), Margin = new Thickness(0, 10, 0, 2) });
                    foreach (var r in grp.Take(16)) stack.Children.Add(MakeRow(r));
                }
                double est = 80 + g.Sensors.Count * 34;
                if (_leftH <= _rightH) { GLeft.Children.Add(card); _leftH += est; } else { GRight.Children.Add(card); _rightH += est; }
            }

            foreach (var r in g.Sensors)
            {
                if (!_rows.TryGetValue(r.Id, out var row)) continue;
                string u = Unit(r.Type);
                string fmt = r.Type == SensorType.Clock || r.Type == SensorType.Fan ? "0" : r.Type == SensorType.Power ? "0.0" : "0";
                row.val.Text = r.Value.HasValue ? r.Value.Value.ToString(fmt) + u : "—";
                row.minmax.Text = r.Min.HasValue && r.Max.HasValue ? $"{r.Min.Value.ToString(fmt)} – {r.Max.Value.ToString(fmt)}" : "";
                bool hot = r.Type == SensorType.Temperature && r.Value >= 90;
                row.val.Foreground = hot ? (Brush)FindResource("DangerBrush") : Brushes.White;
                double frac = r.Value.HasValue ? Math.Max(0, Math.Min(1, Scale(r.Type, r.Value.Value))) : 0;
                row.bar.Width = Math.Max(0, row.track.ActualWidth * frac);
                row.bar.Background = hot ? (Brush)FindResource("DangerBrush") : Brushes.White;
            }
        }

        private static int Order(SensorType t) => t switch
        {
            SensorType.Temperature => 0, SensorType.Load => 1, SensorType.Fan => 2, SensorType.Power => 3, _ => 4
        };

        private FrameworkElement MakeRow(SensorReading r)
        {
            var grid = new Grid { Margin = new Thickness(0, 5, 0, 0) };
            grid.ColumnDefinitions.Add(new ColumnDefinition());
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(90) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(80) });
            grid.RowDefinitions.Add(new RowDefinition());
            grid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            grid.Children.Add(new TextBlock { Text = r.Name, Foreground = new SolidColorBrush(Color.FromRgb(0xC8, 0xC8, 0xC8)), FontSize = 13, TextTrimming = TextTrimming.CharacterEllipsis });
            var mm = new TextBlock { Foreground = (Brush)FindResource("MutedBrush"), FontSize = 11, HorizontalAlignment = HorizontalAlignment.Right, VerticalAlignment = VerticalAlignment.Center, Margin = new Thickness(0, 0, 10, 0) };
            Grid.SetColumn(mm, 1);
            grid.Children.Add(mm);
            var val = new TextBlock { Foreground = Brushes.White, FontSize = 13, FontWeight = FontWeights.SemiBold, HorizontalAlignment = HorizontalAlignment.Right };
            Grid.SetColumn(val, 2);
            grid.Children.Add(val);
            var track = new Grid { Height = 3, Margin = new Thickness(0, 5, 0, 2), Background = new SolidColorBrush(Color.FromRgb(0x22, 0x22, 0x22)) };
            var bar = new Border { HorizontalAlignment = HorizontalAlignment.Left, Background = Brushes.White, Width = 0 };
            track.Children.Add(bar);
            Grid.SetRow(track, 1); Grid.SetColumnSpan(track, 3);
            grid.Children.Add(track);
            _rows[r.Id] = (val, mm, bar, track, r.Type);
            return grid;
        }

        private void Reset_Click(object sender, RoutedEventArgs e)
        {
            _histCpu.Clear(); _histGpu.Clear();
            HistCpu.SetData(Array.Empty<double>()); HistGpu.SetData(Array.Empty<double>());
            Toast.Show("Wykres wyczyszczony", "Historia temperatur zaczyna się od nowa.", ToastKind.Info);
        }

        private void Driver_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                string dir = AppContext.BaseDirectory;
                string setup = Path.Combine(dir, "drivers", "PawnIO_setup.exe");
                if (!File.Exists(setup))
                {
                    Process.Start(new ProcessStartInfo("https://pawnio.eu") { UseShellExecute = true });
                    Toast.Show("Pobierz PawnIO", "Otworzyłem stronę sterownika. Po instalacji uruchom Liquid ponownie.", ToastKind.Info);
                    return;
                }
                DriverBtn.IsEnabled = false;
                Toast.Show("Instaluję sterownik…", "To potrwa kilka sekund.", ToastKind.Info);
                var p = Process.Start(new ProcessStartInfo(setup, "-install -silent") { UseShellExecute = true });
                System.Threading.Tasks.Task.Run(() =>
                {
                    p?.WaitForExit(60000);
                    Dispatcher.BeginInvoke(new Action(() =>
                    {
                        DriverBtn.IsEnabled = true;
                        if (SensorService.PawnIoInstalled)
                        {
                            AppServices.Sensors.Restart();
                            Toast.Show("Sterownik zainstalowany", "Temperatury procesora pojawią się za chwilę.", ToastKind.Success);
                        }
                        else Toast.Show("Instalacja nieudana", "Spróbuj ponownie lub zainstaluj PawnIO ręcznie z pawnio.eu.", ToastKind.Error);
                    }));
                });
            }
            catch (Exception ex)
            {
                DriverBtn.IsEnabled = true;
                Toast.Show("Błąd", ex.Message, ToastKind.Error);
            }
        }
    }
}
