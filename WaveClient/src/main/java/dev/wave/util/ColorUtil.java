package dev.wave.util;

import java.awt.Color;

public final class ColorUtil {
    private ColorUtil() {}

    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;

    public static int alpha(int color, float alpha) {
        int a = (int) (((color >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, alpha)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    public static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    public static int getAlpha(int color) { return (color >>> 24) & 0xFF; }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    public static int rainbow(long offset, float saturation, float brightness, float speed) {
        float hue = ((System.currentTimeMillis() + offset) % (long) (10000 / Math.max(0.05f, speed))) / (10000f / Math.max(0.05f, speed));
        return 0xFF000000 | (Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF);
    }

    /** Smooth white -> grey wave used for the monochrome theme. */
    public static int monoWave(long offset, float speed) {
        double t = (System.currentTimeMillis() * speed + offset) / 600.0;
        float v = (float) (0.55 + 0.45 * (Math.sin(t) * 0.5 + 0.5));
        int c = (int) (v * 255);
        return 0xFF000000 | (c << 16) | (c << 8) | c;
    }

    public static int hsb(float h, float s, float b, int alpha) {
        return (alpha << 24) | (Color.HSBtoRGB(h, s, b) & 0xFFFFFF);
    }
}
