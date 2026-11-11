package tech.fuzir.client.module.player;

import net.minecraft.client.gui.screen.DeathScreen;
import tech.fuzir.client.event.events.TickEvent;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.NumberSetting;

public final class AutoRespawn extends Module {
    private final NumberSetting delay = number("Delay", 0, 0, 40, 1, "t");
    private int deadTicks;

    public AutoRespawn() {
        super("AutoRespawn", "Respawns instantly after death.", Category.PLAYER);
        on(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (!(mc.currentScreen instanceof DeathScreen) || !mc.player.isDead()) {
            deadTicks = 0;
            return;
        }
        if (deadTicks++ < delay.getInt()) return;
        mc.player.requestRespawn();
        mc.setScreen(null);
        deadTicks = 0;
    }
}
