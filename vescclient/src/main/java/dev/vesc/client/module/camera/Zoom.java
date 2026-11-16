package dev.vesc.client.module.camera;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * Zoom – Zoom in your view.
 * Działa – logika w {@link dev.vesc.client.module.Legit} (po stronie klienta, wizualnie / dla wygody).
 */
public class Zoom extends Module {
    public Zoom() {
        super("Zoom", Category.CAMERA, "i_zoom", "Przybliżenie widoku", "Zoom in your view");
        place(Category.CAMERA, "View");
        add(
                slider("Zoom", 4.0, 1.5, 10.0, 0.5),
                toggle("Smooth", true),
                toggle("Scroll Zoom", true),
                toggle("Cinematic Camera", false),
                toggle("Hide Hand", true)
        );
    }
}
