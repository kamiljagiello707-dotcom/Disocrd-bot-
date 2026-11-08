package dev.wave.module.movement;

import dev.wave.module.Category;
import dev.wave.module.Module;

public class AutoWalk extends Module {
    public AutoWalk() { super("AutoWalk", "Keeps walking forward for you", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (mc.currentScreen == null) mc.options.forwardKey.setPressed(true);
    }

    @Override
    public void onDisable() { mc.options.forwardKey.setPressed(false); }
}
