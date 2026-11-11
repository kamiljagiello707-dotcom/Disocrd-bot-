package tech.fuzir.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

/**
 * Rysowanie w prawdziwych pikselach ekranu, niezależnie od skali GUI gry.
 * Współrzędne podaje się w jednostkach; jedna jednostka to {@link #u} pikseli.
 * Kształty rysuje shader SDF (gładkie krawędzie, cienie), tekst to czcionki TTF.
 */
public final class Canvas {
    private static final ShaderProgramKey ROUNDED = new ShaderProgramKey(
            Identifier.of("fuzir-client", "core/rounded"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

    public final DrawContext ctx;
    public final float u;
    public final TextRenderer font;
    /** Krycie wszystkiego, co jest rysowane (np. animacja otwierania). */
    public float alpha = 1f;
    private final int oversample;

    private Canvas(DrawContext ctx, float u) {
        this.ctx = ctx;
        this.u = u;
        this.font = MinecraftClient.getInstance().textRenderer;
        this.oversample = Math.max(1, Math.min(4, Math.round(u)));
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

    // ---------- kształty (shader) ----------

    private void shape(float x, float y, float w, float h, float radius, float soft, float thickness, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        top = c(top);
        bottom = c(bottom);
        if (((top >>> 24) & 0xFF) == 0 && ((bottom >>> 24) & 0xFF) == 0) return;
        // najpierw wysyłamy wszystko, co DrawContext trzyma w buforze, żeby zachować kolejność
        ctx.draw();
        float x0 = x * u, y0 = y * u, pw = w * u, ph = h * u;
        float pad = soft + 2f;
        float qx0 = x0 - pad, qy0 = y0 - pad, qx1 = x0 + pw + pad, qy1 = y0 + ph + pad;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ShaderProgram sh = RenderSystem.setShader(ROUNDED);
        if (sh == null) return;
        sh.getUniform("Shape").set(qx1 - qx0, qy1 - qy0, pw / 2f, ph / 2f);
        sh.getUniform("Style").set(radius * u, soft, thickness, 0f);

        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        b.vertex(m, qx0, qy0, 0).texture(0, 0).color(top);
        b.vertex(m, qx0, qy1, 0).texture(0, 1).color(bottom);
        b.vertex(m, qx1, qy1, 0).texture(1, 1).color(bottom);
        b.vertex(m, qx1, qy0, 0).texture(1, 0).color(top);
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    public void rect(float x, float y, float w, float h, int color) {
        shape(x, y, w, h, 0, 0.55f, 0, color, color);
    }

    public void round(float x, float y, float w, float h, float r, int color) {
        shape(x, y, w, h, r, 0.55f, 0, color, color);
    }

    /** Pionowy gradient z zaokrąglonymi rogami. */
    public void roundGradient(float x, float y, float w, float h, float r, int top, int bottom) {
        shape(x, y, w, h, r, 0.55f, 0, top, bottom);
    }

    /** Obramowanie o grubości podanej w pikselach. */
    public void outline(float x, float y, float w, float h, float r, float thicknessPx, int color) {
        shape(x, y, w, h, r, 0.55f, thicknessPx, color, color);
    }

    public void roundBorder(float x, float y, float w, float h, float r, int fill, int border) {
        round(x, y, w, h, r, fill);
        outline(x, y, w, h, r, 1f, border);
    }

    /** Miękki cień; {@code blur} w jednostkach. */
    public void shadow(float x, float y, float w, float h, float r, float blur, int color) {
        shape(x, y, w, h, r, blur * u, 0, color, color);
    }

    public void circle(float cx, float cy, float r, int color) {
        round(cx - r, cy - r, r * 2, r * 2, r, color);
    }

    /** Linia o grubości jednego piksela. */
    public void hline(float x, float y, float w, int color) {
        rect(x, y, w, 1f / u, color);
    }

    // ---------- tekst i ikony ----------

    public enum F {
        SMALL("small", 7, 5.2f), REGULAR("regular", 9, 6.6f), BOLD("bold", 9, 6.6f), TITLE("title", 18, 13.2f),
        ICON_SM("icons_sm", 8, 8), ICON("icons", 10, 10), ICON_LG("icons_lg", 14, 14);

        final String id;
        public final float size;
        /** Wysokość wielkich liter (do centrowania w pionie). */
        public final float cap;

        F(String id, float size, float cap) {
            this.id = id;
            this.size = size;
            this.cap = cap;
        }
    }

    private static final float BASELINE = 6.8f;

    private Identifier fontId(F f) {
        return Identifier.of("fuzir-client", f.id + "_" + oversample);
    }

    private Text styled(String s, F f) {
        return Text.literal(s).setStyle(Style.EMPTY.withFont(fontId(f)));
    }

    public float width(String s, F f) {
        return font.getTextHandler().getWidth(styled(s, f));
    }

    /** Tekst; {@code y} to górna krawędź wielkich liter. */
    public void text(String s, float x, float y, int color, F f) {
        // gra stawia linię bazową czcionek TTF ok. 6,8 jednostki pod punktem rysowania
        drawText(styled(s, f), x, y + f.cap - BASELINE, color);
    }

    /** Tekst wyśrodkowany w pionie względem {@code cy}. */
    public void textMid(String s, float x, float cy, int color, F f) {
        text(s, x, cy - f.cap / 2f, color, f);
    }

    public void textRight(String s, float right, float cy, int color, F f) {
        textMid(s, right - width(s, f), cy, color, f);
    }

    public void textCenter(String s, float cx, float cy, int color, F f) {
        textMid(s, cx - width(s, f) / 2f, cy, color, f);
    }

    /** Ikona Lucide wyśrodkowana w punkcie (cx, cy). */
    public void icon(char icon, float cx, float cy, int color, F f) {
        String s = String.valueOf(icon);
        float w = width(s, f);
        // środek ikon Lucide wypada ok. 0,49 rozmiaru nad linią bazową
        drawText(styled(s, f), cx - w / 2f, cy - BASELINE + f.size * 0.49f, color);
    }

    private void drawText(Text t, float x, float y, int color) {
        int col = c(color);
        // przy alfie < 4 gra rysuje tekst w pełni nieprzezroczysty, więc go pomijamy
        if (((col >>> 24) & 0xFF) < 5) return;
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(x * u, y * u, 0f);
        m.scale(u, u, 1f);
        ctx.drawText(font, t, 0, 0, col, false);
        m.pop();
    }

    public String trim(String s, float maxW, F f) {
        if (width(s, f) <= maxW) return s;
        int end = s.length();
        while (end > 0 && width(s.substring(0, end) + "...", f) > maxW) end--;
        return s.substring(0, end).stripTrailing() + "...";
    }

    /** Dzieli tekst na linie mieszczące się w podanej szerokości. */
    public java.util.List<String> wrap(String s, float maxW, F f) {
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (width(test, f) > maxW && !line.isEmpty()) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }

    // ---------- przycinanie ----------

    public void scissor(float x, float y, float w, float h) {
        ctx.draw();
        // enableScissor przelicza prostokąt przez aktualną macierz, więc podajemy piksele
        ctx.enableScissor(px(x), px(y), px(x + w), px(y + h));
    }

    public void endScissor() {
        ctx.draw();
        ctx.disableScissor();
    }
}
