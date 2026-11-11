package tech.fuzir.client.mixin;

import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.fuzir.client.module.ModuleManager;
import tech.fuzir.client.module.impl.NoFireOverlay;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void fuzir$noFire(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (!ModuleManager.all().isEmpty() && ModuleManager.get(NoFireOverlay.class).isEnabled()) ci.cancel();
    }
}
