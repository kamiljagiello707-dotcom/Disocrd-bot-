package dev.vesc.client.module.player;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * InvManager – Sorts your inventory.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class InvManager extends Module {
    public InvManager() {
        super("InvManager", Category.PLAYER, "backpack", "Porządkuje ekwipunek", "Sorts your inventory");
        place(Category.PLAYER, "Inventory");
        add(
                slider("Delay", 2, 0, 10, 1),
                choice("Sort", "Smart", "Type", "Name"),
                toggle("Drop Junk", true),
                toggle("Keep Tools", true),
                toggle("Only In Inventory", false),
                toggle("Hotbar Layout", true)
        );
    }
}
