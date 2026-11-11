package tech.fuzir.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Rysowanie w prawdziwych pikselach ekranu, niezależnie od skali GUI gry.
 * Współrzędne podaje się w "jednostkach"; jedna jednostka to {@link #u} pikseli.
 * Dzięki temu zaokrąglenia rogów są gładkie, a nie schodkowe.
 */
public final class Canvas {
    public final DrawContext ctx;
    public final float u;
    public final TextRenderer font;
    /** Krycie wszystkiego, co jest rysowane (animacja otwierania). */
    public float alpha = 1f;

    private Canvas(DrawContext ctx, float u) {
        this.ctx = ctx;
        this.u = u;
        this.font = MinecraftClient.getInstance().textRenderer;
    }

    public static Canvas begin(DrawContext ctx, float u) {
        double sf = MinecraftClient.getInstance().getWindow().getScaleFactor();
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.scale((float) (1.0 / sf), (float) (1.0 / sf), 1f);
        return new Canvas(ctx, u);
    }

    public void end() {
        ctx.getMatrices().pop();
    }

    /** Szerokość ekranu w jednostkach. */
    public float width() {
        return MinecraftClient.getInstance().getWindow().getFramebufferWidth() / u;
    }

    public float height() {
        return MinecraftClient.getInstance().getWindow().getFramebufferHeight() / u;
    }

    public int px(float v) {
        return Math.round(v * u);
    }

    // ---------- kolory ----------

    public static int argb(int a, int rgb) {
        return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF);
    }

    public static int mulAlpha(int color, float k) {
        int a = (color >>> 24) & 0xFF;
        return argb(Math.round(a * Math.max(0f, Math.min(1f, k))), color);
    }

    /** Liniowe przejście między dwoma kolorami ARGB. */
    public static int mix(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((c1 >>> 24) & 255) + (((c2 >>> 24) & 255) - ((c1 >>> 24) & 255)) * t);
        int r = (int) (((c1 >> 16) & 255) + (((c2 >> 16) & 255) - ((c1 >> 16) & 255)) * t);
        int g = (int) (((c1 >> 8) & 255) + (((c2 >> 8) & 255) - ((c1 >> 8) & 255)) * t);
        int b = (int) ((c1 & 255) + ((c2 & 255) - (c1 & 255)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int c(int color) {
        return alpha >= 1f ? color : mulAlpha(color, alpha);
    }

    // ---------- kształty ----------

    public void rect(float x, float y, float w, float h, int color) {
        ctx.fill(px(x), px(y), px(x + w), px(y + h), c(color));
    }

    public void round(float x, float y, float w, float h, float r, int color) {
        roundPx(px(x), px(y), px(x + w), px(y + h), Math.round(r * u), c(color));
    }

    /** Zaokrąglony prostokąt z obramowaniem o grubości 1 piksela. */
    public void roundBorder(float x, float y, float w, float h, float r, int fill, int border) {
        int x0 = px(x), y0 = px(y), x1 = px(x + w), y1 = px(y + h), rr = Math.round(r * u);
        roundPx(x0, y0, x1, y1, rr, c(border));
        roundPx(x0 + 1, y0 + 1, x1 - 1, y1 - 1, Math.max(0, rr - 1), c(fill));
    }

    public void circle(float cx, float cy, float r, int color) {
        round(cx - r, cy - r, r * 2, r * 2, r, color);
    }

    private void roundPx(int x0, int y0, int x1, int y1, int r, int color) {
        if (x1 <= x0 || y1 <= y0 || ((color >>> 24) & 0xFF) == 0) return;
        r = Math.min(r, Math.min((x1 - x0) / 2, (y1 - y0) / 2));
        if (r <= 0) {
            ctx.fill(x0, y0, x1, y1, color);
            return;
        }
        ctx.fill(x0, y0 + r, x1, y1 - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            double inset = r - Math.sqrt(Math.max(0, r * r - dy * dy));
            int full = (int) Math.ceil(inset);
            float cover = (float) (full - inset);
            int top = y0 + i, bottom = y1 - i - 1;
            ctx.fill(x0 + full, top, x1 - full, top + 1, color);
            ctx.fill(x0 + full, bottom, x1 - full, bottom + 1, color);
            if (full > 0 && cover > 0.03f) {
                // półprzezroczysty piksel na krawędzi wygładza łuk
                int soft = mulAlpha(color, cover);
                ctx.fill(x0 + full - 1, top, x0 + full, top + 1, soft);
                ctx.fill(x1 - full, top, x1 - full + 1, top + 1, soft);
                ctx.fill(x0 + full - 1, bottom, x0 + full, bottom + 1, soft);
                ctx.fill(x1 - full, bottom, x1 - full + 1, bottom + 1, soft);
            }
        }
    }

    /** Pionowy gradient. */
    public void gradient(float x, float y, float w, float h, int top, int bottom) {
        ctx.fillGradient(px(x), px(y), px(x + w), px(y + h), c(top), c(bottom));
    }

    // ---------- tekst ----------
    // Czcionka Manrope (TTF) w kilku rozmiarach. Matryca skaluje tylko o u,
    // a wielkość liter wynika z rozmiaru czcionki, więc glify są ostre 1:1.

    private static final Identifier SMALL = Identifier.of("fuzir-client", "small");
    private static final Identifier REGULAR = Identifier.of("fuzir-client", "regular");
    private static final Identifier BOLD = Identifier.of("fuzir-client", "bold");
    private static final Identifier TITLE = Identifier.of("fuzir-client", "title");

    private static Identifier fontFor(float size, boolean bold) {
        if (size >= 1.5f) return TITLE;
        if (size <= 0.6f) return SMALL;
        return bold ? BOLD : REGULAR;
    }

    private static Text styled(String s, float size, boolean bold) {
        return Text.literal(s).setStyle(Style.EMPTY.withFont(fontFor(size, bold)));
    }

    public float textWidth(String s, float size) {
        return font.getWidth(styled(s, size, false));
    }

    public float textWidthBold(String s, float size) {
        return font.getWidth(styled(s, size, true));
    }

    public float textHeight(float size) {
        return size >= 1.5f ? 16f : size <= 0.6f ? 6f : 8f;
    }

    public void text(String s, float x, float y, int color, float size) {
        drawText(styled(s, size, false), x, y, color, false);
    }

    public void textBold(String s, float x, float y, int color, float size) {
        drawText(styled(s, size, true), x, y, color, false);
    }

    public void textShadow(String s, float x, float y, int color, float size) {
        drawText(styled(s, size, false), x, y, color, true);
    }

    private void drawText(Text t, float x, float y, int color, boolean shadow) {
        int col = c(color);
        // przy alfie < 4 gra rysuje tekst w pełni nieprzezroczysty, więc go pomijamy
        if (((col >>> 24) & 0xFF) < 5) return;
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(px(x), px(y), 0f);
        m.scale(u, u, 1f);
        ctx.drawText(font, t, 0, 0, col, shadow);
        m.pop();
    }

    /** Skraca tekst z "..." tak, żeby zmieścił się w podanej szerokości. */
    public String trim(String s, float maxW, float size) {
        if (textWidth(s, size) <= maxW) return s;
        String dots = "...";
        int end = s.length();
        while (end > 0 && textWidth(s.substring(0, end) + dots, size) > maxW) end--;
        return s.substring(0, end) + dots;
    }

    // ---------- przycinanie ----------

    public void scissor(float x, float y, float w, float h) {
        // enableScissor przelicza prostokąt przez aktualną macierz, więc podajemy piksele
        ctx.enableScissor(px(x), px(y), px(x + w), px(y + h));
    }

    public void endScissor() {
        ctx.disableScissor();
    }
}
