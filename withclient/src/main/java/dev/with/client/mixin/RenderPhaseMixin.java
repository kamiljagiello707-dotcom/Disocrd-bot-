package dev.with.client.mixin;

import dev.with.client.visual.V;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** ItemGlint: prędkość połysku zaklęć. */
@Mixin(RenderPhase.class)
public class RenderPhaseMixin {
    @Redirect(method = "setupGlintTexturing", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"))
    private static Object withclient$glintSpeed(SimpleOption<?> option) {
        Object v = option.getValue();
        if (V.on("ItemGlint") && v instanceof Double d) return d * V.num("ItemGlint", "Prędkość", 1);
        return v;
    }
}
