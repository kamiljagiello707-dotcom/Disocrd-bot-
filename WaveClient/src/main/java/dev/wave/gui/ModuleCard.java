package dev.wave.gui;

import dev.wave.module.Module;
import dev.wave.util.*;
import net.minecraft.client.gui.DrawContext;

/** A module tile in the main grid: icon, name, description, switch. */
public class ModuleCard {
    public static final float HEIGHT = 46;

    final Module module;
    private final Animation toggle;
    private final Animation hover = new Animation(16, 0);
    private final Animation gearHover = new Animation(18, 0);
    final Animation appear = new Animation(9, 0);
    private long appearAt;
    float x, y, w;

    public ModuleCard(Module module) {
        this.module = module;
        this.toggle = new Animation(14, module.isEnabled() ? 1 : 0);
    }

    public void resetAppear(int index) {
        appear.set(0);
        appearAt = System.currentTimeMillis() + index * 28L;
    }

    private float[] gearBox() { return new float[]{x + w - 22, y + HEIGHT - 17, 14, 12}; }

    public void render(DrawContext c, int mx, int my, boolean interactive) {
        if (System.currentTimeMillis() >= appearAt) appear.setTarget(1);
        float ap = Easing.outCubic(appear.update());
        float base = RenderUtil.globalAlpha;
        RenderUtil.globalAlpha = base * ap;
        float yy = y + (1 - ap) * 8;

        boolean hov = interactive && RenderUtil.hovered(mx, my, x, y, w, HEIGHT);
        hover.setTarget(hov ? 1 : 0);
        float hv = hover.update();
        toggle.setTarget(module.isEnabled() ? 1 : 0);
        float t = Easing.outCubic(toggle.update());
        float[] g = gearBox();
        gearHover.setTarget(interactive && RenderUtil.hovered(mx, my, g[0] - 2, g[1] - 2, g[2] + 4, g[3] + 4) ? 1 : 0);
        float gh = gearHover.update();

        // card
        RenderUtil.round(c, x, yy, w, HEIGHT, 7, ColorUtil.lerp(0xFF121212, 0xFF181818, hv));
        RenderUtil.roundOutline(c, x, yy, w, HEIGHT, 7, 0.5f, ColorUtil.lerp(ColorUtil.lerp(0x16FFFFFF, 0x30FFFFFF, hv), ColorUtil.alpha(Theme.accent(), 0.55f), t));
        // enabled glow strip on the left
        if (t > 0.01f) RenderUtil.round(c, x + 0.5f, yy + 10, 2, (HEIGHT - 20) * t, 1, Theme.accent());

        // icon tile
        float is = 28, ix = x + 9, iy = yy + HEIGHT / 2 - is / 2;
        RenderUtil.round(c, ix, iy, is, is, 7, ColorUtil.lerp(0xFF1F1F1F, Theme.accent(), t));
        Icons.draw(c, module.getIcon(), ix + 6, iy + 6, 16, ColorUtil.lerp(ColorUtil.lerp(0xFF8C8C8C, 0xFFFFFFFF, hv), Theme.onAccent(), t));

        // texts
        float tx = ix + is + 9;
        RenderUtil.text(c, "§l" + module.getName(), tx, yy + 10, ColorUtil.lerp(0xFFD0D0D0, Theme.TEXT, Math.max(t, hv)));
        float maxDesc = w - (tx - x) - 34;
        RenderUtil.textScaled(c, trim(module.getDescription(), maxDesc / 0.75f), tx, yy + 23, 0.75f, Theme.TEXT_DIM, false);
        String key = KeyUtil.name(module.getKey());
        if (module.getKey() != KeyUtil.NONE) {
            float kw = RenderUtil.width(key) * 0.7f + 6;
            RenderUtil.round(c, tx, yy + 32, kw, 8, 2.5f, 0xFF242424);
            RenderUtil.textScaled(c, key, tx + 3, yy + 33.6f, 0.7f, Theme.TEXT_DIM, false);
        }

        // switch
        float sw = 20, sh = 10, sx = x + w - sw - 9, sy = yy + 9;
        RenderUtil.round(c, sx, sy, sw, sh, 5, ColorUtil.lerp(0xFF2A2A2A, Theme.accent(), t));
        RenderUtil.circle(c, sx + 5 + t * 10, sy + 5, 3.4f, ColorUtil.lerp(0xFF8A8A8A, Theme.onAccent(), t));

        // settings arrow
        Icons.draw(c, "arrow", g[0] + 3 + gh * 1.5f, g[1] + 1, 10, ColorUtil.lerp(0xFF5A5A5A, Theme.TEXT, gh));
        RenderUtil.globalAlpha = base;
    }

    static String trim(String s, float max) {
        if (RenderUtil.width(s) <= max) return s;
        while (s.length() > 1 && RenderUtil.width(s + "...") > max) s = s.substring(0, s.length() - 1);
        return s.trim() + "...";
    }

    /** @return 1 toggled, 2 open settings, 0 nothing */
    public int click(double mx, double my, int button) {
        if (appear.get() < 0.6f || !RenderUtil.hovered(mx, my, x, y, w, HEIGHT)) return 0;
        float[] g = gearBox();
        if (button == 1 || (button == 0 && RenderUtil.hovered(mx, my, g[0] - 2, g[1] - 2, g[2] + 4, g[3] + 4))) return 2;
        if (button == 0) {
            module.toggle();
            return 1;
        }
        return 0;
    }
}
