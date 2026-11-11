package tech.fuzir.client.util;

/**
 * ARGB packed color helpers.
 */
public final class ColorUtil {
    private ColorUtil() {
    }

    public static int alpha(int color) {
        return color >>> 24;
    }

    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    public static int argb(int a, int r, int g, int b) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    public static int withAlpha(int color, int alpha) {
        return (clamp(alpha) << 24) | (color & 0x00FFFFFF);
    }

    /** Multiplies the existing alpha channel by {@code factor} (0..1). */
    public static int multiplyAlpha(int color, float factor) {
        return withAlpha(color, Math.round(alpha(color) * Math.max(0f, Math.min(1f, factor))));
    }

    public static int interpolate(int from, int to, double t) {
        float f = (float) Math.max(0.0, Math.min(1.0, t));
        return argb(
                Math.round(alpha(from) + (alpha(to) - alpha(from)) * f),
                Math.round(red(from) + (red(to) - red(from)) * f),
                Math.round(green(from) + (green(to) - green(from)) * f),
                Math.round(blue(from) + (blue(to) - blue(from)) * f));
    }

    /**
     * Smooth back-and-forth wave between two colors.
     *
     * @param speedMs duration of one full cycle
     * @param offset  phase offset in milliseconds, used to stagger rows
     */
    public static int wave(int first, int second, double speedMs, double offset) {
        double time = (System.currentTimeMillis() + offset) % speedMs / speedMs;
        double t = (Math.sin(time * Math.PI * 2.0) + 1.0) / 2.0;
        return interpolate(first, second, t);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
