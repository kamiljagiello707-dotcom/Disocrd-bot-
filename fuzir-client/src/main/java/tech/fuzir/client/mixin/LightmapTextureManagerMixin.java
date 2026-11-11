package tech.fuzir.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tech.fuzir.client.FuzirClient;
import tech.fuzir.client.module.render.FullBright;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
    /** Replaces {@code options.getGamma().getValue()} (second SimpleOption#getValue call in update). */
    @ModifyExpressionValue(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;", ordinal = 1))
    private Object fuzir$overrideGamma(Object original) {
        FuzirClient client = FuzirClient.get();
        if (client == null) return original;
        FullBright fullBright = client.getModuleManager().get(FullBright.class);
        return fullBright.isEnabled() ? (Object) fullBright.getGamma() : original;
    }
}
