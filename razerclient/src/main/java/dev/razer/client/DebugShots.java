package dev.razer.client;

import dev.razer.client.module.Module;
import dev.razer.client.ui.ClickGui;
import dev.razer.client.ui.Theme;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.ScreenshotRecorder;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;

public class DebugShots {
    static int t = -1;
    public static void init() {
        if (System.getenv("RAZER_SHOTS") == null) return;
        ScreenEvents.AFTER_INIT.register((c, s, w, h) -> {
            if (s instanceof TitleScreen) ScreenEvents.afterRender(s).register((sc, ctx, mx, my, d) -> RazerClient.get().hud.render(ctx, d));
        });
        ClientTickEvents.END_CLIENT_TICK.register(DebugShots::tick);
    }
    static void page(String p) {
        try {
            Class<?> pe = Class.forName("dev.razer.client.ui.ClickGui$Page");
            Field f = ClickGui.class.getDeclaredField("page"); f.setAccessible(true);
            for (Object o : pe.getEnumConstants()) if (o.toString().equals(p)) f.set(null, o);
            Field pt = ClickGui.class.getDeclaredField("pageTime"); pt.setAccessible(true);
            pt.set(MinecraftClient.getInstance().currentScreen, System.currentTimeMillis());
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    static void shot(String n) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ScreenshotRecorder.saveScreenshot(mc.runDirectory, n + ".png", mc.getFramebuffer(), m -> {});
    }
    static void tick(MinecraftClient mc) {
        if (t < 0) { if (mc.currentScreen instanceof TitleScreen) t = 0; else return; }
        t++;
        RazerClient rc = RazerClient.get();
        switch (t) {
            case 60 -> {
                for (String n : new String[]{"KillAura", "Sprint", "ESP", "Fullbright", "Scaffold", "Velocity"}) rc.modules.get(n).setEnabledSilent(true);
                rc.modules.get("KillAura").setKey(GLFW.GLFW_KEY_R);
                rc.modules.get("Speed").setKey(GLFW.GLFW_KEY_F);
                rc.modules.get("ESP").setKey(GLFW.GLFW_KEY_V);
                rc.modules.get("Criticals").setEnabled(true, false);
                rc.modules.get("KillAura").setEnabled(false, true);
                rc.modules.get("KillAura").setEnabled(true, true);
            }
            case 80 -> shot("hud");
            case 90 -> mc.setScreen(new ClickGui());
            case 92 -> shot("gui_opening");
            case 120 -> shot("gui_combat");
            case 125 -> { rc.modules.get("KillAura").expanded = true; rc.modules.get("Velocity").expanded = true; }
            case 150 -> shot("gui_expanded");
            case 155 -> page("THEMES");
            case 180 -> shot("gui_themes");
            case 185 -> page("CONFIG");
            case 210 -> shot("gui_config");
            case 215 -> page("PROFILE");
            case 240 -> shot("gui_profile");
            case 245 -> page("SETTINGS");
            case 270 -> shot("gui_settings");
            case 275 -> { page("CATEGORY"); Theme.set(Theme.Preset.LIGHT); }
            case 305 -> shot("gui_light");
            case 310 -> mc.setScreen(null);
            case 340 -> shot("hud_light");
            case 345 -> { Theme.setInstant(Theme.Preset.DARK); mc.scheduleStop(); }
        }
    }
}
