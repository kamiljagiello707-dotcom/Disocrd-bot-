package dev.vesc.client.ui;

import dev.vesc.client.lang.Lang;
import dev.vesc.client.render.Fonts;
import dev.vesc.client.render.Fonts.Font;
import dev.vesc.client.render.R2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.DirectConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.option.ServerList;

/** Wybór serwera w stylu VescClient: lista serwerów z pingiem, dołączanie, dodawanie i bezpośrednie połączenie. */
public class ServerSelectScreen extends VescScreen {
    private ServerList servers;
    private final MultiplayerServerListPinger pinger = new MultiplayerServerListPinger();

    public ServerSelectScreen(Screen parent) { super(parent, "Multiplayer"); }

    @Override protected String subtitle() { return Lang.pl() ? "Wybierz serwer do gry" : "Pick a server to play on"; }
    @Override protected String subtitleEmpty() { return Lang.pl() ? "Brak serwerów – dodaj pierwszy" : "No servers yet – add one"; }

    @Override
    protected void onOpen() {
        servers = new ServerList(mc);
        servers.loadFile();
        pingAll();
    }

    private void pingAll() {
        pinger.cancel();
        if (servers == null) return;
        for (int i = 0; i < servers.size(); i++) {
            ServerInfo info = servers.get(i);
            info.label = net.minecraft.text.Text.literal(Lang.pl() ? "Pingowanie..." : "Pinging...");
            info.ping = -1L;
            try { pinger.add(info, () -> {}, () -> {}); } catch (Exception ignored) {}
        }
    }

    @Override public void tick() { pinger.tick(); }
    @Override public void removed() { pinger.cancel(); }

    @Override protected float rowHeight() { return 42; }
    @Override protected int rowCount() { return servers == null ? 0 : servers.size(); }

    @Override
    protected void drawRow(DrawContext ctx, int i, float x, float y, float w, float h, boolean hovered, boolean sel) {
        ServerInfo s = servers.get(i);
        int accent = accent();
        float bg = anim("row" + i).animate(hovered || sel ? 1 : 0);
        R2D.roundRect(ctx, x, y, w, h, 11, R2D.blend(0x14FFFFFF, 0x24FFFFFF, bg));
        R2D.roundOutline(ctx, x, y, w, h, 11, 0.5f, sel ? R2D.withAlpha(accent, 0.8f) : R2D.withAlpha(0xFFFFFFFF, 0.06f + 0.08f * bg));
        if (sel) R2D.roundRect(ctx, x + 2, y + h / 2f - 8, 2.5f, 16, 1.25f, accent);

        monogram(ctx, x + 7, y + 7, h - 14, s.name, accent);
        float tx = x + h, tw = w - h - 86;
        Fonts.drawV(ctx, Fonts.trim(s.name, Font.BOLD, 0.84f, tw), tx, y + 14, 0xFFF4F3F8, Font.BOLD, 0.84f);
        Fonts.drawV(ctx, Fonts.trim(s.address, Font.REGULAR, 0.62f, tw), tx, y + 26, 0xFF7E7E8A, Font.REGULAR, 0.62f);
        String motd = s.label != null ? s.label.getString().replace("\n", " ") : "";
        if (!motd.isEmpty()) Fonts.drawV(ctx, Fonts.trim(motd, Font.REGULAR, 0.58f, tw), tx, y + 36, 0xFF60606A, Font.REGULAR, 0.58f);

        // prawy: liczba graczy + ping
        boolean ok = s.ping >= 0 && s.getStatus() == ServerInfo.Status.SUCCESSFUL;
        String count = s.players != null ? s.players.online() + "/" + s.players.max() : "—";
        Fonts.drawV(ctx, count, x + w - 12 - Fonts.width(count, Font.BOLD, 0.66f), y + 15, ok ? 0xFFF4F3F8 : 0xFF6A6A74, Font.BOLD, 0.66f);
        // paski pingu (3)
        int bars = !ok ? 0 : s.ping < 150 ? 3 : s.ping < 400 ? 2 : 1;
        int pc = !ok ? 0xFF6A6A74 : s.ping < 150 ? 0xFF4ADE80 : s.ping < 400 ? 0xFFF2C94C : 0xFFEF4444;
        float bxp = x + w - 12 - 16, byp = y + 28;
        for (int b = 0; b < 3; b++) {
            float bh = 3 + b * 2.5f;
            R2D.roundRect(ctx, bxp + b * 6, byp + (8 - bh), 4, bh, 1.5f, b < bars ? pc : R2D.withAlpha(0xFFFFFFFF, 0.12f));
        }
        if (ok) { String ms = s.ping + "ms"; Fonts.drawV(ctx, ms, bxp - 4 - Fonts.width(ms, Font.SEMIBOLD, 0.54f), y + 31, 0xFF7E7E8A, Font.SEMIBOLD, 0.54f); }
    }

    @Override
    protected void onRow(int i, int button, boolean dbl) {
        ServerInfo s = servers.get(i);
        if (button == 1) { servers.remove(s); servers.saveFile(); if (selected >= rowCount()) selected = -1; return; }
        if (dbl) join(s);
    }

    private void join(ServerInfo s) {
        if (s == null || s.address == null || s.address.isBlank()) return;
        ServerAddress addr = ServerAddress.parse(s.address);
        ConnectScreen.connect(this, mc, addr, s, false, null);
    }

    @Override
    protected void drawFooter(DrawContext ctx, float x, float y, float w) {
        float g = 7, bw = (w - g * 3) / 4f, h = 26;
        footBtn(ctx, "join", x, y, bw, h, "player", Lang.pl() ? "Graj" : "Join", true, false,
                () -> { if (selected >= 0 && selected < rowCount()) join(servers.get(selected)); });
        footBtn(ctx, "direct", x + (bw + g), y, bw, h, "wifi", Lang.pl() ? "Bezpośrednio" : "Direct", false, false,
                () -> { ServerInfo tmp = new ServerInfo("", "", ServerInfo.ServerType.OTHER);
                        mc.setScreen(new DirectConnectScreen(this, ok -> { if (ok) join(tmp); else mc.setScreen(this); }, tmp)); });
        footBtn(ctx, "add", x + (bw + g) * 2, y, bw, h, "plus", Lang.pl() ? "Dodaj" : "Add", false, false,
                () -> { ServerInfo tmp = new ServerInfo(Lang.pl() ? "Nowy serwer" : "New Server", "", ServerInfo.ServerType.OTHER);
                        mc.setScreen(new AddServerScreen(this, ok -> { if (ok) { servers.add(tmp, false); servers.saveFile(); pingAll(); } mc.setScreen(this); }, tmp)); });
        footBtn(ctx, "refresh", x + (bw + g) * 3, y, bw, h, "refresh", Lang.pl() ? "Odśwież" : "Refresh", false, false, this::pingAll);
    }
}
