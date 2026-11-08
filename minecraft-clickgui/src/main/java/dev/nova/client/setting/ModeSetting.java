package dev.nova.client.setting;

import java.util.List;

public class ModeSetting extends Setting<String> {
    public final List<String> modes;

    public ModeSetting(String name, String value, String... modes) {
        super(name, value);
        this.modes = List.of(modes);
    }

    public boolean is(String mode) { return value.equalsIgnoreCase(mode); }

    public void cycle(int dir) {
        int i = modes.indexOf(value);
        value = modes.get(Math.floorMod(i + dir, modes.size()));
    }
}
