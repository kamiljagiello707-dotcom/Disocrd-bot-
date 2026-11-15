package dev.razer.client.module;

import dev.razer.client.setting.NumberSetting;

/** Ambient – świecące kulki unoszące się w powietrzu wokół gracza (kosmetyka). */
public class Ambient extends Module {
    public final NumberSetting count = new NumberSetting("Ilość", "Count", 40, 10, 120, 5);
    public final NumberSetting size = new NumberSetting("Rozmiar", "Size", 1.0, 0.4, 2.5, 0.1);
    public final NumberSetting range = new NumberSetting("Zasięg", "Range", 14, 6, 32, 1);

    public Ambient() {
        super("Ambient", Category.RENDER, "sparkles", "Świecące kulki w powietrzu", "Glowing orbs floating around");
        add(count, size, range);
    }
}
