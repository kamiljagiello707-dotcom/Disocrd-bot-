package dev.wave.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/**
 * Drawing helpers. All shapes are rasterised at half-pixel resolution so rounded
 * corners stay smooth on any GUI scale. A global alpha multiplier makes fading whole
 * screens trivial.
 */
public final class RenderUtil {
    private RenderUtil() {}

    public static float globalAlpha = 1f;

    private static int a(int color) { return globalAlpha >= 1f ? color : ColorUtil.alpha(color, globalAlpha); }

    public static TextRenderer font() { return MinecraftClient.getInstance().textRenderer; }

    public static void rect(DrawContext c, float x, float y, float w, float h, int color) {
        if (w <= 0 || h <= 0) return;
        color = a(color);
        if (ColorUtil.getAlpha(color) == 0) return;
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(0.5f, 0.5f, 1f);
        c.fill(0, 0, Math.round(w * 2), Math.round(h * 2), color);
        m.pop();
    }

    public static void round(DrawContext c, float x, float y, float w, float h, float r, int color) {
        round(c, x, y, w, h, r, color, true, true);
    }

    public static void round(DrawContext c, float x, float y, float w, float h, float r, int color, boolean top, boolean bottom) {
        if (w <= 0 || h <= 0) return;
        color = a(color);
        if (ColorUtil.getAlpha(color) == 0) return;
        int W = Math.round(w * 2), H = Math.round(h * 2);
        int R = Math.max(0, Math.min(Math.round(r * 2), Math.min(W / 2, H / 2)));
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(0.5f, 0.5f, 1f);
        int topR = top ? R : 0, botR = bottom ? R : 0;
        for (int i = 0; i < topR; i++) {
            int inset = inset(R, i);
            c.fill(inset, i, W - inset, i + 1, color);
        }
        for (int i = 0; i < botR; i++) {
            int inset = inset(R, i);
            c.fill(inset, H - i - 1, W - inset, H - i, color);
        }
        c.fill(0, topR, W, H - botR, color);
        m.pop();
    }

    private static int inset(int R, int row) {
        double dy = R - row - 0.5;
        return (int) Math.round(R - Math.sqrt(Math.max(0, R * R - dy * dy)));
    }

    /** Rounded outline drawn as a ring (outer minus inner) row by row. */
    public static void roundOutline(DrawContext c, float x, float y, float w, float h, float r, float thickness, int color) {
        color = a(color);
        if (ColorUtil.getAlpha(color) == 0) return;
        int W = Math.round(w * 2), H = Math.round(h * 2), T = Math.max(1, Math.round(thickness * 2));
        int R = Math.max(0, Math.min(Math.round(r * 2), Math.min(W / 2, H / 2)));
        int Ri = Math.max(0, R - T);
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(0.5f, 0.5f, 1f);
        for (int row = 0; row < H; row++) {
            int distTop = Math.min(row, H - 1 - row);
            int outer = distTop < R ? inset(R, distTop) : 0;
            if (distTop < T) {
                c.fill(outer, row, W - outer, row + 1, color);
            } else {
                int innerRow = distTop - T;
                int inner = innerRow < Ri ? inset(Ri, innerRow) + T : T;
                c.fill(outer, row, inner, row + 1, color);
                c.fill(W - inner, row, W - outer, row + 1, color);
            }
        }
        m.pop();
    }

    public static void circle(DrawContext c, float cx, float cy, float r, int color) {
        color = a(color);
        if (ColorUtil.getAlpha(color) == 0) return;
        int R = Math.max(1, Math.round(r * 2));
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(cx - r, cy - r, 0);
        m.scale(0.5f, 0.5f, 1f);
        for (int i = 0; i < R * 2; i++) {
            double dy = R - i - 0.5;
            int dx = (int) Math.round(Math.sqrt(Math.max(0, R * R - dy * dy)));
            c.fill(R - dx, i, R + dx, i + 1, color);
        }
        m.pop();
    }

    public static void gradientH(DrawContext c, float x, float y, float w, float h, int left, int right) {
        int steps = Math.max(1, Math.round(w * 2));
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(0.5f, 0.5f, 1f);
        int H = Math.round(h * 2);
        for (int i = 0; i < steps; i++) {
            c.fill(i, 0, i + 1, H, a(ColorUtil.lerp(left, right, i / (float) steps)));
        }
        m.pop();
    }

    public static void gradientV(DrawContext c, float x, float y, float w, float h, int top, int bottom) {
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(x, y, 0);
        c.fillGradient(0, 0, Math.round(w), Math.round(h), a(top), a(bottom));
        m.pop();
    }

    // ---------------------------------------------------------------- text

    public static void text(DrawContext c, String s, float x, float y, int color, boolean shadow) {
        color = a(color);
        if (ColorUtil.getAlpha(color) < 6) return;
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(x, y, 0);
        c.drawText(font(), s, 0, 0, color, shadow);
        m.pop();
    }

    public static void text(DrawContext c, String s, float x, float y, int color) { text(c, s, x, y, color, false); }

    public static void text(DrawContext c, Text s, float x, float y, int color, boolean shadow) {
        color = a(color);
        if (ColorUtil.getAlpha(color) < 6) return;
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(x, y, 0);
        c.drawText(font(), s, 0, 0, color, shadow);
        m.pop();
    }

    public static void textScaled(DrawContext c, String s, float x, float y, float scale, int color, boolean shadow) {
        color = a(color);
        if (ColorUtil.getAlpha(color) < 6) return;
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        c.drawText(font(), s, 0, 0, color, shadow);
        m.pop();
    }

    public static void centered(DrawContext c, String s, float cx, float y, int color) {
        text(c, s, cx - width(s) / 2f, y, color, false);
    }

    public static int width(String s) { return font().getWidth(s); }

    public static boolean hovered(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public static void scissor(DrawContext c, float x, float y, float w, float h) {
        c.enableScissor((int) Math.floor(x), (int) Math.floor(y), (int) Math.ceil(x + w), (int) Math.ceil(y + h));
    }

    public static void endScissor(DrawContext c) { c.disableScissor(); }
}
