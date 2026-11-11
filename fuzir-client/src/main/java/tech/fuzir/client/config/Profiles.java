package tech.fuzir.client.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import tech.fuzir.client.FuzirClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Zapisane profile ustawień w config/fuzir-client/profiles/*.json. */
public final class Profiles {
    private static final Path DIR = Config.DIR.resolve("fuzir-client").resolve("profiles");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    private Profiles() {
    }

    public record Profile(String name, long modified, int enabled) {
        public String date() {
            return DATE.format(Instant.ofEpochMilli(modified));
        }
    }

    /** Usuwa znaki, których nie może mieć nazwa pliku. */
    public static String clean(String name) {
        return name.replaceAll("[^\\p{L}\\p{N} _.-]", "").trim();
    }

    public static List<Profile> list() {
        List<Profile> out = new ArrayList<>();
        if (!Files.isDirectory(DIR)) return out;
        try (Stream<Path> files = Files.list(DIR)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                String n = p.getFileName().toString();
                int enabled = 0;
                try {
                    JsonObject mods = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("modules");
                    if (mods != null) for (String k : mods.keySet()) {
                        JsonObject o = mods.getAsJsonObject(k);
                        if (o.has("enabled") && o.get("enabled").getAsBoolean()) enabled++;
                    }
                } catch (Exception ignored) {
                }
                out.add(new Profile(n.substring(0, n.length() - 5), Files.getLastModifiedTime(p).toMillis(), enabled));
            }
        } catch (IOException e) {
            FuzirClient.LOG.warn("Nie udało się odczytać profili", e);
        }
        out.sort(Comparator.comparingLong(Profile::modified).reversed());
        return out;
    }

    public static boolean save(String name) {
        name = clean(name);
        if (name.isEmpty()) return false;
        try {
            Files.createDirectories(DIR);
            Files.writeString(DIR.resolve(name + ".json"), Config.GSON.toJson(Config.toJson()), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            FuzirClient.LOG.warn("Nie udało się zapisać profilu {}", name, e);
            return false;
        }
    }

    public static boolean load(String name) {
        try {
            Config.fromJson(JsonParser.parseString(Files.readString(DIR.resolve(name + ".json"), StandardCharsets.UTF_8)).getAsJsonObject(), false);
            Config.save();
            return true;
        } catch (Exception e) {
            FuzirClient.LOG.warn("Nie udało się wczytać profilu {}", name, e);
            return false;
        }
    }

    public static void delete(String name) {
        try {
            Files.deleteIfExists(DIR.resolve(name + ".json"));
        } catch (IOException e) {
            FuzirClient.LOG.warn("Nie udało się usunąć profilu {}", name, e);
        }
    }

    public static Path folder() {
        return DIR;
    }
}
