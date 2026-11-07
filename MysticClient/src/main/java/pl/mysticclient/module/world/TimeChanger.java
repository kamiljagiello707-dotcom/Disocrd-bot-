package pl.mysticclient.module.world;

import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;
import pl.mysticclient.setting.NumberSetting;

public class TimeChanger extends Module {
    public final NumberSetting time = add(new NumberSetting("Time", 6000, 0, 24000, 500));

    public TimeChanger() {
        super("TimeChanger", "Zmienia porę dnia tylko u Ciebie.", Category.WORLD);
    }

    @Override
    public String getSuffix() { return time.display(); }
}
