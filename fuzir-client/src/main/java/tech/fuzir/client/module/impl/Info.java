package tech.fuzir.client.module.impl;

import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.BoolSetting;

public class Info extends Module {
    public final BoolSetting ping = add(new BoolSetting("Ping", true));
    public final BoolSetting speed = add(new BoolSetting("Prędkość", true));
    public final BoolSetting server = add(new BoolSetting("Serwer", true));

    public Info() {
        super("Informacje", "Ping, prędkość i adres serwera", Category.HUD);
        setEnabledQuiet(true);
    }
}
