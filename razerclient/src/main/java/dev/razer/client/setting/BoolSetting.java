package dev.razer.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String pl, String en, boolean value) { super(pl, en, value); anim.snap(value ? 1 : 0); }
    public void toggle() { value = !value; }
    @Override public JsonElement save() { return new JsonPrimitive(value); }
    @Override public void load(JsonElement e) { value = e.getAsBoolean(); }
}
