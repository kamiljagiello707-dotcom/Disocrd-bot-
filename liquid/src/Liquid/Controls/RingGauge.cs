using System;
using System.Globalization;
using System.Windows;
using System.Windows.Media;

namespace Liquid.Controls
{
    /// <summary>Okrągły wskaźnik (temperatura / obciążenie) rysowany wektorowo.</summary>
    public sealed class RingGauge : FrameworkElement
    {
        public static readonly DependencyProperty ValueProperty = DependencyProperty.Register(nameof(Value), typeof(double), typeof(RingGauge),
            new FrameworkPropertyMetadata(0.0, FrameworkPropertyMetadataOptions.AffectsRender));
        public static readonly DependencyProperty MaximumProperty = DependencyProperty.Register(nameof(Maximum), typeof(double), typeof(RingGauge),
            new FrameworkPropertyMetadata(100.0, FrameworkPropertyMetadataOptions.AffectsRender));
        public static readonly DependencyProperty TextProperty = DependencyProperty.Register(nameof(Text), typeof(string), typeof(RingGauge),
            new FrameworkPropertyMetadata("—", FrameworkPropertyMetadataOptions.AffectsRender));
        public static readonly DependencyProperty UnitProperty = DependencyProperty.Register(nameof(Unit), typeof(string), typeof(RingGauge),
            new FrameworkPropertyMetadata("", FrameworkPropertyMetadataOptions.AffectsRender));
        public static readonly DependencyProperty WarnProperty = DependencyProperty.Register(nameof(Warn), typeof(bool), typeof(RingGauge),
            new FrameworkPropertyMetadata(false, FrameworkPropertyMetadataOptions.AffectsRender));
        public static readonly DependencyProperty StrokeWidthProperty = DependencyProperty.Register(nameof(StrokeWidth), typeof(double), typeof(RingGauge),
            new FrameworkPropertyMetadata(10.0, FrameworkPropertyMetadataOptions.AffectsRender));

        public double Value { get => (double)GetValue(ValueProperty); set => SetValue(ValueProperty, value); }
        public double Maximum { get => (double)GetValue(MaximumProperty); set => SetValue(MaximumProperty, value); }
        public string Text { get => (string)GetValue(TextProperty); set => SetValue(TextProperty, value); }
        public string Unit { get => (string)GetValue(UnitProperty); set => SetValue(UnitProperty, value); }
        public bool Warn { get => (bool)GetValue(WarnProperty); set => SetValue(WarnProperty, value); }
        public double StrokeWidth { get => (double)GetValue(StrokeWidthProperty); set => SetValue(StrokeWidthProperty, value); }

        private static readonly Typeface Bold = new Typeface(new FontFamily("Segoe UI Variable Display, Segoe UI"), FontStyles.Normal, FontWeights.Bold, FontStretches.Normal);
        private static readonly Typeface Semi = new Typeface(new FontFamily("Segoe UI Variable Display, Segoe UI"), FontStyles.Normal, FontWeights.SemiBold, FontStretches.Normal);
        private static readonly Brush Track = Freeze(new SolidColorBrush(Color.FromRgb(0x24, 0x24, 0x24)));
        private static readonly Brush Danger = Freeze(new SolidColorBrush(Color.FromRgb(0xFF, 0x5A, 0x5A)));
        private static readonly Brush Sub = Freeze(new SolidColorBrush(Color.FromRgb(0x8C, 0x8C, 0x8C)));
        private static Brush Freeze(Brush b) { b.Freeze(); return b; }

        protected override Size MeasureOverride(Size available)
        {
            double s = Math.Min(double.IsInfinity(available.Width) ? 150 : available.Width, double.IsInfinity(available.Height) ? 150 : available.Height);
            return new Size(s, s);
        }

        protected override void OnRender(DrawingContext dc)
        {
            double size = Math.Min(ActualWidth, ActualHeight);
            if (size <= 0) return;
            double sw = StrokeWidth;
            var c = new Point(ActualWidth / 2, ActualHeight / 2);
            double r = size / 2 - sw / 2 - 1;
            const double start = 135, sweep = 270;

            var trackPen = new Pen(Track, sw) { StartLineCap = PenLineCap.Round, EndLineCap = PenLineCap.Round };
            dc.DrawGeometry(null, trackPen, Arc(c, r, start, sweep));

            double frac = Maximum > 0 ? Math.Max(0, Math.Min(1, Value / Maximum)) : 0;
            if (frac > 0.002)
            {
                var pen = new Pen(Warn ? Danger : Brushes.White, sw) { StartLineCap = PenLineCap.Round, EndLineCap = PenLineCap.Round };
                dc.DrawGeometry(null, pen, Arc(c, r, start, sweep * frac));
            }

            double dpi = VisualTreeHelper.GetDpi(this).PixelsPerDip;
            var main = new FormattedText(Text ?? "", CultureInfo.CurrentUICulture, FlowDirection.LeftToRight, Bold, size * 0.24, Warn ? Danger : Brushes.White, dpi);
            var unit = new FormattedText(Unit ?? "", CultureInfo.CurrentUICulture, FlowDirection.LeftToRight, Semi, size * 0.1, Sub, dpi);
            dc.DrawText(main, new Point(c.X - main.Width / 2, c.Y - main.Height / 2 - size * 0.03));
            dc.DrawText(unit, new Point(c.X - unit.Width / 2, c.Y + main.Height / 2 - size * 0.04));
        }

        private static Geometry Arc(Point c, double r, double startDeg, double sweepDeg)
        {
            if (sweepDeg >= 359.9) sweepDeg = 359.9;
            double a0 = startDeg * Math.PI / 180, a1 = (startDeg + sweepDeg) * Math.PI / 180;
            var p0 = new Point(c.X + r * Math.Cos(a0), c.Y + r * Math.Sin(a0));
            var p1 = new Point(c.X + r * Math.Cos(a1), c.Y + r * Math.Sin(a1));
            var g = new StreamGeometry();
            using (var ctx = g.Open())
            {
                ctx.BeginFigure(p0, false, false);
                ctx.ArcTo(p1, new Size(r, r), 0, sweepDeg > 180, SweepDirection.Clockwise, true, true);
            }
            g.Freeze();
            return g;
        }
    }
}
