package dev.vesc.client.module.movement;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * ElytraBoost – Faster elytra flight.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class ElytraBoost extends Module {
    public ElytraBoost() {
        super("ElytraBoost", Category.MOVEMENT, "i_rocket", "Szybszy lot na elytrze", "Faster elytra flight");
        place(Category.MOVEMENT, "Air");
        add(
                choice("Mode", "Control", "Boost", "Packet"),
                slider("Speed", 1.6, 0.5, 4.0, 0.1),
                slider("Vertical Speed", 0.8, 0.1, 3.0, 0.1),
                toggle("Auto Takeoff", true),
                toggle("Stop In Water", true),
                toggle("Firework Boost", false)
        );
    }
}
