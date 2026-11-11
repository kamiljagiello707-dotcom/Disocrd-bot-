package tech.fuzir.client.module.impl;

import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.BoolSetting;

public class Coordinates extends Module {
    public final BoolSetting nether = add(new BoolSetting("Przelicznik Netheru", true));
    public final BoolSetting direction = add(new BoolSetting("Kierunek", true));

    public Coordinates() {
        super("Koordynaty", "Pozycja XYZ i kierunek patrzenia", Category.HUD);
        setEnabledQuiet(true);
    }
}
