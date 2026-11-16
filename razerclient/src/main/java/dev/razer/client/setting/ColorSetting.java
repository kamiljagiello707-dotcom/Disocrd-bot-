package dev.razer.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.awt.Color;

/** Kolor ARGB z wyborem w GUI (pole saturacja/jasność + pasek odcienia). */
public class ColorSetting extends Setting<Integer> {
    public float hue, sat, bri;

    public ColorSetting(String pl, String en, int argb) {
        super(pl, en, argb);
        fromArgb(argb);
    }

    private void fromArgb(int argb) {
        float[] hsb = Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, null);
        hue = hsb[0]; sat = hsb[1]; bri = hsb[2];
    }

    @Override public void set(Integer v) { value = v | 0xFF000000; fromArgb(value); }

    @Override public void reset() { set(defaultValue); }

    public void setHsb(float h, float s, float b) {
        hue = clamp(h); sat = clamp(s); bri = clamp(b);
        value = 0xFF000000 | (Color.HSBtoRGB(hue, sat, bri) & 0xFFFFFF);
    }

    public int hueColor() { return 0xFF000000 | (Color.HSBtoRGB(hue, 1, 1) & 0xFFFFFF); }

    public String hex() { return String.format("#%06X", value & 0xFFFFFF); }

    private static float clamp(float f) { return Math.max(0, Math.min(1, f)); }

    @Override public JsonElement save() { return new JsonPrimitive(value); }
    @Override public void load(JsonElement e) { set(e.getAsInt()); }
}
