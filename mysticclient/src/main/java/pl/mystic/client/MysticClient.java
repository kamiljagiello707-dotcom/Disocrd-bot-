package pl.mystic.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mystic.client.config.ConfigManager;
import pl.mystic.client.gui.ClickGuiScreen;
import pl.mystic.client.gui.Hud;
import pl.mystic.client.module.ModuleManager;

public class MysticClient implements ClientModInitializer {
    public static final String NAME = "MysticClient";
    public static final String VERSION = "1.0.0";
    public static final Logger LOG = LoggerFactory.getLogger(NAME);

    public static ModuleManager modules;
    private static KeyBinding guiKey;

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();
        ConfigManager.load();

        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mysticclient.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.mysticclient"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ClickGuiScreen());
            }
            modules.onTick(client);
        });

        HudRenderCallback.EVENT.register(Hud::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());

        LOG.info("{} {} załadowany ({} modułów)", NAME, VERSION, modules.all().size());
    }
}
