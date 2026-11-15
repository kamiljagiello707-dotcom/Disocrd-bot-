package dev.razer.client.module;

import dev.razer.client.setting.BoolSetting;
import dev.razer.client.setting.ModeSetting;
import dev.razer.client.setting.NumberSetting;

/**
 * Target ESP – czysto wizualny znacznik na celu (gracz, którego uderzyłeś lub na którego patrzysz).
 * Rysowany z testem głębi, więc NIE jest widoczny przez ściany.
 */
public class TargetEsp extends Module {
    public final ModeSetting mode = new ModeSetting("Styl", "Style", "Oba / Both", "Pierścień / Ring", "Znacznik / Marker");
    public final BoolSetting playersOnly = new BoolSetting("Tylko gracze", "Players only", true);
    public final BoolSetting trail = new BoolSetting("Smuga", "Trail", true);
    public final NumberSetting speed = new NumberSetting("Prędkość", "Speed", 1.0, 0.3, 3.0, 0.1);
    public final NumberSetting size = new NumberSetting("Rozmiar", "Size", 1.0, 0.5, 2.0, 0.1);

    public TargetEsp() {
        super("TargetESP", Category.RENDER, "targethud", "Efektowny znacznik na celu", "Fancy marker on your target");
        add(mode, playersOnly, trail, speed, size);
    }

    public boolean ring() { return !mode.get().startsWith("Znacznik"); }
    public boolean marker() { return !mode.get().startsWith("Pierścień"); }
}
