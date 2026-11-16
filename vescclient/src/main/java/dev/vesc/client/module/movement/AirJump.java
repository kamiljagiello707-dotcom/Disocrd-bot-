package dev.vesc.client.module.movement;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * AirJump – Jump in mid-air.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class AirJump extends Module {
    public AirJump() {
        super("AirJump", Category.MOVEMENT, "i_up", "Skok w powietrzu", "Jump in mid-air");
        place(Category.MOVEMENT, "Air");
        add(
                choice("Mode", "Infinite", "Double", "Triple"),
                slider("Height", 0.42, 0.2, 1.0, 0.02),
                toggle("Only On Key", true),
                toggle("Reset On Ground", true)
        );
    }
}
