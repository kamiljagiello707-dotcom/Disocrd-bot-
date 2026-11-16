package dev.vesc.client.module.combat;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * AutoClicker – Automatic clicking.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class AutoClicker extends Module {
    public AutoClicker() {
        super("AutoClicker", Category.COMBAT, "i_hand", "Automatyczne klikanie", "Automatic clicking");
        place(Category.COMBAT, "Assist");
        add(
                slider("Min CPS", 10, 1, 25, 1),
                slider("Max CPS", 14, 1, 25, 1),
                choice("Pattern", "Random", "Butterfly", "Jitter", "Drag"),
                toggle("Left Click", true),
                toggle("Right Click", false),
                toggle("Break Blocks", true),
                toggle("Only Weapon", false),
                toggle("Inventory", false)
        );
    }
}
