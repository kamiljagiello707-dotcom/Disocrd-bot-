package dev.razer.client.mixin;

import dev.razer.client.module.Legit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NoFire: niższy i bardziej przezroczysty ogień na ekranie. */
@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void razer$hide(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        if (Legit.hideFire(client)) ci.cancel();
    }

    @ModifyConstant(method = "renderFireOverlay", constant = @Constant(floatValue = 0.9f))
    private static float razer$alpha(float a) { return Legit.fireAlpha(a); }

    @ModifyArg(method = "renderFireOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V", ordinal = 0), index = 1)
    private static float razer$height(float y) { return y - Legit.fireOffset(); }
}
