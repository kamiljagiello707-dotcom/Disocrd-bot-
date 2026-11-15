package dev.razer.client.hud;

import dev.razer.client.RazerClient;
import dev.razer.client.lang.Lang;
import dev.razer.client.module.Module;
import dev.razer.client.render.Fonts;
import dev.razer.client.render.Fonts.Font;
import dev.razer.client.render.R2D;
import dev.razer.client.ui.Anim;
import dev.razer.client.ui.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Powiadomienia (embed) w prawym dolnym rogu: ikona modułu, Enabled/Disabled, nazwa. */
public class Notifications {
    public static final class Notification {
        final String icon, title, subtitle;
        final int state; // 1 = on, 0 = off, -1 = info
        final long start = System.currentTimeMillis();
        final long duration;
        final Anim y = new Anim(-1, 14);
        Notification(String icon, String title, String subtitle, int state, long duration) {
            this.icon = icon; this.title = title; this.subtitle = subtitle; this.state = state; this.duration = duration;
        }
    }

    private final List<Notification> list = new ArrayList<>();

    public void pushModule(Module m, boolean byKey) {
        String title = (m.isEnabled() ? Lang.get("enabled") : Lang.get("disabled"));
        String sub = m.getName() + (byKey && m.hasKey() ? "  •  " + Lang.format("notif.key", Keys.name(m.getKey())) : "");
        push(new Notification(m.getIcon(), title, sub, m.isEnabled() ? 1 : 0, 2200));
    }

    public void info(String icon, String title, String subtitle) {
        push(new Notification(icon, title, subtitle, -1, 2200));
    }

    private void push(Notification n) {
        if (!RazerClient.get().settings.notifications.get()) return;
        list.add(0, n);
        if (list.size() > 6) list.remove(list.size() - 1);
    }

    public void render(DrawContext ctx, int sw, int sh) {
        long now = System.currentTimeMillis();
        list.removeIf(n -> now - n.start > n.duration + 400);
        float w = 118, h = 28, gap = 4;
        float baseY = sh - 8;
        int i = 0;
        for (Notification n : list) {
            long t = now - n.start;
            float in = Anim.easeOutBack(t / 350f);
            float out = t > n.duration ? Anim.easeInCubic((t - n.duration) / 350f) : 0;
            float slide = (1 - in) + out; // 0 = widoczne, 1 = schowane
            float targetY = baseY - (i + 1) * (h + gap) + gap;
            if (n.y.raw() == -1) n.y.snap(targetY);
            float y = n.y.animate(targetY);
            float x = sw - w - 8 + slide * (w + 20);
            float prevAlpha = R2D.alpha;
            R2D.alpha = prevAlpha * (1 - out);
            drawCard(ctx, n, x, y, w, h, Math.min(1, t / (float) n.duration));
            R2D.alpha = prevAlpha;
            i++;
        }
    }

    private void drawCard(DrawContext ctx, Notification n, float x, float y, float w, float h, float progress) {
        R2D.shadow(ctx, x, y, w, h, 7, Theme.shadow());
        R2D.roundRect(ctx, x, y, w, h, 4.5f, Theme.hudBg());
        R2D.roundOutline(ctx, x, y, w, h, 4.5f, 0.5f, Theme.stroke());
        boolean on = n.state != 0;
        R2D.icon(ctx, n.icon, x + 7, y + 7, 8, on ? Theme.accent() : Theme.textFaint());
        float tx = x + 20;
        Fonts.drawV(ctx, n.title, tx, y + 11.5f, Theme.text(), Font.BOLD, 0.72f);
        if (n.state >= 0) {
            String st = on ? "ON" : "OFF";
            Fonts.drawV(ctx, st, x + w - 7 - Fonts.width(st, Font.BOLD, 0.58f), y + 11.5f, on ? Theme.text() : Theme.textFaint(), Font.BOLD, 0.58f);
        }
        Fonts.drawV(ctx, Fonts.trim(n.subtitle, Font.REGULAR, 0.62f, w - 27), tx, y + 21, Theme.textDim(), Font.REGULAR, 0.62f);
        R2D.rect(ctx, x + 4.5f, y + h - 1.5f, (w - 9) * (1 - progress), 1, Theme.accent());
    }
}
