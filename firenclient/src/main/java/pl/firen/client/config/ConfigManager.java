package pl.firen.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import pl.firen.client.FirenClient;
import pl.firen.client.gui.ClickGuiScreen;
import pl.firen.client.gui.Panel;
import pl.firen.client.module.Module;
import pl.firen.client.setting.Setting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Zapisuje moduły, ustawienia, bindy i pozycje paneli do config/firenclient.json. */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file = FabricLoader.getInstance().getConfigDir().resolve("firenclient.json");

    public void save() {
        JsonObject root = new JsonObject();
        JsonObject modules = new JsonObject();
        for (Module m : FirenClient.modules().all()) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.isEnabled());
            o.addProperty("key", m.getKey());
            JsonObject settings = new JsonObject();
            for (Setting<?> s : m.getSettings()) settings.add(s.getName(), s.toJson());
            o.add("settings", settings);
            modules.add(m.getName(), o);
        }
        root.add("modules", modules);

        JsonObject panels = new JsonObject();
        for (Panel p : ClickGuiScreen.panels()) {
            JsonObject o = new JsonObject();
            o.addProperty("x", p.x);
            o.addProperty("y", p.y);
            o.addProperty("collapsed", p.collapsed);
            panels.add(p.category.name(), o);
        }
        root.add("panels", panels);

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            FirenClient.LOGGER.error("Nie udało się zapisać konfiguracji", e);
        }
    }

    public void load() {
        if (!Files.exists(file)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("modules")) {
                JsonObject modules = root.getAsJsonObject("modules");
                for (Module m : FirenClient.modules().all()) {
                    if (!modules.has(m.getName())) continue;
                    JsonObject o = modules.getAsJsonObject(m.getName());
                    if (o.has("key")) m.setKey(o.get("key").getAsInt());
                    if (o.has("settings")) {
                        JsonObject settings = o.getAsJsonObject("settings");
                        for (Setting<?> s : m.getSettings()) {
                            JsonElement e = settings.get(s.getName());
                            if (e != null) s.fromJson(e);
                        }
                    }
                    if (o.has("enabled")) m.setEnabled(o.get("enabled").getAsBoolean());
                    m.toggleAnim = m.isEnabled() ? 1f : 0f;
                }
            }
            if (root.has("panels")) {
                JsonObject panels = root.getAsJsonObject("panels");
                for (Panel p : ClickGuiScreen.panels()) {
                    if (!panels.has(p.category.name())) continue;
                    JsonObject o = panels.getAsJsonObject(p.category.name());
                    p.x = o.get("x").getAsInt();
                    p.y = o.get("y").getAsInt();
                    p.collapsed = o.has("collapsed") && o.get("collapsed").getAsBoolean();
                    p.positioned = true;
                }
            }
        } catch (Exception e) {
            FirenClient.LOGGER.error("Nie udało się wczytać konfiguracji", e);
        }
    }
}
