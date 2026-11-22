package dev.with.client;

import dev.with.client.module.Modules;
import dev.with.client.render.R2D;
import dev.with.client.ui.ClickGui;
import dev.with.client.ui.Theme;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** WithClient – punkt startowy. */
public class WithClient implements ClientModInitializer {
    public static final String VERSION = "1.0.0";
    public static KeyBinding OPEN_GUI;

    @Override
    public void onInitializeClient() {
        R2D.initShaders();
        Theme.load();
        Modules.init();
        OPEN_GUI = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.withclient.clickgui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.withclient"));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (OPEN_GUI.wasPressed()) if (mc.currentScreen == null && mc.player != null) mc.setScreen(new ClickGui());
        });
    }
}
