package tech.fuzir.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import tech.fuzir.client.util.Anim;

public class NumberSetting extends Setting<Double> {
    public final double min, max, step;
    public final String unit;
    public final Anim anim;

    public NumberSetting(String name, double value, double min, double max, double step, String unit) {
        super(name, value);
        this.min = min;
        this.max = max;
        this.step = step;
        this.unit = unit;
        this.anim = new Anim((float) ratio());
    }

    public float f() {
        return value.floatValue();
    }

    public double ratio() {
        return (value - min) / (max - min);
    }

    @Override
    public void set(Double v) {
        double s = Math.round((v - min) / step) * step + min;
        value = Math.max(min, Math.min(max, Math.round(s * 1000.0) / 1000.0));
    }

    public void setRatio(double r) {
        set(min + (max - min) * Math.max(0, Math.min(1, r)));
    }

    public String display() {
        String n = step >= 1 ? String.valueOf(Math.round(value)) : String.format(java.util.Locale.ROOT, step >= 0.1 ? "%.1f" : "%.2f", value);
        return n.replace('.', ',') + unit;
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement e) {
        if (e.isJsonPrimitive()) set(e.getAsDouble());
    }
}
