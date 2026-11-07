package dev.mustic.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mustic.client.MusticClient;
import dev.mustic.client.gui.ClickGui;
import dev.mustic.client.module.Module;
import dev.mustic.client.module.ModuleManager;
import dev.mustic.client.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("musticclient.json");

    /** Pozycje paneli wczytane z pliku (uzywane przy tworzeniu ClickGui). */
    public static JsonObject panels = new JsonObject();

    private ConfigManager() {}

    public static void load() {
        if (!Files.exists(FILE)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("modules")) {
                JsonObject modules = root.getAsJsonObject("modules");
                for (Module m : ModuleManager.getModules()) {
                    if (!modules.has(m.getName())) continue;
                    JsonObject mj = modules.getAsJsonObject(m.getName());
                    if (mj.has("key")) m.setKey(mj.get("key").getAsInt());
                    if (mj.has("settings")) {
                        JsonObject sj = mj.getAsJsonObject("settings");
                        for (Setting<?> s : m.getSettings())
                            if (sj.has(s.getName())) s.fromJson(sj.get(s.getName()));
                    }
                    if (mj.has("enabled")) m.setEnabled(mj.get("enabled").getAsBoolean());
                }
            }
            if (root.has("panels")) panels = root.getAsJsonObject("panels");
        } catch (Exception e) {
            MusticClient.LOGGER.error("Nie udalo sie wczytac configu", e);
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        JsonObject modules = new JsonObject();
        for (Module m : ModuleManager.getModules()) {
            JsonObject mj = new JsonObject();
            mj.addProperty("enabled", m.isEnabled());
            mj.addProperty("key", m.getKey());
            JsonObject sj = new JsonObject();
            for (Setting<?> s : m.getSettings()) sj.add(s.getName(), s.toJson());
            mj.add("settings", sj);
            modules.add(m.getName(), mj);
        }
        root.add("modules", modules);
        if (ClickGui.exists()) panels = ClickGui.get().savePanels();
        root.add("panels", panels);
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception e) {
            MusticClient.LOGGER.error("Nie udalo sie zapisac configu", e);
        }
    }
}
