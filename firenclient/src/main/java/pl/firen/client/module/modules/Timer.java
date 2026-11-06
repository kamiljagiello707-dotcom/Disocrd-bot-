package pl.firen.client.module.modules;

import pl.firen.client.FirenClient;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.NumberSetting;

public class Timer extends Module {
    public final NumberSetting speed = add(new NumberSetting("Speed", 1.5, 0.1, 5.0, 0.05));

    public Timer() { super("Timer", "Zmienia szybkość gry (ticki).", Category.WORLD); }

    @Override public String getSuffix() { return speed.display() + "x"; }

    public static float multiplier() {
        var mods = FirenClient.modules();
        if (mods == null) return 1f;
        Timer t = mods.get(Timer.class);
        return t.isEnabled() ? t.speed.get().floatValue() : 1f;
    }
}
