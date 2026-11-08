package dev.nova.client.module.impl;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.setting.*;

public class Speed extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Strafe", "Strafe", "BHop", "OnGround"));
    public final NumberSetting speed = add(new NumberSetting("Speed", 1.2, 1.0, 3.0, 0.05));
    public Speed() { super("Speed", "Move faster", Category.MOVEMENT); }
}
