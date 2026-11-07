package dev.mustic.client.util;

import net.minecraft.client.gui.DrawContext;

public final class Render2D {
    private Render2D() {}

    public static void rect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + h, color);
    }

    public static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** Prostokat z "zaokraglonymi" (sciętymi) rogami 1px. */
    public static void softRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x + 1, y, x + w - 1, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** Poziomy gradient rysowany paskami. */
    public static void horizontalGradient(DrawContext ctx, int x, int y, int w, int h, int left, int right) {
        int steps = Math.max(1, w / 2);
        for (int i = 0; i < steps; i++) {
            int x1 = x + w * i / steps;
            int x2 = x + w * (i + 1) / steps;
            ctx.fill(x1, y, x2, y + h, ColorUtil.lerp(left, right, i / (float) Math.max(1, steps - 1)));
        }
    }

    /** Miekki cien pod panelem. */
    public static void shadow(DrawContext ctx, int x, int y, int w, int h, int size) {
        for (int i = 1; i <= size; i++) {
            int a = (int) (40f * (1f - i / (float) (size + 1)));
            outline(ctx, x - i, y - i, w + i * 2, h + i * 2, a << 24);
        }
    }

    public static boolean hovered(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
