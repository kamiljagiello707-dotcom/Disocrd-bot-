package pl.mysticclient.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Proste rysowanie linii/boxów w świecie (widoczne przez ściany). */
public final class Render3D {
    private Render3D() {}

    /**
     * W 1.21.4 stos model-view zawiera już obrót kamery podczas WorldRenderEvents.LAST,
     * więc wierzchołki podajemy tylko względem pozycji kamery.
     */
    public static Matrix4f matrix(WorldRenderContext ctx) {
        return new Matrix4f();
    }

    public static BufferBuilder beginLines() {
        return Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
    }

    public static BufferBuilder beginQuads() {
        return Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
    }

    public static void line(BufferBuilder b, Matrix4f m, Vec3d cam, Vec3d p1, Vec3d p2, int color) {
        b.vertex(m, (float) (p1.x - cam.x), (float) (p1.y - cam.y), (float) (p1.z - cam.z)).color(color);
        b.vertex(m, (float) (p2.x - cam.x), (float) (p2.y - cam.y), (float) (p2.z - cam.z)).color(color);
    }

    public static void boxLines(BufferBuilder b, Matrix4f m, Vec3d cam, Box box, int color) {
        float x1 = (float) (box.minX - cam.x), y1 = (float) (box.minY - cam.y), z1 = (float) (box.minZ - cam.z);
        float x2 = (float) (box.maxX - cam.x), y2 = (float) (box.maxY - cam.y), z2 = (float) (box.maxZ - cam.z);
        float[][] edges = {
                {x1, y1, z1, x2, y1, z1}, {x2, y1, z1, x2, y1, z2}, {x2, y1, z2, x1, y1, z2}, {x1, y1, z2, x1, y1, z1},
                {x1, y2, z1, x2, y2, z1}, {x2, y2, z1, x2, y2, z2}, {x2, y2, z2, x1, y2, z2}, {x1, y2, z2, x1, y2, z1},
                {x1, y1, z1, x1, y2, z1}, {x2, y1, z1, x2, y2, z1}, {x2, y1, z2, x2, y2, z2}, {x1, y1, z2, x1, y2, z2}
        };
        for (float[] e : edges) {
            b.vertex(m, e[0], e[1], e[2]).color(color);
            b.vertex(m, e[3], e[4], e[5]).color(color);
        }
    }

    public static void boxFill(BufferBuilder b, Matrix4f m, Vec3d cam, Box box, int color) {
        float x1 = (float) (box.minX - cam.x), y1 = (float) (box.minY - cam.y), z1 = (float) (box.minZ - cam.z);
        float x2 = (float) (box.maxX - cam.x), y2 = (float) (box.maxY - cam.y), z2 = (float) (box.maxZ - cam.z);
        quad(b, m, color, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
        quad(b, m, color, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
        quad(b, m, color, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
        quad(b, m, color, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
        quad(b, m, color, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1);
        quad(b, m, color, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2);
    }

    private static void quad(BufferBuilder b, Matrix4f m, int c, float... v) {
        for (int i = 0; i < 12; i += 3) b.vertex(m, v[i], v[i + 1], v[i + 2]).color(c);
    }

    public static void draw(BufferBuilder b) {
        BuiltBuffer built = b.endNullable();
        if (built == null) return;
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        BufferRenderer.drawWithGlobalProgram(built);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
