package dev.mustic.client.module.modules;

import dev.mustic.client.module.Category;
import dev.mustic.client.module.Module;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class FullbrightModule extends Module {
    public FullbrightModule() {
        super("Fullbright", "Rozjasnia swiat (lokalny efekt Night Vision).", Category.RENDER);
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        StatusEffectInstance current = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (current == null || !current.isInfinite()) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,
                    StatusEffectInstance.INFINITE, 0, false, false, false));
        }
    }

    @Override
    public void onDisable() {
        if (!nullCheck()) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }
}
