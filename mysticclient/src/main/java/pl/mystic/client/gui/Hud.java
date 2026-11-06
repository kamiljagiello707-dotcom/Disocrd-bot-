package pl.mystic.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import pl.mystic.client.MysticClient;
import pl.mystic.client.module.Module;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;

public final class Hud {
    private Hud() {}

    public static int color(int offset) {
        // Fioletowo-błękitny "mistyczny" gradient.
        float t = (float) (((System.currentTimeMillis() / 12.0) + offset) % 360) / 360f;
        float hue = 0.72f + 0.12f * (float) Math.sin(t * Math.PI * 2);
        return Color.HSBtoRGB(hue, 0.65f, 1f) | 0xFF000000;
    }

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud() || MysticClient.modules == null) return;
        var tr = mc.textRenderer;

        String title = MysticClient.NAME + " §7v" + MysticClient.VERSION;
        ctx.fill(2, 2, 6 + tr.getWidth(title), 14, 0x90000000);
        ctx.fill(2, 2, 3, 14, color(0));
        ctx.drawTextWithShadow(tr, title, 5, 4, color(0));

        List<Module> on = MysticClient.modules.all().stream().filter(Module::isEnabled)
                .sorted(Comparator.comparingInt((Module m) -> tr.getWidth(m.getName())).reversed()).toList();
        int screenW = ctx.getScaledWindowWidth();
        int y = 2;
        for (int i = 0; i < on.size(); i++) {
            String name = on.get(i).getName();
            int w = tr.getWidth(name);
            int x = screenW - w - 5;
            ctx.fill(x - 2, y, screenW - 2, y + 11, 0x90000000);
            ctx.fill(screenW - 2, y, screenW - 1, y + 11, color(i * 20));
            ctx.drawTextWithShadow(tr, name, x, y + 2, color(i * 20));
            y += 11;
        }
    }
}
