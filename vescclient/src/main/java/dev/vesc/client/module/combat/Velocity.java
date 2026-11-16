package dev.vesc.client.module.combat;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * Velocity – Reduces knockback.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class Velocity extends Module {
    public Velocity() {
        super("Velocity", Category.COMBAT, "shield", "Zmniejsza odrzut", "Reduces knockback");
        place(Category.COMBAT, "Defense");
        add(
                choice("Mode", "Cancel", "Percent", "Reverse", "Jump Reset"),
                slider("Horizontal", 0, 0, 100, 1),
                slider("Vertical", 0, 0, 100, 1),
                slider("Chance", 100, 0, 100, 1),
                toggle("Only When Targeting", false),
                toggle("Explosions", true),
                toggle("Pause In Water", true)
        );
    }
}
