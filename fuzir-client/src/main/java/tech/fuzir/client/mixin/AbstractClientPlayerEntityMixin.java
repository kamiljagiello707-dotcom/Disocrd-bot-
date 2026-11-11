package tech.fuzir.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.fuzir.client.module.ModuleManager;
import tech.fuzir.client.module.impl.Cape;

@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin {
    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void fuzir$cape(CallbackInfoReturnable<SkinTextures> cir) {
        // peleryna tylko dla Twojej postaci i tylko u Ciebie w grze
        if ((Object) this != MinecraftClient.getInstance().player || ModuleManager.all().isEmpty()) return;
        Cape cape = ModuleManager.get(Cape.class);
        if (!cape.isEnabled()) return;
        SkinTextures s = cir.getReturnValue();
        Identifier tex = cape.texture();
        cir.setReturnValue(new SkinTextures(s.texture(), s.textureUrl(), tex, cape.elytra.on() ? tex : s.elytraTexture(), s.model(), s.secure()));
    }
}
