package dev.vesc.client.hud;

import dev.vesc.client.VescClient;
import dev.vesc.client.lang.Lang;
import dev.vesc.client.render.Fonts;
import dev.vesc.client.render.Fonts.Font;
import dev.vesc.client.render.R2D;
import dev.vesc.client.ui.Theme;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;

/**
 * Przesuwanie elementów HUD (Watermark, KeyBinds, TargetHUD...) myszką przy otwartym czacie.
 * Lewy przycisk – przeciągnij, środkowy – przywróć domyślną pozycję.
 */
public final class ChatDrag {
    private static final float SNAP = 4;
    private static HudElement drag, hovered;
    private static float grabX, grabY;

    private ChatDrag() {}

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof ChatScreen)) return;
            drag = null;
            ScreenEvents.afterRender(screen).register(ChatDrag::render);
            ScreenMouseEvents.allowMouseClick(screen).register((s, mx, my, button) -> !click(s, mx, my, button));
            ScreenMouseEvents.afterMouseRelease(screen).register((s, mx, my, button) -> release());
            ScreenEvents.remove(screen).register(s -> release());
        });
    }

    private static Hud hud() { return VescClient.get().hud; }

    private static boolean inside(double mx, double my, HudElement e) {
        return mx >= e.lastX && mx < e.lastX + e.w && my >= e.lastY && my < e.lastY + e.h;
    }

    private static void render(Screen screen, DrawContext ctx, int mx, int my, float delta) {
        int sw = screen.width, sh = screen.height;
        Hud hud = hud();
        if (drag != null) {
            float nx = mx - grabX, ny = my - grabY;
            float[] xs = {SNAP, sw / 2f - drag.w / 2f, sw - drag.w - SNAP};
            for (float c : xs) if (Math.abs(nx - c) < SNAP) nx = c;
            float[] ys = {SNAP, sh / 2f - drag.h / 2f, sh - drag.h - SNAP};
            for (float c : ys) if (Math.abs(ny - c) < SNAP) ny = c;
            drag.setPosition(nx, ny, sw, sh);
            if (Math.abs(nx + drag.w / 2f - sw / 2f) < 0.5f) R2D.rect(ctx, sw / 2f - 0.5f, 0, 1, sh, R2D.withAlpha(Theme.accent(), 0.6f));
            if (Math.abs(ny + drag.h / 2f - sh / 2f) < 0.5f) R2D.rect(ctx, 0, sh / 2f - 0.5f, sw, 1, R2D.withAlpha(Theme.accent(), 0.6f));
        }
        hovered = drag;
        if (hovered == null)
            for (int i = hud.elements.size() - 1; i >= 0; i--) {
                HudElement e = hud.elements.get(i);
                if (e.enabled.get() && e.visible.raw() > 0.5f && inside(mx, my, e)) { hovered = e; break; }
            }
        R2D.alpha = 1;
        for (HudElement e : hud.elements) {
            if (!e.enabled.get() || e.visible.raw() < 0.5f) continue;
            boolean h = e == hovered;
            R2D.roundOutline(ctx, e.lastX - 2, e.lastY - 2, e.w + 4, e.h + 4, Hud.R + 2, h ? 1f : 0.5f,
                    h ? Theme.accent() : R2D.withAlpha(Theme.text(), 0.25f));
        }
        if (hovered != null) {
            String n = hovered.getName();
            float tw = Fonts.width(n, Font.BOLD, 0.62f) + 16;
            float ty = hovered.lastY - 14 < 2 ? hovered.lastY + hovered.h + 4 : hovered.lastY - 14;
            R2D.roundRect(ctx, hovered.lastX - 2, ty, tw, 11, 5.5f, Theme.accent());
            R2D.icon(ctx, hovered.icon, hovered.lastX + 2, ty + 2.5f, 6, Theme.accentText());
            Fonts.drawV(ctx, n, hovered.lastX + 10, ty + 5.8f, Theme.accentText(), Font.BOLD, 0.62f);
        }
        String hint = Lang.get("chatdrag.hint");
        float hw = Fonts.width(hint, Font.SEMIBOLD, 0.62f) + 22;
        float hx = sw / 2f - hw / 2f;
        R2D.roundRect(ctx, hx, 6, hw, 13, 6.5f, Theme.hudBg());
        R2D.icon(ctx, "move", hx + 6, 9.5f, 6, Theme.accentWave(0));
        Fonts.drawV(ctx, hint, hx + 15, 12.8f, Theme.textDim(), Font.SEMIBOLD, 0.62f);
    }

    /** @return true gdy kliknięcie zostało obsłużone (nie przekazuj do czatu) */
    private static boolean click(Screen screen, double mx, double my, int button) {
        Hud hud = hud();
        for (int i = hud.elements.size() - 1; i >= 0; i--) {
            HudElement e = hud.elements.get(i);
            if (!e.enabled.get() || e.visible.raw() < 0.5f || !inside(mx, my, e)) continue;
            if (button == 0) { drag = e; grabX = (float) mx - e.lastX; grabY = (float) my - e.lastY; }
            else if (button == 2) { e.resetPosition(); VescClient.get().config.saveClient(); }
            return true;
        }
        return false;
    }

    private static void release() {
        if (drag != null) VescClient.get().config.saveClient();
        drag = null;
    }
}
