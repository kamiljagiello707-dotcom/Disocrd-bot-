package dev.wave.module.visuals;

import dev.wave.module.Category;
import dev.wave.module.Module;
import dev.wave.setting.BooleanSetting;
import dev.wave.setting.NumberSetting;

public class Ambience extends Module {
    public final BooleanSetting changeTime = new BooleanSetting("Change Time", true);
    public final NumberSetting time = new NumberSetting("Time", "Client-side time of day (ticks)", 6000, 0, 24000, 250, "").visible(changeTime::isOn);
    public final BooleanSetting clearWeather = new BooleanSetting("Clear Weather", "Hide rain and thunder", true);

    public Ambience() {
        super("Ambience", "Client-side time and weather changer", Category.VISUALS);
        addSettings(changeTime, time, clearWeather);
    }

    @Override
    public String getSuffix() {
        if (!changeTime.isOn()) return null;
        int t = time.getInt();
        int hours = (t / 1000 + 6) % 24;
        int minutes = (int) ((t % 1000) / 1000.0 * 60);
        return String.format("%02d:%02d", hours, minutes);
    }
}
