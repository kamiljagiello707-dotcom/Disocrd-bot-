package dev.nova.client.util;

import net.minecraft.client.gui.DrawContext;

/** Small set of 2D drawing helpers built only on DrawContext.fill (no shaders needed). */
public final class Render2D {
    private Render2D() {}

    public static void rect(DrawContext ctx, double x, double y, double w, double h, int color) {
        ctx.fill((int) x, (int) y, (int) (x + w), (int) (y + h), color);
    }

    /** Rounded rectangle approximated per scanline in the corners. */
    public static void roundRect(DrawContext ctx, double x, double y, double w, double h, int r, int color) {
        int ix = (int) x, iy = (int) y, iw = (int) w, ih = (int) h;
        r = Math.min(r, Math.min(iw, ih) / 2);
        if (r <= 0) { ctx.fill(ix, iy, ix + iw, iy + ih, color); return; }
        ctx.fill(ix, iy + r, ix + iw, iy + ih - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            ctx.fill(ix + inset, iy + i, ix + iw - inset, iy + i + 1, color);
            ctx.fill(ix + inset, iy + ih - i - 1, ix + iw - inset, iy + ih - i, color);
        }
    }

    /** Rounded rect with only the top corners rounded. */
    public static void roundRectTop(DrawContext ctx, double x, double y, double w, double h, int r, int color) {
        int ix = (int) x, iy = (int) y, iw = (int) w, ih = (int) h;
        ctx.fill(ix, iy + r, ix + iw, iy + ih, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            ctx.fill(ix + inset, iy + i, ix + iw - inset, iy + i + 1, color);
        }
    }

    public static void outline(DrawContext ctx, double x, double y, double w, double h, int color) {
        int ix = (int) x, iy = (int) y, iw = (int) w, ih = (int) h;
        ctx.fill(ix, iy, ix + iw, iy + 1, color);
        ctx.fill(ix, iy + ih - 1, ix + iw, iy + ih, color);
        ctx.fill(ix, iy + 1, ix + 1, iy + ih - 1, color);
        ctx.fill(ix + iw - 1, iy + 1, ix + iw, iy + ih - 1, color);
    }

    /** Horizontal accent line that follows the theme wave. */
    public static void accentLine(DrawContext ctx, double x, double y, double w, double h, double alpha) {
        int step = 2;
        for (int i = 0; i < w; i += step) {
            int c = ColorUtil.alpha(Theme.accent(x + i), alpha);
            ctx.fill((int) x + i, (int) y, (int) Math.min(x + i + step, x + w), (int) (y + h), c);
        }
    }

    /** Soft drop shadow made of stacked translucent rounded rects. */
    public static void shadow(DrawContext ctx, double x, double y, double w, double h, int radius, double alpha) {
        for (int i = radius; i > 0; i -= 2) {
            int a = (int) (alpha * 22 * (1 - (double) i / (radius + 1)));
            roundRect(ctx, x - i, y - i, w + i * 2, h + i * 2, i + 3, a << 24);
        }
    }

    public static boolean hovered(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
