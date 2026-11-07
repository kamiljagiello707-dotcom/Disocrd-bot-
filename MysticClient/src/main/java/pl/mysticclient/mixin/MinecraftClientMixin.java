package pl.mysticclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.mysticclient.module.ModuleManager;
import pl.mysticclient.module.render.ESP;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void mystic$glowEsp(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        ESP esp = ModuleManager.get(ESP.class);
        if (esp != null && esp.isEnabled() && esp.mode.is("Glow") && esp.shouldRender(entity)) {
            cir.setReturnValue(true);
        }
    }
}
