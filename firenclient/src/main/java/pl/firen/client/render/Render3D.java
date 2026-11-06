package pl.firen.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Proste rysowanie w świecie (współrzędne względem kamery): pudełka, linie, okręgi. Widoczne przez ściany. */
public final class Render3D {
    private Render3D() {}

    public static void begin() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
    }

    public static void end() {
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1f);
    }

    private static int r(int c) { return c >> 16 & 255; }
    private static int g(int c) { return c >> 8 & 255; }
    private static int b(int c) { return c & 255; }
    private static int a(int c) { return c >>> 24; }

    public static void boxFill(MatrixStack ms, Box box, int color) {
        Matrix4f m = ms.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;
        int R = r(color), G = g(color), B = b(color), A = a(color);
        float[][] q = {
                {x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2}, {x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1},
                {x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1}, {x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2},
                {x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1}, {x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2}};
        for (float[] f : q) {
            for (int i = 0; i < 12; i += 3) bb.vertex(m, f[i], f[i + 1], f[i + 2]).color(R, G, B, A);
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    public static void boxLines(MatrixStack ms, Box box, int color, float width) {
        double x1 = box.minX, y1 = box.minY, z1 = box.minZ, x2 = box.maxX, y2 = box.maxY, z2 = box.maxZ;
        double[][] e = {
                {x1, y1, z1, x2, y1, z1}, {x2, y1, z1, x2, y1, z2}, {x2, y1, z2, x1, y1, z2}, {x1, y1, z2, x1, y1, z1},
                {x1, y2, z1, x2, y2, z1}, {x2, y2, z1, x2, y2, z2}, {x2, y2, z2, x1, y2, z2}, {x1, y2, z2, x1, y2, z1},
                {x1, y1, z1, x1, y2, z1}, {x2, y1, z1, x2, y2, z1}, {x2, y1, z2, x2, y2, z2}, {x1, y1, z2, x1, y2, z2}};
        lines(ms, e, color, width);
    }

    public static void line(MatrixStack ms, Vec3d a, Vec3d b, int color, float width) {
        lines(ms, new double[][]{{a.x, a.y, a.z, b.x, b.y, b.z}}, color, width);
    }

    /** Grube linie (shader linii Minecrafta, działa też w profilu core). */
    public static void lines(MatrixStack ms, double[][] segs, int color, float width) {
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        RenderSystem.lineWidth(width);
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        MatrixStack.Entry entry = ms.peek();
        Matrix4f m = entry.getPositionMatrix();
        int R = r(color), G = g(color), B = b(color), A = a(color);
        for (double[] s : segs) {
            float dx = (float) (s[3] - s[0]), dy = (float) (s[4] - s[1]), dz = (float) (s[5] - s[2]);
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1e-6) continue;
            dx /= len; dy /= len; dz /= len;
            bb.vertex(m, (float) s[0], (float) s[1], (float) s[2]).color(R, G, B, A).normal(entry, dx, dy, dz);
            bb.vertex(m, (float) s[3], (float) s[4], (float) s[5]).color(R, G, B, A).normal(entry, dx, dy, dz);
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    /** Pozioma "wstęga" okręgu: jasna krawędź u dołu, zanikająca ku górze (o height). */
    public static void ringBand(MatrixStack ms, double radius, float height, int color, int segments) {
        Matrix4f m = ms.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        int R = r(color), G = g(color), B = b(color), A = a(color);
        for (int i = 0; i <= segments; i++) {
            double ang = i / (double) segments * Math.PI * 2;
            float x = (float) (Math.cos(ang) * radius), z = (float) (Math.sin(ang) * radius);
            bb.vertex(m, x, 0, z).color(R, G, B, A);
            bb.vertex(m, x, height, z).color(R, G, B, 0);
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    /** Płaski pierścień (od rIn do rOut) na wysokości 0. */
    public static void ring(MatrixStack ms, double rIn, double rOut, int colorIn, int colorOut, int segments) {
        Matrix4f m = ms.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double ang = i / (double) segments * Math.PI * 2;
            float c = (float) Math.cos(ang), s = (float) Math.sin(ang);
            bb.vertex(m, (float) (c * rIn), 0, (float) (s * rIn)).color(r(colorIn), g(colorIn), b(colorIn), a(colorIn));
            bb.vertex(m, (float) (c * rOut), 0, (float) (s * rOut)).color(r(colorOut), g(colorOut), b(colorOut), a(colorOut));
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    /** Okrągły "punkt" zwrócony do kamery (billboard). right/up to wektory kamery. */
    public static void dot(MatrixStack ms, Vec3d c, float radius, int color, org.joml.Vector3f right, org.joml.Vector3f up) {
        Matrix4f m = ms.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        int R = r(color), G = g(color), B = b(color), A = a(color);
        bb.vertex(m, (float) c.x, (float) c.y, (float) c.z).color(R, G, B, A);
        for (int i = 0; i <= 14; i++) {
            double ang = i / 14.0 * Math.PI * 2;
            float cx = (float) Math.cos(ang) * radius, cy = (float) Math.sin(ang) * radius;
            bb.vertex(m, (float) c.x + right.x * cx + up.x * cy, (float) c.y + right.y * cx + up.y * cy,
                    (float) c.z + right.z * cx + up.z * cy).color(R, G, B, 0);
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());
    }
}
