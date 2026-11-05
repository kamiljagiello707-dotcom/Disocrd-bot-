package pl.firen.client.util;

public final class Anim {
    private Anim() {}

    /** Płynne zbliżanie wartości do celu niezależnie od FPS. */
    public static float approach(float current, float target, float dt, float speed) {
        float next = current + (target - current) * (1f - (float) Math.exp(-speed * dt));
        return Math.abs(target - next) < 0.001f ? target : next;
    }

    public static float easeOutCubic(float t) {
        float f = 1f - t;
        return 1f - f * f * f;
    }

    public static int lerpColor(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int aa = a >>> 24, ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int ba = b >>> 24, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return (int) (aa + (ba - aa) * t) << 24 | (int) (ar + (br - ar) * t) << 16
                | (int) (ag + (bg - ag) * t) << 8 | (int) (ab + (bb - ab) * t);
    }

    public static int withAlpha(int color, float alpha) {
        int a = (int) ((color >>> 24) * Math.max(0, Math.min(1, alpha)));
        return a << 24 | (color & 0xFFFFFF);
    }
}
