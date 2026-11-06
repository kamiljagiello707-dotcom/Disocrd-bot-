package pl.firen.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import pl.firen.client.FirenClient;
import pl.firen.client.gui.ClickGuiScreen;
import pl.firen.client.module.Module;
import pl.firen.client.setting.Setting;
import pl.firen.client.util.Friends;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Profile konfiguracji: config/firenclient/configs/&lt;nazwa&gt;.json.
 * client.json trzyma aktywny profil, znajomych i stan GUI.
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path root = FabricLoader.getInstance().getConfigDir().resolve("firenclient");
    private final Path configs = root.resolve("configs");
    private final Path clientFile = root.resolve("client.json");
    private String active = "default";

    public String active() { return active; }
    public Path folder() { return configs; }

    public static String sanitize(String name) {
        String n = name.trim().replaceAll("[^A-Za-z0-9_\\-. ]", "").replace(' ', '_');
        return n.isEmpty() ? "config" : n.length() > 24 ? n.substring(0, 24) : n;
    }

    public List<String> list() {
        List<String> out = new ArrayList<>();
        try {
            Files.createDirectories(configs);
            try (Stream<Path> s = Files.list(configs)) {
                s.filter(p -> p.toString().endsWith(".json")).sorted()
                        .forEach(p -> out.add(p.getFileName().toString().replace(".json", "")));
            }
        } catch (IOException e) {
            FirenClient.LOGGER.error("Nie udało się odczytać listy configów", e);
        }
        if (!out.contains(active)) out.add(0, active);
        return out;
    }

    // ------------------------------------------------------------------ zapis / odczyt modułów

    public JsonObject modulesToJson() {
        JsonObject modules = new JsonObject();
        for (Module m : FirenClient.modules().all()) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.isEnabled());
            o.addProperty("key", m.getKey());
            o.addProperty("favorite", m.favorite);
            JsonObject settings = new JsonObject();
            for (Setting<?> s : m.getSettings()) settings.add(s.getName(), s.toJson());
            o.add("settings", settings);
            modules.add(m.getName(), o);
        }
        JsonObject root = new JsonObject();
        root.addProperty("client", "FirenClient");
        root.addProperty("version", FirenClient.VERSION);
        root.add("modules", modules);
        return root;
    }

    public void modulesFromJson(JsonObject root) {
        if (!root.has("modules")) return;
        JsonObject modules = root.getAsJsonObject("modules");
        for (Module m : FirenClient.modules().all()) {
            if (!modules.has(m.getName())) continue;
            JsonObject o = modules.getAsJsonObject(m.getName());
            if (o.has("key")) m.setKey(o.get("key").getAsInt());
            if (o.has("favorite")) m.favorite = o.get("favorite").getAsBoolean();
            if (o.has("settings")) {
                JsonObject settings = o.getAsJsonObject("settings");
                for (Setting<?> s : m.getSettings()) {
                    JsonElement e = settings.get(s.getName());
                    if (e != null) s.fromJson(e);
                }
            }
            if (o.has("enabled") && MinecraftClient.getInstance().player != null) m.setEnabled(o.get("enabled").getAsBoolean());
            else if (o.has("enabled")) m.setEnabledSilently(o.get("enabled").getAsBoolean());
            m.toggleAnim = m.isEnabled() ? 1f : 0f;
        }
    }

    public boolean saveAs(String name) {
        name = sanitize(name);
        try {
            Files.createDirectories(configs);
            Files.writeString(configs.resolve(name + ".json"), GSON.toJson(modulesToJson()), StandardCharsets.UTF_8);
            active = name;
            saveClient();
            return true;
        } catch (IOException e) {
            FirenClient.LOGGER.error("Nie udało się zapisać configu", e);
            return false;
        }
    }

    public boolean load(String name) {
        Path file = configs.resolve(sanitize(name) + ".json");
        if (!Files.exists(file)) return false;
        try {
            modulesFromJson(JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject());
            active = sanitize(name);
            saveClient();
            return true;
        } catch (Exception e) {
            FirenClient.LOGGER.error("Nie udało się wczytać configu", e);
            return false;
        }
    }

    public boolean delete(String name) {
        try {
            boolean ok = Files.deleteIfExists(configs.resolve(sanitize(name) + ".json"));
            if (sanitize(name).equals(active)) active = "default";
            saveClient();
            return ok;
        } catch (IOException e) {
            return false;
        }
    }

    /** Eksport aktualnych ustawień do schowka (JSON jednej linii). */
    public void exportToClipboard() {
        MinecraftClient.getInstance().keyboard.setClipboard(new Gson().toJson(modulesToJson()));
    }

    /** Import ze schowka – zapisuje jako nowy profil i go wczytuje. */
    public boolean importFromClipboard(String name) {
        try {
            JsonObject root = JsonParser.parseString(MinecraftClient.getInstance().keyboard.getClipboard()).getAsJsonObject();
            if (!root.has("modules")) return false;
            modulesFromJson(root);
            return saveAs(name);
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ plik klienta + autozapis

    public void saveClient() {
        JsonObject o = new JsonObject();
        o.addProperty("active", active);
        JsonArray friends = new JsonArray();
        Friends.all().forEach(friends::add);
        o.add("friends", friends);
        JsonObject gui = new JsonObject();
        gui.addProperty("x", ClickGuiScreen.winX);
        gui.addProperty("y", ClickGuiScreen.winY);
        gui.addProperty("tab", ClickGuiScreen.tab);
        o.add("gui", gui);
        try {
            Files.createDirectories(root);
            Files.writeString(clientFile, GSON.toJson(o), StandardCharsets.UTF_8);
        } catch (IOException e) {
            FirenClient.LOGGER.error("Nie udało się zapisać client.json", e);
        }
    }

    /** Zapis bieżącego profilu (przy zamknięciu GUI i wyjściu z gry). */
    public void save() {
        saveAs(active);
    }

    public void load() {
        try {
            if (Files.exists(clientFile)) {
                JsonObject o = JsonParser.parseString(Files.readString(clientFile, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("active")) active = sanitize(o.get("active").getAsString());
                if (o.has("friends")) {
                    List<String> f = new ArrayList<>();
                    o.getAsJsonArray("friends").forEach(e -> f.add(e.getAsString()));
                    Friends.set(f);
                }
                if (o.has("gui")) {
                    JsonObject gui = o.getAsJsonObject("gui");
                    ClickGuiScreen.winX = gui.get("x").getAsFloat();
                    ClickGuiScreen.winY = gui.get("y").getAsFloat();
                    if (gui.has("tab")) ClickGuiScreen.tab = gui.get("tab").getAsInt();
                }
            }
            // migracja ze starego pliku V1
            Path legacy = FabricLoader.getInstance().getConfigDir().resolve("firenclient.json");
            if (!Files.exists(configs.resolve(active + ".json")) && Files.exists(legacy)) {
                modulesFromJson(JsonParser.parseString(Files.readString(legacy, StandardCharsets.UTF_8)).getAsJsonObject());
                saveAs(active);
                return;
            }
            load(active);
        } catch (Exception e) {
            FirenClient.LOGGER.error("Nie udało się wczytać konfiguracji", e);
        }
    }
}
