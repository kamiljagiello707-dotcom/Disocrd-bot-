package dev.nova.client.module.impl;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.setting.BooleanSetting;

public class HudModule extends Module {
    public final BooleanSetting watermark = add(new BooleanSetting("Watermark", true));
    public final BooleanSetting arrayList = add(new BooleanSetting("ArrayList", true));

    public HudModule() {
        super("HUD", "Watermark and module list", Category.RENDER);
        setEnabled(true);
    }
}
