package dev.razer.client;

import dev.razer.client.config.ClientSettings;
import dev.razer.client.config.ConfigManager;
import dev.razer.client.hud.Hud;
import dev.razer.client.hud.Notifications;
import dev.razer.client.module.Module;
import dev.razer.client.module.ModuleManager;
import dev.razer.client.ui.ClickGui;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class RazerClient implements ClientModInitializer {
    public static final String NAME = "RazerClient";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);
    private static RazerClient instance;

    public final ClientSettings settings = new ClientSettings();
    public ModuleManager modules;
    public Notifications notifications;
    public Hud hud;
    public ConfigManager config;
    public final long startTime = System.currentTimeMillis();

    private KeyBinding openGui;
    private final Set<Integer> held = new HashSet<>();

    public static RazerClient get() { return instance; }

    @Override
    public void onInitializeClient() {
        instance = this;
        notifications = new Notifications();
        modules = new ModuleManager();
        hud = new Hud();
        config = new ConfigManager();
        config.loadAll();

        openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.razerclient.gui",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.razerclient"));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            if (!(MinecraftClient.getInstance().currentScreen instanceof ClickGui))
                hud.render(ctx, tickCounter.getTickDelta(true));
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> config.save(config.current()));
        LOGGER.info("{} {} załadowany", NAME, VERSION);
    }

    private void tick(MinecraftClient mc) {
        while (openGui.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new ClickGui());
        }
        if (mc.currentScreen != null || mc.player == null) { held.clear(); return; }
        long window = mc.getWindow().getHandle();
        for (Module m : modules.all()) {
            if (!m.hasKey()) continue;
            int key = m.getKey();
            boolean down = InputUtil.isKeyPressed(window, key);
            if (down && held.add(key)) {
                for (Module other : modules.all()) if (other.getKey() == key) other.setEnabled(!other.isEnabled(), true);
            } else if (!down) held.remove(key);
        }
    }
}
