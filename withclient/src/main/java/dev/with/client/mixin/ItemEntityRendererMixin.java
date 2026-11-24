package dev.with.client.mixin;

import dev.with.client.visual.V;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ItemPhysics: upuszczone przedmioty leżą płasko na ziemi i obracają się w locie (tylko wygląd). */
@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
    @Shadow @Final private ItemRenderer itemRenderer;
    @Shadow @Final private Random random;

    @Inject(method = "render(Lnet/minecraft/entity/ItemEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true)
    private void withclient$physics(ItemEntity e, float yaw, float tickDelta, MatrixStack ms, VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        if (!V.on("ItemPhysics")) return;
        ItemStack stack = e.getStack();
        if (stack.isEmpty()) return;
        BakedModel model = itemRenderer.getModel(stack, e.getWorld(), null, e.getId());
        boolean depth = model.hasDepth();
        Transformation ground = model.getTransformation().ground;
        random.setSeed(ItemEntityRenderer.getSeed(stack));
        float age = e.getItemAge() + tickDelta;
        float spin = V.num("ItemPhysics", "Obrót", 1);
        float seedYaw = (e.getId() * 47 % 360);
        boolean flat = V.bo("ItemPhysics", "Leżą płasko");
        ms.push();
        if (e.isOnGround() || e.isTouchingWater() && e.getVelocity().lengthSquared() < 0.001) {
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(seedYaw));
            if (flat && !depth) {
                ms.translate(0, 0.02f, 0);
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
                ms.translate(0, -ground.translation.y(), 0);
            } else {
                ms.translate(0, -ground.translation.y() + ground.scale.y() * 0.5f, 0);
            }
        } else {
            ms.translate(0, 0.12f, 0);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(seedYaw + age * 12 * spin));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(age * 18 * spin));
            ms.translate(0, -ground.translation.y(), 0);
        }
        ItemEntityRenderer.renderStack(itemRenderer, ms, vcp, light, stack, model, depth, random);
        ms.pop();
        ci.cancel();
    }
}
