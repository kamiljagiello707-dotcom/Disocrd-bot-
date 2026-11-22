package dev.with.client.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ostatni serwer i świat (szybki start w menu głównym) – config/withclient/recent.json. */
public final class Recent {
    private Recent() {}

    public record Entry(String name, String id) {}

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("withclient").resolve("recent.json");
    private static JsonObject data;

    private static JsonObject data() {
        if (data == null) {
            try { data = Files.exists(FILE) ? JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject() : new JsonObject(); }
            catch (Exception e) { data = new JsonObject(); }
        }
        return data;
    }

    private static Entry get(String k) {
        JsonObject o = data();
        if (!o.has(k)) return null;
        JsonObject e = o.getAsJsonObject(k);
        return new Entry(e.get("name").getAsString(), e.get("id").getAsString());
    }

    private static void put(String k, String name, String id) {
        JsonObject e = new JsonObject();
        e.addProperty("name", name);
        e.addProperty("id", id);
        data().add(k, e);
        try { Files.createDirectories(FILE.getParent()); Files.writeString(FILE, data.toString(), StandardCharsets.UTF_8); } catch (Exception ignored) {}
    }

    public static Entry server() { return get("server"); }
    public static Entry world() { return get("world"); }
    public static void server(String name, String address) { put("server", name, address); }
    public static void world(String name, String folder) { put("world", name, folder); }
}
