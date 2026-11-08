package dev.wave.util;

import dev.wave.WaveClient;
import dev.wave.module.client.ClickGUI;

/** Monochrome (black / white) palette shared by the GUI, HUD and notifications. */
public final class Theme {
    private Theme() {}

    public static final int BG = 0xF00E0E0E;
    public static final int BG_DARK = 0xF0080808;
    public static final int BG_LIGHT = 0xF0181818;
    public static final int HOVER = 0xF0202020;
    public static final int BORDER = 0x30FFFFFF;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_DIM = 0xFF9A9A9A;
    public static final int TEXT_DARK = 0xFF0C0C0C;
    public static final int HUD_BG = 0x90000000;

    public static int accent() { return accent(0); }

    public static int accent(long offset) {
        if (WaveClient.INSTANCE == null || WaveClient.INSTANCE.modules == null) return 0xFFFFFFFF;
        return WaveClient.INSTANCE.modules.get(ClickGUI.class).accent.getColor(offset);
    }

    /** Text colour readable on top of the accent colour. */
    public static int onAccent() {
        int c = accent();
        int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
        return (0.299 * r + 0.587 * g + 0.114 * b) > 150 ? TEXT_DARK : TEXT;
    }
}
