package tech.fuzir.client.setting;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A named, observable, serializable module option.
 */
public abstract class Setting<T> {
    private final String name;
    private final T defaultValue;
    private final List<Consumer<T>> listeners = new ArrayList<>();
    private BooleanSupplier visibility = () -> true;
    protected T value;

    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public T get() {
        return value;
    }

    public T getDefault() {
        return defaultValue;
    }

    public void set(T newValue) {
        T sanitized = sanitize(newValue);
        if (sanitized == null || sanitized.equals(value)) return;
        value = sanitized;
        for (Consumer<T> listener : listeners) listener.accept(value);
    }

    public void reset() {
        set(defaultValue);
    }

    /** Clamps / validates incoming values. Returning {@code null} rejects the value. */
    protected T sanitize(T input) {
        return input;
    }

    public boolean isVisible() {
        return visibility.getAsBoolean();
    }

    /** Only show this setting in the GUI while {@code condition} holds. */
    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(BooleanSupplier condition) {
        this.visibility = condition;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S onChange(Consumer<T> listener) {
        listeners.add(listener);
        return (S) this;
    }

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement element);
}
