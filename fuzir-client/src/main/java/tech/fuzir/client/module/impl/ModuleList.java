package tech.fuzir.client.module.impl;

import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.BoolSetting;

public class ModuleList extends Module {
    public final BoolSetting background = add(new BoolSetting("Tło", true));
    public final BoolSetting bar = add(new BoolSetting("Pasek z boku", true));
    public final BoolSetting suffixes = add(new BoolSetting("Dopiski", true));

    public ModuleList() {
        super("Lista modułów", "Włączone moduły w prawym górnym rogu", Category.HUD);
        setEnabledQuiet(true);
    }
}
