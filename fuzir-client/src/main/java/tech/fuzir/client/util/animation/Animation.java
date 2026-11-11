package tech.fuzir.client.util.animation;

/**
 * Time based interpolator. Retargeting while an animation is running starts the new
 * transition from the current interpolated value, so state changes never "jump".
 * Independent of frame rate because progress is derived from wall-clock time.
 */
public final class Animation {
    private final Easing easing;
    private final double durationMs;

    private double from;
    private double to;
    private double startTime;

    public Animation(Easing easing, double durationMs, double initial) {
        this.easing = easing;
        this.durationMs = durationMs;
        this.from = initial;
        this.to = initial;
        this.startTime = 0;
    }

    public Animation(Easing easing, double durationMs) {
        this(easing, durationMs, 0.0);
    }

    private static double now() {
        return System.nanoTime() / 1_000_000.0;
    }

    /** Smoothly transitions towards {@code target}. No-op when already heading there. */
    public Animation animateTo(double target) {
        if (target == to) return this;
        from = get();
        to = target;
        startTime = now();
        return this;
    }

    public Animation animateTo(boolean state) {
        return animateTo(state ? 1.0 : 0.0);
    }

    /** Immediately snaps to {@code value} without transition. */
    public Animation set(double value) {
        from = value;
        to = value;
        startTime = 0;
        return this;
    }

    public double get() {
        if (durationMs <= 0 || from == to) return to;
        double progress = (now() - startTime) / durationMs;
        if (progress >= 1.0) {
            from = to;
            return to;
        }
        return from + (to - from) * easing.apply(progress);
    }

    public float getFloat() {
        return (float) get();
    }

    public double getTarget() {
        return to;
    }

    public boolean isFinished() {
        return get() == to;
    }
}
