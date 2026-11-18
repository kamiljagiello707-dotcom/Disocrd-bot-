package dev.vesca.client;

import dev.vesca.client.module.Modules;
import dev.vesca.client.render.R2D;
import dev.vesca.client.ui.ClickGui;
import dev.vesca.client.ui.Welcome;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** VescaClient – biało-czarne menu, listy serwerów i światów, ekran wczytywania, powitanie i ClickGUI. */
public class VescaClient implements ClientModInitializer {
    public static final String VERSION = "1.5.0";
    /** klawisz otwierania ClickGUI (domyślnie prawy Shift, zmienisz w Opcje → Sterowanie) */
    public static KeyBinding OPEN_GUI;

    @Override
    public void onInitializeClient() {
        R2D.initShaders();
        Welcome.init();
        Modules.init();
        dev.vesca.client.ui.Theme.load();
        OPEN_GUI = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.vescaclient.clickgui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.vescaclient"));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (OPEN_GUI.wasPressed()) if (mc.currentScreen == null && mc.player != null) mc.setScreen(new ClickGui());
        });
    }
}
