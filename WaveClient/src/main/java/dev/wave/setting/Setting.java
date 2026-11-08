package dev.wave.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    private final String name;
    private final String description;
    protected T value;
    protected final T defaultValue;
    private BooleanSupplier visibility = () -> true;

    protected Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public T get() { return value; }
    public void set(T value) { this.value = value; }
    public void reset() { this.value = defaultValue; }

    public boolean isVisible() { return visibility.getAsBoolean(); }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visible(BooleanSupplier supplier) {
        this.visibility = supplier;
        return (S) this;
    }

    public abstract JsonElement toJson();
    public abstract void fromJson(JsonElement element);
}
