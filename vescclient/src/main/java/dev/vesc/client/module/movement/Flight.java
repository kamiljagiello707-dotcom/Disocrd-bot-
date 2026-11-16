package dev.vesc.client.module.movement;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * Flight – Fly.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class Flight extends Module {
    public Flight() {
        super("Flight", Category.MOVEMENT, "plane", "Latanie", "Fly");
        place(Category.MOVEMENT, "Air");
        add(
                choice("Mode", "Vanilla", "Creative", "Glide", "Motion"),
                slider("Horizontal Speed", 1.0, 0.1, 5.0, 0.1),
                slider("Vertical Speed", 0.6, 0.1, 3.0, 0.1),
                slider("Glide Speed", 0.03, 0.0, 0.2, 0.01),
                toggle("Anti Kick", true),
                toggle("Stop On Disable", true),
                toggle("View Bobbing", false)
        );
    }
}
