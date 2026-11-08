package dev.wave.notification;

import dev.wave.util.Animation;

public class Notification {
    public enum Type { ENABLED, DISABLED, INFO, WARNING }

    public final String title;
    public final String message;
    public final Type type;
    public final long start = System.currentTimeMillis();
    public final long duration;
    public final Animation y = new Animation(14, -1);

    public Notification(String title, String message, Type type, long duration) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.duration = duration;
    }

    public long age() { return System.currentTimeMillis() - start; }
    public boolean expired() { return age() > duration; }
}
