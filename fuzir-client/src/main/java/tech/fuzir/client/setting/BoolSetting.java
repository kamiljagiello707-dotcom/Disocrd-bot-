package tech.fuzir.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import tech.fuzir.client.util.Anim;

public class BoolSetting extends Setting<Boolean> {
    public final Anim anim;

    public BoolSetting(String name, boolean value) {
        super(name, value);
        anim = new Anim(value ? 1 : 0);
    }

    public boolean on() {
        return value;
    }

    public void toggle() {
        value = !value;
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement e) {
        if (e.isJsonPrimitive()) value = e.getAsBoolean();
    }
}
