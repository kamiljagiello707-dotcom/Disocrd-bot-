package dev.wave.mixin;

import dev.wave.WaveClient;
import dev.wave.module.visuals.Fullbright;
import dev.wave.module.visuals.Zoom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleOption.class)
public class SimpleOptionMixin {
    @Shadow Object value;

    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    private void wave$getValue(CallbackInfoReturnable<Object> cir) {
        if (WaveClient.INSTANCE == null || WaveClient.INSTANCE.modules == null) return;
        GameOptions o = MinecraftClient.getInstance().options;
        if (o == null) return;
        Object self = this;
        if (self == o.getGamma()) {
            Fullbright fb = WaveClient.INSTANCE.modules.get(Fullbright.class);
            if (fb.isEnabled()) cir.setReturnValue(fb.brightness.get());
        } else if (self == o.getMouseSensitivity()) {
            Zoom zoom = WaveClient.INSTANCE.modules.get(Zoom.class);
            if (zoom.lowerSensitivity.isOn() && MinecraftClient.getInstance().currentScreen == null && value instanceof Double d) {
                double scale = zoom.sensitivityScale();
                if (scale < 1) cir.setReturnValue(d * scale);
            }
        }
    }
}
