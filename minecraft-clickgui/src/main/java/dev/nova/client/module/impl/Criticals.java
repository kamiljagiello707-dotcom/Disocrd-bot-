package dev.nova.client.module.impl;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.setting.*;

public class Criticals extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Packet", "Packet", "Jump", "Mini"));
    public Criticals() { super("Criticals", "Always land critical hits", Category.COMBAT); }
}
