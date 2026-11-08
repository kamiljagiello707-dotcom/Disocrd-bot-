package dev.wave.notification;

import dev.wave.WaveClient;
import dev.wave.module.client.Notifications;
import dev.wave.util.ColorUtil;
import dev.wave.util.Easing;
import dev.wave.util.RenderUtil;
import dev.wave.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager {
    private static final float HEIGHT = 30, GAP = 4, ANIM = 280;
    private final List<Notification> list = new CopyOnWriteArrayList<>();

    public void push(String title, String message, Notification.Type type) {
        Notifications cfg = WaveClient.INSTANCE.modules.get(Notifications.class);
        if (!cfg.isEnabled()) return;
        list.add(new Notification(title, message, type, (long) (cfg.duration.get() * 1000) + (long) ANIM * 2));
        while (list.size() > 6) list.remove(0);
        if (cfg.sound.isOn()) {
            float pitch = type == Notification.Type.DISABLED ? 0.8f : 1.25f;
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.35f));
        }
    }

    public void render(DrawContext c, int sw, int sh) {
        list.removeIf(Notification::expired);
        if (list.isEmpty()) return;
        Notifications cfg = WaveClient.INSTANCE.modules.get(Notifications.class);
        boolean top = !cfg.position.is("BottomRight");
        boolean center = cfg.position.is("TopCenter");

        int i = 0;
        for (int idx = list.size() - 1; idx >= 0; idx--, i++) {
            Notification n = list.get(idx);
            float targetY = top ? 6 + i * (HEIGHT + GAP) : sh - 6 - (i + 1) * (HEIGHT + GAP) + GAP;
            if (n.y.get() < 0) n.y.set(targetY);
            n.y.setTarget(targetY);
            float ny = n.y.update();

            long age = n.age();
            float in = Easing.outBack(age / ANIM);
            float out = age > n.duration - ANIM ? Easing.inCubic((age - (n.duration - ANIM)) / ANIM) : 0;
            float slide = Math.max(0, in) * (1 - out);

            float w = Math.max(130, Math.max(RenderUtil.width(n.title), RenderUtil.width(n.message)) + 62);
            float nx;
            if (center) {
                nx = sw / 2f - w / 2;
                ny = ny - (1 - slide) * 20;
                RenderUtil.globalAlpha = Easing.clamp01(slide);
            } else {
                nx = sw - 6 - w + (1 - slide) * (w + 12);
                RenderUtil.globalAlpha = Easing.clamp01(1 - out);
            }
            draw(c, n, nx, ny, w, age);
            RenderUtil.globalAlpha = 1f;
        }
    }

    private void draw(DrawContext c, Notification n, float x, float y, float w, long age) {
        RenderUtil.round(c, x, y, w, HEIGHT, 5, Theme.BG);
        RenderUtil.roundOutline(c, x, y, w, HEIGHT, 5, 0.5f, Theme.BORDER);

        // icon
        float ix = x + 15, iy = y + HEIGHT / 2 - 1;
        switch (n.type) {
            case ENABLED -> {
                RenderUtil.circle(c, ix, iy, 7, Theme.accent());
                RenderUtil.text(c, "✔", ix - 3, iy - 4, Theme.onAccent(), false);
            }
            case DISABLED -> {
                RenderUtil.roundOutline(c, ix - 7, iy - 7, 14, 14, 7, 1.2f, Theme.TEXT);
                RenderUtil.text(c, "✖", ix - 3, iy - 4, Theme.TEXT, false);
            }
            case WARNING -> {
                RenderUtil.circle(c, ix, iy, 7, 0xFFFFB020);
                RenderUtil.text(c, "!", ix - 1, iy - 4, Theme.TEXT_DARK, false);
            }
            default -> {
                RenderUtil.circle(c, ix, iy, 7, Theme.TEXT);
                RenderUtil.text(c, "i", ix - 1, iy - 4, Theme.TEXT_DARK, false);
            }
        }

        RenderUtil.text(c, n.title, x + 28, y + 6, Theme.TEXT, false);
        RenderUtil.text(c, n.message, x + 28, y + 16, Theme.TEXT_DIM, false);

        // animated mini toggle switch on the right for module toggles
        if (n.type == Notification.Type.ENABLED || n.type == Notification.Type.DISABLED) {
            float t = Easing.outCubic((age - 120) / 260f);
            float knob = n.type == Notification.Type.ENABLED ? t : 1 - t;
            float sx = x + w - 26, sy = y + HEIGHT / 2 - 5;
            RenderUtil.round(c, sx, sy, 18, 10, 5, ColorUtil.lerp(0xFF2A2A2A, Theme.accent(), knob));
            RenderUtil.circle(c, sx + 5 + knob * 8, sy + 5, 3.5f, ColorUtil.lerp(0xFF9A9A9A, Theme.onAccent(), knob));
        }

        // progress bar
        float progress = 1f - Easing.clamp01(age / (float) n.duration);
        RenderUtil.rect(c, x + 5, y + HEIGHT - 2.5f, (w - 10), 1, 0x30FFFFFF);
        RenderUtil.rect(c, x + 5, y + HEIGHT - 2.5f, (w - 10) * progress, 1, Theme.accent());
    }
}
