package dev.vesc.client.module.combat;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * AutoTotem – Totem in offhand.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class AutoTotem extends Module {
    public AutoTotem() {
        super("AutoTotem", Category.COMBAT, "heart", "Totem w drugiej ręce", "Totem in offhand");
        place(Category.COMBAT, "Defense");
        add(
                choice("Mode", "Always", "Health", "Smart"),
                slider("Health", 10, 1, 20, 0.5),
                slider("Delay", 0, 0, 10, 1),
                toggle("Open Inventory", false),
                toggle("Swap Back", true),
                toggle("Elytra Check", true)
        );
    }
}
