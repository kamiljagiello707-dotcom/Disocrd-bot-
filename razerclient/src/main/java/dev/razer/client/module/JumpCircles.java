package dev.razer.client.module;

import dev.razer.client.setting.NumberSetting;

/** JumpCircles – kręgi rozchodzące się pod nogami przy skoku (kosmetyka). */
public class JumpCircles extends Module {
    public final dev.razer.client.setting.ModeSetting colorMode = new dev.razer.client.setting.ModeSetting("Kolor", "Color", "Accent", "Custom");
    public final dev.razer.client.setting.ColorSetting customColor = new dev.razer.client.setting.ColorSetting("Własny kolor", "Custom Color", 0xFF4D8BFF);
    /** własny kolor albo null (= kolor akcentu) */
    public Integer custom() { return colorMode.get().equals("Custom") ? customColor.get() : null; }

    public final NumberSetting size = new NumberSetting("Rozmiar", "Size", 1.2, 0.5, 3.0, 0.1);
    public final NumberSetting time = new NumberSetting("Czas", "Duration", 1.2, 0.4, 3.0, 0.1);

    public JumpCircles() {
        super("JumpCircles", Category.RENDER, "radar", "Kręgi pod nogami przy skoku", "Rings under your feet when jumping");
        add(size, time, colorMode, customColor);
    }
}
