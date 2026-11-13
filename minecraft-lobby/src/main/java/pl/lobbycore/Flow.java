package pl.lobbycore;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Przepływ gracza między trybami: wejście, wyjście, zmiana świata, odrodzenie, czat. */
final class Flow implements Listener {
    private final LobbyCore plugin;
    private final Set<UUID> skipSave = new HashSet<>();
    private final Map<UUID, Mode> diedIn = new HashMap<>();

    Flow(LobbyCore plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        e.joinMessage(Txt.c(plugin.ph(plugin.getConfig().getString("lobby.dolaczenie"), p)));
        if (!p.hasPlayedBefore()) skipSave.add(p.getUniqueId()); // nowy gracz nie ma jeszcze nic na anarchii
        plugin.boards.create(p);
        boolean alreadyInLobby = plugin.modes.of(p) == Mode.LOBBY;
        p.teleport(plugin.data.lobbySpawn());
        if (alreadyInLobby) plugin.lobby.setup(p, false);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && plugin.modes.of(p) == Mode.LOBBY) plugin.lobby.setup(p, true);
            plugin.lobby.refreshVisibility();
        }, 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        e.quitMessage(Txt.c(plugin.ph(plugin.getConfig().getString("lobby.wyjscie"), p)));
        plugin.boards.remove(p);
        plugin.lobby.forget(p);
        plugin.builders.remove(p.getUniqueId());
        skipSave.remove(p.getUniqueId());
    }

    /** Zapis ekwipunku Anarchii zanim gracz ją opuści. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getTo().getWorld() == e.getFrom().getWorld()) return;
        Mode from = plugin.modes.of(e.getFrom().getWorld()), to = plugin.modes.of(e.getTo().getWorld());
        if (from != Mode.ANARCHIA || to == Mode.ANARCHIA) return;
        if (skipSave.remove(e.getPlayer().getUniqueId())) return;
        plugin.anarchia.save(e.getPlayer(), e.getFrom());
    }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        Mode from = plugin.modes.of(e.getFrom()), to = plugin.modes.of(p);
        if (from == to) return; // np. overworld -> nether na anarchii
        if (from == Mode.LOBBY) { p.setAllowFlight(false); p.setFlying(false); }
        switch (to) {
            case LOBBY -> plugin.lobby.setup(p, false);
            case ANARCHIA -> plugin.anarchia.apply(p);
            case FFA -> plugin.ffa.prepare(p, true);
            default -> {}
        }
        plugin.lobby.refreshVisibility();
        plugin.boards.update(p);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent e) { diedIn.put(e.getEntity().getUniqueId(), plugin.modes.of(e.getEntity())); }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Mode m = diedIn.remove(p.getUniqueId());
        if (m == null) return;
        switch (m) {
            case FFA -> e.setRespawnLocation(plugin.ffa.randomSpawn());
            case LOBBY -> e.setRespawnLocation(plugin.data.lobbySpawn());
            case ANARCHIA -> { if (!e.isBedSpawn() && !e.isAnchorSpawn()) e.setRespawnLocation(plugin.modes.anarchiaWorld().getSpawnLocation()); }
            default -> {}
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            Mode now = plugin.modes.of(p);
            if (now == Mode.FFA && m == Mode.FFA) plugin.ffa.prepare(p, false);
            if (now == Mode.LOBBY && m == Mode.LOBBY) plugin.lobby.setup(p, false);
        });
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        var c = plugin.getConfig();
        String key = switch (plugin.modes.of(e.getPlayer())) {
            case FFA -> "ffa";
            case ANARCHIA -> "anarchia";
            default -> "lobby";
        };
        String fmt = c.getString("czat.format", "{tryb} &f{gracz}&8 » &7")
                .replace("{tryb}", c.getString("czat." + key, ""))
                .replace("{gracz}", e.getPlayer().getName());
        var prefix = Txt.c(fmt);
        e.renderer((source, name, message, viewer) -> prefix.append(message.colorIfAbsent(NamedTextColor.GRAY)));
    }
}
