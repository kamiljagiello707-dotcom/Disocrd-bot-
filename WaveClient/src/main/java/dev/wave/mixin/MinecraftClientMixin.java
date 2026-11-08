package dev.wave.mixin;

import dev.wave.WaveClient;
import dev.wave.module.visuals.ESP;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void wave$hasOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (WaveClient.INSTANCE == null) return;
        ESP esp = WaveClient.INSTANCE.modules.get(ESP.class);
        if (esp.isEnabled() && esp.shouldOutline(entity)) cir.setReturnValue(true);
    }
}
