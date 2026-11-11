package tech.fuzir.client.util;

/** Płynne dojście wartości do celu, niezależne od liczby klatek. */
public final class Anim {
    /** Globalny mnożnik szybkości, ustawiany w module ClickGUI. */
    public static float speedMul = 1f;

    private float value;
    private long last;

    public Anim(float start) {
        this.value = start;
    }

    public float get() {
        return value;
    }

    public void set(float v) {
        value = v;
        last = System.nanoTime();
    }

    /** Przesuwa wartość w stronę {@code target}; większe {@code speed} = szybciej. */
    public float to(float target, float speed) {
        long now = System.nanoTime();
        float dt = last == 0 ? 0f : Math.min((now - last) / 1_000_000_000f, 0.25f);
        last = now;
        value += (target - value) * (1f - (float) Math.exp(-speed * speedMul * dt));
        if (Math.abs(target - value) < 0.0005f) value = target;
        return value;
    }

    public static float easeOutCubic(float x) {
        float t = 1f - Math.max(0f, Math.min(1f, x));
        return 1f - t * t * t;
    }
}
