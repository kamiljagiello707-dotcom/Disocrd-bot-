package dev.vesc.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting<String> {
    public final List<String> modes;
    public ModeSetting(String pl, String en, String... modes) { super(pl, en, modes[0]); this.modes = Arrays.asList(modes); }
    public void cycle(int dir) {
        int i = modes.indexOf(value);
        value = modes.get(Math.floorMod(i + dir, modes.size()));
    }
    @Override public JsonElement save() { return new JsonPrimitive(value); }
    @Override public void load(JsonElement e) { if (modes.contains(e.getAsString())) value = e.getAsString(); }
}
