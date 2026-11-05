package pl.firen.client.module.modules;

import pl.firen.client.module.Category;
import pl.firen.client.module.Module;

public class Sprint extends Module {
    public Sprint() { super("Sprint", "Automatycznie biegnie.", Category.MOVEMENT); }

    @Override
    public void onTick() {
        mc.options.sprintKey.setPressed(true);
    }

    @Override
    public void onDisable() {
        mc.options.sprintKey.setPressed(false);
    }
}
