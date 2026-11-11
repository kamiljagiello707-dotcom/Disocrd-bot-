package tech.fuzir.client.module.impl;

import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.util.Keys;

public class AutoWalk extends Module {
    private final BoolSetting jump = add(new BoolSetting("Skacz w wodzie", true));

    public AutoWalk() {
        super("AutoWalk", "Idziesz do przodu bez trzymania klawisza", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        mc.options.forwardKey.setPressed(true);
        if (jump.on()) {
            boolean swim = mc.player.isTouchingWater() || mc.player.isInLava();
            mc.options.jumpKey.setPressed(swim || Keys.physical(mc.options.jumpKey));
        }
    }

    @Override
    public void onDisable() {
        Keys.restore(mc.options.forwardKey);
        Keys.restore(mc.options.jumpKey);
    }
}
