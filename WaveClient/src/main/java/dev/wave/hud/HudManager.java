package dev.wave.hud;

import dev.wave.WaveClient;
import dev.wave.gui.HudEditorScreen;
import dev.wave.module.Module;
import dev.wave.module.visuals.Crosshair;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class HudManager {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public void render(DrawContext c, float delta) {
        if (mc.options.hudHidden) return;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();

        Crosshair crosshair = WaveClient.INSTANCE.modules.get(Crosshair.class);
        if (crosshair.isEnabled() && mc.currentScreen == null) crosshair.render(c, sw, sh, delta);

        if (!(mc.currentScreen instanceof HudEditorScreen)) {
            for (Module m : WaveClient.INSTANCE.modules.getModules()) {
                if (m instanceof HudElement h && h.isEnabled()) {
                    h.prepare(sw, sh);
                    h.render(c, delta, false);
                }
            }
        }
        if (mc.currentScreen == null) WaveClient.INSTANCE.notifications.render(c, sw, sh);
    }
}
