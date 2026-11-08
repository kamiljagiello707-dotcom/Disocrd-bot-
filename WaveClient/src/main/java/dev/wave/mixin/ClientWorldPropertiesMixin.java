package dev.wave.mixin;

import dev.wave.WaveClient;
import dev.wave.module.visuals.Ambience;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.Properties.class)
public class ClientWorldPropertiesMixin {
    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void wave$time(CallbackInfoReturnable<Long> cir) {
        if (WaveClient.INSTANCE == null) return;
        Ambience a = WaveClient.INSTANCE.modules.get(Ambience.class);
        if (a.isEnabled() && a.changeTime.isOn()) cir.setReturnValue((long) a.time.getInt());
    }
}
