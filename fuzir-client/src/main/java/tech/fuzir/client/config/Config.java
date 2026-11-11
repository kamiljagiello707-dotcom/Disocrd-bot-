package tech.fuzir.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import tech.fuzir.client.FuzirClient;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.module.ModuleManager;
import tech.fuzir.client.setting.Setting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Zapis włączonych modułów, klawiszy i ustawień do config/fuzir-client.json. */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("fuzir-client.json");

    private Config() {
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject mods = root.getAsJsonObject("modules");
            if (mods == null) return;
            for (Module m : ModuleManager.all()) {
                JsonObject o = mods.getAsJsonObject(m.name);
                if (o == null) continue;
                if (o.has("enabled") && m.hasToggle()) m.setEnabledQuiet(o.get("enabled").getAsBoolean());
                if (o.has("key")) m.bind.load(o.get("key"));
                JsonObject s = o.getAsJsonObject("settings");
                if (s == null) continue;
                for (Setting<?> st : m.settings) {
                    JsonElement e = s.get(st.name);
                    if (e != null) st.load(e);
                }
            }
        } catch (Exception e) {
            FuzirClient.LOG.warn("Nie udało się wczytać configu, zostają ustawienia domyślne", e);
        }
    }

    public static void save() {
        JsonObject mods = new JsonObject();
        for (Module m : ModuleManager.all()) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.isEnabled());
            o.add("key", m.bind.save());
            JsonObject s = new JsonObject();
            for (Setting<?> st : m.settings) s.add(st.name, st.save());
            o.add("settings", s);
            mods.add(m.name, o);
        }
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.add("modules", mods);
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            FuzirClient.LOG.warn("Nie udało się zapisać configu", e);
        }
    }
}
