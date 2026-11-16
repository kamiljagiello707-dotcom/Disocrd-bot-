package dev.vesc.client.module.movement;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * Sprint – Automatic sprint.
 * Działa – logika w {@link dev.vesc.client.module.Legit} (po stronie klienta, wizualnie / dla wygody).
 */
public class Sprint extends Module {
    public Sprint() {
        super("Sprint", Category.MOVEMENT, "footprints", "Automatyczny sprint", "Automatic sprint");
        place(Category.MOVEMENT, "Ground");
        add(
                toggle("Only Forward", true),
                toggle("Stop On Sneak", true)
        );
    }
}
