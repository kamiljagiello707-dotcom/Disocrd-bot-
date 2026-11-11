package tech.fuzir.client.module.impl;

import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.NumberSetting;

public class NotificationsModule extends Module {
    public final NumberSetting time = add(new NumberSetting("Czas", 2, 1, 5, 0.5, " s"));

    public NotificationsModule() {
        super("Powiadomienia", "Dymki po włączeniu i wyłączeniu modułu", Category.HUD);
        setEnabledQuiet(true);
    }
}
