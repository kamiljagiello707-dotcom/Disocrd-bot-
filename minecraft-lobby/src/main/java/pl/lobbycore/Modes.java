package pl.lobbycore;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Rozpoznawanie trybu po świecie, liczniki online i przenoszenie graczy (lokalnie albo przez proxy). */
final class Modes implements PluginMessageListener {
    static final String CHANNEL = "BungeeCord";
    private final LobbyCore plugin;
    private final Map<String, Integer> proxyCounts = new ConcurrentHashMap<>();

    Modes(LobbyCore plugin) { this.plugin = plugin; }

    Mode of(World w) {
        if (w == null) return Mode.INNY;
        String n = w.getName();
        var c = plugin.getConfig();
        if (n.equalsIgnoreCase(c.getString("swiaty.lobby", "lobby"))) return Mode.LOBBY;
        if (n.equalsIgnoreCase(c.getString("swiaty.ffa", "ffa"))) return Mode.FFA;
        for (String a : c.getStringList("swiaty.anarchia")) if (n.equalsIgnoreCase(a)) return Mode.ANARCHIA;
        return Mode.INNY;
    }

    Mode of(Player p) { return of(p.getWorld()); }

    World anarchiaWorld() {
        List<String> names = plugin.getConfig().getStringList("swiaty.anarchia");
        World w = names.isEmpty() ? null : Bukkit.getWorld(names.get(0));
        return w != null ? w : Bukkit.getWorlds().get(0);
    }

    private String proxy(Mode m) {
        return plugin.getConfig().getString("proxy." + m.name().toLowerCase(), "");
    }

    int online(Mode m) {
        String server = proxy(m);
        if (!server.isEmpty()) return proxyCounts.getOrDefault(server, 0);
        int n = 0;
        for (Player p : Bukkit.getOnlinePlayers()) if (of(p) == m) n++;
        return n;
    }

    int onlineAll() {
        int n = Bukkit.getOnlinePlayers().size();
        for (Mode m : new Mode[]{Mode.ANARCHIA, Mode.FFA}) if (!proxy(m).isEmpty()) n += online(m);
        return n;
    }

    void send(Player p, Mode target) {
        Mode now = of(p);
        if (now == Mode.FFA && target != Mode.FFA && plugin.ffa.inCombat(p)) {
            p.sendActionBar(Txt.c("&c⚔ Jesteś w walce! Poczekaj &f" + plugin.ffa.combatLeft(p) + "s"));
            p.playSound(p, Sound.ENTITY_VILLAGER_NO, 1, 1);
            return;
        }
        if (target == now && target != Mode.LOBBY) {
            p.sendActionBar(Txt.c("&7Już tu jesteś."));
            return;
        }
        String server = proxy(target);
        if (!server.isEmpty()) {
            p.sendMessage(Txt.c("&8» &7Łączenie z &e" + server + "&7..."));
            connect(p, server);
            return;
        }
        switch (target) {
            case LOBBY -> p.teleport(plugin.data.lobbySpawn());
            case ANARCHIA -> plugin.anarchia.enter(p);
            case FFA -> p.teleport(plugin.ffa.randomSpawn());
            default -> {}
        }
    }

    private void connect(Player p, String server) {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(b)) {
            out.writeUTF("Connect");
            out.writeUTF(server);
            p.sendPluginMessage(plugin, CHANNEL, b.toByteArray());
        } catch (IOException ignored) {}
    }

    /** Pyta proxy o liczbę graczy na serwerach trybów. */
    void requestCounts() {
        Player any = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (any == null) return;
        for (Mode m : new Mode[]{Mode.ANARCHIA, Mode.FFA}) {
            String server = proxy(m);
            if (server.isEmpty()) continue;
            try (ByteArrayOutputStream b = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(b)) {
                out.writeUTF("PlayerCount");
                out.writeUTF(server);
                any.sendPluginMessage(plugin, CHANNEL, b.toByteArray());
            } catch (IOException ignored) {}
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel)) return;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            if (!"PlayerCount".equals(in.readUTF())) return;
            proxyCounts.put(in.readUTF(), in.readInt());
        } catch (IOException ignored) {}
    }

    static Component modeName(Mode m) {
        return Txt.c(switch (m) {
            case ANARCHIA -> "&c&lANARCHIA";
            case FFA -> "&b&lFFA";
            default -> "&e&lLOBBY";
        });
    }
}
