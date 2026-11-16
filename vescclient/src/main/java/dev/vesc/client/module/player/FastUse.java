package dev.vesc.client.module.player;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * FastUse – Use items faster.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class FastUse extends Module {
    public FastUse() {
        super("FastUse", Category.PLAYER, "potion", "Szybsze używanie przedmiotów", "Use items faster");
        place(Category.PLAYER, "Actions");
        add(
                slider("Speed", 2, 1, 10, 1),
                toggle("Food", true),
                toggle("Potions", true),
                toggle("Bows", false),
                toggle("Tridents", false)
        );
    }
}
