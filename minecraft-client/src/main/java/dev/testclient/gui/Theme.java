package dev.testclient.gui;

public final class Theme {
    private Theme() {}

    public static final int ACCENT = 0xFF8A5CFF;
    public static final int ACCENT_DIM = 0xFF5B3BB0;
    public static final int HEADER = 0xF0141220;
    public static final int PANEL = 0xE01C1A2B;
    public static final int ROW_HOVER = 0x30FFFFFF;
    public static final int SETTING_BG = 0xE0141320;
    public static final int TEXT = 0xFFE8E6F0;
    public static final int TEXT_DIM = 0xFF9A96AD;
    public static final int OUTLINE = 0xFF2A2740;

    /** Tęczowy kolor przesunięty o offset - do ArrayListy w HUD. */
    public static int rainbow(long offset) {
        float hue = ((System.currentTimeMillis() + offset) % 4000L) / 4000f;
        return 0xFF000000 | (java.awt.Color.HSBtoRGB(hue, 0.55f, 1f) & 0xFFFFFF);
    }
}
