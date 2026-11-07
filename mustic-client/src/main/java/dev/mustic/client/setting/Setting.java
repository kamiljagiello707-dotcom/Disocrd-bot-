package dev.mustic.client.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    private final String name;
    protected T value;
    private BooleanSupplier visibility = () -> true;

    protected Setting(String name, T value) {
        this.name = name;
        this.value = value;
    }

    /** Pokazuj ustawienie tylko gdy warunek jest spelniony. */
    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(BooleanSupplier supplier) {
        this.visibility = supplier;
        return (S) this;
    }

    public boolean isVisible() { return visibility.getAsBoolean(); }
    public String getName() { return name; }
    public T get() { return value; }
    public void set(T value) { this.value = value; }

    public abstract JsonElement toJson();
    public abstract void fromJson(JsonElement element);
}
