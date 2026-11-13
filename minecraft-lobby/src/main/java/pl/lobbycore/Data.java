package pl.lobbycore;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** data.yml – spawny i informacja, czy lobby/arena są zbudowane. */
final class Data {
    private final LobbyCore plugin;
    private final File file;
    private final YamlConfiguration yml;

    Data(LobbyCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        this.yml = YamlConfiguration.loadConfiguration(file);
    }

    boolean built(String what) { return yml.getBoolean("zbudowane." + what); }
    void setBuilt(String what) { yml.set("zbudowane." + what, true); save(); }

    Location lobbySpawn() {
        Location l = read(yml.getConfigurationSection("spawn-lobby"));
        return l != null ? l : new Location(plugin.lobbyWorld, 0.5, 101, 7.5, 180, 0);
    }
    void setLobbySpawn(Location l) { write("spawn-lobby", l); save(); }

    List<Location> ffaSpawns() {
        List<Location> out = new ArrayList<>();
        ConfigurationSection s = yml.getConfigurationSection("spawny-ffa");
        if (s != null) for (String k : s.getKeys(false)) {
            Location l = read(s.getConfigurationSection(k));
            if (l != null) out.add(l);
        }
        return out;
    }
    void setFfaSpawns(List<Location> list) {
        yml.set("spawny-ffa", null);
        for (int i = 0; i < list.size(); i++) write("spawny-ffa." + i, list.get(i));
        save();
    }

    static Location read(ConfigurationSection s) {
        if (s == null) return null;
        World w = Bukkit.getWorld(s.getString("swiat", ""));
        if (w == null) return null;
        return new Location(w, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                (float) s.getDouble("yaw"), (float) s.getDouble("pitch"));
    }

    static void write(YamlConfiguration yml, String path, Location l) {
        yml.set(path + ".swiat", l.getWorld().getName());
        yml.set(path + ".x", l.getX());
        yml.set(path + ".y", l.getY());
        yml.set(path + ".z", l.getZ());
        yml.set(path + ".yaw", l.getYaw());
        yml.set(path + ".pitch", l.getPitch());
    }

    private void write(String path, Location l) { write(yml, path, l); }

    void save() {
        try { yml.save(file); } catch (IOException e) { plugin.getLogger().severe("Zapis data.yml: " + e.getMessage()); }
    }
}
