package tech.fuzir.client.module.impl;

import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.setting.ModeSetting;

public class Watermark extends Module {
    public final ModeSetting style = add(new ModeSetting("Styl", "Pełny", "Pełny", "Minimalny"));
    public final BoolSetting fps = add(new BoolSetting("FPS", true));
    public final BoolSetting time = add(new BoolSetting("Godzina", true));

    public Watermark() {
        super("Watermark", "Logo klienta w rogu ekranu", Category.HUD);
        setEnabledQuiet(true);
    }
}
