package dev.wave.hud.elements;

import dev.wave.hud.HudElement;
import dev.wave.setting.BooleanSetting;
import dev.wave.util.RenderUtil;
import dev.wave.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InfoHud extends HudElement {
    public final BooleanSetting fps = new BooleanSetting("FPS", true);
    public final BooleanSetting ping = new BooleanSetting("Ping", true);
    public final BooleanSetting speed = new BooleanSetting("Speed", true);
    public final BooleanSetting server = new BooleanSetting("Server", false);
    public final BooleanSetting biome = new BooleanSetting("Biome", false);

    private double bps;

    public InfoHud() {
        super("Info", "FPS, ping, speed and more");
        addSettings(fps, ping, speed, server, biome, background);
        setEnabledSilently(true);
    }

    @Override
    protected void defaultPosition(int sw, int sh) { x = 4; y = sh - 60; }

    @Override
    public void onTick() {}

    @Override
    public void render(DrawContext c, float delta, boolean editor) {
        List<String[]> lines = new ArrayList<>();
        if (fps.isOn()) lines.add(new String[]{"FPS", String.valueOf(mc.getCurrentFps())});
        if (ping.isOn()) lines.add(new String[]{"Ping", ping() + "ms"});
        if (speed.isOn() && mc.player != null) {
            double dx = mc.player.getX() - mc.player.prevX, dz = mc.player.getZ() - mc.player.prevZ;
            double target = Math.hypot(dx, dz) * 20;
            bps += (target - bps) * 0.15;
            lines.add(new String[]{"Speed", String.format(Locale.ROOT, "%.2f b/s", bps)});
        }
        if (server.isOn()) lines.add(new String[]{"Server", mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "Singleplayer"});
        if (biome.isOn() && mc.player != null && mc.world != null) {
            String b = mc.world.getBiome(mc.player.getBlockPos()).getKey().map(k -> k.getValue().getPath()).orElse("?");
            lines.add(new String[]{"Biome", b.replace('_', ' ')});
        }
        if (lines.isEmpty()) lines.add(new String[]{"Info", "-"});

        float w = 0;
        for (String[] l : lines) w = Math.max(w, RenderUtil.width(l[0] + ": " + l[1]));
        w += 10;
        float h = lines.size() * 10 + 6;
        resize(w, h);
        if (background.isOn()) RenderUtil.round(c, x, y, w, h, 3, Theme.HUD_BG);
        boolean right = rightAligned();
        float ly = y + 4;
        for (String[] l : lines) {
            String s = "§f" + l[0] + "§7: §r" + l[1];
            float tx = right ? x + w - 5 - RenderUtil.width(s) : x + 5;
            RenderUtil.text(c, s, tx, ly, Theme.TEXT_DIM, true);
            ly += 10;
        }
    }

    private int ping() {
        if (mc.getNetworkHandler() == null || mc.player == null) return 0;
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return e == null ? 0 : e.getLatency();
    }
}
