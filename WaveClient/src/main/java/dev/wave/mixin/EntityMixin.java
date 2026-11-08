package dev.wave.mixin;

import dev.wave.WaveClient;
import dev.wave.module.visuals.ESP;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void wave$teamColor(CallbackInfoReturnable<Integer> cir) {
        if (WaveClient.INSTANCE == null) return;
        ESP esp = WaveClient.INSTANCE.modules.get(ESP.class);
        Entity self = (Entity) (Object) this;
        if (esp.isEnabled() && esp.shouldOutline(self)) cir.setReturnValue(esp.getColor(self));
    }
}
