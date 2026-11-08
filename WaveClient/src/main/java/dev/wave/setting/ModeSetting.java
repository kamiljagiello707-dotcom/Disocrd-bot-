package dev.wave.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String description, String def, String... modes) {
        super(name, description, def);
        this.modes = Arrays.asList(modes);
    }

    public List<String> getModes() { return modes; }
    public boolean is(String mode) { return value.equalsIgnoreCase(mode); }
    public int index() { return Math.max(0, modes.indexOf(value)); }

    public void cycle(int dir) {
        int i = (index() + dir + modes.size()) % modes.size();
        value = modes.get(i);
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override
    public void fromJson(JsonElement e) {
        if (!e.isJsonPrimitive()) return;
        String s = e.getAsString();
        for (String m : modes) if (m.equalsIgnoreCase(s)) value = m;
    }
}
