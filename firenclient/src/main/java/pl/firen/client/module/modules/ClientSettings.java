package pl.firen.client.module.modules;

import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.ModeSetting;
import pl.firen.client.setting.NumberSetting;
import pl.firen.client.util.Notifications;

/** Globalne ustawienia klienta: powiadomienia, animacje, HUD. */
public class ClientSettings extends Module {
    public final BooleanSetting notifications = add(new BooleanSetting("Notifications", true));
    public final ModeSetting position = add(new ModeSetting("Position", "TopRight", "TopRight", "BottomRight", "TopLeft", "BottomLeft"));
    public final NumberSetting duration = add(new NumberSetting("Duration (s)", 2.5, 1.0, 6.0, 0.5));
    public final BooleanSetting notifyModules = add(new BooleanSetting("Notify on toggle", true));
    public final BooleanSetting guiAnimations = add(new BooleanSetting("GUI animations", true));
    public final BooleanSetting rainbowArray = add(new BooleanSetting("Rainbow ArrayList", false));

    public ClientSettings() {
        super("ClientSettings", "Ustawienia klienta: powiadomienia, HUD, animacje.", Category.MISC);
        setEnabled(true);
        position.visibleWhen(notifications::get);
        duration.visibleWhen(notifications::get);
        notifyModules.visibleWhen(notifications::get);
    }

    @Override
    public void onEnable() {
        Notifications.post("FirenClient", "klient gotowy", Notifications.Type.INFO);
    }
}
