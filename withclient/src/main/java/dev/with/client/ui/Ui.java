package dev.with.client.ui;

import dev.with.client.render.Fonts;
import dev.with.client.render.Fonts.Font;
import dev.with.client.render.R2D;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;

/** Biało-czarna paleta VescaClient i wspólne elementy (logo, napis, przyciski, klawisze). */
public final class Ui {
    private Ui() {}

    public static final int WHITE = 0xFFF5F5F6, TEXT = 0xFFF4F4F5, DIM = 0xFFA1A1AA, FAINT = 0xFF5E5E68, BLACK = 0xFF09090B, INK = 0xFF52525B;

    public static boolean pl() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc != null && mc.options != null && mc.options.language.startsWith("pl");
    }

    public static String t(String pl, String en) { return pl() ? pl : en; }

    /** logo: ciemny kafelek z biało-szarym znakiem "VV" WithClient i miękką poświatą */
    public static void logo(DrawContext ctx, float x, float y, float s, float glow) {
        if (glow > 0) R2D.glow(ctx, x - s * 0.5f, y - s * 0.5f, s * 2f, s * 2f, R2D.withAlpha(0xFFFFFFFF, 0.12f * glow));
        R2D.shadow(ctx, x, y + 1.5f, s, s, s * 0.4f, 0x99000000);
        R2D.roundRectV(ctx, x, y, s, s, s * 0.28f, 0xFF232328, 0xFF0B0B0D);
        R2D.roundOutline(ctx, x, y, s, s, s * 0.28f, 0.6f, 0x2EFFFFFF);
        R2D.icon(ctx, "with_logo", x + s * 0.14f, y + s * 0.14f, s * 0.72f, 0xFFFFFFFF);
    }

    /** tekst z rozstrzelonymi literami; zwraca szerokość */
    public static float spaced(DrawContext ctx, String s, float x, float y, int color, Font f, float scale, float spacing) {
        float x0 = x;
        for (char ch : s.toCharArray()) {
            String c = String.valueOf(ch);
            if (ctx != null) Fonts.draw(ctx, c, x, y, color, f, scale);
            x += Fonts.width(c, f, scale) + spacing;
        }
        return x - x0 - spacing;
    }

    public static float spacedWidth(String s, Font f, float scale, float spacing) { return spaced(null, s, 0, 0, 0, f, scale, spacing); }

    /** duży napis wyrównany do lewej; górna krawędź wielkich liter = topY */
    public static void display(DrawContext ctx, String s, float x, float topY, int color, float scale) {
        Fonts.drawDisplay(ctx, s, x + Fonts.width(s, Font.DISPLAY, scale) / 2f, topY, color, scale);
    }

    /** klawisz (np. ↑ ↓ Enter); zwraca szerokość */
    public static float keyCap(DrawContext ctx, String k, float x, float y) {
        float w = Math.max(12, Fonts.width(k, Font.BOLD, 0.52f) + 8);
        R2D.roundRect(ctx, x, y, w, 12, 3.5f, 0x1AFFFFFF);
        R2D.roundOutline(ctx, x, y, w, 12, 3.5f, 0.5f, 0x26FFFFFF);
        Fonts.drawV(ctx, k, x + (w - Fonts.width(k, Font.BOLD, 0.52f)) / 2f, y + 6.5f, DIM, Font.BOLD, 0.52f);
        return w;
    }

    /**
     * Pigułka-przycisk. primary = biała z czarnym tekstem, zwykła = ciemna z obwódką.
     * hover 0..1, enabled = false → przygaszony.
     */
    public static void button(DrawContext ctx, float x, float y, float w, float h, String icon, String label, boolean primary, float hover, boolean enabled) {
        float prev = R2D.alpha;
        if (!enabled) R2D.alpha = prev * 0.4f;
        float r = h / 2f;
        if (primary) {
            R2D.glow(ctx, x - 8, y - 6, w + 16, h + 12, R2D.withAlpha(0xFFFFFFFF, 0.10f + 0.12f * hover));
            R2D.roundRectV(ctx, x, y, w, h, r, R2D.blend(0xFFF4F4F5, 0xFFFFFFFF, hover), R2D.blend(0xFFDADADF, 0xFFF0F0F2, hover));
        } else {
            R2D.roundRect(ctx, x, y, w, h, r, R2D.blend(0x14FFFFFF, 0x2BFFFFFF, hover));
            R2D.roundOutline(ctx, x, y, w, h, r, 0.6f, R2D.blend(0x22FFFFFF, 0x66FFFFFF, hover));
        }
        int fg = primary ? BLACK : R2D.blend(0xFFD4D4D8, 0xFFFFFFFF, hover);
        boolean hasLabel = label != null && !label.isEmpty();
        float fs = h >= 21 ? 0.68f : 0.6f, is = Math.round(h * 0.46f);
        float tw = hasLabel ? Fonts.width(label, Font.BOLD, fs) : 0;
        float iw = icon != null ? is : 0, gap = icon != null && hasLabel ? 4 : 0;
        float cx = x + (w - tw - iw - gap) / 2f;
        if (icon != null) { R2D.icon(ctx, icon, cx, y + h / 2f - is / 2f, is, fg); cx += iw + gap; }
        if (hasLabel) Fonts.drawV(ctx, label, cx, y + h / 2f + 0.5f, fg, Font.BOLD, fs);
        R2D.alpha = prev;
    }

    /** kafelek z pierwszą literą nazwy (gdy serwer/świat nie ma ikony) */
    public static void monogram(DrawContext ctx, float x, float y, float s, String name) {
        R2D.roundRectV(ctx, x, y, s, s, s * 0.26f, 0xFF26262B, 0xFF18181B);
        R2D.roundOutline(ctx, x, y, s, s, s * 0.26f, 0.6f, 0x2AFFFFFF);
        String ch = name == null || name.isBlank() ? "?" : name.trim().substring(0, 1).toUpperCase();
        Fonts.drawCentered(ctx, ch, x + s / 2f, y + s / 2f - Fonts.height(1.05f) / 2f + 0.5f, TEXT, Font.BOLD, 1.05f);
    }

    /** głowa skina gracza */
    public static void head(DrawContext ctx, float x, float y, int size) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ctx.draw();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, R2D.alpha);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        PlayerSkinDrawer.draw(ctx, mc.getSkinProvider().getSkinTextures(mc.getGameProfile()), 0, 0, size);
        ctx.getMatrices().pop();
        ctx.draw();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    /** przełącznik (wł./wył.): on 0..1 (animowane), hover 0..1 */
    public static void toggle(DrawContext ctx, float x, float y, float on, float hover) {
        float w = 18, h = 10;
        int acc = Theme.accent();
        if (on > 0.01f) R2D.glow(ctx, x - 6, y - 6, w + 12, h + 12, R2D.withAlpha(acc, (Theme.mono() ? 0.12f : 0.25f) * on));
        R2D.roundRect(ctx, x, y, w, h, h / 2f, R2D.blend(R2D.blend(0x22FFFFFF, 0x34FFFFFF, hover), acc, on));
        if (on < 0.99f) R2D.roundOutline(ctx, x, y, w, h, h / 2f, 0.5f, R2D.withAlpha(0x22FFFFFF, 1 - on));
        float k = x + h / 2f + (w - h) * on;
        R2D.circle(ctx, k, y + h / 2f, 3.3f + 0.3f * hover, R2D.blend(0xFFA1A1AA, Theme.onAccent(), on));
    }
}
