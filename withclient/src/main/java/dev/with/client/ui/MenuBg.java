package dev.with.client.ui;

import dev.with.client.mixin.ScreenAccessor;
import dev.with.client.render.R2D;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;

import java.util.Random;

/**
 * Wspólne tło ekranów WithClient (menu, listy, wczytywanie): obracająca się panorama Minecrafta,
 * opcjonalne rozmycie, ciemny gradient, wolno płynące światła i siatka cząsteczek reagująca na kursor.
 * Animacja trwa dalej przy przechodzeniu między ekranami.
 */
public final class MenuBg {
    private MenuBg() {}

    private static final RotatingCubeMapRenderer PANORAMA = new RotatingCubeMapRenderer(new CubeMapRenderer(Identifier.ofVanilla("textures/gui/title/background/panorama")));
    private static final long START = System.currentTimeMillis();
    private static final int N = 46;
    private static final float[] PX = new float[N], PY = new float[N], VX = new float[N], VY = new float[N];
    private static long last = System.nanoTime();
    static {
        Random r = new Random(7);
        for (int i = 0; i < N; i++) { PX[i] = r.nextFloat(); PY[i] = r.nextFloat(); VX[i] = (r.nextFloat() - 0.5f) * 0.012f; VY[i] = (r.nextFloat() - 0.5f) * 0.012f; }
    }

    public static float time() { return (System.currentTimeMillis() - START) / 1000f; }

    /**
     * @param blur  rozmycie panoramy (listy, wczytywanie)
     * @param dark  dodatkowe przyciemnienie 0..1
     * @param side  ciemniejsza lewa strona (menu główne)
     */
    public static void render(DrawContext ctx, Screen s, int w, int h, int mx, int my, float delta, boolean blur, float dark, boolean side) {
        float prevA = R2D.alpha;
        R2D.alpha = 1;
        PANORAMA.render(ctx, w, h, 1f, delta);
        if (blur && s != null) { ctx.draw(); ((ScreenAccessor) s).withclient$applyBlur(delta); }
        // ciemne warstwy
        R2D.rect(ctx, 0, 0, w, h, R2D.withAlpha(0xFF050507, 0.38f + 0.45f * dark));
        if (side) R2D.gradientH(ctx, 0, 0, w * 0.62f, h, 0xE6050507, 0x00050507);
        R2D.gradientV(ctx, 0, h * 0.55f, w, h * 0.45f, 0x00050507, 0xCC050507);
        R2D.gradientV(ctx, 0, 0, w, h * 0.25f, 0x99050507, 0x00050507);
        // płynące światła
        float t = time();
        for (int i = 0; i < 3; i++) {
            float ox = w * (0.25f + 0.55f * (0.5f + 0.5f * (float) Math.sin(t * 0.07f + i * 2.1f)));
            float oy = h * (0.3f + 0.5f * (0.5f + 0.5f * (float) Math.cos(t * 0.05f + i * 1.7f)));
            float r = Math.max(w, h) * (0.35f + 0.1f * i);
            R2D.glow(ctx, ox - r, oy - r, r * 2, r * 2, R2D.withAlpha(0xFFFFFFFF, 0.035f));
        }
        particles(ctx, w, h, mx, my);
        R2D.alpha = prevA;
    }

    private static void particles(DrawContext ctx, int w, int h, int mx, int my) {
        float dt = Math.min(0.1f, (System.nanoTime() - last) / 1e9f);
        last = System.nanoTime();
        for (int i = 0; i < N; i++) {
            PX[i] += VX[i] * dt; PY[i] += VY[i] * dt;
            if (PX[i] < 0) PX[i] += 1; if (PX[i] > 1) PX[i] -= 1;
            if (PY[i] < 0) PY[i] += 1; if (PY[i] > 1) PY[i] -= 1;
        }
        float maxD = Math.min(w, h) * 0.2f, md2 = maxD * maxD;
        int[] cols = new int[N * N / 2 + N];
        float[] seg = new float[cols.length * 4];
        int n = 0;
        float[] xs = new float[N], ys = new float[N];
        for (int i = 0; i < N; i++) { xs[i] = PX[i] * w; ys[i] = PY[i] * h; }
        for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N && n < cols.length - N; j++) {
                float dx = xs[i] - xs[j], dy = ys[i] - ys[j], d2 = dx * dx + dy * dy;
                if (d2 > md2) continue;
                float k = 1 - (float) Math.sqrt(d2) / maxD;
                seg[n * 4] = xs[i]; seg[n * 4 + 1] = ys[i]; seg[n * 4 + 2] = xs[j]; seg[n * 4 + 3] = ys[j];
                cols[n++] = R2D.withAlpha(0xFFFFFFFF, 0.08f * k);
            }
            float dx = xs[i] - mx, dy = ys[i] - my, d2 = dx * dx + dy * dy;
            if (d2 < maxD * maxD * 1.6f) {
                float k = 1 - (float) Math.sqrt(d2) / (maxD * 1.27f);
                seg[n * 4] = xs[i]; seg[n * 4 + 1] = ys[i]; seg[n * 4 + 2] = mx; seg[n * 4 + 3] = my;
                cols[n++] = R2D.withAlpha(0xFFFFFFFF, Math.max(0, 0.18f * k));
            }
        }
        R2D.lines(ctx, seg, cols, n, 0.5f);
        for (int i = 0; i < N; i++) R2D.circle(ctx, xs[i], ys[i], 0.8f, 0x55FFFFFF);
    }
}
