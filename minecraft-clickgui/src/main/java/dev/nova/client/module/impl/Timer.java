package dev.nova.client.module.impl;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.setting.*;

public class Timer extends Module {
    public final NumberSetting speed = add(new NumberSetting("Speed", 1.0, 0.1, 5.0, 0.05));
    public Timer() { super("Timer", "Changes game tick speed", Category.MISC); }
}
