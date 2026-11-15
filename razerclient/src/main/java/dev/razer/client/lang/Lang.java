package dev.razer.client.lang;

import java.util.HashMap;
import java.util.Map;

/** Prosty system tłumaczeń klienta (niezależny od języka gry). */
public final class Lang {
    public enum Language {
        POLSKI("Polski", "PL"), ENGLISH("English", "EN");
        public final String display, code;
        Language(String display, String code) { this.display = display; this.code = code; }
    }

    private static Language current = Language.POLSKI;
    private static final Map<String, String[]> STRINGS = new HashMap<>();

    private static void put(String key, String pl, String en) { STRINGS.put(key, new String[]{pl, en}); }

    static {
        // kategorie
        put("cat.combat", "Walka", "Combat");
        put("cat.movement", "Ruch", "Movement");
        put("cat.render", "Wizualne", "Render");
        put("cat.player", "Gracz", "Player");
        put("cat.world", "Świat", "World");
        put("cat.misc", "Inne", "Misc");
        put("page.themes", "Motywy", "Themes");
        put("page.config", "Konfiguracje", "Config");
        put("page.profile", "Profil gracza", "Player profile");
        put("page.settings", "Ustawienia klienta", "Client settings");
        put("section.modules", "MODUŁY", "MODULES");
        put("section.client", "KLIENT", "CLIENT");
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
        put("themes.desc", "Biało-czarne motywy z płynnym przejściem kolorów.", "Black & white themes with smooth color transitions.");
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
        put("set.language.desc", "Język całego interfejsu RazerClient", "Language of the whole RazerClient interface");
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
        put("set.menu", "Własne menu główne", "Custom main menu");
        put("set.menu.desc", "Ekran blokady z godziną i nowoczesne menu RazerClient", "Lock screen with clock and modern RazerClient menu");
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
