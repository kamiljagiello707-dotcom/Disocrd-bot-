package pl.lobbycore;

import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Arena FFA: kit, zabójstwa, serie, ochrona po spawnie, blokada ucieczki z walki. */
final class Ffa implements Listener {
    private final LobbyCore plugin;
    private final Map<UUID, Long> protectedUntil = new HashMap<>();
    private final Map<UUID, Long> combatUntil = new HashMap<>();
    private final Map<UUID, UUID> lastAttacker = new HashMap<>();

    Ffa(LobbyCore plugin) { this.plugin = plugin; }

    private boolean in(Player p) { return plugin.modes.of(p) == Mode.FFA; }

    Location randomSpawn() {
        List<Location> spawns = plugin.data.ffaSpawns();
        if (spawns.isEmpty()) return plugin.ffaWorld.getSpawnLocation();
        return spawns.get(ThreadLocalRandom.current().nextInt(spawns.size()));
    }

    boolean inCombat(Player p) { return combatUntil.getOrDefault(p.getUniqueId(), 0L) > System.currentTimeMillis(); }
    long combatLeft(Player p) { return Math.max(0, (combatUntil.getOrDefault(p.getUniqueId(), 0L) - System.currentTimeMillis() + 999) / 1000); }

    void prepare(Player p, boolean title) {
        Lobby.reset(p);
        p.setGameMode(plugin.builders.contains(p.getUniqueId()) ? GameMode.CREATIVE : GameMode.ADVENTURE);
        p.setAllowFlight(p.getGameMode() == GameMode.CREATIVE);
        giveKit(p);
        protectedUntil.put(p.getUniqueId(), System.currentTimeMillis() + plugin.getConfig().getInt("ffa.ochrona-po-spawnie", 3) * 1000L);
        combatUntil.remove(p.getUniqueId());
        if (title) p.showTitle(Title.title(Txt.c("&b&lFFA"), Txt.c("&7Każdy na każdego!"),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1500), Duration.ofMillis(400))));
    }

    private void giveKit(Player p) {
        ConfigurationSection k = plugin.getConfig().getConfigurationSection("ffa.kit");
        if (k == null) return;
        PlayerInventory inv = p.getInventory();
        inv.setHelmet(Items.parse(k.getString("helm")));
        inv.setChestplate(Items.parse(k.getString("klata")));
        inv.setLeggings(Items.parse(k.getString("spodnie")));
        inv.setBoots(Items.parse(k.getString("buty")));
        for (String s : k.getStringList("przedmioty")) {
            ItemStack it = Items.parse(s);
            if (it != null) inv.addItem(it);
        }
    }

    void forget(Player p) {
        protectedUntil.remove(p.getUniqueId());
        combatUntil.remove(p.getUniqueId());
        lastAttacker.remove(p.getUniqueId());
    }

    // ───────────── walka ─────────────

    private static Player attacker(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) return p;
        if (e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player p) return p;
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim) || !in(victim)) return;
        Player att = attacker(e);
        if (att == null || att == victim) return;
        long now = System.currentTimeMillis();
        if (protectedUntil.getOrDefault(victim.getUniqueId(), 0L) > now) {
            e.setCancelled(true);
            att.sendActionBar(Txt.c("&7Ten gracz ma jeszcze &bochronę spawnu&7."));
            return;
        }
        protectedUntil.remove(att.getUniqueId()); // atak zdejmuje własną ochronę
        long until = now + plugin.getConfig().getInt("ffa.walka-sekundy", 10) * 1000L;
        if (!inCombat(victim)) victim.sendActionBar(Txt.c("&c⚔ Jesteś w walce!"));
        combatUntil.put(victim.getUniqueId(), until);
        combatUntil.put(att.getUniqueId(), until);
        lastAttacker.put(victim.getUniqueId(), att.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onVoid(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && in(p) && e.getCause() == EntityDamageEvent.DamageCause.VOID) e.setDamage(1000);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        if (!in(victim)) return;
        e.getDrops().clear();
        e.setDroppedExp(0);
        e.deathMessage(null);

        Player killer = victim.getKiller();
        if (killer == null) {
            UUID last = lastAttacker.get(victim.getUniqueId());
            if (last != null && inCombat(victim)) killer = Bukkit.getPlayer(last);
        }
        plugin.stats.addDeath(victim.getUniqueId());
        forget(victim);

        if (killer != null && killer != victim && in(killer)) reward(killer, victim);
        else broadcast("&c☠ &f" + victim.getName() + " &7zginął.");
    }

    @SuppressWarnings("deprecation")
    private void reward(Player killer, Player victim) {
        double hp = Math.ceil(killer.getHealth()) / 2.0;
        int streak = plugin.stats.addKill(killer.getUniqueId());
        broadcast("&c☠ &f" + victim.getName() + " &7zginął od &f" + killer.getName() + " &8(&c" + hp + "❤&8)");
        killer.setHealth(killer.getMaxHealth());
        killer.setFoodLevel(20);
        killer.sendActionBar(Txt.c("&a+1 zabójstwo &8| &6seria " + streak));
        killer.playSound(killer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1.2f);
        killer.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, killer.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.2);
        if (streak % 5 == 0) {
            Bukkit.broadcast(Txt.c("&8[&bFFA&8] &f" + killer.getName() + " &7ma serię &6&l" + streak + " &7zabójstw!"));
            for (Player p : Bukkit.getOnlinePlayers()) if (in(p)) p.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 1.6f);
        }
    }

    private void broadcast(String msg) {
        for (Player p : Bukkit.getOnlinePlayers()) if (in(p)) p.sendMessage(Txt.c(msg));
    }

    /** Wyjście z serwera w trakcie walki = śmierć i zabójstwo dla przeciwnika. */
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (in(p) && inCombat(p)) {
            UUID last = lastAttacker.get(p.getUniqueId());
            plugin.stats.addDeath(p.getUniqueId());
            Player killer = last == null ? null : Bukkit.getPlayer(last);
            if (killer != null && in(killer)) {
                plugin.stats.addKill(killer.getUniqueId());
                broadcast("&c☠ &f" + p.getName() + " &7uciekł z walki z &f" + killer.getName() + "&7.");
            }
        } else plugin.stats.resetStreak(p.getUniqueId());
        forget(p);
    }

    // ───────────── ochrona areny ─────────────

    private boolean locked(Player p) { return in(p) && !plugin.builders.contains(p.getUniqueId()); }
    @EventHandler public void onBreak(BlockBreakEvent e) { if (locked(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onPlace(BlockPlaceEvent e) { if (locked(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onDrop(PlayerDropItemEvent e) { if (locked(e.getPlayer())) e.setCancelled(true); }
}
