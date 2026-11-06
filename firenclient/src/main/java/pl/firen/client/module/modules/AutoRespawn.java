package pl.firen.client.module.modules;

import net.minecraft.client.gui.screen.DeathScreen;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;

public class AutoRespawn extends Module {
    public AutoRespawn() { super("AutoRespawn", "Odradza od razu po śmierci.", Category.MISC); }

    @Override
    public void onTick() {
        if (mc.currentScreen instanceof DeathScreen) {
            mc.player.requestRespawn();
            mc.setScreen(null);
        }
    }
}
