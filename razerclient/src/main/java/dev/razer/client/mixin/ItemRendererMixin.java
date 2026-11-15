package dev.razer.client.mixin;

import dev.razer.client.module.ToolColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Barwi miecze / narzędzia kolorem z modułu ToolColor (mnożenie koloru tekstury). */
@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    @Unique private static ToolColor razer$current;

    @Inject(method = "renderBakedItemQuads", at = @At("HEAD"))
    private void razer$begin(MatrixStack matrices, VertexConsumer vertices, List<BakedQuad> quads, ItemStack stack, int light, int overlay, CallbackInfo ci) {
        ToolColor tc = ToolColor.get();
        razer$current = tc != null && tc.affects(stack) ? tc : null;
    }

    @Redirect(method = "renderBakedItemQuads", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/model/BakedQuad;hasColor()Z"))
    private boolean razer$hasColor(BakedQuad quad) {
        return razer$current != null || quad.hasColor();
    }

    @Redirect(method = "renderBakedItemQuads", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/color/item/ItemColors;getColor(Lnet/minecraft/item/ItemStack;I)I"))
    private int razer$color(ItemColors colors, ItemStack stack, int tintIndex) {
        if (razer$current != null) return razer$current.tint();
        return colors.getColor(stack, tintIndex);
    }
}
