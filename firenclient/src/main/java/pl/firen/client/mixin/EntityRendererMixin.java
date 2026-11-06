package pl.firen.client.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.firen.client.FirenClient;
import pl.firen.client.module.modules.Nametags;

/** Ukrywa vanilla nametagi graczy, gdy działa moduł Nametags. */
@Mixin(EntityRenderer.class)
abstract class EntityRendererMixin {
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void firen$hideLabel(EntityRenderState state, Text text, MatrixStack matrices,
                                 VertexConsumerProvider consumers, int light, CallbackInfo ci) {
        if (state instanceof PlayerEntityRenderState && FirenClient.modules() != null
                && FirenClient.modules().get(Nametags.class).isEnabled()) ci.cancel();
    }
}
