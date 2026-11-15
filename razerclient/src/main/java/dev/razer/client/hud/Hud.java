package dev.razer.client.hud;

import dev.razer.client.RazerClient;
import dev.razer.client.lang.Lang;
import dev.razer.client.module.Module;
import dev.razer.client.render.Fonts;
import dev.razer.client.render.Fonts.Font;
import dev.razer.client.render.R2D;
import dev.razer.client.ui.Anim;
import dev.razer.client.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** HUD: watermark (prawy górny), lista modułów i KeyBinds (lewy górny), powiadomienia. */
public class Hud {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private final Anim modulesHeight = new Anim(0, 12);
    private final Anim bindsHeight = new Anim(0, 12);
    private final Anim modulesWidth = new Anim(110, 12);
    private final Anim bindsWidth = new Anim(110, 12);

    public void render(DrawContext ctx, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;
        RazerClient rc = RazerClient.get();
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        R2D.alpha = 1f;

        if (rc.settings.watermark.get()) watermark(ctx, mc, sw);
        float y = 8;
        if (rc.settings.arraylist.get()) y = moduleList(ctx, rc, y) + 6;
        if (rc.settings.keybinds.get()) keybinds(ctx, rc, y);
        rc.notifications.render(ctx, sw, sh);
    }

    // ------------------------------------------------------------ watermark

    private void watermark(DrawContext ctx, MinecraftClient mc, int sw) {
        String name = mc.getSession().getUsername();
        String[] parts = {"RazerClient", mc.getCurrentFps() + " " + Lang.get("wm.fps"), LocalTime.now().format(TIME), name};
        Font[] fonts = {Font.BOLD, Font.SEMIBOLD, Font.SEMIBOLD, Font.SEMIBOLD};
        float s = 0.9f, pad = 8, dotGap = 7, iconSize = 12;
        float w = pad + iconSize + 5;
        for (int i = 0; i < parts.length; i++) {
            w += Fonts.width(parts[i], fonts[i], s);
            if (i < parts.length - 1) w += dotGap * 2 + 2;
        }
        w += pad;
        float h = 20, x = sw - w - 8, y = 8;
        R2D.shadow(ctx, x, y, w, h, 10, Theme.shadow());
        R2D.roundRect(ctx, x, y, w, h, 6, Theme.hudBg());
        R2D.roundOutline(ctx, x, y, w, h, 6, 0.5f, Theme.stroke());
        float cx = x + pad;
        R2D.icon(ctx, "logo", cx, y + (h - iconSize) / 2f, iconSize, Theme.accent());
        cx += iconSize + 5;
        for (int i = 0; i < parts.length; i++) {
            Fonts.drawV(ctx, parts[i], cx, y + h / 2f, i == 0 ? Theme.text() : Theme.textDim(), fonts[i], s);
            cx += Fonts.width(parts[i], fonts[i], s);
            if (i < parts.length - 1) {
                cx += dotGap;
                R2D.circle(ctx, cx + 1, y + h / 2f, 1.2f, Theme.textFaint());
                cx += 2 + dotGap;
            }
        }
    }

    // ------------------------------------------------------------ lista modułów

    private float header(DrawContext ctx, float x, float y, float w, String icon, String title, String count) {
        R2D.roundRect(ctx, x + 6, y + 6, 16, 16, 4.5f, Theme.accent());
        R2D.icon(ctx, icon, x + 9, y + 9, 10, Theme.accentText());
        Fonts.drawV(ctx, title, x + 27, y + 14, Theme.text(), Font.BOLD, 0.95f);
        float cw = Fonts.width(count, Font.SEMIBOLD, 0.75f) + 8;
        R2D.roundRect(ctx, x + w - cw - 6, y + 8, cw, 12, 6, Theme.toggleOff());
        Fonts.drawV(ctx, count, x + w - cw - 2, y + 14, Theme.textDim(), Font.SEMIBOLD, 0.75f);
        R2D.rect(ctx, x + 6, y + 27, w - 12, 0.5f, Theme.stroke());
        return y + 30;
    }

    private float moduleList(DrawContext ctx, RazerClient rc, float y) {
        float x = 8, rowH = 15, s = 0.85f;
        List<Module> shown = new ArrayList<>();
        for (Module m : rc.modules.all()) {
            float a = m.hudAnim.animate(m.isEnabled() ? 1 : 0);
            if (a > 0.01f) shown.add(m);
        }
        shown.sort(Comparator.comparingDouble(m -> -Fonts.width(m.getName(), Font.SEMIBOLD, s)));

        float targetW = 120;
        for (Module m : shown) targetW = Math.max(targetW, Fonts.width(m.getName(), Font.SEMIBOLD, s) + 50);
        float w = modulesWidth.animate(targetW);
        float contentH = 0;
        for (Module m : shown) contentH += rowH * Anim.easeOutCubic(m.hudAnim.raw());
        if (shown.isEmpty()) contentH = rowH;
        float h = modulesHeight.animate(30 + contentH + 5);

        R2D.shadow(ctx, x, y, w, h, 10, Theme.shadow());
        R2D.roundRect(ctx, x, y, w, h, 7, Theme.hudBg());
        R2D.roundOutline(ctx, x, y, w, h, 7, 0.5f, Theme.stroke());
        float cy = header(ctx, x, y, w, "layers", Lang.get("hud.modules"), String.valueOf(rc.modules.enabledCount()));

        R2D.scissor(ctx, x, cy, w, h - 30);
        if (shown.isEmpty()) {
            Fonts.drawV(ctx, Lang.get("hud.nomodules"), x + 8, cy + rowH / 2f + 1, Theme.textFaint(), Font.REGULAR, 0.8f);
        }
        for (Module m : shown) {
            float a = Anim.easeOutCubic(m.hudAnim.raw());
            float prev = R2D.alpha;
            R2D.alpha = prev * a;
            float ox = (1 - a) * -20;
            R2D.icon(ctx, m.getIcon(), x + 8 + ox, cy + 3.5f, 9, Theme.accent());
            Fonts.drawV(ctx, m.getName(), x + 22 + ox, cy + rowH / 2f + 0.5f, Theme.text(), Font.SEMIBOLD, s);
            String cat = m.getCategory().getName();
            Fonts.drawV(ctx, cat, x + w - 8 - Fonts.width(cat, Font.REGULAR, 0.7f) - ox, cy + rowH / 2f + 0.5f, Theme.textFaint(), Font.REGULAR, 0.7f);
            R2D.alpha = prev;
            cy += rowH * a;
        }
        R2D.endScissor(ctx);
        return y + h;
    }

    // ------------------------------------------------------------ keybinds

    private void keybinds(DrawContext ctx, RazerClient rc, float y) {
        float x = 8, rowH = 15, s = 0.85f;
        List<Module> bound = new ArrayList<>();
        for (Module m : rc.modules.all()) if (m.hasKey()) bound.add(m);

        float targetW = 120;
        for (Module m : bound) targetW = Math.max(targetW, Fonts.width(m.getName(), Font.SEMIBOLD, s) + Fonts.width(Keys.name(m.getKey()), Font.BOLD, 0.75f) + 54);
        float w = bindsWidth.animate(targetW);
        float h = bindsHeight.animate(30 + Math.max(1, bound.size()) * rowH + 5);

        R2D.shadow(ctx, x, y, w, h, 10, Theme.shadow());
        R2D.roundRect(ctx, x, y, w, h, 7, Theme.hudBg());
        R2D.roundOutline(ctx, x, y, w, h, 7, 0.5f, Theme.stroke());
        float cy = header(ctx, x, y, w, "keyboard", Lang.get("hud.keybinds"), String.valueOf(bound.size()));

        R2D.scissor(ctx, x, cy, w, h - 30);
        if (bound.isEmpty()) Fonts.drawV(ctx, Lang.get("hud.nobinds"), x + 8, cy + rowH / 2f + 1, Theme.textFaint(), Font.REGULAR, 0.8f);
        for (Module m : bound) {
            float on = m.toggleAnim.animate(m.isEnabled() ? 1 : 0);
            int nameCol = R2D.blend(Theme.textDim(), Theme.text(), on);
            R2D.icon(ctx, m.getIcon(), x + 8, cy + 3.5f, 9, nameCol);
            Fonts.drawV(ctx, m.getName(), x + 22, cy + rowH / 2f + 0.5f, nameCol, Font.SEMIBOLD, s);
            String k = Keys.name(m.getKey());
            float kw = Fonts.width(k, Font.BOLD, 0.75f) + 8;
            float kx = x + w - kw - 8;
            R2D.roundRect(ctx, kx, cy + 2, kw, 11, 3.5f, R2D.blend(Theme.toggleOff(), Theme.accent(), on));
            Fonts.drawV(ctx, k, kx + 4, cy + 7.5f, R2D.blend(Theme.textDim(), Theme.accentText(), on), Font.BOLD, 0.75f);
            R2D.circle(ctx, kx - 6, cy + rowH / 2f, 1.8f, R2D.blend(Theme.textFaint(), Theme.accent(), on));
            cy += rowH;
        }
        R2D.endScissor(ctx);
    }
}
