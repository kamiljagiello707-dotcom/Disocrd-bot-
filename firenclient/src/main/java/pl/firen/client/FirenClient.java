package pl.firen.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
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

    public static ModuleManager modules() { return modules; }
    public static ConfigManager config() { return config; }

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();
        config = new ConfigManager();
        config.load();

        HudRenderer hud = new HudRenderer();
        HudRenderCallback.EVENT.register(hud::render);
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            RotationManager.beginTick();
            modules.preTick(client);
            RotationManager.endTick();
        });
        ClientTickEvents.END_CLIENT_TICK.register(modules::tick);
        WorldRenderEvents.LAST.register(modules::renderWorld);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> config.save());

        LOGGER.info("FirenClient {} załadowany — Right Shift otwiera ClickGUI", VERSION);
    }
}
