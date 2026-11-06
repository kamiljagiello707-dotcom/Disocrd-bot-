package pl.mystic.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.mystic.client.MysticClient;
import pl.mystic.client.module.modules.Esp;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void mystic$esp(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (MysticClient.modules == null) return;
        Esp esp = MysticClient.modules.get(Esp.class);
        if (esp != null && esp.isEnabled() && esp.shouldHighlight(entity)) cir.setReturnValue(true);
    }
}
