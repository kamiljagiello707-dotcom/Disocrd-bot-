package pl.firen.client.module.modules;

import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.ModeSetting;
import pl.firen.client.setting.NumberSetting;

public class Flight extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Vanilla", "Vanilla", "Glide"));
    public final NumberSetting speed = add(new NumberSetting("Speed", 1.0, 0.1, 5.0, 0.1));

    public Flight() { super("Flight", "Pozwala latać.", Category.MOVEMENT); }

    @Override public String getSuffix() { return mode.get(); }
}
