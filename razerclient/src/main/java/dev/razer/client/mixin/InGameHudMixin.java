package dev.razer.client.mixin;

import dev.razer.client.RazerClient;
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
    private void razer$hideEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        RazerClient rc = RazerClient.get();
        if (rc != null && rc.hud != null && rc.hud.potions.enabled.get()) ci.cancel();
    }
}
