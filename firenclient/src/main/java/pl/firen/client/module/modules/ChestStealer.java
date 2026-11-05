package pl.firen.client.module.modules;

import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.NumberSetting;

public class ChestStealer extends Module {
    public final NumberSetting delay = add(new NumberSetting("Delay (ms)", 80, 0, 500, 10));

    public ChestStealer() { super("ChestStealer", "Szybko zabiera przedmioty ze skrzyń.", Category.PLAYER); }
}
