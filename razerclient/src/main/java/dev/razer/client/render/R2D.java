package dev.razer.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
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

    private static final int[] CIRCLE_SIZES = {16, 32, 64, 128, 256};
    private static final Identifier[] CIRCLES = new Identifier[CIRCLE_SIZES.length];
    private static final Identifier SHADOW = id("textures/ui/shadow.png");
    private static final Map<String, Identifier> ICONS = new HashMap<>();
    private static final Deque<int[]> SCISSORS = new ArrayDeque<>();

    static {
        for (int i = 0; i < CIRCLE_SIZES.length; i++) CIRCLES[i] = id("textures/ui/circle_" + CIRCLE_SIZES[i] + ".png");
    }

    public static Identifier id(String path) { return Identifier.of("razerclient", path); }

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
        RenderSystem.defaultBlendFunc();
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

    public static void roundRect(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        int c = col(color);
        if ((c >>> 24) == 0 || w <= 0 || h <= 0) return;
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
        if (w <= 0 || h <= 0) return;
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

    public static void rect(DrawContext ctx, float x, float y, float w, float h, int color) {
        roundRect(ctx, x, y, w, h, 0, color);
    }

    /** obramowanie o grubości t z zaokrąglonymi rogami */
    public static void roundOutline(DrawContext ctx, float x, float y, float w, float h, float r, float t, int color) {
        int c = col(color);
        if ((c >>> 24) == 0) return;
        r = Math.max(t, Math.min(r, Math.min(w, h) / 2f));
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
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

    private static void rq(BufferBuilder b, Matrix4f m, float x1, float y1, float x2, float y2, int c) {
        if (x2 <= x1 || y2 <= y1) return;
        b.vertex(m, x1, y1, 0).color(c);
        b.vertex(m, x1, y2, 0).color(c);
        b.vertex(m, x2, y2, 0).color(c);
        b.vertex(m, x2, y1, 0).color(c);
    }

    /** pozioma gradientowa belka (np. pasek postępu) */
    public static void gradientH(DrawContext ctx, float x, float y, float w, float h, int left, int right) {
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
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
        roundRect(ctx, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
    }

    /** miękki cień pod prostokątem */
    public static void shadow(DrawContext ctx, float x, float y, float w, float h, float spread, int color) {
        int c = col(color);
        if ((c >>> 24) == 0) return;
        spread = Math.min(spread, Math.min(w, h) / 2f);
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder b = begin(SHADOW);
        nineSlice(b, m, x - spread, y - spread, w + spread * 2, h + spread * 2, spread * 2, spread, c);
        end(b);
    }

    public static void texture(DrawContext ctx, Identifier tex, float x, float y, float w, float h, int color) {
        int c = col(color);
        if ((c >>> 24) == 0) return;
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
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2f));
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
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
