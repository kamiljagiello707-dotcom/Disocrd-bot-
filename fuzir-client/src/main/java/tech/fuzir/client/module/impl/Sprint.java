package tech.fuzir.client.module.impl;

import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.util.Icons;
import tech.fuzir.client.util.Keys;

public class Sprint extends Module {
    public Sprint() {
        super("Sprint", "Automatycznie biegniesz, gdy idziesz do przodu", Category.MOVEMENT, Icons.ZAP);
    }

    @Override
    public void onTick() {
        // gra sama sprawdza głód, kucanie i kolizje, jak przy wciśniętym klawiszu
        mc.options.sprintKey.setPressed(true);
    }

    @Override
    public void onDisable() {
        Keys.restore(mc.options.sprintKey);
    }
}
