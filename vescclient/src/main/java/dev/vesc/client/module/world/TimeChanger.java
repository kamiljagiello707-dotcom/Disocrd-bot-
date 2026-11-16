package dev.vesc.client.module.world;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * TimeChanger – Client-side time of day.
 * Działa – logika w {@link dev.vesc.client.module.Legit} (po stronie klienta, wizualnie / dla wygody).
 */
public class TimeChanger extends Module {
    public TimeChanger() {
        super("TimeChanger", Category.WORLD, "i_time", "Własna pora dnia (tylko u Ciebie)", "Client-side time of day");
        place(Category.WORLD, "Atmosphere");
        add(
                choice("Time", "Day", "Sunset", "Night", "Midnight", "Custom"),
                slider("Custom Time", 6000, 0, 24000, 500),
                toggle("Animate", true)
        );
    }
}
