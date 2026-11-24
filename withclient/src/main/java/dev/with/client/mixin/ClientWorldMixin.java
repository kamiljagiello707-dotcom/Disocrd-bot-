package dev.with.client.mixin;

import dev.with.client.visual.View;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ambience: kolor nieba i chmur. */
@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void withclient$sky(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        cir.setReturnValue(View.sky(cir.getReturnValue(), (ClientWorld) (Object) this, tickDelta));
    }

    @Inject(method = "getCloudsColor", at = @At("RETURN"), cancellable = true)
    private void withclient$clouds(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        cir.setReturnValue(View.clouds(cir.getReturnValue(), (ClientWorld) (Object) this, tickDelta));
    }
}
