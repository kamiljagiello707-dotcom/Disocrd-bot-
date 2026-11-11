package tech.fuzir.client.module.impl.render;

import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.impl.ModeSetting;

public class FullbrightExtras extends Module {

    public ModeSetting mode = new ModeSetting("Mode", "Gamma", "Gamma", "NightVision", "Ambient");

    public FullbrightExtras() {
        super("Fullbright+", "Advanced brightness control modes.", Category.RENDER);
        this.addSettings(mode);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mode.is("Gamma") && mc.options != null) {
            mc.options.getGamma().setValue(100.0D);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mode.is("Gamma") && mc.options != null) {
            mc.options.getGamma().setValue(1.0D);
        }
    }
}
