package dev.nova.client.util;

public final class ColorUtil {
    private ColorUtil() {}

    public static int alpha(int color, double alpha) {
        int a = (int) ((color >>> 24 & 0xFF) * Math.max(0, Math.min(1, alpha)));
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static int lerp(int c1, int c2, double t) {
        t = Math.max(0, Math.min(1, t));
        int a = (int) ((c1 >>> 24 & 0xFF) + ((c2 >>> 24 & 0xFF) - (c1 >>> 24 & 0xFF)) * t);
        int r = (int) ((c1 >> 16 & 0xFF) + ((c2 >> 16 & 0xFF) - (c1 >> 16 & 0xFF)) * t);
        int g = (int) ((c1 >> 8 & 0xFF) + ((c2 >> 8 & 0xFF) - (c1 >> 8 & 0xFF)) * t);
        int b = (int) ((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int hsb(float h, float s, float b) {
        return 0xFF000000 | (java.awt.Color.HSBtoRGB(h, s, b) & 0xFFFFFF);
    }
}
