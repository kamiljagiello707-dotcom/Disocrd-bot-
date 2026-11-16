package dev.vesc.client.module.visuals;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * Fullbright – Full brightness.
 * Działa – logika w {@link dev.vesc.client.module.Legit} (po stronie klienta, wizualnie / dla wygody).
 */
public class Fullbright extends Module {
    public Fullbright() {
        super("Fullbright", Category.VISUALS, "sun", "Pełna jasność", "Full brightness");
        place(Category.VISUALS, "Overlays");
        add(
                choice("Mode", "Gamma", "Night Vision"),
                slider("Gamma", 100, 0, 100, 5),
                toggle("Fade", true)
        );
    }
}
