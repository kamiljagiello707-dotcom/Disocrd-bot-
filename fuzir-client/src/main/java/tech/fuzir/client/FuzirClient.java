package tech.fuzir.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.fuzir.client.config.ConfigManager;
import tech.fuzir.client.event.EventBus;
import tech.fuzir.client.event.events.Render2DEvent;
import tech.fuzir.client.event.events.Render3DEvent;
import tech.fuzir.client.event.events.TickEvent;
import tech.fuzir.client.gui.ClickGuiScreen;
import tech.fuzir.client.hud.HudManager;
import tech.fuzir.client.module.ModuleManager;
import tech.fuzir.client.render.Blur;

public final class FuzirClient implements ClientModInitializer {
    public static final String NAME = "Fuzir";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static FuzirClient instance;

    private EventBus eventBus;
    private ModuleManager moduleManager;
    private HudManager hudManager;
    private ConfigManager configManager;
    private ClickGuiScreen clickGui;

    public static FuzirClient get() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        eventBus = new EventBus();
        moduleManager = new ModuleManager(eventBus);
        hudManager = new HudManager(moduleManager);
        configManager = new ConfigManager(moduleManager);

        moduleManager.applyDefaults();
        configManager.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null) eventBus.post(TickEvent.INSTANCE);
        });
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            hudManager.render(context, tickDelta);
            eventBus.post(new Render2DEvent(context, tickDelta));
        });
        WorldRenderEvents.LAST.register(context ->
                eventBus.post(new Render3DEvent(context.matrixStack(), context.camera(), context.tickDelta())));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            configManager.save();
            Blur.invalidate();
        });

        LOGGER.info("{} initialized with {} modules", NAME, moduleManager.getModules().size());
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public HudManager getHudManager() {
        return hudManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    /** The ClickGUI is created lazily (it needs a ready text renderer) and kept to preserve its state. */
    public ClickGuiScreen getClickGui() {
        if (clickGui == null) clickGui = new ClickGuiScreen(moduleManager, configManager.getGuiLayout());
        return clickGui;
    }

    public ClickGuiScreen getClickGuiIfCreated() {
        return clickGui;
    }
}
