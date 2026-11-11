package tech.fuzir.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import tech.fuzir.client.util.ColorUtil;

/**
 * Immediate-mode 2D primitives rendered in {@link MatrixStack} space with the position-color shader:
 * rounded rectangles (solid / gradient), outlines, soft shadows / glows and circles.
 */
public final class RenderUtil {
    private static final int CORNER_SEGMENTS = 8;

    private RenderUtil() {
    }

    // ------------------------------------------------------------------ plain rectangles

    public static void rect(MatrixStack matrices, float x, float y, float width, float height, int color) {
        if (width <= 0 || height <= 0 || ColorUtil.alpha(color) == 0) return;
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = begin(VertexFormat.DrawMode.QUADS);
        vertex(buffer, matrix, x, y, color);
        vertex(buffer, matrix, x, y + height, color);
        vertex(buffer, matrix, x + width, y + height, color);
        vertex(buffer, matrix, x + width, y, color);
        end(buffer);
    }

    /** Horizontal gradient from {@code left} to {@code right}. */
    public static void horizontalGradient(MatrixStack matrices, float x, float y, float width, float height, int left, int right) {
        if (width <= 0 || height <= 0) return;
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = begin(VertexFormat.DrawMode.QUADS);
        vertex(buffer, matrix, x, y, left);
        vertex(buffer, matrix, x, y + height, left);
        vertex(buffer, matrix, x + width, y + height, right);
        vertex(buffer, matrix, x + width, y, right);
        end(buffer);
    }

    // ------------------------------------------------------------------ rounded rectangles

    public static void roundedRect(MatrixStack matrices, float x, float y, float width, float height, float radius, int color) {
        roundedRect(matrices, x, y, width, height, radius, radius, radius, radius, color);
    }

    public static void roundedRect(MatrixStack matrices, float x, float y, float width, float height,
                                   float topLeft, float topRight, float bottomRight, float bottomLeft, int color) {
        roundedGradient(matrices, x, y, width, height, topLeft, topRight, bottomRight, bottomLeft, color, color, false);
    }

    /**
     * Rounded rectangle with a two-stop gradient.
     *
     * @param horizontal {@code true} for left→right, {@code false} for top→bottom
     */
    public static void roundedGradient(MatrixStack matrices, float x, float y, float width, float height, float radius,
                                       int from, int to, boolean horizontal) {
        roundedGradient(matrices, x, y, width, height, radius, radius, radius, radius, from, to, horizontal);
    }

    public static void roundedGradient(MatrixStack matrices, float x, float y, float width, float height,
                                       float topLeft, float topRight, float bottomRight, float bottomLeft,
                                       int from, int to, boolean horizontal) {
        if (width <= 0 || height <= 0) return;
        if (ColorUtil.alpha(from) == 0 && ColorUtil.alpha(to) == 0) return;
        float[] path = path(x, y, width, height, topLeft, topRight, bottomRight, bottomLeft, 0f);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = begin(VertexFormat.DrawMode.TRIANGLE_FAN);
        vertex(buffer, matrix, x + width / 2f, y + height / 2f, ColorUtil.interpolate(from, to, 0.5));
        for (int i = 0; i <= path.length / 2; i++) {
            int index = (i % (path.length / 2)) * 2;
            float px = path[index];
            float py = path[index + 1];
            double t = horizontal ? (px - x) / width : (py - y) / height;
            vertex(buffer, matrix, px, py, ColorUtil.interpolate(from, to, t));
        }
        end(buffer);
    }

    public static void roundedOutline(MatrixStack matrices, float x, float y, float width, float height, float radius,
                                      float thickness, int color) {
        if (width <= 0 || height <= 0 || ColorUtil.alpha(color) == 0) return;
        float[] inner = path(x, y, width, height, radius, radius, radius, radius, 0f);
        float[] outer = path(x, y, width, height, radius, radius, radius, radius, thickness);
        strip(matrices, inner, outer, color, color);
    }

    /**
     * Soft drop shadow / glow: a gradient band that fades from {@code color} at the shape edge to fully
     * transparent {@code size} pixels outside of it.
     */
    public static void shadow(MatrixStack matrices, float x, float y, float width, float height, float radius,
                              float size, int color) {
        if (width <= 0 || height <= 0 || size <= 0 || ColorUtil.alpha(color) == 0) return;
        float[] inner = path(x, y, width, height, radius, radius, radius, radius, 0f);
        float[] outer = path(x, y, width, height, radius, radius, radius, radius, size);
        strip(matrices, inner, outer, color, ColorUtil.withAlpha(color, 0));
    }

    public static void circle(MatrixStack matrices, float centerX, float centerY, float radius, int color) {
        if (radius <= 0 || ColorUtil.alpha(color) == 0) return;
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int segments = Math.max(12, Math.min(48, (int) (radius * 6)));
        BufferBuilder buffer = begin(VertexFormat.DrawMode.TRIANGLE_FAN);
        vertex(buffer, matrix, centerX, centerY, color);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0 * i / segments;
            vertex(buffer, matrix, centerX + (float) Math.cos(angle) * radius, centerY + (float) Math.sin(angle) * radius, color);
        }
        end(buffer);
    }

    // ------------------------------------------------------------------ internals

    private static void strip(MatrixStack matrices, float[] inner, float[] outer, int innerColor, int outerColor) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = begin(VertexFormat.DrawMode.TRIANGLE_STRIP);
        int points = inner.length / 2;
        for (int i = 0; i <= points; i++) {
            int index = (i % points) * 2;
            vertex(buffer, matrix, inner[index], inner[index + 1], innerColor);
            vertex(buffer, matrix, outer[index], outer[index + 1], outerColor);
        }
        end(buffer);
    }

    /**
     * Clockwise (in screen space) outline of a rounded rectangle, optionally expanded outward by {@code expand}.
     * Every corner always emits the same number of points so paths of different sizes can be stitched together.
     */
    private static float[] path(float x, float y, float width, float height,
                                float topLeft, float topRight, float bottomRight, float bottomLeft, float expand) {
        float max = Math.min(width, height) / 2f;
        float[] radii = {
                Math.min(topLeft, max), Math.min(topRight, max), Math.min(bottomRight, max), Math.min(bottomLeft, max)
        };
        float[][] centers = {
                {x + radii[0], y + radii[0]},
                {x + width - radii[1], y + radii[1]},
                {x + width - radii[2], y + height - radii[2]},
                {x + radii[3], y + height - radii[3]}
        };
        float[] out = new float[4 * (CORNER_SEGMENTS + 1) * 2];
        int cursor = 0;
        for (int corner = 0; corner < 4; corner++) {
            float radius = radii[corner] + expand;
            double startAngle = Math.PI + corner * (Math.PI / 2.0);
            for (int i = 0; i <= CORNER_SEGMENTS; i++) {
                double angle = startAngle + (Math.PI / 2.0) * i / CORNER_SEGMENTS;
                out[cursor++] = centers[corner][0] + (float) Math.cos(angle) * radius;
                out[cursor++] = centers[corner][1] + (float) Math.sin(angle) * radius;
            }
        }
        return out;
    }

    private static BufferBuilder begin(VertexFormat.DrawMode mode) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(mode, VertexFormats.POSITION_COLOR);
        return buffer;
    }

    private static void end(BufferBuilder buffer) {
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, int color) {
        buffer.vertex(matrix, x, y, 0f)
                .color(ColorUtil.red(color), ColorUtil.green(color), ColorUtil.blue(color), ColorUtil.alpha(color))
                .next();
    }
}
