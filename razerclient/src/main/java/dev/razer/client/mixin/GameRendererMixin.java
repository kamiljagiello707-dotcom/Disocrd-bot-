package dev.razer.client.mixin;

import dev.razer.client.module.Legit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom, NoHurtCam (siła trzęsienia, bobbing, animacja totemu). */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow @Final MinecraftClient client;

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void razer$zoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        double d = Legit.fovDivisor();
        if (d != 1) cir.setReturnValue(cir.getReturnValue() / d);
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void razer$hand(Camera camera, float tickDelta, Matrix4f matrix, CallbackInfo ci) {
        if (Legit.hideHand()) ci.cancel();
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void razer$bob(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (Legit.noBobbing()) ci.cancel();
    }

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void razer$totem(ItemStack stack, CallbackInfo ci) {
        if (Legit.noTotem(stack)) ci.cancel();
    }

    /** vanilla tiltViewWhenHurt ze skalowaną siłą */
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void razer$hurt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        float k = Legit.hurtCamStrength();
        if (k == 1f) return;
        ci.cancel();
        if (!(client.getCameraEntity() instanceof LivingEntity le)) return;
        float g = le.hurtTime - tickDelta;
        if (le.isDead()) {
            float h = Math.min(le.deathTime + tickDelta, 20.0F);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(40.0F - 8000.0F / (h + 200.0F)));
        }
        if (g < 0 || k <= 0) return;
        g /= le.maxHurtTime;
        g = MathHelper.sin(g * g * g * g * (float) Math.PI);
        float h = le.getDamageTiltYaw();
        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(-h));
        float i = (float) (-g * 14.0 * client.options.getDamageTiltStrength().getValue() * k);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i));
        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(h));
    }
}
