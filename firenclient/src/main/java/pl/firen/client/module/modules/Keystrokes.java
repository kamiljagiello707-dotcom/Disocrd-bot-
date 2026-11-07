package pl.firen.client.module.modules;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import pl.firen.client.gui.Theme;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.render.Fonts;
import pl.firen.client.render.Render2D;
import pl.firen.client.setting.NumberSetting;
import pl.firen.client.util.Anim;

/** Pokazuje wciskane klawisze WASD, spację i przyciski myszy. */
public class Keystrokes extends Module {
    public final NumberSetting x = add(new NumberSetting("X", 14, 0, 400, 1));
    public final NumberSetting y = add(new NumberSetting("Y", 140, 0, 400, 1));

    private final float[] anim = new float[7];

    public Keystrokes() { super("Keystrokes", "Podgląd wciskanych klawiszy (WASD + mysz).", Category.RENDER); }

    private boolean down(int key) {
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }
    private boolean mouse(int btn) {
        return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), btn) == GLFW.GLFW_PRESS;
    }

    @Override
    public void onRenderHud(DrawContext ctx, float s) {
        if (!isEnabled() || mc.options.hudHidden) return;
        boolean[] st = {
                mc.options.forwardKey.isPressed(), mc.options.leftKey.isPressed(),
                mc.options.backKey.isPressed(), mc.options.rightKey.isPressed(),
                mouse(GLFW.GLFW_MOUSE_BUTTON_LEFT), mouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT),
                mc.options.jumpKey.isPressed()
        };
        float dt = 1f / 60f;
        float ox = x.get().floatValue(), oy = y.get().floatValue();
        float u = 24, g = 3;
        // W
        key(ctx, 0, st[0], ox + u + g, oy, u, u, "W", dt);
        // A S D
        key(ctx, 1, st[1], ox, oy + u + g, u, u, "A", dt);
        key(ctx, 2, st[2], ox + u + g, oy + u + g, u, u, "S", dt);
        key(ctx, 3, st[3], ox + (u + g) * 2, oy + u + g, u, u, "D", dt);
        // LMB RMB
        float rowY = oy + (u + g) * 2;
        key(ctx, 4, st[4], ox, rowY, u * 1.5f + g * 0.5f, u, "LPM", dt);
        key(ctx, 5, st[5], ox + u * 1.5f + g * 1.5f, rowY, u * 1.5f + g * 0.5f, u, "PPM", dt);
        // Space
        key(ctx, 6, st[6], ox, rowY + u + g, (u + g) * 3 - g, u * 0.8f, "____", dt);
    }

    private void key(DrawContext ctx, int i, boolean down, float x, float y, float w, float h, String label, float dt) {
        anim[i] = Anim.approach(anim[i], down ? 1f : 0f, dt, 20f);
        int bg = Anim.lerpColor(0xB0121216, Theme.accent(), anim[i]);
        int fg = Anim.lerpColor(Theme.TEXT, Theme.onAccent(), anim[i]);
        Render2D.round(ctx, x, y, w, h, 5, bg);
        Render2D.outline(ctx, x, y, w, h, 5, 0.5f, Theme.accentAlpha((int) (0x30 + 0x60 * anim[i])));
        Fonts.drawCentered(ctx, label, Fonts.SMALL, x + w / 2f, y + h / 2f - 3, fg);
    }
}
