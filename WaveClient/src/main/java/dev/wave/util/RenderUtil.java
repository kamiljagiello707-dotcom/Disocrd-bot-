package dev.wave.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modern drawing helpers.
 * <ul>
 *   <li>Shapes are rasterised in real screen pixels (not GUI pixels) with anti-aliased rounded corners.</li>
 *   <li>Text uses the bundled Inter TTF font; the glyph atlas whose resolution matches the current on-screen
 *   pixel density is picked automatically, so text is always rendered 1:1 and stays sharp.</li>
 *   <li>{@link #globalAlpha} multiplies every colour, which makes fading whole screens trivial.</li>
 * </ul>
 */
public final class RenderUtil {
    private RenderUtil() {}

    public static float globalAlpha = 1f;

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    private static int a(int color) { return globalAlpha >= 1f ? color : ColorUtil.alpha(color, globalAlpha); }

    /** Screen pixels per GUI unit (window scale x current matrix scale). */
    private static double pixelScale(DrawContext c) {
        float m = c.getMatrices().peek().getPositionMatrix().m00();
        return mc().getWindow().getScaleFactor() * (m <= 0 ? 1 : m);
    }

    public static TextRenderer font() { return mc().textRenderer; }

    // ------------------------------------------------------------------ shapes

    private record Span(int y, int x0, int x1, float a) {}

    private static final Map<Long, List<Span>> CORNERS = new HashMap<>();

    /** Coverage spans for an R x R top-left corner; {@code t <= 0} means filled, otherwise a ring of thickness t. */
    private static List<Span> corner(int r, int t) {
        long key = ((long) r << 32) | (t & 0xffffffffL);
        List<Span> cached = CORNERS.get(key);
        if (cached != null) return cached;
        List<Span> spans = new ArrayList<>();
        int ri = t > 0 ? r - t : -1;
        for (int y = 0; y < r; y++) {
            int runStart = -1;
            float runA = 0;
            for (int x = 0; x <= r; x++) {
                float cov = 0;
                if (x < r) {
                    double d = Math.hypot(r - (x + 0.5), r - (y + 0.5));
                    cov = clamp01((float) (r - d + 0.5));
                    if (ri > 0) cov -= clamp01((float) (ri - d + 0.5));
                    cov = Math.round(cov * 20) / 20f;
                }
                if (runStart >= 0 && cov != runA) {
                    if (runA > 0) spans.add(new Span(y, runStart, x, runA));
                    runStart = -1;
                }
                if (runStart < 0 && x < r) {
                    runStart = x;
                    runA = cov;
                }
            }
        }
        CORNERS.put(key, spans);
        return spans;
    }

    private static float clamp01(float f) { return f < 0 ? 0 : (f > 1 ? 1 : f); }

    /** Pushes a matrix that maps 1 unit to 1 screen pixel at a pixel-snapped (x, y). Returns the scale. */
    private static double pushPixels(DrawContext c, float x, float y) {
        double s = pixelScale(c);
        MatrixStack m = c.getMatrices();
        m.push();
        float sx = (float) (Math.round(x * s) / s), sy = (float) (Math.round(y * s) / s);
        m.translate(sx, sy, 0);
        m.scale((float) (1 / s), (float) (1 / s), 1f);
        return s;
    }

    public static void rect(DrawContext c, float x, float y, float w, float h, int color) {
        if (w <= 0 || h <= 0) return;
        color = a(color);
        if (ColorUtil.getAlpha(color) == 0) return;
        double s = pushPixels(c, x, y);
        c.fill(0, 0, Math.max(1, (int) Math.round(w * s)), Math.max(1, (int) Math.round(h * s)), color);
        c.getMatrices().pop();
    }

    public static void round(DrawContext c, float x, float y, float w, float h, float r, int color) {
        round(c, x, y, w, h, r, color, true, true);
    }

    public static void round(DrawContext c, float x, float y, float w, float h, float r, int color, boolean top, boolean bottom) {
        if (w <= 0 || h <= 0) return;
        color = a(color);
        if (ColorUtil.getAlpha(color) == 0) return;
        double s = pushPixels(c, x, y);
        int W = Math.max(1, (int) Math.round(w * s)), H = Math.max(1, (int) Math.round(h * s));
        int R = Math.max(0, Math.min((int) Math.round(r * s), Math.min(W / 2, H / 2)));
        int topR = top ? R : 0, botR = bottom ? R : 0;
        if (R > 0) {
            for (Span sp : corner(R, 0)) {
                int col = ColorUtil.alpha(color, sp.a);
                if (top) {
                    c.fill(sp.x0, sp.y, sp.x1, sp.y + 1, col);
                    c.fill(W - sp.x1, sp.y, W - sp.x0, sp.y + 1, col);
                }
                if (bottom) {
                    c.fill(sp.x0, H - sp.y - 1, sp.x1, H - sp.y, col);
                    c.fill(W - sp.x1, H - sp.y - 1, W - sp.x0, H - sp.y, col);
                }
            }
        }
        if (topR > 0) c.fill(R, 0, W - R, topR, color);
        if (botR > 0) c.fill(R, H - botR, W - R, H, color);
        c.fill(0, topR, W, H - botR, color);
        c.getMatrices().pop();
    }

    /** Anti-aliased rounded outline. */
    public static void roundOutline(DrawContext c, float x, float y, float w, float h, float r, float thickness, int color) {
        color = a(color);
        if (ColorUtil.getAlpha(color) == 0 || w <= 0 || h <= 0) return;
        double s = pushPixels(c, x, y);
        int W = Math.max(2, (int) Math.round(w * s)), H = Math.max(2, (int) Math.round(h * s));
        int T = Math.max(1, (int) Math.round(thickness * s));
        int R = Math.max(0, Math.min((int) Math.round(r * s), Math.min(W / 2, H / 2)));
        if (R > 0) {
            for (Span sp : corner(R, T)) {
                int col = ColorUtil.alpha(color, sp.a);
                c.fill(sp.x0, sp.y, sp.x1, sp.y + 1, col);
                c.fill(W - sp.x1, sp.y, W - sp.x0, sp.y + 1, col);
                c.fill(sp.x0, H - sp.y - 1, sp.x1, H - sp.y, col);
                c.fill(W - sp.x1, H - sp.y - 1, W - sp.x0, H - sp.y, col);
            }
        }
        c.fill(R, 0, W - R, T, color);
        c.fill(R, H - T, W - R, H, color);
        c.fill(0, R, T, H - R, color);
        c.fill(W - T, R, W, H - R, color);
        c.getMatrices().pop();
    }

    public static void circle(DrawContext c, float cx, float cy, float r, int color) {
        round(c, cx - r, cy - r, r * 2, r * 2, r, color);
    }

    public static void gradientH(DrawContext c, float x, float y, float w, float h, int left, int right) {
        double s = pushPixels(c, x, y);
        int W = Math.max(1, (int) Math.round(w * s)), H = Math.max(1, (int) Math.round(h * s));
        int step = Math.max(1, W / 96);
        for (int i = 0; i < W; i += step) c.fill(i, 0, Math.min(W, i + step), H, a(ColorUtil.lerp(left, right, i / (float) W)));
        c.getMatrices().pop();
    }

    public static void gradientV(DrawContext c, float x, float y, float w, float h, int top, int bottom) {
        double s = pushPixels(c, x, y);
        c.fillGradient(0, 0, Math.max(1, (int) Math.round(w * s)), Math.max(1, (int) Math.round(h * s)), a(top), a(bottom));
        c.getMatrices().pop();
    }

    // ------------------------------------------------------------------ text

    private static final float[] OVERSAMPLES = {1.5f, 2, 2.5f, 3, 4, 5, 6, 8};
    private static final Identifier[] MEDIUM = new Identifier[OVERSAMPLES.length];
    private static final Identifier[] BOLD = new Identifier[OVERSAMPLES.length];

    static {
        for (int i = 0; i < OVERSAMPLES.length; i++) {
            String suffix = String.valueOf(OVERSAMPLES[i]).replace(".0", "").replace('.', '_');
            MEDIUM[i] = Identifier.of("waveclient", "inter_medium_" + suffix);
            BOLD[i] = Identifier.of("waveclient", "inter_bold_" + suffix);
        }
    }

    private static int fontIndex(double pixelScale) {
        int best = 0;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < OVERSAMPLES.length; i++) {
            double d = Math.abs(Math.log(OVERSAMPLES[i] / pixelScale));
            if (d < bestD) { bestD = d; best = i; }
        }
        return best;
    }

    private static final Map<String, Text>[] CACHE = createCaches();

    @SuppressWarnings("unchecked")
    private static Map<String, Text>[] createCaches() {
        Map<String, Text>[] arr = new Map[OVERSAMPLES.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = new LinkedHashMap<>(256, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, Text> e) { return size() > 1024; }
            };
        }
        return arr;
    }

    /** Converts a string with legacy § codes into a Text using the Inter fonts (§l switches to the bold face). */
    public static Text styled(String s, int fontIdx) {
        Map<String, Text> cache = CACHE[fontIdx];
        Text cached = cache.get(s);
        if (cached != null) return cached;
        Style base = Style.EMPTY.withFont(MEDIUM[fontIdx]);
        Style cur = base;
        MutableText out = Text.empty();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '§' && i + 1 < s.length()) {
                if (!sb.isEmpty()) { out.append(Text.literal(sb.toString()).setStyle(cur)); sb.setLength(0); }
                char code = Character.toLowerCase(s.charAt(++i));
                if (code == 'l') cur = cur.withFont(BOLD[fontIdx]);
                else if (code == 'r') cur = base;
                else if (code == 'n') cur = cur.withUnderline(true);
                else if (code == 'o') cur = cur.withItalic(true);
                else {
                    Formatting f = Formatting.byCode(code);
                    if (f != null && f.isColor()) cur = cur.withColor(f);
                }
                continue;
            }
            sb.append(ch);
        }
        if (!sb.isEmpty()) out.append(Text.literal(sb.toString()).setStyle(cur));
        cache.put(s, out);
        return out;
    }

    private static final Map<String, Integer> WIDTHS = new LinkedHashMap<>(512, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Integer> e) { return size() > 2048; }
    };

    public static int width(String s) {
        Integer w = WIDTHS.get(s);
        if (w == null) {
            w = font().getWidth(styled(s, 3));
            WIDTHS.put(s, w);
        }
        return w;
    }

    public static void text(DrawContext c, String s, float x, float y, int color, boolean shadow) {
        color = a(color);
        if (ColorUtil.getAlpha(color) < 6 || s.isEmpty()) return;
        MatrixStack m = c.getMatrices();
        m.push();
        double ps = pixelScale(c);
        // snap the baseline to the pixel grid for crisp glyphs
        m.translate((float) (Math.round(x * ps) / ps), (float) (Math.round(y * ps) / ps), 0);
        int idx = fontIndex(ps);
        Text t = styled(s, idx);
        if (shadow) {
            m.push();
            m.translate((float) (1 / ps), (float) (1 / ps), 0);
            c.drawText(font(), t, 0, 0, ColorUtil.withAlpha(0, (int) (ColorUtil.getAlpha(color) * 0.45f)), false);
            m.pop();
        }
        c.drawText(font(), t, 0, 0, color, false);
        m.pop();
    }

    public static void text(DrawContext c, String s, float x, float y, int color) { text(c, s, x, y, color, false); }

    public static void text(DrawContext c, Text s, float x, float y, int color, boolean shadow) {
        text(c, s.getString(), x, y, color, shadow);
    }

    public static void textScaled(DrawContext c, String s, float x, float y, float scale, int color, boolean shadow) {
        if (ColorUtil.getAlpha(a(color)) < 6) return;
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        text(c, s, 0, 0, color, shadow);
        m.pop();
    }

    public static void centered(DrawContext c, String s, float cx, float y, int color) {
        text(c, s, cx - width(s) / 2f, y, color, false);
    }

    public static boolean hovered(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public static void scissor(DrawContext c, float x, float y, float w, float h) {
        c.enableScissor((int) Math.floor(x), (int) Math.floor(y), (int) Math.ceil(x + w), (int) Math.ceil(y + h));
    }

    public static void endScissor(DrawContext c) { c.disableScissor(); }
}
