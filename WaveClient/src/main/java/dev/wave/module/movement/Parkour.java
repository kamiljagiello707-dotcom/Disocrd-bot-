package dev.wave.module.movement;

import dev.wave.module.Category;
import dev.wave.module.Module;

public class Parkour extends Module {
    public Parkour() { super("Parkour", "Jumps automatically at the edge of a block", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        if (mc.player.isOnGround() && !mc.player.isSneaking() && !mc.options.jumpKey.isPressed()
                && mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset(0, -0.5, 0).expand(-0.001, 0, -0.001))) {
            mc.player.jump();
        }
    }
}
