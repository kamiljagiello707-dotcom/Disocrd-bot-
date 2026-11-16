package dev.vesc.client.module.world;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * Nuker – Breaks blocks around.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class Nuker extends Module {
    public Nuker() {
        super("Nuker", Category.WORLD, "bomb", "Niszczy bloki wokół", "Breaks blocks around");
        place(Category.WORLD, "Building");
        add(
                choice("Mode", "All", "Flatten", "Smash", "Filter"),
                slider("Range", 4, 1, 6, 1),
                slider("Delay", 0, 0, 10, 1),
                choice("Rotations", "Silent", "Legit", "None"),
                toggle("Swing", true),
                toggle("Only Selected", false)
        );
    }
}
