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

    /** Mały czarny embed: ikonka modułu | nazwa | Enabled / Disabled. */
    static void render(Canvas cv) {
        float h = 24, gap = 5;
        float baseY = cv.height() - 10;
        long now = System.currentTimeMillis();
        int slot = 0;
        for (int i = LIST.size() - 1; i >= 0; i--) {
            Entry e = LIST.get(i);
            boolean alive = now - e.start < e.life;
            float a = e.anim.to(alive ? 1 : 0, alive ? 13 : 11);
            String status = e.on ? "Enabled" : "Disabled";
            float nameW = cv.width(e.module.name, F.BOLD), statusW = cv.width(status, F.REGULAR);
            float w = 5 + 16 + 8 + nameW + 9 + 9 + statusW + 10;
            float targetY = baseY - (slot + 1) * (h + gap);
            if (e.y.get() < 0) e.y.set(targetY);
            float y = e.y.to(targetY, 14);
            if (alive) slot++;
            float x = cv.width() - 10 - w + (1 - Anim.easeOutCubic(a)) * (w + 20);
            float prev = cv.alpha;
            cv.alpha = Math.min(1, a * 1.3f);
            cv.shadow(x, y + 2, w, h, 7, 9, 0x80000000);
            cv.round(x, y, w, h, 7, 0xF2070708);
            cv.outline(x, y, w, h, 7, 1f, 0x18FFFFFF);
            float cy = y + h / 2;
            // ikonka modułu
            cv.round(x + 5, cy - 8, 16, 16, 5, e.on ? Hud.ACCENT : Hud.TILE);
            cv.icon(e.module.icon, x + 13, cy, e.on ? Hud.ON_ACCENT : Hud.MUTED, F.ICON_SM);
            // nazwa | status
            float tx = x + 29;
            cv.textMid(e.module.name, tx, cy, 0xFFFFFFFF, F.BOLD);
            tx += nameW + 9;
            cv.rect(tx - 0.5f / cv.u, cy - 6, 1f / cv.u, 12, 0x30FFFFFF);
            tx += 9;
            cv.textMid(status, tx, cy, e.on ? Hud.ACCENT : 0xFF77777E, F.REGULAR);
            // pasek czasu
            float p = Math.max(0, 1 - (now - e.start) / (float) e.life);
            cv.round(x + 7, y + h - 2.5f, (w - 14) * p, 1.2f, 0.6f, Canvas.mulAlpha(e.on ? Hud.ACCENT : 0xFF77777E, 0.7f));
            cv.alpha = prev;
        }
        LIST.removeIf(e -> now - e.start > e.life && e.anim.get() < 0.01f);
    }
}
