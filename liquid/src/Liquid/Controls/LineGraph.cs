using System;
using System.Globalization;
using System.Windows;
using System.Windows.Media;

namespace Liquid.Controls
{
    /// <summary>Wykres liniowy (frametime / historia temperatur).</summary>
    public sealed class LineGraph : FrameworkElement
    {
        private double[] _data = Array.Empty<double>();
        public string UnitSuffix { get; set; } = " ms";
        public double MinScale { get; set; } = 20;

        private static readonly Pen Grid = MakePen(Color.FromRgb(0x22, 0x22, 0x22), 1);
        private static readonly Pen Line = MakePen(Colors.White, 1.6);
        private static readonly Brush Fill;
        private static readonly Brush Sub;
        private static readonly Typeface Face = new Typeface("Segoe UI");

        static LineGraph()
        {
            var lg = new LinearGradientBrush(Color.FromArgb(0x50, 0xFF, 0xFF, 0xFF), Color.FromArgb(0x00, 0xFF, 0xFF, 0xFF), 90);
            lg.Freeze(); Fill = lg;
            var s = new SolidColorBrush(Color.FromRgb(0x6A, 0x6A, 0x6A)); s.Freeze(); Sub = s;
        }

        private static Pen MakePen(Color c, double w) { var p = new Pen(new SolidColorBrush(c), w) { LineJoin = PenLineJoin.Round }; p.Freeze(); return p; }

        public void SetData(double[] data)
        {
            _data = data ?? Array.Empty<double>();
            InvalidateVisual();
        }

        protected override void OnRender(DrawingContext dc)
        {
            double w = ActualWidth, h = ActualHeight;
            if (w <= 0 || h <= 0) return;
            dc.DrawRectangle(Brushes.Transparent, null, new Rect(0, 0, w, h));

            double max = MinScale;
            foreach (var d in _data) if (d > max) max = d;
            max = Math.Ceiling(max / 5) * 5;
            double dpi = VisualTreeHelper.GetDpi(this).PixelsPerDip;

            for (int i = 0; i <= 4; i++)
            {
                double y = Math.Round(h * i / 4) + 0.5;
                dc.DrawLine(Grid, new Point(0, y), new Point(w, y));
                var ft = new FormattedText($"{max * (4 - i) / 4:0}{UnitSuffix}", CultureInfo.InvariantCulture, FlowDirection.LeftToRight, Face, 10, Sub, dpi);
                dc.DrawText(ft, new Point(4, Math.Min(h - ft.Height, y + 2)));
            }
            if (_data.Length < 2) return;

            var g = new StreamGeometry();
            var fill = new StreamGeometry();
            double step = w / (_data.Length - 1);
            using (var ctx = g.Open())
            using (var fctx = fill.Open())
            {
                for (int i = 0; i < _data.Length; i++)
                {
                    var p = new Point(i * step, h - Math.Min(1, _data[i] / max) * (h - 4) - 2);
                    if (i == 0) { ctx.BeginFigure(p, false, false); fctx.BeginFigure(new Point(0, h), true, true); fctx.LineTo(p, false, false); }
                    else { ctx.LineTo(p, true, true); fctx.LineTo(p, false, false); }
                }
                fctx.LineTo(new Point(w, h), false, false);
            }
            g.Freeze(); fill.Freeze();
            dc.DrawGeometry(Fill, null, fill);
            dc.DrawGeometry(null, Line, g);
        }
    }
}
