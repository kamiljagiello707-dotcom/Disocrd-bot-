package tech.fuzir.client.module.render;

import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.NumberSetting;

/**
 * Overrides the gamma value sampled by the lightmap (see LightmapTextureManagerMixin) without
 * touching the user's saved video options.
 */
public final class FullBright extends Module {
    private final NumberSetting brightness = number("Brightness", 12.0, 1.0, 16.0, 0.5);

    public FullBright() {
        super("FullBright", "See clearly in the dark without changing your gamma option.", Category.RENDER);
    }

    public double getGamma() {
        return brightness.get();
    }
}
