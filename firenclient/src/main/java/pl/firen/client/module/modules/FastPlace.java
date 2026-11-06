package pl.firen.client.module.modules;

import pl.firen.client.mixin.MinecraftClientAccessor;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.NumberSetting;

public class FastPlace extends Module {
    public final NumberSetting delay = add(new NumberSetting("Delay ticks", 0, 0, 4, 1));

    public FastPlace() { super("FastPlace", "Szybsze stawianie bloków i używanie przedmiotów.", Category.PLAYER); }

    @Override
    public void onTick() {
        MinecraftClientAccessor acc = (MinecraftClientAccessor) mc;
        if (acc.firen$getItemUseCooldown() > delay.get()) acc.firen$setItemUseCooldown(delay.get().intValue());
    }
}
