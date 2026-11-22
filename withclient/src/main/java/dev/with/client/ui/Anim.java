package dev.with.client.ui;

/** Płynna animacja wartości (wykładnicze dążenie do celu, niezależne od FPS). */
public class Anim {
    private float value, target;
    private final float speed;
    private long last = System.nanoTime();

    public Anim(float start, float speed) { this.value = start; this.target = start; this.speed = speed; }

    /** Globalny mnożnik szybkości animacji (ustawienia klienta). */
    public static float globalSpeed = 1f;

    public float get() {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1_000_000_000f);
        last = now;
        float k = 1f - (float) Math.exp(-speed * globalSpeed * dt);
        value += (target - value) * k;
        if (Math.abs(target - value) < 0.0005f) value = target;
        return value;
    }

    public float animate(float target) { this.target = target; return get(); }
    public float target() { return target; }
    public void setTarget(float t) { target = t; }
    public void snap(float v) { value = v; target = v; last = System.nanoTime(); }
    public float raw() { return value; }

    public static float easeOutCubic(float t) { t = clamp01(t); return 1 - (float) Math.pow(1 - t, 3); }
    public static float easeInCubic(float t) { t = clamp01(t); return t * t * t; }
    public static float easeOutBack(float t) {
        t = clamp01(t); float c1 = 1.70158f, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }
    public static float clamp01(float t) { return Math.max(0, Math.min(1, t)); }
    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
