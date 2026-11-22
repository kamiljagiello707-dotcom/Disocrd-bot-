package dev.with.client.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.with.client.render.R2D;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ustawienia wyglądu ClickGUI (strona "Wygląd"), zapisywane w config/withclient/gui.json. */
public final class Theme {
    private Theme() {}

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("withclient").resolve("gui.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String[] ACCENT_PL = {"Biały", "Srebrny", "Fiolet", "Niebieski", "Cyjan", "Zielony", "Różowy", "Czerwony", "Pomarańcz", "Żółty"};
    public static final String[] ACCENT_EN = {"White", "Silver", "Violet", "Blue", "Cyan", "Green", "Pink", "Red", "Orange", "Yellow"};
    public static final int[] ACCENTS = {0xFFF4F4F5, 0xFFA8A8B2, 0xFF8B5CF6, 0xFF3B82F6, 0xFF22D3EE, 0xFF22C55E, 0xFFEC4899, 0xFFEF4444, 0xFFF97316, 0xFFEAB308};
    public static final String[] MOTIONS = {"Slow", "Normal", "Fast", "Off"};
    public static final String[] EASINGS = {"Smooth", "Spring", "Linear"};
    public static final String[] STYLES = {"Solid", "Glass", "Minimal"};

    public static int accentIndex = 0;
    public static String motion = "Normal", easing = "Smooth", style = "Solid";
    public static float scale = 1f, radius = 1f, dim = 0.4f;
    public static boolean blur = true, sounds = true, shadow = true;
    public static boolean particles = true, border = true, vignette = true;
    public static boolean tooltips = true, toasts = true, descriptions = true, keysInList = true;
    public static String config = "default";

    public static int accent() { return ACCENTS[Math.max(0, Math.min(ACCENTS.length - 1, accentIndex))]; }

    /** biało-czarny (biały lub srebrny akcent) */
    public static boolean mono() { return accentIndex <= 1; }

    /** kolor tekstu/ikon na tle akcentu (czarny na jasnym, biały na ciemnym) */
    public static int onAccent() { return contrast(accent()); }

    public static int contrast(int c) {
        float r = ((c >> 16) & 0xFF) / 255f, g = ((c >> 8) & 0xFF) / 255f, b = (c & 0xFF) / 255f;
        return 0.2126f * r + 0.7152f * g + 0.0722f * b > 0.55f ? 0xFF09090B : 0xFFFFFFFF;
    }

    /** jaśniejszy wariant akcentu (górna część gradientu) */
    public static int accentTop() { return R2D.blend(accent(), 0xFFFFFFFF, mono() ? 0.55f : 0.14f); }
    public static int accentBottom() { return mono() ? R2D.blend(accent(), 0xFF000000, 0.1f) : R2D.blend(accent(), 0xFF000000, 0.08f); }

    public static float motionSpeed() {
        return switch (motion) { case "Slow" -> 0.6f; case "Fast" -> 1.7f; case "Off" -> 40f; default -> 1f; };
    }

    public static void apply() {
        Anim.globalSpeed = motionSpeed();
        Anim.mode = switch (easing) { case "Spring" -> 1; case "Linear" -> 2; default -> 0; };
    }

    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                JsonObject o = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("accent")) accentIndex = o.get("accent").getAsInt();
                if (o.has("motion")) motion = o.get("motion").getAsString();
                if (o.has("easing")) easing = o.get("easing").getAsString();
                if (o.has("style")) style = o.get("style").getAsString();
                if (o.has("scale")) scale = clamp(o.get("scale").getAsFloat(), 0.8f, 1.4f);
                if (o.has("radius")) radius = clamp(o.get("radius").getAsFloat(), 0.3f, 1.6f);
                if (o.has("dim")) dim = clamp(o.get("dim").getAsFloat(), 0, 0.8f);
                if (o.has("blur")) blur = o.get("blur").getAsBoolean();
                if (o.has("sounds")) sounds = o.get("sounds").getAsBoolean();
                if (o.has("shadow")) shadow = o.get("shadow").getAsBoolean();
                if (o.has("particles")) particles = o.get("particles").getAsBoolean();
                if (o.has("border")) border = o.get("border").getAsBoolean();
                if (o.has("vignette")) vignette = o.get("vignette").getAsBoolean();
                if (o.has("tooltips")) tooltips = o.get("tooltips").getAsBoolean();
                if (o.has("toasts")) toasts = o.get("toasts").getAsBoolean();
                if (o.has("descriptions")) descriptions = o.get("descriptions").getAsBoolean();
                if (o.has("keysInList")) keysInList = o.get("keysInList").getAsBoolean();
                if (o.has("config")) config = o.get("config").getAsString();
                // 1.1: nowy, biało-czarny wygląd domyślnie i nowa (mniejsza) skala
                if (!o.has("v") || o.get("v").getAsInt() < 3) { accentIndex = 0; scale = 1f; }
                accentIndex = Math.max(0, Math.min(ACCENTS.length - 1, accentIndex));
            }
        } catch (Exception ignored) {}
        apply();
    }

    public static void save() {
        JsonObject o = new JsonObject();
        o.addProperty("accent", accentIndex);
        o.addProperty("motion", motion);
        o.addProperty("easing", easing);
        o.addProperty("style", style);
        o.addProperty("scale", scale);
        o.addProperty("radius", radius);
        o.addProperty("dim", dim);
        o.addProperty("blur", blur);
        o.addProperty("sounds", sounds);
        o.addProperty("shadow", shadow);
        o.addProperty("particles", particles);
        o.addProperty("border", border);
        o.addProperty("vignette", vignette);
        o.addProperty("tooltips", tooltips);
        o.addProperty("toasts", toasts);
        o.addProperty("descriptions", descriptions);
        o.addProperty("keysInList", keysInList);
        o.addProperty("config", config);
        o.addProperty("v", 3);
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(o), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
        apply();
    }
}
