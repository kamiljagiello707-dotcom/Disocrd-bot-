package dev.with.client.mixin;

import dev.with.client.visual.View;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ViewModel (pozycja i skala przedmiotu w ręce) i SwingAnimation (własne machnięcia). */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Shadow private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {}

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void withclient$push(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item,
                                 float equipProgress, MatrixStack matrices, VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        matrices.push();
        View.viewModel(matrices, hand, player.getMainArm(), item);
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void withclient$pop(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item,
                                float equipProgress, MatrixStack matrices, VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        matrices.pop();
    }

    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    private void withclient$scale(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item,
                                  float equipProgress, MatrixStack matrices, VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        View.viewScale(matrices);
    }

    @Redirect(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applySwingOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V"))
    private void withclient$swing(HeldItemRenderer self, MatrixStack ms, Arm arm, float progress) {
        if (!View.swing(ms, arm, progress)) applySwingOffset(ms, arm, progress);
    }
}
