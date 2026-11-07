package pl.mysticclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Nowoczesny renderer interfejsu.
 * <p>
 * Wszystko rysujemy w jednostkach "dp" (jak px w CSS). 1 dp = {@link #u} prawdziwych pikseli ekranu.
 * Kształty są wielokątami z wygładzonymi krawędziami (miękkie przejście 1px), a tekst używa czcionki
 * Inter rastrowanej dokładnie w docelowym rozmiarze w pikselach, więc jest ostry przy każdej skali GUI.
 */
public final class UI {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public enum Weight { REGULAR("regular"), MEDIUM("medium"), SEMIBOLD("semibold");
        final String id;
        Weight(String id) { this.id = id; }
    }

    // metryki Inter (hhea): (ascent + descent) / em, ascent / em
    private static final float INTER_LINE = 2478f / 2048f;
    private static final float INTER_ASC = 1984f / 2048f;
    /** Linia bazowa glifów TTF w Minecrafcie leży 7 jednostek pod punktem rysowania (jak w czcionce bitmapowej). */
    private static final float MC_BASELINE = 7f;

    private static DrawContext ctx;
    /** ile pikseli ekranu przypada na 1 dp */
    public static float u = 1f;
    private static float gs = 1f;
    /** globalna przezroczystość (animacje otwierania) */
    public static float alpha = 1f;

    private static final Map<String, Identifier> FONT_CACHE = new HashMap<>();
    private static final Map<String, Float> ANIMS = new HashMap<>();

    private UI() {}

    // ------------------------------------------------------------------ cykl

    public static void begin(DrawContext context, float scale) {
        ctx = context;
        gs = (float) mc.getWindow().getScaleFactor();
        u = scale;
        ctx.getMatrices().push();
        ctx.getMatrices().scale(u / gs, u / gs, 1f);
    }

    public static void end() {
        ctx.getMatrices().pop();
        alpha = 1f;
    }

    /** Domyślna skala: 1.0 dla 1080p, proporcjonalnie dla innych rozdzielczości. */
    public static float autoScale() {
        float h = mc.getWindow().getFramebufferHeight();
        float w = mc.getWindow().getFramebufferWidth();
        return Math.max(0.6f, Math.min(2.5f, Math.min(h / 1080f, w / 1920f)));
    }

    public static float screenW() { return mc.getWindow().getFramebufferWidth() / u; }
    public static float screenH() { return mc.getWindow().getFramebufferHeight() / u; }

    /** Zamiana współrzędnych myszy z GUI Minecrafta na dp. */
    public static float dp(double guiCoord) { return (float) (guiCoord * gs / u); }

    public static DrawContext ctx() { return ctx; }

    // ------------------------------------------------------------------ animacje

    public static float approach(float current, float target, float speed) {
        float dt = Math.min(0.1f, Frame.delta());
        float f = 1f - (float) Math.exp(-speed * dt);
        float r = current + (target - current) * f;
        return Math.abs(r - target) < 0.002f ? target : r;
    }

    /** Animacja przypisana do klucza – wygodne dla hoverów. */
    public static float anim(String key, float target, float speed) {
        float v = approach(ANIMS.getOrDefault(key, target), target, speed);
        ANIMS.put(key, v);
        return v;
    }

    public static float anim(String key, boolean on) { return anim(key, on ? 1f : 0f, 16f); }

    public static final class Frame {
        private static long last = System.nanoTime();
        private static float delta = 0.016f;
        private static long frameId = -1;

        public static void tick() {
            long now = System.nanoTime();
            delta = (now - last) / 1_000_000_000f;
            last = now;
            frameId++;
        }

        public static float delta() { return delta; }
    }

    // ------------------------------------------------------------------ kolory

    public static int col(int c) {
        if (alpha >= 1f) return c;
        return ((int) (((c >>> 24) & 0xFF) * alpha) << 24) | (c & 0xFFFFFF);
    }

    public static int a(int c, float mul) {
        return ((int) Math.max(0, Math.min(255, ((c >>> 24) & 0xFF) * mul)) << 24) | (c & 0xFFFFFF);
    }

    public static int withAlpha(int c, int alpha) { return (Math.max(0, Math.min(255, alpha)) << 24) | (c & 0xFFFFFF); }

    public static int mix(int c1, int c2, float t) {
        t = Math.max(0, Math.min(1, t));
        int a = (int) (((c1 >>> 24) & 0xFF) + ((((c2 >>> 24) & 0xFF) - ((c1 >>> 24) & 0xFF)) * t));
        int r = (int) (((c1 >> 16) & 0xFF) + ((((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t));
        int g = (int) (((c1 >> 8) & 0xFF) + ((((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t));
        int b = (int) ((c1 & 0xFF) + (((c2 & 0xFF) - (c1 & 0xFF)) * t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // ------------------------------------------------------------------ kształty

    private static float px() { return 1f / u; }

    public static void rect(float x, float y, float w, float h, int color) {
        round(x, y, w, h, 0, color);
    }

    public static void round(float x, float y, float w, float h, float r, int color) {
        if (w <= 0 || h <= 0 || ((color >>> 24) & 0xFF) == 0) return;
        int c = col(color);
        float half = px() * 0.5f;
        fill(x, y, w, h, r, c, half);
        ring(x, y, w, h, r, half, c, -half, c & 0xFFFFFF);
    }

    public static void circle(float cx, float cy, float radius, int color) {
        round(cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
    }

    /** Obramowanie o grubości t (dp). */
    public static void outline(float x, float y, float w, float h, float r, float t, int color) {
        if (w <= 0 || h <= 0 || ((color >>> 24) & 0xFF) == 0) return;
        int c = col(color);
        float half = px() * 0.5f;
        float t2 = Math.max(t, px());
        ring(x, y, w, h, r, half, c, -half, c & 0xFFFFFF);
        ring(x, y, w, h, r, t2 - half, c, half, c);
        ring(x, y, w, h, r, t2 + half, c & 0xFFFFFF, t2 - half, c);
    }

    /** Miękki cień pod prostokątem. */
    public static void shadow(float x, float y, float w, float h, float r, float spread, int color) {
        int c = col(color);
        fill(x, y, w, h, r, c, 0);
        ring(x, y, w, h, r, 0, c, -spread, c & 0xFFFFFF);
    }

    /** Poziomy gradient w prostokącie z zaokrągleniem. */
    public static void gradientH(float x, float y, float w, float h, int left, int right) {
        int l = col(left), rr = col(right);
        ctx.draw(vcp -> {
            VertexConsumer vc = vcp.getBuffer(RenderLayer.getGui());
            Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
            quad(vc, m, x, y, l, x, y + h, l, x + w, y + h, rr, x + w, y, rr);
        });
    }

    public static void gradientV(float x, float y, float w, float h, int top, int bottom) {
        int t = col(top), b = col(bottom);
        ctx.draw(vcp -> {
            VertexConsumer vc = vcp.getBuffer(RenderLayer.getGui());
            Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
            quad(vc, m, x, y, t, x, y + h, b, x + w, y + h, b, x + w, y, t);
        });
    }

    // geometria zaokrąglonego prostokąta: punkty na łukach z normalnymi
    private static float[] shape(float x, float y, float w, float h, float r) {
        r = Math.max(0.01f, Math.min(r, Math.min(w, h) / 2f));
        int seg = Math.max(2, Math.min(18, (int) Math.ceil(r * u / 1.5f)));
        float[] out = new float[(seg + 1) * 4 * 4 + 1];
        float[][] centers = {{x + r, y + r}, {x + w - r, y + r}, {x + w - r, y + h - r}, {x + r, y + h - r}};
        double[] start = {Math.PI, Math.PI * 1.5, 0, Math.PI * 0.5};
        int i = 0;
        for (int c = 0; c < 4; c++) {
            for (int s = 0; s <= seg; s++) {
                double ang = start[c] + (Math.PI / 2) * s / seg;
                float nx = (float) Math.cos(ang), ny = (float) Math.sin(ang);
                out[i++] = centers[c][0];
                out[i++] = centers[c][1];
                out[i++] = nx;
                out[i++] = ny;
            }
        }
        out[i] = r; // ostatni element = faktyczny promień
        return out;
    }

    private static void fill(float x, float y, float w, float h, float r, int color, float inset) {
        float[] p = shape(x, y, w, h, r);
        float rad = p[p.length - 1];
        float d = rad - inset;
        float cx = x + w / 2f, cy = y + h / 2f;
        int n = (p.length - 1) / 4;
        ctx.draw(vcp -> {
            VertexConsumer vc = vcp.getBuffer(RenderLayer.getGui());
            Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                float ax = p[i * 4] + p[i * 4 + 2] * d, ay = p[i * 4 + 1] + p[i * 4 + 3] * d;
                float bx = p[j * 4] + p[j * 4 + 2] * d, by = p[j * 4 + 1] + p[j * 4 + 3] * d;
                quad(vc, m, cx, cy, color, ax, ay, color, bx, by, color, bx, by, color);
            }
        });
    }

    /** Pierścień między wcięciem dA (kolor cA) a dB (kolor cB). Ujemne wcięcie = na zewnątrz. */
    private static void ring(float x, float y, float w, float h, float r, float dA, int cA, float dB, int cB) {
        float[] p = shape(x, y, w, h, r);
        float rad = p[p.length - 1];
        float ra = rad - dA, rb = rad - dB;
        int n = (p.length - 1) / 4;
        ctx.draw(vcp -> {
            VertexConsumer vc = vcp.getBuffer(RenderLayer.getGui());
            Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                float ax = p[i * 4] + p[i * 4 + 2] * ra, ay = p[i * 4 + 1] + p[i * 4 + 3] * ra;
                float bx = p[j * 4] + p[j * 4 + 2] * ra, by = p[j * 4 + 1] + p[j * 4 + 3] * ra;
                float cx = p[j * 4] + p[j * 4 + 2] * rb, cy = p[j * 4 + 1] + p[j * 4 + 3] * rb;
                float dx = p[i * 4] + p[i * 4 + 2] * rb, dy = p[i * 4 + 1] + p[i * 4 + 3] * rb;
                quad(vc, m, ax, ay, cA, bx, by, cA, cx, cy, cB, dx, dy, cB);
            }
        });
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                             float x1, float y1, int c1, float x2, float y2, int c2,
                             float x3, float y3, int c3, float x4, float y4, int c4) {
        // ta sama orientacja co DrawContext.fill (na wypadek włączonego cullingu)
        float area = (x1 * y2 - x2 * y1) + (x2 * y3 - x3 * y2) + (x3 * y4 - x4 * y3) + (x4 * y1 - x1 * y4);
        if (area > 0) {
            vc.vertex(m, x4, y4, 0).color(c4);
            vc.vertex(m, x3, y3, 0).color(c3);
            vc.vertex(m, x2, y2, 0).color(c2);
            vc.vertex(m, x1, y1, 0).color(c1);
        } else {
            vc.vertex(m, x1, y1, 0).color(c1);
            vc.vertex(m, x2, y2, 0).color(c2);
            vc.vertex(m, x3, y3, 0).color(c3);
            vc.vertex(m, x4, y4, 0).color(c4);
        }
    }

    // ------------------------------------------------------------------ tekst

    private static TextRenderer tr() { return mc.textRenderer; }

    private static Identifier font(String name, int px) {
        String key = name + "_" + px;
        return FONT_CACHE.computeIfAbsent(key, k -> Identifier.of("mysticclient", k));
    }

    private static int interPx(float size) {
        return Math.max(6, Math.min(72, Math.round(size * INTER_LINE * u)));
    }

    private static Text styled(String s, Identifier font) {
        return Text.literal(s).setStyle(Style.EMPTY.withFont(font));
    }

    /**
     * Rysuje tekst. (x, y) = lewy górny róg linii o wysokości {@link #lineHeight(float)}.
     * @param size rozmiar czcionki jak w CSS (dp)
     */
    public static void text(String s, float x, float y, float size, Weight weight, int color) {
        int c = col(color);
        if (((c >>> 24) & 0xFF) < 6 || s.isEmpty()) return;
        int px = interPx(size);
        float lineH = px; // w pikselach
        float baselinePx = lineH * (INTER_ASC / INTER_LINE);
        drawRaw(styled(s, font(weight.id, px)), x, y, baselinePx, c);
    }

    public static void text(String s, float x, float y, float size, int color) {
        text(s, x, y, size, Weight.REGULAR, color);
    }

    /** Tekst wyśrodkowany w pionie względem cy. */
    public static void textMid(String s, float x, float cy, float size, Weight weight, int color) {
        text(s, x, cy - capCenter(size), size, weight, color);
    }

    public static void textCenter(String s, float cx, float cy, float size, Weight weight, int color) {
        text(s, cx - width(s, size, weight) / 2f, cy - capCenter(size), size, weight, color);
    }

    public static void textRight(String s, float rx, float cy, float size, Weight weight, int color) {
        text(s, rx - width(s, size, weight), cy - capCenter(size), size, weight, color);
    }

    /** Odległość od góry linii do środka wysokości wersalików – do centrowania w pionie. */
    private static float capCenter(float size) {
        float asc = size * INTER_ASC;
        float cap = size * (1490f / 2048f);
        return asc - cap / 2f;
    }

    public static float width(String s, float size, Weight weight) {
        if (s.isEmpty()) return 0;
        int px = interPx(size);
        return tr().getWidth(styled(s, font(weight.id, px))) / u;
    }

    public static float width(String s, float size) { return width(s, size, Weight.REGULAR); }

    public static float lineHeight(float size) { return size * INTER_LINE; }

    /** Przycina tekst z wielokropkiem do maksymalnej szerokości. */
    public static String ellipsis(String s, float maxW, float size, Weight weight) {
        if (width(s, size, weight) <= maxW) return s;
        String e = "…";
        int end = s.length();
        while (end > 0 && width(s.substring(0, end) + e, size, weight) > maxW) end--;
        return s.substring(0, end).trim() + e;
    }

    /** Ikona Lucide wyśrodkowana w (cx, cy). */
    public static void icon(char code, float cx, float cy, float size, int color) {
        int c = col(color);
        if (((c >>> 24) & 0xFF) < 6) return;
        int px = Math.max(6, Math.min(72, Math.round(size * u)));
        Text t = styled(String.valueOf(code), font("icons", px));
        float wPx = tr().getWidth(t);
        float x = cx - wPx / 2f / u;
        float y = cy - px / 2f / u;
        drawRaw(t, x, y, px, c);
    }

    private static void drawRaw(Text t, float x, float y, float baselinePx, int color) {
        var ms = ctx.getMatrices();
        ms.push();
        // przyciągnięcie do pełnego piksela = ostry tekst
        float rx = Math.round(x * u) / u;
        float ry = Math.round(y * u) / u;
        ms.translate(rx, ry, 0);
        ms.scale(1f / u, 1f / u, 1f);
        int yOff = Math.round(baselinePx - MC_BASELINE);
        ctx.drawText(tr(), t, 0, yOff, color, false);
        ms.pop();
    }

    // ------------------------------------------------------------------ scissor

    public static void scissor(float x, float y, float w, float h) {
        ctx.enableScissor((int) Math.floor(x), (int) Math.floor(y), (int) Math.ceil(x + w), (int) Math.ceil(y + h));
    }

    public static void endScissor() { ctx.disableScissor(); }

    public static boolean hover(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------------ ikony (Lucide)

    public static final class Icons {
        public static final char SWORDS = '', MOVE = '', EYE = '', USER = '', GLOBE = '',
                BOXES = '', LAYERS = '', FOLDER = '', PALETTE = '', SEARCH = '',
                KEYBOARD = '', RESET = '', STAR = '', SETTINGS = '', SPARKLES = '',
                CHECK = '', SAVE = '', FOLDER_OPEN = '', TRASH = '', PLUS = '',
                DOWNLOAD = '', SUN = '', MOON = '', ZAP = '', SHIELD = '',
                CROSSHAIR = '', GAUGE = '', COMMAND = '', GRID = '', SLIDERS = '',
                BELL = '', CIRCLE_CHECK = '', CIRCLE_X = '', X = '', FOOTPRINTS = '';

        private Icons() {}
    }
}
