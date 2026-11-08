package dev.wave.module.client;

import dev.wave.gui.ClickGuiScreen;
import dev.wave.module.Category;
import dev.wave.module.Module;
import dev.wave.setting.BooleanSetting;
import dev.wave.setting.ColorSetting;
import dev.wave.setting.ModeSetting;
import dev.wave.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class ClickGUI extends Module {
    public final ColorSetting accent = new ColorSetting("Accent", "Main accent colour of the GUI", 0xFFFFFFFF);
    public final ModeSetting animation = new ModeSetting("Animation", "Opening animation", "Scale", "Scale", "Slide", "Fade");
    public final NumberSetting dim = new NumberSetting("Background", "Background darkness", 0.55, 0, 0.95, 0.05, "");
    public final BooleanSetting blur = new BooleanSetting("Blur", "Blur the world behind the GUI", true);
    public final BooleanSetting tooltips = new BooleanSetting("Tooltips", "Show module descriptions", true);
    public final BooleanSetting sounds = new BooleanSetting("Sounds", "Click sounds", true);

    public ClickGUI() {
        super("ClickGUI", "Opens this menu", Category.CLIENT);
        addSettings(accent, animation, dim, blur, tooltips, sounds);
        drawn.set(false);
        setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public void onEnable() {
        mc.setScreen(ClickGuiScreen.get());
        setEnabledSilently(false);
    }
}
