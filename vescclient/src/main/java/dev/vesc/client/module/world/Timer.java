package dev.vesc.client.module.world;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * Timer – Changes game speed.
 * Atrapa: tylko przełącznik z ustawieniami w GUI – bez żadnej logiki w grze.
 */
public class Timer extends Module {
    public Timer() {
        super("Timer", Category.WORLD, "timer", "Zmienia prędkość gry", "Changes game speed");
        place(Category.WORLD, "Automation");
        add(
                slider("Speed", 1.0, 0.1, 5.0, 0.05),
                toggle("Only Moving", false),
                toggle("Pause In Liquids", true)
        );
    }
}
