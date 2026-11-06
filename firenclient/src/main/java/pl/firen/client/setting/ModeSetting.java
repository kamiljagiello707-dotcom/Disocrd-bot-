package pl.firen.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

public class ModeSetting extends Setting<String> {
    private final List<String> modes;
    /** Animowana pozycja wskaźnika w GUI. */
    public float anim = -1f;

    public ModeSetting(String name, String value, String... modes) {
        super(name, value);
        this.modes = List.of(modes);
    }

    public List<String> getModes() { return modes; }
    public int index() { return modes.indexOf(value); }
    public void setIndex(int i) { value = modes.get(Math.floorMod(i, modes.size())); }

    public boolean is(String mode) { return value.equalsIgnoreCase(mode); }

    public void cycle(int dir) {
        int i = modes.indexOf(value);
        value = modes.get(Math.floorMod(i + dir, modes.size()));
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) {
        if (e.isJsonPrimitive() && modes.contains(e.getAsString())) value = e.getAsString();
    }
}
