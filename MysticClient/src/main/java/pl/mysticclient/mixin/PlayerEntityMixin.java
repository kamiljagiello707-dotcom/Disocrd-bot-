package pl.mysticclient.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.mysticclient.module.ModuleManager;
import pl.mysticclient.module.movement.SafeWalk;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "clipAtLedge", at = @At("HEAD"), cancellable = true)
    private void mystic$safeWalk(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ClientPlayerEntity && ModuleManager.isOn(SafeWalk.class)) {
            cir.setReturnValue(true);
        }
    }
}
