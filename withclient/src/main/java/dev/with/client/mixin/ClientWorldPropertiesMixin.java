package dev.with.client.mixin;

import dev.with.client.visual.V;
import dev.with.client.visual.View;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** TimeChanger: własna pora dnia tylko u Ciebie. */
@Mixin(ClientWorld.Properties.class)
public class ClientWorldPropertiesMixin {
    @Inject(method = "getTimeOfDay", at = @At("RETURN"), cancellable = true)
    private void withclient$time(CallbackInfoReturnable<Long> cir) {
        if (V.on("TimeChanger")) cir.setReturnValue(View.timeOfDay(cir.getReturnValue()));
    }
}
