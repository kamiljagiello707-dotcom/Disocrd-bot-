using System;
using System.Collections.Generic;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Effects;
using System.Windows.Shapes;
using System.Windows.Threading;

namespace Liquid.Services
{
    public enum ToastKind { Success, Info, Warning, Error }

    /// <summary>Ładne powiadomienia wysuwane w prawym dolnym rogu ekranu.</summary>
    public static class Toast
    {
        private static readonly List<ToastWindow> Open = new List<ToastWindow>();

        public static void Show(string title, string message, ToastKind kind = ToastKind.Info, bool force = false)
        {
            var app = Application.Current;
            if (app == null) return;
            if (!force && AppServices.Settings != null && !AppServices.Settings.Notifications && kind != ToastKind.Error) return;
            app.Dispatcher.BeginInvoke(new Action(() =>
            {
                try
                {
                    while (Open.Count >= 4) { var old = Open[0]; Open.RemoveAt(0); old.Close(); }
                    var w = new ToastWindow(title, message, kind);
                    w.Closed += (s, e) => { Open.Remove(w); Reposition(); };
                    Open.Add(w);
                    w.Show();
                    Reposition();
                }
                catch { }
            }));
        }

        internal static void Reposition()
        {
            var wa = SystemParameters.WorkArea;
            double y = wa.Bottom - 12;
            for (int i = Open.Count - 1; i >= 0; i--)
            {
                var w = Open[i];
                double h = w.ActualHeight > 0 ? w.ActualHeight : 110;
                y -= h;
                w.Left = wa.Right - w.Width - 12;
                w.Top = y;
            }
        }
    }

    internal sealed class ToastWindow : Window
    {
        private readonly DispatcherTimer _timer;
        private bool _closing;
        private Border _root;
        private TranslateTransform _slide;

        public ToastWindow(string title, string message, ToastKind kind)
        {
            WindowStyle = WindowStyle.None;
            AllowsTransparency = true;
            Background = Brushes.Transparent;
            Topmost = true;
            ShowInTaskbar = false;
            ShowActivated = false;
            Focusable = false;
            ResizeMode = ResizeMode.NoResize;
            Width = 384;
            SizeToContent = SizeToContent.Height;
            FontFamily = (FontFamily)Application.Current.Resources["UiFont"];
            Left = SystemParameters.WorkArea.Right - Width - 12;
            Top = SystemParameters.WorkArea.Bottom - 130;

            string glyph = kind switch { ToastKind.Success => "✓", ToastKind.Warning => "!", ToastKind.Error => "✕", _ => "i" };
            bool danger = kind == ToastKind.Error || kind == ToastKind.Warning;

            var root = new Border
            {
                Margin = new Thickness(12),
                CornerRadius = new CornerRadius(16),
                Background = new SolidColorBrush(Color.FromArgb(0xF5, 0x0E, 0x0E, 0x0E)),
                BorderBrush = new SolidColorBrush(Color.FromRgb(0x30, 0x30, 0x30)),
                BorderThickness = new Thickness(1),
                Effect = new DropShadowEffect { BlurRadius = 24, ShadowDepth = 4, Opacity = 0.6, Color = Colors.Black },
                Cursor = System.Windows.Input.Cursors.Hand
            };
            var grid = new Grid { Margin = new Thickness(16, 14, 16, 0) };
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
            grid.ColumnDefinitions.Add(new ColumnDefinition());
            grid.RowDefinitions.Add(new RowDefinition());
            grid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });

            var icon = new Grid { Width = 34, Height = 34, VerticalAlignment = VerticalAlignment.Top };
            icon.Children.Add(new Ellipse { Fill = danger ? Brushes.Black : Brushes.White, Stroke = Brushes.White, StrokeThickness = 2 });
            icon.Children.Add(new TextBlock
            {
                Text = glyph, FontSize = 16, FontWeight = FontWeights.Bold,
                Foreground = danger ? Brushes.White : Brushes.Black,
                HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center,
                FontFamily = new FontFamily("Segoe UI Symbol, Segoe UI")
            });
            grid.Children.Add(icon);

            var texts = new StackPanel { Margin = new Thickness(14, 0, 0, 14) };
            Grid.SetColumn(texts, 1);
            texts.Children.Add(new TextBlock { Text = title, Foreground = Brushes.White, FontSize = 14, FontWeight = FontWeights.SemiBold, TextTrimming = TextTrimming.CharacterEllipsis });
            texts.Children.Add(new TextBlock { Text = message, Foreground = new SolidColorBrush(Color.FromRgb(0xA8, 0xA8, 0xA8)), FontSize = 12.5, TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 3, 0, 0) });
            grid.Children.Add(texts);

            var bar = new Border
            {
                Height = 3, CornerRadius = new CornerRadius(2), Background = Brushes.White, Opacity = 0.9,
                Margin = new Thickness(-16, 0, -16, 0), RenderTransformOrigin = new Point(0, 0.5),
                RenderTransform = new ScaleTransform(1, 1)
            };
            Grid.SetRow(bar, 1); Grid.SetColumnSpan(bar, 2);
            grid.Children.Add(bar);

            var clip = new Border { CornerRadius = new CornerRadius(16), ClipToBounds = true, Child = grid };
            root.Child = clip;
            Content = root;

            var duration = TimeSpan.FromSeconds(danger ? 5.5 : 3.8);
            var slide = new TranslateTransform(60, 0);
            root.RenderTransform = slide;
            root.Opacity = 0;
            _root = root; _slide = slide;
            Loaded += (s, e) =>
            {
                Toast.Reposition();
                var ease = new CubicEase { EasingMode = EasingMode.EaseOut };
                slide.BeginAnimation(TranslateTransform.XProperty, new DoubleAnimation(60, 0, TimeSpan.FromMilliseconds(320)) { EasingFunction = ease });
                root.BeginAnimation(UIElement.OpacityProperty, new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(260)));
                ((ScaleTransform)bar.RenderTransform).BeginAnimation(ScaleTransform.ScaleXProperty, new DoubleAnimation(1, 0, duration));
            };
            SizeChanged += (s, e) => Toast.Reposition();
            MouseLeftButtonUp += (s, e) => FadeOut();

            _timer = new DispatcherTimer { Interval = duration };
            _timer.Tick += (s, e) => FadeOut();
            _timer.Start();
        }

        protected override void OnSourceInitialized(EventArgs e)
        {
            base.OnSourceInitialized(e);
            var hwnd = new WindowInteropHelper(this).Handle;
            Native.AddExStyle(hwnd, Native.WS_EX_TOOLWINDOW | Native.WS_EX_NOACTIVATE);
        }

        private void FadeOut()
        {
            if (_closing) return;
            _closing = true;
            _timer.Stop();
            var a = new DoubleAnimation(_root.Opacity, 0, TimeSpan.FromMilliseconds(220));
            a.Completed += (s, e) => Close();
            _root.BeginAnimation(UIElement.OpacityProperty, a);
            _slide.BeginAnimation(TranslateTransform.XProperty, new DoubleAnimation(0, 40, TimeSpan.FromMilliseconds(220)));
        }
    }
}
