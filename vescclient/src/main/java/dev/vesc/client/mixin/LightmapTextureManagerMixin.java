package dev.vesc.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.vesc.client.module.Legit;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Fullbright: podbija gammę albo symuluje Night Vision (tylko oświetlenie po stronie klienta). */
@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {
    @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 1))
    private float vesc$gamma(float g) { return Legit.gamma(g); }

    @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z", ordinal = 0))
    private boolean vesc$nv(boolean has) { return has || Legit.nightVision(); }

    @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;getNightVisionStrength(Lnet/minecraft/entity/LivingEntity;F)F"))
    private float vesc$nvStrength(float s) { return Legit.nightVision() ? Math.max(s, Legit.nightVisionStrength()) : s; }
}
