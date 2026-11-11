package tech.fuzir.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import tech.fuzir.client.util.Anim;

import java.util.List;

public class ModeSetting extends Setting<String> {
    public final List<String> modes;
    /** Pozycja podświetlenia w przełączniku segmentowym. */
    public final Anim anim = new Anim(-1);

    public ModeSetting(String name, String value, String... modes) {
        super(name, value);
        this.modes = List.of(modes);
    }

    public boolean is(String m) {
        return value.equals(m);
    }

    public int index() {
        return modes.indexOf(value);
    }

    public void cycle(int dir) {
        int i = modes.indexOf(value);
        value = modes.get(Math.floorMod(i + dir, modes.size()));
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement e) {
        if (e.isJsonPrimitive() && modes.contains(e.getAsString())) value = e.getAsString();
    }
}
