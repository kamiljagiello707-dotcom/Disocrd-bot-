package dev.vesc.client.module.world;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * Scaffold – Places blocks below.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class Scaffold extends Module {
    public Scaffold() {
        super("Scaffold", Category.WORLD, "construction", "Stawia bloki pod nogami", "Places blocks below");
        place(Category.WORLD, "Building");
        add(
                choice("Mode", "Normal", "Telly", "Godbridge", "Keep Y"),
                choice("Rotations", "Silent", "Legit", "Snap"),
                slider("Delay", 0, 0, 10, 1),
                slider("Expand", 0, 0, 5, 1),
                toggle("Tower", true),
                toggle("Safe Walk", true),
                toggle("Sprint", false),
                toggle("Swing", true),
                toggle("Block Counter", true)
        );
    }
}
