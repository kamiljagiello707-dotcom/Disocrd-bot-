package pl.mysticclient.module.movement;

import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;

public class AutoWalk extends Module {
    public AutoWalk() {
        super("AutoWalk", "Automatycznie idzie do przodu.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        mc.options.forwardKey.setPressed(true);
    }

    @Override
    public void onDisable() {
        if (mc.options != null) mc.options.forwardKey.setPressed(false);
    }
}
