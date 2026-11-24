package dev.with.client.mixin;

import dev.with.client.visual.Tints;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cape: peleryna WithClient (i ten sam wzór na elytrze) na Twojej postaci. */
@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin {
    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void withclient$cape(CallbackInfoReturnable<SkinTextures> cir) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        Identifier cape = Tints.cape();
        if (cape == null) return;
        SkinTextures s = cir.getReturnValue();
        cir.setReturnValue(new SkinTextures(s.texture(), s.textureUrl(), cape, cape, s.model(), s.secure()));
    }
}
