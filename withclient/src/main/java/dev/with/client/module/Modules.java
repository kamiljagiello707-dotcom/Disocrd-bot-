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
    private static final Map<String, Module> BY_NAME = new HashMap<>();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("withclient").resolve("modules.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<Integer> held = new HashSet<>();
    private static final int CONFIG_VERSION = 7;

    public static List<Module> all() { return ALL; }
    public static Module get(String name) { return BY_NAME.get(name); }
    public static Path configDir() { return FILE.getParent().resolve("configs"); }
    public static List<Module> of(Category c) { return ALL.stream().filter(m -> m.category == c).toList(); }
    public static long enabledIn(Category c) { return ALL.stream().filter(m -> m.category == c && m.isEnabled()).count(); }

    private static Module m(String name, Category c, String icon, String pl, String en, Setting<?>... s) {
        Module mod = new Module(name, c, "l_" + icon.replace('-', '_'), pl, en).add(s);
        ALL.add(mod);
        BY_NAME.put(name, mod);
        return mod;
    }

    private static Bool b(String n, boolean d) { return new Bool(n, d); }
    private static Num n(String n, double d, double min, double max, double step, String unit) { return new Num(n, d, min, max, step, unit); }
    private static Setting.Range r(String n, double lo, double hi, double min, double max, double step, String unit) { return new Setting.Range(n, lo, hi, min, max, step, unit); }
    private static Mode o(String n, String... modes) { return new Mode(n, modes); }
    private static Setting.Multi mu(String n, String[] opts, String... on) { return new Setting.Multi(n, opts, on); }
    private static Setting.Color cl(String n, int d) { return new Setting.Color(n, d); }
    private static Setting.Text tx(String n, String d, int max) { return new Setting.Text(n, d, max); }
    private static Setting.Group g(String n) { return new Setting.Group(n); }
    private static String[] a(String... s) { return s; }
    private static Setting.Multi targets(String... on) { return mu("Cele", a("Gracze", "Moby", "Zwierzęta", "Niewidzialni", "Znajomi"), on); }

    private static final int WHITE = 0xFFFFFFFF, SILVER = 0xFFB8B8C0, SOFT = 0xAAFFFFFF, RED = 0xFFFF5C7A, GREEN = 0xFF46D17A;

    public static void init() {
        // ================= Walka =================
        Mode kaRot = o("Rotacje", "Smooth", "Snap", "Silent", "None");
        m("KillAura", COMBAT, "crosshair", "Atakuje cele w zasięgu", "Attacks targets in range",
                g("Cel"), o("Tryb", "Single", "Switch", "Multi"), targets("Gracze"), o("Sortowanie", "Distance", "Health", "Angle", "Armor", "Hurt time"),
                n("Zasięg", 3.5, 2.5, 6, 0.1, "m"), n("Zasięg szukania", 5, 3, 8, 0.1, "m"), n("FOV", 180, 30, 360, 10, "°"), b("Przez ściany", false),
                g("Atak"), r("CPS", 10, 14, 1, 20, 1, ""), b("Wymuś krytyki", false), b("Tylko z bronią", false), b("Pauza przy jedzeniu", true), n("Opóźnienie switch", 150, 0, 1000, 10, "ms"),
                g("Rotacje"), kaRot, n("Smoothness", 40, 0, 100, 1, "%").vis(() -> kaRot.is("Smooth")), r("Prędkość obrotu", 60, 120, 10, 180, 5, "°").vis(() -> !kaRot.is("None")), b("Losowość", true).vis(() -> !kaRot.is("None")),
                g("Wygląd"), o("Znacznik celu", "Ring", "Box", "Glow", "None"), cl("Kolor celu", WHITE), b("Animacja", true));
        m("AimAssist", COMBAT, "locate-fixed", "Pomaga celować w przeciwnika", "Helps you aim at enemies",
                g("Cel"), targets("Gracze"), n("Zasięg", 4.5, 3, 8, 0.1, "m"), n("FOV", 90, 10, 180, 5, "°"), o("Część ciała", "Head", "Chest", "Closest"),
                g("Ruch"), n("Prędkość", 4, 1, 10, 0.5, ""), o("Płynność", "Low", "Medium", "High"), b("Tylko przy kliku", true), b("Przewidywanie", false), b("Pionowo", false));
        m("TriggerBot", COMBAT, "zap", "Bije, gdy celujesz w gracza", "Hits when you look at a player",
                r("Opóźnienie", 60, 120, 0, 500, 10, "ms"), b("Tylko krytyki", false), targets("Gracze"), b("Tylko z bronią", true));
        m("AutoClicker", COMBAT, "mouse-pointer-click", "Klika za Ciebie", "Clicks for you",
                g("Lewy"), b("Lewy przycisk", true), r("CPS", 9, 13, 1, 20, 1, ""), o("Wzór", "Normal", "Butterfly", "Jitter", "Drag"),
                g("Prawy"), b("Prawy przycisk", false), r("CPS prawy", 12, 16, 1, 25, 1, ""),
                g("Inne"), b("Rozbijanie bloków", true), b("Tylko z mieczem", false), b("W ekwipunku", false));
        m("Criticals", COMBAT, "flame", "Każde uderzenie krytyczne", "Every hit is a critical", o("Tryb", "Packet", "Jump", "Mini jump", "Smart"), b("Tylko KillAura", false), b("Efekt cząsteczek", true));
        m("Reach", COMBAT, "move-horizontal", "Większy zasięg bicia", "Longer attack reach", r("Zasięg", 3.1, 3.4, 3, 6, 0.05, "m"), n("Szansa", 100, 0, 100, 5, "%"), b("Tylko przy sprincie", false));
        Mode velMode = o("Tryb", "Cancel", "Percent", "Reverse", "Jump reset");
        m("Velocity", COMBAT, "shield", "Mniejszy odrzut", "Less knockback", velMode,
                n("Poziomo", 0, 0, 100, 5, "%").vis(() -> velMode.is("Percent")), n("Pionowo", 0, 0, 100, 5, "%").vis(() -> velMode.is("Percent")),
                n("Szansa", 100, 0, 100, 5, "%"), b("Tylko przy celu", false), b("W wodzie", false));
        m("AutoTotem", COMBAT, "heart-pulse", "Totem w drugiej ręce", "Keeps a totem in your offhand", n("Zdrowie", 10, 1, 20, 1, "HP"), b("Zawsze", true), b("Licznik", true), n("Opóźnienie", 0, 0, 500, 10, "ms"));
        m("Hitboxes", COMBAT, "square-dashed", "Większe hitboxy", "Bigger hitboxes", n("Rozmiar", 0.1, 0, 1, 0.05, "bl"), targets("Gracze"), b("Pokaż hitbox", false));
        m("WTap", COMBAT, "repeat", "Reset sprintu po uderzeniu", "Resets sprint after each hit", o("Tryb", "Legit", "Packet", "S-Tap"), r("Opóźnienie", 1, 3, 0, 10, 1, "t"), n("Szansa", 100, 0, 100, 5, "%"));
        m("FakeLag", COMBAT, "wifi-off", "Udaje opóźnienie", "Simulates lag", o("Tryb", "Dynamic", "Constant", "Pulse"), n("Opóźnienie", 300, 50, 1000, 10, "ms"), n("Zakres", 6, 2, 10, 0.5, "m"), b("Pokaż ducha", true), cl("Kolor ducha", SOFT));
        m("Backtrack", COMBAT, "history", "Cofa pozycję wroga", "Backtracks enemies", n("Opóźnienie", 100, 0, 500, 10, "ms"), n("Zakres", 4, 1, 6, 0.1, "m"), b("Pokaż pozycję", true), cl("Kolor", RED));
        m("AutoPot", COMBAT, "flask-conical", "Rzuca mikstury leczenia", "Throws healing potions", n("Zdrowie", 12, 1, 20, 1, "HP"), mu("Mikstury", a("Leczenie", "Regeneracja", "Siła", "Szybkość"), "Leczenie", "Regeneracja"), b("Tylko na ziemi", true), n("Opóźnienie", 400, 100, 2000, 50, "ms"));
        m("BowAimbot", COMBAT, "target", "Celowanie z łuku", "Bow aiming", targets("Gracze"), n("FOV", 60, 10, 180, 5, "°"), b("Przewidywanie", true), b("Tylko naciągnięty", true));
        m("AntiBot", COMBAT, "bot", "Ignoruje boty serwera", "Ignores server bots", o("Tryb", "Tab", "Hypixel", "Matrix", "Custom"), b("Usuń z świata", false), b("Sprawdzaj ping", true));
        m("Teams", COMBAT, "users-round", "Nie bije drużyny", "Ignores teammates", mu("Sprawdzaj", a("Kolor nicku", "Zbroja", "Scoreboard"), "Kolor nicku", "Scoreboard"));
        m("ShieldBreaker", COMBAT, "shield-off", "Zmienia na topór przy tarczy", "Switches to axe on shields", b("Wróć do miecza", true), n("Opóźnienie", 50, 0, 300, 10, "ms"));
        // ================= Ruch =================
        m("Sprint", MOVEMENT, "chevrons-right", "Zawsze biegniesz", "Always sprint", o("Tryb", "Legit", "Omni"), b("Keep sprint", true), b("Ukryj w HUD", false), b("Przy głodzie", false));
        Mode spMode = o("Tryb", "Strafe", "BHop", "Ground", "YPort", "Vulcan", "Grim");
        Bool spTimer = b("Timer boost", false);
        m("Speed", MOVEMENT, "gauge", "Szybsze poruszanie się", "Move faster", spMode, n("Prędkość", 1.3, 1, 3, 0.05, "×"), n("Wysokość skoku", 0.42, 0.1, 0.6, 0.01, "").vis(() -> spMode.is("BHop") || spMode.is("YPort")),
                spTimer, n("Timer", 1.08, 1, 2, 0.01, "×").vis(spTimer::get), b("Auto jump", true), b("Wyłącz w wodzie", true));
        Mode flMode = o("Tryb", "Vanilla", "Glide", "Creative", "Jetpack", "Motion");
        m("Flight", MOVEMENT, "plane", "Latanie", "Fly", flMode, n("Prędkość", 1.5, 0.1, 5, 0.1, ""), n("Pionowo", 1, 0.1, 3, 0.1, ""), n("Opadanie", 0.03, 0, 0.2, 0.01, "").vis(() -> flMode.is("Glide")), b("Anti-kick", true), b("Wyłącz po upadku", false));
        m("NoFall", MOVEMENT, "umbrella", "Brak obrażeń od upadku", "No fall damage", o("Tryb", "Packet", "Spoof", "Bucket", "AirJump", "Grim"), n("Min. wysokość", 3, 2, 10, 0.5, "bl"));
        m("Step", MOVEMENT, "arrow-big-up", "Wchodzenie na bloki", "Step up blocks", n("Wysokość", 1, 0.5, 3, 0.5, "bl"), o("Tryb", "Vanilla", "NCP", "Motion"), b("Płynnie", true));
        m("Jesus", MOVEMENT, "waves", "Chodzenie po wodzie", "Walk on water", o("Tryb", "Solid", "Dolphin", "Bounce"), b("Lawa", false));
        m("Spider", MOVEMENT, "arrow-up-to-line", "Wspinanie po ścianach", "Climb walls", n("Prędkość", 0.2, 0.1, 1, 0.05, ""), o("Tryb", "Vanilla", "Jump", "Vulcan"));
        m("LongJump", MOVEMENT, "move-up-right", "Dalekie skoki", "Long jumps", o("Tryb", "Boost", "Damage", "Glide"), n("Siła", 2, 1, 5, 0.1, "×"), b("Auto wyłącz", true));
        m("Sneak", MOVEMENT, "footprints", "Auto skradanie", "Auto sneak", o("Tryb", "Legit", "Packet"), b("Przy krawędzi", true));
        m("ElytraFly", MOVEMENT, "feather", "Lepsza elytra", "Better elytra", o("Tryb", "Vanilla", "Boost", "Bounce", "Control"), n("Boost", 1, 0.5, 3, 0.1, "×"), b("Auto start", true), b("Auto fajerwerki", false));
        m("Strafe", MOVEMENT, "move", "Pełna kontrola w powietrzu", "Full air control", n("Siła", 100, 0, 100, 5, "%"), b("Tylko z Speed", false));
        m("Glide", MOVEMENT, "wind", "Powolne opadanie", "Slow falling", n("Opadanie", 0.05, 0.01, 0.3, 0.01, ""), b("Tylko z sprintem", false));
        m("InvMove", MOVEMENT, "package-open", "Ruch w ekwipunku", "Move in inventories", b("Skok", true), b("Sneak", false), b("Rozglądanie strzałkami", true), b("W czacie", false));
        m("AutoWalk", MOVEMENT, "person-standing", "Idzie za Ciebie", "Walks for you", o("Tryb", "Forward", "Baritone"), b("Skacz przez przeszkody", true));
        m("SafeWalk", MOVEMENT, "shield-check", "Nie spadniesz z krawędzi", "Won't fall off edges", b("Tylko na krawędzi", true), b("W powietrzu", false));
        m("Parkour", MOVEMENT, "mountain", "Skok na krawędzi bloku", "Jumps at block edges", n("Margines", 0.05, 0.01, 0.3, 0.01, "bl"));
        m("NoJumpDelay", MOVEMENT, "step-forward", "Bez opóźnienia skoku", "No jump delay", n("Opóźnienie", 0, 0, 10, 1, "t"));
        // ================= Gracz =================
        m("AutoArmor", PLAYER, "shirt", "Zakłada najlepszą zbroję", "Equips best armor", n("Opóźnienie", 120, 0, 500, 10, "ms"), b("W ekwipunku", true), b("Elytra zamiast napierśnika", false), b("Zamień uszkodzoną", true));
        m("AutoEat", PLAYER, "apple", "Je gdy głodny", "Eats when hungry", n("Głód", 14, 1, 19, 1, ""), n("Zdrowie", 10, 1, 20, 1, "HP"), mu("Jedzenie", a("Złote jabłka", "Mięso", "Chleb", "Marchewki"), "Mięso", "Chleb"), b("Pauza KillAura", true));
        m("FastPlace", PLAYER, "boxes", "Szybsze stawianie", "Faster placing", n("Opóźnienie", 0, 0, 4, 1, "t"), b("Tylko bloki", true));
        Setting.Group imSort = g("Sortowanie");
        m("InvManager", PLAYER, "package", "Porządkuje ekwipunek", "Sorts inventory",
                g("Ogólne"), r("Opóźnienie", 80, 140, 0, 500, 10, "ms"), b("Tylko otwarty ekwipunek", true), b("Wyrzuć śmieci", true), b("Auto refill", false),
                imSort, n("Slot miecza", 1, 1, 9, 1, ""), n("Slot łuku", 2, 1, 9, 1, ""), n("Slot jabłek", 3, 1, 9, 1, ""), n("Slot bloków", 9, 1, 9, 1, ""), n("Max bloków", 128, 0, 512, 16, ""));
        m("AutoTool", PLAYER, "pickaxe", "Najlepsze narzędzie", "Best tool", b("Przełącz z powrotem", true), b("Pauza KillAura", true), b("Chroń uszkodzone", true));
        m("NoSlow", PLAYER, "snail", "Bez spowolnienia", "No slowdown", o("Tryb", "Vanilla", "NCP", "Grim", "Switch"), mu("Przedmioty", a("Jedzenie", "Łuki", "Tarcze", "Pajęczyna", "Miód"), "Jedzenie", "Łuki", "Tarcze"));
        m("ChestStealer", PLAYER, "archive", "Kradnie ze skrzyń", "Loots chests", r("Opóźnienie", 60, 120, 0, 300, 10, "ms"), b("Auto zamknij", true), b("Tylko przydatne", true), b("Ignoruj nazwane", true));
        m("AutoRespawn", PLAYER, "refresh-cw", "Odradza się od razu", "Instant respawn", n("Opóźnienie", 0, 0, 3000, 100, "ms"), b("Wiadomość o śmierci", true));
        m("Blink", PLAYER, "timer-off", "Wstrzymuje pakiety", "Holds packets", n("Max", 60, 10, 200, 10, "t"), b("Pokaż licznik", true), b("Pokaż ducha", true));
        m("FastUse", PLAYER, "hourglass", "Szybsze używanie", "Faster item use", n("Tempo", 2, 1, 5, 1, "×"), mu("Przedmioty", a("Jedzenie", "Mikstury", "Mleko"), "Jedzenie"));
        m("AutoFish", PLAYER, "fish", "Łowi ryby", "Fishes for you", r("Opóźnienie", 300, 600, 100, 2000, 50, "ms"), b("Zarzucaj ponownie", true), b("Naprawa Mending", false));
        m("MiddleClick", PLAYER, "mouse", "Akcja środkowego przycisku", "Middle click action", o("Akcja", "Friend", "Pearl", "Firework", "XP"), b("Wiadomość", true));
        m("NoRotate", PLAYER, "rotate-3d", "Ignoruje obroty serwera", "Ignores server rotations", b("Tylko w walce", false));
        m("Replenish", PLAYER, "refresh-ccw", "Uzupełnia pasek", "Refills the hotbar", n("Próg", 16, 1, 63, 1, ""), b("Bloki", true), b("Jedzenie", true));
        // ================= Wizualne =================
        Mode espMode = o("Tryb", "Box", "2D", "Outline", "Glow", "Shader");
        m("ESP", VISUALS, "scan", "Podświetla byty", "Highlights entities",
                g("Cele"), mu("Byty", a("Gracze", "Moby", "Zwierzęta", "Przedmioty", "Kryształy"), "Gracze"), b("Przez ściany", true), n("Zasięg", 128, 16, 512, 16, "bl"),
                g("Styl"), espMode, n("Grubość", 1.5, 0.5, 4, 0.5, "px").vis(() -> !espMode.is("Glow")), n("Wypełnienie", 20, 0, 100, 5, "%").vis(() -> espMode.is("Box") || espMode.is("2D")), b("Health bar", true), b("Zbroja", false),
                g("Kolory"), cl("Kolor", WHITE), cl("Kolor znajomych", GREEN), b("Kolor wg zdrowia", false), b("Tęcza", false));
        m("Fullbright", VISUALS, "sun", "Pełna jasność (tylko obraz)", "Full brightness (visual only)", o("Tryb", "Gamma", "Night vision"), n("Gamma", 100, 10, 100, 5, "%"), b("Płynne przejście", true));
        m("Nametags", VISUALS, "tag", "Lepsze nicki", "Better nametags",
                g("Info"), b("Zdrowie", true), b("Zbroja", true), b("Ping", false), b("Odległość", false), b("Przedmiot w ręce", true),
                g("Styl"), n("Skala", 1, 0.5, 2, 0.1, "×"), b("Skaluj z odległością", true), cl("Tło", 0xAA000000), cl("Tekst", WHITE), b("Obrys", false));
        m("Tracers", VISUALS, "route", "Linie do bytów", "Tracers to entities", targets("Gracze"), o("Start", "Crosshair", "Feet", "Bottom"), n("Grubość", 1.5, 0.5, 4, 0.5, "px"), cl("Kolor", SILVER), b("Zanikanie", true));
        Mode teCol = o("Kolor tryb", "Static", "Rainbow");
        m("TargetESP", VISUALS, "crosshair", "Świecący znacznik na przeciwniku, którego bijesz", "Glowing marker on the enemy you hit",
                g("Wygląd"), o("Styl", "Diamond", "Triad", "Shuriken", "Brackets", "Bold", "Soft", "Thin"), n("Rozmiar", 1, 0.5, 2.5, 0.05, "×"),
                n("Prędkość obrotu", 1, 0, 4, 0.1, "×"), n("Jasność", 80, 20, 100, 5, "%"), b("Pulsowanie", true), b("Poświata", true), b("Kontrast", true), b("Obrót ramki", true),
                g("Kolor"), teCol, cl("Kolor", WHITE).vis(() -> teCol.is("Static")), b("Czerwony przy trafieniu", true),
                g("Cel"), b("Też celowany", false), b("Tylko gracze", false), n("Zasięg", 12, 4, 32, 1, "m")).on();
        m("Chams", VISUALS, "ghost", "Prześwitujące modele", "See-through models", o("Styl", "Flat", "Shiny", "Texture", "Wireframe"), cl("Widoczne", WHITE), cl("Za ścianą", 0x88FFFFFF), b("Ręce", false));
        m("Zoom", VISUALS, "zoom-in", "Płynne przybliżenie pod klawiszem C (zmienisz w Sterowaniu)", "Smooth zoom on the C key (change it in Controls)",
                n("Siła", 4, 1.5, 15, 0.5, "×"), b("Płynnie", true), b("Scroll", true), b("Przełączanie", false), b("Niższa czułość", true), b("Kinowa kamera", false), b("Ukryj rękę", true), b("Wskaźnik", true)).on();
        m("NoHurtCam", VISUALS, "vibrate-off", "Kamera nie trzęsie się przy obrażeniach", "No camera shake when hurt", n("Siła", 0, 0, 100, 5, "%"));
        m("Freecam", VISUALS, "camera", "Swobodna kamera", "Free camera", n("Prędkość", 1, 0.1, 5, 0.1, ""), b("Przez bloki", true), b("Pokaż ciało", true));
        m("Trajectories", VISUALS, "spline", "Tor pocisków", "Projectile paths", mu("Pociski", a("Łuk", "Perła", "Trójząb", "Mikstury"), "Łuk", "Perła"), cl("Kolor", WHITE), b("Punkt trafienia", true));
        Mode boStyle = o("Styl", "Both", "Outline", "Fill");
        m("BlockOutline", VISUALS, "box", "Ładniejszy, płynny obrys bloku", "Nicer, smooth block outline", boStyle, n("Grubość", 2, 0.5, 5, 0.5, "px").vis(() -> !boStyle.is("Fill")),
                cl("Kolor", WHITE).vis(() -> !boStyle.is("Fill")), cl("Wypełnienie", 0x22FFFFFF).vis(() -> !boStyle.is("Outline")), b("Animacja", true), b("Tęcza", false));
        Mode bcCol = o("Kolor tryb", "Static", "Rainbow");
        m("Breadcrumbs", VISUALS, "waypoints", "Wstęga za Tobą (widać w F5)", "Ribbon trail behind you (see it in F5)", bcCol, cl("Kolor", WHITE).vis(() -> bcCol.is("Static")),
                n("Długość", 200, 20, 500, 10, ""), n("Grubość", 1.5, 0.5, 4, 0.5, "px"), b("Zanikanie", true));
        m("ItemPhysics", VISUALS, "atom", "Upuszczone przedmioty leżą płasko i kręcą się w locie", "Dropped items lie flat and spin in the air", n("Obrót", 1, 0, 3, 0.1, "×"), b("Leżą płasko", true));
        m("CustomFOV", VISUALS, "scan-eye", "Własne pole widzenia, bez skakania przy sprincie", "Custom field of view without sprint jumps", n("FOV", 100, 30, 140, 1, "°"), b("Stałe przy sprincie", true), b("Bez efektów", false));
        Bool chTarget = b("Kolor na celu", false);
        m("Crosshair", VISUALS, "circle-dot", "Własny celownik z hitmarkerem", "Custom crosshair with hitmarker",
                g("Kształt"), o("Styl", "Cross", "Dot", "Circle", "T", "Square", "Chevron"), n("Rozmiar", 4, 1, 12, 0.5, "px"), n("Przerwa", 2, 0, 8, 0.5, "px"), n("Grubość", 1, 0.5, 4, 0.25, "px"), b("Kropka", false),
                g("Wygląd"), cl("Kolor", WHITE), b("Obrys", true), b("Dynamiczny", true), chTarget, cl("Kolor celu", RED).vis(chTarget::get),
                g("Dodatki"), b("Hitmarker", true), b("Wskaźnik ataku", true));
        m("NoRender", VISUALS, "eye-off", "Ukrywa nakładki na ekranie", "Hides screen overlays", mu("Ukryj", a("Ogień", "Woda", "Mgła", "Bossbar", "Totem", "Pumpkin", "Winieta", "Portal", "Scoreboard", "Śnieg"), "Ogień", "Pumpkin", "Winieta"));
        m("Ambience", VISUALS, "cloud-sun", "Kolor nieba, mgły i chmur", "Sky, fog and cloud color",
                g("Niebo"), b("Kolor nieba", true), cl("Niebo", 0xFF9D8CFF), g("Mgła"), b("Kolor mgły", true), cl("Mgła", 0xFFE0C8FF),
                g("Chmury"), b("Kolor chmur", true), cl("Chmury", 0xFFF3E8FF), g("Inne"), b("Tęcza", false));
        Bool tcCycle = b("Cykl", false);
        m("TimeChanger", VISUALS, "clock", "Własna pora dnia (tylko u Ciebie)", "Custom time of day (only for you)", n("Godzina", 18, 0, 24, 0.5, "h"), tcCycle, n("Prędkość", 1, 0.1, 10, 0.1, "×").vis(tcCycle::get));
        Mode hcMode = o("Tryb", "Static", "Rainbow");
        m("HitColor", VISUALS, "droplet", "Kolor błysku przy obrażeniach", "Hurt flash color", hcMode, cl("Kolor", 0x66B4A0FF));
        m("ViewModel", VISUALS, "hand", "Pozycja rąk", "Hand position",
                g("Prawa"), n("X", 0, -1, 1, 0.05, ""), n("Y", 0, -1, 1, 0.05, ""), n("Z", 0, -1, 1, 0.05, ""),
                g("Lewa"), n("X lewa", 0, -1, 1, 0.05, ""), n("Y lewa", 0, -1, 1, 0.05, ""), n("Z lewa", 0, -1, 1, 0.05, ""),
                g("Inne"), n("Skala", 1, 0.3, 2, 0.05, "×"), b("Bez bujania", false));
        Mode dnStyle = o("Styl", "Pop", "Float", "Minimal");
        Bool dnAuto = b("Kolor wg obrażeń", true);
        m("DamageNumbers", VISUALS, "badge-minus", "Liczby obrażeń nad przeciwnikiem", "Damage numbers above enemies", dnStyle, n("Skala", 1, 0.5, 2, 0.05, "×"), dnAuto,
                cl("Kolor", WHITE).vis(() -> !dnAuto.get()), b("Tylko moje uderzenia", true), b("Cień", true));
        Mode atCol = o("Kolor tryb", "Styl", "Static", "Rainbow");
        m("Atmosphere", VISUALS, "snowflake", "Klimat wokół Ciebie: świetliki, śnieg, gwiazdy, sakura, iskry", "Ambient particles: fireflies, snow, stars, sakura, embers",
                o("Styl", "Fireflies", "Snow", "Stars", "Sakura", "Embers"), n("Ilość", 120, 10, 300, 10, ""), n("Zasięg", 16, 6, 40, 1, "bl"), n("Rozmiar", 1, 0.4, 3, 0.1, "×"),
                atCol, cl("Kolor", WHITE).vis(() -> atCol.is("Static")));
        // ================= Świat =================
        Mode scMode = o("Tryb", "Normal", "Telly", "Expand", "Godbridge", "Breezily");
        m("Scaffold", WORLD, "brick-wall", "Stawia bloki pod nogami", "Places blocks under you",
                g("Ogólne"), scMode, n("Rozszerzenie", 2, 1, 6, 1, "bl").vis(() -> scMode.is("Expand")), o("Rotacje", "Normal", "Snap", "Back"), b("Swing", true), r("Opóźnienie", 0, 1, 0, 5, 1, "t"),
                g("Wieża"), b("Tower", true), o("Tryb tower", "Vanilla", "NCP", "Motion"),
                g("Bloki"), b("Licznik bloków", true), b("Tylko pełne", true), b("Zmieniaj slot", true));
        m("Nuker", WORLD, "bomb", "Niszczy bloki wokół", "Breaks nearby blocks", n("Promień", 4, 1, 6, 1, "bl"), o("Tryb", "Flat", "Sphere", "Cube"), b("Tylko wybrany blok", false), b("Rotacje", true));
        m("FastBreak", WORLD, "hammer", "Szybsze kopanie", "Mine faster", o("Tryb", "Damage", "Instant", "Haste"), n("Prędkość", 30, 0, 100, 5, "%"));
        m("Timer", WORLD, "timer", "Tempo gry", "Game speed", n("Prędkość", 1, 0.1, 5, 0.05, "×"), b("Tylko w ruchu", false));
        m("Weather", WORLD, "cloud-rain", "Własna pogoda (tylko u Ciebie)", "Custom weather (only for you)", o("Tryb", "Clear", "Rain", "Thunder", "Snow"), n("Intensywność", 100, 0, 100, 5, "%"));
        m("XRay", WORLD, "gem", "Rudy przez bloki", "See ores", mu("Rudy", a("Diamenty", "Złoto", "Żelazo", "Netheryt", "Szmaragdy", "Redstone"), "Diamenty", "Złoto", "Netheryt"), n("Przezroczystość", 0, 0, 100, 5, "%"), b("Tylko odsłonięte", false));
        m("AutoFarm", WORLD, "wheat", "Automatyczna farma", "Auto farm", o("Tryb", "Crops", "Trees", "Sugar cane"), n("Zasięg", 4, 2, 6, 0.5, "m"), b("Sadź ponownie", true), b("Mączka kostna", false));
        m("AutoMine", WORLD, "shovel", "Kopie przed siebie", "Mines forward", o("Kierunek", "Forward", "Down", "Tunnel"), b("Zatrzymaj przy lawie", true));
        m("Tower", WORLD, "tower-control", "Szybkie budowanie w górę", "Pillars up fast", o("Tryb", "Vanilla", "Jump", "Motion"), n("Prędkość", 1, 0.5, 2, 0.1, "×"));
        m("AirPlace", WORLD, "square-dashed", "Stawianie w powietrzu", "Place in the air", n("Zasięg", 4, 1, 6, 0.5, "bl"), b("Podgląd", true));
        m("LiquidInteract", WORLD, "droplets", "Klikanie w ciecze", "Interact with liquids", b("Woda", true), b("Lawa", true));
        m("ChestESP", WORLD, "package-open", "Podświetla skrzynie", "Highlights chests", mu("Bloki", a("Skrzynie", "Ender", "Shulkery", "Beczki", "Spawnery"), "Skrzynie", "Ender", "Shulkery"), cl("Kolor", WHITE), n("Zasięg", 32, 8, 96, 4, "bl"));
        // ================= HUD =================
        m("Watermark", HUD, "badge", "Pasek ze statystykami (FPS, ping, serwer, TPS, nick)", "Stats bar (FPS, ping, server, TPS, name)", o("Styl", "Capsule", "Stats", "Bar", "Pill", "Logo", "Text"),
                mu("Info", a("FPS", "Ping", "Serwer", "TPS", "Nick", "Czas"), "Ping", "Serwer", "TPS", "Nick", "Czas"), b("Logo", true), tx("Tekst", "WithClient", 20), b("Tło", true)).on();
        m("ArrayList", HUD, "list", "Lista włączonych modułów", "Enabled module list", o("Styl", "Embed", "Pills", "Classic", "Text"), o("Sortowanie", "Length", "Alphabet"),
                o("Animacja", "Slide", "Fade", "None"), b("Grupuj kategorie", true), b("Nagłówek", true), b("Ikony", true), b("Sufiks trybu", true), b("Tło", true), b("Pasek boczny", true), b("Ukryj wizualne", false), cl("Kolor", WHITE)).on();
        m("Keybinds", HUD, "keyboard", "Lista przypisanych klawiszy", "Bound keys list", o("Styl", "Pills", "Embed"), b("Tylko włączone", false), b("Nagłówek", true), n("Max", 10, 3, 20, 1, "")).on();
        m("TargetHUD", HUD, "contact", "Info o przeciwniku, którego bijesz", "Info about the enemy you hit", o("Styl", "Modern", "Model", "Compact", "Minimal"),
                b("Głowa", true), b("Zbroja", true), b("Animacja zdrowia", true), b("Cząsteczki", true), n("Zasięg", 8, 3, 20, 1, "m")).on();
        m("Coordinates", HUD, "map-pin", "Współrzędne", "Coordinates", o("Styl", "Pill", "Panel"), b("Nether", false), b("Kierunek", true), b("Biom", false));
        m("FPS", HUD, "activity", "Licznik FPS", "FPS counter", b("Wykres", false), b("Min/max", false));
        m("Ping", HUD, "signal", "Opóźnienie do serwera", "Server latency", b("Paski", true), b("Kolor wg wartości", false));
        m("TPS", HUD, "hourglass", "Wydajność serwera (szacowana)", "Server TPS (estimated)", b("Wykres", false));
        m("ArmorHUD", HUD, "shield-half", "Stan zbroi i przedmiotu w ręce", "Armor and held item status", o("Układ", "Horizontal", "Vertical"), b("Procenty", false), b("Przedmiot w ręce", false));
        m("Potions", HUD, "flask-round", "Aktywne efekty", "Active effects", b("Ikony", true), b("Pasek czasu", true), o("Sortowanie", "Time", "Name"));
        m("InventoryHUD", HUD, "grid-3x3", "Podgląd ekwipunku", "Inventory preview", b("Tło", true));
        m("Notifications", HUD, "bell", "Powiadomienia o przełączeniu modułów", "Module toggle notifications", o("Styl", "Pill", "Card"), n("Czas", 2, 1, 6, 0.5, "s"),
                o("Pozycja", "Bottom right", "Top right", "Bottom"), b("Przełączanie modułów", true), b("Dźwięk", false)).on();
        m("Speedometer", HUD, "circle-gauge", "Prędkość gracza", "Player speed", o("Jednostka", "bps", "km/h"), b("Wykres", false));
        m("Clock", HUD, "clock", "Zegar", "Clock", o("Format", "24h", "12h"), b("Sekundy", false), b("Data", false));
        m("ServerInfo", HUD, "server", "Adres serwera i gracze online", "Server address and players online", b("Gracze online", true));
        m("CPS", HUD, "mouse-pointer-click", "Kliknięcia na sekundę", "Clicks per second");
        m("Keystrokes", HUD, "keyboard-music", "Podgląd wciskanych klawiszy", "Pressed keys overlay", n("Rozmiar", 18, 12, 28, 1, "px"), b("Mysz", true), b("Spacja", true), b("CPS", true));
        m("HealthBar", HUD, "heart", "Pasek zdrowia nad paskiem przedmiotów", "Health bar above the hotbar", o("Styl", "Split", "Bar", "Minimal"), b("Ukryj serca Minecrafta", true), b("Tekst", true), b("Absorpcja", true), b("Głód", false));
        m("Session", HUD, "timer", "Czas gry, dystans, śmierci", "Playtime, distance, deaths", b("Czas gry", true), b("Dystans", true), b("Śmierci", true));
        m("Memory", HUD, "memory-stick", "Użycie pamięci RAM", "RAM usage");
        m("TotemCounter", HUD, "shield-plus", "Liczba totemów", "Totem count", b("Ukryj przy 0", false));
        m("Spotify", HUD, "music", "Co teraz gra: okładka, tytuł, sterowanie, tekst (przyciski działają przy otwartym czacie)", "Now playing: cover, title, controls, lyrics (buttons work with chat open)",
                o("Styl", "Vinyl", "Card", "Compact"), b("Okładka", true), b("Przyciski", true), b("Pasek postępu", true), b("Tekst piosenki", true),
                b("Napisy na ekranie", false), b("Ukryj gdy nic nie gra", true));
        m("Hotbar", HUD, "panel-bottom", "Własny pasek przedmiotów", "Custom hotbar", o("Styl", "Panel", "Clean"), b("Animacja", true), b("Paski wytrzymałości", true), b("Numery slotów", false));
        m("Compass", HUD, "compass", "Kompas", "Compass", o("Styl", "Bar", "Circle"), b("Stopnie", true));
        // ================= Kosmetyki =================
        m("Cape", COSMETICS, "flag", "Peleryna WithClient (też na elytrze)", "WithClient cape (also on elytra)", o("Peleryna", "WithClient", "Mono", "Stripes", "Minimal", "Aurora", "Galaxy")).on();
        m("Wings", COSMETICS, "bird", "Skrzydła na plecach (F5)", "Wings on your back (F5)", o("Styl", "Angel", "Dragon", "Mono"), cl("Kolor", WHITE), b("Tęcza", false), n("Skala", 1, 0.5, 1.5, 0.05, "×"), b("Animacja", true));
        m("Halo", COSMETICS, "circle-dashed", "Świecąca aureola nad głową (F5)", "Glowing halo above your head (F5)", cl("Kolor", WHITE), b("Tęcza", false), n("Wysokość", 0.3, 0, 1, 0.05, "bl"), n("Rozmiar", 1, 0.5, 2, 0.05, "×"), b("Poświata", true));
        m("Hat", COSMETICS, "crown", "Czapka: China hat, korona, cylinder (F5)", "Hat: China hat, crown, top hat (F5)", o("Styl", "China hat", "Crown", "Top hat"), cl("Kolor", WHITE), b("Tęcza", true), n("Rozmiar", 1, 0.6, 1.6, 0.05, "×"), b("Obrót", true));
        Mode trCol = o("Kolor tryb", "Static", "Rainbow");
        Mode trType = o("Cząsteczki", "Sparkle", "Snow", "Hearts", "Dust", "Flame", "Soul", "Totem", "Cherry", "Glow", "Notes");
        m("Trails", COSMETICS, "sparkles", "Cząsteczki za Tobą", "Particle trail behind you", trType, n("Ilość", 3, 1, 10, 1, ""),
                trCol.vis(() -> trType.is("Dust")), cl("Kolor", WHITE).vis(() -> trType.is("Dust") && trCol.is("Static")), b("Tylko w ruchu", true));
        Mode jcCol = o("Kolor tryb", "Static", "Rainbow");
        m("JumpCircles", COSMETICS, "orbit", "Kręgi pod nogami przy skoku", "Circles under your feet when jumping", o("Styl", "Ring", "Glow", "Soft", "Swirl"),
                n("Rozmiar", 1, 0.5, 2.5, 0.05, "×"), n("Czas", 1, 0.4, 3, 0.1, "s"), jcCol, cl("Kolor", WHITE).vis(() -> jcCol.is("Static")), b("Obrót", false)).on();
        Mode swMode = o("Tryb", "Static", "Gradient", "Rainbow");
        m("SwordColor", COSMETICS, "paintbrush", "Własny kolor mieczy", "Custom sword color", swMode, cl("Kolor", 0xFF7DA7FF).vis(() -> !swMode.is("Rainbow")),
                cl("Kolor 2", WHITE).vis(() -> swMode.is("Gradient")), b("W ręce", true), b("Na pasku", true), b("Upuszczone", true), b("Topory", false));
        Mode hpCol = o("Kolor tryb", "Static", "Rainbow");
        Mode hpType = o("Typ", "Stars", "Hearts", "Crits", "Magic", "Blood", "Flame", "Soul", "Totem", "Glow", "Snow", "Cherry", "Notes", "Dust");
        m("HitParticles", COSMETICS, "party-popper", "Cząsteczki przy uderzeniu", "Particles on hit", hpType, n("Ilość", 6, 1, 30, 1, ""), b("Fizyka", true),
                hpCol.vis(() -> hpType.is("Dust")), cl("Kolor", WHITE).vis(() -> hpType.is("Dust") && hpCol.is("Static")));
        m("CustomSky", COSMETICS, "moon-star", "Własne niebo: galaktyka, zorza, zachód słońca", "Custom sky: galaxy, aurora, sunset", o("Niebo", "Galaxy", "Aurora", "Night", "Sunset", "Mono"), b("Animacja", true), b("Chmury", false));
        m("SwingAnimation", COSMETICS, "sword", "Animacja machnięcia mieczem", "Sword swing animation", o("Styl", "1.7", "Smooth", "Spin", "Slide", "Push", "Chop"), n("Prędkość", 1, 0.2, 3, 0.1, "×"), b("Zawsze", false));
        Mode igMode = o("Tryb", "Static", "Rainbow");
        m("ItemGlint", COSMETICS, "sparkle", "Kolor i prędkość połysku zaklęć", "Enchant glint color and speed", igMode, cl("Kolor", WHITE), n("Prędkość", 1, 0.1, 5, 0.1, "×"));
        Mode ncMode = o("Tryb", "Gradient", "Static", "Rainbow");
        m("NameColor", COSMETICS, "brush", "Kolor Twojego nicku na liście graczy (TAB)", "Your name color in the player list (TAB)", ncMode,
                cl("Kolor", WHITE).vis(() -> !ncMode.is("Rainbow")), cl("Kolor 2", SILVER).vis(() -> ncMode.is("Gradient")), b("Animacja", true), b("Pogrubienie", false));
        m("KillEffect", COSMETICS, "cloud-lightning", "Efekt przy zabiciu: piorun, dusze, wybuch, fajerwerki", "Kill effect: lightning, souls, explosion, fireworks",
                o("Efekt", "Lightning", "Souls", "Explosion", "Blood", "Firework", "Totem"), b("Dźwięk", true), b("Tylko gracze", false));
        m("HitSound", COSMETICS, "bell-ring", "Dźwięk przy uderzeniu", "Sound on hit", o("Dźwięk", "Ding", "Pop", "Crit", "Bell", "Click", "Bass", "Chime", "Pling"),
                n("Głośność", 80, 0, 100, 5, "%"), n("Wysokość", 1, 0.5, 2, 0.05, ""), b("Losowa wysokość", true));
        // ================= Narzędzia =================
        m("AutoGG", UTILITY, "message-circle", "Pisze GG po grze", "Says GG after a game", tx("Wiadomość", "gg", 32), n("Opóźnienie", 1, 0, 5, 0.5, "s"), b("Tylko wygrana", false));
        m("NameProtect", UTILITY, "venetian-mask", "Ukrywa nick", "Hides your name", tx("Nazwa", "You", 16), b("Znajomi", false), b("Tab", true));
        m("AntiAFK", UTILITY, "coffee", "Nie wyrzuci za AFK", "Avoids AFK kicks", n("Co ile", 30, 5, 120, 5, "s"), mu("Akcje", a("Skok", "Obrót", "Machnięcie", "Ruch"), "Skok", "Obrót"));
        m("AutoReconnect", UTILITY, "plug-zap", "Ponowne łączenie", "Reconnects automatically", n("Opóźnienie", 5, 1, 30, 1, "s"), n("Próby", 3, 1, 20, 1, ""));
        m("Spammer", UTILITY, "megaphone", "Wysyła wiadomości", "Sends messages", tx("Wiadomość", "WithClient", 64), n("Co ile", 10, 1, 120, 1, "s"), b("Losowy suffix", true));
        m("ChatTweaks", UTILITY, "messages-square", "Lepszy czat", "Better chat", b("Czas wiadomości", true), b("Bez limitu historii", true), b("Animacja", true), b("Kopiuj PPM", true), b("Ukryj spam", false));
        m("AutoLog", UTILITY, "log-out", "Wychodzi przy niskim HP", "Leaves on low HP", n("Zdrowie", 6, 1, 20, 1, "HP"), b("Tylko bez totemu", true), b("Gracz w pobliżu", false));
        m("FakePlayer", UTILITY, "user-plus", "Atrapa gracza (lokalnie)", "Local dummy player", tx("Nick", "WithBot", 16), b("Kopiuj ekwipunek", true));
        m("DeathCoords", UTILITY, "skull", "Zapisuje miejsce śmierci", "Saves death location", b("Wiadomość", true), b("Waypoint", true));
        m("Translator", UTILITY, "languages", "Tłumaczy czat", "Translates chat", o("Na język", "Polski", "English", "Deutsch"), b("Wysyłane", false));
        m("StreamerMode", UTILITY, "cast", "Tryb streamera", "Streamer mode", b("Ukryj IP", true), b("Ukryj nicki", false), b("Ukryj koordy", true));
        m("Screenshot", UTILITY, "image", "Lepsze zrzuty ekranu", "Better screenshots", b("Kopiuj do schowka", true), b("Bez HUD", false));
        // ================= Exploity =================
        m("AntiAFKKick", EXPLOITS, "coffee", "Omija limit AFK", "Bypasses AFK limit", n("Co ile", 60, 10, 300, 10, "s"));
        m("PacketMine", EXPLOITS, "pickaxe", "Kopanie pakietami", "Packet mining", b("Auto reset", true), cl("Kolor", WHITE), b("Pokaż postęp", true));
        m("Disabler", EXPLOITS, "shield-off", "Omija anticheat", "Bypasses anticheat", o("Tryb", "Grim", "Vulcan", "NCP", "Matrix"));
        m("NoWeb", EXPLOITS, "grid-3x3", "Ignoruje pajęczynę", "Ignores cobwebs", o("Tryb", "Vanilla", "Grim"));
        m("PortalGUI", EXPLOITS, "door-open", "GUI w portalu", "GUIs inside portals");
        m("AntiHunger", EXPLOITS, "utensils", "Wolniejszy głód", "Slower hunger", b("Sprint", true), b("Skok", true));
        m("AntiVoid", EXPLOITS, "shield-alert", "Ratuje przed voidem", "Saves you from the void", o("Tryb", "Flag", "Bounce", "Blink"), n("Wysokość", 5, 1, 20, 1, "bl"));
        m("PingSpoof", EXPLOITS, "wifi", "Fałszywy ping", "Fake ping", n("Ping", 200, 0, 1000, 10, "ms"));
        m("ClientSpoof", EXPLOITS, "fingerprint", "Udaje innego klienta", "Spoofs client brand", o("Marka", "Vanilla", "Forge", "Lunar", "Badlion"));
        m("Phase", EXPLOITS, "scan-line", "Przechodzenie przez bloki", "Walk through blocks", o("Tryb", "Vanilla", "Clip", "Packet"), n("Odległość", 1, 0.1, 3, 0.1, "bl"));
        // ================= Klient =================
        m("ClickGUI", CLIENT, "app-window", "Interfejs klienta", "Client interface", b("Rozmycie tła", true), b("Dźwięki", true), b("Pamiętaj stronę", true));
        m("HUDEditor", CLIENT, "layout-template", "Edytor i wygląd HUD (kliknij, żeby edytować)", "HUD editor and style (click to edit)",
                g("Wygląd"), o("Tło", "Dark", "Glass", "Clear"), n("Skala", 1, 0.5, 2, 0.05, "×"), n("Przezroczystość", 90, 20, 100, 5, "%"),
                n("Zaokrąglenie", 6, 0, 12, 1, "px"), b("Obrys", true), b("Cień", true), b("Ikony", true), cl("Akcent", WHITE),
                g("Edytor"), b("Siatka", true), b("Przyciąganie", true), n("Rozmiar siatki", 8, 2, 32, 1, "px"));
        m("DiscordRPC", CLIENT, "gamepad-2", "Status w Discordzie: WithClient z logo, wersją i serwerem", "Discord status: WithClient with logo, version and server",
                tx("ID aplikacji", "", 24), o("Duży obraz", "Logo", "Skin"), b("Pokaż serwer", true), b("Pokaż nick", false), b("Pokaż czas", true), tx("Opis", "", 40)).on();
        m("Sounds", CLIENT, "volume-2", "Dźwięki klienta", "Client sounds", n("Głośność", 60, 0, 100, 5, "%"), o("Paczka", "Soft", "Click", "Pop"), b("Przełączanie modułów", true));
        m("Performance", CLIENT, "cpu", "Optymalizacje", "Optimizations", b("Ogranicz FPS w menu", true), b("Szybkie czcionki", false), n("Limit FPS w tle", 30, 5, 120, 5, ""));
        m("Panic", CLIENT, "octagon-x", "Wyłącza wszystko", "Disables everything", b("Ukryj HUD", true), b("Wyczyść czat", false));
        m("Updater", CLIENT, "cloud-download", "Sprawdza aktualizacje", "Checks for updates", b("Przy starcie", true), o("Kanał", "Stable", "Beta"));

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
            if (down && held.add(m.key)) {
                if (m.name.equals("HUDEditor")) { mc.setScreen(new dev.with.client.hud.HudEditor(null)); continue; }
                m.toggle(); save();
                dev.with.client.hud.Hud.notify(m);
            }
            else if (!down) held.remove(m.key);
        }
    }

    // ------------------------------------------------------------------ zapis

    public static void save() { saveTo(FILE); }

    private static void saveTo(Path file) {
        JsonObject root = new JsonObject();
        JsonObject meta = new JsonObject();
        meta.addProperty("v", CONFIG_VERSION);
        root.add("_meta", meta);
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
            int v = root.has("_meta") ? root.getAsJsonObject("_meta").get("v").getAsInt() : 1;
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
            // 1.3: nowy wygląd HUD (Stats, pigułki) także dla starych ustawień
            if (v < 3) {
                for (String n : new String[]{"Watermark", "ArrayList", "Keybinds", "Coordinates", "Notifications", "TargetHUD"}) {
                    Module m = get(n);
                    if (m != null && m.setting("Styl") != null) m.setting("Styl").reset();
                }
                Module w = get("Watermark");
                if (w != null && w.setting("Info") != null) w.setting("Info").reset();
            }
            // 1.3: stary domyślny opis zasłaniał miejsce gry w statusie Discorda
            // 1.5: wygląd z nagrania (kapsuła, pigułki, winyl, paski zdrowia i głodu, panel hotbara)
            if (v < 6) {
                for (String n : new String[]{"Watermark", "Keybinds", "Spotify", "HealthBar", "Hotbar"}) {
                    Module m = get(n);
                    if (m != null && m.setting("Styl") != null) m.setting("Styl").reset();
                }
                Module w = get("Watermark");
                if (w != null) { if (w.setting("Info") != null) w.setting("Info").reset(); if (w.setting("Logo") != null) w.setting("Logo").reset(); }
                Module ar = get("ArmorHUD");
                if (ar != null) for (String n : new String[]{"Procenty", "Przedmiot w ręce"}) if (ar.setting(n) != null) ar.setting(n).reset();
            }
            // 1.4: karty (embed) dla listy modułów i klawiszy
            if (v < 5) {
                for (String n : new String[]{"ArrayList", "Keybinds"}) {
                    Module m = get(n);
                    if (m != null && m.setting("Styl") != null) m.setting("Styl").reset();
                }
            }
            // 1.6: zoom pod C i peleryna działają od razu
            if (v < 7) {
                for (String n : new String[]{"Zoom", "Cape"}) { Module m = get(n); if (m != null) m.setEnabled(true); }
            }
            if (v < 4) {
                Module d = get("DiscordRPC");
                if (d != null && d.setting("Opis") != null) d.setting("Opis").reset();
                if (d != null) d.setEnabled(true); // w 1.2 był tylko atrapą; bez ID aplikacji i tak nic nie wysyła
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
