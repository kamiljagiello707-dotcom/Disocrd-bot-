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
        float w = 132, h = 30, gap = 5;
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
        boolean on = n.state != 0;
        Hud.panel(ctx, x, y, w, h, 10);
        // ikona w kółku
        float ic = 18, ix = x + 6, iy = y + (h - ic) / 2f;
        if (on) {
            int ac = Theme.accentWave(0);
            R2D.texture(ctx, R2D.id("textures/ui/glow.png"), ix - 8, iy - 8, ic + 16, ic + 16, R2D.withAlpha(ac, 0.45f));
            R2D.roundRectV(ctx, ix, iy, ic, ic, ic / 2f, R2D.blend(ac, 0xFFFFFFFF, 0.15f), R2D.blend(ac, 0xFF000000, 0.1f));
            R2D.icon(ctx, n.icon, ix + 5, iy + 5, 8, Theme.accentText());
        } else {
            R2D.circle(ctx, ix + ic / 2f, iy + ic / 2f, ic / 2f, Theme.toggleOff());
            R2D.icon(ctx, n.icon, ix + 5, iy + 5, 8, Theme.textFaint());
        }
        float tx = ix + ic + 6;
        Fonts.drawV(ctx, n.title, tx, y + 10.5f, Theme.text(), Font.BOLD, 0.72f);
        if (n.state >= 0) {
            String st = on ? "ON" : "OFF";
            float pw = Fonts.width(st, Font.BOLD, 0.52f) + 7;
            float px = x + w - 7 - pw;
            R2D.roundRect(ctx, px, y + 6.5f, pw, 8, 4, on ? R2D.withAlpha(0xFF4ADE80, 0.18f) : R2D.withAlpha(0xFFEF4444, 0.16f));
            Fonts.drawV(ctx, st, px + 3.5f, y + 10.6f, on ? 0xFF4ADE80 : 0xFFF87171, Font.BOLD, 0.52f);
        }
        Fonts.drawV(ctx, Fonts.trim(n.subtitle, Font.REGULAR, 0.6f, w - (tx - x) - 8), tx, y + 20, Theme.textDim(), Font.REGULAR, 0.6f);
        float pw = (w - 20) * (1 - progress);
        if (pw > 1) {
            R2D.roundRect(ctx, x + 10, y + h - 2.5f, w - 20, 1.3f, 0.65f, R2D.withAlpha(Theme.toggleOff(), 0.6f));
            R2D.roundRect(ctx, x + 10, y + h - 2.5f, pw, 1.3f, 0.65f, Theme.accentWave(0));
        }
    }
}
