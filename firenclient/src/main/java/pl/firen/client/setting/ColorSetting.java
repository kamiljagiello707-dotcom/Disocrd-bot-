package pl.firen.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.awt.Color;

/** Kolor (ARGB) z wyborem HSV w GUI. */
public class ColorSetting extends Setting<Integer> {
    private float hue, sat, bri;
    public boolean open;
    public float openAnim;

    public ColorSetting(String name, int argb) {
        super(name, argb);
        syncHsv();
    }

    private void syncHsv() {
        float[] hsv = Color.RGBtoHSB(value >> 16 & 255, value >> 8 & 255, value & 255, null);
        hue = hsv[0]; sat = hsv[1]; bri = hsv[2];
    }

    @Override
    public void set(Integer v) {
        value = v;
        syncHsv();
    }

    public float hue() { return hue; }
    public float sat() { return sat; }
    public float bri() { return bri; }

    public void setHsv(float h, float s, float b) {
        hue = Math.max(0, Math.min(1, h));
        sat = Math.max(0, Math.min(1, s));
        bri = Math.max(0, Math.min(1, b));
        value = (value & 0xFF000000) | (Color.HSBtoRGB(hue, sat, bri) & 0xFFFFFF);
    }

    /** Kolor z podaną przezroczystością (0–255). */
    public int withAlpha(int alpha) { return (alpha & 255) << 24 | (value & 0xFFFFFF); }

    @Override public JsonElement toJson() { return new JsonPrimitive(String.format("#%08X", value)); }

    @Override
    public void fromJson(JsonElement e) {
        try {
            set((int) Long.parseLong(e.getAsString().replace("#", ""), 16));
        } catch (Exception ignored) {
        }
    }
}
