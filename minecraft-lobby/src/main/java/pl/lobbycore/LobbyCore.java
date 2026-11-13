package pl.lobbycore;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LobbyCore extends JavaPlugin {

    NamespacedKey keyItem, keyNpc, keyHolo;
    World lobbyWorld, ffaWorld;
    Data data;
    Stats stats;
    AnarchiaStore anarchia;
    Modes modes;
    Lobby lobby;
    Ffa ffa;
    Boards boards;
    final Set<UUID> builders = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        keyItem = new NamespacedKey(this, "item");
        keyNpc = new NamespacedKey(this, "npc");
        keyHolo = new NamespacedKey(this, "holo");

        lobbyWorld = voidWorld(getConfig().getString("swiaty.lobby", "lobby"), false);
        ffaWorld = voidWorld(getConfig().getString("swiaty.ffa", "ffa"), true);

        data = new Data(this);
        stats = new Stats(this);
        anarchia = new AnarchiaStore(this);
        modes = new Modes(this);
        lobby = new Lobby(this);
        ffa = new Ffa(this);
        boards = new Boards(this);

        if (!data.built("lobby")) buildLobby();
        if (!data.built("ffa")) buildArena();

        var pm = getServer().getPluginManager();
        pm.registerEvents(lobby, this);
        pm.registerEvents(ffa, this);
        pm.registerEvents(new Flow(this), this);
        pm.registerEvents(new Menu.Listen(), this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, Modes.CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, Modes.CHANNEL, modes);

        Commands cmds = new Commands(this);
        for (String c : new String[]{"lobby", "anarchia", "ffa", "lobbyadmin"}) {
            PluginCommand pc = getCommand(c);
            pc.setExecutor(cmds);
            pc.setTabCompleter(cmds);
        }

        var sch = Bukkit.getScheduler();
        sch.runTaskTimer(this, boards::updateAll, 20L, 20L);
        sch.runTaskTimer(this, this::updateHolograms, 40L, 40L);
        sch.runTaskTimer(this, this::lookAtPlayers, 10L, 4L);
        sch.runTaskTimer(this, modes::requestCounts, 60L, 100L);
        sch.runTaskTimer(this, stats::save, 6000L, 6000L);

        // /reload – gracze już online
        for (Player p : Bukkit.getOnlinePlayers()) {
            boards.create(p);
            if (modes.of(p) == Mode.LOBBY) lobby.setup(p, false);
        }
        getLogger().info("LobbyCore gotowy. Lobby: " + lobbyWorld.getName() + ", FFA: " + ffaWorld.getName());
    }

    @Override
    public void onDisable() {
        if (stats != null) stats.save();
    }

    // ───────────── światy ─────────────

    private World voidWorld(String name, boolean ffa) {
        World w = Bukkit.getWorld(name);
        if (w == null) w = new WorldCreator(name).generator(new VoidGenerator()).generateStructures(false).createWorld();
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.DO_FIRE_TICK, false);
        w.setGameRule(GameRule.MOB_GRIEFING, false);
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        w.setGameRule(GameRule.DO_INSOMNIA, false);
        w.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        w.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        w.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, ffa);
        w.setGameRule(GameRule.KEEP_INVENTORY, false);
        w.setGameRule(GameRule.FALL_DAMAGE, ffa);
        w.setDifficulty(ffa ? Difficulty.NORMAL : Difficulty.PEACEFUL);
        w.setTime(getConfig().getLong("lobby.czas", 6000));
        w.setStorm(false);
        w.setThundering(false);
        return w;
    }

    void buildLobby() {
        removeTagged(lobbyWorld);
        Builder.LobbyLayout l = Builder.lobby(lobbyWorld);
        npc(l.npcAnarchia(), Mode.ANARCHIA, "&c&lANARCHIA", Villager.Profession.WEAPONSMITH, Villager.Type.SWAMP);
        npc(l.npcFfa(), Mode.FFA, "&b&lFFA", Villager.Profession.ARMORER, Villager.Type.SNOW);
        hologram(l.holoAnarchia(), "anarchia", 1.25f);
        hologram(l.holoFfa(), "ffa", 1.25f);
        hologram(l.holoWelcome(), "powitanie", 1.6f);
        data.setLobbySpawn(l.spawn());
        lobbyWorld.setSpawnLocation(l.spawn());
        data.setBuilt("lobby");
        updateHolograms();
    }

    void buildArena() {
        removeTagged(ffaWorld);
        Builder.ArenaLayout l = Builder.arena(ffaWorld);
        hologram(l.holo(), "arena", 1.5f);
        data.setFfaSpawns(l.spawns());
        ffaWorld.setSpawnLocation(l.spawns().get(0));
        data.setBuilt("ffa");
        updateHolograms();
    }

    private void removeTagged(World w) {
        for (Entity e : w.getEntities()) {
            var pdc = e.getPersistentDataContainer();
            if (pdc.has(keyNpc) || pdc.has(keyHolo)) e.remove();
        }
    }

    private void npc(Location at, Mode mode, String name, Villager.Profession prof, Villager.Type type) {
        at.getWorld().spawn(at, Villager.class, v -> {
            v.customName(Txt.c(name));
            v.setCustomNameVisible(true);
            v.setAI(false);
            v.setInvulnerable(true);
            v.setSilent(true);
            v.setPersistent(true);
            v.setRemoveWhenFarAway(false);
            v.setCollidable(false);
            v.setVillagerType(type);
            v.setProfession(prof);
            v.setVillagerLevel(5);
            v.getPersistentDataContainer().set(keyNpc, PersistentDataType.STRING, mode.name());
        });
    }

    private void hologram(Location at, String type, float scale) {
        at.getWorld().spawn(at, TextDisplay.class, t -> {
            t.setBillboard(Display.Billboard.CENTER);
            t.setAlignment(TextDisplay.TextAlignment.CENTER);
            t.setShadowed(true);
            t.setLineWidth(260);
            t.setBackgroundColor(Color.fromARGB(110, 10, 10, 18));
            t.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(scale, scale, scale), new AxisAngle4f()));
            t.setPersistent(true);
            t.getPersistentDataContainer().set(keyHolo, PersistentDataType.STRING, type);
        });
    }

    // ───────────── zadania cykliczne ─────────────

    private void updateHolograms() {
        for (World w : new World[]{lobbyWorld, ffaWorld}) for (TextDisplay t : w.getEntitiesByClass(TextDisplay.class)) {
            String type = t.getPersistentDataContainer().get(keyHolo, PersistentDataType.STRING);
            if (type == null) continue;
            t.text(Txt.c(ph(getConfig().getString("hologramy." + type, ""), null)));
        }
    }

    /** NPC obracają głowę do najbliższego gracza. */
    private void lookAtPlayers() {
        if (lobbyWorld.getPlayers().isEmpty()) return;
        for (Villager v : lobbyWorld.getEntitiesByClass(Villager.class)) {
            if (!v.getPersistentDataContainer().has(keyNpc)) continue;
            Player near = null;
            double best = 100;
            for (Player p : lobbyWorld.getPlayers()) {
                double d = p.getLocation().distanceSquared(v.getLocation());
                if (d < best) { best = d; near = p; }
            }
            if (near == null) continue;
            var dir = near.getEyeLocation().toVector().subtract(v.getEyeLocation().toVector());
            float yaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
            float pitch = (float) -Math.toDegrees(Math.atan2(dir.getY(), Math.hypot(dir.getX(), dir.getZ())));
            v.setRotation(yaw, pitch);
        }
    }

    // ───────────── placeholdery ─────────────

    String ph(String s, Player p) {
        if (s == null) return "";
        var c = getConfig();
        Map<String, String> m = new HashMap<>();
        m.put("nazwa", c.getString("nazwa-serwera", ""));
        m.put("ip", c.getString("ip", ""));
        m.put("discord", c.getString("discord", ""));
        m.put("online", String.valueOf(modes.onlineAll()));
        m.put("anarchia", String.valueOf(modes.online(Mode.ANARCHIA)));
        m.put("ffa", String.valueOf(modes.online(Mode.FFA)));
        m.put("lobby", String.valueOf(modes.online(Mode.LOBBY)));
        m.put("data", LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        if (p != null) {
            UUID id = p.getUniqueId();
            Location l = p.getLocation();
            m.put("gracz", p.getName());
            m.put("ping", String.valueOf(p.getPing()));
            m.put("kills", String.valueOf(stats.kills(id)));
            m.put("deaths", String.valueOf(stats.deaths(id)));
            m.put("kd", stats.kd(id));
            m.put("seria", String.valueOf(stats.streak(id)));
            m.put("rekord", String.valueOf(stats.best(id)));
            m.put("swiat", switch (p.getWorld().getEnvironment()) {
                case NETHER -> "&cNether";
                case THE_END -> "&dEnd";
                default -> "&aOverworld";
            });
            m.put("x", String.valueOf(l.getBlockX()));
            m.put("y", String.valueOf(l.getBlockY()));
            m.put("z", String.valueOf(l.getBlockZ()));
        }
        // {nazwa} może zawierać inne placeholdery – dwa przebiegi
        return Txt.ph(Txt.ph(s, m), m);
    }
}
