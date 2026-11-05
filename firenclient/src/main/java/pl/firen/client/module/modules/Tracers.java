package pl.firen.client.module.modules;

import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.NumberSetting;

public class Tracers extends Module {
    public final NumberSetting width = add(new NumberSetting("Width", 1.5, 0.5, 4.0, 0.1));

    public Tracers() { super("Tracers", "Linie do graczy.", Category.RENDER); }
}
