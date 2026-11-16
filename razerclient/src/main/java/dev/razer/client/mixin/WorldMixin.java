package dev.razer.client.mixin;

import dev.razer.client.module.Legit;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Weather: pogoda widoczna tylko u Ciebie (świat klienta, serwer bez zmian). */
@Mixin(World.class)
public class WorldMixin {
    @Inject(method = "getRainGradient", at = @At("RETURN"), cancellable = true)
    private void razer$rain(float delta, CallbackInfoReturnable<Float> cir) {
        if (((World) (Object) this).isClient) cir.setReturnValue(Legit.rain(cir.getReturnValue()));
    }

    @Inject(method = "getThunderGradient", at = @At("RETURN"), cancellable = true)
    private void razer$thunder(float delta, CallbackInfoReturnable<Float> cir) {
        if (((World) (Object) this).isClient) cir.setReturnValue(Legit.thunder(cir.getReturnValue()));
    }
}
