package dev.mustic.client.util;

import net.minecraft.util.math.MathHelper;

public final class ColorUtil {
    private ColorUtil() {}

    public static int alpha(int color, float alpha) {
        int a = (int) (((color >>> 24) & 0xFF) * MathHelper.clamp(alpha, 0f, 1f));
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static int withAlpha(int color, int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | (color & 0xFFFFFF);
    }

    public static int lerp(int a, int b, float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24)
                | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8)
                | (int) (ab + (bb - ab) * t);
    }

    public static int rainbow(long offsetMs, float saturation, float brightness) {
        float hue = ((System.currentTimeMillis() + offsetMs) % 4000L) / 4000f;
        return 0xFF000000 | MathHelper.hsvToRgb(hue, saturation, brightness);
    }

    /** Animowany gradient miedzy dwoma kolorami. */
    public static int wave(int c1, int c2, long offsetMs) {
        double t = (Math.sin((System.currentTimeMillis() + offsetMs) / 600.0) + 1.0) / 2.0;
        return lerp(c1, c2, (float) t);
    }
}
