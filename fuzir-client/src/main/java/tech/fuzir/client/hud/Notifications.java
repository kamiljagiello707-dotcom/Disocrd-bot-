package tech.fuzir.client.hud;

import tech.fuzir.client.module.Module;
import tech.fuzir.client.module.ModuleManager;
import tech.fuzir.client.module.impl.NotificationsModule;
import tech.fuzir.client.util.Anim;
import tech.fuzir.client.util.Canvas;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Notifications {
    private static final List<Entry> LIST = new ArrayList<>();

    private Notifications() {
    }

    private static final class Entry {
        final String title, text;
        final boolean on;
        final long start = System.currentTimeMillis();
        final long life;
        final Anim anim = new Anim(0);
        final Anim y = new Anim(-1);

        Entry(String title, String text, boolean on, long life) {
            this.title = title;
            this.text = text;
            this.on = on;
            this.life = life;
        }
    }

    public static void module(Module m) {
        if (!m.hasToggle() || ModuleManager.all().isEmpty()) return;
        NotificationsModule nm = ModuleManager.get(NotificationsModule.class);
        if (!nm.isEnabled() && m != nm) return;
        // ten sam moduł przełączony kilka razy z rzędu: zostaje tylko najnowszy dymek
        LIST.removeIf(e -> e.title.equals(m.name) && e.anim.get() < 0.5f);
        LIST.add(new Entry(m.name, m.isEnabled() ? "Włączono" : "Wyłączono", m.isEnabled(), (long) (nm.time.get() * 1000)));
        while (LIST.size() > 5) LIST.remove(0);
    }

    static void render(Canvas cv) {
        float w = 132, h = 30, gap = 6;
        float baseY = cv.height() - 10;
        long now = System.currentTimeMillis();
        int slot = 0;
        for (int i = LIST.size() - 1; i >= 0; i--) {
            Entry e = LIST.get(i);
            boolean alive = now - e.start < e.life;
            float a = e.anim.to(alive ? 1 : 0, alive ? 14 : 12);
            float targetY = baseY - (slot + 1) * (h + gap);
            if (e.y.get() < 0) e.y.set(targetY);
            float y = e.y.to(targetY, 14);
            if (alive) slot++;
            float x = cv.width() - 10 - w + (1 - Anim.easeOutCubic(a)) * (w + 16);
            float prev = cv.alpha;
            cv.alpha = Math.min(1, a * 1.3f);
            cv.roundBorder(x, y, w, h, 8, 0xE60C0C0C, 0xFF262626);
            // ikona stanu
            if (e.on) {
                cv.circle(x + 15, y + 15, 6, 0xFFFFFFFF);
                cv.circle(x + 15, y + 15, 2.4f, 0xFF0C0C0C);
            } else {
                cv.circle(x + 15, y + 15, 6, 0xFF555555);
                cv.circle(x + 15, y + 15, 4.6f, 0xFF0C0C0C);
            }
            cv.textBold(cv.trim(e.title, w - 34, 1), x + 27, y + 6, 0xFFFFFFFF, 1);
            cv.text(e.text, x + 27, y + 17, e.on ? 0xFFBDBDBD : 0xFF7A7A7A, 0.5f);
            float p = Math.max(0, 1 - (now - e.start) / (float) e.life);
            cv.round(x + 8, y + h - 3.5f, (w - 16) * p, 1.5f, 0.75f, e.on ? 0xFFFFFFFF : 0xFF555555);
            cv.alpha = prev;
        }
        Iterator<Entry> it = LIST.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (now - e.start > e.life && e.anim.get() < 0.01f) it.remove();
        }
    }
}
