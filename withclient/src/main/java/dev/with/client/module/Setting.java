package dev.with.client.module;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * Ustawienie modułu: przełącznik, liczba (suwak), zakres (dwa suwaki), tryb (chipsy / lista rozwijana),
 * wielokrotny wybór, kolor, tekst albo nagłówek grupy (zwijany). Ustawienie może być widoczne tylko
 * przy spełnionym warunku (np. "Smoothness" tylko gdy Rotacje = Smooth).
 */
public abstract class Setting<T> {
    public final String name;
    protected T value;
    protected final T def;
    private BooleanSupplier visible = () -> true;

    protected Setting(String name, T def) { this.name = name; this.value = def; this.def = def; }

    public T get() { return value; }
    public void set(T v) { value = v; }
    public void reset() { value = def; }
    public boolean isDefault() { return Objects.equals(value, def); }
    public abstract JsonElement save();
    public abstract void load(JsonElement e);

    /** pokazuj tylko gdy warunek jest spełniony */
    public Setting<T> vis(BooleanSupplier s) { visible = s; return this; }
    public boolean visible() { return visible.getAsBoolean(); }

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
        @Override public void set(Double v) { value = snap(v, min, max, step); }
        public float fraction() { return (float) ((value - min) / (max - min)); }
        public String text() { return fmt(value, step, unit); }
        @Override public JsonElement save() { return new JsonPrimitive(value); }
        @Override public void load(JsonElement e) { set(e.getAsDouble()); }
    }

    /** zakres lo..hi (np. CPS 9–13) */
    public static final class Range extends Setting<double[]> {
        public final double min, max, step;
        public final String unit;
        public Range(String name, double lo, double hi, double min, double max, double step, String unit) {
            super(name, new double[]{lo, hi}); this.min = min; this.max = max; this.step = step; this.unit = unit;
            value = new double[]{lo, hi};
        }
        public double lo() { return value[0]; }
        public double hi() { return value[1]; }
        public void setLo(double v) { value[0] = Math.min(snap(v, min, max, step), value[1]); }
        public void setHi(double v) { value[1] = Math.max(snap(v, min, max, step), value[0]); }
        public float fLo() { return (float) ((value[0] - min) / (max - min)); }
        public float fHi() { return (float) ((value[1] - min) / (max - min)); }
        public String text() { return fmt(value[0], step, "") + " – " + fmt(value[1], step, unit); }
        @Override public void reset() { value = def.clone(); }
        @Override public boolean isDefault() { return Arrays.equals(value, def); }
        @Override public JsonElement save() { JsonArray a = new JsonArray(); a.add(value[0]); a.add(value[1]); return a; }
        @Override public void load(JsonElement e) {
            JsonArray a = e.getAsJsonArray();
            value = new double[]{snap(a.get(0).getAsDouble(), min, max, step), snap(a.get(1).getAsDouble(), min, max, step)};
            if (value[0] > value[1]) value = new double[]{value[1], value[0]};
        }
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
        public boolean is(String m) { return value.equals(m); }
        public void cycle(int dir) { value = modes.get(Math.floorMod(modes.indexOf(value) + dir, modes.size())); }
        @Override public JsonElement save() { return new JsonPrimitive(value); }
        @Override public void load(JsonElement e) { if (modes.contains(e.getAsString())) value = e.getAsString(); }
    }

    /** wiele opcji naraz (np. cele: Gracze, Moby, Zwierzęta) */
    public static final class Multi extends Setting<Set<String>> {
        public final List<String> options;
        public Multi(String name, String[] options, String... on) {
            super(name, new LinkedHashSet<>(List.of(on))); this.options = List.of(options);
            value = new LinkedHashSet<>(def);
        }
        public boolean has(String o) { return value.contains(o); }
        public void flip(String o) { if (!value.remove(o)) value.add(o); }
        @Override public void reset() { value = new LinkedHashSet<>(def); }
        @Override public JsonElement save() { JsonArray a = new JsonArray(); value.forEach(a::add); return a; }
        @Override public void load(JsonElement e) {
            value = new LinkedHashSet<>();
            for (JsonElement x : e.getAsJsonArray()) if (options.contains(x.getAsString())) value.add(x.getAsString());
        }
    }

    /** krótki tekst (np. wiadomość, nick) */
    public static final class Text extends Setting<String> {
        public final int maxLen;
        public Text(String name, String def, int maxLen) { super(name, def); this.maxLen = maxLen; }
        @Override public JsonElement save() { return new JsonPrimitive(value); }
        @Override public void load(JsonElement e) { String s = e.getAsString(); value = s.length() > maxLen ? s.substring(0, maxLen) : s; }
    }

    /** nagłówek grupy; wartość = rozwinięta */
    public static final class Group extends Setting<Boolean> {
        public Group(String name) { super(name, true); }
        public void toggle() { value = !value; }
        @Override public boolean isDefault() { return true; }
        @Override public JsonElement save() { return new JsonPrimitive(value); }
        @Override public void load(JsonElement e) { value = e.getAsBoolean(); }
    }

    static double snap(double v, double min, double max, double step) {
        double s = Math.round((Math.max(min, Math.min(max, v)) - min) / step) * step + min;
        return Math.max(min, Math.min(max, Math.round(s * 1000) / 1000.0));
    }

    static String fmt(double v, double step, String unit) {
        boolean whole = step >= 1 && Math.abs(v - Math.round(v)) < 1e-6;
        String s = whole ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, step >= 0.1 ? "%.1f" : "%.2f", v);
        return unit.isEmpty() ? s : s + " " + unit;
    }
}
