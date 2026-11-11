package tech.fuzir.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import tech.fuzir.client.gui.HudEditorScreen;
import tech.fuzir.client.module.HudModule;
import tech.fuzir.client.module.ModuleManager;
import tech.fuzir.client.module.impl.ClickGui;
import tech.fuzir.client.util.Canvas;

public final class Hud {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static final int PANEL = 0xD90C0C0E;
    public static final int BORDER = 0x16FFFFFF;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int TEXT = 0xFFF2F2F3;
    public static final int MUTED = 0xFF8A8A90;
    public static final int DIM = 0xFF56565C;
    public static final float MARGIN = 6;

    private Hud() {
    }

    public static float scale() {
        return mc.getWindow().getFramebufferHeight() / 540f * ModuleManager.get(ClickGui.class).hudScale.f();
    }

    public static boolean editing() {
        return mc.currentScreen instanceof HudEditorScreen;
    }

    /** Wspólny wygląd panelu: cień, półprzezroczyste tło i cienkie obramowanie. */
    public static void panel(Canvas cv, float x, float y, float w, float h, float r) {
        cv.shadow(x, y + 2, w, h, r, 9, 0x70000000);
        cv.round(x, y, w, h, r, PANEL);
        cv.outline(x, y, w, h, r, 1f, BORDER);
    }

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        if (mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud() || mc.player == null) return;
        Canvas cv = Canvas.begin(ctx, scale());
        for (HudModule m : ModuleManager.hud()) {
            if (!m.isEnabled()) continue;
            float[] s = m.size(cv);
            if (s == null) continue;
            float x = MARGIN + m.fx * Math.max(0, cv.width() - s[0] - MARGIN * 2);
            float y = MARGIN + m.fy * Math.max(0, cv.height() - s[1] - MARGIN * 2);
            m.lastX = x;
            m.lastY = y;
            m.lastW = s[0];
            m.lastH = s[1];
            m.draw(cv, x, y, s[0], s[1], m.fx > 0.5f);
        }
        Notifications.render(cv);
        cv.end();
    }
}
