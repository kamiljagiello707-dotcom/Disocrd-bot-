package dev.wave.util;

/** Frame-rate independent exponential animation towards a target value. */
public class Animation {
    private double value;
    private double target;
    private double speed;
    private long last = System.nanoTime();

    public Animation(double speed, double initial) {
        this.speed = speed;
        this.value = initial;
        this.target = initial;
    }

    public float update() {
        long now = System.nanoTime();
        double dt = Math.min((now - last) / 1_000_000_000.0, 0.1);
        last = now;
        value += (target - value) * (1.0 - Math.exp(-speed * dt));
        if (Math.abs(target - value) < 0.0005) value = target;
        return (float) value;
    }

    public float get() { return (float) value; }
    public double getTarget() { return target; }
    public void setTarget(double target) { this.target = target; }
    public void set(double v) { this.value = v; this.target = v; }
    public void setSpeed(double speed) { this.speed = speed; }
    public boolean isDone() { return value == target; }
}
