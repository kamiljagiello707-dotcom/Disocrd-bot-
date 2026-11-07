package dev.mustic.client.module.modules;

import dev.mustic.client.gui.ClickGui;
import dev.mustic.client.module.Category;
import dev.mustic.client.module.Module;
import dev.mustic.client.setting.BooleanSetting;
import dev.mustic.client.setting.ModeSetting;
import dev.mustic.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class ClickGuiModule extends Module {
    public final ModeSetting theme = add(new ModeSetting("Theme", "Mustic",
            "Mustic", "Rainbow", "Ocean", "Blood", "Mint", "Sunset"));
    public final NumberSetting dim = add(new NumberSetting("Background", 0.55, 0.0, 0.9, 0.05));
    public final BooleanSetting descriptions = add(new BooleanSetting("Descriptions", true));
    public final BooleanSetting blur = add(new BooleanSetting("Blur", true));

    public ClickGuiModule() {
        super("ClickGUI", "Otwiera menu klienta.", Category.CLIENT, GLFW.GLFW_KEY_RIGHT_SHIFT);
        hideFromArrayList();
    }

    @Override
    public void toggle() {
        // Nie przelaczamy stanu - tylko otwieramy menu (w nastepnej klatce,
        // zeby ten sam event klawisza nie trafil od razu do nowego ekranu).
        mc.send(() -> mc.setScreen(ClickGui.get()));
    }

    @Override
    public void setEnabled(boolean enabled) {
        // ClickGUI nigdy nie jest "wlaczony"
    }
}
