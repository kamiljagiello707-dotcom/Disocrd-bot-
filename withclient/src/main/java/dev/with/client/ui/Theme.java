package dev.with.client.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ustawienia wyglądu ClickGUI (strona "Motyw"), zapisywane w config/withclient/gui.json. */
public final class Theme {
    private Theme() {}

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("withclient").resolve("gui.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String[] ACCENT_PL = {"Biały", "Fiolet", "Niebieski", "Cyjan", "Zielony", "Różowy", "Czerwony", "Pomarańcz", "Żółty"};
    public static final String[] ACCENT_EN = {"White", "Violet", "Blue", "Cyan", "Green", "Pink", "Red", "Orange", "Yellow"};
    public static final int[] ACCENTS = {0xFFF4F4F5, 0xFF8B5CF6, 0xFF3B82F6, 0xFF22D3EE, 0xFF22C55E, 0xFFEC4899, 0xFFEF4444, 0xFFF97316, 0xFFEAB308};
    public static final String[] MOTIONS = {"Slow", "Normal", "Fast", "Off"};
    public static final String[] SIDEBARS = {"Auto", "Expanded", "Collapsed"};

    public static int accentIndex = 0;
    public static String motion = "Normal";
    public static float scale = 1f;
    public static String sidebar = "Expanded";
    public static boolean blur = true, sounds = true, shadow = true;
    public static float dim = 0.45f;
    public static String config = "default";

    public static int accent() { return ACCENTS[Math.max(0, Math.min(ACCENTS.length - 1, accentIndex))]; }

    /** kolor tekstu/ikon na tle akcentu (czarny na jasnym, biały na ciemnym) */
    public static int onAccent() { return contrast(accent()); }

    public static int contrast(int c) {
        float r = ((c >> 16) & 0xFF) / 255f, g = ((c >> 8) & 0xFF) / 255f, b = (c & 0xFF) / 255f;
        return 0.2126f * r + 0.7152f * g + 0.0722f * b > 0.55f ? 0xFF09090B : 0xFFFFFFFF;
    }

    /** jaśniejszy wariant akcentu (górna część gradientu) */
    public static int accentTop() { return dev.with.client.render.R2D.blend(accent(), 0xFFFFFFFF, accentIndex == 0 ? 0.6f : 0.14f); }
    public static int accentBottom() { return accentIndex == 0 ? 0xFFDCDCE0 : dev.with.client.render.R2D.blend(accent(), 0xFF000000, 0.08f); }

    public static float motionSpeed() {
        return switch (motion) { case "Slow" -> 0.6f; case "Fast" -> 1.7f; case "Off" -> 40f; default -> 1f; };
    }

    public static void apply() { Anim.globalSpeed = motionSpeed(); }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                JsonObject o = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("accent")) accentIndex = o.get("accent").getAsInt();
                if (o.has("motion")) motion = o.get("motion").getAsString();
                if (o.has("scale")) scale = Math.max(0.8f, Math.min(1.2f, o.get("scale").getAsFloat()));
                if (o.has("sidebar")) sidebar = o.get("sidebar").getAsString();
                if (o.has("blur")) blur = o.get("blur").getAsBoolean();
                if (o.has("sounds")) sounds = o.get("sounds").getAsBoolean();
                if (o.has("shadow")) shadow = o.get("shadow").getAsBoolean();
                if (o.has("dim")) dim = o.get("dim").getAsFloat();
                if (o.has("config")) config = o.get("config").getAsString();
                // 1.5: domyślnie stały pasek boczny (jak w nowym wyglądzie)
                if (!o.has("v") || o.get("v").getAsInt() < 2) sidebar = "Expanded";
            }
        } catch (Exception ignored) {}
        apply();
    }

    public static void save() {
        JsonObject o = new JsonObject();
        o.addProperty("accent", accentIndex);
        o.addProperty("motion", motion);
        o.addProperty("scale", scale);
        o.addProperty("sidebar", sidebar);
        o.addProperty("blur", blur);
        o.addProperty("sounds", sounds);
        o.addProperty("shadow", shadow);
        o.addProperty("dim", dim);
        o.addProperty("config", config);
        o.addProperty("v", 2);
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(o), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
        apply();
    }
}
