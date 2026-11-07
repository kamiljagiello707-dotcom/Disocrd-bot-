package dev.mustic.client.module.modules;

import dev.mustic.client.module.Category;
import dev.mustic.client.module.Module;
import dev.mustic.client.setting.ModeSetting;

public class SprintModule extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Legit", "Legit", "Toggle"));

    public SprintModule() {
        super("Sprint", "Automatycznie sprintuje.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        if (mode.is("Toggle")) {
            mc.options.sprintKey.setPressed(true);
        } else if (mc.player.forwardSpeed > 0 && !mc.player.isSneaking() && !mc.player.horizontalCollision) {
            mc.player.setSprinting(true);
        }
    }

    @Override
    public void onDisable() {
        if (mc.options != null) mc.options.sprintKey.setPressed(false);
    }

    @Override
    public String getSuffix() {
        return mode.get();
    }
}
