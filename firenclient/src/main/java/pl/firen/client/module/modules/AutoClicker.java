package pl.firen.client.module.modules;

import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.NumberSetting;

public class AutoClicker extends Module {
    public final NumberSetting minCps = add(new NumberSetting("Min CPS", 8, 1, 20, 1));
    public final NumberSetting maxCps = add(new NumberSetting("Max CPS", 12, 1, 20, 1));
    public final BooleanSetting onlyWeapon = add(new BooleanSetting("Only weapon", true));

    public AutoClicker() { super("AutoClicker", "Automatycznie klika lewym przyciskiem.", Category.COMBAT); }
}
