package pl.firen.client.module.modules;

import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.BooleanSetting;

public class Hud extends Module {
    public final BooleanSetting watermark = add(new BooleanSetting("Watermark", true));
    public final BooleanSetting arrayList = add(new BooleanSetting("ArrayList", true));
    public final BooleanSetting info = add(new BooleanSetting("FPS / XYZ", true));

    public Hud() {
        super("HUD", "Napis FirenClient i lista włączonych modułów.", Category.RENDER);
        setEnabled(true);
    }
}
