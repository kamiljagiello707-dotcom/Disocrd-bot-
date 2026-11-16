package dev.razer.client.mixin;

import dev.razer.client.module.Legit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** FreeLook: ruch myszy obraca tylko kamerę, postać patrzy dalej w tę samą stronę. */
@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void razer$freeLook(double dx, double dy, CallbackInfo ci) {
        if ((Object) this == MinecraftClient.getInstance().player && Legit.freeLookMouse(dx, dy)) ci.cancel();
    }
}
