package dev.razer.client.mixin;

import dev.razer.client.hud.Input;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Licznik CPS w Keystrokes (tylko odczyt) i scroll dla Zoom. */
@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void razer$cps(long window, int button, int action, int mods, CallbackInfo ci) {
        if (action == 1 && MinecraftClient.getInstance().currentScreen == null) Input.click(button);
    }

    /** Zoom: kółko myszy zmienia przybliżenie */
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void razer$zoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen == null && dev.razer.client.module.Legit.zoomScroll(vertical)) ci.cancel();
    }
}
