package pl.firen.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import pl.firen.client.FirenClient;
import pl.firen.client.gui.ClickGuiScreen;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.Setting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Zapisuje moduły, ustawienia, bindy i pozycję okna GUI do config/firenclient.json. */
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

        JsonObject gui = new JsonObject();
        gui.addProperty("x", ClickGuiScreen.winX);
        gui.addProperty("y", ClickGuiScreen.winY);
        gui.addProperty("category", ClickGuiScreen.category.name());
        root.add("gui", gui);

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
            if (root.has("gui")) {
                JsonObject gui = root.getAsJsonObject("gui");
                ClickGuiScreen.winX = gui.get("x").getAsFloat();
                ClickGuiScreen.winY = gui.get("y").getAsFloat();
                try {
                    ClickGuiScreen.category = Category.valueOf(gui.get("category").getAsString());
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (Exception e) {
            FirenClient.LOGGER.error("Nie udało się wczytać konfiguracji", e);
        }
    }
}
