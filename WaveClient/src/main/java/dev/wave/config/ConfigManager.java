package dev.wave.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wave.WaveClient;
import dev.wave.gui.ClickGuiScreen;
import dev.wave.hud.HudElement;
import dev.wave.module.Module;
import dev.wave.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file = FabricLoader.getInstance().getConfigDir().resolve("waveclient.json");

    public void save() {
        JsonObject root = new JsonObject();
        JsonObject mods = new JsonObject();
        for (Module m : WaveClient.INSTANCE.modules.getModules()) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.isEnabled());
            o.addProperty("key", m.getKey());
            JsonObject settings = new JsonObject();
            for (Setting<?> s : m.getAllSettings()) settings.add(s.getName(), s.toJson());
            o.add("settings", settings);
            if (m instanceof HudElement h && h.isPlaced()) {
                o.addProperty("x", h.getX());
                o.addProperty("y", h.getY());
            }
            mods.add(m.getName(), o);
        }
        root.add("modules", mods);
        root.add("gui", ClickGuiScreen.saveState());
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
        } catch (Exception e) {
            WaveClient.LOGGER.error("Failed to save config", e);
        }
    }

    public void load() {
        if (!Files.exists(file)) return;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            if (root.has("modules")) {
                JsonObject mods = root.getAsJsonObject("modules");
                for (Module m : WaveClient.INSTANCE.modules.getModules()) {
                    if (!mods.has(m.getName())) continue;
                    JsonObject o = mods.getAsJsonObject(m.getName());
                    if (o.has("key")) m.setKey(o.get("key").getAsInt());
                    if (o.has("settings")) {
                        JsonObject settings = o.getAsJsonObject("settings");
                        for (Setting<?> s : m.getAllSettings()) {
                            JsonElement e = settings.get(s.getName());
                            if (e != null) {
                                try { s.fromJson(e); } catch (Exception ignored) {}
                            }
                        }
                    }
                    if (m instanceof HudElement h && o.has("x") && o.has("y")) h.setPosition(o.get("x").getAsFloat(), o.get("y").getAsFloat());
                    if (o.has("enabled")) m.setEnabledSilently(o.get("enabled").getAsBoolean());
                }
            }
            if (root.has("gui")) ClickGuiScreen.loadState(root.getAsJsonObject("gui"));
        } catch (Exception e) {
            WaveClient.LOGGER.error("Failed to load config", e);
        }
    }
}
