package pl.lobbycore;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Statystyki FFA (stats.yml) + bieżąca seria zabójstw w pamięci. */
final class Stats {
    private final LobbyCore plugin;
    private final File file;
    private final YamlConfiguration yml;
    private final Map<UUID, Integer> streak = new HashMap<>();
    private boolean dirty;

    Stats(LobbyCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        this.yml = YamlConfiguration.loadConfiguration(file);
    }

    int kills(UUID id) { return yml.getInt(id + ".zabojstwa"); }
    int deaths(UUID id) { return yml.getInt(id + ".smierci"); }
    int best(UUID id) { return yml.getInt(id + ".rekord"); }
    int streak(UUID id) { return streak.getOrDefault(id, 0); }

    String kd(UUID id) {
        int d = deaths(id);
        double kd = d == 0 ? kills(id) : (double) kills(id) / d;
        return String.format(java.util.Locale.ROOT, "%.2f", kd);
    }

    /** @return nowa seria zabójcy */
    int addKill(UUID id) {
        yml.set(id + ".zabojstwa", kills(id) + 1);
        int s = streak(id) + 1;
        streak.put(id, s);
        if (s > best(id)) yml.set(id + ".rekord", s);
        dirty = true;
        return s;
    }

    void addDeath(UUID id) {
        yml.set(id + ".smierci", deaths(id) + 1);
        streak.remove(id);
        dirty = true;
    }

    void resetStreak(UUID id) { streak.remove(id); }

    void save() {
        if (!dirty) return;
        try { yml.save(file); dirty = false; } catch (IOException e) { plugin.getLogger().severe("Zapis stats.yml: " + e.getMessage()); }
    }
}
