package tech.fuzir.client.module.impl;

import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.BoolSetting;

public class Keystrokes extends Module {
    public final BoolSetting mouse = add(new BoolSetting("Przyciski myszy", true));
    public final BoolSetting space = add(new BoolSetting("Spacja", true));

    public Keystrokes() {
        super("Klawisze", "Podgląd wciskanych klawiszy WASD", Category.HUD);
    }
}
