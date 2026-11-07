package pl.mysticclient.setting;

import java.util.List;

public class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String defaultValue, String... modes) {
        super(name, defaultValue);
        this.modes = List.of(modes);
    }

    public List<String> getModes() { return modes; }
    public boolean is(String mode) { return get().equalsIgnoreCase(mode); }

    @Override
    public void set(String value) {
        for (String m : modes) if (m.equalsIgnoreCase(value)) { super.set(m); return; }
    }
}
