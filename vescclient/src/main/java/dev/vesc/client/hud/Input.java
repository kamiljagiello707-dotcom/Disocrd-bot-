package dev.vesc.client.hud;

import java.util.ArrayDeque;

/** Liczenie kliknięć myszy (CPS) dla HUD. */
public final class Input {
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>(), RIGHT = new ArrayDeque<>();

    private Input() {}

    public static void click(int button) {
        if (button == 0) LEFT.addLast(System.currentTimeMillis());
        else if (button == 1) RIGHT.addLast(System.currentTimeMillis());
    }

    public static int cps(int button) {
        ArrayDeque<Long> q = button == 0 ? LEFT : RIGHT;
        long now = System.currentTimeMillis();
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
        return q.size();
    }
}
