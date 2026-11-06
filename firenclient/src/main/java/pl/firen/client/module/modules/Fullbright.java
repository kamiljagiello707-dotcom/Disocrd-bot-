package pl.firen.client.module.modules;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import pl.firen.client.mixin.SimpleOptionAccessor;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.ModeSetting;

public class Fullbright extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Gamma", "Gamma", "Night vision"));
    private Double oldGamma;

    public Fullbright() { super("Fullbright", "Pełna jasność w ciemności.", Category.RENDER); }

    @Override
    public void onTick() {
        if (mode.is("Gamma")) {
            if (oldGamma == null) oldGamma = mc.options.getGamma().getValue();
            ((SimpleOptionAccessor) (Object) mc.options.getGamma()).firen$setValueRaw(16.0);
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        } else {
            restoreGamma();
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 420, 0, false, false));
        }
    }

    private void restoreGamma() {
        if (oldGamma != null) {
            ((SimpleOptionAccessor) (Object) mc.options.getGamma()).firen$setValueRaw(oldGamma);
            oldGamma = null;
        }
    }

    @Override
    public void onDisable() {
        restoreGamma();
        if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }
}
