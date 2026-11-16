package dev.razer.client.module;

import dev.razer.client.setting.NumberSetting;

/** Ambient – świecące kulki unoszące się w powietrzu wokół gracza (kosmetyka). */
public class Ambient extends Module {
    public final dev.razer.client.setting.ModeSetting colorMode = new dev.razer.client.setting.ModeSetting("Kolor", "Color", "Accent", "Custom");
    public final dev.razer.client.setting.ColorSetting customColor = new dev.razer.client.setting.ColorSetting("Własny kolor", "Custom Color", 0xFF4DE8FF);
    /** własny kolor albo null (= kolor akcentu) */
    public Integer custom() { return colorMode.get().equals("Custom") ? customColor.get() : null; }

    public final NumberSetting count = new NumberSetting("Ilość", "Count", 40, 10, 120, 5);
    public final NumberSetting size = new NumberSetting("Rozmiar", "Size", 1.0, 0.4, 2.5, 0.1);
    public final NumberSetting range = new NumberSetting("Zasięg", "Range", 14, 6, 32, 1);

    public Ambient() {
        super("Ambient", Category.VISUALS, "sparkles", "Świecące kulki w powietrzu", "Glowing orbs floating around");
        add(count, size, range, colorMode, customColor);
    }
}
