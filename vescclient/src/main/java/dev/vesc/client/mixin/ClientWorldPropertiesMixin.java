package dev.vesc.client.mixin;

import dev.vesc.client.module.Legit;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** TimeChanger: pora dnia widoczna tylko u Ciebie. */
@Mixin(ClientWorld.Properties.class)
public class ClientWorldPropertiesMixin {
    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void vesc$time(CallbackInfoReturnable<Long> cir) {
        long t = Legit.timeOfDay();
        if (t >= 0) cir.setReturnValue(t);
    }
}
