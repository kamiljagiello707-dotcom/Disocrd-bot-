package dev.vesc.client.module.player;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * FastPlace – Place blocks faster.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class FastPlace extends Module {
    public FastPlace() {
        super("FastPlace", Category.PLAYER, "blocks", "Szybkie stawianie", "Place blocks faster");
        place(Category.PLAYER, "Actions");
        add(
                slider("Delay", 0, 0, 4, 1),
                toggle("Blocks", true),
                toggle("Projectiles", false),
                toggle("XP Bottles", true),
                toggle("Crystals", false)
        );
    }
}
