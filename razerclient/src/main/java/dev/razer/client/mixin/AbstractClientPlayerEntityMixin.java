package dev.razer.client.mixin;

import dev.razer.client.RazerClient;
import dev.razer.client.module.Cosmetic;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Podmienia pelerynę lokalnego gracza na pelerynę RazerClient, gdy moduł Cosmetic jest włączony. */
@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin {
    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void razer$cape(CallbackInfoReturnable<SkinTextures> cir) {
        RazerClient rc = RazerClient.get();
        if (rc == null || rc.modules == null) return;
        if ((Object) this != MinecraftClient.getInstance().player) return;
        if (!(rc.modules.get("Cosmetic") instanceof Cosmetic c) || !c.isEnabled()) return;
        SkinTextures t = cir.getReturnValue();
        cir.setReturnValue(new SkinTextures(t.texture(), t.textureUrl(), c.capeTexture(), t.elytraTexture(), t.model(), t.secure()));
    }
}
