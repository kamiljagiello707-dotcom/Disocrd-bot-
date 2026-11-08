package dev.wave.module.movement;

import dev.wave.module.Category;
import dev.wave.module.Module;

public class Sprint extends Module {
    public Sprint() { super("Sprint", "Automatically sprints while moving forward", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        if (mc.player.input.hasForwardMovement() && !mc.player.isSneaking() && !mc.player.horizontalCollision
                && (mc.player.getHungerManager().getFoodLevel() > 6 || mc.player.getAbilities().allowFlying)) {
            mc.player.setSprinting(true);
        }
    }
}
