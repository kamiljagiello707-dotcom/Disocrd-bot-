package dev.with.client.mixin;

import dev.with.client.visual.View;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** SwingAnimation: tempo animacji machnięcia (tylko wygląd Twojej ręki). */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void withclient$swingSpeed(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(View.swingDuration(cir.getReturnValue(), (LivingEntity) (Object) this));
    }
}
