package pl.mysticclient.util;

import pl.mysticclient.module.ModuleManager;
import pl.mysticclient.module.misc.ClientSettings;
import pl.mysticclient.module.render.HUD;
import pl.mysticclient.module.render.ModernHud;
import pl.mysticclient.util.UI.Icons;
import pl.mysticclient.util.UI.Weight;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class NotificationManager {
    private static final List<Notification> list = new ArrayList<>();

    private NotificationManager() {}

    private static final class Notification {
        final String title, text;
        final boolean positive;
        final long start = System.currentTimeMillis();
        float anim = 0f;
        float y = Float.NaN;

        Notification(String title, String text, boolean positive) {
            this.title = title;
            this.text = text;
            this.positive = positive;
        }
    }

    public static void push(String title, String text, boolean positive) {
        synchronized (list) {
            list.removeIf(n -> n.title.equals(title));
            list.add(new Notification(title, text, positive));
            if (list.size() > 5) list.remove(0);
        }
    }

    /** Wywoływane wewnątrz UI.begin/end. */
    public static void render() {
        ClientSettings cs = ModuleManager.get(ClientSettings.class);
        if (cs == null) return;
        float sw = UI.screenW(), sh = UI.screenH();
        String pos = cs.position.get();
        boolean right = pos.endsWith("Right");
        boolean bottom = pos.startsWith("Bottom");
        long durationMs = (long) (cs.duration.get() * 1000);

        float w = 240, h = 52, gap = 8;
        synchronized (list) {
            int index = 0;
            Iterator<Notification> it = list.iterator();
            while (it.hasNext()) {
                Notification n = it.next();
                long age = System.currentTimeMillis() - n.start;
                boolean leaving = age > durationMs;
                n.anim = UI.approach(n.anim, leaving ? 0f : 1f, 12f);
                if (leaving && n.anim <= 0.01f) { it.remove(); continue; }

                float top = right && !bottom ? Math.max(12, HUD.arrayListBottom + 8) : (!right && !bottom ? 230 : 12);
                float targetY = bottom ? sh - 12 - h - index * (h + gap) - (right ? 120 : 50) : top + index * (h + gap);
                if (Float.isNaN(n.y)) n.y = targetY;
                n.y = UI.approach(n.y, targetY, 14f);

                float ease = 1f - (float) Math.pow(1f - n.anim, 3);
                float slide = (1f - ease) * (w + 24);
                float x = right ? sw - w - 12 + slide : 12 - slide;
                float y = n.y;

                float prev = UI.alpha;
                UI.alpha = Math.min(1f, n.anim * 1.4f);
                ModernHud.card(x, y, w, h, 12);
                UI.round(x + 10, y + 10, 32, 32, 9, n.positive ? Theme.accent() : UI.a(Theme.text(), 0.08f));
                UI.icon(n.positive ? Icons.CHECK : Icons.X, x + 26, y + 26, 16, n.positive ? Theme.onAccent() : Theme.text());
                UI.text(UI.ellipsis(n.title, w - 66, 13, Weight.SEMIBOLD), x + 52, y + 10, 13, Weight.SEMIBOLD, Theme.text());
                UI.text(UI.ellipsis(n.text, w - 66, 11.5f, Weight.REGULAR), x + 52, y + 28, 11.5f, Weight.REGULAR, Theme.sub());
                float progress = Math.min(1f, age / (float) durationMs);
                UI.round(x + 12, y + h - 4, (w - 24) * (1f - progress), 2, 1, UI.a(Theme.text(), 0.5f));
                UI.alpha = prev;
                index++;
            }
        }
    }
}
