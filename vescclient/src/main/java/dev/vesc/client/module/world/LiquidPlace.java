package dev.vesc.client.module.world;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * LiquidPlace – Place blocks on liquids.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class LiquidPlace extends Module {
    public LiquidPlace() {
        super("LiquidPlace", Category.WORLD, "i_drops", "Stawianie bloków na wodzie", "Place blocks on liquids");
        place(Category.WORLD, "Building");
        add(
                toggle("Water", true),
                toggle("Lava", true),
                toggle("Air Place", false)
        );
    }
}
