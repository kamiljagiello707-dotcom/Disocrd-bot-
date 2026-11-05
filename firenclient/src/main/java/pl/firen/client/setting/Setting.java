package pl.firen.client.setting;

import com.google.gson.JsonElement;

/** Bazowe ustawienie modułu, widoczne w ClickGUI po kliknięciu prawym przyciskiem. */
public abstract class Setting<T> {
    private final String name;
    protected T value;

    protected Setting(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }
    public T get() { return value; }
    public void set(T value) { this.value = value; }

    public abstract JsonElement toJson();
    public abstract void fromJson(JsonElement element);
}
