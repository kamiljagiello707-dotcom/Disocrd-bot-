package pl.lobbycore;

import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Wszystko, co dzieje się w świecie lobby. */
final class Lobby implements Listener {
    private final LobbyCore plugin;
    private final Set<UUID> hidePlayers = new HashSet<>();
    private final Map<UUID, Long> cooldown = new HashMap<>();

    Lobby(LobbyCore plugin) { this.plugin = plugin; }

    private boolean in(Player p) { return plugin.modes.of(p) == Mode.LOBBY; }
    private boolean in(org.bukkit.World w) { return plugin.modes.of(w) == Mode.LOBBY; }
    private boolean builder(Player p) { return plugin.builders.contains(p.getUniqueId()); }

    private boolean cooldown(Player p, String what, long ms) {
        long now = System.currentTimeMillis();
        UUID key = UUID.nameUUIDFromBytes((p.getUniqueId() + what).getBytes());
        Long until = cooldown.get(key);
        if (until != null && until > now) return true;
        cooldown.put(key, now + ms);
        return false;
    }

    /** Czyści gracza do zera (ekwipunek, efekty, życie). */
    @SuppressWarnings("deprecation")
    static void reset(Player p) {
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        p.setItemOnCursor(null);
        for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.setSaturation(20);
        p.setExp(0);
        p.setLevel(0);
        p.setFireTicks(0);
        p.setFallDistance(0);
    }

    void setup(Player p, boolean welcome) {
        reset(p);
        p.setGameMode(builder(p) ? GameMode.CREATIVE : GameMode.ADVENTURE);
        p.setAllowFlight(builder(p) || plugin.getConfig().getBoolean("lobby.podwojny-skok", true));
        p.setFlying(false);
        giveItems(p);
        if (welcome) {
            var c = plugin.getConfig();
            p.showTitle(Title.title(Txt.c(plugin.ph(c.getString("lobby.tytul"), p)), Txt.c(plugin.ph(c.getString("lobby.podtytul"), p)),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(700))));
            for (String line : c.getStringList("lobby.powitanie")) p.sendMessage(Txt.c(plugin.ph(line, p)));
            p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
        }
    }

    void giveItems(Player p) {
        var inv = p.getInventory();
        inv.setItem(0, Items.tag(Items.glow(Items.of(Material.COMPASS, "&a&lWybierz tryb &8(PPM)",
                "&7Anarchia, FFA i więcej.")), plugin.keyItem, "tryby"));
        inv.setItem(4, Items.tag(Items.head(p, "&e&lTwój profil &8(PPM)", "&7Statystyki, czas gry, portfel."), plugin.keyItem, "profil"));
        inv.setItem(8, visibilityItem(p));
        inv.setHeldItemSlot(0);
    }

    private ItemStack visibilityItem(Player p) {
        boolean hidden = hidePlayers.contains(p.getUniqueId());
        return Items.tag(Items.of(hidden ? Material.GRAY_DYE : Material.LIME_DYE,
                "&fGracze: " + (hidden ? "&cukryci" : "&awidoczni") + " &8(PPM)", "&7Kliknij, aby przełączyć."), plugin.keyItem, "gracze");
    }

    /** Kto kogo widzi: w lobby można ukryć innych, poza lobby wszyscy widzą wszystkich. */
    void refreshVisibility() {
        for (Player viewer : Bukkit.getOnlinePlayers()) for (Player target : Bukkit.getOnlinePlayers()) {
            if (viewer == target) continue;
            boolean hide = hidePlayers.contains(viewer.getUniqueId()) && in(viewer) && in(target);
            if (hide) viewer.hidePlayer(plugin, target); else viewer.showPlayer(plugin, target);
        }
    }

    void forget(Player p) { hidePlayers.remove(p.getUniqueId()); }

    // ───────────── przedmioty i NPC ─────────────

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!in(p)) return;
        if (e.getAction() == Action.PHYSICAL) {
            if (e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.FARMLAND) e.setCancelled(true);
            return;
        }
        if (!builder(p)) e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        String tag = Items.tagOf(e.getItem(), plugin.keyItem);
        if (tag == null) return;
        e.setCancelled(true);
        switch (tag) {
            case "tryby" -> Menus.selector(plugin, p);
            case "profil" -> Menus.profile(plugin, p);
            case "gracze" -> {
                if (cooldown(p, "vis", 1500)) { p.sendActionBar(Txt.c("&cPoczekaj chwilę.")); return; }
                if (!hidePlayers.remove(p.getUniqueId())) hidePlayers.add(p.getUniqueId());
                refreshVisibility();
                p.getInventory().setItem(8, visibilityItem(p));
                p.playSound(p, Sound.UI_BUTTON_CLICK, 1, 1.2f);
                p.sendActionBar(Txt.c(hidePlayers.contains(p.getUniqueId()) ? "&7Gracze &cukryci" : "&7Gracze &awidoczni"));
            }
        }
    }

    @EventHandler
    public void onNpc(PlayerInteractEntityEvent e) {
        String mode = e.getRightClicked().getPersistentDataContainer().get(plugin.keyNpc, PersistentDataType.STRING);
        if (mode == null) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (cooldown(e.getPlayer(), "npc", 800)) return;
        plugin.modes.send(e.getPlayer(), Mode.valueOf(mode));
    }

    // ───────────── ruch: podwójny skok, wyrzutnie, portale, void ─────────────

    @EventHandler(ignoreCancelled = true)
    public void onFlight(PlayerToggleFlightEvent e) {
        Player p = e.getPlayer();
        if (!in(p) || p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        e.setCancelled(true);
        p.setAllowFlight(false);
        p.setFlying(false);
        p.setVelocity(p.getLocation().getDirection().setY(0).normalize().multiply(1.3).setY(0.95));
        p.playSound(p, Sound.ENTITY_BAT_TAKEOFF, 0.5f, 1.3f);
        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 14, 0.3, 0.05, 0.3, 0.03);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!in(p)) return;
        Location to = e.getTo();

        if (to.getY() < plugin.data.lobbySpawn().getY() - 45) {
            p.teleport(plugin.data.lobbySpawn());
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.2f);
            return;
        }

        if (!p.getAllowFlight() && p.getGameMode() != GameMode.CREATIVE
                && plugin.getConfig().getBoolean("lobby.podwojny-skok", true)
                && p.getLocation().subtract(0, 0.1, 0).getBlock().getType().isSolid()) {
            p.setAllowFlight(true);
        }

        if (e.getFrom().getBlockX() == to.getBlockX() && e.getFrom().getBlockY() == to.getBlockY()
                && e.getFrom().getBlockZ() == to.getBlockZ()) return;

        Block feet = to.getBlock();
        Material t = feet.getType();
        if (t == Material.LIGHT_WEIGHTED_PRESSURE_PLATE && !cooldown(p, "pad", 900)) {
            Vector dir = p.getLocation().getDirection().setY(0).normalize();
            p.setVelocity(dir.multiply(1.9).setY(1.05));
            p.playSound(p, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.1f);
            p.getWorld().spawnParticle(Particle.FIREWORK, p.getLocation(), 25, 0.3, 0.1, 0.3, 0.08);
            return;
        }
        Material head = to.clone().add(0, 1, 0).getBlock().getType();
        if ((t == Material.NETHER_PORTAL || head == Material.NETHER_PORTAL) && !cooldown(p, "portal", 2500)) {
            p.playSound(p, Sound.BLOCK_PORTAL_TRAVEL, 0.25f, 1.6f);
            plugin.modes.send(p, Mode.ANARCHIA);
        } else if ((t == Material.END_GATEWAY || head == Material.END_GATEWAY) && !cooldown(p, "portal", 2500)) {
            p.playSound(p, Sound.BLOCK_END_PORTAL_SPAWN, 0.25f, 1.8f);
            plugin.modes.send(p, Mode.FFA);
        }
    }

    @EventHandler public void onPortal(PlayerPortalEvent e) { if (in(e.getFrom().getWorld())) e.setCancelled(true); }
    @EventHandler public void onEntityPortal(EntityPortalEvent e) { if (in(e.getFrom().getWorld())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onGateway(PlayerTeleportEvent e) {
        if (e.getCause() == PlayerTeleportEvent.TeleportCause.END_GATEWAY && in(e.getFrom().getWorld())) e.setCancelled(true);
    }

    // ───────────── ochrona ─────────────

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageEvent e) {
        Entity en = e.getEntity();
        if (en.getPersistentDataContainer().has(plugin.keyNpc)) { e.setCancelled(true); return; }
        if (!in(en.getWorld())) return;
        e.setCancelled(true);
        if (en instanceof Player p && e.getCause() == EntityDamageEvent.DamageCause.VOID) p.teleport(plugin.data.lobbySpawn());
    }

    @EventHandler public void onBreak(BlockBreakEvent e) { if (in(e.getPlayer()) && !builder(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onPlace(BlockPlaceEvent e) { if (in(e.getPlayer()) && !builder(e.getPlayer())) e.setCancelled(true); }

    @EventHandler
    public void onFood(FoodLevelChangeEvent e) {
        if (in(e.getEntity().getWorld())) { e.setCancelled(true); e.getEntity().setFoodLevel(20); }
    }

    @EventHandler public void onDrop(PlayerDropItemEvent e) { if (in(e.getPlayer()) && !builder(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onPickup(EntityPickupItemEvent e) { if (in(e.getEntity().getWorld()) && !(e.getEntity() instanceof Player p && builder(p))) e.setCancelled(true); }
    @EventHandler public void onSwap(PlayerSwapHandItemsEvent e) { if (in(e.getPlayer()) && !builder(e.getPlayer())) e.setCancelled(true); }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !in(p) || builder(p)) return;
        if (e.getInventory().getHolder() instanceof Menu) return;
        e.setCancelled(true);
    }

    @EventHandler public void onWeather(WeatherChangeEvent e) { if (e.toWeatherState() && plugin.modes.of(e.getWorld()) != Mode.ANARCHIA && plugin.modes.of(e.getWorld()) != Mode.INNY) e.setCancelled(true); }
    @EventHandler public void onSpawn(CreatureSpawnEvent e) {
        Mode m = plugin.modes.of(e.getLocation().getWorld());
        if ((m == Mode.LOBBY || m == Mode.FFA) && e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) e.setCancelled(true);
    }
    @EventHandler public void onDecay(LeavesDecayEvent e) { if (in(e.getBlock().getWorld())) e.setCancelled(true); }
    @EventHandler public void onFade(BlockFadeEvent e) { if (in(e.getBlock().getWorld())) e.setCancelled(true); }
    @EventHandler public void onSpread(BlockSpreadEvent e) { if (in(e.getBlock().getWorld())) e.setCancelled(true); }
}
