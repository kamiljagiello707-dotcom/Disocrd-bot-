package pl.firen.client.module.modules;

import org.lwjgl.glfw.GLFW;
import pl.firen.client.gui.ClickGuiScreen;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.BooleanSetting;

public class ClickGui extends Module {
    public final BooleanSetting blur = add(new BooleanSetting("Dark background", true));

    public ClickGui() {
        super("ClickGUI", "Otwiera to menu (domyślnie Right Shift).", Category.RENDER);
        setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public void onEnable() {
        mc.setScreen(new ClickGuiScreen());
        setEnabled(false);
    }
}
