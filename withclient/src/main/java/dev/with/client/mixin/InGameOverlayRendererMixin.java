package dev.with.client.mixin;

import dev.with.client.visual.V;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NoRender: ogień i woda na ekranie. */
@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void withclient$fire(MinecraftClient mc, MatrixStack ms, CallbackInfo ci) {
        if (V.hide("NoRender", "Ogień")) ci.cancel();
    }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    private static void withclient$water(MinecraftClient mc, MatrixStack ms, CallbackInfo ci) {
        if (V.hide("NoRender", "Woda")) ci.cancel();
    }
}
