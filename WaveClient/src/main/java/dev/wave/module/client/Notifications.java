package dev.wave.module.client;

import dev.wave.module.Category;
import dev.wave.module.Module;
import dev.wave.setting.BooleanSetting;
import dev.wave.setting.ModeSetting;
import dev.wave.setting.NumberSetting;

public class Notifications extends Module {
    public final ModeSetting position = new ModeSetting("Position", "", "BottomRight", "BottomRight", "TopRight", "TopCenter");
    public final NumberSetting duration = new NumberSetting("Duration", "Seconds on screen", 2, 0.5, 6, 0.25, "s");
    public final BooleanSetting toggles = new BooleanSetting("Module Toggles", "Notify when modules are toggled", true);
    public final BooleanSetting sound = new BooleanSetting("Sound", true);

    public Notifications() {
        super("Notifications", "Pop-up notifications for module toggles", Category.CLIENT);
        addSettings(position, duration, toggles, sound);
        drawn.set(false);
        setEnabledSilently(true);
    }
}
