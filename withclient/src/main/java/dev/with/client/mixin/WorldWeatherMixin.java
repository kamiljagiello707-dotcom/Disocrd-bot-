package dev.with.client.mixin;

import dev.with.client.visual.View;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Weather: deszcz/burza/śnieg/czyste niebo tylko po stronie klienta (świat serwera bez zmian). */
@Mixin(World.class)
public class WorldWeatherMixin {
    @Inject(method = "getRainGradient", at = @At("RETURN"), cancellable = true)
    private void withclient$rain(float delta, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientWorld && View.weatherActive()) cir.setReturnValue(View.rain(cir.getReturnValue()));
    }

    @Inject(method = "getThunderGradient", at = @At("RETURN"), cancellable = true)
    private void withclient$thunder(float delta, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientWorld && View.weatherActive()) cir.setReturnValue(View.thunder(cir.getReturnValue()));
    }
}
