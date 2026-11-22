package dev.with.client.mixin;

import dev.with.client.module.Module;
import dev.with.client.module.Modules;
import dev.with.client.module.Setting;
import dev.with.client.render.R2D;
import dev.with.client.render.WorldFx;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** SwordColor: barwi miecze (i opcjonalnie topory) – w ręce, na pasku i na ziemi. */
@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    @Unique private static ModelTransformationMode withclient$mode = ModelTransformationMode.NONE;
    @Unique private static ItemStack withclient$stack = ItemStack.EMPTY;

    @Inject(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V", at = @At("HEAD"))
    private void withclient$mode(ItemStack stack, ModelTransformationMode mode, boolean left, MatrixStack ms, VertexConsumerProvider vcp, int light, int overlay, BakedModel model, CallbackInfo ci) {
        withclient$mode = mode;
    }

    @Inject(method = "renderBakedItemQuads", at = @At("HEAD"))
    private void withclient$stack(MatrixStack ms, VertexConsumer vc, List<BakedQuad> quads, ItemStack stack, int light, int overlay, CallbackInfo ci) {
        withclient$stack = stack;
    }

    @Redirect(method = "renderBakedItemQuads", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumer;quad(Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/client/render/model/BakedQuad;FFFFII)V"))
    private void withclient$tint(VertexConsumer vc, MatrixStack.Entry entry, BakedQuad quad, float r, float g, float b, float a, int light, int overlay) {
        int c = withclient$color(withclient$stack, withclient$mode);
        if (c != 0) { r *= ((c >> 16) & 0xFF) / 255f; g *= ((c >> 8) & 0xFF) / 255f; b *= (c & 0xFF) / 255f; }
        vc.quad(entry, quad, r, g, b, a, light, overlay);
    }

    @Unique
    private static int withclient$color(ItemStack st, ModelTransformationMode mode) {
        if (st == null || st.isEmpty()) return 0;
        Module m = Modules.get("SwordColor");
        if (m == null || !m.isEnabled()) return 0;
        boolean axes = m.setting("Topory") instanceof Setting.Bool bt && bt.get();
        if (!st.isIn(ItemTags.SWORDS) && !(axes && st.isIn(ItemTags.AXES))) return 0;
        String where = switch (mode) {
            case GUI -> "Na pasku";
            case GROUND, FIXED -> "Upuszczone";
            default -> "W ręce";
        };
        if (!(m.setting(where) instanceof Setting.Bool w && w.get())) return 0;
        int c1 = m.setting("Kolor") instanceof Setting.Color c ? c.get() : 0xFF7DA7FF;
        int c2 = m.setting("Kolor 2") instanceof Setting.Color c ? c.get() : 0xFFFFFFFF;
        String tryb = m.setting("Tryb") instanceof Setting.Mode o ? o.get() : "Static";
        return switch (tryb) {
            case "Gradient" -> R2D.blend(c1, c2, 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 600.0));
            case "Rainbow" -> WorldFx.rainbow(2, 0);
            default -> c1;
        };
    }
}
