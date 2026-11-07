package dev.testclient.module.render;

import dev.testclient.module.Category;
import dev.testclient.module.Module;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

/** Klientowy efekt Night Vision - widać w ciemności. */
public class Fullbright extends Module {
    public Fullbright() {
        super("Fullbright", "Pełna jasność (klientowy Night Vision)", Category.RENDER);
    }

    @Override
    public void onTick() {
        var effect = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (effect == null || effect.getDuration() < 220) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 420, 0, false, false));
        }
    }

    @Override
    protected void onDisable() {
        if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }
}
