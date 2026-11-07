package dev.mustic.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
    private final double min, max, step;

    public NumberSetting(String name, double value, double min, double max, double step) {
        super(name, value);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    @Override
    public void set(Double v) {
        double clamped = Math.max(min, Math.min(max, v));
        double stepped = Math.round(clamped / step) * step;
        this.value = Math.round(stepped * 1000.0) / 1000.0;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }
    public int getInt() { return (int) Math.round(value); }
    public float getFloat() { return value.floatValue(); }

    public String format() {
        return step >= 1 ? String.valueOf(getInt()) : String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { if (e.isJsonPrimitive()) set(e.getAsDouble()); }
}
