package pl.mysticclient.module.misc;

import org.lwjgl.glfw.GLFW;
import pl.mysticclient.gui.ClickGuiScreen;
import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;

public class ClickGUI extends Module {
    public ClickGUI() {
        super("ClickGUI", "Menu klienta. Zmień klawisz, którym je otwierasz.", Category.MISC);
        setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
        hideFromArrayList();
    }

    @Override
    public void onEnable() {
        mc.setScreen(new ClickGuiScreen());
        setEnabled(false, false);
    }
}
