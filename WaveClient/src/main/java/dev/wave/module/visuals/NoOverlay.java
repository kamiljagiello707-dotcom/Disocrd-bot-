package dev.wave.module.visuals;

import dev.wave.module.Category;
import dev.wave.module.Module;
import dev.wave.setting.BooleanSetting;

public class NoOverlay extends Module {
    public final BooleanSetting fire = new BooleanSetting("Fire", "Hide the burning overlay", true);
    public final BooleanSetting water = new BooleanSetting("Underwater", "Hide the underwater overlay", true);

    public NoOverlay() {
        super("NoOverlay", "Removes annoying screen overlays", Category.VISUALS);
        addSettings(fire, water);
    }
}
