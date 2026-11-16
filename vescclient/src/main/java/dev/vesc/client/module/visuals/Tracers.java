package dev.vesc.client.module.visuals;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * Tracers – Lines to players.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class Tracers extends Module {
    public Tracers() {
        super("Tracers", Category.VISUALS, "radar", "Linie do graczy", "Lines to players");
        place(Category.VISUALS, "Entities");
        add(
                choice("Origin", "Crosshair", "Feet", "Top"),
                slider("Width", 1.0, 0.5, 4.0, 0.5),
                slider("Opacity", 80, 10, 100, 5),
                toggle("Players", true),
                toggle("Mobs", false),
                toggle("Distance Color", true),
                toggle("Ignore Friends", false)
        );
    }
}
