package dev.vesc.client.module.visuals;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * ESP – Player outlines.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class ESP extends Module {
    public ESP() {
        super("ESP", Category.VISUALS, "scan", "Obrysy graczy", "Player outlines");
        place(Category.VISUALS, "Entities");
        add(
                choice("Mode", "Box", "2D", "Outline", "Glow", "Shader"),
                slider("Line Width", 1.5, 0.5, 5.0, 0.5),
                slider("Fill Opacity", 25, 0, 100, 5),
                slider("Range", 128, 16, 512, 16),
                toggle("Players", true),
                toggle("Mobs", false),
                toggle("Animals", false),
                toggle("Items", false),
                toggle("Health Bar", true),
                toggle("Friends Color", true),
                toggle("Self", false)
        );
    }
}
