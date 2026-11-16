package dev.vesc.client.module.movement;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * Speed – Move faster.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class Speed extends Module {
    public Speed() {
        super("Speed", Category.MOVEMENT, "gauge", "Szybsze poruszanie", "Move faster");
        place(Category.MOVEMENT, "Ground");
        add(
                choice("Mode", "Strafe", "Vanilla", "Ground", "BHop", "Low Hop"),
                slider("Speed", 1.2, 1.0, 3.0, 0.05),
                slider("Timer", 1.0, 1.0, 2.0, 0.05),
                slider("Jump Height", 0.42, 0.2, 0.6, 0.01),
                toggle("Auto Jump", true),
                toggle("Damage Boost", false),
                toggle("Liquid Check", true),
                toggle("Pause On Sneak", true)
        );
    }
}
