package dev.nova.client.setting;

import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    private final String name;
    protected T value;
    private BooleanSupplier visible = () -> true;

    protected Setting(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }
    public T get() { return value; }
    public void set(T v) { value = v; }

    /** Hide this setting unless the condition is true, e.g. {@code .visibleIf(() -> mode.is("Packet"))}. */
    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleIf(BooleanSupplier cond) {
        this.visible = cond;
        return (S) this;
    }

    public boolean isVisible() { return visible.getAsBoolean(); }
}
