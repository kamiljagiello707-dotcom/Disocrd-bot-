package dev.wave.module.player;

import dev.wave.module.Category;
import dev.wave.module.Module;
import net.minecraft.client.gui.screen.DeathScreen;

public class AutoRespawn extends Module {
    public AutoRespawn() { super("AutoRespawn", "Respawns instantly after death", Category.PLAYER); }

    @Override
    public void onTick() {
        if (mc.player != null && mc.currentScreen instanceof DeathScreen) {
            mc.player.requestRespawn();
            mc.setScreen(null);
        }
    }
}
