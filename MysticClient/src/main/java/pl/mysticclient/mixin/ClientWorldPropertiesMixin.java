package pl.mysticclient.mixin;

import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.mysticclient.module.ModuleManager;
import pl.mysticclient.module.world.TimeChanger;

@Mixin(ClientWorld.Properties.class)
public class ClientWorldPropertiesMixin {
    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void mystic$time(CallbackInfoReturnable<Long> cir) {
        TimeChanger tc = ModuleManager.get(TimeChanger.class);
        if (tc != null && tc.isEnabled()) cir.setReturnValue(tc.time.get().longValue());
    }
}
