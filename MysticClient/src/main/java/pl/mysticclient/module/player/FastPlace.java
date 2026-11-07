package pl.mysticclient.module.player;

import pl.mysticclient.mixin.MinecraftClientAccessor;
import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;
import pl.mysticclient.setting.NumberSetting;

public class FastPlace extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay", 0, 0, 3, 1));

    public FastPlace() {
        super("FastPlace", "Szybsze stawianie bloków.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        MinecraftClientAccessor acc = (MinecraftClientAccessor) mc;
        if (acc.mystic$getItemUseCooldown() > delay.getInt()) acc.mystic$setItemUseCooldown(delay.getInt());
    }
}
