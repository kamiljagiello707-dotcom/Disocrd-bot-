package tech.fuzir.client.gui;

import tech.fuzir.client.FuzirClient;
import tech.fuzir.client.module.misc.ClickGuiModule;
import tech.fuzir.client.util.ColorUtil;

/**
 * Central palette. Deep black glass surfaces with either a soft sakura pink/white or a neon green/mint accent.
 */
public final class Theme {
    public static final int OVERLAY = 0x000000;
    public static final int PANEL = 0xE60B0B0E;
    public static final int HEADER = 0xF2111115;
    public static final int ELEMENT_HOVER = 0x14FFFFFF;
    public static final int SETTINGS_BG = 0x66000000;
    public static final int BORDER = 0x12FFFFFF;
    public static final int TRACK = 0xFF26262C;
    public static final int KNOB = 0xFFF5F5F7;
    public static final int TEXT = 0xFFEDEDF2;
    public static final int TEXT_DIM = 0xFF9A9AA6;
    public static final int TEXT_MUTED = 0xFF5E5E69;
    public static final int HUD_BG = 0xA8070709;

    public static final String SEPARATOR_GLYPH = "︲";

    private static final int SAKURA = 0xFFFFA7C9;
    private static final int SAKURA_SECONDARY = 0xFFFFE6F0;
    private static final int NEON = 0xFF3CFF8F;
    private static final int NEON_SECONDARY = 0xFF00E0C6;

    private static ClickGuiModule settings;

    private Theme() {
    }

    private static ClickGuiModule settings() {
        if (settings == null) settings = FuzirClient.get().getModuleManager().get(ClickGuiModule.class);
        return settings;
    }

    private static boolean neon() {
        return settings().accent.is("Neon");
    }

    public static int accent() {
        return neon() ? NEON : SAKURA;
    }

    public static int accentSecondary() {
        return neon() ? NEON_SECONDARY : SAKURA_SECONDARY;
    }

    /** Animated accent gradient, {@code offset} staggers rows so the wave travels across a list. */
    public static int accentWave(double offset) {
        return ColorUtil.wave(accent(), accentSecondary(), 3200.0, offset);
    }

    /** Readable text color drawn on top of a filled accent surface. */
    public static int onAccent() {
        return 0xFF0B0B0E;
    }

    public static boolean customFont() {
        return settings().customFont.isEnabled();
    }
}
