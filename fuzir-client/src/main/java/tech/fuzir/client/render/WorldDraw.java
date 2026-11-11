package tech.fuzir.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

/** Proste kształty w świecie gry: pierścienie i stożki z przezroczystością. */
public final class WorldDraw {
    private static final int SEGMENTS = 72;

    private WorldDraw() {
    }

    public static void begin() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
    }

    public static void end() {
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** Płaski pierścień leżący poziomo; kolory: wewnętrzna i zewnętrzna krawędź. */
    public static void ring(Matrix4f m, float x, float y, float z, float inner, float outer, int cIn, int cOut) {
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= SEGMENTS; i++) {
            double a = Math.PI * 2 * i / SEGMENTS;
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            b.vertex(m, x + cos * inner, y, z + sin * inner).color(cIn);
            b.vertex(m, x + cos * outer, y, z + sin * outer).color(cOut);
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    /** Stożek: wierzchołek nad środkiem podstawy. */
    public static void cone(Matrix4f m, float x, float y, float z, float radius, float height, int cTop, int cBase) {
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        b.vertex(m, x, y + height, z).color(cTop);
        for (int i = 0; i <= SEGMENTS; i++) {
            double a = Math.PI * 2 * i / SEGMENTS;
            b.vertex(m, x + (float) Math.cos(a) * radius, y, z + (float) Math.sin(a) * radius).color(cBase);
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
    }
}
