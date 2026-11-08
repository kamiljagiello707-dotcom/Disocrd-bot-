package dev.nova.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.nova.client.NovaClient;
import dev.nova.client.gui.PanelState;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.setting.*;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Saves module states, settings and panel positions to config/nova.json. */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final NovaClient client;
    private final Path file = FabricLoader.getInstance().getConfigDir().resolve("nova.json");

    public ConfigManager(NovaClient client) { this.client = client; }

    public void save() {
        JsonObject root = new JsonObject();
        JsonObject mods = new JsonObject();
        for (Module m : client.modules.all()) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.isEnabled());
            JsonObject s = new JsonObject();
            for (Setting<?> st : m.getSettings()) {
                Object v = st.get();
                if (v instanceof Boolean b) s.addProperty(st.getName(), b);
                else if (v instanceof Number n) s.addProperty(st.getName(), n);
                else if (v instanceof String str) s.addProperty(st.getName(), str);
            }
            o.add("settings", s);
            mods.add(m.getName(), o);
        }
        root.add("modules", mods);

        JsonObject panels = new JsonObject();
        for (Category c : Category.values()) {
            PanelState ps = client.panelState(c);
            JsonObject o = new JsonObject();
            o.addProperty("x", ps.x);
            o.addProperty("y", ps.y);
            o.addProperty("open", ps.open);
            panels.add(c.name(), o);
        }
        root.add("panels", panels);

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root));
        } catch (Exception e) {
            System.err.println("[Nova] Failed to save config: " + e);
        }
    }

    public void load() {
        if (!Files.exists(file)) return;
        try {
            JsonObject root = GSON.fromJson(Files.readString(file), JsonObject.class);
            JsonObject mods = root.getAsJsonObject("modules");
            if (mods != null) for (Module m : client.modules.all()) {
                if (!mods.has(m.getName())) continue;
                JsonObject o = mods.getAsJsonObject(m.getName());
                JsonObject s = o.getAsJsonObject("settings");
                if (s != null) for (Setting<?> st : m.getSettings()) {
                    if (!s.has(st.getName())) continue;
                    var e = s.get(st.getName());
                    if (st instanceof BooleanSetting b) b.set(e.getAsBoolean());
                    else if (st instanceof NumberSetting n) n.set(e.getAsDouble());
                    else if (st instanceof ColorSetting c) c.set(e.getAsFloat());
                    else if (st instanceof KeybindSetting k) k.set(e.getAsInt());
                    else if (st instanceof ModeSetting md && md.modes.contains(e.getAsString())) md.set(e.getAsString());
                }
                if (o.has("enabled") && !(m instanceof dev.nova.client.module.impl.ClickGuiModule)) m.setEnabled(o.get("enabled").getAsBoolean());
            }
            JsonObject panels = root.getAsJsonObject("panels");
            if (panels != null) for (Category c : Category.values()) {
                if (!panels.has(c.name())) continue;
                JsonObject o = panels.getAsJsonObject(c.name());
                PanelState ps = client.panelState(c);
                ps.x = o.get("x").getAsDouble();
                ps.y = o.get("y").getAsDouble();
                ps.open = o.get("open").getAsBoolean();
            }
        } catch (Exception e) {
            System.err.println("[Nova] Failed to load config: " + e);
        }
    }
}
