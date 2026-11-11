package tech.fuzir.client.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.fuzir.client.module.ModuleManager;
import tech.fuzir.client.module.impl.NoHurtCam;
import tech.fuzir.client.module.impl.Zoom;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void fuzir$zoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        // przybliżamy tylko świat, ręka zostaje bez zmian
        if (!changingFov || ModuleManager.all().isEmpty()) return;
        float d = ModuleManager.get(Zoom.class).fovDivisor();
        if (d != 1f) cir.setReturnValue(cir.getReturnValueF() / d);
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void fuzir$noHurtCam(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (!ModuleManager.all().isEmpty() && ModuleManager.get(NoHurtCam.class).isEnabled()) ci.cancel();
    }
}
