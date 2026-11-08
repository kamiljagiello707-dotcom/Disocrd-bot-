package dev.nova.client.module.impl;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.setting.*;

public class FastPlace extends Module {
    public final NumberSetting delay = add(new NumberSetting("Delay", 0, 0, 4, 1));
    public FastPlace() { super("FastPlace", "Removes block place delay", Category.WORLD); }
}
