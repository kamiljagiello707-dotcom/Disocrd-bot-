package dev.vesc.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderLayerSet;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;

/**
 * Warstwy tekstu dla czcionek VescClient (Inter TTF) z filtrowaniem liniowym.
 * Minecraft rysuje glify TTF z próbkowaniem "nearest", przez co mały tekst ma kanciaste, poszarpane krawędzie.
 */
public final class SmoothTextLayer extends RenderLayer {
    private SmoothTextLayer(String name, Runnable start, Runnable end) {
        super(name, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, VertexFormat.DrawMode.QUADS, 786432, false, true, start, end);
    }

    public static TextRenderLayerSet of(Identifier atlas) {
        return new TextRenderLayerSet(layer(atlas, 0), layer(atlas, 1), layer(atlas, 2));
    }

    /** mode: 0 = normalny, 1 = przez ściany (vanilla see-through), 2 = polygon offset */
    private static RenderLayer layer(Identifier atlas, int mode) {
        return new SmoothTextLayer("vescclient_text_" + mode + "_" + atlas, () -> {
            RenderSystem.setShader(() -> R2D.TEXT != null ? R2D.TEXT : GameRenderer.getRenderTypeTextIntensityProgram());
            AbstractTexture t = MinecraftClient.getInstance().getTextureManager().getTexture(atlas);
            t.setFilter(true, false);
            RenderSystem.setShaderTexture(0, t.getGlId());
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                    com.mojang.blaze3d.platform.GlStateManager.SrcFactor.ONE, com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();
            if (mode == 1) { RenderSystem.disableDepthTest(); RenderSystem.depthMask(false); }
            else { RenderSystem.enableDepthTest(); RenderSystem.depthFunc(515); }
            if (mode == 2) { RenderSystem.polygonOffset(-1.0F, -10.0F); RenderSystem.enablePolygonOffset(); }
        }, () -> {
            MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            if (mode == 1) { RenderSystem.enableDepthTest(); RenderSystem.depthMask(true); }
            if (mode == 2) { RenderSystem.polygonOffset(0.0F, 0.0F); RenderSystem.disablePolygonOffset(); }
        });
    }
}
