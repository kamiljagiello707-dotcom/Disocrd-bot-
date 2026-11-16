package dev.razer.client.module;

import dev.razer.client.setting.BoolSetting;
import dev.razer.client.setting.ModeSetting;
import dev.razer.client.setting.NumberSetting;
import dev.razer.client.setting.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista modułów. Działają moduły kosmetyczne i legit (Render/World/Misc oraz Sprint, AutoRespawn) –
 * logika w {@link Legit} i podklasach. Moduły typu cheat (Combat, Movement poza Sprint, ESP, Nuker...)
 * są wyłącznie przełącznikami z ustawieniami w GUI, bez żadnej logiki w grze.
 */
public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    // krótkie fabryki ustawień (nazwy po angielsku)
    private static BoolSetting b(String name, boolean v) { return new BoolSetting(name, name, v); }
    private static NumberSetting n(String name, double v, double min, double max, double step) { return new NumberSetting(name, name, v, min, max, step); }
    private static ModeSetting m(String name, String... modes) { return new ModeSetting(name, name, modes); }

    private void mod(String name, Category c, String icon, String pl, String en, Setting<?>... s) {
        modules.add(new Module(name, c, icon, pl, en).add(s));
    }

    public ModuleManager() {
        // ======================================================== COMBAT
        mod("KillAura", Category.COMBAT, "target", "Atakuje cele w zasięgu", "Attacks targets in range",
                m("Mode", "Single", "Switch", "Multi"),
                m("Rotations", "Silent", "Legit", "Snap", "None"),
                m("Priority", "Distance", "Health", "Angle", "Armor", "Hurt Time"),
                n("Range", 3.5, 3.0, 6.0, 0.05),
                n("Search Range", 5.0, 3.0, 8.0, 0.1),
                n("Rotation Speed", 180, 10, 360, 5),
                n("FOV", 180, 30, 360, 5),
                n("Min CPS", 9, 1, 20, 1),
                n("Max CPS", 13, 1, 20, 1),
                n("Switch Delay", 250, 0, 1000, 10),
                b("Rage Mode", false),
                b("Through Walls", false),
                b("Raytrace", true),
                b("Keep Sprint", true),
                m("Swing", "Client", "Server", "None"),
                m("Auto Block", "Off", "Fake", "Real"),
                b("Players", true), b("Mobs", false), b("Animals", false), b("Invisibles", false),
                b("Ignore Friends", true), b("Only Weapon", false), b("Pause On Eat", true));
        mod("Velocity", Category.COMBAT, "shield", "Zmniejsza odrzut", "Reduces knockback",
                m("Mode", "Cancel", "Percent", "Reverse", "Jump Reset"),
                n("Horizontal", 0, 0, 100, 1), n("Vertical", 0, 0, 100, 1),
                n("Chance", 100, 0, 100, 1), b("Only When Targeting", false), b("Explosions", true), b("Pause In Water", true));
        mod("Criticals", Category.COMBAT, "bolt", "Uderzenia krytyczne", "Critical hits",
                m("Mode", "Packet", "Jump", "Mini Jump", "Strict"),
                n("Hurt Time", 10, 0, 10, 1), b("Only KillAura", false), b("Only Weapon", true), b("Ignore In Air", true));
        mod("AutoTotem", Category.COMBAT, "heart", "Totem w drugiej ręce", "Totem in offhand",
                m("Mode", "Always", "Health", "Smart"),
                n("Health", 10, 1, 20, 0.5), n("Delay", 0, 0, 10, 1),
                b("Open Inventory", false), b("Swap Back", true), b("Elytra Check", true));
        mod("TriggerBot", Category.COMBAT, "click", "Atak po najechaniu", "Attacks what's under the crosshair",
                n("Min CPS", 8, 1, 20, 1), n("Max CPS", 12, 1, 20, 1), n("Delay", 50, 0, 500, 5),
                m("Timing", "CPS", "Cooldown", "1.8"), b("Players", true), b("Mobs", false), b("Only Weapon", false), b("Ignore Friends", true));
        mod("AimAssist", Category.COMBAT, "i_scaneye", "Delikatnie pomaga celować", "Gently helps your aim",
                n("Speed", 4.0, 0.5, 15.0, 0.5), n("FOV", 90, 10, 360, 5), n("Range", 4.5, 3.0, 8.0, 0.1),
                m("Target Part", "Head", "Body", "Legs", "Closest"),
                b("Only On Click", true), b("Vertical", false), b("Through Walls", false), b("Ignore Friends", true));
        mod("AutoClicker", Category.COMBAT, "i_hand", "Automatyczne klikanie", "Automatic clicking",
                n("Min CPS", 10, 1, 25, 1), n("Max CPS", 14, 1, 25, 1),
                m("Pattern", "Random", "Butterfly", "Jitter", "Drag"),
                b("Left Click", true), b("Right Click", false), b("Break Blocks", true), b("Only Weapon", false), b("Inventory", false));
        mod("Reach", Category.COMBAT, "i_ruler", "Większy zasięg", "Hit from further away",
                n("Combat Range", 3.6, 3.0, 6.0, 0.05), n("Block Range", 4.5, 4.5, 7.0, 0.1),
                n("Chance", 100, 0, 100, 1), b("Only Sprinting", false), b("Disable In Water", true));
        mod("WTap", Category.COMBAT, "footprints", "Resetuje sprint po uderzeniu", "Resets sprint after each hit",
                m("Mode", "Packet", "Legit", "SneakTap"), n("Delay", 2, 0, 10, 1), n("Duration", 1, 1, 5, 1),
                n("Chance", 100, 0, 100, 1), b("Only Players", true));
        mod("HitBoxes", Category.COMBAT, "box", "Większe hitboxy celów", "Bigger target hitboxes",
                n("Expand", 0.2, 0.0, 1.0, 0.05), b("Players", true), b("Mobs", false), b("Render", false), b("Ignore Friends", true));

        // ======================================================== MOVEMENT
        mod("Sprint", Category.MOVEMENT, "footprints", "Automatyczny sprint", "Automatic sprint",
                b("Only Forward", true), b("Stop On Sneak", true));
        mod("Speed", Category.MOVEMENT, "gauge", "Szybsze poruszanie", "Move faster",
                m("Mode", "Strafe", "Vanilla", "Ground", "BHop", "Low Hop"),
                n("Speed", 1.2, 1.0, 3.0, 0.05), n("Timer", 1.0, 1.0, 2.0, 0.05), n("Jump Height", 0.42, 0.2, 0.6, 0.01),
                b("Auto Jump", true), b("Damage Boost", false), b("Liquid Check", true), b("Pause On Sneak", true));
        mod("Flight", Category.MOVEMENT, "plane", "Latanie", "Fly",
                m("Mode", "Vanilla", "Creative", "Glide", "Motion"),
                n("Horizontal Speed", 1.0, 0.1, 5.0, 0.1), n("Vertical Speed", 0.6, 0.1, 3.0, 0.1), n("Glide Speed", 0.03, 0.0, 0.2, 0.01),
                b("Anti Kick", true), b("Stop On Disable", true), b("View Bobbing", false));
        mod("NoFall", Category.MOVEMENT, "nofall", "Brak obrażeń od upadku", "No fall damage",
                m("Mode", "Packet", "Ground", "Bucket", "Elytra"), n("Min Fall", 3, 2, 20, 1), b("Only Damage", true), b("Pause In Creative", true));
        mod("Step", Category.MOVEMENT, "step", "Wchodzenie na bloki", "Step up blocks",
                m("Mode", "Vanilla", "Packet", "Jump"), n("Height", 1.0, 0.5, 2.5, 0.5), n("Delay", 0, 0, 10, 1), b("Entity Step", false));
        mod("Spider", Category.MOVEMENT, "i_bug", "Wspinanie po ścianach", "Climb walls",
                m("Mode", "Vanilla", "Jump", "Motion"), n("Speed", 0.3, 0.1, 1.0, 0.05), b("Only Sneak", false), b("Liquids", false));
        mod("LongJump", Category.MOVEMENT, "i_jump", "Daleki skok", "Jump further",
                m("Mode", "Boost", "Glide", "Damage"), n("Boost", 2.0, 1.0, 6.0, 0.1), n("Height", 0.42, 0.2, 1.0, 0.02),
                b("Auto Disable", true), b("Auto Jump", true));
        mod("Jesus", Category.MOVEMENT, "i_waves", "Chodzenie po wodzie", "Walk on water",
                m("Mode", "Solid", "Dolphin", "Bounce"), n("Speed", 1.0, 0.5, 2.0, 0.05), b("Lava", true), b("Pause On Sneak", true));
        mod("ElytraBoost", Category.MOVEMENT, "i_rocket", "Szybszy lot na elytrze", "Faster elytra flight",
                m("Mode", "Control", "Boost", "Packet"), n("Speed", 1.6, 0.5, 4.0, 0.1), n("Vertical Speed", 0.8, 0.1, 3.0, 0.1),
                b("Auto Takeoff", true), b("Stop In Water", true), b("Firework Boost", false));
        mod("AirJump", Category.MOVEMENT, "i_up", "Skok w powietrzu", "Jump in mid-air",
                m("Mode", "Infinite", "Double", "Triple"), n("Height", 0.42, 0.2, 1.0, 0.02), b("Only On Key", true), b("Reset On Ground", true));

        // ======================================================== RENDER
        mod("ESP", Category.VISUALS, "scan", "Obrysy graczy", "Player outlines",
                m("Mode", "Box", "2D", "Outline", "Glow", "Shader"),
                n("Line Width", 1.5, 0.5, 5.0, 0.5), n("Fill Opacity", 25, 0, 100, 5), n("Range", 128, 16, 512, 16),
                b("Players", true), b("Mobs", false), b("Animals", false), b("Items", false), b("Health Bar", true),
                b("Friends Color", true), b("Self", false));
        mod("Fullbright", Category.VISUALS, "sun", "Pełna jasność", "Full brightness",
                m("Mode", "Gamma", "Night Vision"), n("Gamma", 100, 0, 100, 5), b("Fade", true));
        mod("Tracers", Category.VISUALS, "radar", "Linie do graczy", "Lines to players",
                m("Origin", "Crosshair", "Feet", "Top"), n("Width", 1.0, 0.5, 4.0, 0.5), n("Opacity", 80, 10, 100, 5),
                b("Players", true), b("Mobs", false), b("Distance Color", true), b("Ignore Friends", false));
        modules.add(new Nametags());
        mod("ChestESP", Category.VISUALS, "package", "Podświetlanie skrzyń", "Highlights chests",
                m("Mode", "Box", "Outline", "Glow"), n("Range", 64, 16, 256, 8), b("Chests", true), b("Ender Chests", true),
                b("Shulkers", true), b("Barrels", false), b("Spawners", false), b("Tracers", false));
        modules.add(new TargetEsp());
        modules.add(new Ambient());
        modules.add(new ChinaHat());
        modules.add(new JumpCircles());
        modules.add(new Sky());
        modules.add(new ToolColor());
        mod("Zoom", Category.VISUALS, "i_zoom", "Przybliżenie widoku", "Zoom in your view",
                n("Zoom", 4.0, 1.5, 10.0, 0.5), b("Smooth", true), b("Scroll Zoom", true), b("Cinematic Camera", false), b("Hide Hand", true));
        mod("FreeLook", Category.VISUALS, "render", "Rozglądanie bez obracania postaci", "Look around without turning",
                m("Mode", "Hold", "Toggle"), n("Sensitivity", 1.0, 0.1, 3.0, 0.1), b("Invert Pitch", false), b("Third Person", true));
        mod("NoHurtCam", Category.VISUALS, "i_camera", "Brak trzęsienia kamery", "No camera shake on damage",
                n("Strength", 0, 0, 100, 5), b("No Bobbing", false), b("No Totem Animation", false));
        mod("BlockOutline", Category.VISUALS, "i_outline", "Własny obrys bloku", "Custom block outline",
                m("Mode", "Outline", "Fill", "Both"), n("Width", 2.0, 0.5, 5.0, 0.5), n("Fill Opacity", 20, 0, 100, 5),
                b("Accent Color", true), b("Animated", true));
        mod("NoFire", Category.VISUALS, "i_flame", "Mniejszy ogień na ekranie", "Lower fire overlay",
                n("Height", 0.3, 0.0, 1.0, 0.05), n("Opacity", 50, 0, 100, 5), b("Hide In Water", true));

        // ======================================================== PLAYER
        mod("AutoArmor", Category.PLAYER, "shield", "Zakłada najlepszą zbroję", "Equips the best armor",
                n("Delay", 2, 0, 10, 1), m("Priority", "Protection", "Blast", "Projectile"),
                b("Open Inventory", false), b("Elytra Priority", false), b("Ignore Curse", true));
        mod("FastPlace", Category.PLAYER, "blocks", "Szybkie stawianie", "Place blocks faster",
                n("Delay", 0, 0, 4, 1), b("Blocks", true), b("Projectiles", false), b("XP Bottles", true), b("Crystals", false));
        mod("NoSlow", Category.PLAYER, "snail", "Brak spowolnienia", "No slowdown",
                m("Mode", "Vanilla", "Packet", "Strict"), b("Food", true), b("Bow", true), b("Shield", true),
                b("Soul Sand", false), b("Cobwebs", false), b("Sneak", false));
        mod("InvManager", Category.PLAYER, "backpack", "Porządkuje ekwipunek", "Sorts your inventory",
                n("Delay", 2, 0, 10, 1), m("Sort", "Smart", "Type", "Name"), b("Drop Junk", true), b("Keep Tools", true),
                b("Only In Inventory", false), b("Hotbar Layout", true));
        mod("AutoEat", Category.PLAYER, "i_eat", "Automatyczne jedzenie", "Eats automatically",
                n("Hunger", 14, 1, 19, 1), n("Health", 10, 1, 20, 0.5), m("Food", "Best", "Golden Apple", "Any"),
                b("Pause KillAura", true), b("Swap Back", true));
        mod("AutoTool", Category.PLAYER, "i_wrench", "Wybiera najlepsze narzędzie", "Picks the best tool",
                b("Swap Back", true), b("Weapons", true), b("Silk Touch", false), b("Anti Break", true), n("Min Durability", 10, 0, 100, 5));
        mod("ChestStealer", Category.PLAYER, "i_archive", "Szybko wyjmuje przedmioty", "Quickly takes items",
                n("Delay", 80, 0, 500, 10), b("Random Delay", true), b("Auto Close", true), b("Only Useful", true), b("Ignore Custom Names", true));
        mod("FastUse", Category.PLAYER, "potion", "Szybsze używanie przedmiotów", "Use items faster",
                n("Speed", 2, 1, 10, 1), b("Food", true), b("Potions", true), b("Bows", false), b("Tridents", false));
        mod("AutoRespawn", Category.PLAYER, "i_skull", "Automatyczne odrodzenie", "Respawns automatically",
                n("Delay", 0, 0, 20, 1), b("Show Death Coords", true), b("Coords In Chat", false));

        // ======================================================== WORLD
        mod("Nuker", Category.WORLD, "bomb", "Niszczy bloki wokół", "Breaks blocks around",
                m("Mode", "All", "Flatten", "Smash", "Filter"), n("Range", 4, 1, 6, 1), n("Delay", 0, 0, 10, 1),
                m("Rotations", "Silent", "Legit", "None"), b("Swing", true), b("Only Selected", false));
        mod("Scaffold", Category.WORLD, "construction", "Stawia bloki pod nogami", "Places blocks below",
                m("Mode", "Normal", "Telly", "Godbridge", "Keep Y"), m("Rotations", "Silent", "Legit", "Snap"),
                n("Delay", 0, 0, 10, 1), n("Expand", 0, 0, 5, 1),
                b("Tower", true), b("Safe Walk", true), b("Sprint", false), b("Swing", true), b("Block Counter", true));
        mod("FastBreak", Category.WORLD, "pickaxe", "Szybsze kopanie", "Break blocks faster",
                m("Mode", "Damage", "Packet", "Instant"), n("Speed", 1.3, 1.0, 3.0, 0.05), b("Ignore Ground", false));
        mod("Timer", Category.WORLD, "timer", "Zmienia prędkość gry", "Changes game speed",
                n("Speed", 1.0, 0.1, 5.0, 0.05), b("Only Moving", false), b("Pause In Liquids", true));
        mod("AutoFarm", Category.WORLD, "i_sprout", "Zbiera i sadzi rośliny", "Harvests and replants crops",
                n("Range", 4, 1, 6, 1), n("Delay", 2, 0, 10, 1), b("Harvest", true), b("Replant", true), b("Bonemeal", false),
                b("Rotate", true));
        mod("Weather", Category.WORLD, "i_rain", "Własna pogoda (tylko u Ciebie)", "Client-side weather",
                m("Weather", "Clear", "Rain", "Thunder"), n("Intensity", 100, 0, 100, 5));
        mod("LiquidPlace", Category.WORLD, "i_drops", "Stawianie bloków na wodzie", "Place blocks on liquids",
                b("Water", true), b("Lava", true), b("Air Place", false));
        mod("AutoSign", Category.WORLD, "i_pen", "Automatyczny tekst na tabliczkach", "Writes signs automatically",
                m("Lines", "Custom", "Coords", "Date"), b("Glow", false), b("Waterproof", true));
        mod("TimeChanger", Category.WORLD, "i_time", "Własna pora dnia (tylko u Ciebie)", "Client-side time of day",
                m("Time", "Day", "Sunset", "Night", "Midnight", "Custom"), n("Custom Time", 6000, 0, 24000, 500), b("Animate", true));

        // ======================================================== MISC
        mod("AntiAFK", Category.UTILITY, "coffee", "Zapobiega wyrzuceniu za AFK", "Prevents AFK kicks",
                m("Mode", "Jump", "Rotate", "Walk", "Swing"), n("Interval", 30, 5, 300, 5), b("Random", true));
        mod("AutoReconnect", Category.UTILITY, "refresh", "Automatyczne łączenie", "Reconnects automatically",
                n("Delay", 5, 1, 30, 1), n("Attempts", 5, 1, 50, 1), b("Show Button", true));
        mod("MiddleClick", Category.UTILITY, "player", "Znajomi pod kółkiem myszy", "Add friends with middle click",
                b("Notify", true), b("Only Players", true));
        mod("DiscordRPC", Category.UTILITY, "message", "Status w Discordzie", "Discord rich presence",
                m("Large Image", "Logo", "Skin"), b("Show Server", true), b("Show Name", true), b("Show Time", true));
        modules.add(new Cosmetic());
        modules.add(new HitSound());
        mod("NameProtect", Category.UTILITY, "i_mask", "Ukrywa Twój nick na ekranie", "Hides your name on screen",
                m("Replace With", "You", "Player", "Stars"), b("Friends", false));
        mod("AutoGG", Category.UTILITY, "i_trophy", "Pisze GG po wygranej", "Says GG after a win",
                m("Message", "gg", "GG", "good game", "gg wp"), n("Delay", 1, 0, 10, 1), b("Only Win", true));
        mod("ChatSuffix", Category.UTILITY, "i_msgplus", "Dopisek do wiadomości", "Adds a suffix to messages",
                m("Suffix", "| RazerClient", "» Razer", "razer.dev"), b("Small Font", true));
        mod("AntiBot", Category.UTILITY, "i_bot", "Ignoruje boty serwera", "Ignores server bots",
                m("Mode", "Tab", "Ping", "Advanced"), b("Hide In Tab", false), b("Remove", false));
        modules.add(new FakePlayer());
        modules.add(new Crosshair());
        modules.add(new ViewModel());
        modules.add(new Trail());
        placeAll();
    }

    /** kategoria i podgrupa każdego modułu (RazerClient 4.0) */
    private static final Object[][] PLACES = {
            {Category.COMBAT, "Aura", "KillAura", "TriggerBot"},
            {Category.COMBAT, "Assist", "AimAssist", "AutoClicker"},
            {Category.COMBAT, "Tweaks", "Criticals", "Reach", "WTap", "HitBoxes"},
            {Category.COMBAT, "Defense", "Velocity", "AutoTotem"},
            {Category.MOVEMENT, "Ground", "Sprint", "Speed", "Step", "NoFall"},
            {Category.MOVEMENT, "Air", "Flight", "LongJump", "AirJump", "ElytraBoost"},
            {Category.MOVEMENT, "Terrain", "Spider", "Jesus"},
            {Category.PLAYER, "Inventory", "AutoArmor", "InvManager", "ChestStealer", "AutoTool"},
            {Category.PLAYER, "Actions", "FastPlace", "FastUse", "NoSlow"},
            {Category.PLAYER, "Survival", "AutoEat", "AutoRespawn"},
            {Category.VISUALS, "Entities", "TargetESP", "Nametags", "ESP", "Tracers"},
            {Category.VISUALS, "Blocks", "BlockOutline", "ChestESP"},
            {Category.VISUALS, "Overlays", "Fullbright", "NoFire"},
            {Category.CAMERA, "View", "Zoom", "FreeLook", "Crosshair"},
            {Category.CAMERA, "Hand", "ViewModel"},
            {Category.CAMERA, "Effects", "NoHurtCam"},
            {Category.WORLD, "Atmosphere", "Sky", "Ambient", "TimeChanger", "Weather"},
            {Category.WORLD, "Building", "Scaffold", "Nuker", "FastBreak", "LiquidPlace", "AutoSign"},
            {Category.WORLD, "Automation", "AutoFarm", "Timer"},
            {Category.COSMETICS, "Wearables", "Cosmetic", "ChinaHat"},
            {Category.COSMETICS, "Effects", "Trail", "JumpCircles"},
            {Category.COSMETICS, "Items", "ToolColor"},
            {Category.COSMETICS, "Sounds", "HitSound"},
            {Category.UTILITY, "Chat", "AutoGG", "ChatSuffix"},
            {Category.UTILITY, "Social", "MiddleClick", "DiscordRPC"},
            {Category.UTILITY, "Server", "AutoReconnect", "AntiAFK", "AntiBot"},
            {Category.UTILITY, "Privacy", "NameProtect"},
            {Category.UTILITY, "Tools", "FakePlayer"},
    };

    private void placeAll() {
        for (Object[] row : PLACES)
            for (int i = 2; i < row.length; i++) {
                Module m = get((String) row[i]);
                if (m != null) m.place((Category) row[0], (String) row[1]);
            }
        // kolejność: kategoria -> podgrupa -> kolejność z tabeli
        List<String> order = new ArrayList<>();
        for (Object[] row : PLACES) for (int i = 2; i < row.length; i++) order.add((String) row[i]);
        modules.sort(java.util.Comparator.comparingInt((Module m) -> m.getCategory().ordinal())
                .thenComparingInt(m -> m.getCategory().groups.indexOf(m.getGroup()))
                .thenComparingInt(m -> order.indexOf(m.getName())));
    }

    /** moduły kategorii w danej podgrupie (null = wszystkie) */
    public List<Module> byGroup(Category c, String group) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.getCategory() == c && (group == null || group.equals(m.getGroup()))) out.add(m);
        return out;
    }

    /** ostatnio przełączane moduły: {Module, czas} */
    public final java.util.ArrayDeque<Object[]> recent = new java.util.ArrayDeque<>();

    public void noteToggle(Module m) {
        recent.removeIf(o -> o[0] == m);
        recent.addFirst(new Object[]{m, System.currentTimeMillis()});
        while (recent.size() > 8) recent.removeLast();
    }

    public List<Module> all() { return modules; }

    public List<Module> byCategory(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.getCategory() == c) out.add(m);
        return out;
    }

    public Module get(String name) {
        for (Module m : modules) if (m.getName().equalsIgnoreCase(name)) return m;
        return null;
    }

    public long enabledCount() { return modules.stream().filter(Module::isEnabled).count(); }
}
