package dev.nova.client.module.impl;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.setting.*;

public class NoFall extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Packet", "Packet", "Bucket"));
    public NoFall() { super("NoFall", "Prevents fall damage", Category.PLAYER); }
}
