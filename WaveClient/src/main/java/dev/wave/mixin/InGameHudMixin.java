package dev.wave.mixin;

import dev.wave.WaveClient;
import dev.wave.module.visuals.Crosshair;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void wave$crosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (WaveClient.INSTANCE != null && WaveClient.INSTANCE.modules.get(Crosshair.class).isEnabled()) ci.cancel();
    }
}
