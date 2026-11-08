package dev.nova.client.util;

/** Frame-rate independent exponential smoothing towards a target (0..1 or any value). */
public final class Animation {
    private double value, target;
    private final double speed;
    private long last = System.nanoTime();

    public Animation(double speed, double initial) {
        this.speed = speed;
        this.value = this.target = initial;
    }

    public Animation setTarget(double t) { target = t; return this; }
    public void snap(double v) { value = target = v; }

    public double get() {
        long now = System.nanoTime();
        double dt = Math.min((now - last) / 1e9, 0.1);
        last = now;
        value += (target - value) * (1 - Math.exp(-speed * dt));
        if (Math.abs(target - value) < 0.0005) value = target;
        return value;
    }

    public double getTarget() { return target; }

    public static double easeOutBack(double t) {
        double c1 = 1.70158, c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }
}
