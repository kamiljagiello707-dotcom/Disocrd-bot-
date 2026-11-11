package tech.fuzir.client.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    public final String name;
    protected T value;
    private BooleanSupplier visible = () -> true;

    protected Setting(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public T get() {
        return value;
    }

    public void set(T v) {
        value = v;
    }

    /** Ustawienie widoczne tylko, gdy warunek jest spełniony (np. zależy od trybu). */
    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(BooleanSupplier cond) {
        this.visible = cond;
        return (S) this;
    }

    public boolean isVisible() {
        return visible.getAsBoolean();
    }

    public abstract JsonElement save();

    public abstract void load(JsonElement e);
}
