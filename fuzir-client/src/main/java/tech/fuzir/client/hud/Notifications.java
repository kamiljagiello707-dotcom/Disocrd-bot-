package tech.fuzir.client.hud;

import tech.fuzir.client.module.Module;
import tech.fuzir.client.module.ModuleManager;
import tech.fuzir.client.module.impl.NotificationsModule;
import tech.fuzir.client.util.Anim;
import tech.fuzir.client.util.Canvas;
import tech.fuzir.client.util.Canvas.F;

import java.util.ArrayList;
import java.util.List;

public final class Notifications {
    private static final List<Entry> LIST = new ArrayList<>();

    private Notifications() {
    }

    private static final class Entry {
        final Module module;
        final boolean on;
        final long start = System.currentTimeMillis();
        final long life;
        final Anim anim = new Anim(0);
        final Anim y = new Anim(-1);

        Entry(Module module, boolean on, long life) {
            this.module = module;
            this.on = on;
            this.life = life;
        }
    }

    public static void module(Module m) {
        if (!m.hasToggle() || ModuleManager.all().isEmpty()) return;
        NotificationsModule nm = ModuleManager.get(NotificationsModule.class);
        if (!nm.isEnabled() && m != nm) return;
        // ten sam moduł przełączony kilka razy z rzędu: zostaje tylko najnowszy dymek
        LIST.removeIf(e -> e.module == m);
        LIST.add(new Entry(m, m.isEnabled(), (long) (nm.time.get() * 1000)));
        while (LIST.size() > 5) LIST.remove(0);
    }

    static void render(Canvas cv) {
        float w = 150, h = 34, gap = 6;
        float baseY = cv.height() - 10;
        long now = System.currentTimeMillis();
        int slot = 0;
        for (int i = LIST.size() - 1; i >= 0; i--) {
            Entry e = LIST.get(i);
            boolean alive = now - e.start < e.life;
            float a = e.anim.to(alive ? 1 : 0, alive ? 13 : 11);
            float targetY = baseY - (slot + 1) * (h + gap);
            if (e.y.get() < 0) e.y.set(targetY);
            float y = e.y.to(targetY, 14);
            if (alive) slot++;
            float x = cv.width() - 10 - w + (1 - Anim.easeOutCubic(a)) * (w + 20);
            float prev = cv.alpha;
            cv.alpha = Math.min(1, a * 1.3f);
            Hud.panel(cv, x, y, w, h, 9);
            float cy = y + h / 2 - 1;
            cv.round(x + 7, cy - 10, 20, 20, 6, e.on ? Hud.WHITE : 0xFF1F1F23);
            cv.icon(e.module.icon, x + 17, cy, e.on ? 0xFF000000 : Hud.MUTED, F.ICON);
            cv.text(cv.trim(e.module.name, w - 44, F.BOLD), x + 34, cy - 7.5f, Hud.WHITE, F.BOLD);
            cv.text(e.on ? "Włączono" : "Wyłączono", x + 34, cy + 3, e.on ? 0xFFB9B9BF : Hud.DIM, F.SMALL);
            float p = Math.max(0, 1 - (now - e.start) / (float) e.life);
            cv.round(x + 9, y + h - 4, (w - 18), 1.5f, 0.75f, 0x14FFFFFF);
            cv.round(x + 9, y + h - 4, (w - 18) * p, 1.5f, 0.75f, e.on ? Hud.WHITE : Hud.DIM);
            cv.alpha = prev;
        }
        LIST.removeIf(e -> now - e.start > e.life && e.anim.get() < 0.01f);
    }
}
