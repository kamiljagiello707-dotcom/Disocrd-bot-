package dev.vesc.client.module.visuals;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

import dev.vesc.client.setting.BoolSetting;
import dev.vesc.client.setting.ModeSetting;
import dev.vesc.client.setting.NumberSetting;

/**
 * Target ESP – czysto wizualny znacznik na celu (gracz, którego uderzyłeś lub na którego patrzysz).
 * Rysowany z testem głębi, więc NIE jest widoczny przez ściany.
 */
public class TargetEsp extends Module {
    public final dev.vesc.client.setting.ModeSetting colorMode = new dev.vesc.client.setting.ModeSetting("Kolor", "Color", "Accent", "Custom");
    public final dev.vesc.client.setting.ColorSetting customColor = new dev.vesc.client.setting.ColorSetting("Własny kolor", "Custom Color", 0xFFB46BFF);
    /** własny kolor albo null (= kolor akcentu) */
    public Integer custom() { return colorMode.get().equals("Custom") ? customColor.get() : null; }

    public final ModeSetting mode = new ModeSetting("Styl", "Style", "Mix", "Ghosts", "Pulse", "Circle", "Ring", "Marker", "Orbit", "Helix", "Halo", "Sparkles", "Brackets", "Vortex", "Crystal", "Reticle", "Gyro", "Aura", "Souls", "Rune", "Stars", "Comet", "Bubble", "Spirits", "Neon", "Quad", "Clover");
    public final BoolSetting playersOnly = new BoolSetting("Tylko gracze", "Players only", true);
    public final BoolSetting trail = new BoolSetting("Smuga", "Trail", true);
    public final NumberSetting ghosts = new NumberSetting("Ilość duchów", "Ghost count", 3, 1, 6, 1);
    public final NumberSetting speed = new NumberSetting("Prędkość", "Speed", 1.0, 0.3, 3.0, 0.1);
    public final NumberSetting size = new NumberSetting("Rozmiar", "Size", 1.0, 0.5, 2.0, 0.1);

    public TargetEsp() {
        super("TargetESP", Category.VISUALS, "targethud", "Efektowny znacznik na celu", "Fancy marker on your target");
        place(Category.VISUALS, "Entities");
        add(mode, playersOnly, trail, ghosts, speed, size, colorMode, customColor);
    }

    public boolean is(String m) { return mode.get().equals(m); }
}
