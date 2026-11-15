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
        float y = 6;
        if (rc.settings.arraylist.get()) y = moduleList(ctx, rc, y) + 4;
        if (rc.settings.keybinds.get()) keybinds(ctx, rc, y);
        rc.notifications.render(ctx, sw, sh);
    }

    // wspólny kompaktowy styl paneli (jak małe "embedy")
    private static final float R = 4.5f, PADX = 6, HEAD = 15, ROW = 10.5f, NAME = 0.66f, ICON = 6.5f;

    private void panel(DrawContext ctx, float x, float y, float w, float h) {
        R2D.shadow(ctx, x, y, w, h, 7, Theme.shadow());
        R2D.roundRect(ctx, x, y, w, h, R, Theme.hudBg());
        R2D.roundOutline(ctx, x, y, w, h, R, 0.5f, Theme.stroke());
    }

    private float header(DrawContext ctx, float x, float y, float w, String icon, String title, String count) {
        R2D.icon(ctx, icon, x + PADX, y + HEAD / 2f - 3.75f, 7.5f, Theme.accent());
        Fonts.drawV(ctx, title, x + PADX + 11, y + HEAD / 2f + 0.5f, Theme.text(), Font.BOLD, 0.72f);
        Fonts.drawV(ctx, count, x + w - PADX - Fonts.width(count, Font.SEMIBOLD, 0.62f), y + HEAD / 2f + 0.5f, Theme.textFaint(), Font.SEMIBOLD, 0.62f);
        return y + HEAD;
    }

    // ------------------------------------------------------------ watermark

    private void watermark(DrawContext ctx, MinecraftClient mc, int sw) {
        String name = mc.getSession().getUsername();
        String[] parts = {"RazerClient", mc.getCurrentFps() + " " + Lang.get("wm.fps"), LocalTime.now().format(TIME), name};
        Font[] fonts = {Font.BOLD, Font.SEMIBOLD, Font.SEMIBOLD, Font.SEMIBOLD};
        float s = 0.7f, pad = 6, dotGap = 5, iconSize = 8.5f;
        float w = pad + iconSize + 4;
        for (int i = 0; i < parts.length; i++) {
            w += Fonts.width(parts[i], fonts[i], s);
            if (i < parts.length - 1) w += dotGap * 2 + 2;
        }
        w += pad;
        float h = 15, x = sw - w - 6, y = 6;
        panel(ctx, x, y, w, h);
        float cx = x + pad;
        R2D.icon(ctx, "logo", cx, y + (h - iconSize) / 2f, iconSize, Theme.accent());
        cx += iconSize + 4;
        for (int i = 0; i < parts.length; i++) {
            Fonts.drawV(ctx, parts[i], cx, y + h / 2f + 0.5f, i == 0 ? Theme.text() : Theme.textDim(), fonts[i], s);
            cx += Fonts.width(parts[i], fonts[i], s);
            if (i < parts.length - 1) {
                cx += dotGap;
                R2D.circle(ctx, cx + 1, y + h / 2f, 0.9f, Theme.textFaint());
                cx += 2 + dotGap;
            }
        }
    }

    // ------------------------------------------------------------ lista modułów

    private float moduleList(DrawContext ctx, RazerClient rc, float y) {
        float x = 6;
        List<Module> shown = new ArrayList<>();
        for (Module m : rc.modules.all()) {
            float a = m.hudAnim.animate(m.isEnabled() ? 1 : 0);
            if (a > 0.01f) shown.add(m);
        }
        shown.sort(Comparator.comparingDouble(m -> -Fonts.width(m.getName(), Font.SEMIBOLD, NAME)));

        float targetW = 84;
        for (Module m : shown) targetW = Math.max(targetW, Fonts.width(m.getName(), Font.SEMIBOLD, NAME) + PADX * 2 + 11);
        float w = modulesWidth.animate(targetW);
        float contentH = 0;
        for (Module m : shown) contentH += ROW * Anim.easeOutCubic(m.hudAnim.raw());
        if (shown.isEmpty()) contentH = ROW;
        float h = modulesHeight.animate(HEAD + contentH + 4);

        panel(ctx, x, y, w, h);
        float cy = header(ctx, x, y, w, "list", Lang.get("hud.modules"), String.valueOf(rc.modules.enabledCount()));
        R2D.scissor(ctx, x, cy, w, h - HEAD);
        if (shown.isEmpty()) Fonts.drawV(ctx, Lang.get("hud.nomodules"), x + PADX, cy + ROW / 2f + 0.5f, Theme.textFaint(), Font.REGULAR, 0.6f);
        for (Module m : shown) {
            float a = Anim.easeOutCubic(m.hudAnim.raw());
            float prev = R2D.alpha;
            R2D.alpha = prev * a;
            float ox = (1 - a) * -12;
            R2D.icon(ctx, m.getIcon(), x + PADX + ox, cy + ROW / 2f - ICON / 2f, ICON, Theme.accent());
            Fonts.drawV(ctx, m.getName(), x + PADX + 11 + ox, cy + ROW / 2f + 0.5f, Theme.text(), Font.SEMIBOLD, NAME);
            R2D.alpha = prev;
            cy += ROW * a;
        }
        R2D.endScissor(ctx);
        return y + h;
    }

    // ------------------------------------------------------------ keybinds

    private void keybinds(DrawContext ctx, RazerClient rc, float y) {
        float x = 6;
        List<Module> bound = new ArrayList<>();
        for (Module m : rc.modules.all()) if (m.hasKey()) bound.add(m);

        float targetW = 84;
        for (Module m : bound)
            targetW = Math.max(targetW, Fonts.width(m.getName(), Font.SEMIBOLD, NAME) + Fonts.width(Keys.name(m.getKey()), Font.SEMIBOLD, NAME) + PADX * 2 + 24);
        float w = bindsWidth.animate(targetW);
        float h = bindsHeight.animate(HEAD + Math.max(1, bound.size()) * ROW + 4);

        panel(ctx, x, y, w, h);
        float cy = header(ctx, x, y, w, "keyboard", Lang.get("hud.keybinds"), String.valueOf(bound.size()));
        R2D.scissor(ctx, x, cy, w, h - HEAD);
        if (bound.isEmpty()) Fonts.drawV(ctx, Lang.get("hud.nobinds"), x + PADX, cy + ROW / 2f + 0.5f, Theme.textFaint(), Font.REGULAR, 0.6f);
        for (Module m : bound) {
            float on = m.toggleAnim.animate(m.isEnabled() ? 1 : 0);
            int nameCol = R2D.blend(Theme.textFaint(), Theme.text(), on);
            R2D.icon(ctx, m.getIcon(), x + PADX, cy + ROW / 2f - ICON / 2f, ICON, R2D.blend(Theme.textFaint(), Theme.accent(), on));
            Fonts.drawV(ctx, m.getName(), x + PADX + 11, cy + ROW / 2f + 0.5f, nameCol, Font.SEMIBOLD, NAME);
            String k = Keys.name(m.getKey());
            Fonts.drawV(ctx, k, x + w - PADX - Fonts.width(k, Font.SEMIBOLD, NAME), cy + ROW / 2f + 0.5f,
                    R2D.blend(Theme.textFaint(), Theme.textDim(), on), Font.SEMIBOLD, NAME);
            cy += ROW;
        }
        R2D.endScissor(ctx);
    }
}
