package pl.mysticclient.module.movement;

import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;
import pl.mysticclient.setting.ModeSetting;

public class Sprint extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Legit", "Legit", "Omni"));

    public Sprint() {
        super("Sprint", "Automatyczny sprint.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        if (mode.is("Legit")) {
            mc.options.sprintKey.setPressed(true);
        } else {
            boolean moving = mc.player.input.getMovementInput().lengthSquared() > 0;
            if (moving && !mc.player.isSneaking() && mc.player.getHungerManager().getFoodLevel() > 6 && !mc.player.horizontalCollision) {
                mc.player.setSprinting(true);
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc.options != null) mc.options.sprintKey.setPressed(false);
    }

    @Override
    public String getSuffix() { return mode.get(); }
}
