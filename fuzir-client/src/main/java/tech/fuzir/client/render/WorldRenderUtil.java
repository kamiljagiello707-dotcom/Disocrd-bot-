package tech.fuzir.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import tech.fuzir.client.util.ColorUtil;

import java.util.function.IntUnaryOperator;

/**
 * Camera-relative 3D primitives for ESP and world effects. ESP shapes ignore depth, world effects can use it.
 */
public final class WorldRenderUtil {
    private static final int RING_SEGMENTS = 72;

    private WorldRenderUtil() {
    }

    /**
     * Horizontal ring with a vertical gradient band.
     *
     * @param center    ring center in world space
     * @param bandOffset signed height of the fading band relative to the ring (negative = below)
     * @param colorAt   ring color for a segment index, allowing a gradient around the circumference
     * @param bandAlpha alpha of the band at the ring edge (fades to 0 at the band end)
     */
    public static void ring(MatrixStack matrices, Camera camera, Vec3d center, double radius, double bandOffset,
                            float lineHeight, IntUnaryOperator colorAt, int bandAlpha) {
        Vec3d c = center.subtract(camera.getPos());
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        BufferBuilder buffer = begin(VertexFormat.DrawMode.TRIANGLE_STRIP);
        for (int i = 0; i <= RING_SEGMENTS; i++) {
            double angle = Math.PI * 2.0 * i / RING_SEGMENTS;
            float x = (float) (c.x + Math.cos(angle) * radius);
            float z = (float) (c.z + Math.sin(angle) * radius);
            int color = colorAt.applyAsInt(i % RING_SEGMENTS);
            vertex(buffer, matrix, x, (float) c.y, z, ColorUtil.withAlpha(color, bandAlpha));
            vertex(buffer, matrix, x, (float) (c.y + bandOffset), z, ColorUtil.withAlpha(color, 0));
        }
        end(buffer);

        buffer = begin(VertexFormat.DrawMode.TRIANGLE_STRIP);
        for (int i = 0; i <= RING_SEGMENTS; i++) {
            double angle = Math.PI * 2.0 * i / RING_SEGMENTS;
            float x = (float) (c.x + Math.cos(angle) * radius);
            float z = (float) (c.z + Math.sin(angle) * radius);
            int color = colorAt.applyAsInt(i % RING_SEGMENTS);
            vertex(buffer, matrix, x, (float) c.y - lineHeight / 2f, z, color);
            vertex(buffer, matrix, x, (float) c.y + lineHeight / 2f, z, color);
        }
        end(buffer);
    }

    /**
     * Flat annulus lying on the XZ plane. {@code colorAt(segment)} gives the outer color; the inner edge
     * uses the same color multiplied by {@code innerAlpha}, which produces a soft glow towards the center.
     */
    public static void flatRing(MatrixStack matrices, Camera camera, Vec3d center, double innerRadius,
                                double outerRadius, IntUnaryOperator colorAt, float innerAlpha, boolean depthTest) {
        Vec3d c = center.subtract(camera.getPos());
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = begin(VertexFormat.DrawMode.TRIANGLE_STRIP, depthTest);
        for (int i = 0; i <= RING_SEGMENTS; i++) {
            double angle = Math.PI * 2.0 * i / RING_SEGMENTS;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            int color = colorAt.applyAsInt(i % RING_SEGMENTS);
            vertex(buffer, matrix, (float) (c.x + cos * innerRadius), (float) c.y, (float) (c.z + sin * innerRadius),
                    ColorUtil.multiplyAlpha(color, innerAlpha));
            vertex(buffer, matrix, (float) (c.x + cos * outerRadius), (float) c.y, (float) (c.z + sin * outerRadius), color);
        }
        end(buffer);
    }

    /**
     * Cone with its apex above the base circle. Base vertices take {@code colorAt(segment)}, the apex uses
     * {@code apexColor}, giving a vertical gradient.
     */
    public static void cone(MatrixStack matrices, Camera camera, Vec3d baseCenter, double radius, double height,
                            IntUnaryOperator colorAt, int apexColor) {
        Vec3d c = baseCenter.subtract(camera.getPos());
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = begin(VertexFormat.DrawMode.TRIANGLE_FAN, true);
        vertex(buffer, matrix, (float) c.x, (float) (c.y + height), (float) c.z, apexColor);
        for (int i = 0; i <= RING_SEGMENTS; i++) {
            double angle = Math.PI * 2.0 * i / RING_SEGMENTS;
            vertex(buffer, matrix, (float) (c.x + Math.cos(angle) * radius), (float) c.y,
                    (float) (c.z + Math.sin(angle) * radius), colorAt.applyAsInt(i % RING_SEGMENTS));
        }
        end(buffer);

        // Rim line for a crisp edge.
        buffer = begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, true);
        for (int i = 0; i <= RING_SEGMENTS; i++) {
            double angle = Math.PI * 2.0 * i / RING_SEGMENTS;
            int color = colorAt.applyAsInt(i % RING_SEGMENTS);
            vertex(buffer, matrix, (float) (c.x + Math.cos(angle) * radius), (float) c.y,
                    (float) (c.z + Math.sin(angle) * radius), ColorUtil.withAlpha(color, Math.min(255, ColorUtil.alpha(color) * 2)));
        }
        end(buffer);
    }

    /** Translucent filled box plus outline. */
    public static void box(MatrixStack matrices, Camera camera, Box worldBox, int fill, int outline) {
        Box b = worldBox.offset(camera.getPos().negate());
        Matrix4f m = matrices.peek().getPositionMatrix();
        float x1 = (float) b.minX, y1 = (float) b.minY, z1 = (float) b.minZ;
        float x2 = (float) b.maxX, y2 = (float) b.maxY, z2 = (float) b.maxZ;

        BufferBuilder buffer = begin(VertexFormat.DrawMode.QUADS);
        quad(buffer, m, fill, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
        quad(buffer, m, fill, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
        quad(buffer, m, fill, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
        quad(buffer, m, fill, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
        quad(buffer, m, fill, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1);
        quad(buffer, m, fill, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2);
        end(buffer);

        buffer = begin(VertexFormat.DrawMode.DEBUG_LINES);
        float[][] corners = {
                {x1, y1, z1}, {x2, y1, z1}, {x2, y1, z2}, {x1, y1, z2},
                {x1, y2, z1}, {x2, y2, z1}, {x2, y2, z2}, {x1, y2, z2}
        };
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] edge : edges) {
            float[] a = corners[edge[0]];
            float[] e = corners[edge[1]];
            vertex(buffer, m, a[0], a[1], a[2], outline);
            vertex(buffer, m, e[0], e[1], e[2], outline);
        }
        end(buffer);
    }

    private static void quad(BufferBuilder buffer, Matrix4f m, int color,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        vertex(buffer, m, ax, ay, az, color);
        vertex(buffer, m, bx, by, bz, color);
        vertex(buffer, m, cx, cy, cz, color);
        vertex(buffer, m, dx, dy, dz, color);
    }

    private static BufferBuilder begin(VertexFormat.DrawMode mode) {
        return begin(mode, false);
    }

    private static BufferBuilder begin(VertexFormat.DrawMode mode, boolean depthTest) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        if (depthTest) RenderSystem.enableDepthTest();
        else RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(mode, VertexFormats.POSITION_COLOR);
        return buffer;
    }

    private static void end(BufferBuilder buffer) {
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, int color) {
        buffer.vertex(matrix, x, y, z)
                .color(ColorUtil.red(color), ColorUtil.green(color), ColorUtil.blue(color), ColorUtil.alpha(color))
                .next();
    }
}
