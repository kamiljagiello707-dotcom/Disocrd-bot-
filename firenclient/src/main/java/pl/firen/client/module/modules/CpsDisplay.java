package pl.firen.client.module.modules;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import pl.firen.client.gui.Theme;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.render.Fonts;
import pl.firen.client.render.Render2D;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.NumberSetting;

import java.util.ArrayDeque;
import java.util.Deque;

/** Licznik kliknięć na sekundę (lewy i prawy przycisk). */
public class CpsDisplay extends Module {
    public final NumberSetting x = add(new NumberSetting("X", 14, 0, 400, 1));
    public final NumberSetting y = add(new NumberSetting("Y", 230, 0, 400, 1));
    public final BooleanSetting right = add(new BooleanSetting("Right click too", false));

    private final Deque<Long> left = new ArrayDeque<>(), rightClicks = new ArrayDeque<>();
    private boolean wasL, wasR;

    public CpsDisplay() { super("CPS", "Licznik kliknięć na sekundę.", Category.RENDER); }

    private boolean mouse(int b) { return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), b) == GLFW.GLFW_PRESS; }

    private int cps(Deque<Long> q) {
        long now = System.currentTimeMillis();
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.removeFirst();
        return q.size();
    }

    @Override
    public void onTick() {
        boolean l = mouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        if (l && !wasL) left.addLast(System.currentTimeMillis());
        wasL = l;
        boolean r = mouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        if (r && !wasR) rightClicks.addLast(System.currentTimeMillis());
        wasR = r;
    }

    @Override
    public void onRenderHud(DrawContext ctx, float s) {
        if (!isEnabled() || mc.options.hudHidden) return;
        String txt = cps(left) + " CPS" + (right.get() ? "  ·  " + cps(rightClicks) + " R" : "");
        float w = Fonts.width(txt, Fonts.SEMIBOLD) + 18, h = 18;
        float ox = x.get().floatValue(), oy = y.get().floatValue();
        Render2D.round(ctx, ox, oy, w, h, 7, 0xCC0C0C0F);
        Render2D.outline(ctx, ox, oy, w, h, 7, 0.5f, Theme.accentAlpha(0x40));
        Render2D.circle(ctx, ox + 9, oy + h / 2f, 2f, Theme.accent());
        Fonts.draw(ctx, txt, Fonts.SEMIBOLD, ox + 16, oy + 5, Theme.TEXT);
    }
}
