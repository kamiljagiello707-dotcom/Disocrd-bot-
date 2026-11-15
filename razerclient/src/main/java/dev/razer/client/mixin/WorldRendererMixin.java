package dev.razer.client.mixin;

import dev.razer.client.render.SkyRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Podmienia niebo, gdy moduł Sky jest włączony. */
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void razer$sky(Matrix4f matrix4f, Matrix4f projection, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo ci) {
        if (!SkyRenderer.active(camera)) return;
        SkyRenderer.render(matrix4f);
        ci.cancel();
    }
}
