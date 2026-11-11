package tech.fuzir.client.module.movement;

import tech.fuzir.client.event.events.TickEvent;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.NumberSetting;

public final class Step extends Module {
    private static final float VANILLA_STEP = 0.6f;

    private final NumberSetting height = number("Height", 1.0, 0.6, 2.5, 0.1, "b");

    public Step() {
        super("Step", "Walk up full blocks without jumping.", Category.MOVEMENT);
        on(TickEvent.class, e -> mc.player.setStepHeight(mc.player.isSneaking() ? VANILLA_STEP : height.getFloat()));
    }

    @Override
    protected void onDisable() {
        if (mc.player != null) mc.player.setStepHeight(VANILLA_STEP);
    }

    @Override
    public String getSuffix() {
        return height.format();
    }
}
