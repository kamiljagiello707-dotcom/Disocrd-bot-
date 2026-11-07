package dev.testclient.gui;

import dev.testclient.TestClient;
import dev.testclient.module.Module;
import dev.testclient.module.render.HudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.Comparator;
import java.util.List;

public final class Hud {
    private Hud() {}

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        HudModule hud = TestClient.modules().get(HudModule.class);
        if (hud == null || !hud.isEnabled() || mc.player == null || mc.getDebugHud().shouldShowDebugHud()) return;
        TextRenderer tr = mc.textRenderer;
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();

        if (hud.watermark.get()) {
            String text = TestClient.NAME + " | " + mc.getCurrentFps() + " FPS";
            ctx.fill(2, 2, 8 + tr.getWidth(text), 15, 0x90000000);
            ctx.fill(2, 2, 4, 15, Theme.ACCENT);
            ctx.drawTextWithShadow(tr, text, 6, 5, Theme.TEXT);
        }

        if (hud.arrayList.get()) {
            List<Module> enabled = TestClient.modules().getModules().stream()
                    .filter(m -> m.isEnabled() && m != hud)
                    .sorted(Comparator.comparingInt((Module m) -> tr.getWidth(m.getName())).reversed())
                    .toList();
            int y = 2;
            for (int i = 0; i < enabled.size(); i++) {
                String name = enabled.get(i).getName();
                int w = tr.getWidth(name);
                int color = Theme.rainbow(i * 200L);
                ctx.fill(sw - w - 6, y, sw, y + 11, 0x90000000);
                ctx.fill(sw - 2, y, sw, y + 11, color);
                ctx.drawTextWithShadow(tr, name, sw - w - 4, y + 2, color);
                y += 11;
            }
        }

        if (hud.coords.get()) {
            String c = String.format("XYZ: %.1f / %.1f / %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            boolean chat = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
            ctx.drawTextWithShadow(tr, c, 4, sh - (chat ? 26 : 12), Theme.TEXT);
        }
    }
}
