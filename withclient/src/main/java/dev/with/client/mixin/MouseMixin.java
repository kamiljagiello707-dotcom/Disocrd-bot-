package dev.with.client.mixin;

import dev.with.client.hud.Hud;
import dev.with.client.visual.View;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Liczy kliknięcia LPM/PPM dla elementów HUD "CPS" i "Keystrokes"; kółko i czułość przy zoomie. */
@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void withclient$click(long window, int button, int action, int mods, CallbackInfo ci) {
        if (action == 1) {
            if (button == 0) Hud.click(true);
            else if (button == 1) Hud.click(false);
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void withclient$zoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen == null && View.scroll(vertical)) ci.cancel();
    }

    @Redirect(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void withclient$zoomSensitivity(ClientPlayerEntity player, double dx, double dy) {
        double k = View.sensitivity();
        player.changeLookDirection(dx * k, dy * k);
    }
}
