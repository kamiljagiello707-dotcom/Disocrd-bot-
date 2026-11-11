package tech.fuzir.client.module.impl;

import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.setting.ModeSetting;
import tech.fuzir.client.setting.NumberSetting;
import tech.fuzir.client.util.Icons;
import tech.fuzir.client.util.Theme;

public class ClickGui extends Module {
    public final BoolSetting blur = add(new BoolSetting("Rozmycie tła", true));
    public final BoolSetting sounds = add(new BoolSetting("Dźwięki kliknięć", true));
    public final BoolSetting descriptions = add(new BoolSetting("Opisy modułów", true));
    public final NumberSetting speed = add(new NumberSetting("Szybkość animacji", 1, 0.5, 2, 0.1, "x"));
    public final NumberSetting guiScale = add(new NumberSetting("Skala menu", 1, 0.7, 1.3, 0.05, "x"));
    public final NumberSetting hudScale = add(new NumberSetting("Skala HUD", 1, 0.7, 1.5, 0.05, "x"));
    /** Wybierany na stronie Motywy, dlatego ukryty na liście ustawień. */
    public final ModeSetting theme = add(new ModeSetting("Motyw", "Monochrom",
            Theme.ALL.stream().map(Theme::name).toArray(String[]::new))).visibleWhen(() -> false);

    public ClickGui() {
        super("Menu", "Wygląd menu, animacje i skala", Category.OTHER, Icons.SETTINGS);
        setEnabledQuiet(true);
    }

    @Override
    public boolean hasToggle() {
        return false;
    }
}
