package pl.firen.client.module.modules;

import pl.firen.client.module.Category;
import pl.firen.client.module.Module;

/** Skacze automatycznie na samej krawędzi bloku. */
public class Parkour extends Module {
    public Parkour() { super("Parkour", "Automatyczny skok na krawędzi bloku.", Category.MOVEMENT); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (!p.isOnGround() || p.isSneaking() || mc.options.jumpKey.isPressed()) return;
        boolean moving = p.input.movementForward != 0 || p.input.movementSideways != 0;
        if (moving && mc.world.isSpaceEmpty(p, p.getBoundingBox().offset(0, -0.5, 0).expand(-0.001, 0, -0.001))) p.jump();
    }
}
