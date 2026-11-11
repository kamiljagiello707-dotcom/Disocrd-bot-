package tech.fuzir.client.module.impl;

import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;

public class NoWeather extends Module {
    public NoWeather() {
        super("NoWeather", "Ukrywa deszcz, śnieg i burzę", Category.RENDER);
    }

    @Override
    public void onTick() {
        if (mc.world == null) return;
        mc.world.setRainGradient(0f);
        mc.world.setThunderGradient(0f);
    }
}
