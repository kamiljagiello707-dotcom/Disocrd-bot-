package dev.razer.client.module;

import dev.razer.client.setting.BoolSetting;
import dev.razer.client.setting.NumberSetting;

/** ChinaHat – świecący stożkowy kapelusz nad głową (kosmetyka, widoczny u Ciebie). */
public class ChinaHat extends Module {
    public final NumberSetting size = new NumberSetting("Rozmiar", "Size", 0.65, 0.3, 1.2, 0.05);
    public final NumberSetting height = new NumberSetting("Wysokość", "Height", 0.32, 0.1, 0.8, 0.02);
    public final BoolSetting friends = new BoolSetting("Na znajomych", "On friends", true);
    public final BoolSetting spin = new BoolSetting("Obrót", "Spin", true);

    public ChinaHat() {
        super("ChinaHat", Category.RENDER, "sparkles", "Świecący kapelusz nad głową", "Glowing hat above your head");
        add(size, height, friends, spin);
    }
}
