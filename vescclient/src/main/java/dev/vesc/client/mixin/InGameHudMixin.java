package dev.vesc.client.mixin;

import dev.vesc.client.VescClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ukrywa ikony efektów Minecrafta, gdy włączony jest element HUD "Efekty". */
@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void vesc$hideEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        VescClient rc = VescClient.get();
        if (rc != null && rc.hud != null && rc.hud.potions.enabled.get()) ci.cancel();
    }

    /** styl HUD "Vesc": własny zaokrąglony pasek przedmiotów */
    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void vesc$hotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (VescClient.get() == null || VescClient.get().hud == null || !dev.vesc.client.hud.Hud.hudStyle().equals("Vesc")) return;
        if (!(net.minecraft.client.MinecraftClient.getInstance().getCameraEntity() instanceof net.minecraft.entity.player.PlayerEntity p)) return;
        ci.cancel();
        dev.vesc.client.hud.Hud.renderHotbar(context, p);
    }

    /** Crosshair: własny celownik zamiast vanilla */
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void vesc$crosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (VescClient.get() != null && VescClient.get().modules != null && dev.vesc.client.module.camera.Crosshair.render(context)) ci.cancel();
    }
}
