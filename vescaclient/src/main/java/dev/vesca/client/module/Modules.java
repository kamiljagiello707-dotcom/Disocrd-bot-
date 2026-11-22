package dev.vesca.client.module;

import com.google.gson.*;
import dev.vesca.client.module.Setting.Bool;
import dev.vesca.client.module.Setting.Mode;
import dev.vesca.client.module.Setting.Num;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static dev.vesca.client.module.Category.*;

/** Lista modułów, przełączanie klawiszami i zapis do config/vescaclient/modules.json. */
public final class Modules {
    private Modules() {}

    private static final List<Module> ALL = new ArrayList<>();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("vescaclient").resolve("modules.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<Integer> held = new HashSet<>();

    public static List<Module> all() { return ALL; }
    public static Module get(String name) { for (Module m : ALL) if (m.name.equals(name)) return m; return null; }
    public static Path configDir() { return FILE.getParent().resolve("configs"); }
    public static List<Module> of(Category c) { return ALL.stream().filter(m -> m.category == c).toList(); }
    public static long enabledIn(Category c) { return ALL.stream().filter(m -> m.category == c && m.isEnabled()).count(); }

    private static Module m(String name, Category c, String pl, String en, Setting<?>... s) {
        Module mod = new Module(name, c, "g_m_" + name.toLowerCase(Locale.ROOT).replace(" ", ""), pl, en).add(s);
        ALL.add(mod);
        return mod;
    }

    private static Bool b(String n, boolean d) { return new Bool(n, d); }
    private static Num n(String n, double d, double min, double max, double step, String unit) { return new Num(n, d, min, max, step, unit); }
    private static Mode o(String n, String... modes) { return new Mode(n, modes); }

    public static void init() {
        // --- Walka
        m("KillAura", COMBAT, "Atakuje cele w zasięgu", "Attacks targets in range",
                o("Mode", "Single", "Switch", "Multi"), n("Range", 3.2, 2.5, 6, 0.1, "m"), n("CPS", 12, 1, 20, 1, ""),
                o("Rotations", "Smooth", "Snap", "None"), b("Players", true), b("Mobs", false), b("Through walls", false));
        m("AimAssist", COMBAT, "Pomaga celować w przeciwnika", "Helps you aim at enemies",
                n("Speed", 4, 1, 10, 0.5, ""), n("Range", 4.5, 3, 8, 0.1, "m"), n("FOV", 90, 30, 180, 5, "°"), b("Only while clicking", true));
        m("TriggerBot", COMBAT, "Bije, gdy celujesz w gracza", "Hits when you look at a player",
                n("Delay", 80, 0, 500, 10, "ms"), b("Only crits", false));
        m("AutoClicker", COMBAT, "Klika za Ciebie", "Clicks for you",
                n("Min CPS", 9, 1, 20, 1, ""), n("Max CPS", 13, 1, 20, 1, ""), b("Jitter", false), b("Break blocks", true));
        m("Criticals", COMBAT, "Każde uderzenie krytyczne", "Every hit is a critical",
                o("Mode", "Packet", "Jump", "Mini jump"));
        m("Reach", COMBAT, "Większy zasięg bicia", "Longer attack reach",
                n("Range", 3.3, 3, 6, 0.05, "m"), n("Chance", 100, 0, 100, 5, "%"));
        m("Velocity", COMBAT, "Mniejszy odrzut", "Less knockback",
                o("Mode", "Cancel", "Percent", "Reverse"), n("Horizontal", 0, 0, 100, 5, "%"), n("Vertical", 0, 0, 100, 5, "%"));
        m("AutoTotem", COMBAT, "Totem zawsze w drugiej ręce", "Keeps a totem in your offhand",
                n("Health", 10, 1, 20, 1, "HP"), b("Always", true));
        m("WTap", COMBAT, "Reset sprintu po uderzeniu", "Resets sprint after each hit",
                o("Mode", "Legit", "Packet"), n("Delay", 2, 0, 10, 1, "t"));
        m("FakeLag", COMBAT, "Udaje opóźnienie połączenia", "Simulates a laggy connection",
                o("Mode", "Dynamic", "Constant", "Pulse"), n("Delay", 300, 50, 1000, 10, "ms"));
        // --- Ruch
        m("Sprint", MOVEMENT, "Zawsze biegniesz", "Always sprint", b("Omni-sprint", false), b("Keep sprint", true));
        m("Speed", MOVEMENT, "Szybsze poruszanie się", "Move faster",
                o("Mode", "Strafe", "BHop", "Ground"), n("Speed", 1.2, 1, 3, 0.05, "×"), b("Timer boost", false));
        m("Flight", MOVEMENT, "Latanie w trybie przetrwania", "Fly in survival",
                o("Mode", "Vanilla", "Glide", "Creative"), n("Speed", 1.5, 0.1, 5, 0.1, ""), b("Anti-kick", true));
        m("NoFall", MOVEMENT, "Brak obrażeń od upadku", "No fall damage", o("Mode", "Packet", "Spoof", "Bucket"));
        m("Step", MOVEMENT, "Wchodzenie na wyższe bloki", "Step up taller blocks", n("Height", 1, 0.5, 2.5, 0.5, "bl"));
        m("Jesus", MOVEMENT, "Chodzenie po wodzie", "Walk on water", o("Mode", "Solid", "Dolphin"));
        m("Spider", MOVEMENT, "Wspinanie się po ścianach", "Climb walls", n("Speed", 0.2, 0.1, 1, 0.05, ""));
        m("LongJump", MOVEMENT, "Dalekie skoki", "Jump much further", n("Boost", 2, 1, 5, 0.1, "×"), b("Auto disable", true));
        // --- Gracz
        m("AutoArmor", PLAYER, "Sam zakłada najlepszą zbroję", "Equips your best armor", n("Delay", 120, 0, 500, 10, "ms"));
        m("AutoEat", PLAYER, "Je, gdy jesteś głodny", "Eats when you are hungry", n("Hunger", 14, 1, 19, 1, ""), b("Golden apples", false));
        m("FastPlace", PLAYER, "Szybsze stawianie bloków", "Place blocks faster", n("Delay", 0, 0, 4, 1, "t"));
        m("InvManager", PLAYER, "Porządkuje ekwipunek", "Sorts your inventory", n("Delay", 100, 0, 500, 10, "ms"), b("Drop junk", true));
        m("AutoTool", PLAYER, "Wybiera najlepsze narzędzie", "Picks the best tool", b("Switch back", true));
        m("NoSlow", PLAYER, "Bez spowolnienia przy jedzeniu", "No slowdown while eating", b("Food", true), b("Bows", true), b("Shields", true));
        m("ChestStealer", PLAYER, "Szybko wyjmuje rzeczy ze skrzyń", "Quickly loots chests", n("Delay", 80, 0, 300, 10, "ms"), b("Auto close", true));
        m("AutoRespawn", PLAYER, "Odradza się od razu", "Respawns instantly", n("Delay", 0, 0, 3000, 100, "ms"));
        // --- Wygląd
        m("3D GUI", RENDER, "ClickGUI jako pochylony panel w przestrzeni", "ClickGUI as a tilted panel in space",
                n("Tilt", 16, 6, 30, 1, "°"), o("Side", "Right", "Left"), b("Follow mouse", true));
        m("ESP", RENDER, "Podświetla graczy i moby", "Highlights players and mobs",
                o("Mode", "Box", "Outline", "2D"), b("Players", true), b("Mobs", false), b("Items", false));
        m("Fullbright", RENDER, "Pełna jasność wszędzie", "Full brightness everywhere", n("Gamma", 100, 10, 100, 5, "%"));
        m("Nametags", RENDER, "Czytelniejsze nicki nad graczami", "Cleaner name tags", n("Scale", 1, 0.5, 2, 0.1, "×"), b("Health", true), b("Armor", true));
        m("Tracers", RENDER, "Linie do graczy", "Lines to players", n("Width", 1.5, 0.5, 4, 0.5, "px"), b("Players", true));
        m("Chams", RENDER, "Gracze widoczni w kolorze", "Colored player models", o("Style", "Flat", "Shiny", "Glow"));
        m("Zoom", RENDER, "Przybliżenie kamery", "Camera zoom", n("Factor", 4, 2, 10, 0.5, "×"), b("Smooth", true), b("Scroll to zoom", true));
        m("NoHurtCam", RENDER, "Kamera nie trzęsie się po obrażeniach", "No camera shake when hurt");
        m("Freecam", RENDER, "Swobodna kamera", "Free-flying camera", n("Speed", 1, 0.1, 5, 0.1, ""));
        // --- Świat
        m("Scaffold", WORLD, "Stawia bloki pod nogami", "Places blocks under you", o("Mode", "Normal", "Telly", "Expand"), b("Tower", true), b("Swing", false));
        m("Nuker", WORLD, "Niszczy bloki wokół", "Breaks blocks around you", n("Radius", 4, 1, 6, 1, "bl"));
        m("FastBreak", WORLD, "Szybsze kopanie", "Mine faster", n("Speed", 30, 0, 100, 5, "%"));
        m("Timer", WORLD, "Zmienia tempo gry", "Changes game speed", n("Speed", 1, 0.1, 5, 0.05, "×"));
        m("Weather", WORLD, "Własna pogoda", "Custom weather", o("Mode", "Clear", "Rain", "Snow"));
        m("XRay", WORLD, "Widać rudy przez bloki", "See ores through blocks", b("Diamonds", true), b("Gold", true), b("Iron", false), n("Opacity", 0, 0, 100, 5, "%"));
        m("TimeChanger", WORLD, "Własna pora dnia", "Custom time of day", n("Time", 6000, 0, 24000, 500, ""));
        // --- Inne
        m("DiscordRPC", MISC, "Status w Discordzie", "Discord rich presence", b("Show server", true), b("Show time", true));
        m("AntiAFK", MISC, "Nie wyrzuci Cię za AFK", "Avoids AFK kicks", n("Interval", 30, 5, 120, 5, "s"));
        m("Notifications", MISC, "Powiadomienia o modułach", "Module notifications", n("Duration", 2, 1, 6, 0.5, "s"), o("Position", "Bottom", "Corner"));
        m("HUD", MISC, "Elementy na ekranie", "On-screen elements", b("Watermark", true), b("Module list", true), b("Coordinates", false));
        m("AutoGG", MISC, "Pisze GG po grze", "Says GG after a match", o("Message", "gg", "GG WP", "Good game"));
        m("NameProtect", MISC, "Ukrywa Twój nick", "Hides your nickname", o("Name", "You", "Player", "Vesca"));

        load();
        ClientTickEvents.END_CLIENT_TICK.register(Modules::tick);
    }

    /** przełączanie modułów przypisanymi klawiszami (tylko w grze, bez otwartego ekranu) */
    private static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.currentScreen != null) { held.clear(); return; }
        long win = mc.getWindow().getHandle();
        for (Module m : ALL) {
            if (m.key < 0) continue;
            boolean down = InputUtil.isKeyPressed(win, m.key);
            if (down && held.add(m.key)) { m.toggle(); dev.vesca.client.ui.Hud.onToggle(m); save(); }
            else if (!down) held.remove(m.key);
        }
    }

    // ------------------------------------------------------------------ zapis

    public static void save() { saveTo(FILE); }

    private static void saveTo(Path file) {
        JsonObject root = new JsonObject();
        for (Module m : ALL) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.isEnabled());
            o.addProperty("key", m.key);
            o.addProperty("favorite", m.favorite);
            JsonObject s = new JsonObject();
            for (Setting<?> st : m.settings) s.add(st.name, st.save());
            o.add("settings", s);
            root.add(m.name, o);
        }
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }

    private static void load() { loadFrom(FILE); }

    private static boolean loadFrom(Path file) {
        if (!Files.exists(file)) return false;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Module m : ALL) {
                if (!root.has(m.name)) continue;
                JsonObject o = root.getAsJsonObject(m.name);
                try {
                    m.setEnabled(o.get("enabled").getAsBoolean());
                    m.key = o.get("key").getAsInt();
                    m.favorite = o.has("favorite") && o.get("favorite").getAsBoolean();
                    JsonObject s = o.getAsJsonObject("settings");
                    for (Setting<?> st : m.settings) if (s != null && s.has(st.name)) st.load(s.get(st.name));
                } catch (Exception ignored) {}
            }
            return true;
        } catch (Exception ignored) { return false; }
    }

    // ------------------------------------------------------------------ konfiguracje (strona "Konfiguracje")

    public record Config(String name, long modified) {}

    public static List<Config> configs() {
        List<Config> out = new ArrayList<>();
        try {
            Files.createDirectories(configDir());
            try (var s = Files.list(configDir())) {
                s.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                    String n = p.getFileName().toString();
                    long t = 0;
                    try { t = Files.getLastModifiedTime(p).toMillis(); } catch (IOException ignored) {}
                    out.add(new Config(n.substring(0, n.length() - 5), t));
                });
            }
        } catch (IOException ignored) {}
        out.sort(Comparator.comparing(Config::name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public static void saveConfig(String name) { saveTo(configDir().resolve(name + ".json")); }

    public static boolean loadConfig(String name) {
        for (Module m : ALL) { m.setEnabled(false); m.key = -1; m.favorite = false; for (Setting<?> s : m.settings) s.reset(); }
        boolean ok = loadFrom(configDir().resolve(name + ".json"));
        save();
        return ok;
    }

    public static void deleteConfig(String name) {
        try { Files.deleteIfExists(configDir().resolve(name + ".json")); } catch (IOException ignored) {}
    }

    public static String newConfigName() {
        List<String> names = configs().stream().map(Config::name).toList();
        int i = 1;
        while (names.contains("config-" + i)) i++;
        return "config-" + i;
    }
}
