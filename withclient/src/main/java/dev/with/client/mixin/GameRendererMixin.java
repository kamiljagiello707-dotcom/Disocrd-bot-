package dev.with.client.mixin;

import dev.with.client.visual.V;
import dev.with.client.visual.View;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom, CustomFOV, NoHurtCam, ukrywanie animacji totemu, ręka przy zoomie i bez bujania ręki. */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow private float fovMultiplier;
    @Shadow private float lastFovMultiplier;
    @Shadow private boolean renderingPanorama;

    @Shadow private void bobView(MatrixStack matrices, float tickDelta) {}

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void withclient$fov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (renderingPanorama || !changingFov) return;
        cir.setReturnValue(View.fov(cir.getReturnValue(), MathHelper.lerp(tickDelta, lastFovMultiplier, fovMultiplier)));
    }

    @ModifyConstant(method = "tiltViewWhenHurt", constant = @Constant(doubleValue = 14.0))
    private double withclient$hurtCam(double v) {
        return V.on("NoHurtCam") ? v * V.num("NoHurtCam", "Siła", 0) / 100.0 : v;
    }

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void withclient$totem(ItemStack stack, CallbackInfo ci) {
        if (V.hide("NoRender", "Totem")) ci.cancel();
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void withclient$hand(Camera camera, float tickDelta, Matrix4f matrix, CallbackInfo ci) {
        if (View.hideHand()) ci.cancel();
    }

    @Redirect(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;bobView(Lnet/minecraft/client/util/math/MatrixStack;F)V"))
    private void withclient$handBob(GameRenderer self, MatrixStack ms, float td) {
        if (!(V.on("ViewModel") && V.bo("ViewModel", "Bez bujania"))) bobView(ms, td);
    }
}
