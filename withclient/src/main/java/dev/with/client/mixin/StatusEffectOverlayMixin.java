package dev.with.client.mixin;

import dev.with.client.hud.Hud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gdy włączony jest element HUD "Potions", chowa ikony efektów Minecrafta w prawym górnym rogu (żeby się nie dublowały). */
@Mixin(InGameHud.class)
public class StatusEffectOverlayMixin {
    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void withclient$effects(DrawContext ctx, RenderTickCounter tc, CallbackInfo ci) {
        if (Hud.hideVanillaEffects()) ci.cancel();
    }
}
