package dev.nova.client.module.impl;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class Fullbright extends Module {
    public Fullbright() { super("Fullbright", "See in the dark", Category.RENDER); }

    @Override
    public void onTick() {
        if (mc.player != null)
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 420, 0, false, false));
    }

    @Override
    public void onDisable() {
        if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }
}
