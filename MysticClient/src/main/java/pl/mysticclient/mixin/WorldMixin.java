package pl.mysticclient.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.mysticclient.module.ModuleManager;
import pl.mysticclient.module.world.NoWeather;

@Mixin(World.class)
public class WorldMixin {
    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
    private void mystic$noRain(float delta, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientWorld && ModuleManager.isOn(NoWeather.class)) cir.setReturnValue(0f);
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
    private void mystic$noThunder(float delta, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientWorld && ModuleManager.isOn(NoWeather.class)) cir.setReturnValue(0f);
    }
}
