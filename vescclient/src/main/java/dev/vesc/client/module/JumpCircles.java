package dev.vesc.client.module;

import dev.vesc.client.setting.NumberSetting;

/** JumpCircles – kręgi rozchodzące się pod nogami przy skoku (kosmetyka). */
public class JumpCircles extends Module {
    public final dev.vesc.client.setting.ModeSetting colorMode = new dev.vesc.client.setting.ModeSetting("Kolor", "Color", "Accent", "Custom");
    public final dev.vesc.client.setting.ColorSetting customColor = new dev.vesc.client.setting.ColorSetting("Własny kolor", "Custom Color", 0xFF4D8BFF);
    /** własny kolor albo null (= kolor akcentu) */
    public Integer custom() { return colorMode.get().equals("Custom") ? customColor.get() : null; }

    public final NumberSetting size = new NumberSetting("Rozmiar", "Size", 1.2, 0.5, 3.0, 0.1);
    public final NumberSetting time = new NumberSetting("Czas", "Duration", 1.2, 0.4, 3.0, 0.1);

    public JumpCircles() {
        super("JumpCircles", Category.VISUALS, "radar", "Kręgi pod nogami przy skoku", "Rings under your feet when jumping");
        add(size, time, colorMode, customColor);
    }
}
