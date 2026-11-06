package pl.firen.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
    private final double min, max, step;
    /** Animowana pozycja suwaka w GUI. */
    public float anim = -1f;

    public NumberSetting(String name, double value, double min, double max, double step) {
        super(name, value);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }

    @Override
    public void set(Double v) {
        double snapped = Math.round(v / step) * step;
        value = Math.max(min, Math.min(max, snapped));
    }

    /** Ustawia wartość z pozycji suwaka 0..1. */
    public void setFromPercent(double p) { set(min + (max - min) * Math.max(0, Math.min(1, p))); }
    public double getPercent() { return (value - min) / (max - min); }

    public String display() {
        if (step >= 1) return String.valueOf((long) Math.round(value));
        int decimals = step >= 0.1 ? 1 : 2;
        return String.format(java.util.Locale.ROOT, "%." + decimals + "f", value);
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { if (e.isJsonPrimitive()) set(e.getAsDouble()); }
}
