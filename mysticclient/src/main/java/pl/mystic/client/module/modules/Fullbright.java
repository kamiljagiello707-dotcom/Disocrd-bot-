package pl.mystic.client.module.modules;

import pl.mystic.client.module.Category;
import pl.mystic.client.module.Module;
import org.lwjgl.glfw.GLFW;

/** Jasność ustawiana przez SimpleOptionMixin. */
public class Fullbright extends Module {
    public Fullbright() { super("Fullbright", "Pełna jasność w ciemności", Category.RENDER, GLFW.GLFW_KEY_B); }
}
