package pl.firen.client.module.modules;

import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.NumberSetting;

public class Velocity extends Module {
    public final NumberSetting horizontal = add(new NumberSetting("Horizontal", 0, 0, 100, 1));
    public final NumberSetting vertical = add(new NumberSetting("Vertical", 0, 0, 100, 1));

    public Velocity() { super("Velocity", "Zmniejsza odrzut po otrzymaniu ciosu.", Category.COMBAT); }

    @Override public String getSuffix() { return horizontal.display() + "% " + vertical.display() + "%"; }
}
