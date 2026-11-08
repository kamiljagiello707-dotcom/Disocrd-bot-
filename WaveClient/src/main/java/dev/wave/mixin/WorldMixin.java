package dev.wave.mixin;

import dev.wave.WaveClient;
import dev.wave.module.visuals.Ambience;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldMixin {
    @Inject(method = {"getRainGradient", "getThunderGradient"}, at = @At("HEAD"), cancellable = true)
    private void wave$weather(float delta, CallbackInfoReturnable<Float> cir) {
        if (WaveClient.INSTANCE == null || !((Object) this instanceof ClientWorld)) return;
        Ambience a = WaveClient.INSTANCE.modules.get(Ambience.class);
        if (a.isEnabled() && a.clearWeather.isOn()) cir.setReturnValue(0f);
    }
}
