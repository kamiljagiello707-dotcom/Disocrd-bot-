package dev.vesc.client.module.world;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * AutoSign – Writes signs automatically.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class AutoSign extends Module {
    public AutoSign() {
        super("AutoSign", Category.WORLD, "i_pen", "Automatyczny tekst na tabliczkach", "Writes signs automatically");
        place(Category.WORLD, "Building");
        add(
                choice("Lines", "Custom", "Coords", "Date"),
                toggle("Glow", false),
                toggle("Waterproof", true)
        );
    }
}
