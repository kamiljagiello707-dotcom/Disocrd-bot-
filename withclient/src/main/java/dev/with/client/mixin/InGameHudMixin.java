package dev.with.client.mixin;

import dev.with.client.hud.Hud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ikony efektów (gdy włączony element Potions) i pasek przedmiotów (moduł Hotbar) rysujemy po swojemu. */
@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void withclient$effects(DrawContext ctx, RenderTickCounter tc, CallbackInfo ci) {
        if (Hud.hideVanillaEffects()) ci.cancel();
    }

    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void withclient$bars(DrawContext ctx, CallbackInfo ci) {
        if (Hud.hideStatusBars()) ci.cancel();
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void withclient$hotbar(DrawContext ctx, RenderTickCounter tc, CallbackInfo ci) {
        if (Hud.hotbarEnabled()) { Hud.drawHotbar(ctx, tc); ci.cancel(); }
    }
}
