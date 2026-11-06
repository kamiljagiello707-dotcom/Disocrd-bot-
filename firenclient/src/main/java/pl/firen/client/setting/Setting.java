package pl.firen.client.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

/** Bazowe ustawienie modułu, widoczne w ClickGUI po kliknięciu prawym przyciskiem. */
public abstract class Setting<T> {
    private final String name;
    protected T value;
    private BooleanSupplier visible = () -> true;

    protected Setting(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }

    /** Pokazuj w GUI tylko, gdy warunek jest spełniony (np. zależnie od trybu). */
    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(BooleanSupplier condition) {
        this.visible = condition;
        return (S) this;
    }

    public boolean isVisible() { return visible.getAsBoolean(); }
    public T get() { return value; }
    public void set(T value) { this.value = value; }

    public abstract JsonElement toJson();
    public abstract void fromJson(JsonElement element);
}
