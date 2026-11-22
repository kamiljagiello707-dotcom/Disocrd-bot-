package dev.with.client.ui;

import dev.with.client.render.Fonts;
import dev.with.client.render.Fonts.Font;
import dev.with.client.render.R2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.DirectConnectScreen;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static dev.with.client.ui.Ui.*;

/** Wybór serwera WithClient: lista z ikonami serwerów, pingiem i graczami; dołączanie, dodawanie, edycja, usuwanie. */
public class ServersScreen extends ListScreen {
    private static final ExecutorService PING_POOL = Executors.newFixedThreadPool(5, r -> {
        Thread t = new Thread(r, "WithClient server pinger");
        t.setDaemon(true);
        return t;
    });

    private ServerList servers;
    private final MultiplayerServerListPinger pinger = new MultiplayerServerListPinger();
    /** ikony serwerów: adres -> (bajty PNG, tekstura) */
    private final Map<String, Object[]> icons = new HashMap<>();
    private ServerInfo editing;

    public ServersScreen(Screen parent) { super(parent, Ui.t("Tryb wieloosobowy", "Multiplayer")); }

    @Override
    protected void init() {
        if (servers == null) {
            servers = new ServerList(mc);
            servers.loadFile();
        } else {
            // po powrocie z innego ekranu pingi przerwane w removed() trzeba zacząć od nowa
            for (int i = 0; i < servers.size(); i++) if (servers.get(i).getStatus() == ServerInfo.Status.PINGING) servers.get(i).setStatus(ServerInfo.Status.INITIAL);
        }
        pingAll(false);
    }

    private void pingAll(boolean force) {
        for (int i = 0; i < servers.size(); i++) {
            ServerInfo info = servers.get(i);
            if (!force && info.getStatus() != ServerInfo.Status.INITIAL) continue;
            info.setStatus(ServerInfo.Status.PINGING);
            info.label = Text.literal(t("Pingowanie…", "Pinging…"));
            info.ping = -1L;
            info.playerListSummary = null;
            PING_POOL.submit(() -> {
                try {
                    pinger.add(info, () -> mc.execute(servers::saveFile), () -> {});
                } catch (Exception e) {
                    info.setStatus(ServerInfo.Status.UNREACHABLE);
                    info.label = Text.literal(t("Nie można połączyć", "Can't connect to server"));
                }
            });
        }
    }

    @Override public void tick() { pinger.tick(); }

    @Override
    public void removed() {
        pinger.cancel();
    }

    @Override
    protected void back() {
        for (Object[] o : icons.values()) mc.getTextureManager().destroyTexture((Identifier) o[1]);
        icons.clear();
        super.back();
    }

    // ------------------------------------------------------------------ lista

    @Override protected String subtitle() {
        int n = servers == null ? 0 : servers.size();
        return n == 0 ? t("Dodaj serwer, żeby zacząć", "Add a server to get started")
                : t(n + (n == 1 ? " serwer" : n < 5 ? " serwery" : " serwerów") + " • wybierz i dołącz", n + (n == 1 ? " server" : " servers") + " • pick one and join");
    }
    @Override protected String emptyText() { return t("Brak serwerów – kliknij „Dodaj”", "No servers yet – click \"Add\""); }
    @Override protected String emptyIcon() { return "l_users"; }
    @Override protected int rowCount() { return servers == null ? 0 : servers.size(); }
    @Override protected String rowSearch(int i) { ServerInfo s = servers.get(i); return s.name + " " + s.address; }
    @Override protected float rowHeight() { return 42; }

    private Identifier icon(ServerInfo s) {
        byte[] fav = s.getFavicon();
        Object[] cur = icons.get(s.address);
        if (fav == null) return cur != null ? (Identifier) cur[1] : null;
        if (cur != null && cur[0] == fav) return (Identifier) cur[1];
        try {
            NativeImage img = NativeImage.read(fav);
            Identifier id = Identifier.of("withclient", "servers/" + Integer.toHexString(s.address.hashCode()) + "_" + Integer.toHexString(Arrays.hashCode(fav)));
            if (cur != null) mc.getTextureManager().destroyTexture((Identifier) cur[1]);
            mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
            icons.put(s.address, new Object[]{fav, id});
            return id;
        } catch (Exception e) {
            icons.put(s.address, new Object[]{fav, null});
            return null;
        }
    }

    @Override
    protected void drawRow(DrawContext ctx, int i, float x, float y, float w, float h, float hover, boolean sel) {
        ServerInfo s = servers.get(i);
        float is = h - 12;
        Identifier tex = icon(s);
        if (tex != null) {
            R2D.roundTexture(ctx, tex, x + 10, y + 6, is, is, is * 0.24f, 0, 0, 1, 1, 0xFFFFFFFF);
            R2D.roundOutline(ctx, x + 10, y + 6, is, is, is * 0.24f, 0.6f, 0x2AFFFFFF);
        } else monogram(ctx, x + 10, y + 6, is, s.name);

        float tx = x + 10 + is + 10, tw = w - (tx - x) - 96;
        Fonts.draw(ctx, Fonts.trim(s.name, Font.BOLD, 0.82f, tw), tx, y + 7, TEXT, Font.BOLD, 0.82f);
        String motd = s.label != null ? s.label.getString().replace("\n", "  ").replaceAll("§.", "") : "";
        Fonts.draw(ctx, Fonts.trim(motd, Font.REGULAR, 0.6f, tw), tx, y + 19, DIM, Font.REGULAR, 0.6f);
        Fonts.draw(ctx, Fonts.trim(s.address, Font.REGULAR, 0.56f, tw), tx, y + 29, FAINT, Font.REGULAR, 0.56f);

        // prawa strona: gracze + paski pingu
        boolean ok = s.getStatus() == ServerInfo.Status.SUCCESSFUL && s.ping >= 0;
        boolean pinging = s.getStatus() == ServerInfo.Status.PINGING || s.getStatus() == ServerInfo.Status.INITIAL;
        float rx = x + w - 12;
        String count = s.players != null ? s.players.online() + " / " + s.players.max() : "—";
        float cw = Fonts.width(count, Font.BOLD, 0.68f);
        Fonts.draw(ctx, count, rx - cw, y + 8, ok ? TEXT : FAINT, Font.BOLD, 0.68f);
        int bars = !ok ? 0 : s.ping < 100 ? 4 : s.ping < 200 ? 3 : s.ping < 400 ? 2 : 1;
        float bx = rx - 19, by = y + 22;
        for (int b = 0; b < 4; b++) {
            float bh = 3.5f + b * 1.7f;
            int c = pinging ? R2D.withAlpha(0xFFFFFFFF, 0.15f + 0.5f * (0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 160.0 - b)))
                    : b < bars ? 0xFFFFFFFF : 0x22FFFFFF;
            R2D.roundRect(ctx, bx + b * 5, by + (8.6f - bh), 3.2f, bh, 1f, c);
        }
        String ms = ok ? s.ping + " ms" : pinging ? "…" : s.getStatus() == ServerInfo.Status.INCOMPATIBLE ? t("inna wersja", "incompatible") : "offline";
        float mw = Fonts.width(ms, Font.SEMIBOLD, 0.56f);
        Fonts.draw(ctx, ms, bx - 5 - mw, y + 24, ok ? DIM : FAINT, Font.SEMIBOLD, 0.56f);
    }

    // ------------------------------------------------------------------ akcje

    @Override protected void activate(int i) { join(servers.get(i)); }

    private void join(ServerInfo s) {
        if (s == null || s.address == null || s.address.isBlank()) return;
        Loading.target = s.name != null && !s.name.isBlank() ? s.name : s.address;
        Recent.server(Loading.target, s.address);
        ConnectScreen.connect(this, mc, ServerAddress.parse(s.address), s, false, null);
    }

    private void add() {
        editing = new ServerInfo(t("Serwer Minecraft", "Minecraft Server"), "", ServerInfo.ServerType.OTHER);
        mc.setScreen(new AddServerScreen(this, ok -> {
            if (ok) {
                ServerInfo e = servers.tryUnhide(editing.address);
                if (e != null) { e.copyWithSettingsFrom(editing); } else servers.add(editing, false);
                servers.saveFile();
                selected = servers.size() - 1;
            }
            mc.setScreen(this);
        }, editing));
    }

    private void edit() {
        if (!hasSelection()) return;
        ServerInfo orig = servers.get(selected);
        editing = new ServerInfo(orig.name, orig.address, ServerInfo.ServerType.OTHER);
        editing.copyWithSettingsFrom(orig);
        mc.setScreen(new AddServerScreen(this, ok -> {
            if (ok) {
                orig.copyFrom(editing);
                orig.setStatus(ServerInfo.Status.INITIAL);
                servers.saveFile();
            }
            mc.setScreen(this);
        }, editing));
    }

    private void delete() {
        if (!hasSelection()) return;
        ServerInfo s = servers.get(selected);
        mc.setScreen(new ConfirmScreen(ok -> {
            if (ok) {
                servers.remove(s);
                servers.saveFile();
                selected = -1;
            }
            mc.setScreen(this);
        }, Text.translatable("selectServer.deleteQuestion"), Text.translatable("selectServer.deleteWarning", s.name),
                Text.translatable("selectServer.deleteButton"), ScreenTexts.CANCEL));
    }

    private void direct() {
        editing = new ServerInfo(t("Serwer Minecraft", "Minecraft Server"), "", ServerInfo.ServerType.OTHER);
        mc.setScreen(new DirectConnectScreen(this, ok -> {
            if (ok) join(editing);
            else mc.setScreen(this);
        }, editing));
    }

    @Override protected void rightClick(int i) { edit(); }

    @Override
    protected List<Btn> buttons() {
        boolean sel = hasSelection();
        return List.of(
                new Btn("join", "l_play", t("Dołącz", "Join"), true, sel, () -> join(servers.get(selected))),
                new Btn("direct", "l_wifi", t("Bezpośrednio", "Direct"), false, true, this::direct),
                new Btn("add", "l_plus", t("Dodaj", "Add"), false, true, this::add),
                new Btn("edit", "l_pencil", t("Edytuj", "Edit"), false, sel, this::edit),
                new Btn("delete", "l_trash_2", t("Usuń", "Delete"), false, sel, this::delete),
                new Btn("l_refresh_cw", "l_refresh_cw", t("Odśwież", "Refresh"), false, true, () -> pingAll(true))
        );
    }
}
