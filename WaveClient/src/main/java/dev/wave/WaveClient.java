package dev.wave;

import dev.wave.config.ConfigManager;
import dev.wave.hud.HudManager;
import dev.wave.module.Module;
import dev.wave.module.ModuleManager;
import dev.wave.module.client.ClickGUI;
import dev.wave.module.client.HudEditor;
import dev.wave.module.client.Notifications;
import dev.wave.module.visuals.ESP;
import dev.wave.module.visuals.StorageESP;
import dev.wave.module.visuals.Tracers;
import dev.wave.notification.Notification;
import dev.wave.notification.NotificationManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

public class WaveClient implements ClientModInitializer {
    public static final String NAME = "WaveClient";
    public static final String VERSION = "1.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);
    public static WaveClient INSTANCE;

    public ModuleManager modules;
    public NotificationManager notifications;
    public HudManager hud;
    public ConfigManager config;

    private final Deque<Long> leftClicks = new ArrayDeque<>();
    private final Deque<Long> rightClicks = new ArrayDeque<>();

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        modules = new ModuleManager();
        notifications = new NotificationManager();
        hud = new HudManager();
        config = new ConfigManager();
        config.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> modules.onTick());
        HudRenderCallback.EVENT.register((ctx, tick) -> hud.render(ctx, tick.getTickDelta(true)));
        WorldRenderEvents.LAST.register(ctx -> {
            ESP esp = modules.get(ESP.class);
            Tracers tracers = modules.get(Tracers.class);
            StorageESP storage = modules.get(StorageESP.class);
            if (storage.isEnabled()) storage.render(ctx);
            if (tracers.isEnabled()) tracers.render(ctx, esp);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> config.save());

        LOGGER.info("{} {} loaded with {} modules", NAME, VERSION, modules.getModules().size());
    }

    public void onModuleToggle(Module m) {
        if (m instanceof ClickGUI || m instanceof HudEditor) return;
        Notifications cfg = modules.get(Notifications.class);
        if (m instanceof Notifications) {
            if (m.isEnabled()) notifications.push("Notifications", "Notifications enabled", Notification.Type.INFO);
            return;
        }
        if (cfg.toggles.isOn()) {
            notifications.push(m.getName(), m.isEnabled() ? "Module §fenabled" : "Module §7disabled",
                    m.isEnabled() ? Notification.Type.ENABLED : Notification.Type.DISABLED, m.getIcon());
        }
    }

    // --------------------------------------------------------------------- input

    public void onKey(int key, int action) {
        if (action == GLFW.GLFW_REPEAT) return;
        modules.onBind(key, action);
    }

    public void onMouseButton(int button, int action) {
        if (action == GLFW.GLFW_PRESS && MinecraftClient.getInstance().currentScreen == null) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) leftClicks.add(System.currentTimeMillis());
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) rightClicks.add(System.currentTimeMillis());
        }
        if (button > GLFW.GLFW_MOUSE_BUTTON_RIGHT) modules.onBind(dev.wave.util.KeyUtil.mouse(button), action);
    }

    public int leftCps() { return cps(leftClicks); }
    public int rightCps() { return cps(rightClicks); }

    private static int cps(Deque<Long> d) {
        long now = System.currentTimeMillis();
        while (!d.isEmpty() && now - d.peekFirst() > 1000) d.pollFirst();
        return d.size();
    }
}
