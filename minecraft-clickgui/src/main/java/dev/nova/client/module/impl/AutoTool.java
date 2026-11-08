package dev.nova.client.module.impl;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.setting.*;

public class AutoTool extends Module {
    public final BooleanSetting switchBack = add(new BooleanSetting("Switch Back", true));
    public AutoTool() { super("AutoTool", "Switches to the best tool", Category.WORLD); }
}
