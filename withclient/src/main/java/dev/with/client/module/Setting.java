package dev.with.client.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

/** Ustawienie modułu: przełącznik, liczba (suwak) albo tryb (lista). */
public abstract class Setting<T> {
    public final String name;
    protected T value;
    protected final T def;

    protected Setting(String name, T def) { this.name = name; this.value = def; this.def = def; }

    public T get() { return value; }
    public void set(T v) { value = v; }
    public void reset() { value = def; }
    public boolean isDefault() { return value.equals(def); }
    public abstract JsonElement save();
    public abstract void load(JsonElement e);

    /** przełącznik */
    public static final class Bool extends Setting<Boolean> {
        public Bool(String name, boolean def) { super(name, def); }
        public void toggle() { value = !value; }
        @Override public JsonElement save() { return new JsonPrimitive(value); }
        @Override public void load(JsonElement e) { value = e.getAsBoolean(); }
    }

    /** liczba z zakresu min..max z krokiem step; unit – dopisek (np. "ms", "%") */
    public static final class Num extends Setting<Double> {
        public final double min, max, step;
        public final String unit;
        public Num(String name, double def, double min, double max, double step, String unit) {
            super(name, def); this.min = min; this.max = max; this.step = step; this.unit = unit;
        }
        @Override public void set(Double v) {
            double s = Math.round((Math.max(min, Math.min(max, v)) - min) / step) * step + min;
            value = Math.max(min, Math.min(max, Math.round(s * 1000) / 1000.0));
        }
        public float fraction() { return (float) ((value - min) / (max - min)); }
        public String text() {
            boolean whole = step >= 1 && Math.abs(value - Math.round(value)) < 1e-6;
            String v = whole ? String.valueOf(Math.round(value)) : String.format(java.util.Locale.ROOT, step >= 0.1 ? "%.1f" : "%.2f", value);
            return unit.isEmpty() ? v : v + " " + unit;
        }
        @Override public JsonElement save() { return new JsonPrimitive(value); }
        @Override public void load(JsonElement e) { set(e.getAsDouble()); }
    }

    /** kolor ARGB (picker w GUI) */
    public static final class Color extends Setting<Integer> {
        public Color(String name, int def) { super(name, def); }
        @Override public JsonElement save() { return new JsonPrimitive(value); }
        @Override public void load(JsonElement e) { value = e.getAsInt(); }
    }

    /** jeden z kilku trybów */
    public static final class Mode extends Setting<String> {
        public final List<String> modes;
        public Mode(String name, String... modes) { super(name, modes[0]); this.modes = List.of(modes); }
        public void cycle(int dir) { value = modes.get(Math.floorMod(modes.indexOf(value) + dir, modes.size())); }
        @Override public JsonElement save() { return new JsonPrimitive(value); }
        @Override public void load(JsonElement e) { if (modes.contains(e.getAsString())) value = e.getAsString(); }
    }
}
