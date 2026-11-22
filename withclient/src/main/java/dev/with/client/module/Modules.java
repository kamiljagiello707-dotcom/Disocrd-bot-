package dev.with.client.module;

import com.google.gson.*;
import dev.with.client.module.Setting.Bool;
import dev.with.client.module.Setting.Mode;
import dev.with.client.module.Setting.Num;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static dev.with.client.module.Category.*;

/** Lista modułów, przełączanie klawiszami i zapis do config/withclient/modules.json. */
public final class Modules {
    private Modules() {}

    private static final List<Module> ALL = new ArrayList<>();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("withclient").resolve("modules.json");
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
    private static Setting.Color cl(String n, int d) { return new Setting.Color(n, d); }
     public static void init() {
        int ACC = 0xFF4DA6FF;
        // ================= Walka =================
        m("KillAura", COMBAT, "Atakuje cele w zasięgu", "Attacks targets in range",
                o("Tryb", "Single", "Switch", "Multi"), n("Zasięg", 3.5, 2.5, 6, 0.1, "m"), n("CPS", 12, 1, 20, 1, ""),
                o("Rotacje", "Smooth", "Snap", "Silent", "None"), n("Smoothness", 40, 0, 100, 1, "%"),
                o("Sortowanie", "Distance", "Health", "Angle"), b("Gracze", true), b("Moby", false), b("Zwierzęta", false),
                b("Przez ściany", false), b("Wymuś krytyki", false), n("Opóźnienie", 0, 0, 500, 10, "ms"), cl("Kolor celu", ACC));
        m("AimAssist", COMBAT, "Pomaga celować w przeciwnika", "Helps you aim at enemies",
                n("Prędkość", 4, 1, 10, 0.5, ""), n("Zasięg", 4.5, 3, 8, 0.1, "m"), n("FOV", 90, 10, 180, 5, "°"),
                o("Płynność", "Low", "Medium", "High"), b("Tylko przy kliku", true), b("Przewidywanie", false));
        m("TriggerBot", COMBAT, "Bije, gdy celujesz w gracza", "Hits when you look at a player",
                n("Opóźnienie", 80, 0, 500, 10, "ms"), b("Tylko krytyki", false), b("Gracze", true), b("Moby", false));
        m("AutoClicker", COMBAT, "Klika za Ciebie", "Clicks for you",
                n("Min CPS", 9, 1, 20, 1, ""), n("Max CPS", 13, 1, 20, 1, ""), b("Jitter", true), b("Rozbijanie bloków", true), b("Tylko z mieczem", false));
        m("Criticals", COMBAT, "Każde uderzenie krytyczne", "Every hit is a critical", o("Tryb", "Packet", "Jump", "Mini jump"), b("Tylko KillAura", false));
        m("Reach", COMBAT, "Większy zasięg bicia", "Longer attack reach", n("Zasięg", 3.3, 3, 6, 0.05, "m"), n("Szansa", 100, 0, 100, 5, "%"));
        m("Velocity", COMBAT, "Mniejszy odrzut", "Less knockback", o("Tryb", "Cancel", "Percent", "Reverse", "Jump reset"),
                n("Poziomo", 0, 0, 100, 5, "%"), n("Pionowo", 0, 0, 100, 5, "%"), b("Tylko przy celu", false));
        m("AutoTotem", COMBAT, "Totem w drugiej ręce", "Keeps a totem in your offhand", n("Zdrowie", 10, 1, 20, 1, "HP"), b("Zawsze", true), b("Licznik", true));
        m("Hitboxes", COMBAT, "Większe hitboxy", "Bigger hitboxes", n("Rozmiar", 0.1, 0, 1, 0.05, "bl"), b("Gracze", true));
        m("WTap", COMBAT, "Reset sprintu po uderzeniu", "Resets sprint after each hit", o("Tryb", "Legit", "Packet"), n("Opóźnienie", 2, 0, 10, 1, "t"));
        m("FakeLag", COMBAT, "Udaje opóźnienie", "Simulates lag", o("Tryb", "Dynamic", "Constant", "Pulse"), n("Opóźnienie", 300, 50, 1000, 10, "ms"), n("Zakres", 6, 2, 10, 0.5, "m"));
        m("Backtrack", COMBAT, "Cofa pozycję wroga", "Backtracks enemies", n("Opóźnienie", 100, 0, 500, 10, "ms"), n("Zakres", 4, 1, 6, 0.1, "m"), cl("Kolor", 0xFFFF5C7A));
        // ================= Ruch =================
        m("Sprint", MOVEMENT, "Zawsze biegniesz", "Always sprint", b("Omni-sprint", false), b("Keep sprint", true), b("Ukryj w HUD", false));
        m("Speed", MOVEMENT, "Szybsze poruszanie się", "Move faster", o("Tryb", "Strafe", "BHop", "Ground", "YPort"), n("Prędkość", 1.3, 1, 3, 0.05, "×"), b("Timer boost", false), b("Auto jump", true));
        m("Flight", MOVEMENT, "Latanie", "Fly", o("Tryb", "Vanilla", "Glide", "Creative", "Jetpack"), n("Prędkość", 1.5, 0.1, 5, 0.1, ""), n("Pionowo", 1, 0.1, 3, 0.1, ""), b("Anti-kick", true));
        m("NoFall", MOVEMENT, "Brak obrażeń od upadku", "No fall damage", o("Tryb", "Packet", "Spoof", "Bucket", "AirJump"));
        m("Step", MOVEMENT, "Wchodzenie na bloki", "Step up blocks", n("Wysokość", 1, 0.5, 3, 0.5, "bl"), b("Płynnie", true));
        m("Jesus", MOVEMENT, "Chodzenie po wodzie", "Walk on water", o("Tryb", "Solid", "Dolphin"), b("Lawa", false));
        m("Spider", MOVEMENT, "Wspinanie po ścianach", "Climb walls", n("Prędkość", 0.2, 0.1, 1, 0.05, ""));
        m("LongJump", MOVEMENT, "Dalekie skoki", "Long jumps", n("Siła", 2, 1, 5, 0.1, "×"), b("Auto wyłącz", true));
        m("Sneak", MOVEMENT, "Auto skradanie", "Auto sneak", o("Tryb", "Legit", "Packet"), b("Przy krawędzi", true));
        m("ElytraFly", MOVEMENT, "Lepsza elytra", "Better elytra", o("Tryb", "Vanilla", "Boost", "Bounce"), n("Boost", 1, 0.5, 3, 0.1, "×"));
        // ================= Gracz =================
        m("AutoArmor", PLAYER, "Zakłada najlepszą zbroję", "Equips best armor", n("Opóźnienie", 120, 0, 500, 10, "ms"), b("W ekwipunku", true));
        m("AutoEat", PLAYER, "Je gdy głodny", "Eats when hungry", n("Głód", 14, 1, 19, 1, ""), b("Złote jabłka", false), b("Pauza KillAura", true));
        m("FastPlace", PLAYER, "Szybsze stawianie", "Faster placing", n("Opóźnienie", 0, 0, 4, 1, "t"));
        m("InvManager", PLAYER, "Porządkuje ekwipunek", "Sorts inventory", n("Opóźnienie", 100, 0, 500, 10, "ms"), b("Wyrzuć śmieci", true), b("Auto refill", false));
        m("AutoTool", PLAYER, "Najlepsze narzędzie", "Best tool", b("Przełącz z powrotem", true), b("Pauza KillAura", true));
        m("NoSlow", PLAYER, "Bez spowolnienia", "No slowdown", b("Jedzenie", true), b("Łuki", true), b("Tarcze", true), b("Pajęczyna", false));
        m("ChestStealer", PLAYER, "Kradnie ze skrzyń", "Loots chests", n("Opóźnienie", 80, 0, 300, 10, "ms"), b("Auto zamknij", true));
        m("AutoRespawn", PLAYER, "Odradza się od razu", "Instant respawn", n("Opóźnienie", 0, 0, 3000, 100, "ms"));
        m("Blink", PLAYER, "Wstrzymuje pakiety", "Holds packets", n("Max", 60, 10, 200, 10, "t"), b("Pokaż licznik", true));
        // ================= Wizualne =================
        m("ESP", VISUALS, "Podświetla byty", "Highlights entities", o("Tryb", "Box", "2D", "Outline", "Glow"),
                b("Gracze", true), b("Moby", false), b("Przedmioty", false), n("Grubość", 1.5, 0.5, 4, 0.5, "px"),
                cl("Kolor", 0xFF4DA6FF), cl("Kolor znajomych", 0xFF46D17A), b("Przez ściany", true), b("Health bar", true));
        m("Fullbright", VISUALS, "Pełna jasność", "Full brightness", n("Gamma", 100, 10, 100, 5, "%"), o("Tryb", "Gamma", "Night vision"));
        m("Nametags", VISUALS, "Lepsze nicki", "Better nametags", n("Skala", 1, 0.5, 2, 0.1, "×"), b("Zdrowie", true), b("Zbroja", true), b("Ping", false), b("Odległość", false), cl("Tło", 0xAA000000));
        m("Tracers", VISUALS, "Linie do bytów", "Tracers to entities", n("Grubość", 1.5, 0.5, 4, 0.5, "px"), b("Gracze", true), b("Moby", false), cl("Kolor", 0xFF4DA6FF));
        m("Chams", VISUALS, "Prześwitujące modele", "See-through models", o("Styl", "Flat", "Shiny", "Texture"), cl("Widoczne", 0xFF4DA6FF), cl("Za ścianą", 0xFFFF5C7A));
        m("Zoom", VISUALS, "Przybliżenie", "Zoom", n("Siła", 4, 2, 10, 0.5, "×"), b("Płynnie", true), b("Scroll", true));
        m("NoHurtCam", VISUALS, "Brak trzęsienia", "No hurt cam");
        m("Freecam", VISUALS, "Swobodna kamera", "Free camera", n("Prędkość", 1, 0.1, 5, 0.1, ""));
        m("Trajectories", VISUALS, "Tor pocisków", "Projectile paths", cl("Kolor", 0xFF4DA6FF), b("Punkt trafienia", true));
        m("BlockOutline", VISUALS, "Lepszy obrys bloków", "Better block outline", n("Grubość", 2, 0.5, 5, 0.5, "px"), cl("Kolor", 0xFFFFFFFF), b("Tęcza", false));
        m("Breadcrumbs", VISUALS, "Ślad za graczem", "Trail behind you", cl("Kolor", 0xFF4DA6FF), n("Długość", 200, 20, 500, 10, ""));
        // ================= Świat =================
        m("Scaffold", WORLD, "Stawia bloki pod nogami", "Places blocks under you", o("Tryb", "Normal", "Telly", "Expand", "Godbridge"), b("Tower", true), b("Swing", true), n("Opóźnienie", 0, 0, 5, 1, "t"));
        m("Nuker", WORLD, "Niszczy bloki wokół", "Breaks nearby blocks", n("Promień", 4, 1, 6, 1, "bl"), o("Tryb", "Flat", "Sphere"));
        m("FastBreak", WORLD, "Szybsze kopanie", "Mine faster", n("Prędkość", 30, 0, 100, 5, "%"));
        m("Timer", WORLD, "Tempo gry", "Game speed", n("Prędkość", 1, 0.1, 5, 0.05, "×"));
        m("Weather", WORLD, "Własna pogoda", "Custom weather", o("Tryb", "Clear", "Rain", "Snow"));
        m("XRay", WORLD, "Rudy przez bloki", "See ores", b("Diamenty", true), b("Złoto", true), b("Żelazo", false), b("Netheryt", true), n("Przezroczystość", 0, 0, 100, 5, "%"));
        m("AutoFarm", WORLD, "Automatyczna farma", "Auto farm", o("Tryb", "Crops", "Trees"), n("Zasięg", 4, 2, 6, 0.5, "m"));
        // ================= Exploity =================
        m("AntiAFK", EXPLOITS, "Nie wyrzuci za AFK", "Avoids AFK kicks", n("Co ile", 30, 5, 120, 5, "s"), o("Akcja", "Jump", "Rotate", "Swing"));
        m("PacketMine", EXPLOITS, "Kopanie pakietami", "Packet mining", b("Auto reset", true));
        m("Disabler", EXPLOITS, "Omija anticheat", "Bypasses anticheat", o("Tryb", "Grim", "Vulcan", "NCP"));
        m("ChestESP", EXPLOITS, "Podświetla skrzynie", "Highlights chests", cl("Kolor", 0xFFF2C94C), n("Zasięg", 32, 8, 96, 4, "bl"));
        m("NoWeb", EXPLOITS, "Ignoruje pajęczynę", "Ignores cobwebs");
        // ================= Klient =================
        m("ClickGUI", CLIENT, "Interfejs klienta", "Client interface", b("Rozmycie tła", true), b("Dźwięki", true));
        m("HUD", CLIENT, "Elementy na ekranie", "On-screen HUD", b("Watermark", true), b("Lista modułów", true), b("Keybindy", true), cl("Kolor", 0xFF4DA6FF));
        m("DiscordRPC", CLIENT, "Status w Discordzie", "Discord presence", b("Pokaż serwer", true), b("Pokaż czas", true));
        m("Notifications", CLIENT, "Powiadomienia", "Notifications", n("Czas", 2, 1, 6, 0.5, "s"), o("Pozycja", "Corner", "Bottom"));
        m("AutoGG", CLIENT, "Pisze GG", "Says GG", o("Wiadomość", "gg", "GG WP", "Good game"));
        m("NameProtect", CLIENT, "Ukrywa nick", "Hides your name", o("Nazwa", "You", "Player", "With"));


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
            if (down && held.add(m.key)) { m.toggle(); save(); }
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
