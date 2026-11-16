package dev.vesc.client.module.visuals;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * NoFire – Lower fire overlay.
 * Działa – logika w {@link dev.vesc.client.module.Legit} (po stronie klienta, wizualnie / dla wygody).
 */
public class NoFire extends Module {
    public NoFire() {
        super("NoFire", Category.VISUALS, "i_flame", "Mniejszy ogień na ekranie", "Lower fire overlay");
        place(Category.VISUALS, "Overlays");
        add(
                slider("Height", 0.3, 0.0, 1.0, 0.05),
                slider("Opacity", 50, 0, 100, 5),
                toggle("Hide In Water", true)
        );
    }
}
