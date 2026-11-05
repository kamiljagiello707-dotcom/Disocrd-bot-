package pl.firen.client.module.modules;

import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.ModeSetting;
import pl.firen.client.setting.NumberSetting;

public class Speed extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Strafe", "Strafe", "BHop", "Ground"));
    public final NumberSetting multiplier = add(new NumberSetting("Multiplier", 1.2, 1.0, 3.0, 0.05));

    public Speed() { super("Speed", "Szybsze poruszanie się.", Category.MOVEMENT); }

    @Override public String getSuffix() { return mode.get(); }
}
