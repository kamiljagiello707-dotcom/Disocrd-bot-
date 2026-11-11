package tech.fuzir.client.module.player;

import tech.fuzir.client.event.events.TickEvent;
import tech.fuzir.client.mixin.MinecraftClientAccessor;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.NumberSetting;

public final class FastPlace extends Module {
    private final NumberSetting delay = number("Delay", 0, 0, 3, 1, "t");

    public FastPlace() {
        super("FastPlace", "Reduces the delay between block placements and item uses.", Category.PLAYER);
        on(TickEvent.class, e -> {
            MinecraftClientAccessor accessor = (MinecraftClientAccessor) mc;
            if (accessor.fuzir$getItemUseCooldown() > delay.getInt()) accessor.fuzir$setItemUseCooldown(delay.getInt());
        });
    }

    @Override
    public String getSuffix() {
        return delay.format();
    }
}
