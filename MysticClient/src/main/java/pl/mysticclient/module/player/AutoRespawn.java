package pl.mysticclient.module.player;

import net.minecraft.client.gui.screen.DeathScreen;
import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;

public class AutoRespawn extends Module {
    public AutoRespawn() {
        super("AutoRespawn", "Automatycznie odradza po śmierci.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        if (mc.currentScreen instanceof DeathScreen) {
            mc.player.requestRespawn();
            mc.setScreen(null);
        }
    }
}
