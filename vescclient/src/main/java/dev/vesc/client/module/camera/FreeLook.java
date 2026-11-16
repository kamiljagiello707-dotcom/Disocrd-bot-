package dev.vesc.client.module.camera;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * FreeLook – Look around without turning.
 * Działa – logika w {@link dev.vesc.client.module.Legit} (po stronie klienta, wizualnie / dla wygody).
 */
public class FreeLook extends Module {
    public FreeLook() {
        super("FreeLook", Category.CAMERA, "render", "Rozglądanie bez obracania postaci", "Look around without turning");
        place(Category.CAMERA, "View");
        add(
                choice("Mode", "Hold", "Toggle"),
                slider("Sensitivity", 1.0, 0.1, 3.0, 0.1),
                toggle("Invert Pitch", false),
                toggle("Third Person", true)
        );
    }
}
