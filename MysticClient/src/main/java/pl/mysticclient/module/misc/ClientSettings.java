package pl.mysticclient.module.misc;

import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;
import pl.mysticclient.setting.BoolSetting;
import pl.mysticclient.setting.ModeSetting;
import pl.mysticclient.setting.NumberSetting;

public class ClientSettings extends Module {
    public final BoolSetting notifications = add(new BoolSetting("Powiadomienia", true));
    public final ModeSetting position = add(new ModeSetting("Pozycja", "TopRight", "TopRight", "BottomRight", "TopLeft", "BottomLeft")).visibleWhen(notifications::get);
    public final NumberSetting duration = add(new NumberSetting("Czas (s)", 2.5, 0.5, 6, 0.5)).visibleWhen(notifications::get);
    public final BoolSetting notifyOnToggle = add(new BoolSetting("Przy przełączeniu", true)).visibleWhen(notifications::get);
    public final BoolSetting guiAnimations = add(new BoolSetting("Animacje GUI", true));
    public final BoolSetting guiBlur = add(new BoolSetting("Rozmycie tła", true));
    public final NumberSetting hudScale = add(new NumberSetting("Skala HUD", 1.0, 0.6, 1.6, 0.05));
    public final NumberSetting guiScale = add(new NumberSetting("Skala GUI", 1.0, 0.7, 1.5, 0.05));

    public ClientSettings() {
        super("ClientSettings", "Ustawienia klienta: powiadomienia, HUD, animacje.", Category.MISC);
        hideFromArrayList();
    }

    @Override
    public void onDisable() {
        setEnabled(true, false);
    }
}
