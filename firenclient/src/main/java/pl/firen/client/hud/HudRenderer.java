package pl.firen.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import pl.firen.client.FirenClient;
import pl.firen.client.gui.Draw;
import pl.firen.client.gui.Theme;
import pl.firen.client.module.Module;
import pl.firen.client.module.modules.Hud;
import pl.firen.client.util.Anim;

import java.util.Comparator;
import java.util.List;

public final class HudRenderer {
    private long lastFrame = System.nanoTime();

    public void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;

        Hud hud = FirenClient.modules().get(Hud.class);
        if (!hud.isEnabled() || mc.options.hudHidden || mc.player == null) return;
        TextRenderer tr = mc.textRenderer;
        int sw = ctx.getScaledWindowWidth();

        if (hud.watermark.get()) {
            Text logo = Text.literal("Firen").formatted(Formatting.BOLD)
                    .append(Text.literal("Client").formatted(Formatting.GRAY));
            String fps = mc.getCurrentFps() + " FPS";
            int w = tr.getWidth(logo) + 8 + (hud.info.get() ? tr.getWidth(fps) + 8 : 0);
            Draw.round(ctx, 4, 4, 4 + w + 10, 20, 0xC0000000);
            ctx.fill(4, 4, 6, 20, Theme.WHITE);
            ctx.drawText(tr, logo, 11, 8, Theme.WHITE, false);
            if (hud.info.get()) {
                int fx = 11 + tr.getWidth(logo) + 8;
                ctx.fill(fx - 4, 8, fx - 3, 16, 0xFF3A3A3A);
                ctx.drawText(tr, fps, fx + 1, 8, Theme.TEXT, false);
            }
        }

        if (hud.info.get()) {
            String xyz = String.format("XYZ  %.0f  %.0f  %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            ctx.drawText(tr, xyz, 6, ctx.getScaledWindowHeight() - 12, Theme.TEXT, true);
        }

        if (hud.arrayList.get()) {
            List<Module> mods = FirenClient.modules().all().stream()
                    .peek(m -> m.arrayAnim = Anim.approach(m.arrayAnim, m.isEnabled() && m != hud ? 1f : 0f, dt, 14f))
                    .filter(m -> m.arrayAnim > 0.01f)
                    .sorted(Comparator.comparingInt((Module m) -> tr.getWidth(label(m))).reversed())
                    .toList();
            float y = 4;
            for (Module m : mods) {
                String name = m.getName();
                String suffix = m.getSuffix();
                int w = tr.getWidth(label(m)) + 8;
                int h = 11;
                int x = sw - 2 - Math.round(w * m.arrayAnim);
                int iy = Math.round(y);
                ctx.fill(x, iy, sw - 2, iy + h, 0xC0000000);
                ctx.fill(sw - 2, iy, sw, iy + h, Theme.WHITE);
                ctx.drawText(tr, name, x + 4, iy + 2, Theme.WHITE, false);
                if (suffix != null) ctx.drawText(tr, " " + suffix, x + 4 + tr.getWidth(name), iy + 2, Theme.TEXT_DIM, false);
                y += h * m.arrayAnim;
            }
        }
    }

    private static String label(Module m) {
        return m.getSuffix() == null ? m.getName() : m.getName() + " " + m.getSuffix();
    }

}
