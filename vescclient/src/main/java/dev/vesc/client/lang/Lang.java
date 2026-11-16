package dev.vesc.client.lang;

import java.util.HashMap;
import java.util.Map;

/** Prosty system tłumaczeń klienta (niezależny od języka gry). */
public final class Lang {
    public enum Language {
        POLSKI("Polski", "PL"), ENGLISH("English", "EN");
        public final String display, code;
        Language(String display, String code) { this.display = display; this.code = code; }
    }

    private static Language current = Language.ENGLISH;
    private static final Map<String, String[]> STRINGS = new HashMap<>();

    private static void put(String key, String pl, String en) { STRINGS.put(key, new String[]{pl, en}); }

    static {
        // kategorie
        put("cat.combat", "Walka", "Combat");
        put("cat.movement", "Ruch", "Movement");
        put("cat.player", "Gracz", "Player");
        put("cat.visuals", "Wizualne", "Visuals");
        put("cat.camera", "Kamera", "Camera");
        put("cat.world", "Świat", "World");
        put("cat.cosmetics", "Kosmetyki", "Cosmetics");
        put("cat.utility", "Narzędzia", "Utility");
        put("cat.combat.desc", "Ustawienia walki, celowania i obrony", "Fighting, aiming and defense options");
        put("cat.movement.desc", "Poruszanie się po ziemi, w powietrzu i po terenie", "Moving on the ground, in the air and over terrain");
        put("cat.player.desc", "Ekwipunek, akcje i przetrwanie", "Inventory, actions and survival");
        put("cat.visuals.desc", "Znaczniki celu, nametagi, obrysy bloków i nakładki", "Target markers, nametags, block outlines and overlays");
        put("cat.camera.desc", "Zoom, swobodna kamera, celownik i model ręki", "Zoom, free camera, crosshair and hand view");
        put("cat.world.desc", "Niebo, pogoda, pora dnia i budowanie", "Sky, weather, time of day and building");
        put("cat.cosmetics.desc", "Peleryny, kapelusze, smugi, efekty i dźwięki", "Capes, hats, trails, effects and sounds");
        put("cat.utility.desc", "Czat, znajomi, serwer i prywatność", "Chat, friends, server and privacy");
        String[][] groups = {{"aura", "Aura", "Aura"}, {"assist", "Wspomaganie", "Assist"}, {"tweaks", "Poprawki", "Tweaks"}, {"defense", "Obrona", "Defense"},
                {"ground", "Ziemia", "Ground"}, {"air", "Powietrze", "Air"}, {"terrain", "Teren", "Terrain"},
                {"inventory", "Ekwipunek", "Inventory"}, {"actions", "Akcje", "Actions"}, {"survival", "Przetrwanie", "Survival"},
                {"entities", "Byty", "Entities"}, {"blocks", "Bloki", "Blocks"}, {"overlays", "Nakładki", "Overlays"},
                {"view", "Widok", "View"}, {"hand", "Ręka", "Hand"}, {"effects", "Efekty", "Effects"},
                {"atmosphere", "Atmosfera", "Atmosphere"}, {"building", "Budowanie", "Building"}, {"automation", "Automatyzacja", "Automation"},
                {"wearables", "Ubrania", "Wearables"}, {"items", "Przedmioty", "Items"}, {"sounds", "Dźwięki", "Sounds"},
                {"chat", "Czat", "Chat"}, {"social", "Znajomi", "Social"}, {"server", "Serwer", "Server"}, {"privacy", "Prywatność", "Privacy"}, {"tools", "Narzędzia", "Tools"},
                {"all", "Wszystkie", "All"}};
        for (String[] g : groups) put("group." + g[0], g[1], g[2]);
        put("section.overview", "PRZEGLĄD", "OVERVIEW");
        put("hero.stats", "%d modułów  •  %d włączonych  •  %d z klawiszem", "%d modules  •  %d enabled  •  %d bound");
        put("hero.enableAll", "Włącz wszystkie", "Enable all");
        put("hero.disableAll", "Wyłącz wszystkie", "Disable all");
        put("page.themes", "Motywy", "Themes");
        put("theme.charcoal", "Grafit", "Charcoal");
        put("accent.violet", "Fiolet", "Violet");
        put("page.config", "Konfiguracje", "Config");
        put("page.profile", "Profil gracza", "Player profile");
        put("page.settings", "Ustawienia klienta", "Client settings");
        put("section.modules", "MODUŁY", "MODULES");
        put("section.client", "INNE", "OTHER");
        put("search", "Szukaj modułów...", "Search modules...");
        put("modules.count", "%d modułów • %d włączonych", "%d modules • %d enabled");
        put("profile.sub", "PPM – ustawienia", "Right click – settings");
        put("enabled", "Włączono", "Enabled");
        put("disabled", "Wyłączono", "Disabled");
        put("bind", "Klawisz", "Keybind");
        put("bind.none", "Brak", "None");
        put("bind.listening", "Naciśnij klawisz...", "Press a key...");
        put("bind.hint", "ESC / DEL – usuń", "ESC / DEL – clear");
        put("rmb.settings", "PPM – opcje", "RMB – options");
        put("hud.modules", "Moduły", "Modules");
        put("hud.keybinds", "KeyBinds", "KeyBinds");
        put("hud.nomodules", "Brak włączonych modułów", "No modules enabled");
        put("hud.nobinds", "Brak przypisanych klawiszy", "No keybinds set");
        put("notif.key", "Klawisz %s", "Key %s");
        // motywy
        put("themes.title", "Wybierz motyw", "Choose a theme");
        put("themes.desc", "Motywy i kolory akcentu z płynnym przejściem.", "Themes and accent colors with smooth transitions.");
        put("theme.dark", "Ciemny", "Dark");
        put("theme.light", "Jasny", "Light");
        put("theme.amoled", "AMOLED", "AMOLED");
        put("theme.active", "Aktywny", "Active");
        // config
        put("config.title", "Konfiguracje", "Configs");
        put("config.desc", "Zapisuj i wczytuj ustawienia modułów, klawiszy i motywu.", "Save and load module, keybind and theme settings.");
        put("config.new", "Nowa konfiguracja", "New config");
        put("config.load", "Wczytaj", "Load");
        put("config.save", "Zapisz", "Save");
        put("config.delete", "Usuń", "Delete");
        put("config.current", "Aktualna", "Current");
        put("config.loaded", "Wczytano konfigurację", "Config loaded");
        put("config.saved", "Zapisano konfigurację", "Config saved");
        put("config.deleted", "Usunięto konfigurację", "Config deleted");
        put("config.created", "Utworzono konfigurację", "Config created");
        put("config.empty", "Brak zapisanych konfiguracji", "No saved configs");
        put("config.folder", "Otwórz folder", "Open folder");
        // profil
        put("profile.desc", "Informacje o koncie i bieżącej sesji", "Account and current session info");
        put("settings.desc", "Język, rozmiar GUI, animacje i elementy HUD", "Language, GUI size, animations and HUD elements");
        put("profile.uuid", "UUID", "UUID");
        put("profile.server", "Serwer", "Server");
        put("profile.singleplayer", "Świat jednoosobowy", "Singleplayer");
        put("profile.menu", "Menu główne", "Main menu");
        put("profile.session", "Czas sesji", "Session time");
        put("profile.enabled", "Włączone moduły", "Enabled modules");
        put("profile.binds", "Przypisane klawisze", "Keybinds");
        put("profile.lang", "Język klienta", "Client language");
        put("profile.hint", "Kliknij prawym przyciskiem na swój profil, aby otworzyć ustawienia klienta.", "Right click your profile to open client settings.");
        // ustawienia
        put("set.language", "Język klienta", "Client language");
        put("set.language.desc", "Język całego interfejsu VescClient", "Language of the whole VescClient interface");
        put("set.guisize", "Rozmiar GUI", "GUI size");
        put("set.guisize.desc", "Skala okna ClickGUI", "Scale of the ClickGUI window");
        put("set.anim", "Szybkość animacji", "Animation speed");
        put("set.anim.desc", "Jak szybko działają animacje w GUI", "How fast GUI animations play");
        put("set.blur", "Rozmycie tła", "Background blur");
        put("set.blur.desc", "Rozmywa świat za otwartym GUI", "Blurs the world behind the GUI");
        put("set.watermark", "Watermark", "Watermark");
        put("set.watermark.desc", "Logo • FPS • Godzina • Nick w prawym górnym rogu", "Logo • FPS • Time • Nick in the top right");
        put("set.arraylist", "Lista modułów", "Module list");
        put("set.arraylist.desc", "Włączone moduły w lewym górnym rogu", "Enabled modules in the top left");
        put("set.keybinds", "Panel KeyBinds", "KeyBinds panel");
        put("set.keybinds.desc", "Lista przypisanych klawiszy pod listą modułów", "Bound keys under the module list");
        put("set.notifications", "Powiadomienia", "Notifications");
        put("set.notifications.desc", "Powiadomienie po włączeniu / wyłączeniu modułu", "Popup when a module is toggled");
        put("set.reset", "Resetuj ustawienia", "Reset settings");
        put("size.small", "Mały", "Small");
        put("size.normal", "Średni", "Normal");
        put("size.large", "Duży", "Large");
        put("wm.fps", "FPS", "FPS");
        put("panels.hint", "Przeciągnij nagłówek  •  PPM nagłówek – zwiń  •  LPM moduł – włącz  •  PPM – ustawienia  •  ŚPM – klawisz", "Drag header  •  RMB header – collapse  •  LMB module – toggle  •  RMB – settings  •  MMB – bind");
        put("layout.title", "Układ GUI", "GUI layout");
        put("layout.desc", "Pływające panele kategorii albo jedno okno", "Floating category panels or a single window");
        put("hudstyle.title", "Styl HUD", "HUD style");
        put("hudstyle.desc", "Szkło, neon albo minimalistyczny", "Glass, neon or minimal");
        put("hud.el.keystrokes", "Keystrokes", "Keystrokes");
        put("hud.el.keystrokes.desc", "WASD, spacja i myszka z licznikiem CPS", "WASD, space and mouse with CPS");
        put("hud.el.compass", "Kompas", "Compass");
        put("hud.el.compass.desc", "Pasek kierunków u góry ekranu", "Direction strip at the top of the screen");
        put("hud.el.clock", "Zegar", "Clock");
        put("hud.el.clock.desc", "Godzina z sekundami i data", "Time with seconds and date");
        put("hud.el.inventory", "Ekwipunek", "Inventory");
        put("hud.el.inventory.desc", "Podgląd ekwipunku na ekranie", "Inventory preview on screen");
        put("hud.el.stats", "Statystyki", "Session stats");
        put("hud.el.stats.desc", "Zabójstwa, śmierci, K/D i seria", "Kills, deaths, K/D and streak");
        put("hud.el.performance", "Wydajność", "Performance");
        put("hud.el.performance.desc", "Wykres FPS i ping", "FPS graph and ping");
        put("hud.el.speed", "Prędkość", "Speed");
        put("hud.el.speed.desc", "Bloki na sekundę z wykresem", "Blocks per second with a graph");
        put("hud.el.helditem", "Przedmiot w ręce", "Held item");
        put("hud.el.helditem.desc", "Nazwa, wytrzymałość i zaklęcia", "Name, durability and enchantments");
        put("hud.el.playermodel", "Model gracza", "Player model");
        put("hud.el.playermodel.desc", "Postać 3D ze zdrowiem i pancerzem", "3D player with health and armor");
        put("hud.el.server", "Serwer", "Server");
        put("hud.el.server.desc", "Adres, gracze, ping, wymiar i sesja", "Address, players, ping, dimension and session");
        put("hud.kills", "Zabójstwa", "Kills");
        put("hud.deaths", "Śmierci", "Deaths");
        put("hud.streak", "Seria", "Streak");
        put("hud.presets", "Presety układu", "Layout presets");
        put("preset.classic", "Klasyczny", "Classic");
        put("preset.minimal", "Minimalny", "Minimal");
        put("preset.pvp", "PvP", "PvP");
        put("preset.full", "Wszystko", "Full");
        put("page.home", "Start", "Home");
        put("home.desc", "Przegląd klienta i szybkie akcje", "Client overview and quick actions");
        put("home.morning", "Dzień dobry", "Good morning");
        put("home.afternoon", "Miłego popołudnia", "Good afternoon");
        put("home.evening", "Dobry wieczór", "Good evening");
        put("home.night", "Dobranoc", "Good night");
        put("home.enabled", "Włączone moduły", "Enabled modules");
        put("home.binds", "Przypisane klawisze", "Keybinds");
        put("home.session", "Czas sesji", "Session time");
        put("home.favorites", "Ulubione", "Favorites");
        put("home.recent", "Ostatnio używane", "Recently used");
        put("home.nofav", "Dodaj gwiazdkę na module", "Star a module to pin it");
        put("home.norecent", "Brak – przełącz jakiś moduł", "Nothing yet – toggle a module");
        put("home.palette", "Paleta komend", "Command palette");
        put("style.title", "Styl", "Style");
        put("style.desc", "Pełne panele albo półprzezroczyste szkło", "Solid panels or translucent glass");
        put("filter.all", "Wszystkie", "All");
        put("filter.enabled", "Włączone", "Enabled");
        put("filter.disabled", "Wyłączone", "Disabled");
        put("filter.bound", "Z klawiszem", "Bound");
        put("sort.default", "Domyślnie", "Default");
        put("sort.name", "A–Z", "A–Z");
        put("sort.enabled", "Włączone", "Enabled first");
        put("insp.enabled", "Włączony", "Enabled");
        put("insp.keybind", "Klawisz", "Keybind");
        put("insp.fav", "Ulubiony", "Favorite");
        put("insp.unfav", "Usuń z ulub.", "Unfavorite");
        put("insp.reset", "Resetuj", "Reset");
        put("insp.resetdone", "Zresetowano ustawienia", "Settings reset");
        put("insp.modes", "TRYBY", "MODES");
        put("insp.values", "WARTOŚCI", "VALUES");
        put("insp.toggles", "PRZEŁĄCZNIKI", "TOGGLES");
        put("insp.colors", "KOLORY", "COLORS");
        put("insp.nosettings", "Ten moduł nie ma ustawień", "This module has no settings");
        put("tip.settings", "%d ustawień", "%d settings");
        put("pal.placeholder", "Wpisz moduł, stronę albo akcję…", "Type a module, page or action…");
        put("pal.hint", "↑↓ wybór  •  Enter uruchom  •  Shift+Enter / PPM inspektor", "↑↓ select  •  Enter run  •  Shift+Enter / RMB inspect");
        put("pal.goto", "Przejdź", "Go to");
        put("pal.page", "Strona", "Page");
        put("pal.action", "Akcja", "Action");
        put("pal.theme", "Motyw", "Theme");
        put("pal.accent", "Akcent", "Accent");
        put("pal.language", "Język", "Language");
        put("pal.save", "Zapisz konfigurację", "Save config");
        put("pal.disableall", "Wyłącz wszystkie moduły", "Disable all modules");
        put("hud.el.notifications", "Powiadomienia", "Notifications");
        put("hud.el.notifications.desc", "Włączenie / wyłączenie modułu i inne komunikaty", "Module toggles and other messages");
        put("page.friends", "Znajomi", "Friends");
        put("friends.desc", "Lista Twoich znajomych na serwerach", "Your friends list");
        put("friends.placeholder", "Wpisz nick znajomego...", "Type a friend's name...");
        put("friends.add", "Dodaj", "Add");
        put("friends.added", "Dodano znajomego", "Friend added");
        put("friends.empty", "Brak znajomych – dodaj kogoś powyżej", "No friends yet – add someone above");
        put("profile.modules", "%d modułów włączonych", "%d modules on");
        put("theme.midnight", "Midnight", "Midnight");
        put("footer.toggle", "włącz", "toggle");
        put("footer.settings", "opcje", "settings");
        put("footer.bind", "klawisz", "bind");
        put("footer.wheel", "przewijanie (na suwakach: wartość)", "scroll (on sliders: value)");
        put("footer.close", "zamknij", "close");
        put("set.menu", "Własne menu główne", "Custom main menu");
        put("set.menu.desc", "Ekran blokady z godziną i nowoczesne menu VescClient", "Lock screen with clock and modern VescClient menu");
        put("menu.continue", "Kliknij, aby kontynuować", "Click to continue");
        put("menu.singleplayer", "Jednoosobowy", "Singleplayer");
        put("menu.multiplayer", "Wieloosobowy", "Multiplayer");
        put("menu.options", "Ustawienia", "Options");
        put("menu.quit", "Wyjdź", "Quit");
        put("menu.realms", "Realms", "Realms");
        put("menu.welcome", "Witaj, %s", "Welcome, %s");
        put("chatdrag.hint", "Przeciągnij HUD myszką  •  ŚPM – domyślna pozycja", "Drag the HUD with your mouse  •  MMB – default position");
        put("welcome", "Witaj, %s!  •  Prawy Shift – menu", "Welcome, %s!  •  Right Shift – menu");
        put("cat.favorites", "Ulubione", "Favorites");
        put("page.hud", "HUD", "HUD");
        put("hud.desc", "Włączaj elementy HUD i ustawiaj je myszką w edytorze.", "Toggle HUD elements and move them with the editor.");
        put("hud.editor", "Edytor HUD", "HUD editor");
        put("hud.editor.desc", "Przeciągnij elementy w dowolne miejsce ekranu – przyciąganie do krawędzi i środka.", "Drag elements anywhere – snaps to edges and center.");
        put("hud.editor.open", "Otwórz edytor", "Open editor");
        put("hud.reset", "Resetuj pozycje", "Reset positions");
        put("hud.el.watermark", "Watermark", "Watermark");
        put("hud.el.watermark.desc", "Logo • FPS • Godzina • Nick", "Logo • FPS • Time • Nick");
        put("hud.el.modules", "Lista modułów", "Module list");
        put("hud.el.modules.desc", "Włączone moduły z ikonami", "Enabled modules with icons");
        put("hud.el.keybinds", "Keybinds", "Keybinds");
        put("hud.el.keybinds.desc", "Moduły z przypisanym klawiszem", "Modules with a bound key");
        put("hud.el.target", "TargetHUD", "TargetHUD");
        put("hud.el.target.desc", "Nick, zdrowie i dystans bytu, na którego patrzysz", "Name, health and distance of the entity you look at");
        put("hud.el.armor", "Zbroja", "Armor");
        put("hud.el.armor.desc", "Zbroja i przedmiot w ręce z wytrzymałością", "Armor and held item with durability");
        put("hud.el.potions", "Efekty", "Effects");
        put("hud.el.potions.desc", "Aktywne efekty mikstur i czas", "Active potion effects and duration");
        put("hud.el.coords", "Koordynaty", "Coordinates");
        put("hud.el.coords.desc", "XYZ, kierunek i koordynaty Netheru", "XYZ, facing and Nether coords");
        put("hud.el.info", "Informacje", "Info");
        put("hud.el.info.desc", "Ping, prędkość, serwer i czas sesji", "Ping, speed, server and session time");
        put("hud.potions", "Efekty", "Effects");
        put("hud.coords", "Koordynaty", "Coordinates");
        put("hud.facing", "Kierunek", "Facing");
        put("hud.info", "Informacje", "Info");
        put("hud.speed", "Prędkość", "Speed");
        put("editor.hint", "Przeciągnij  •  PPM – ukryj  •  ŚPM – reset", "Drag  •  RMB – hide  •  MMB – reset");
        put("editor.done", "Gotowe", "Done");
        put("themes.accent", "Kolor akcentu", "Accent color");
        put("themes.accent.desc", "Gradient używany w HUD, przełącznikach i zaznaczeniach", "Gradient used in HUD, toggles and selection");
        put("accent.mono", "Mono", "Mono");
        put("accent.purple", "Fiolet", "Purple");
        put("accent.pink", "Róż", "Pink");
        put("accent.blue", "Niebieski", "Blue");
        put("accent.cyan", "Cyjan", "Cyan");
        put("accent.green", "Zielony", "Green");
        put("accent.red", "Czerwony", "Red");
        put("accent.gold", "Złoty", "Gold");
        put("set.particles", "Animowane tło", "Animated background");
        put("set.particles.desc", "Delikatne cząsteczki i poświata za oknem GUI", "Soft particles and glow behind the GUI");
        put("set.sounds", "Dźwięki GUI", "GUI sounds");
        put("set.sounds.desc", "Ciche kliknięcia przy przełączaniu", "Quiet clicks when toggling");
        put("set.welcome", "Powitanie", "Welcome message");
        put("set.welcome.desc", "Powiadomienie po wejściu na serwer", "Notification when joining a world");
        put("fav.empty", "Brak ulubionych – kliknij gwiazdkę na module", "No favorites – click the star on a module");
        put("shortcuts", "Ctrl+F – szukaj  •  Tab – następna kategoria", "Ctrl+F – search  •  Tab – next category");
    }

    public static String get(String key) {
        String[] v = STRINGS.get(key);
        if (v == null) return key;
        return v[current.ordinal()];
    }

    public static String format(String key, Object... args) { return String.format(get(key), args); }

    public static Language current() { return current; }
    public static void set(Language l) { current = l; }
    public static boolean pl() { return current == Language.POLSKI; }
}
