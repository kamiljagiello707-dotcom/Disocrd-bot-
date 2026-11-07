package pl.mysticclient.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import pl.mysticclient.MysticClient;
import pl.mysticclient.module.Module;
import pl.mysticclient.module.ModuleManager;
import pl.mysticclient.module.misc.ClientSettings;
import pl.mysticclient.setting.BoolSetting;
import pl.mysticclient.setting.ModeSetting;
import pl.mysticclient.setting.NumberSetting;
import pl.mysticclient.setting.Setting;
import pl.mysticclient.util.FriendManager;
import pl.mysticclient.util.Theme;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("mysticclient");
    private static final Path CONFIGS = DIR.resolve("configs");
    private static final Path CLIENT = DIR.resolve("client.json");

    private static String current = "default";

    private ConfigManager() {}

    public static String getCurrent() { return current; }
    public static Path getDir() { return CONFIGS; }

    public static void load() {
        try {
            Files.createDirectories(CONFIGS);
            if (Files.exists(CLIENT)) {
                JsonObject o = JsonParser.parseString(Files.readString(CLIENT, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("current")) current = sanitize(o.get("current").getAsString());
                if (o.has("light")) Theme.light = o.get("light").getAsBoolean();
                if (o.has("friends")) {
                    List<String> f = new ArrayList<>();
                    o.getAsJsonArray("friends").forEach(e -> f.add(e.getAsString()));
                    FriendManager.set(f);
                }
            }
        } catch (Exception e) {
            MysticClient.LOGGER.error("Nie udało się wczytać client.json", e);
        }
        loadConfig(current);
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIGS);
            JsonObject o = new JsonObject();
            o.addProperty("current", current);
            o.addProperty("light", Theme.light);
            JsonArray f = new JsonArray();
            FriendManager.all().forEach(f::add);
            o.add("friends", f);
            Files.writeString(CLIENT, GSON.toJson(o), StandardCharsets.UTF_8);
            saveConfig(current);
        } catch (Exception e) {
            MysticClient.LOGGER.error("Nie udało się zapisać configu", e);
        }
    }

    public static void saveConfig(String name) throws IOException {
        name = sanitize(name);
        JsonObject root = new JsonObject();
        for (Module m : ModuleManager.getModules()) {
            JsonObject mo = new JsonObject();
            mo.addProperty("enabled", m.isEnabled());
            mo.addProperty("key", m.getKey());
            mo.addProperty("favorite", m.favorite);
            JsonObject so = new JsonObject();
            for (Setting<?> s : m.getSettings()) {
                if (s instanceof BoolSetting b) so.addProperty(s.getName(), b.get());
                else if (s instanceof NumberSetting n) so.addProperty(s.getName(), n.get());
                else if (s instanceof ModeSetting md) so.addProperty(s.getName(), md.get());
            }
            mo.add("settings", so);
            root.add(m.getName(), mo);
        }
        Files.createDirectories(CONFIGS);
        Files.writeString(CONFIGS.resolve(name + ".json"), GSON.toJson(root), StandardCharsets.UTF_8);
    }

    public static void loadConfig(String name) {
        name = sanitize(name);
        Path file = CONFIGS.resolve(name + ".json");
        current = name;
        if (!Files.exists(file)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Module m : ModuleManager.getModules()) {
                if (!root.has(m.getName())) continue;
                JsonObject mo = root.getAsJsonObject(m.getName());
                if (mo.has("key")) m.setKey(mo.get("key").getAsInt());
                if (mo.has("favorite")) m.favorite = mo.get("favorite").getAsBoolean();
                if (mo.has("settings")) {
                    JsonObject so = mo.getAsJsonObject("settings");
                    for (Setting<?> s : m.getSettings()) {
                        if (!so.has(s.getName())) continue;
                        JsonElement v = so.get(s.getName());
                        try {
                            if (s instanceof BoolSetting b) { b.set(v.getAsBoolean()); b.anim = b.get() ? 1 : 0; }
                            else if (s instanceof NumberSetting n) n.set(v.getAsDouble());
                            else if (s instanceof ModeSetting md) md.set(v.getAsString());
                        } catch (Exception ignored) {}
                    }
                }
                if (mo.has("enabled") && !(m instanceof ClientSettings) && !(m instanceof pl.mysticclient.module.misc.ClickGUI)) {
                    m.setEnabled(mo.get("enabled").getAsBoolean(), false);
                    m.toggleAnim = m.isEnabled() ? 1 : 0;
                }
            }
        } catch (Exception e) {
            MysticClient.LOGGER.error("Nie udało się wczytać configu " + name, e);
        }
    }

    public static List<String> list() {
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(CONFIGS)) {
            s.filter(p -> p.toString().endsWith(".json")).sorted()
                    .forEach(p -> out.add(p.getFileName().toString().replace(".json", "")));
        } catch (IOException ignored) {}
        if (!out.contains(current)) out.add(0, current);
        return out;
    }

    public static void delete(String name) {
        try { Files.deleteIfExists(CONFIGS.resolve(sanitize(name) + ".json")); } catch (IOException ignored) {}
    }

    public static String sanitize(String name) {
        String s = name.replaceAll("[^a-zA-Z0-9_\\-]", "").trim();
        return s.isEmpty() ? "default" : s.substring(0, Math.min(24, s.length()));
    }
}
