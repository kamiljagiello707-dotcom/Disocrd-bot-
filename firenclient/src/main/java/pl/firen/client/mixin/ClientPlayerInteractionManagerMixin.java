package pl.firen.client.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.firen.client.module.modules.Criticals;

@Mixin(ClientPlayerInteractionManager.class)
abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void firen$beforeAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (pl.firen.client.module.modules.FakePlayer.isFake(target)) {
            // manekin istnieje tylko u nas – symulujemy cios lokalnie
            pl.firen.client.module.modules.FakePlayer.simulateHit(target);
            player.resetLastAttackedTicks();
            ci.cancel();
            return;
        }
        Criticals.beforeAttack(target);
    }
}
