package tech.fuzir.client.module.impl;

import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.util.Icons;
import tech.fuzir.client.util.Keys;

public class ToggleSneak extends Module {
    public ToggleSneak() {
        super("ToggleSneak", "Kucanie bez trzymania klawisza", Category.MOVEMENT, Icons.ARROW_DOWN);
    }

    @Override
    public void onTick() {
        mc.options.sneakKey.setPressed(true);
    }

    @Override
    public void onDisable() {
        Keys.restore(mc.options.sneakKey);
    }
}
