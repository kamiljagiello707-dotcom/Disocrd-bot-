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
