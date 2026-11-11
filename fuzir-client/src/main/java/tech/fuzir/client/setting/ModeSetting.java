package tech.fuzir.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

/**
 * Single choice out of a fixed list of named modes.
 */
public final class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String defaultValue, String... modes) {
        super(name, defaultValue);
        if (modes.length == 0) throw new IllegalArgumentException("ModeSetting " + name + " needs at least one mode");
        this.modes = List.of(modes);
        if (!this.modes.contains(defaultValue)) {
            throw new IllegalArgumentException("Default mode " + defaultValue + " is not one of " + this.modes);
        }
    }

    @Override
    protected String sanitize(String input) {
        if (input == null) return null;
        for (String mode : modes) {
            if (mode.equalsIgnoreCase(input)) return mode;
        }
        return null;
    }

    public List<String> getModes() {
        return modes;
    }

    public boolean is(String mode) {
        return value.equalsIgnoreCase(mode);
    }

    public int getIndex() {
        return modes.indexOf(value);
    }

    public void cycle(int direction) {
        int size = modes.size();
        set(modes.get(Math.floorMod(getIndex() + direction, size)));
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element.isJsonPrimitive()) set(element.getAsString());
    }
}
