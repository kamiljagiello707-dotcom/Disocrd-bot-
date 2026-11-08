package dev.nova.client.gui;

import dev.nova.client.NovaClient;
import dev.nova.client.module.Module;
import dev.nova.client.module.impl.ClickGuiModule;
import dev.nova.client.module.impl.HudModule;
import dev.nova.client.util.Animation;
import dev.nova.client.util.ColorUtil;
import dev.nova.client.util.Render2D;
import dev.nova.client.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Watermark + animated ArrayList of enabled modules. */
public final class HudRenderer {
    private final Map<Module, Animation> slide = new HashMap<>();

    public void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        HudModule hud = NovaClient.get().modules.get(HudModule.class);
        if (!hud.isEnabled() || mc.options.hudHidden || mc.currentScreen instanceof ClickGuiScreen) return;
        var font = mc.textRenderer;
        int sw = ctx.getScaledWindowWidth();

        if (hud.watermark.get()) {
            String text = "Nova";
            int w = font.getWidth(text) + 10;
            Render2D.roundRect(ctx, 4, 4, w, 14, 3, 0xC0101016);
            Render2D.accentLine(ctx, 6, 17, w - 4, 1, 1);
            ctx.drawText(font, text, 9, 7, Theme.accent(), false);
        }

        if (!hud.arrayList.get()) return;
        List<Module> mods = NovaClient.get().modules.all().stream()
                .filter(m -> !(m instanceof ClickGuiModule) && !(m instanceof HudModule))
                .sorted(Comparator.comparingInt((Module m) -> font.getWidth(m.getName())).reversed())
                .toList();
        double y = 4;
        for (Module m : mods) {
            double t = slide.computeIfAbsent(m, k -> new Animation(12, 0)).setTarget(m.isEnabled() ? 1 : 0).get();
            if (t < 0.01) continue;
            int tw = font.getWidth(m.getName());
            double w = tw + 8, h = 11 * t;
            double x = sw - 4 - w * t;
            int accent = Theme.accent(y * 4);
            ctx.enableScissor((int) x - 2, (int) y, sw, (int) Math.ceil(y + h));
            Render2D.rect(ctx, x, y, w, 11, ColorUtil.alpha(0xC0101016, t));
            Render2D.rect(ctx, x + w, y, 2, 11, accent);
            ctx.drawText(font, m.getName(), (int) (x + 4), (int) (y + 2), ColorUtil.alpha(accent, Math.max(0.05, t)), false);
            ctx.disableScissor();
            y += h;
        }
    }
}
