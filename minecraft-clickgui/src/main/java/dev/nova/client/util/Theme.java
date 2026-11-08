package dev.nova.client.util;

import dev.nova.client.NovaClient;
import dev.nova.client.module.impl.ClickGuiModule;

/** Central palette. Accent colors come from the ClickGUI module settings. */
public final class Theme {
    private Theme() {}

    public static final int BACKGROUND = 0xF0121218;
    public static final int PANEL      = 0xF0181820;
    public static final int MODULE     = 0xFF1C1C25;
    public static final int MODULE_HOV = 0xFF242430;
    public static final int SETTING_BG = 0xFF15151C;
    public static final int OUTLINE    = 0xFF2A2A36;
    public static final int TEXT       = 0xFFEDEDF5;
    public static final int TEXT_DIM   = 0xFF8C8CA0;
    public static final int TEXT_OFF   = 0xFFB4B4C4;

    private static ClickGuiModule gui() { return NovaClient.get().modules.get(ClickGuiModule.class); }

    /** Accent at a given offset (used for gradients / rainbow waves). */
    public static int accent(double offset) {
        ClickGuiModule g = gui();
        if (g.rainbow.get()) {
            double period = 12000.0 / g.rainbowSpeed.get();
            float hue = (float) (((System.currentTimeMillis() + offset * 12) % period) / period);
            return ColorUtil.hsb(hue, 0.65f, 1f);
        }
        float base = g.accent.get();
        if (!g.gradient.get()) return ColorUtil.hsb(base, 0.7f, 1f);
        double wave = (Math.sin(System.currentTimeMillis() / 600.0 + offset / 40.0) + 1) / 2;
        return ColorUtil.lerp(ColorUtil.hsb(base, 0.7f, 1f), ColorUtil.hsb((base + 0.12f) % 1f, 0.7f, 1f), wave);
    }

    public static int accent() { return accent(0); }
}
