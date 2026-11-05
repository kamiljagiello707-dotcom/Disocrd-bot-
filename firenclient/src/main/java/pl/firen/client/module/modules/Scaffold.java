package pl.firen.client.module.modules;

import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.BooleanSetting;

public class Scaffold extends Module {
    public final BooleanSetting tower = add(new BooleanSetting("Tower", false));
    public final BooleanSetting swing = add(new BooleanSetting("Swing", true));

    public Scaffold() { super("Scaffold", "Stawia bloki pod nogami.", Category.WORLD); }
}
