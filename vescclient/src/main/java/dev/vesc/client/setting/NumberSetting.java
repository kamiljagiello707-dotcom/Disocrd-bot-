package dev.vesc.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
    public final double min, max, step;
    public NumberSetting(String pl, String en, double value, double min, double max, double step) {
        super(pl, en, value); this.min = min; this.max = max; this.step = step;
    }
    @Override public void set(Double v) {
        double c = Math.max(min, Math.min(max, v));
        value = Math.round(c / step) * step;
    }
    public double percent() { return (value - min) / (max - min); }
    public String display() {
        if (step >= 1) return String.valueOf((int) Math.round(value));
        int decimals = step >= 0.1 ? 1 : 2;
        return String.format(java.util.Locale.ROOT, "%." + decimals + "f", value);
    }
    @Override public JsonElement save() { return new JsonPrimitive(value); }
    @Override public void load(JsonElement e) { set(e.getAsDouble()); }
}
