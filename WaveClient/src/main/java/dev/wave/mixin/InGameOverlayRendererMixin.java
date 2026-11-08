package dev.wave.mixin;

import dev.wave.WaveClient;
import dev.wave.module.visuals.NoOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void wave$fire(MatrixStack matrices, VertexConsumerProvider vcp, CallbackInfo ci) {
        if (WaveClient.INSTANCE == null) return;
        NoOverlay m = WaveClient.INSTANCE.modules.get(NoOverlay.class);
        if (m.isEnabled() && m.fire.isOn()) ci.cancel();
    }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    private static void wave$water(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider vcp, CallbackInfo ci) {
        if (WaveClient.INSTANCE == null) return;
        NoOverlay m = WaveClient.INSTANCE.modules.get(NoOverlay.class);
        if (m.isEnabled() && m.water.isOn()) ci.cancel();
    }
}
