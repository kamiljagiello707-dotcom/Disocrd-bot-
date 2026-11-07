package pl.mysticclient.module.render;

import pl.mysticclient.mixin.SimpleOptionAccessor;
import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;
import pl.mysticclient.setting.NumberSetting;

public class Fullbright extends Module {
    private final NumberSetting brightness = add(new NumberSetting("Brightness", 15, 1, 20, 0.5));
    private double previous = 0.5;
    private boolean applied;

    public Fullbright() {
        super("Fullbright", "Pełna jasność – widzisz wszystko w ciemności.", Category.RENDER);
    }

    @Override
    public void onEnable() {
        if (mc.options == null) return;
        previous = Math.min(1.0, mc.options.getGamma().getValue());
        applied = true;
    }

    @Override
    public void onTick() {
        if (mc.options == null) return;
        if (!applied) onEnable();
        setGamma(brightness.get());
    }

    @Override
    public void onDisable() {
        if (mc.options == null || !applied) return;
        setGamma(previous);
        applied = false;
    }

    /** Przywraca gammę przed zapisem opcji, by nie zapisać złej wartości. */
    public void restoreForSave() {
        if (applied) setGamma(previous);
    }

    @SuppressWarnings("unchecked")
    private void setGamma(double value) {
        ((SimpleOptionAccessor<Double>) (Object) mc.options.getGamma()).mystic$setValue(value);
    }
}
