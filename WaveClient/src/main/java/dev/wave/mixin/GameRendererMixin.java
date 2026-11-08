package dev.wave.mixin;

import dev.wave.WaveClient;
import dev.wave.module.visuals.NoHurtCam;
import dev.wave.module.visuals.Zoom;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void wave$noHurtCam(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (WaveClient.INSTANCE != null && WaveClient.INSTANCE.modules.get(NoHurtCam.class).isEnabled()) ci.cancel();
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void wave$zoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        if (WaveClient.INSTANCE == null) return;
        cir.setReturnValue(WaveClient.INSTANCE.modules.get(Zoom.class).applyFov(cir.getReturnValue()));
    }
}
