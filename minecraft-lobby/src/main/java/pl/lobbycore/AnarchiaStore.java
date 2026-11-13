package pl.lobbycore;

import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Anarchia ma własny ekwipunek: przy wyjściu do lobby/FFA zapisujemy wszystko
 * (przedmioty, XP, życie, głód, pozycję), przy powrocie przywracamy.
 */
final class AnarchiaStore {
    private final LobbyCore plugin;
    private final File dir;

    AnarchiaStore(LobbyCore plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "anarchia");
        dir.mkdirs();
    }

    private File file(UUID id) { return new File(dir, id + ".yml"); }

    @SuppressWarnings("deprecation")
    void save(Player p, Location from) {
        YamlConfiguration y = new YamlConfiguration();
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) if (contents[i] != null && !contents[i].getType().isAir()) y.set("eq." + i, contents[i]);
        y.set("poziom", p.getLevel());
        y.set("exp", p.getExp());
        y.set("zycie", Math.min(p.getHealth(), p.getMaxHealth()));
        y.set("glod", p.getFoodLevel());
        y.set("nasycenie", p.getSaturation());
        Data.write(y, "pozycja", from);
        try { y.save(file(p.getUniqueId())); } catch (IOException e) { plugin.getLogger().severe("Zapis anarchii " + p.getName() + ": " + e.getMessage()); }
    }

    /** Przywraca ekwipunek po wejściu na anarchię. */
    @SuppressWarnings("deprecation")
    void apply(Player p) {
        Lobby.reset(p);
        p.setGameMode(GameMode.SURVIVAL);
        File f = file(p.getUniqueId());
        if (f.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            ConfigurationSection eq = y.getConfigurationSection("eq");
            if (eq != null) {
                ItemStack[] contents = new ItemStack[p.getInventory().getSize()];
                for (String k : eq.getKeys(false)) {
                    int slot = Integer.parseInt(k);
                    if (slot < contents.length) contents[slot] = eq.getItemStack(k);
                }
                p.getInventory().setContents(contents);
            }
            p.setLevel(y.getInt("poziom"));
            p.setExp((float) y.getDouble("exp"));
            p.setHealth(Math.max(1, Math.min(p.getMaxHealth(), y.getDouble("zycie", 20))));
            p.setFoodLevel(y.getInt("glod", 20));
            p.setSaturation((float) y.getDouble("nasycenie", 5));
        }
        p.showTitle(Title.title(Txt.c("&c&lANARCHIA"), Txt.c("&7Brak zasad. Powodzenia!"),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1800), Duration.ofMillis(500))));
    }

    /** Teleport na anarchię: ostatnia pozycja albo losowe miejsce. */
    void enter(Player p) {
        File f = file(p.getUniqueId());
        if (f.exists()) {
            Location l = Data.read(YamlConfiguration.loadConfiguration(f).getConfigurationSection("pozycja"));
            if (l != null) { p.teleportAsync(l); return; }
        }
        p.sendActionBar(Txt.c("&7Szukam bezpiecznego miejsca..."));
        randomTeleport(p, 0);
    }

    private void randomTeleport(Player p, int attempt) {
        World w = plugin.modes.anarchiaWorld();
        int r = plugin.getConfig().getInt("anarchia.losowy-teleport-promien", 3000);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int x = rnd.nextInt(-r, r + 1), z = rnd.nextInt(-r, r + 1);
        w.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            if (!p.isOnline()) return;
            Block b = w.getHighestBlockAt(x, z);
            Material t = b.getType();
            boolean bad = b.isLiquid() || t == Material.LAVA || t == Material.MAGMA_BLOCK || t == Material.CACTUS
                    || t.name().contains("LEAVES") || t == Material.POWDER_SNOW;
            if (bad && attempt < 12) { randomTeleport(p, attempt + 1); return; }
            Location l = b.getLocation().add(0.5, 1, 0.5);
            l.setYaw(rnd.nextFloat() * 360f);
            p.teleportAsync(l);
        });
    }

    boolean worldsLoaded() {
        for (String n : plugin.getConfig().getStringList("swiaty.anarchia")) if (Bukkit.getWorld(n) != null) return true;
        return false;
    }
}
