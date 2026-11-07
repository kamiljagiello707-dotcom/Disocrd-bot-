package dev.mustic.client;

import dev.mustic.client.config.ConfigManager;
import dev.mustic.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MusticClient implements ClientModInitializer {
    public static final String NAME = "MusticClient";
    public static final String VERSION = "0.1.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        ConfigManager.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> ModuleManager.onTick());
        HudRenderCallback.EVENT.register((context, tickCounter) ->
                ModuleManager.onRender2D(context, tickCounter.getTickDelta(true)));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());

        LOGGER.info("{} {} zaladowany ({} modulow)", NAME, VERSION, ModuleManager.getModules().size());
    }
}
