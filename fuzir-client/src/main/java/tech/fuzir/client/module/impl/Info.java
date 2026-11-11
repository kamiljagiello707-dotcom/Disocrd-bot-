package tech.fuzir.client.module.impl;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import tech.fuzir.client.hud.Hud;
import tech.fuzir.client.module.HudModule;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.util.Canvas;
import tech.fuzir.client.util.Canvas.F;
import tech.fuzir.client.util.Icons;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Info extends HudModule {
    public final BoolSetting ping = add(new BoolSetting("Ping", true));
    public final BoolSetting speed = add(new BoolSetting("Prędkość", true));
    public final BoolSetting server = add(new BoolSetting("Serwer", true));
    private static final float ROW = 15, PAD = 7;

    public Info() {
        super("Informacje", "Ping, prędkość i adres serwera", Icons.ACTIVITY, 0f, 0.105f);
        setEnabledQuiet(true);
    }

    private record Row(char icon, String label, String value) {
    }

    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        if (mc.player == null) return rows;
        if (ping.on() && mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            rows.add(new Row(Icons.ACTIVITY, "Ping", (e == null ? 0 : e.getLatency()) + " ms"));
        }
        if (speed.on()) {
            double dx = mc.player.getX() - mc.player.prevX, dz = mc.player.getZ() - mc.player.prevZ;
            rows.add(new Row(Icons.GAUGE, "Prędkość", String.format(Locale.ROOT, "%.1f b/s", Math.sqrt(dx * dx + dz * dz) * 20).replace('.', ',')));
        }
        if (server.on()) {
            ServerInfo s = mc.getCurrentServerEntry();
            rows.add(new Row(Icons.LAYERS, "Serwer", s == null ? "Świat lokalny" : s.address));
        }
        return rows;
    }

    @Override
    public float[] size(Canvas cv) {
        List<Row> rows = rows();
        if (rows.isEmpty()) return null;
        float lw = 0, vw = 0;
        for (Row r : rows) {
            lw = Math.max(lw, cv.width(r.label, F.REGULAR));
            vw = Math.max(vw, cv.width(r.value, F.REGULAR));
        }
        return new float[]{PAD + 14 + lw + 14 + vw + PAD, rows.size() * ROW + 8};
    }

    @Override
    public void draw(Canvas cv, float x, float y, float w, float h, boolean right) {
        Hud.panel(cv, x, y, w, h, 8);
        float ry = y + 4;
        for (Row r : rows()) {
            float cy = ry + ROW / 2;
            cv.icon(r.icon, x + PAD + 4, cy, Hud.MUTED, F.ICON_SM);
            cv.textMid(r.label, x + PAD + 14, cy, Hud.MUTED, F.REGULAR);
            cv.textRight(r.value, x + w - PAD, cy, Hud.TEXT, F.REGULAR);
            ry += ROW;
        }
    }
}
