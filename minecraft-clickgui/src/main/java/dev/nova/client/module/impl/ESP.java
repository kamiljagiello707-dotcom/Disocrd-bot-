package dev.nova.client.module.impl;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.setting.*;

public class ESP extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Box", "Box", "Outline", "Glow"));
    public final ColorSetting color = add(new ColorSetting("Color", 0.55f));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting items = add(new BooleanSetting("Items", false));
    public ESP() { super("ESP", "Highlights entities", Category.RENDER); }
}
