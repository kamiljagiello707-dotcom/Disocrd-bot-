package pl.firen.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BooleanSetting extends Setting<Boolean> {
    public float anim;

    public BooleanSetting(String name, boolean value) {
        super(name, value);
        this.anim = value ? 1f : 0f;
    }

    public void toggle() { value = !value; }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { if (e.isJsonPrimitive()) value = e.getAsBoolean(); }
}
