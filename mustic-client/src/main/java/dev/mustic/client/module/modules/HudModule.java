package dev.mustic.client.module.modules;

import dev.mustic.client.MusticClient;
import dev.mustic.client.module.Category;
import dev.mustic.client.module.Module;
import dev.mustic.client.module.ModuleManager;
import dev.mustic.client.setting.BooleanSetting;
import dev.mustic.client.util.Render2D;
import dev.mustic.client.util.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HudModule extends Module {
    private final BooleanSetting watermark = add(new BooleanSetting("Watermark", true));
    private final BooleanSetting arrayList = add(new BooleanSetting("ArrayList", true));
    private final BooleanSetting coords = add(new BooleanSetting("Coords", true));
    private final BooleanSetting fps = add(new BooleanSetting("FPS", true));

    public HudModule() {
        super("HUD", "Watermark, lista modulow i informacje na ekranie.", Category.CLIENT);
        hideFromArrayList();
        setEnabled(true);
    }

    @Override
    public void onRender2D(DrawContext ctx, float tickDelta) {
        TextRenderer tr = mc.textRenderer;
        if (watermark.get()) drawWatermark(ctx, tr);
        if (arrayList.get()) drawArrayList(ctx, tr);

        int y = ctx.getScaledWindowHeight() - 12;
        if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) y -= 14;
        if (coords.get() && mc.player != null) {
            String text = String.format(java.util.Locale.ROOT, "XYZ %.1f %.1f %.1f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ());
            ctx.drawText(tr, "XYZ ", 4, y, Theme.accent(), true);
            ctx.drawText(tr, text.substring(4), 4 + tr.getWidth("XYZ "), y, Theme.TEXT, true);
            y -= 11;
        }
        if (fps.get()) {
            ctx.drawText(tr, "FPS ", 4, y, Theme.accent(200), true);
            ctx.drawText(tr, String.valueOf(mc.getCurrentFps()), 4 + tr.getWidth("FPS "), y, Theme.TEXT, true);
        }
    }

    private void drawWatermark(DrawContext ctx, TextRenderer tr) {
        String name = "Mustic";
        String rest = "Client";
        String ver = " v" + MusticClient.VERSION;
        int w = tr.getWidth(name + rest + ver) + 10;
        Render2D.rect(ctx, 3, 3, w, 15, 0xB0101016);
        Render2D.horizontalGradient(ctx, 3, 3, w, 1, Theme.accent(0), Theme.accent(800));
        int x = 8;
        for (int i = 0; i < name.length(); i++) {
            String c = String.valueOf(name.charAt(i));
            ctx.drawText(tr, c, x, 7, Theme.accent(i * 120L), true);
            x += tr.getWidth(c);
        }
        ctx.drawText(tr, rest, x, 7, Theme.TEXT, true);
        x += tr.getWidth(rest);
        ctx.drawText(tr, ver, x, 7, Theme.TEXT_DIM, true);
    }

    private void drawArrayList(DrawContext ctx, TextRenderer tr) {
        List<Module> list = new ArrayList<>();
        for (Module m : ModuleManager.getModules()) {
            if (!m.isVisibleInArrayList()) continue;
            float a = m.arrayAnim.update(m.isEnabled() ? 1f : 0f);
            if (a > 0.01f) list.add(m);
        }
        list.sort(Comparator.comparingInt((Module m) -> tr.getWidth(label(m))).reversed());

        int screenW = ctx.getScaledWindowWidth();
        float y = 3;
        int i = 0;
        for (Module m : list) {
            float a = m.arrayAnim.get();
            String name = m.getName();
            String suffix = m.getSuffix();
            int w = tr.getWidth(label(m)) + 6;
            int x = (int) (screenW - 3 - w * a);
            int h = (int) (11 * a);
            int color = Theme.accent(i * 150L);
            Render2D.rect(ctx, x - 2, (int) y, w + 2, h, 0xA0101016);
            Render2D.rect(ctx, screenW - 3, (int) y, 1, h, color);
            if (a > 0.5f) {
                ctx.drawText(tr, name, x + 1, (int) y + 2, color, true);
                if (suffix != null)
                    ctx.drawText(tr, " " + suffix, x + 1 + tr.getWidth(name), (int) y + 2, Theme.TEXT_DIM, true);
            }
            y += 11 * a;
            i++;
        }
    }

    private static String label(Module m) {
        return m.getSuffix() == null ? m.getName() : m.getName() + " " + m.getSuffix();
    }
}
