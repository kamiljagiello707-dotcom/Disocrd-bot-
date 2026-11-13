package pl.npcmenu;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/** Prosty portfel graczy zapisywany w plugins/NpcMenu/portfel.yml */
public final class Wallet {

    private final NpcMenuPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    public Wallet(NpcMenuPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "portfel.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public double get(UUID id) {
        String path = id.toString();
        if (!data.contains(path)) {
            data.set(path, plugin.getConfig().getDouble("saldo-startowe", 0));
            save();
        }
        return data.getDouble(path);
    }

    public void set(UUID id, double amount) {
        data.set(id.toString(), Math.max(0, amount));
        save();
    }

    public void add(UUID id, double amount) {
        set(id, get(id) + amount);
    }

    public void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie udało się zapisać portfel.yml: " + e.getMessage());
        }
    }

    public String format(double amount) {
        String s = amount == Math.floor(amount) ? String.valueOf((long) amount) : String.format("%.2f", amount);
        return s;
    }
}
