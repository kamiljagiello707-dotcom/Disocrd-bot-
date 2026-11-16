package dev.vesc.client.mixin;

import dev.vesc.client.module.Legit;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** FreeLook: kamera używa własnego kierunku zamiast kierunku gracza. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPos(net.minecraft.util.math.Vec3d pos);
    @Shadow public abstract net.minecraft.util.math.Vec3d getPos();
    @Shadow public abstract float getYaw();

    /** GUI 3D: kamera przesuwa się w prawo, żeby postać była po lewej, a panel po prawej */
    @org.spongepowered.asm.mixin.injection.Inject(method = "update", at = @At("TAIL"))
    private void vesc$spatialShift(net.minecraft.world.BlockView area, net.minecraft.entity.Entity focused, boolean thirdPerson, boolean inverseView, float tickDelta,
                                   org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        float k = dev.vesc.client.ui.ClickGui.camShift();
        if (k < 0.001f || !thirdPerson || inverseView) return;
        double r = Math.toRadians(getYaw());
        setPos(getPos().add(-Math.cos(r) * 2.3 * k, 0.12 * k, -Math.sin(r) * 2.3 * k));
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
    private void vesc$rot(Camera self, float yaw, float pitch) {
        if (Legit.freeLook()) setRotation(Legit.freeYaw, Legit.freePitch);
        else setRotation(yaw, pitch);
    }
}
