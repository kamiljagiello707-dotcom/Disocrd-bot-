package pl.firen.client.gui;

import pl.firen.client.FirenClient;
import pl.firen.client.module.modules.ClickGui;

/** Ciemny motyw z wybieranym kolorem akcentu. */
public final class Theme {
    private Theme() {}

    public static final int WINDOW_TOP = 0xF5111114;
    public static final int WINDOW_BOTTOM = 0xF507070A;
    public static final int SIDEBAR = 0xFF0C0C0F;
    public static final int CARD = 0xFF131317;
    public static final int CARD_HOVER = 0xFF19191E;
    public static final int FIELD = 0xFF16161A;
    public static final int TRACK = 0xFF2A2A30;
    public static final int STROKE = 0x16FFFFFF;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;
    public static final int TEXT = 0xFFEDEDF0;
    public static final int TEXT_2 = 0xFFA4A4AD;
    public static final int TEXT_3 = 0xFF63636D;

    /** Główny kolor akcentu (z ustawień ClickGUI → Theme). */
    public static int accent() {
        try {
            return FirenClient.modules().get(ClickGui.class).accent();
        } catch (Exception e) {
            return WHITE;
        }
    }

    /** Drugi kolor do gradientów (lekko przesunięty odcień / ciemniejszy). */
    public static int accent2() {
        int a = accent();
        float[] hsv = java.awt.Color.RGBtoHSB(a >> 16 & 255, a >> 8 & 255, a & 255, null);
        if (hsv[1] < 0.08f) return 0xFF9A9AA2;
        return 0xFF000000 | (java.awt.Color.HSBtoRGB(hsv[0] + 0.08f, Math.min(1, hsv[1] * 1.05f), hsv[2] * 0.85f) & 0xFFFFFF);
    }

    /** Czytelny kolor tekstu/ikony na tle akcentu. */
    public static int onAccent() {
        int a = accent();
        double lum = (0.2126 * (a >> 16 & 255) + 0.7152 * (a >> 8 & 255) + 0.0722 * (a & 255)) / 255.0;
        return lum > 0.6 ? BLACK : WHITE;
    }

    public static int accentAlpha(int alpha) {
        return (alpha & 255) << 24 | (accent() & 0xFFFFFF);
    }
}
