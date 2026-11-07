package dev.testclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.testclient.gui.ClickGuiScreen;
import dev.testclient.module.Module;
import dev.testclient.setting.BooleanSetting;
import dev.testclient.setting.NumberSetting;
import dev.testclient.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Zapis/odczyt stanu modułów i pozycji paneli do config/testclient.json */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("testclient.json");

    private Config() {}

    public static void save() {
        JsonObject root = new JsonObject();
        JsonObject mods = new JsonObject();
        for (Module m : TestClient.modules().getModules()) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.isEnabled());
            o.addProperty("key", m.getKey());
            JsonObject s = new JsonObject();
            for (Setting<?> setting : m.getSettings()) {
                if (setting instanceof BooleanSetting b) s.addProperty(b.getName(), b.get());
                else if (setting instanceof NumberSetting n) s.addProperty(n.getName(), n.get());
            }
            o.add("settings", s);
            mods.add(m.getName(), o);
        }
        root.add("modules", mods);
        root.add("panels", ClickGuiScreen.savePanels());
        try {
            Files.writeString(FILE, GSON.toJson(root));
        } catch (IOException e) {
            TestClient.LOGGER.error("Nie udało się zapisać configu", e);
        }
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try {
            JsonObject root = GSON.fromJson(Files.readString(FILE), JsonObject.class);
            if (root == null) return;
            if (root.has("modules")) {
                JsonObject mods = root.getAsJsonObject("modules");
                for (Module m : TestClient.modules().getModules()) {
                    if (!mods.has(m.getName())) continue;
                    JsonObject o = mods.getAsJsonObject(m.getName());
                    if (o.has("key")) m.setKey(o.get("key").getAsInt());
                    if (o.has("settings")) {
                        JsonObject s = o.getAsJsonObject("settings");
                        for (Setting<?> setting : m.getSettings()) {
                            JsonElement e = s.get(setting.getName());
                            if (e == null) continue;
                            if (setting instanceof BooleanSetting b) b.set(e.getAsBoolean());
                            else if (setting instanceof NumberSetting n) n.set(e.getAsDouble());
                        }
                    }
                    // stan włączenia ustawiamy dopiero po wejściu do świata (patrz TestClient)
                    if (o.has("enabled") && o.get("enabled").getAsBoolean()) TestClient.pendingEnable.add(m);
                }
            }
            if (root.has("panels")) ClickGuiScreen.loadPanels(root.getAsJsonObject("panels"));
        } catch (Exception e) {
            TestClient.LOGGER.error("Nie udało się wczytać configu", e);
        }
    }
}
