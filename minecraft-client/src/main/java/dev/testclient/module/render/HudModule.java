package dev.testclient.module.render;

import dev.testclient.module.Category;
import dev.testclient.module.Module;
import dev.testclient.setting.BooleanSetting;

public class HudModule extends Module {
    public final BooleanSetting watermark = add(new BooleanSetting("Watermark", true));
    public final BooleanSetting arrayList = add(new BooleanSetting("ArrayList", true));
    public final BooleanSetting coords = add(new BooleanSetting("Coords", true));

    public HudModule() {
        super("HUD", "Lista włączonych modułów i info na ekranie", Category.RENDER);
        setEnabled(true);
    }
}
