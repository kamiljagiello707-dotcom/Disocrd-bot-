package dev.vesc.client.module.camera;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * NoHurtCam – No camera shake on damage.
 * Działa – logika w {@link dev.vesc.client.module.Legit} (po stronie klienta, wizualnie / dla wygody).
 */
public class NoHurtCam extends Module {
    public NoHurtCam() {
        super("NoHurtCam", Category.CAMERA, "i_camera", "Brak trzęsienia kamery", "No camera shake on damage");
        place(Category.CAMERA, "Effects");
        add(
                slider("Strength", 0, 0, 100, 5),
                toggle("No Bobbing", false),
                toggle("No Totem Animation", false)
        );
    }
}
