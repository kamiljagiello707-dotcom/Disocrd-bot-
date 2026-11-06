package pl.firen.client.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.firen.client.module.modules.NoHurtCam;
import pl.firen.client.module.modules.Zoom;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void firen$zoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        float d = Zoom.divisor();
        if (d > 1.001f) cir.setReturnValue(cir.getReturnValue() / d);
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void firen$noHurtCam(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (NoHurtCam.active()) ci.cancel();
    }
}
