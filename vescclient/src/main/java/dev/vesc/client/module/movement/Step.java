package dev.vesc.client.module.movement;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * Step – Step up blocks.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class Step extends Module {
    public Step() {
        super("Step", Category.MOVEMENT, "step", "Wchodzenie na bloki", "Step up blocks");
        place(Category.MOVEMENT, "Ground");
        add(
                choice("Mode", "Vanilla", "Packet", "Jump"),
                slider("Height", 1.0, 0.5, 2.5, 0.5),
                slider("Delay", 0, 0, 10, 1),
                toggle("Entity Step", false)
        );
    }
}
