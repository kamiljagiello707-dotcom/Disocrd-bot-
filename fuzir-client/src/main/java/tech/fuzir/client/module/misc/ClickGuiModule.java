package tech.fuzir.client.module.misc;

import org.lwjgl.glfw.GLFW;
import tech.fuzir.client.FuzirClient;
import tech.fuzir.client.gui.ClickGuiScreen;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.setting.ModeSetting;
import tech.fuzir.client.setting.NumberSetting;

/**
 * Opens the ClickGUI and holds its appearance options. It never stays enabled itself.
 */
public final class ClickGuiModule extends Module {
    public final ModeSetting accent = mode("Accent", "Sakura", "Sakura", "Neon");
    public final BoolSetting blur = bool("Blur", true);
    public final NumberSetting blurStrength = register(new NumberSetting("Blur Strength", 12, 2, 24, 1))
            .visibleWhen(blur::isEnabled);
    public final NumberSetting scale = number("Scale", 1.0, 0.6, 1.6, 0.05, "x");
    public final BoolSetting descriptions = bool("Descriptions", true);
    public final BoolSetting customFont = bool("Custom Font", true);

    public ClickGuiModule() {
        super("ClickGUI", "Opens this menu. Ctrl + scroll to resize.", Category.MISC, GLFW.GLFW_KEY_RIGHT_SHIFT);
        setDrawn(false);
    }

    @Override
    public void toggle() {
        if (mc.currentScreen instanceof ClickGuiScreen) return;
        mc.setScreen(FuzirClient.get().getClickGui());
    }

    @Override
    public void setEnabled(boolean state) {
        // Purely an action; never persists an enabled state.
    }
}
