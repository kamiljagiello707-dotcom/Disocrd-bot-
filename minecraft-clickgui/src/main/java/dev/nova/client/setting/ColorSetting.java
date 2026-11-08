package dev.nova.client.setting;

/** Stores a hue (0..1); the GUI renders it as a hue slider. */
public class ColorSetting extends Setting<Float> {
    public ColorSetting(String name, float hue) { super(name, hue); }

    public int rgb() { return 0xFF000000 | (java.awt.Color.HSBtoRGB(value, 0.75f, 1f) & 0xFFFFFF); }
}
