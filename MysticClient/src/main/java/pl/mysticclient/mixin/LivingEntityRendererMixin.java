package pl.mysticclient.mixin;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.mysticclient.module.ModuleManager;
import pl.mysticclient.module.render.NameTags;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    /** LivingEntityRenderer nadpisuje hasLabel (gracze, moby) – też go ukrywamy. */
    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
    private void mystic$hideVanillaLabel(LivingEntity entity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> cir) {
        NameTags tags = ModuleManager.get(NameTags.class);
        if (tags != null && tags.replacesVanilla(entity)) cir.setReturnValue(false);
    }
}
