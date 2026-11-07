package pl.mysticclient.module.world;

import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;

public class NoWeather extends Module {
    public NoWeather() {
        super("NoWeather", "Wyłącza deszcz i burzę po stronie klienta.", Category.WORLD);
    }
}
