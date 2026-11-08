package dev.wave.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
    private final double min, max, step;
    private final String unit;

    public NumberSetting(String name, double def, double min, double max, double step) { this(name, "", def, min, max, step, ""); }

    public NumberSetting(String name, String description, double def, double min, double max, double step, String unit) {
        super(name, description, def);
        this.min = min;
        this.max = max;
        this.step = step;
        this.unit = unit;
    }

    @Override
    public void set(Double v) {
        double snapped = Math.round(v / step) * step;
        this.value = Math.max(min, Math.min(max, snapped));
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }
    public float getFloat() { return value.floatValue(); }
    public int getInt() { return (int) Math.round(value); }

    public String display() {
        String s;
        if (step >= 1) s = String.valueOf((long) Math.round(value));
        else if (step >= 0.1) s = String.format(java.util.Locale.ROOT, "%.1f", value);
        else s = String.format(java.util.Locale.ROOT, "%.2f", value);
        return s + unit;
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { if (e.isJsonPrimitive()) set(e.getAsDouble()); }
}
