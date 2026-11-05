package pl.firen.client.module.modules;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;

public class Fullbright extends Module {
    public Fullbright() { super("Fullbright", "Pełna jasność w ciemności.", Category.RENDER); }

    @Override
    public void onTick() {
        mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 420, 0, false, false));
    }

    @Override
    public void onDisable() {
        if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }
}
