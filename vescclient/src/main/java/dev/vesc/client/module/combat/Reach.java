package dev.vesc.client.module.combat;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * Reach – Hit from further away.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class Reach extends Module {
    public Reach() {
        super("Reach", Category.COMBAT, "i_ruler", "Większy zasięg", "Hit from further away");
        place(Category.COMBAT, "Tweaks");
        add(
                slider("Combat Range", 3.6, 3.0, 6.0, 0.05),
                slider("Block Range", 4.5, 4.5, 7.0, 0.1),
                slider("Chance", 100, 0, 100, 1),
                toggle("Only Sprinting", false),
                toggle("Disable In Water", true)
        );
    }
}
