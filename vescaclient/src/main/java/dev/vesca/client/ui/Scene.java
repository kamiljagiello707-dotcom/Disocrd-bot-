package dev.vesca.client.ui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.vesca.client.render.R2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Wspólne, animowane tło wszystkich ekranów VescaClient (biało-czarne):
 * nocne niebo z gwiazdami i spadającymi gwiazdami, srebrna zorza, dwie warstwy gór z bloków i świetliki.
 * Stan jest statyczny, więc animacja płynnie trwa dalej przy przejściu między ekranami.
 */
public final class Scene {
    private Scene() {}

    private static final Random RND = new Random();
    private static final long EPOCH = System.currentTimeMillis();
    private static float[][] stars, motes;
    private static float[] shoot;
    private static long nextShoot = System.currentTimeMillis() + 2500, lastFrame = System.nanoTime();
    private static float pmx, pmy;
    private static int width, height;

    private static float sin(double v) { return (float) Math.sin(v); }

    private static void ensure() {
        if (stars != null) return;
        stars = new float[150][];
        for (int i = 0; i < stars.length; i++)
            stars[i] = new float[]{RND.nextFloat(), (float) Math.pow(RND.nextFloat(), 1.4) * 0.72f, 0.35f + RND.nextFloat() * (RND.nextFloat() < 0.12f ? 1.1f : 0.45f),
                    RND.nextFloat() * 6.28f, 0.6f + RND.nextFloat() * 2.2f};
        motes = new float[26][];
        for (int i = 0; i < motes.length; i++) motes[i] = newMote(true);
    }

    private static float[] newMote(boolean anywhere) {
        return new float[]{RND.nextFloat(), anywhere ? 0.55f + RND.nextFloat() * 0.45f : 1.02f,
                (RND.nextFloat() - 0.5f) * 0.012f, -0.006f - RND.nextFloat() * 0.012f, 0.6f + RND.nextFloat() * 1.4f, RND.nextFloat() * 6.28f};
    }

    /** czas animacji tła w sekundach */
    public static float time() { return (System.currentTimeMillis() - EPOCH) / 1000f; }

    /**
     * Rysuje całe tło. intro 0..1 – wejście (zorza i góry lekko wjeżdżają).
     * dim 0..1 – dodatkowe przyciemnienie (np. pod listą albo ekranem wczytywania).
     */
    public static void render(DrawContext ctx, int w, int h, int mouseX, int mouseY, float intro, float dim) {
        ensure();
        width = w; height = h;
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        float t = time();
        float k = 1 - (float) Math.exp(-4 * dt);
        pmx += ((mouseX - w / 2f) - pmx) * k;
        pmy += ((mouseY - h / 2f) - pmy) * k;
        float ox = -pmx, oy = -pmy;
        float prev = R2D.alpha;
        R2D.alpha = 1;

        R2D.gradientV(ctx, 0, 0, w, h, 0xFF040405, 0xFF0E0E11);
        R2D.glow(ctx, -w * 0.2f, h * 0.25f, w * 0.8f, h * 0.8f, R2D.withAlpha(0xFFFFFFFF, 0.035f));
        R2D.glow(ctx, w * 0.45f, h * 0.12f, w * 0.8f, h * 0.75f, R2D.withAlpha(0xFFFFFFFF, 0.03f));
        stars(ctx, t, ox, oy);
        shootingStar(ctx, dt);
        aurora(ctx, t, ox, intro);
        terrain(ctx, ox, 0, intro);
        motes(ctx, t, dt, ox);
        terrain(ctx, ox, 1, intro);
        R2D.gradientV(ctx, 0, 0, w, h * 0.25f, 0x99000000, 0x00000000);
        if (dim > 0) R2D.rect(ctx, 0, 0, w, h, R2D.withAlpha(0xFF000000, dim));
        R2D.alpha = prev;
    }

    private static void stars(DrawContext ctx, float t, float ox, float oy) {
        ctx.draw();
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (float[] s : stars) {
            float tw = 0.5f + 0.5f * sin(t * s[4] + s[3]);
            float x = s[0] * width + ox * 0.012f, y = s[1] * height + oy * 0.012f, r = s[2] * 0.5f;
            int c = R2D.withAlpha(0xFFFFFFFF, 0.18f + 0.62f * tw);
            b.vertex(m, x - r, y - r, 0).color(c);
            b.vertex(m, x - r, y + r, 0).color(c);
            b.vertex(m, x + r, y + r, 0).color(c);
            b.vertex(m, x + r, y - r, 0).color(c);
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
        RenderSystem.defaultBlendFunc();
        for (int i = 0; i < stars.length; i += 11) {
            float[] s = stars[i];
            float tw = 0.5f + 0.5f * sin(t * s[4] + s[3]);
            float x = s[0] * width + ox * 0.012f, y = s[1] * height + oy * 0.012f;
            R2D.glow(ctx, x - 5, y - 5, 10, 10, R2D.withAlpha(0xFFFFFFFF, 0.22f * tw));
        }
    }

    private static void shootingStar(DrawContext ctx, float dt) {
        long now = System.currentTimeMillis();
        if (shoot == null && now > nextShoot) {
            float sx = width * (0.25f + RND.nextFloat() * 0.7f), sy = height * (0.04f + RND.nextFloat() * 0.22f);
            float sp = 260 + RND.nextFloat() * 160;
            shoot = new float[]{sx, sy, -sp * 0.86f, sp * 0.5f, 0};
            nextShoot = now + 4000 + RND.nextInt(6000);
        }
        if (shoot == null) return;
        shoot[4] += dt / 0.9f;
        shoot[0] += shoot[2] * dt; shoot[1] += shoot[3] * dt;
        if (shoot[4] >= 1) { shoot = null; return; }
        float a = sin(Math.PI * shoot[4]);
        float len = 70 * a + 6;
        float vx = shoot[2], vy = shoot[3], vl = (float) Math.sqrt(vx * vx + vy * vy);
        float dx = vx / vl, dy = vy / vl, px = -dy, py = dx, w = 0.7f;
        float hx = shoot[0], hy = shoot[1], tx = hx - dx * len, ty = hy - dy * len;
        ctx.draw();
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int head = R2D.withAlpha(0xFFFFFFFF, a), tail = R2D.withAlpha(0xFFFFFFFF, 0);
        b.vertex(m, hx + px * w, hy + py * w, 0).color(head);
        b.vertex(m, tx, ty, 0).color(tail);
        b.vertex(m, tx, ty, 0).color(tail);
        b.vertex(m, hx - px * w, hy - py * w, 0).color(head);
        BufferRenderer.drawWithGlobalProgram(b.end());
        RenderSystem.defaultBlendFunc();
        R2D.glow(ctx, hx - 6, hy - 6, 12, 12, R2D.withAlpha(0xFFFFFFFF, 0.6f * a));
    }

    /** srebrna zorza: trzy falujące wstęgi z pionowymi "promieniami", mieszane addytywnie */
    private static void aurora(DrawContext ctx, float t, float ox, float intro) {
        ctx.draw();
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        int[][] pal = {{0xFFFFFFFF, 0xFFB4B8C4}, {0xFFDCDEE6, 0xFF8C909C}, {0xFFFFFFFF, 0xFF9CA0AC}};
        float[] base = {0.30f, 0.39f, 0.24f};
        float[] inten = {0.30f, 0.22f, 0.15f};
        float rise = 18 * (1 - intro);
        float step = 3;
        for (int k = 0; k < 3; k++) {
            float par = ox * (0.025f + k * 0.012f);
            int cap = (int) ((width + 80) / step) + 2;
            float[] ys = new float[cap], as = new float[cap];
            int[] cs = new int[cap];
            int n = 0;
            BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            for (float x = -40; x <= width + 40 && n < cap; x += step, n++) {
                double xx = x + par;
                float y = height * base[k] + rise + 20 * sin(xx * 0.0055 + t * 0.22 + k * 1.7) + 9 * sin(xx * 0.017 - t * 0.38 + k)
                        + 4 * sin(xx * 0.041 + t * 0.6 + k * 3);
                float hh = height * (0.17f + 0.05f * k) * (0.62f + 0.38f * sin(xx * 0.0105 + t * 0.31 + k * 2.1));
                float rays = 0.5f + 0.5f * sin(xx * 0.085 + t * 0.7 + k) * sin(xx * 0.029 - t * 0.43 + k * 0.5);
                float al = inten[k] * intro * (0.35f + 0.65f * rays) * (0.55f + 0.45f * sin(xx * 0.0042 + t * 0.19 + k));
                int c = R2D.blend(pal[k][0], pal[k][1], 0.5f + 0.5f * sin(xx * 0.0031 + t * 0.14 + k * 1.3));
                b.vertex(m, x, y - hh, 0).color(R2D.withAlpha(c, 0));
                b.vertex(m, x, y, 0).color(R2D.withAlpha(c, al));
                ys[n] = y; as[n] = al; cs[n] = c;
            }
            BufferRenderer.drawWithGlobalProgram(b.end());
            BufferBuilder e = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            float x = -40;
            for (int i = 0; i < n; i++, x += step) {
                e.vertex(m, x, ys[i] - 2, 0).color(R2D.withAlpha(cs[i], as[i] * 0.9f));
                e.vertex(m, x, ys[i] + 9, 0).color(R2D.withAlpha(cs[i], 0));
            }
            BufferRenderer.drawWithGlobalProgram(e.end());
        }
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
    }

    /** wysokość kolumny gór (w blokach) – deterministyczny "szum" z sinusów */
    private static float hill(int i, int layer) {
        double s = layer * 17.3;
        return (float) (Math.sin(i * 0.19 + s) * 2.2 + Math.sin(i * 0.071 + s * 2) * 4.0 + Math.sin(i * 0.53 + s * 3) * 0.9 + Math.sin(i * 0.013 + s) * 3.0);
    }

    private static void terrain(DrawContext ctx, float ox, int layer, float intro) {
        float B = layer == 0 ? 6 : 8;
        float base = height * (layer == 0 ? 0.79f : 0.88f) + (1 - intro) * (layer == 0 ? 12 : 22);
        float par = ox * (layer == 0 ? 0.05f : 0.1f);
        int top = layer == 0 ? 0xFF131316 : 0xFF08080A;
        int bot = layer == 0 ? 0xFF0C0C0E : 0xFF040405;
        ctx.draw();
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int i0 = (int) Math.floor((-par) / B) - 2, i1 = i0 + (int) (width / B) + 5;
        int rim = R2D.withAlpha(0xFFFFFFFF, layer == 0 ? 0.09f : 0.07f);
        for (int i = i0; i <= i1; i++) {
            float x = i * B + par;
            float hgt = Math.max(0, Math.round(hill(i, layer) + 5)) * B;
            float y = base - hgt;
            b.vertex(m, x, y, 0).color(top);
            b.vertex(m, x, height, 0).color(bot);
            b.vertex(m, x + B, height, 0).color(bot);
            b.vertex(m, x + B, y, 0).color(top);
            b.vertex(m, x, y, 0).color(rim);
            b.vertex(m, x, y + 1, 0).color(rim);
            b.vertex(m, x + B, y + 1, 0).color(rim);
            b.vertex(m, x + B, y, 0).color(rim);
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
        if (layer == 0) R2D.gradientV(ctx, 0, base - 30, width, 60, R2D.withAlpha(0xFF16161A, 0f), R2D.withAlpha(0xFF16161A, 0.55f));
    }

    private static void motes(DrawContext ctx, float t, float dt, float ox) {
        for (int i = 0; i < motes.length; i++) {
            float[] p = motes[i];
            p[0] += p[2] * dt; p[1] += p[3] * dt;
            if (p[1] < 0.45f) motes[i] = p = newMote(false);
            float life = Anim.clamp01((1.02f - p[1]) / 0.12f) * Anim.clamp01((p[1] - 0.45f) / 0.2f);
            float tw = 0.5f + 0.5f * sin(t * 2.3f + p[5]);
            float x = p[0] * width + ox * 0.08f + 6 * sin(t * 0.7f + p[5]), y = p[1] * height;
            float s = 3 + p[4] * 2.5f;
            R2D.glow(ctx, x - s, y - s, s * 2, s * 2, R2D.withAlpha(0xFFFFFFFF, 0.35f * life * (0.4f + 0.6f * tw)));
            R2D.circle(ctx, x, y, p[4] * 0.45f, R2D.withAlpha(0xFFFFFFFF, 0.85f * life * tw));
        }
    }
}
