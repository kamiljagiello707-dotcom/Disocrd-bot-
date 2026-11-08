package dev.nova.client.module.impl;

import dev.nova.client.gui.ClickGuiScreen;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.setting.BooleanSetting;
import dev.nova.client.setting.ColorSetting;
import dev.nova.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

/** Opens the GUI and holds its theme settings. */
public class ClickGuiModule extends Module {
    public final ColorSetting accent = add(new ColorSetting("Accent", 0.75f));
    public final BooleanSetting rainbow = add(new BooleanSetting("Rainbow", false));
    public final NumberSetting rainbowSpeed = add(new NumberSetting("Rainbow Speed", 4, 1, 10, 1).visibleIf(rainbow::get));
    public final BooleanSetting gradient = add(new BooleanSetting("Gradient", true));
    public final BooleanSetting descriptions = add(new BooleanSetting("Descriptions", true));
    public final NumberSetting dim = add(new NumberSetting("Background Dim", 45, 0, 90, 5));

    public ClickGuiModule() {
        super("ClickGUI", "Opens this menu", Category.MISC);
        bind.set(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public void onEnable() {
        mc.setScreen(new ClickGuiScreen());
        setEnabled(false);
    }
}
