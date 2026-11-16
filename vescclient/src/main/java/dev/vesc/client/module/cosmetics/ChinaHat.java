package dev.vesc.client.module.cosmetics;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

import dev.vesc.client.setting.BoolSetting;
import dev.vesc.client.setting.NumberSetting;

/** ChinaHat – świecący stożkowy kapelusz nad głową (kosmetyka, widoczny u Ciebie). */
public class ChinaHat extends Module {
    public final dev.vesc.client.setting.ModeSetting colorMode = new dev.vesc.client.setting.ModeSetting("Kolor", "Color", "Accent", "Custom");
    public final dev.vesc.client.setting.ColorSetting customColor = new dev.vesc.client.setting.ColorSetting("Własny kolor", "Custom Color", 0xFFFF6BC8);
    /** własny kolor albo null (= kolor akcentu) */
    public Integer custom() { return colorMode.get().equals("Custom") ? customColor.get() : null; }

    public final NumberSetting size = new NumberSetting("Rozmiar", "Size", 0.65, 0.3, 1.2, 0.05);
    public final NumberSetting height = new NumberSetting("Wysokość", "Height", 0.32, 0.1, 0.8, 0.02);
    public final BoolSetting friends = new BoolSetting("Na znajomych", "On friends", true);
    public final BoolSetting spin = new BoolSetting("Obrót", "Spin", true);

    public ChinaHat() {
        super("ChinaHat", Category.VISUALS, "sparkles", "Świecący kapelusz nad głową", "Glowing hat above your head");
        place(Category.COSMETICS, "Wearables");
        add(size, height, friends, spin, colorMode, customColor);
    }
}
