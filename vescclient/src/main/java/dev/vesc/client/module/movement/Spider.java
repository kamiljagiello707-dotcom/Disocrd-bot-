package dev.vesc.client.module.movement;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * Spider – Climb walls.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class Spider extends Module {
    public Spider() {
        super("Spider", Category.MOVEMENT, "i_bug", "Wspinanie po ścianach", "Climb walls");
        place(Category.MOVEMENT, "Terrain");
        add(
                choice("Mode", "Vanilla", "Jump", "Motion"),
                slider("Speed", 0.3, 0.1, 1.0, 0.05),
                toggle("Only Sneak", false),
                toggle("Liquids", false)
        );
    }
}
