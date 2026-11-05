package pl.firen.client.module.modules;

import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.ModeSetting;

public class Criticals extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Jump", "Jump", "Packet"));

    public Criticals() { super("Criticals", "Każdy cios jest ciosem krytycznym.", Category.COMBAT); }

    @Override public String getSuffix() { return mode.get(); }
}
