package pl.mysticclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mysticclient.config.ConfigManager;
import pl.mysticclient.module.Module;
import pl.mysticclient.module.ModuleManager;
import pl.mysticclient.module.render.Fullbright;
import pl.mysticclient.util.NotificationManager;
import pl.mysticclient.module.render.ModernHud;
import pl.mysticclient.util.UI;

public class MysticClient implements ClientModInitializer {
    public static final String NAME = "MysticClient";
    public static final String VERSION = "V1";
    public static final String VERSION_FULL = "1.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        ConfigManager.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (Module m : ModuleManager.getModules()) {
                if (!m.isEnabled()) continue;
                try { m.onTick(); } catch (Exception e) { LOGGER.error("Błąd w module " + m.getName(), e); }
            }
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            // ClickGUI sam liczy klatki – tylko jeden licznik na klatkę
            if (!(mc.currentScreen instanceof pl.mysticclient.gui.ClickGuiScreen)) UI.Frame.tick();
            if (mc.options.hudHidden) return;
            float td = tickCounter.getTickDelta(true);
            UI.begin(ctx, ModernHud.scale());
            for (Module m : ModuleManager.getModules()) {
                if (m.isEnabled()) m.onRender2D(ctx, td);
            }
            NotificationManager.render();
            UI.end();
        });

        WorldRenderEvents.LAST.register(ctx -> {
            for (Module m : ModuleManager.getModules()) {
                if (m.isEnabled()) m.onRender3D(ctx);
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ConfigManager.save();
            ModuleManager.get(Fullbright.class).restoreForSave();
        });

        LOGGER.info("{} {} załadowany – {} modułów", NAME, VERSION, ModuleManager.getModules().size());
    }
}
