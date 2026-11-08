package dev.wave.mixin;

import dev.wave.WaveClient;
import dev.wave.module.visuals.Zoom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void wave$onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (window == MinecraftClient.getInstance().getWindow().getHandle() && WaveClient.INSTANCE != null) {
            WaveClient.INSTANCE.onMouseButton(button, action);
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void wave$onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (WaveClient.INSTANCE == null || MinecraftClient.getInstance().currentScreen != null || vertical == 0) return;
        if (WaveClient.INSTANCE.modules.get(Zoom.class).onScroll(vertical)) ci.cancel();
    }
}
