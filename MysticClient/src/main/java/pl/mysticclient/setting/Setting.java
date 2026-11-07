package pl.mysticclient.setting;

import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    private final String name;
    private final T defaultValue;
    protected T value;
    private BooleanSupplier visibility = () -> true;

    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName() { return name; }
    public T get() { return value; }
    public void set(T value) { this.value = value; }
    public void reset() { this.value = defaultValue; }
    public boolean isVisible() { return visibility.getAsBoolean(); }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(BooleanSupplier supplier) {
        this.visibility = supplier;
        return (S) this;
    }
}
