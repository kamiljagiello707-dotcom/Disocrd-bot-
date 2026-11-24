package dev.with.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.with.client.visual.View;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ambience / CustomSky: kolor mgły; NoRender: bez mgły terenu. */
@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    @Shadow private static float red;
    @Shadow private static float green;
    @Shadow private static float blue;

    @Inject(method = "render", at = @At("TAIL"))
    private static void withclient$fogColor(Camera camera, float tickDelta, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfo ci) {
        float[] c = View.fogColor(camera, world, tickDelta);
        if (c == null) return;
        red = c[0]; green = c[1]; blue = c[2];
        RenderSystem.clearColor(red, green, blue, 0);
    }

    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void withclient$noFog(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        if (!View.noFog(camera, fogType)) return;
        RenderSystem.setShaderFogStart(viewDistance * 4f);
        RenderSystem.setShaderFogEnd(viewDistance * 5f);
    }
}
