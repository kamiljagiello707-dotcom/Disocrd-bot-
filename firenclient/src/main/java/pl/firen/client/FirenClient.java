package pl.firen.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import pl.firen.client.gui.ClickGuiScreen;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import pl.firen.client.rotation.RotationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.firen.client.config.ConfigManager;
import pl.firen.client.hud.HudRenderer;
import pl.firen.client.module.ModuleManager;

public final class FirenClient implements ClientModInitializer {
    public static final String VERSION = "0.1.0";
    public static final Logger LOGGER = LoggerFactory.getLogger("FirenClient");

    private static ModuleManager modules;
    private static ConfigManager config;

    private static KeyBinding openGuiKey;

    public static ModuleManager modules() { return modules; }
    /** Klawisz otwierania GUI (do zmiany w Opcje → Sterowanie → FirenClient). */
    public static KeyBinding openGuiKey() { return openGuiKey; }
    public static ConfigManager config() { return config; }

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();
        config = new ConfigManager();
        config.load();

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.firenclient.clickgui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.firenclient"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ClickGuiScreen());
            }
        });

        // Przycisk "FirenClient" w menu pauzy (ESC)
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof GameMenuScreen) {
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("FirenClient"),
                        b -> client.setScreen(new ClickGuiScreen())).dimensions(8, 8, 98, 20).build());
            }
        });

        HudRenderer hud = new HudRenderer();
        HudRenderCallback.EVENT.register(hud::render);
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            RotationManager.beginTick();
            pl.firen.client.render.OutlineManager.clear();
            modules.preTick(client);
            RotationManager.endTick();
        });
        ClientTickEvents.END_CLIENT_TICK.register(modules::tick);
        WorldRenderEvents.LAST.register(modules::renderWorld);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> config.save());

        LOGGER.info("FirenClient {} załadowany — Right Shift otwiera ClickGUI", VERSION);
    }
}
