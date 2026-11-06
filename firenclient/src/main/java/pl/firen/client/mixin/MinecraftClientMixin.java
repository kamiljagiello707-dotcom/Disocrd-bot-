package pl.firen.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.firen.client.render.OutlineManager;

/** Glow ESP: wymusza obrys (outline) dla wybranych encji. */
@Mixin(MinecraftClient.class)
abstract class MinecraftClientMixin {
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void firen$outline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (OutlineManager.has(entity.getId())) cir.setReturnValue(true);
    }
}
