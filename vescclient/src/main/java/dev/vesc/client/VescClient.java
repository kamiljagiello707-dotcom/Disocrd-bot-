package dev.vesc.client;

import dev.vesc.client.config.ClientSettings;
import dev.vesc.client.config.ConfigManager;
import dev.vesc.client.hud.Hud;
import dev.vesc.client.hud.Notifications;
import dev.vesc.client.lang.Lang;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import dev.vesc.client.module.Module;
import dev.vesc.client.module.ModuleManager;
import dev.vesc.client.ui.ClickGui;
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

public class VescClient implements ClientModInitializer {
    public static final String NAME = "VescClient";
    public static final String VERSION = "5.1.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);
    private static VescClient instance;

    public final ClientSettings settings = new ClientSettings();
    public final dev.vesc.client.config.Friends friends = new dev.vesc.client.config.Friends();
    public ModuleManager modules;
    public Notifications notifications;
    public Hud hud;
    public ConfigManager config;
    public final long startTime = System.currentTimeMillis();

    private KeyBinding openGui;
    private final Set<Integer> held = new HashSet<>();

    public static VescClient get() { return instance; }

    @Override
    public void onInitializeClient() {
        instance = this;
        dev.vesc.client.render.R2D.initShaders();
        notifications = new Notifications();
        modules = new ModuleManager();
        hud = new Hud();
        config = new ConfigManager();
        config.loadAll();
        if (modules.get("DiscordRPC") instanceof dev.vesc.client.module.DiscordPresence dp) dp.ensureRunning();

        openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.vescclient.gui",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.vescclient"));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            if (!(MinecraftClient.getInstance().currentScreen instanceof ClickGui))
                hud.render(ctx, tickCounter.getTickDelta(true));
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            dev.vesc.client.hud.Tps.reset();
            if (settings.welcome.get())
                notifications.info("sparkles", "VescClient " + VERSION, Lang.format("welcome", dev.vesc.client.module.Legit.name()));
        }));
        dev.vesc.client.hud.ChatDrag.init();
        dev.vesc.client.render.TargetEspRenderer.init();
        dev.vesc.client.render.CosmeticsRenderer.init();
        dev.vesc.client.render.NametagRenderer.init();
        dev.vesc.client.module.Legit.init();
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> config.save(config.current()));
        LOGGER.info("{} {} załadowany", NAME, VERSION);
    }

    private void tick(MinecraftClient mc) {
        hud.stats.tick(mc);
        dev.vesc.client.module.Legit.tick(mc);
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
