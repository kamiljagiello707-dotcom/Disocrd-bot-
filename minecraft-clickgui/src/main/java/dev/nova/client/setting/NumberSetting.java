package dev.nova.client.setting;

public class NumberSetting extends Setting<Double> {
    public final double min, max, step;

    public NumberSetting(String name, double value, double min, double max, double step) {
        super(name, value);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    @Override
    public void set(Double v) {
        double clamped = Math.max(min, Math.min(max, v));
        value = Math.round(clamped / step) * step;
    }

    public String display() {
        return step >= 1 ? String.valueOf((long) Math.round(value)) : String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
