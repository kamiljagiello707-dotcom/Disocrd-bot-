package dev.with.client.ui;

/**
 * Płynna animacja wartości, niezależna od FPS. Trzy tryby (Wygląd → Animacje):
 * 0 = płynny (wykładnicze dążenie), 1 = sprężysty (lekkie przestrzelenie, tylko dla pozycji),
 * 2 = liniowy (stałe tempo).
 */
public class Anim {
    private float value, target, velocity, span = 1;
    private final float speed;
    private boolean springy;
    private long last = System.nanoTime();

    public Anim(float start, float speed) { this.value = start; this.target = start; this.speed = speed; }

    /** Globalny mnożnik szybkości animacji (ustawienia klienta). */
    public static float globalSpeed = 1f;
    /** 0 płynny, 1 sprężysty, 2 liniowy */
    public static int mode = 0;

    /** animacja pozycji – w trybie sprężystym może lekko przestrzelić */
    public Anim springy() { springy = true; return this; }

    public float get() {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1_000_000_000f);
        last = now;
        float sp = speed * globalSpeed;
        if (mode == 1 && springy && globalSpeed < 20) {
            float k = sp * sp * 1.15f, c = 2f * 0.62f * (float) Math.sqrt(k);
            int steps = Math.max(1, (int) Math.ceil(dt * 240));
            float h = dt / steps;
            for (int i = 0; i < steps; i++) {
                velocity += (k * (target - value) - c * velocity) * h;
                value += velocity * h;
            }
            if (Math.abs(target - value) < 0.0005f && Math.abs(velocity) < 0.01f) { value = target; velocity = 0; }
            return value;
        }
        velocity = 0;
        if (mode == 2 && globalSpeed < 20) {
            float d = target - value, step = span * sp * dt * 0.3f;
            value = Math.abs(d) <= step ? target : value + Math.signum(d) * step;
            return value;
        }
        float k = 1f - (float) Math.exp(-sp * dt);
        value += (target - value) * k;
        if (Math.abs(target - value) < 0.0005f) value = target;
        return value;
    }

    public float animate(float target) { setTarget(target); return get(); }
    public float target() { return target; }
    public void setTarget(float t) {
        if (t != target) span = Math.max(1f, Math.abs(t - value));
        target = t;
    }
    public void snap(float v) { value = v; target = v; velocity = 0; last = System.nanoTime(); }
    public float raw() { return value; }

    public static float easeOutCubic(float t) { t = clamp01(t); return 1 - (float) Math.pow(1 - t, 3); }
    public static float easeInCubic(float t) { t = clamp01(t); return t * t * t; }
    public static float easeInOutCubic(float t) { t = clamp01(t); return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2; }
    public static float easeOutBack(float t) {
        t = clamp01(t); float c1 = 1.70158f, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }
    /** krzywa wejścia zależna od trybu animacji (do animacji czasowych) */
    public static float enter(float t) {
        return switch (mode) { case 1 -> easeOutBack(t); case 2 -> clamp01(t); default -> easeOutCubic(t); };
    }
    public static float clamp01(float t) { return Math.max(0, Math.min(1, t)); }
    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
