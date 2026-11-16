package dev.vesc.client.mixin;

import dev.vesc.client.VescClient;
import dev.vesc.client.module.camera.ViewModel;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ViewModel: przesunięcie / skala przedmiotu w ręce z pierwszej osoby. */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    private static ViewModel vesc$vm() {
        VescClient rc = VescClient.get();
        return rc != null && rc.modules != null && rc.modules.get("ViewModel") instanceof ViewModel v && v.isEnabled() ? v : null;
    }

    private static ItemStack vesc$item = ItemStack.EMPTY;

    /** SwordAnimations: własna animacja machnięcia zamiast vanilla */
    @org.spongepowered.asm.mixin.injection.Redirect(method = "renderFirstPersonItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applySwingOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V"))
    private void vesc$swing(HeldItemRenderer self, MatrixStack matrices, Arm arm, float swingProgress) {
        VescClient rc = VescClient.get();
        if (rc != null && rc.modules != null && rc.modules.get("SwordAnimations") instanceof dev.vesc.client.module.camera.SwordAnimations sa && sa.applies(vesc$item)) {
            sa.apply(matrices, arm, swingProgress);
        } else ((HeldItemRendererInvoker) self).vesc$applySwingOffset(matrices, arm, swingProgress);
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void vesc$push(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item,
                            float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        matrices.push();
        vesc$item = item;
        ViewModel v = vesc$vm();
        if (v == null || (hand == Hand.OFF_HAND && !v.offhand.get())) return;
        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate(v.x.get() * side, v.y.get(), v.z.get());
        float s = v.scale.get().floatValue();
        matrices.scale(s, s, s);
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void vesc$pop(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item,
                           float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        matrices.pop();
    }

    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private float vesc$equip(float equipProgress) {
        ViewModel v = vesc$vm();
        return v != null && v.noEquip.get() ? 0 : equipProgress;
    }
}
