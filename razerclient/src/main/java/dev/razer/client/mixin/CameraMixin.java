package dev.razer.client.mixin;

import dev.razer.client.module.Legit;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** FreeLook: kamera używa własnego kierunku zamiast kierunku gracza. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
    private void razer$rot(Camera self, float yaw, float pitch) {
        if (Legit.freeLook()) setRotation(Legit.freeYaw, Legit.freePitch);
        else setRotation(yaw, pitch);
    }
}
