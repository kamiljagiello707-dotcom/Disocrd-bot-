package dev.vesc.client.module.movement;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * LongJump – Jump further.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class LongJump extends Module {
    public LongJump() {
        super("LongJump", Category.MOVEMENT, "i_jump", "Daleki skok", "Jump further");
        place(Category.MOVEMENT, "Air");
        add(
                choice("Mode", "Boost", "Glide", "Damage"),
                slider("Boost", 2.0, 1.0, 6.0, 0.1),
                slider("Height", 0.42, 0.2, 1.0, 0.02),
                toggle("Auto Disable", true),
                toggle("Auto Jump", true)
        );
    }
}
