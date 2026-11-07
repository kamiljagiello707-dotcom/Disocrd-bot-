package pl.mysticclient.mixin;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.mysticclient.module.ModuleManager;
import pl.mysticclient.module.render.NameTags;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    /** Ukrywa waniliowy nick nad głową, gdy NameTags rysuje własną tabliczkę. */
    @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
    private void mystic$hideVanillaLabel(Entity entity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> cir) {
        NameTags tags = ModuleManager.get(NameTags.class);
        if (tags != null && tags.replacesVanilla(entity)) cir.setReturnValue(false);
    }
}
