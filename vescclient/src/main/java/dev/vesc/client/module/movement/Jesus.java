package dev.vesc.client.module.movement;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * Jesus – Walk on water.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class Jesus extends Module {
    public Jesus() {
        super("Jesus", Category.MOVEMENT, "i_waves", "Chodzenie po wodzie", "Walk on water");
        place(Category.MOVEMENT, "Terrain");
        add(
                choice("Mode", "Solid", "Dolphin", "Bounce"),
                slider("Speed", 1.0, 0.5, 2.0, 0.05),
                toggle("Lava", true),
                toggle("Pause On Sneak", true)
        );
    }
}
