package pl.mysticclient.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.mysticclient.module.ModuleManager;
import pl.mysticclient.module.render.ESP;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void mystic$espColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        pl.mysticclient.module.combat.KillAura ka = ModuleManager.get(pl.mysticclient.module.combat.KillAura.class);
        if (ka != null && ka.glowTarget(self)) { cir.setReturnValue(ka.espRgb() & 0xFFFFFF); return; }
        ESP esp = ModuleManager.get(ESP.class);
        if (esp != null && esp.isEnabled() && esp.mode.is("Glow") && esp.shouldRender(self)) {
            cir.setReturnValue(esp.colorFor(self) & 0xFFFFFF);
        }
    }
}
