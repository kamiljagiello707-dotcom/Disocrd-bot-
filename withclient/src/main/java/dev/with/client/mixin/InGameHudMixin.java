package dev.with.client.mixin;

import dev.with.client.hud.Hud;
import dev.with.client.visual.V;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ikony efektów (Potions), pasek przedmiotów (Hotbar), własny celownik (Crosshair) i ukrywanie nakładek (NoRender). */
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

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void withclient$crosshair(DrawContext ctx, RenderTickCounter tc, CallbackInfo ci) {
        if (Hud.drawCrosshair(ctx, tc)) ci.cancel();
    }

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void withclient$overlay(DrawContext ctx, Identifier texture, float opacity, CallbackInfo ci) {
        String p = texture.getPath();
        if ((p.contains("pumpkinblur") && V.hide("NoRender", "Pumpkin")) || (p.contains("powder_snow") && V.hide("NoRender", "Śnieg"))) ci.cancel();
    }

    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    private void withclient$vignette(DrawContext ctx, Entity entity, CallbackInfo ci) {
        if (V.hide("NoRender", "Winieta")) ci.cancel();
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void withclient$portal(DrawContext ctx, float strength, CallbackInfo ci) {
        if (V.hide("NoRender", "Portal")) ci.cancel();
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
    private void withclient$scoreboard(DrawContext ctx, RenderTickCounter tc, CallbackInfo ci) {
        if (V.hide("NoRender", "Scoreboard")) ci.cancel();
    }
}
