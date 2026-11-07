package dev.mustic.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String value, String... modes) {
        super(name, value);
        this.modes = Arrays.asList(modes);
    }

    public boolean is(String mode) { return value.equalsIgnoreCase(mode); }
    public List<String> getModes() { return modes; }

    public void cycle(boolean forward) {
        int i = modes.indexOf(value);
        int n = modes.size();
        value = modes.get(((i + (forward ? 1 : -1)) % n + n) % n);
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) {
        if (e.isJsonPrimitive() && modes.contains(e.getAsString())) value = e.getAsString();
    }
}
