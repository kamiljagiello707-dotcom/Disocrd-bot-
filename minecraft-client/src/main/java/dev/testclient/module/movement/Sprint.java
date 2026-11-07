package dev.testclient.module.movement;

import dev.testclient.module.Category;
import dev.testclient.module.Module;

public class Sprint extends Module {
    public Sprint() {
        super("Sprint", "Automatycznie sprintuje przy chodzeniu do przodu", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player.input.movementForward > 0 && !mc.player.isSneaking()
                && !mc.player.horizontalCollision && mc.player.getHungerManager().getFoodLevel() > 6) {
            mc.player.setSprinting(true);
        }
    }
}
