package dev.vesc.client.mixin;

import dev.vesc.client.render.NametagRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ukrywa nicki Minecrafta nad graczami, gdy włączone są Nametags VescClient. */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void vesc$hideLabel(Entity entity, Text text, MatrixStack matrices, VertexConsumerProvider vcp, int light, float tickDelta, CallbackInfo ci) {
        if (entity instanceof PlayerEntity && NametagRenderer.active()) ci.cancel();
    }
}
