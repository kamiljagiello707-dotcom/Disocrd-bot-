package dev.razer.client.config;

import com.google.gson.*;
import dev.razer.client.RazerClient;
import dev.razer.client.hud.HudElement;
import dev.razer.client.lang.Lang;
import dev.razer.client.module.Module;
import dev.razer.client.setting.Setting;
import dev.razer.client.ui.Anim;
import dev.razer.client.ui.Theme;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Zapis / odczyt konfiguracji do .minecraft/razerclient/configs/*.json */
public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path dir = FabricLoader.getInstance().getGameDir().resolve("razerclient");
    private final Path configs = dir.resolve("configs");
    private final Path clientFile = dir.resolve("client.json");
    private String current = "default";

    public Path folder() { return configs; }
    public String current() { return current; }

    public List<String> list() {
        List<String> out = new ArrayList<>();
        try {
            Files.createDirectories(configs);
            try (Stream<Path> s = Files.list(configs)) {
                s.filter(p -> p.toString().endsWith(".json")).sorted().forEach(p -> {
                    String n = p.getFileName().toString();
                    out.add(n.substring(0, n.length() - 5));
                });
            }
        } catch (IOException ignored) {}
        return out;
    }

    // ---------------------------------------------------------------- moduły

    public void save(String name) {
        RazerClient rc = RazerClient.get();
        JsonObject root = new JsonObject();
        JsonObject mods = new JsonObject();
        for (Module m : rc.modules.all()) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.isEnabled());
            o.addProperty("key", m.getKey());
            o.addProperty("favorite", m.favorite);
            JsonObject set = new JsonObject();
            for (Setting<?> s : m.getSettings()) set.add(s.getId(), s.save());
            o.add("settings", set);
            mods.add(m.getName(), o);
        }
        root.add("modules", mods);
        write(configs.resolve(name + ".json"), root);
        current = name;
        saveClient();
    }

    public boolean load(String name) {
        JsonObject root = read(configs.resolve(name + ".json"));
        if (root == null) return false;
        RazerClient rc = RazerClient.get();
        JsonObject mods = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
        for (Module m : rc.modules.all()) {
            if (!mods.has(m.getName())) continue;
            try {
                JsonObject o = mods.getAsJsonObject(m.getName());
                m.setEnabledSilent(o.get("enabled").getAsBoolean());
                m.setKey(o.get("key").getAsInt());
                if (o.has("favorite")) m.favorite = o.get("favorite").getAsBoolean();
                JsonObject set = o.getAsJsonObject("settings");
                for (Setting<?> s : m.getSettings()) if (set != null && set.has(s.getId())) s.load(set.get(s.getId()));
            } catch (Exception ignored) {}
        }
        current = name;
        saveClient();
        return true;
    }

    public void delete(String name) {
        try { Files.deleteIfExists(configs.resolve(name + ".json")); } catch (IOException ignored) {}
        if (name.equals(current)) current = "default";
    }

    public String newName() {
        List<String> l = list();
        int i = 1;
        while (l.contains("config-" + i)) i++;
        return "config-" + i;
    }

    // ---------------------------------------------------------------- klient

    public void saveClient() {
        ClientSettings s = RazerClient.get().settings;
        JsonObject o = new JsonObject();
        o.addProperty("theme", Theme.preset().name());
        o.addProperty("language", Lang.current().name());
        o.addProperty("config", current);
        o.add("guiSize", s.guiSize.save());
        o.add("animSpeed", s.animSpeed.save());
        o.addProperty("accent", Theme.accentPreset().name());
        o.add("blur", s.blur.save());
        o.add("particles", s.particles.save());
        o.add("sounds", s.sounds.save());
        o.add("welcome", s.welcome.save());
        o.add("customMenu", s.customMenu.save());
        o.addProperty("ui", 4);
        com.google.gson.JsonArray fr = new com.google.gson.JsonArray();
        for (String f : RazerClient.get().friends.all()) fr.add(f);
        o.add("friends", fr);
        o.add("notifications", s.notifications.save());
        JsonObject hud = new JsonObject();
        for (HudElement e : RazerClient.get().hud.elements) hud.add(e.id, e.save());
        o.add("hud", hud);
        write(clientFile, o);
    }

    public void loadAll() {
        JsonObject o = read(clientFile);
        ClientSettings s = RazerClient.get().settings;
        if (o != null) {
            try {
                Theme.setInstant(Theme.Preset.valueOf(o.get("theme").getAsString()));
                Lang.set(Lang.Language.valueOf(o.get("language").getAsString()));
                current = o.get("config").getAsString();
                s.guiSize.load(o.get("guiSize"));
                s.animSpeed.load(o.get("animSpeed"));
                s.blur.load(o.get("blur"));
                s.notifications.load(o.get("notifications"));
                if (o.has("accent")) Theme.setAccentInstant(Theme.Accent.valueOf(o.get("accent").getAsString()));
                if (o.has("particles")) s.particles.load(o.get("particles"));
                if (o.has("sounds")) s.sounds.load(o.get("sounds"));
                if (o.has("welcome")) s.welcome.load(o.get("welcome"));
                if (o.has("customMenu")) s.customMenu.load(o.get("customMenu"));
                if (o.has("friends")) for (var f : o.getAsJsonArray("friends")) RazerClient.get().friends.add(f.getAsString());
                // nowy wygląd 2.1 – jednorazowo przełącz na Midnight + niebieski akcent
                if (!o.has("ui") || o.get("ui").getAsInt() < 3) { Theme.setInstant(Theme.Preset.MIDNIGHT); Theme.setAccentInstant(Theme.Accent.BLUE); }
                // 2.2 – klient domyślnie po angielsku (jednorazowo)
                if (!o.has("ui") || o.get("ui").getAsInt() < 4) Lang.set(Lang.Language.ENGLISH);
                if (o.has("hud")) {
                    JsonObject hud = o.getAsJsonObject("hud");
                    for (HudElement e : RazerClient.get().hud.elements) if (hud.has(e.id)) e.load(hud.getAsJsonObject(e.id));
                }
            } catch (Exception ignored) {}
        }
        if (o == null) { Theme.setInstant(Theme.Preset.MIDNIGHT); Theme.setAccentInstant(Theme.Accent.BLUE); }
        Anim.globalSpeed = s.animSpeed.get().floatValue();
        if (!load(current)) save(current);
    }

    // ---------------------------------------------------------------- io

    private static void write(Path p, JsonObject o) {
        try {
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(o), StandardCharsets.UTF_8);
        } catch (IOException e) {
            RazerClient.LOGGER.error("Nie udało się zapisać {}", p, e);
        }
    }

    private static JsonObject read(Path p) {
        if (!Files.exists(p)) return null;
        try {
            return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }
}
