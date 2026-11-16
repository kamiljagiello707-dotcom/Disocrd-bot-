package dev.vesc.client.hud;

/** Szacowanie TPS serwera z pakietów czasu świata (serwer wysyła je co 20 ticków). */
public final class Tps {
    private Tps() {}

    private static long lastWorldTime = -1, lastNanos;
    private static final float[] samples = new float[6];
    private static int n, head;

    public static void onTimeUpdate(long worldTime) {
        long now = System.nanoTime();
        if (lastWorldTime >= 0) {
            double sec = (now - lastNanos) / 1e9;
            long ticks = worldTime - lastWorldTime;
            if (sec > 0.2 && ticks > 0) {
                samples[head] = (float) Math.min(20.0, ticks / sec);
                head = (head + 1) % samples.length;
                n = Math.min(samples.length, n + 1);
            }
        }
        lastWorldTime = worldTime;
        lastNanos = now;
    }

    public static void reset() { lastWorldTime = -1; n = 0; head = 0; }

    /** średni TPS z ostatnich pomiarów (20 gdy brak danych) */
    public static float get() {
        if (n == 0) return 20f;
        float s = 0;
        for (int i = 0; i < n; i++) s += samples[i];
        return s / n;
    }
}
