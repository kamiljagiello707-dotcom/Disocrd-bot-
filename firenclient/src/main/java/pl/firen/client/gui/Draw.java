package pl.firen.client.gui;

import net.minecraft.client.gui.DrawContext;

public final class Draw {
    private Draw() {}

    /** Prostokąt z lekko zaokrąglonymi rogami (1 px). */
    public static void round(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        if (x2 - x1 < 3 || y2 - y1 < 3) { ctx.fill(x1, y1, x2, y2, color); return; }
        ctx.fill(x1 + 1, y1, x2 - 1, y1 + 1, color);
        ctx.fill(x1, y1 + 1, x2, y2 - 1, color);
        ctx.fill(x1 + 1, y2 - 1, x2 - 1, y2, color);
    }

    public static void outline(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        ctx.fill(x1 + 1, y1, x2 - 1, y1 + 1, color);
        ctx.fill(x1 + 1, y2 - 1, x2 - 1, y2, color);
        ctx.fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
        ctx.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
    }
}
