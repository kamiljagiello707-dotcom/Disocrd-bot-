package dev.vesc.client.module.player;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * AutoEat – Eats automatically.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class AutoEat extends Module {
    public AutoEat() {
        super("AutoEat", Category.PLAYER, "i_eat", "Automatyczne jedzenie", "Eats automatically");
        place(Category.PLAYER, "Survival");
        add(
                slider("Hunger", 14, 1, 19, 1),
                slider("Health", 10, 1, 20, 0.5),
                choice("Food", "Best", "Golden Apple", "Any"),
                toggle("Pause KillAura", true),
                toggle("Swap Back", true)
        );
    }
}
