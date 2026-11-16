package dev.vesc.client.module.world;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

/**
 * Weather – Client-side weather.
 * Działa – logika w {@link dev.vesc.client.module.Legit} (po stronie klienta, wizualnie / dla wygody).
 */
public class Weather extends Module {
    public Weather() {
        super("Weather", Category.WORLD, "i_rain", "Własna pogoda (tylko u Ciebie)", "Client-side weather");
        place(Category.WORLD, "Atmosphere");
        add(
                choice("Weather", "Clear", "Rain", "Thunder"),
                slider("Intensity", 100, 0, 100, 5)
        );
    }
}
