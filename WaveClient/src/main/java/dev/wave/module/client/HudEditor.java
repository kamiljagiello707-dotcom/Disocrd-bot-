package dev.wave.module.client;

import dev.wave.gui.HudEditorScreen;
import dev.wave.module.Category;
import dev.wave.module.Module;
import dev.wave.setting.BooleanSetting;
import dev.wave.setting.NumberSetting;

public class HudEditor extends Module {
    public final BooleanSetting snapping = new BooleanSetting("Snapping", "Snap elements to edges and the centre", true);
    public final NumberSetting snapRange = new NumberSetting("Snap Range", "", 4, 1, 12, 1, "px").visible(snapping::isOn);
    public final BooleanSetting grid = new BooleanSetting("Grid", "Show a grid in the editor", true);

    public HudEditor() {
        super("HUD Editor", "Drag HUD elements around", Category.CLIENT);
        addSettings(snapping, snapRange, grid);
        drawn.set(false);
    }

    @Override
    public void onEnable() {
        mc.setScreen(new HudEditorScreen(mc.currentScreen));
        setEnabledSilently(false);
    }
}
