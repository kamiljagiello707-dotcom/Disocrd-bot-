package pl.firen.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import pl.firen.client.FirenClient;
import pl.firen.client.gui.Theme;
import pl.firen.client.module.modules.ClientSettings;
import pl.firen.client.render.Fonts;
import pl.firen.client.render.Render2D;

import java.util.ArrayList;
import java.util.List;

/** Wyskakujące powiadomienia (toasty) w rogu ekranu, np. o włączeniu modułu. */
public final class Notifications {
    private Notifications() {}

    public enum Type {
        ENABLE(0xFF3DDC97, Fonts.I_CHECK),
        DISABLE(0xFFFF5C6A, "✕"),
        INFO(0xFF4DA3FF, "i"),
        WARN(0xFFE9C46A, "!");
        final int color; final String icon;
        Type(int c, String i) { this.color = c; this.icon = i; }
    }

    private static final class Toast {
        final String title, sub; final Type type; final long born; final long life;
        float anim;
        Toast(String title, String sub, Type type, long life) {
            this.title = title; this.sub = sub; this.type = type; this.born = System.currentTimeMillis(); this.life = life;
        }
    }

    private static final List<Toast> TOASTS = new ArrayList<>();

    public static void post(String title, String sub, Type type) {
        ClientSettings cs = settings();
        if (cs != null && !cs.notifications.get()) return;
        long life = cs != null ? (long) (cs.duration.get() * 1000) : 2500;
        synchronized (TOASTS) {
            TOASTS.add(new Toast(title, sub, type, life));
            while (TOASTS.size() > 6) TOASTS.remove(0);
        }
    }

    public static void module(String name, boolean enabled, String suffix) {
        post(name, enabled ? "włączony" + (suffix != null ? " · " + suffix : "") : "wyłączony",
                enabled ? Type.ENABLE : Type.DISABLE);
    }

    private static ClientSettings settings() {
        var m = FirenClient.modules();
        return m == null ? null : m.get(ClientSettings.class);
    }

    public static void render(DrawContext ctx, float s, float dt) {
        ClientSettings cs = settings();
        if (cs == null || !cs.notifications.get()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        float vw = ctx.getScaledWindowWidth() / s, vh = ctx.getScaledWindowHeight() / s;
        boolean right = cs.position.get().contains("Right");
        boolean bottom = cs.position.get().contains("Bottom");

        long now = System.currentTimeMillis();
        synchronized (TOASTS) {
            TOASTS.removeIf(t -> now - t.born > t.life + 400);
            float stackY = bottom ? vh - 8 : 8;
            for (int i = TOASTS.size() - 1; i >= 0; i--) {
                Toast t = TOASTS.get(i);
                long age = now - t.born;
                float target = age > t.life ? 0f : 1f;
                t.anim = Anim.approach(t.anim, target, dt, 14f);
                float a = Anim.easeOutCubic(t.anim);
                if (a < 0.01f) continue;

                float w = Math.max(132, Fonts.width(t.title, Fonts.SEMIBOLD) + 44), h = 30;
                float x = right ? vw - 8 - w + (1 - a) * (w + 14) : 8 - (1 - a) * (w + 14);
                float y = bottom ? stackY - h : stackY;

                Render2D.alpha = a;
                Render2D.shadow(ctx, x, y + 2, w, h, 9, 10, 0x80000000);
                Render2D.round(ctx, x, y, w, h, 9, 0xF2111116);
                Render2D.outline(ctx, x, y, w, h, 9, 0.5f, (t.type.color & 0xFFFFFF) | 0x40000000);
                // pasek akcentu z lewej + ikona
                Render2D.round(ctx, x, y + 6, 2.5f, h - 12, 1.25f, 0xFF000000 | (t.type.color & 0xFFFFFF));
                Render2D.round(ctx, x + 8, y + 7, 16, 16, 5, (t.type.color & 0xFFFFFF) | 0x28000000);
                Fonts.drawCentered(ctx, t.type.icon, t.type.icon.length() == 1 ? Fonts.SEMIBOLD : Fonts.ICONS, x + 16, y + 11.5f, 0xFF000000 | (t.type.color & 0xFFFFFF));
                Fonts.draw(ctx, t.title, Fonts.SEMIBOLD, x + 30, y + 7, Theme.TEXT);
                Fonts.draw(ctx, t.sub, Fonts.SMALL, x + 30, y + 18, Theme.TEXT_3);
                // pasek czasu
                float prog = Math.max(0, 1 - (float) age / t.life);
                Render2D.round(ctx, x + 8, y + h - 3, (w - 16) * prog, 1.5f, 0.75f, (t.type.color & 0xFFFFFF) | 0x90000000);

                stackY += (bottom ? -1 : 1) * (h + 5) * a;
            }
        }
        Render2D.alpha = 1f;
    }
}
