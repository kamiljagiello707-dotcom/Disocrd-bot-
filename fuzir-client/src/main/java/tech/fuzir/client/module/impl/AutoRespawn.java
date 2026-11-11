package tech.fuzir.client.module.impl;

import net.minecraft.client.gui.screen.DeathScreen;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.NumberSetting;
import tech.fuzir.client.util.Icons;

public class AutoRespawn extends Module {
    private final NumberSetting delay = add(new NumberSetting("Opóźnienie", 0.5, 0, 3, 0.1, " s"));
    private int dead;

    public AutoRespawn() {
        super("AutoRespawn", "Odradza się sam po śmierci", Category.PLAYER, Icons.HEART);
    }

    @Override
    public void onTick() {
        if (mc.player.isDead() && mc.currentScreen instanceof DeathScreen) {
            if (++dead >= delay.get() * 20) {
                mc.player.requestRespawn();
                mc.setScreen(null);
                dead = 0;
            }
        } else {
            dead = 0;
        }
    }
}
