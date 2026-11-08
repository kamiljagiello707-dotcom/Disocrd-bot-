package dev.nova.client.module.impl;

import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.setting.BooleanSetting;

public class Sprint extends Module {
    public final BooleanSetting omni = add(new BooleanSetting("Omni", false));

    public Sprint() { super("Sprint", "Automatically sprints", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        boolean moving = omni.get() ? mc.player.input.getMovementInput().lengthSquared() > 0 : mc.player.input.movementForward > 0;
        if (moving && !mc.player.isSneaking() && mc.player.getHungerManager().getFoodLevel() > 6) mc.player.setSprinting(true);
    }
}
