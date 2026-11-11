package tech.fuzir.client.module.impl;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.util.Icons;

public class Fullbright extends Module {
    public Fullbright() {
        super("Fullbright", "Widzisz wszystko jak w dzień, także w jaskiniach", Category.VISUALS, Icons.SUN);
    }

    @Override
    public void onTick() {
        // efekt istnieje tylko po stronie klienta, serwer o nim nie wie
        if (!mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, StatusEffectInstance.INFINITE, 0, false, false, false));
        }
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;
        StatusEffectInstance e = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (e != null && e.isInfinite() && !e.shouldShowIcon()) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }
}
