package pl.mystic.client.module.modules;

import pl.mystic.client.module.Category;
import pl.mystic.client.module.Module;
import org.lwjgl.glfw.GLFW;

/** Przybliżenie obsługiwane w GameRendererMixin. */
public class Zoom extends Module {
    public static final double FACTOR = 4.0;

    public Zoom() { super("Zoom", "Przybliżenie widoku (jak luneta)", Category.RENDER, GLFW.GLFW_KEY_C); }

    @Override
    public void onTick() { mc.options.smoothCameraEnabled = true; }

    @Override
    protected void onDisable() { mc.options.smoothCameraEnabled = false; }
}
