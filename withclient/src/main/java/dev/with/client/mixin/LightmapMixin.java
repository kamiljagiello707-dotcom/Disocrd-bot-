package dev.with.client.mixin;

import dev.with.client.visual.View;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Fullbright: jasność (gamma) albo noktowizja – tylko obraz, bez efektu na serwerze. */
@Mixin(LightmapTextureManager.class)
public class LightmapMixin {
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 1))
    private float withclient$gamma(Double value) {
        return View.gamma(value.floatValue());
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z", ordinal = 0))
    private boolean withclient$nightVision(ClientPlayerEntity player, RegistryEntry<StatusEffect> effect) {
        return player.hasStatusEffect(effect) || View.nightVision() > 0;
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;getNightVisionStrength(Lnet/minecraft/entity/LivingEntity;F)F"))
    private float withclient$nightVisionStrength(LivingEntity e, float delta) {
        float own = e.hasStatusEffect(StatusEffects.NIGHT_VISION) ? GameRenderer.getNightVisionStrength(e, delta) : 0;
        return Math.max(own, View.nightVision());
    }
}
