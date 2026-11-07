package dev.mustic.client.util;

/** Plynna animacja niezalezna od FPS (wykladnicze dazenie do celu). */
public class Anim {
    private float value;
    private float target;
    private final float speed;
    private long last = System.nanoTime();

    public Anim(float speed, float start) {
        this.speed = speed;
        this.value = start;
        this.target = start;
    }

    public float update(float target) {
        this.target = target;
        return update();
    }

    public float update() {
        long now = System.nanoTime();
        float dt = Math.min((now - last) / 1_000_000_000f, 0.1f);
        last = now;
        value += (target - value) * (1f - (float) Math.exp(-speed * dt));
        if (Math.abs(target - value) < 0.002f) value = target;
        return value;
    }

    public float get() { return value; }
    public void set(float v) { value = v; target = v; }
}
