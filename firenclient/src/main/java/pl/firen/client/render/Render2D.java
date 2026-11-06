package pl.firen.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Rysowanie nowoczesnych kształtów 2D shaderem SDF: zaokrąglone prostokąty z antyaliasingiem,
 * gradienty, obrysy i miękkie cienie / poświaty.
 */
public final class Render2D {
    private Render2D() {}

    private static final ShaderProgramKey ROUNDED = new ShaderProgramKey(
            Identifier.of("firenclient", "core/rounded"), VertexFormats.POSITION_COLOR, Defines.EMPTY);

    /** Globalny mnożnik przezroczystości (animacja otwierania GUI). */
    public static float alpha = 1f;

    public static int a(int color) {
        int al = Math.round((color >>> 24) * alpha);
        return al << 24 | (color & 0xFFFFFF);
    }

    public static void round(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        draw(ctx, x, y, w, h, r, color, color, color, color, 0f, 0f);
    }

    public static void gradientV(DrawContext ctx, float x, float y, float w, float h, float r, int top, int bottom) {
        draw(ctx, x, y, w, h, r, top, top, bottom, bottom, 0f, 0f);
    }

    public static void gradientH(DrawContext ctx, float x, float y, float w, float h, float r, int left, int right) {
        draw(ctx, x, y, w, h, r, left, right, right, left, 0f, 0f);
    }

    public static void outline(DrawContext ctx, float x, float y, float w, float h, float r, float thickness, int color) {
        draw(ctx, x, y, w, h, r, color, color, color, color, 0f, thickness);
    }

    /** Miękki cień / poświata wokół prostokąta. */
    public static void shadow(DrawContext ctx, float x, float y, float w, float h, float r, float blur, int color) {
        draw(ctx, x, y, w, h, r, color, color, color, color, blur, 0f);
    }

    public static void circle(DrawContext ctx, float cx, float cy, float radius, int color) {
        round(ctx, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
    }

    private static void draw(DrawContext ctx, float x, float y, float w, float h, float r,
                             int cTL, int cTR, int cBR, int cBL, float softness, float thickness) {
        if (w <= 0 || h <= 0) return;
        if (((cTL | cTR | cBR | cBL) >>> 24) == 0) return;
        ctx.draw(); // zachowaj kolejność względem tekstu i elementów vanilla

        MinecraftClient mc = MinecraftClient.getInstance();
        double sf = mc.getWindow().getScaleFactor();
        Matrix4f mat = ctx.getMatrices().peek().getPositionMatrix();
        Vector4f p1 = mat.transform(new Vector4f(x, y, 0, 1));
        Vector4f p2 = mat.transform(new Vector4f(x + w, y + h, 0, 1));
        float unitPx = (float) ((p2.x - p1.x) / w * sf);
        if (unitPx <= 0) return;

        float pw = (float) ((p2.x - p1.x) * sf), ph = (float) ((p2.y - p1.y) * sf);
        float softPx = Math.max(softness * unitPx, 0.75f);
        float radiusPx = Math.min(r * unitPx, Math.min(pw, ph) / 2f);
        float pad = (softPx + 2f) / unitPx;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ShaderProgram shader = RenderSystem.setShader(ROUNDED);
        if (shader == null) return;
        set(shader, "Rect", new Vector4f((float) (p1.x * sf), (float) (p1.y * sf), pw, ph));
        set(shader, "Radius", radiusPx);
        set(shader, "Softness", softPx);
        set(shader, "Thickness", thickness > 0 ? Math.max(thickness * unitPx, 1f) : 0f);

        float x1 = x - pad, y1 = y - pad, x2 = x + w + pad, y2 = y + h + pad;
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bb.vertex(mat, x1, y1, 0).color(a(cTL));
        bb.vertex(mat, x1, y2, 0).color(a(cBL));
        bb.vertex(mat, x2, y2, 0).color(a(cBR));
        bb.vertex(mat, x2, y1, 0).color(a(cTR));
        BufferRenderer.drawWithGlobalProgram(bb.end());
        RenderSystem.disableBlend();
    }

    private static void set(ShaderProgram shader, String name, Vector4f v) {
        GlUniform u = shader.getUniform(name);
        if (u != null) u.set(v);
    }

    private static void set(ShaderProgram shader, String name, float v) {
        GlUniform u = shader.getUniform(name);
        if (u != null) u.set(v);
    }
}
