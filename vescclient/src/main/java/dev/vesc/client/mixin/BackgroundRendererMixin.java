package dev.vesc.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.vesc.client.render.SkyRenderer;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dopasowuje kolor mgły i tła do własnego nieba. */
@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    @Shadow private static float red;
    @Shadow private static float green;
    @Shadow private static float blue;

    @Inject(method = "render", at = @At("TAIL"))
    private static void vesc$fog(Camera camera, float tickDelta, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfo ci) {
        if (!SkyRenderer.active(camera) || SkyRenderer.module() == null || !SkyRenderer.module().fog.get()) return;
        int c = SkyRenderer.horizonColor();
        red = ((c >> 16) & 0xFF) / 255f;
        green = ((c >> 8) & 0xFF) / 255f;
        blue = (c & 0xFF) / 255f;
        RenderSystem.clearColor(red, green, blue, 0);
    }
}
