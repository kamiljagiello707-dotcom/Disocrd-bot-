package dev.vesc.client.hud;

import dev.vesc.client.VescClient;
import dev.vesc.client.lang.Lang;
import dev.vesc.client.module.Module;
import dev.vesc.client.render.Fonts;
import dev.vesc.client.render.Fonts.Font;
import dev.vesc.client.render.R2D;
import dev.vesc.client.ui.Anim;
import dev.vesc.client.ui.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Powiadomienia (embed) w prawym dolnym rogu: ikona modułu, Enabled/Disabled, nazwa. */
public class Notifications {
    public static final class Notification {
        final String icon, title, subtitle;
        /** nazwa modułu (styl Pill) – null dla zwykłych informacji */
        String name;
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
        Notification n = new Notification(m.getIcon(), title, sub, m.isEnabled() ? 1 : 0, 2200);
        n.name = m.getName();
        push(n);
    }

    public void info(String icon, String title, String subtitle) {
        push(new Notification(icon, title, subtitle, -1, 2200));
    }

    private void push(Notification n) {
        Hud hud = VescClient.get().hud;
        if (hud != null && !hud.notifs.enabled.get()) return;
        list.add(0, n);
        if (list.size() > 6) list.remove(list.size() - 1);
    }

    public static final float W = 132, H = 30, GAP = 5;
    /** styl Pill: rozmiar ramki elementu w edytorze HUD i wysokość jednej pigułki */
    public static final float PW = 150, PH = 15, PGAP = 3.5f;

    public static boolean pill() {
        VescClient rc = VescClient.get();
        return rc == null || rc.settings.notifStyle.get().equals("Pill");
    }

    public boolean isEmpty() { return list.isEmpty(); }

    /**
     * Rysuje stos powiadomień zaczepiony w (ax, ay) – najnowsze w tym miejscu, starsze w górę (up)
     * lub w dół. Wjeżdżają z bliższej krawędzi ekranu (fromRight).
     */
    public void render(DrawContext ctx, float ax, float ay, boolean up, boolean fromRight, boolean preview) {
        long now = System.currentTimeMillis();
        list.removeIf(n -> now - n.start > n.duration + 400);
        float w = W, h = H, gap = GAP;
        if (list.isEmpty() && preview) {
            drawCard(ctx, new Notification("target", Lang.get("enabled"), "KillAura  •  " + Lang.format("notif.key", "R"), 1, 2200), ax, ay, w, h, 0.35f);
            return;
        }
        int i = 0;
        for (Notification n : list) {
            long t = now - n.start;
            float in = Anim.easeOutBack(t / 350f);
            float out = t > n.duration ? Anim.easeInCubic((t - n.duration) / 350f) : 0;
            float slide = (1 - in) + out; // 0 = widoczne, 1 = schowane
            float targetY = ay + (up ? -i : i) * (h + gap);
            if (n.y.raw() == -1) n.y.snap(targetY);
            float y = n.y.animate(targetY);
            float x = ax + (fromRight ? 1 : -1) * slide * (w + 20);
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
            R2D.glow(ctx, ix - 8, iy - 8, ic + 16, ic + 16, R2D.withAlpha(ac, 0.45f));
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

    // ================================================================== styl Pill

    /**
     * Pigułki "Enabled KillAura" – wyśrodkowane względem elementu (albo do lewej/prawej, gdy element stoi przy krawędzi).
     * Najnowsza na górze, starsze zjeżdżają niżej i lekko przygasają. Kolor przełącznika, pierścienia i poświaty
     * to akcent motywu (zmienisz akcent na zielony → powiadomienia też są zielone).
     */
    public void renderPills(DrawContext ctx, float ax, float ay, float ew, int sw, int sh, boolean preview) {
        long now = System.currentTimeMillis();
        list.removeIf(n -> now - n.start > n.duration + 400);
        float h = PH, gap = PGAP;
        float center = ax + ew / 2f;
        int align = center < sw / 3f ? -1 : center > sw * 2f / 3f ? 1 : 0;
        boolean down = ay + h / 2f < sh * 0.72f;
        if (list.isEmpty() && preview) {
            String[][] demo = {{"1", "TargetESP"}, {"1", "Fullbright"}, {"0", "Zoom"}};
            for (int i = 0; i < demo.length; i++) {
                Notification n = new Notification("target", Lang.get(demo[i][0].equals("1") ? "enabled" : "disabled"), "", Integer.parseInt(demo[i][0]), 2200);
                n.name = demo[i][1];
                float w = pillWidth(n);
                float x = align < 0 ? ax : align > 0 ? ax + ew - w : center - w / 2f;
                float prev = R2D.alpha;
                R2D.alpha = prev * (0.85f - i * 0.15f);
                drawPill(ctx, n, x, ay + (down ? i : -i) * (h + gap), w, h, 0.35f + i * 0.2f, demo[i][0].equals("1") ? 1 : 0, 1);
                R2D.alpha = prev;
            }
            return;
        }
        int i = 0;
        for (Notification n : list) {
            if (i >= 4) break;
            long t = now - n.start;
            float in = Anim.easeOutCubic(t / 260f);
            float out = t > n.duration ? Anim.easeInCubic((t - n.duration) / 350f) : 0;
            float targetY = ay + (down ? i : -i) * (h + gap);
            if (n.y.raw() == -1) n.y.snap(targetY);
            float y = n.y.animate(targetY) + (1 - in) * (down ? -7 : 7) + out * 4;
            float w = pillWidth(n);
            float x = align < 0 ? ax : align > 0 ? ax + ew - w : center - w / 2f;
            // przełącznik w ikonie przesuwa się po pojawieniu (włączanie → w prawo, wyłączanie → w lewo)
            float k = Anim.easeOutBack(Math.max(0, (t - 90) / 320f));
            float knob = n.state == 1 ? k : n.state == 0 ? 1 - k : 1;
            float prev = R2D.alpha;
            R2D.alpha = prev * in * (1 - out) * (1 - Math.min(0.45f, i * 0.16f));
            drawPill(ctx, n, x, y, w, h, Math.min(1, t / (float) n.duration), knob, 0.96f + 0.04f * in);
            R2D.alpha = prev;
            i++;
        }
    }

    private static String pillText(Notification n) {
        return n.name != null ? n.name : n.subtitle;
    }

    private static float pillWidth(Notification n) {
        float tw = Fonts.width(n.title, Font.BOLD, 0.64f) + 4 + Fonts.width(Fonts.trim(pillText(n), Font.BOLD, 0.64f, 150), Font.BOLD, 0.64f);
        return 6 + 13 + 6 + tw + 7 + 9 + 4;
    }

    private void drawPill(DrawContext ctx, Notification n, float x, float y, float w, float h, float progress, float knob, float scale) {
        int acc = Theme.accentWave(0);
        boolean mono = Theme.accentPreset() == Theme.Accent.MONO;
        boolean on = n.state != 0;
        float r = h / 2f;
        // lekkie "wyskoczenie" przy pojawieniu
        if (scale < 1) { float dw = w * (1 - scale); x += dw / 2f; w -= dw; }
        // poświata w kolorze akcentu
        if (on) R2D.glow(ctx, x - 10, y - 8, w + 20, h + 16, R2D.withAlpha(acc, mono ? 0.10f : 0.24f));
        R2D.shadow(ctx, x, y + 1, w, h, 7, R2D.withAlpha(0xFF000000, 0.45f));
        int bg = R2D.blend(0xFF09090D, acc, mono ? 0.04f : 0.10f);
        R2D.roundRectV(ctx, x, y, w, h, r, R2D.withAlpha(R2D.blend(bg, 0xFFFFFFFF, 0.05f), 0.84f), R2D.withAlpha(bg, 0.84f));
        R2D.roundOutline(ctx, x, y, w, h, r, 0.6f, on ? R2D.withAlpha(acc, mono ? 0.22f : 0.42f) : R2D.withAlpha(0xFFFFFFFF, 0.08f));

        // ikona: mini przełącznik (moduły) albo ikona (informacje)
        float tx0 = x + 6, cy = y + h / 2f;
        if (n.state >= 0) {
            float tw = 13, th = 7;
            int offTrack = R2D.withAlpha(0xFFFFFFFF, 0.16f);
            int onTrack = mono ? 0xFFF2F2F5 : acc;
            R2D.roundRect(ctx, tx0, cy - th / 2f, tw, th, th / 2f, R2D.blend(offTrack, onTrack, Anim.clamp01(knob)));
            float kx = tx0 + th / 2f + knob * (tw - th);
            R2D.circle(ctx, kx, cy, 2.3f, R2D.blend(0xFFB4B4BE, Theme.contrast(onTrack), Anim.clamp01(knob)));
        } else {
            R2D.icon(ctx, n.icon, tx0 + 2, cy - 4.5f, 9, mono ? 0xFFF2F2F5 : acc);
        }

        // tekst: "Enabled" w kolorze akcentu + nazwa modułu
        float tx = tx0 + 13 + 6;
        int stCol = !on ? 0xFF8E8E9A : mono ? 0xFFF4F3F8 : R2D.blend(acc, 0xFFFFFFFF, 0.28f);
        Fonts.drawV(ctx, n.title, tx, cy + 0.5f, stCol, Font.BOLD, 0.64f);
        tx += Fonts.width(n.title, Font.BOLD, 0.64f) + 4;
        Fonts.drawV(ctx, Fonts.trim(pillText(n), Font.BOLD, 0.64f, 150), tx, cy + 0.5f, on ? 0xFFF4F3F8 : 0xFFB4B4BE, Font.BOLD, 0.64f);

        // pierścień czasu po prawej
        float rr = 3.6f, rcx = x + w - 4 - 4.5f;
        R2D.roundOutline(ctx, rcx - rr - 0.6f, cy - rr - 0.6f, (rr + 0.6f) * 2, (rr + 0.6f) * 2, rr + 0.6f, 1.2f, R2D.withAlpha(0xFFFFFFFF, 0.12f));
        float left = 1 - Anim.clamp01(progress);
        int dots = (int) Math.ceil(left * 44);
        int ringCol = on ? (mono ? 0xFFF2F2F5 : acc) : 0xFF8E8E9A;
        for (int d = 0; d < dots; d++) {
            double a = -Math.PI / 2 + (d / 44.0) * Math.PI * 2;
            R2D.circle(ctx, rcx + (float) Math.cos(a) * rr, cy + (float) Math.sin(a) * rr, 0.62f, ringCol);
        }
    }

}
