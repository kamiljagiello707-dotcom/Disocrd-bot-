package tech.fuzir.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Bounded numeric option snapped to a fixed increment.
 */
public final class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;
    private final int decimals;
    private final String unit;

    public NumberSetting(String name, double defaultValue, double min, double max, double step, String unit) {
        super(name, defaultValue);
        if (min >= max) throw new IllegalArgumentException("min must be smaller than max for " + name);
        if (step <= 0) throw new IllegalArgumentException("step must be positive for " + name);
        this.min = min;
        this.max = max;
        this.step = step;
        this.unit = unit == null ? "" : unit;
        this.decimals = Math.max(0, BigDecimal.valueOf(step).stripTrailingZeros().scale());
        this.value = sanitize(defaultValue);
    }

    public NumberSetting(String name, double defaultValue, double min, double max, double step) {
        this(name, defaultValue, min, max, step, "");
    }

    @Override
    protected Double sanitize(Double input) {
        if (input == null || input.isNaN()) return null;
        double clamped = Math.max(min, Math.min(max, input));
        double snapped = min + Math.round((clamped - min) / step) * step;
        snapped = Math.max(min, Math.min(max, snapped));
        return BigDecimal.valueOf(snapped).setScale(decimals, RoundingMode.HALF_UP).doubleValue();
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    public float getFloat() {
        return value.floatValue();
    }

    public int getInt() {
        return (int) Math.round(value);
    }

    /** Position of the current value inside the range, 0..1. */
    public double getProgress() {
        return (value - min) / (max - min);
    }

    public void setProgress(double progress) {
        set(min + Math.max(0.0, Math.min(1.0, progress)) * (max - min));
    }

    public void increment(int direction) {
        set(value + step * direction);
    }

    public String format() {
        return BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).toPlainString() + unit;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) set(element.getAsDouble());
    }
}
