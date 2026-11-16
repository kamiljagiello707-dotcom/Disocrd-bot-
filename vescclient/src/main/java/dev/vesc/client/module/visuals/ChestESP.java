package dev.vesc.client.module.visuals;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * ChestESP – Highlights chests.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class ChestESP extends Module {
    public ChestESP() {
        super("ChestESP", Category.VISUALS, "package", "Podświetlanie skrzyń", "Highlights chests");
        place(Category.VISUALS, "Blocks");
        add(
                choice("Mode", "Box", "Outline", "Glow"),
                slider("Range", 64, 16, 256, 8),
                toggle("Chests", true),
                toggle("Ender Chests", true),
                toggle("Shulkers", true),
                toggle("Barrels", false),
                toggle("Spawners", false),
                toggle("Tracers", false)
        );
    }
}
