package dev.vesc.client.module;

import dev.vesc.client.setting.BoolSetting;
import dev.vesc.client.setting.ModeSetting;
import dev.vesc.client.setting.NumberSetting;

/** Sky – własne, animowane niebo (tylko wygląd, widoczne u Ciebie). */
public class Sky extends Module {
    public final ModeSetting preset = new ModeSetting("Styl", "Preset", "Aurora", "Galaxy", "Sunset", "Midnight", "Accent");
    public final BoolSetting stars = new BoolSetting("Gwiazdy", "Stars", true);
    public final NumberSetting starAmount = new NumberSetting("Ilość gwiazd", "Star Amount", 100, 10, 200, 10);
    public final BoolSetting aurora = new BoolSetting("Zorza", "Aurora", true);
    public final BoolSetting nebula = new BoolSetting("Mgławice", "Nebula", true);
    public final BoolSetting shootingStars = new BoolSetting("Spadające gwiazdy", "Shooting Stars", true);
    public final BoolSetting fog = new BoolSetting("Kolor mgły", "Match Fog", true);
    public final NumberSetting speed = new NumberSetting("Prędkość", "Speed", 1.0, 0.1, 3.0, 0.1);
    public final NumberSetting brightness = new NumberSetting("Jasność", "Brightness", 100, 30, 150, 5);

    public Sky() {
        super("Sky", Category.VISUALS, "i_rain", "Piękne, animowane niebo", "Beautiful animated sky");
        add(preset, stars, starAmount, aurora, nebula, shootingStars, fog, speed, brightness);
    }
}
