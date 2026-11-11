package tech.fuzir.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import tech.fuzir.client.FuzirClient;
import tech.fuzir.client.module.HudModule;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.module.ModuleManager;
import tech.fuzir.client.module.impl.ClickGui;
import tech.fuzir.client.setting.Setting;
import tech.fuzir.client.util.Theme;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Bieżące ustawienia w config/fuzir-client.json; zapisane profile obsługuje {@link Profiles}. */
public final class Config {
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static final Path DIR = FabricLoader.getInstance().getConfigDir();
    private static final Path FILE = DIR.resolve("fuzir-client.json");

    private Config() {
    }

    public static JsonObject toJson() {
        JsonObject mods = new JsonObject();
        for (Module m : ModuleManager.all()) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.isEnabled());
            o.add("key", m.bind.save());
            o.add("keyMode", m.bindMode.save());
            if (m instanceof HudModule h) {
                o.addProperty("x", h.fx);
                o.addProperty("y", h.fy);
            }
            JsonObject s = new JsonObject();
            for (Setting<?> st : m.settings) s.add(st.name, st.save());
            o.add("settings", s);
            mods.add(m.name, o);
        }
        JsonObject root = new JsonObject();
        root.addProperty("version", 3);
        root.add("modules", mods);
        return root;
    }

    /** Wczytuje ustawienia; {@code quiet} = bez powiadomień o przełączeniu modułów. */
    public static void fromJson(JsonObject root, boolean quiet) {
        JsonObject mods = root.getAsJsonObject("modules");
        if (mods == null) return;
        for (Module m : ModuleManager.all()) {
            JsonObject o = mods.getAsJsonObject(m.name);
            if (o == null) continue;
            if (o.has("enabled") && m.hasToggle()) {
                if (quiet) m.setEnabledQuiet(o.get("enabled").getAsBoolean());
                else m.setEnabled(o.get("enabled").getAsBoolean());
            }
            if (o.has("key")) m.bind.load(o.get("key"));
            if (o.has("keyMode")) m.bindMode.load(o.get("keyMode"));
            if (m instanceof HudModule h && o.has("x") && o.has("y")) {
                h.fx = Math.max(0, Math.min(1, o.get("x").getAsFloat()));
                h.fy = Math.max(0, Math.min(1, o.get("y").getAsFloat()));
            }
            JsonObject s = o.getAsJsonObject("settings");
            if (s == null) continue;
            for (Setting<?> st : m.settings) {
                JsonElement e = s.get(st.name);
                if (e != null) st.load(e);
            }
        }
        String theme = ModuleManager.get(ClickGui.class).theme.get();
        if (quiet) Theme.apply(theme);
        else Theme.select(theme);
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try {
            fromJson(JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject(), true);
        } catch (Exception e) {
            FuzirClient.LOG.warn("Nie udało się wczytać configu, zostają ustawienia domyślne", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(DIR);
            Files.writeString(FILE, GSON.toJson(toJson()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            FuzirClient.LOG.warn("Nie udało się zapisać configu", e);
        }
    }
}
