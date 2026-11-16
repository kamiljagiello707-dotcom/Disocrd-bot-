package dev.razer.client.setting;

import com.google.gson.JsonElement;
import dev.razer.client.lang.Lang;
import dev.razer.client.ui.Anim;

public abstract class Setting<T> {
    private final String namePl, nameEn;
    protected T value;
    protected final T defaultValue;
    /** animacja używana przez widget w GUI */
    public final Anim anim = new Anim(0, 14);

    protected Setting(String namePl, String nameEn, T value) {
        this.namePl = namePl; this.nameEn = nameEn; this.value = value; this.defaultValue = value;
    }

    public String getName() { return Lang.pl() ? namePl : nameEn; }
    public String getId() { return nameEn.toLowerCase().replace(' ', '_'); }
    public T get() { return value; }
    public void set(T v) { value = v; }
    public void reset() { value = defaultValue; }
    public boolean changed() { return !java.util.Objects.equals(value, defaultValue); }
    public T getDefault() { return defaultValue; }

    public abstract JsonElement save();
    public abstract void load(JsonElement e);
}
