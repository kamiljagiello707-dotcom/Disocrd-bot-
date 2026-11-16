package dev.vesc.client.module.player;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * AutoTool – Picks the best tool.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class AutoTool extends Module {
    public AutoTool() {
        super("AutoTool", Category.PLAYER, "i_wrench", "Wybiera najlepsze narzędzie", "Picks the best tool");
        place(Category.PLAYER, "Inventory");
        add(
                toggle("Swap Back", true),
                toggle("Weapons", true),
                toggle("Silk Touch", false),
                toggle("Anti Break", true),
                slider("Min Durability", 10, 0, 100, 5)
        );
    }
}
