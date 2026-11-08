package dev.wave.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.wave.util.ColorUtil;

import java.awt.Color;

/** HSB colour with alpha and an optional rainbow mode. Value is the packed ARGB colour. */
public class ColorSetting extends Setting<Integer> {
    private float hue, saturation, brightness, alpha;
    private boolean rainbow;

    public ColorSetting(String name, int argb) { this(name, "", argb); }

    public ColorSetting(String name, String description, int argb) {
        super(name, description, argb);
        setArgb(argb);
    }

    private void setArgb(int argb) {
        float[] hsb = Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, null);
        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
        alpha = ((argb >>> 24) & 0xFF) / 255f;
        value = argb;
    }

    private void rebuild() { value = ColorUtil.hsb(hue, saturation, brightness, Math.round(alpha * 255)); }

    @Override public void set(Integer v) { setArgb(v); }
    @Override public void reset() { setArgb(defaultValue); rainbow = false; }

    public int getColor() { return getColor(0); }

    public int getColor(long offset) {
        if (rainbow) return ColorUtil.withAlpha(ColorUtil.rainbow(offset, Math.max(0.35f, saturation), Math.max(0.6f, brightness), 1f), Math.round(alpha * 255));
        return value;
    }

    public float getHue() { return hue; }
    public float getSaturation() { return saturation; }
    public float getBrightness() { return brightness; }
    public float getAlphaF() { return alpha; }
    public boolean isRainbow() { return rainbow; }

    public void setHue(float h) { hue = clamp(h); rebuild(); }
    public void setSaturation(float s) { saturation = clamp(s); rebuild(); }
    public void setBrightness(float b) { brightness = clamp(b); rebuild(); }
    public void setAlpha(float a) { alpha = clamp(a); rebuild(); }
    public void setRainbow(boolean r) { rainbow = r; }

    private static float clamp(float f) { return Math.max(0f, Math.min(1f, f)); }

    @Override
    public JsonElement toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("argb", value);
        o.addProperty("rainbow", rainbow);
        return o;
    }

    @Override
    public void fromJson(JsonElement e) {
        if (!e.isJsonObject()) return;
        JsonObject o = e.getAsJsonObject();
        if (o.has("argb")) setArgb(o.get("argb").getAsInt());
        if (o.has("rainbow")) rainbow = o.get("rainbow").getAsBoolean();
    }
}
