package pl.firen.client.module.modules;

import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.ModeSetting;

public class Esp extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Box", "Box", "Outline", "2D"));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", false));

    public Esp() { super("ESP", "Podświetla byty przez ściany.", Category.RENDER); }

    @Override public String getSuffix() { return mode.get(); }
}
