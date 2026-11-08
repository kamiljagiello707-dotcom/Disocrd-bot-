package dev.wave.module.visuals;

import dev.wave.module.Category;
import dev.wave.module.Module;
import dev.wave.setting.NumberSetting;

public class Fullbright extends Module {
    public final NumberSetting brightness = new NumberSetting("Brightness", "Gamma value used while enabled", 12, 1, 16, 0.5, "x");

    public Fullbright() {
        super("Fullbright", "Lights up the whole world, no torches needed", Category.VISUALS);
        addSettings(brightness);
    }

    @Override public String getSuffix() { return brightness.display(); }
}
