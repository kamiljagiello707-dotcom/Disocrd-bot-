package pl.mysticclient.setting;

public class NumberSetting extends Setting<Double> {
    private final double min, max, step;

    public NumberSetting(String name, double defaultValue, double min, double max, double step) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    @Override
    public void set(Double v) {
        double clamped = Math.max(min, Math.min(max, v));
        double stepped = Math.round(clamped / step) * step;
        super.set(Math.round(stepped * 1000.0) / 1000.0);
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }
    public int getInt() { return (int) Math.round(get()); }
    public float getFloat() { return get().floatValue(); }

    public String display() {
        return step >= 1 ? String.valueOf(getInt()) : String.format(java.util.Locale.ROOT, step >= 0.1 ? "%.1f" : "%.2f", get());
    }
}
