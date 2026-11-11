package tech.fuzir.client.module.world;

import tech.fuzir.client.event.events.TickEvent;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.setting.NumberSetting;

/**
 * Client-side only time / weather override. The server re-syncs the real values once disabled.
 */
public final class Ambience extends Module {
    private final BoolSetting changeTime = bool("Change Time", true);
    private final NumberSetting time = register(new NumberSetting("Time", 13000, 0, 24000, 250))
            .visibleWhen(changeTime::isEnabled);
    private final BoolSetting clearWeather = bool("Clear Weather", true);

    public Ambience() {
        super("Ambience", "Client-side time of day and weather.", Category.WORLD);
        on(TickEvent.class, e -> {
            if (changeTime.isEnabled()) mc.world.setTimeOfDay(time.getInt());
            if (clearWeather.isEnabled()) {
                mc.world.setRainGradient(0f);
                mc.world.setThunderGradient(0f);
            }
        });
    }

    @Override
    public String getSuffix() {
        return changeTime.isEnabled() ? String.valueOf(time.getInt()) : null;
    }
}
