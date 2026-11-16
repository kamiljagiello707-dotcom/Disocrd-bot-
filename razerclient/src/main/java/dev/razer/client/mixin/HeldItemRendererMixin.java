package dev.razer.client.mixin;

import dev.razer.client.RazerClient;
import dev.razer.client.module.ViewModel;
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
    private static ViewModel razer$vm() {
        RazerClient rc = RazerClient.get();
        return rc != null && rc.modules != null && rc.modules.get("ViewModel") instanceof ViewModel v && v.isEnabled() ? v : null;
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void razer$push(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item,
                            float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        matrices.push();
        ViewModel v = razer$vm();
        if (v == null || (hand == Hand.OFF_HAND && !v.offhand.get())) return;
        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate(v.x.get() * side, v.y.get(), v.z.get());
        float s = v.scale.get().floatValue();
        matrices.scale(s, s, s);
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void razer$pop(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item,
                           float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        matrices.pop();
    }

    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private float razer$equip(float equipProgress) {
        ViewModel v = razer$vm();
        return v != null && v.noEquip.get() ? 0 : equipProgress;
    }
}
