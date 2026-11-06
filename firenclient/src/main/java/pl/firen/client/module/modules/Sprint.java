package pl.firen.client.module.modules;

import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.ModeSetting;

public class Sprint extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Legit", "Legit", "Omni"));

    public Sprint() { super("Sprint", "Automatycznie biegnie.", Category.MOVEMENT); }

    @Override public String getSuffix() { return mode.get(); }

    @Override
    public void onTick() {
        if (mode.is("Legit")) {
            mc.options.sprintKey.setPressed(true);
        } else {
            boolean moving = mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
            if (moving && !mc.player.isSneaking() && mc.player.getHungerManager().getFoodLevel() > 6) mc.player.setSprinting(true);
        }
    }

    @Override
    public void onDisable() {
        mc.options.sprintKey.setPressed(false);
    }
}
