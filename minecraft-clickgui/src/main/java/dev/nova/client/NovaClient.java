package dev.nova.client;

import dev.nova.client.config.ConfigManager;
import dev.nova.client.gui.HudRenderer;
import dev.nova.client.gui.PanelState;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class NovaClient implements ClientModInitializer {
    public static final String VERSION = "1.0";
    private static NovaClient instance;

    public ModuleManager modules;
    public ConfigManager config;
    private final Map<Category, PanelState> panelStates = new EnumMap<>(Category.class);
    private final Map<Module, Boolean> keyWasDown = new HashMap<>();
    private final HudRenderer hud = new HudRenderer();

    public static NovaClient get() { return instance; }

    @Override
    public void onInitializeClient() {
        instance = this;
        modules = new ModuleManager();
        for (Category c : Category.values()) panelStates.put(c, new PanelState(0, 0));

        config = new ConfigManager(this);
        config.load();

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> hud.render(ctx));
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> config.save());
    }

    public PanelState panelState(Category c) { return panelStates.get(c); }

    private void tick(MinecraftClient mc) {
        if (mc.player == null) return;
        long window = mc.getWindow().getHandle();
        for (Module m : modules.all()) {
            int key = m.bind.get();
            boolean down = key >= 0 && mc.currentScreen == null && InputUtil.isKeyPressed(window, key);
            if (down && !keyWasDown.getOrDefault(m, false)) m.toggle();
            keyWasDown.put(m, down);
        }
        modules.onTick();
    }
}
