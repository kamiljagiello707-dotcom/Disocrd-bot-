package dev.wave.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String name, boolean def) { this(name, "", def); }
    public BooleanSetting(String name, String description, boolean def) { super(name, description, def); }

    public boolean isOn() { return value; }
    public void toggle() { value = !value; }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { if (e.isJsonPrimitive()) value = e.getAsBoolean(); }
}
