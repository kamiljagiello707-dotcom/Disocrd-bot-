package dev.nova.client.module.impl;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.setting.*;

public class KillAura extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Single", "Single", "Switch", "Multi"));
    public final NumberSetting range = add(new NumberSetting("Range", 3.5, 3.0, 6.0, 0.1));
    public final NumberSetting cps = add(new NumberSetting("CPS", 12, 1, 20, 1));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", false));
    public final NumberSetting switchDelay = add(new NumberSetting("Switch Delay", 150, 0, 1000, 10).visibleIf(() -> mode.is("Switch")));
    public KillAura() { super("KillAura", "Attacks entities around you", Category.COMBAT); }
}
