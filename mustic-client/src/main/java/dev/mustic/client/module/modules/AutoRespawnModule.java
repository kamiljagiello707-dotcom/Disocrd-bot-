package dev.mustic.client.module.modules;

import dev.mustic.client.module.Category;
import dev.mustic.client.module.Module;
import dev.mustic.client.setting.NumberSetting;

public class AutoRespawnModule extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay", 0, 0, 60, 1));
    private int deadTicks;

    public AutoRespawnModule() {
        super("AutoRespawn", "Automatycznie odradza po smierci.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        if (mc.player.isDead()) {
            if (deadTicks++ >= delay.getInt()) {
                mc.player.requestRespawn();
                mc.setScreen(null);
                deadTicks = 0;
            }
        } else {
            deadTicks = 0;
        }
    }
}
