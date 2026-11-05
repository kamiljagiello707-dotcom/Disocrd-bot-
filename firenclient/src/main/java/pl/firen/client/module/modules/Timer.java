package pl.firen.client.module.modules;

import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.NumberSetting;

public class Timer extends Module {
    public final NumberSetting speed = add(new NumberSetting("Speed", 1.5, 0.1, 5.0, 0.1));

    public Timer() { super("Timer", "Zmienia szybkość gry.", Category.WORLD); }

    @Override public String getSuffix() { return speed.display() + "x"; }
}
