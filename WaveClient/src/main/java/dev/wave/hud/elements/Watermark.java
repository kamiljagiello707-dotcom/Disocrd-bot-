package dev.wave.hud.elements;

import dev.wave.WaveClient;
import dev.wave.hud.HudElement;
import dev.wave.setting.BooleanSetting;
import dev.wave.setting.ModeSetting;
import dev.wave.util.RenderUtil;
import dev.wave.util.Theme;
import net.minecraft.client.gui.DrawContext;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Watermark extends HudElement {
    public final ModeSetting style = new ModeSetting("Style", "", "Modern", "Modern", "Minimal", "Logo");
    public final BooleanSetting showFps = new BooleanSetting("FPS", true);
    public final BooleanSetting showTime = new BooleanSetting("Time", true);
    public final BooleanSetting showName = new BooleanSetting("Username", false);

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    public Watermark() {
        super("Watermark", "Client logo and info in the corner");
        addSettings(style, showFps, showTime, showName, background);
        setEnabledSilently(true);
    }

    @Override
    protected void defaultPosition(int sw, int sh) { x = 4; y = 4; }

    @Override
    public void render(DrawContext c, float delta, boolean editor) {
        StringBuilder info = new StringBuilder();
        if (showFps.isOn()) info.append(" §7|§f ").append(mc.getCurrentFps()).append(" fps");
        if (showTime.isOn()) info.append(" §7|§f ").append(LocalTime.now().format(TIME));
        if (showName.isOn()) info.append(" §7|§f ").append(mc.getSession().getUsername());

        if (style.is("Minimal")) {
            String s = "§lWave§r§7Client" + info;
            float w = RenderUtil.width(s) + 8;
            resize(w, 14);
            if (background.isOn()) RenderUtil.round(c, x, y, w, 14, 3, Theme.HUD_BG);
            RenderUtil.text(c, s, x + 4, y + 3, Theme.accent(), true);
            return;
        }

        if (style.is("Logo")) {
            float w = 30 + RenderUtil.width(info.toString().replaceFirst(" §7\\|§f ", "")) + 10;
            resize(w, 26);
            if (background.isOn()) RenderUtil.round(c, x, y, w, 26, 5, Theme.BG);
            RenderUtil.round(c, x + 4, y + 4, 18, 18, 4, Theme.accent());
            dev.wave.util.Icons.draw(c, "watermark", x + 7, y + 7, 12, Theme.onAccent());
            RenderUtil.text(c, "§lWaveClient", x + 27, y + 5, Theme.TEXT, false);
            RenderUtil.text(c, info.toString().replaceFirst(" §7\\|§f ", ""), x + 27, y + 15, Theme.TEXT_DIM, false);
            return;
        }

        // Modern: pill with a solid accent chip
        String chip = "WAVE";
        String rest = "Client " + WaveClient.VERSION + info;
        float chipW = RenderUtil.width("§l" + chip) + 20;
        float w = chipW + RenderUtil.width(rest) + 12;
        resize(w, 18);
        if (background.isOn()) {
            RenderUtil.round(c, x, y, w, 18, 5, Theme.BG);
            RenderUtil.roundOutline(c, x, y, w, 18, 5, 0.5f, 0x22FFFFFF);
        }
        RenderUtil.round(c, x + 2, y + 2, chipW, 14, 4, Theme.accent());
        dev.wave.util.Icons.draw(c, "watermark", x + 5, y + 4, 10, Theme.onAccent());
        RenderUtil.text(c, "§l" + chip, x + 17, y + 5, Theme.onAccent(), false);
        RenderUtil.text(c, rest, x + chipW + 7, y + 5, Theme.TEXT, false);
    }
}
