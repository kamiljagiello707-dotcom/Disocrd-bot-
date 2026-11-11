package tech.fuzir.client.module.impl;

import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.setting.NumberSetting;

public class ClickGui extends Module {
    public final BoolSetting blur = add(new BoolSetting("Rozmycie tła", true));
    public final BoolSetting sounds = add(new BoolSetting("Dźwięki kliknięć", true));
    public final NumberSetting speed = add(new NumberSetting("Szybkość animacji", 1, 0.5, 2, 0.1, "x"));
    public final NumberSetting guiScale = add(new NumberSetting("Skala menu", 1, 0.7, 1.4, 0.05, "x"));
    public final NumberSetting hudScale = add(new NumberSetting("Skala HUD", 1, 0.7, 1.5, 0.05, "x"));

    public ClickGui() {
        super("Ustawienia", "Wygląd menu i HUD", Category.CLIENT);
        setEnabledQuiet(true);
    }

    @Override
    public boolean hasToggle() {
        return false;
    }
}
