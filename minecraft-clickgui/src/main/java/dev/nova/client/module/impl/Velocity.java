package dev.nova.client.module.impl;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.setting.*;

public class Velocity extends Module {
    public final NumberSetting horizontal = add(new NumberSetting("Horizontal", 0, 0, 100, 1));
    public final NumberSetting vertical = add(new NumberSetting("Vertical", 0, 0, 100, 1));
    public Velocity() { super("Velocity", "Modifies knockback", Category.COMBAT); }
}
