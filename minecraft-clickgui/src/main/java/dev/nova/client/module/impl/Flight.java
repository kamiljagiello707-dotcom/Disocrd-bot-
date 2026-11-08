package dev.nova.client.module.impl;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.setting.*;

public class Flight extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Vanilla", "Vanilla", "Glide", "Motion"));
    public final NumberSetting speed = add(new NumberSetting("Speed", 1.5, 0.1, 10, 0.1));
    public final BooleanSetting antiKick = add(new BooleanSetting("Anti Kick", true));
    public Flight() { super("Flight", "Lets you fly", Category.MOVEMENT); }
}
