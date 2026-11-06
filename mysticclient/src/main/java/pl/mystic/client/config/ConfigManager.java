package pl.mystic.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import pl.mystic.client.MysticClient;
import pl.mystic.client.module.Module;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Zapis włączonych modułów i klawiszy do config/mysticclient.json. */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("mysticclient.json");

    /** Moduły włączone w configu, aktywowane po wejściu do świata. */
    public static final Set<Module> pending = new HashSet<>();

    private ConfigManager() {}

    public static void load() {
        if (!Files.exists(FILE)) return;
        try {
            JsonObject root = GSON.fromJson(Files.readString(FILE), JsonObject.class);
            for (Module m : MysticClient.modules.all()) {
                if (!root.has(m.getName())) continue;
                JsonObject o = root.getAsJsonObject(m.getName());
                if (o.has("key")) m.setKey(o.get("key").getAsInt());
                if (o.has("enabled") && o.get("enabled").getAsBoolean()) pending.add(m);
            }
        } catch (Exception e) {
            MysticClient.LOG.error("Nie udało się wczytać configu", e);
        }
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            for (Module m : MysticClient.modules.all()) {
                JsonObject o = new JsonObject();
                o.addProperty("enabled", m.isEnabled() || pending.contains(m));
                o.addProperty("key", m.getKey());
                root.add(m.getName(), o);
            }
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(root));
        } catch (Exception e) {
            MysticClient.LOG.error("Nie udało się zapisać configu", e);
        }
    }
}
