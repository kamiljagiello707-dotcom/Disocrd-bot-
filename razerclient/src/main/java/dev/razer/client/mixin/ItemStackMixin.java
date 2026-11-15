package dev.razer.client.mixin;

import dev.razer.client.module.ToolColor;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Opcjonalny połysk (glint) na kolorowanych narzędziach – tylko wygląd po stronie klienta. */
@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = "hasGlint", at = @At("HEAD"), cancellable = true)
    private void razer$glint(CallbackInfoReturnable<Boolean> cir) {
        ToolColor tc = ToolColor.get();
        if (tc != null && tc.glint.get() && tc.affects((ItemStack) (Object) this)) cir.setReturnValue(true);
    }
}
