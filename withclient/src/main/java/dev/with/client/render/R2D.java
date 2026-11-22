package dev.with.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Renderer 2D na floatach: wygładzone zaokrąglone prostokąty (9-slice z tekstury koła),
 * miękkie cienie i kolorowane ikony. Wszystko rysowane od razu, więc kolejność jest zachowana.
 */
public final class R2D {
    private R2D() {}

    /** globalna przezroczystość (animacja otwierania GUI) */
    public static float alpha = 1f;
    /** gdy true – rysowanie z testem głębi (np. elementy 2D w świecie) */
    public static boolean worldDepth = false;
    /** mnożnik promienia zaokrągleń (styl Neon w ClickGUI) */
    public static float radiusMul = 1f;

    private static final int[] CIRCLE_SIZES = {16, 32, 64, 128, 256};
    private static final Identifier[] CIRCLES = new Identifier[CIRCLE_SIZES.length];
    private static final Identifier SHADOW = id("textures/ui/shadow.png");
    private static final Map<String, Identifier> ICONS = new HashMap<>();
    private static final Deque<int[]> SCISSORS = new ArrayDeque<>();

    static {
        for (int i = 0; i < CIRCLE_SIZES.length; i++) CIRCLES[i] = id("textures/ui/circle_" + CIRCLE_SIZES[i] + ".png");
    }

    /** mieszanie z poprawnym kanałem alfa (ważne przy renderze do przezroczystego bufora – GUI 3D) */
    private static void blend() {
        RenderSystem.blendFuncSeparate(com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.SrcFactor.ONE, com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
    }

    /** mieszanie addytywne tylko dla koloru – kanał alfa celu bez zmian (poprawne także przy renderze do przezroczystego bufora) */
    private static void addBlend() {
        RenderSystem.blendFuncSeparate(com.mojang.blaze3d.platform.GlStateManager.SrcFactor.ONE, com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.SrcFactor.ZERO, com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE);
    }

    public static Identifier id(String path) { return Identifier.of("withclient", path); }

    // ---------------------------------------------------------------- kolory

    public static int withAlpha(int argb, float a) {
        int al = Math.round(((argb >>> 24) & 0xFF) * Math.max(0, Math.min(1, a)));
        return (al << 24) | (argb & 0xFFFFFF);
    }

    /** kolor z uwzględnieniem globalnej przezroczystości */
    public static int col(int argb) { return withAlpha(argb, alpha); }

    public static int blend(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    // ---------------------------------------------------------------- prymitywy

    private static float pixelScale(DrawContext ctx) {
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        return (float) MinecraftClient.getInstance().getWindow().getScaleFactor() * m.m00();
    }

    private static BufferBuilder begin(Identifier tex) {
        RenderSystem.enableBlend();
        blend();
        if (worldDepth) { RenderSystem.enableDepthTest(); RenderSystem.depthMask(false); } else RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        return Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
    }

    private static void end(BufferBuilder b) {
        BuiltBuffer built = b.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
        RenderSystem.enableDepthTest();
        if (worldDepth) RenderSystem.depthMask(true);
    }

    private static void quad(BufferBuilder b, Matrix4f m, float x1, float y1, float x2, float y2,
                             float u1, float v1, float u2, float v2, int c) {
        if (x2 <= x1 || y2 <= y1) return;
        b.vertex(m, x1, y1, 0).texture(u1, v1).color(c);
        b.vertex(m, x1, y2, 0).texture(u1, v2).color(c);
        b.vertex(m, x2, y2, 0).texture(u2, v2).color(c);
        b.vertex(m, x2, y1, 0).texture(u2, v1).color(c);
    }

    /** 9-slice: rogi z tekstury (0..0.5), krawędzie i środek z jej środka */
    private static void nineSlice(BufferBuilder b, Matrix4f m, float x, float y, float w, float h, float r, float ri, int c) {
        // r – rozmiar rogu na ekranie, ri – część rogu wchodząca do środka (dla cienia r=2*ri)
        float x2 = x + w, y2 = y + h;
        float h5 = 0.5f;
        quad(b, m, x, y, x + r, y + r, 0, 0, h5, h5, c);
        quad(b, m, x2 - r, y, x2, y + r, h5, 0, 1, h5, c);
        quad(b, m, x, y2 - r, x + r, y2, 0, h5, h5, 1, c);
        quad(b, m, x2 - r, y2 - r, x2, y2, h5, h5, 1, 1, c);
        quad(b, m, x + r, y, x2 - r, y + r, h5, 0, h5, h5, c);
        quad(b, m, x + r, y2 - r, x2 - r, y2, h5, h5, h5, 1, c);
        quad(b, m, x, y + r, x + r, y2 - r, 0, h5, h5, h5, c);
        quad(b, m, x2 - r, y + r, x2, y2 - r, h5, h5, 1, h5, c);
        quad(b, m, x + r, y + r, x2 - r, y2 - r, h5, h5, h5, h5, c);
    }

    // ---------------------------------------------------------------- shadery SDF (pixel-perfect)

    private static ShaderProgram SHAPE, TEX;
    /** shader tekstu z wygładzaniem (używany przez SmoothTextLayer) */
    public static ShaderProgram TEXT;

    /** rejestracja shaderów – wywołać w onInitializeClient (przed wczytaniem zasobów) */
    public static void initShaders() {
        net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback.EVENT.register(c -> {
            c.register(id("ui_shape"), VertexFormats.POSITION_TEXTURE_COLOR, p -> SHAPE = p);
            c.register(id("ui_tex"), VertexFormats.POSITION_TEXTURE_COLOR, p -> TEX = p);
            c.register(id("ui_text"), VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, p -> TEXT = p);
        });
    }

    /** gęstość pikseli ekranu na jednostkę GUI w osi X/Y (0 gdy macierz nie jest płaska 2D) */
    private static float pxX(Matrix4f m) { return (float) MinecraftClient.getInstance().getWindow().getScaleFactor() * m.m00(); }
    private static float pxY(Matrix4f m) { return (float) MinecraftClient.getInstance().getWindow().getScaleFactor() * m.m11(); }
    private static boolean flat(Matrix4f m) { return !worldDepth && m.m01() == 0 && m.m10() == 0 && m.m03() == 0 && m.m13() == 0 && m.m00() > 0 && m.m11() > 0; }

    /** liczba fizycznych pikseli ekranu na jednostkę GUI (z uwzględnieniem bieżącej macierzy) */
    public static float px(DrawContext ctx) { return Math.max(0.25f, pxX(ctx.getMatrices().peek().getPositionMatrix())); }

    /** przyciąga współrzędną X do siatki fizycznych pikseli ekranu */
    public static float snapX(Matrix4f m, float x) {
        if (!flat(m)) return x;
        float s = pxX(m), o = m.m30() * (float) MinecraftClient.getInstance().getWindow().getScaleFactor();
        return (Math.round(x * s + o) - o) / s;
    }

    public static float snapY(Matrix4f m, float y) {
        if (!flat(m)) return y;
        float s = pxY(m), o = m.m31() * (float) MinecraftClient.getInstance().getWindow().getScaleFactor();
        return (Math.round(y * s + o) - o) / s;
    }

    /**
     * Rysuje zaokrąglony prostokąt shaderem SDF: idealnie gładkie krawędzie (1 px AA), ostre linie
     * przyciągnięte do pikseli, gradient z 4 rogów (lg, pg, pd, ld), obrys (thick>0) lub miękki cień (soft>0).
     * @return false gdy shader niedostępny (wtedy używana jest stara ścieżka)
     */
    private static boolean shape(DrawContext ctx, float x, float y, float w, float h, float r,
                                 float thick, float soft, int ctl, int ctr, int cbr, int cbl) {
        if (SHAPE == null) return false;
        if ((((ctl | ctr | cbr | cbl) >>> 24) & 0xFF) == 0) return true;
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        if (soft <= 0 && flat(m)) {
            float sx = pxX(m), sy = pxY(m);
            float x1 = snapX(m, x), y1 = snapY(m, y), x2 = snapX(m, x + w), y2 = snapY(m, y + h);
            // linie cieńsze niż piksel -> dokładnie 1 piksel (zamiast rozmytych pół-pikseli)
            if (w > 0 && x2 - x1 < 1 / sx) { x1 = snapX(m, x + w / 2 - 0.5f / sx); x2 = x1 + 1 / sx; }
            if (h > 0 && y2 - y1 < 1 / sy) { y1 = snapY(m, y + h / 2 - 0.5f / sy); y2 = y1 + 1 / sy; }
            x = x1; y = y1; w = x2 - x1; h = y2 - y1;
            if (thick > 0) thick = Math.max(1 / Math.min(sx, sy), Math.round(thick * Math.min(sx, sy)) / Math.min(sx, sy));
        }
        if (w <= 0 || h <= 0) return true;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2f));
        float ps = flat(m) ? Math.min(pxX(m), pxY(m)) : 0;
        float pad = soft > 0 ? soft * 1.05f : (ps > 0 ? 2f / ps : Math.max(w, h) * 0.04f);
        RenderSystem.enableBlend();
        blend();
        if (worldDepth) { RenderSystem.enableDepthTest(); RenderSystem.depthMask(false); } else RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> SHAPE);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        uni(SHAPE, "Size", w, h);
        uni(SHAPE, "Radius", r, r, r, r);
        uni(SHAPE, "Thickness", thick);
        uni(SHAPE, "Soft", soft);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        float ax = x - pad, ay = y - pad, bx = x + w + pad, by = y + h + pad;
        b.vertex(m, ax, ay, 0).texture(-pad, -pad).color(ctl);
        b.vertex(m, ax, by, 0).texture(-pad, h + pad).color(cbl);
        b.vertex(m, bx, by, 0).texture(w + pad, h + pad).color(cbr);
        b.vertex(m, bx, ay, 0).texture(w + pad, -pad).color(ctr);
        end(b);
        return true;
    }

    /** tekstura shaderem z 16 próbkami (bez schodków przy pomniejszaniu), opcjonalnie zaokrąglona */
    private static boolean tex(DrawContext ctx, Identifier tex, float x, float y, float w, float h, float r,
                               float u0, float v0, float u1, float v1, int c, boolean additive) {
        if (TEX == null) return false;
        if (((c >>> 24) & 0xFF) == 0 || w <= 0 || h <= 0) return true;
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        if (additive) addBlend();
        else blend();
        if (worldDepth) { RenderSystem.enableDepthTest(); RenderSystem.depthMask(false); } else RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> TEX);
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2f));
        uni(TEX, "UVRect", u0, v0, u1, v1);
        uni(TEX, "Size", w, h);
        uni(TEX, "Radius", r, r, r, r);
        uni(TEX, "Rounded", r > 0 ? 1 : 0);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        quad(b, m, x, y, x + w, y + h, u0, v0, u1, v1, c);
        end(b);
        if (additive) blend();
        return true;
    }

    private static void uni(ShaderProgram p, String name, float... v) {
        net.minecraft.client.gl.GlUniform u = p.getUniform(name);
        if (u == null) return;
        switch (v.length) {
            case 1 -> u.set(v[0]);
            case 2 -> u.set(v[0], v[1]);
            case 4 -> u.set(v[0], v[1], v[2], v[3]);
            default -> {}
        }
    }

    public static void roundRect(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        r *= radiusMul;
        int c = col(color);
        if ((c >>> 24) == 0 || w <= 0 || h <= 0) return;
        if (shape(ctx, x, y, w, h, r, 0, 0, c, c, c, c)) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2f));
        float px = r * 2 * pixelScale(ctx);
        Identifier tex = CIRCLES[CIRCLES.length - 1];
        for (int i = 0; i < CIRCLE_SIZES.length; i++) if (CIRCLE_SIZES[i] >= px) { tex = CIRCLES[i]; break; }
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder b = begin(tex);
        nineSlice(b, m, x, y, w, h, r, r, c);
        end(b);
    }

    /** zaokrąglony prostokąt z gradientem góra -> dół */
    public static void roundRectV(DrawContext ctx, float x, float y, float w, float h, float r, int top, int bottom) {
        r *= radiusMul;
        if (w <= 0 || h <= 0) return;
        if (shape(ctx, x, y, w, h, r, 0, 0, col(top), col(top), col(bottom), col(bottom))) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2f));
        float px = r * 2 * pixelScale(ctx);
        Identifier tex = CIRCLES[CIRCLES.length - 1];
        for (int i = 0; i < CIRCLE_SIZES.length; i++) if (CIRCLE_SIZES[i] >= px) { tex = CIRCLES[i]; break; }
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder b = begin(tex);
        int ct = col(top), cb = col(bottom);
        float x2 = x + w, y2 = y + h, h5 = 0.5f;
        float[] ys = {y, y + r, y2 - r, y2};
        int[] cs = new int[4];
        for (int i = 0; i < 4; i++) cs[i] = blend(ct, cb, (ys[i] - y) / h);
        float[] xs = {x, x + r, x2 - r, x2};
        float[] us = {0, h5, h5, 1};
        for (int row = 0; row < 3; row++)
            for (int colI = 0; colI < 3; colI++) {
                float ax = xs[colI], bx = xs[colI + 1], ay = ys[row], by = ys[row + 1];
                if (bx <= ax || by <= ay) continue;
                float u1 = us[colI], u2 = us[colI + 1], v1 = us[row], v2 = us[row + 1];
                b.vertex(m, ax, ay, 0).texture(u1, v1).color(cs[row]);
                b.vertex(m, ax, by, 0).texture(u1, v2).color(cs[row + 1]);
                b.vertex(m, bx, by, 0).texture(u2, v2).color(cs[row + 1]);
                b.vertex(m, bx, ay, 0).texture(u2, v1).color(cs[row]);
            }
        end(b);
    }

    /** zaokrąglony prostokąt z gradientem lewo -> prawo */
    public static void roundRectH(DrawContext ctx, float x, float y, float w, float h, float r, int left, int right) {
        r *= radiusMul;
        if (w <= 0 || h <= 0) return;
        if (shape(ctx, x, y, w, h, r, 0, 0, col(left), col(right), col(right), col(left))) return;
        roundRect(ctx, x, y, w, h, r / radiusMul, blend(left, right, 0.5f));
    }

    public static void rect(DrawContext ctx, float x, float y, float w, float h, int color) {
        roundRect(ctx, x, y, w, h, 0, color);
    }

    /** obramowanie o grubości t z zaokrąglonymi rogami */
    public static void roundOutline(DrawContext ctx, float x, float y, float w, float h, float r, float t, int color) {
        r *= radiusMul;
        int c = col(color);
        if ((c >>> 24) == 0) return;
        if (shape(ctx, x, y, w, h, r, t, 0, c, c, c, c)) return;
        r = Math.max(t, Math.min(r, Math.min(w, h) / 2f));
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        blend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        // proste odcinki
        rq(b, m, x + r, y, x + w - r, y + t, c);
        rq(b, m, x + r, y + h - t, x + w - r, y + h, c);
        rq(b, m, x, y + r, x + t, y + h - r, c);
        rq(b, m, x + w - t, y + r, x + w, y + h - r, c);
        // łuki
        float[][] centers = {{x + r, y + r, 180}, {x + w - r, y + r, 270}, {x + w - r, y + h - r, 0}, {x + r, y + h - r, 90}};
        int seg = 10;
        for (float[] cc : centers) {
            for (int i = 0; i < seg; i++) {
                double a1 = Math.toRadians(cc[2] + 90.0 * i / seg), a2 = Math.toRadians(cc[2] + 90.0 * (i + 1) / seg);
                float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1), c2 = (float) Math.cos(a2), s2 = (float) Math.sin(a2);
                b.vertex(m, cc[0] + c1 * r, cc[1] + s1 * r, 0).color(c);
                b.vertex(m, cc[0] + c1 * (r - t), cc[1] + s1 * (r - t), 0).color(c);
                b.vertex(m, cc[0] + c2 * (r - t), cc[1] + s2 * (r - t), 0).color(c);
                b.vertex(m, cc[0] + c2 * r, cc[1] + s2 * r, 0).color(c);
            }
        }
        end(b);
    }

    /** obramowanie z gradientem od lewej (cl) do prawej (cr) */
    public static void roundOutlineGrad(DrawContext ctx, float x, float y, float w, float h, float r, float t, int cl, int cr) {
        r *= radiusMul;
        int l = col(cl), rr = col(cr);
        if (((l >>> 24) | (rr >>> 24)) == 0) return;
        if (shape(ctx, x, y, w, h, r, t, 0, l, rr, rr, l)) return;
        r = Math.max(t, Math.min(r, Math.min(w, h) / 2f));
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        blend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        java.util.function.Function<Float, Integer> at = px -> blend(l, rr, (px - x) / w);
        float[][] edges = {{x + r, y, x + w - r, y + t}, {x + r, y + h - t, x + w - r, y + h}, {x, y + r, x + t, y + h - r}, {x + w - t, y + r, x + w, y + h - r}};
        for (float[] e : edges) {
            if (e[2] <= e[0] || e[3] <= e[1]) continue;
            int c1 = at.apply(e[0]), c2 = at.apply(e[2]);
            b.vertex(m, e[0], e[1], 0).color(c1);
            b.vertex(m, e[0], e[3], 0).color(c1);
            b.vertex(m, e[2], e[3], 0).color(c2);
            b.vertex(m, e[2], e[1], 0).color(c2);
        }
        float[][] centers = {{x + r, y + r, 180}, {x + w - r, y + r, 270}, {x + w - r, y + h - r, 0}, {x + r, y + h - r, 90}};
        int seg = 10;
        for (float[] cc : centers) {
            for (int i = 0; i < seg; i++) {
                double a1 = Math.toRadians(cc[2] + 90.0 * i / seg), a2 = Math.toRadians(cc[2] + 90.0 * (i + 1) / seg);
                float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1), c2 = (float) Math.cos(a2), s2 = (float) Math.sin(a2);
                float px1 = cc[0] + c1 * r, px2 = cc[0] + c2 * r;
                b.vertex(m, px1, cc[1] + s1 * r, 0).color(at.apply(px1));
                b.vertex(m, cc[0] + c1 * (r - t), cc[1] + s1 * (r - t), 0).color(at.apply(px1));
                b.vertex(m, cc[0] + c2 * (r - t), cc[1] + s2 * (r - t), 0).color(at.apply(px2));
                b.vertex(m, px2, cc[1] + s2 * r, 0).color(at.apply(px2));
            }
        }
        end(b);
    }

    /**
     * wiele odcinków w jednym wywołaniu: seg = [x1, y1, x2, y2] * n, colors = kolor każdego odcinka,
     * t = grubość. Używane np. do siatki cząsteczek w tle okna.
     */
    public static void lines(DrawContext ctx, float[] seg, int[] colors, int n, float t) {
        if (n <= 0) return;
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        blend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int drawn = 0;
        for (int i = 0; i < n; i++) {
            int c = col(colors[i]);
            if ((c >>> 24) == 0) continue;
            float x1 = seg[i * 4], y1 = seg[i * 4 + 1], x2 = seg[i * 4 + 2], y2 = seg[i * 4 + 3];
            float dx = x2 - x1, dy = y2 - y1, len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 1e-3f) continue;
            float nx = -dy / len * t / 2f, ny = dx / len * t / 2f;
            b.vertex(m, x1 + nx, y1 + ny, 0).color(c);
            b.vertex(m, x1 - nx, y1 - ny, 0).color(c);
            b.vertex(m, x2 - nx, y2 - ny, 0).color(c);
            b.vertex(m, x2 + nx, y2 + ny, 0).color(c);
            drawn++;
        }
        end(b);
    }

    private static void rq(BufferBuilder b, Matrix4f m, float x1, float y1, float x2, float y2, int c) {
        if (x2 <= x1 || y2 <= y1) return;
        b.vertex(m, x1, y1, 0).color(c);
        b.vertex(m, x1, y2, 0).color(c);
        b.vertex(m, x2, y2, 0).color(c);
        b.vertex(m, x2, y1, 0).color(c);
    }

    /** pionowy gradient (góra -> dół) */
    public static void gradientV(DrawContext ctx, float x, float y, float w, float h, int top, int bottom) {
        if (shape(ctx, x, y, w, h, 0, 0, 0, col(top), col(top), col(bottom), col(bottom))) return;
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        blend();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int t = col(top), bt = col(bottom);
        b.vertex(m, x, y, 0).color(t);
        b.vertex(m, x, y + h, 0).color(bt);
        b.vertex(m, x + w, y + h, 0).color(bt);
        b.vertex(m, x + w, y, 0).color(t);
        end(b);
    }

    /** pozioma gradientowa belka (np. pasek postępu) */
    public static void gradientH(DrawContext ctx, float x, float y, float w, float h, int left, int right) {
        if (shape(ctx, x, y, w, h, 0, 0, 0, col(left), col(right), col(right), col(left))) return;
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        blend();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int l = col(left), r = col(right);
        b.vertex(m, x, y, 0).color(l);
        b.vertex(m, x, y + h, 0).color(l);
        b.vertex(m, x + w, y + h, 0).color(r);
        b.vertex(m, x + w, y, 0).color(r);
        end(b);
    }

    public static void circle(DrawContext ctx, float cx, float cy, float radius, int color) {
        float rm = radiusMul;
        radiusMul = 1f; // koło zawsze okrągłe
        roundRect(ctx, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
        radiusMul = rm;
    }

    /** miękki cień pod prostokątem */
    public static void shadow(DrawContext ctx, float x, float y, float w, float h, float spread, int color) {
        int c = col(color);
        if ((c >>> 24) == 0) return;
        if (shape(ctx, x, y, w, h, Math.min(Math.min(w, h) / 2f, Math.max(8, spread * 0.75f) * radiusMul), 0, spread * 0.6f, c, c, c, c)) return;
        spread = Math.min(spread, Math.min(w, h) / 2f);
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder b = begin(SHADOW);
        nineSlice(b, m, x - spread, y - spread, w + spread * 2, h + spread * 2, spread * 2, spread, c);
        end(b);
    }

    public static void texture(DrawContext ctx, Identifier tex, float x, float y, float w, float h, int color) {
        int c = col(color);
        if ((c >>> 24) == 0) return;
        if (tex(ctx, tex, x, y, w, h, 0, 0, 0, 1, 1, c, false)) return;
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder b = begin(tex);
        quad(b, m, x, y, x + w, y + h, 0, 0, 1, 1, c);
        end(b);
    }

    /** tekstura przycięta do zaokrąglonego prostokąta (wachlarz trójkątów) */
    public static void roundTexture(DrawContext ctx, Identifier tex, float x, float y, float w, float h, float r,
                                    float u0, float v0, float u1, float v1, int color) {
        int c = col(color);
        if ((c >>> 24) == 0) return;
        r = Math.max(0, Math.min(r * radiusMul, Math.min(w, h) / 2f));
        if (tex(ctx, tex, x, y, w, h, Math.max(r, 0.001f), u0, v0, u1, v1, c, false)) return;
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        blend();
        if (worldDepth) { RenderSystem.enableDepthTest(); RenderSystem.depthMask(false); } else RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, tex);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_TEXTURE_COLOR);
        float cx = x + w / 2f, cy = y + h / 2f;
        b.vertex(m, cx, cy, 0).texture(u0 + (u1 - u0) / 2f, v0 + (v1 - v0) / 2f).color(c);
        float[][] corners = {{x + w - r, y + r, 270}, {x + w - r, y + h - r, 0}, {x + r, y + h - r, 90}, {x + r, y + r, 180}};
        int seg = 8;
        float fx = 0, fy = 0;
        boolean first = true;
        for (float[] cc : corners) {
            for (int i = 0; i <= seg; i++) {
                double a = Math.toRadians(cc[2] + 90.0 * i / seg);
                float px = cc[0] + (float) Math.cos(a) * r, py = cc[1] + (float) Math.sin(a) * r;
                if (first) { fx = px; fy = py; first = false; }
                b.vertex(m, px, py, 0).texture(u0 + (u1 - u0) * (px - x) / w, v0 + (v1 - v0) * (py - y) / h).color(c);
            }
        }
        b.vertex(m, fx, fy, 0).texture(u0 + (u1 - u0) * (fx - x) / w, v0 + (v1 - v0) * (fy - y) / h).color(c);
        end(b);
    }

    /** tekstura z dowolnymi UV (np. przewijane, powtarzane tło) */
    public static void textureUV(DrawContext ctx, Identifier tex, float x, float y, float w, float h,
                                 float u0, float v0, float u1, float v1, int color) {
        int c = col(color);
        if ((c >>> 24) == 0) return;
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder b = begin(tex);
        quad(b, m, x, y, x + w, y + h, u0, v0, u1, v1, c);
        end(b);
    }

    private static final Identifier GLOW_ADD = id("textures/ui/glow_add.png");

    /**
     * miękka poświata – rysowana addytywnie (ONE, ONE) z jasnością w RGB,
     * bo shader Minecrafta odrzuca piksele z alfą &lt; 0.1 (dawało to ostre krawędzie)
     */
    public static void glow(DrawContext ctx, float x, float y, float w, float h, int color) {
        int c = col(color);
        float a = ((c >>> 24) & 0xFF) / 255f;
        if (a <= 0.003f) return;
        int pm = 0xFF000000 | ((int) (((c >> 16) & 0xFF) * a) << 16) | ((int) (((c >> 8) & 0xFF) * a) << 8) | (int) ((c & 0xFF) * a);
        if (tex(ctx, GLOW_ADD, x, y, w, h, 0, 0, 0, 1, 1, pm, true)) return;
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder b = begin(GLOW_ADD);
        addBlend();
        quad(b, m, x, y, x + w, y + h, 0, 0, 1, 1, pm);
        end(b);
        blend();
    }

    public static void icon(DrawContext ctx, String name, float x, float y, float size, int color) {
        Identifier tex = ICONS.computeIfAbsent(name, n -> id("textures/icons/" + n + ".png"));
        texture(ctx, tex, x, y, size, size, color);
    }

    // ---------------------------------------------------------------- scissor

    public static void scissor(DrawContext ctx, float x, float y, float w, float h) {
        ctx.draw();
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        Vector4f a = m.transform(new Vector4f(x, y, 0, 1));
        Vector4f bb = m.transform(new Vector4f(x + w, y + h, 0, 1));
        double s = MinecraftClient.getInstance().getWindow().getScaleFactor();
        int fbH = MinecraftClient.getInstance().getWindow().getFramebufferHeight();
        int sx = (int) Math.floor(a.x * s), sy = (int) Math.floor(fbH - bb.y * s);
        int sw = (int) Math.ceil((bb.x - a.x) * s), sh = (int) Math.ceil((bb.y - a.y) * s);
        if (!SCISSORS.isEmpty()) { // przecięcie z poprzednim
            int[] p = SCISSORS.peek();
            int nx = Math.max(sx, p[0]), ny = Math.max(sy, p[1]);
            int nx2 = Math.min(sx + sw, p[0] + p[2]), ny2 = Math.min(sy + sh, p[1] + p[3]);
            sx = nx; sy = ny; sw = Math.max(0, nx2 - nx); sh = Math.max(0, ny2 - ny);
        }
        int[] r = {sx, sy, Math.max(0, sw), Math.max(0, sh)};
        SCISSORS.push(r);
        RenderSystem.enableScissor(r[0], r[1], r[2], r[3]);
    }

    public static void endScissor(DrawContext ctx) {
        ctx.draw();
        SCISSORS.pop();
        if (SCISSORS.isEmpty()) RenderSystem.disableScissor();
        else { int[] p = SCISSORS.peek(); RenderSystem.enableScissor(p[0], p[1], p[2], p[3]); }
    }
}
