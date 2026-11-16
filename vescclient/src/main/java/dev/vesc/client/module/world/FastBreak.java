package dev.vesc.client.module.world;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * FastBreak – Break blocks faster.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class FastBreak extends Module {
    public FastBreak() {
        super("FastBreak", Category.WORLD, "pickaxe", "Szybsze kopanie", "Break blocks faster");
        place(Category.WORLD, "Building");
        add(
                choice("Mode", "Damage", "Packet", "Instant"),
                slider("Speed", 1.3, 1.0, 3.0, 0.05),
                toggle("Ignore Ground", false)
        );
    }
}
