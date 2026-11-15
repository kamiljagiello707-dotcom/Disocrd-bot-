package dev.razer.client.config;

import dev.razer.client.setting.BoolSetting;
import dev.razer.client.setting.ModeSetting;
import dev.razer.client.setting.NumberSetting;

/** Ustawienia samego klienta (PPM na profil gracza). */
public class ClientSettings {
    public final ModeSetting guiSize = new ModeSetting("Rozmiar GUI", "GUI size", "size.normal", "size.small", "size.large");
    public final NumberSetting animSpeed = new NumberSetting("Szybkość animacji", "Animation speed", 1.0, 0.5, 2.0, 0.1);
    public final BoolSetting blur = new BoolSetting("Rozmycie tła", "Background blur", true);
    public final BoolSetting particles = new BoolSetting("Animowane tło", "Animated background", true);
    public final BoolSetting sounds = new BoolSetting("Dźwięki GUI", "GUI sounds", true);
    public final BoolSetting notifications = new BoolSetting("Powiadomienia", "Notifications", true);
    public final BoolSetting welcome = new BoolSetting("Powitanie", "Welcome", true);
    public final BoolSetting customMenu = new BoolSetting("Menu główne", "Main menu", true);

    public float guiScale() {
        return switch (guiSize.get()) {
            case "size.small" -> 0.88f;
            case "size.large" -> 1.12f;
            default -> 1f;
        };
    }

    public void reset() {
        guiSize.reset(); animSpeed.reset(); blur.reset(); particles.reset();
        sounds.reset(); notifications.reset(); welcome.reset(); customMenu.reset();
    }
}
