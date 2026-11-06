package pl.firen.client.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.firen.client.rotation.RotationManager;

/** Podmienia rotację tylko na czas wysyłania pakietu ruchu (ciche rotacje). */
@Mixin(ClientPlayerEntity.class)
abstract class ClientPlayerEntityMixin {
    @Unique private float firen$realYaw, firen$realPitch;
    @Unique private boolean firen$swapped;

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void firen$beforeMovement(CallbackInfo ci) {
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        firen$swapped = RotationManager.isActive();
        if (firen$swapped) {
            firen$realYaw = self.getYaw();
            firen$realPitch = self.getPitch();
            self.setYaw(RotationManager.yaw());
            self.setPitch(RotationManager.pitch());
        }
        RotationManager.sentYaw = self.getYaw();
        RotationManager.sentPitch = self.getPitch();
    }

    @Inject(method = "sendMovementPackets", at = @At("RETURN"))
    private void firen$afterMovement(CallbackInfo ci) {
        if (!firen$swapped) return;
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        self.setYaw(firen$realYaw);
        self.setPitch(firen$realPitch);
        firen$swapped = false;
    }
}
