package dev.wave.gui.components;

import dev.wave.gui.ClickGuiScreen;
import dev.wave.setting.ModeSetting;
import dev.wave.util.Animation;
import dev.wave.util.ColorUtil;
import dev.wave.util.Easing;
import dev.wave.util.RenderUtil;
import dev.wave.util.Theme;
import net.minecraft.client.gui.DrawContext;

public class ModeComponent extends Component {
    private final ModeSetting s;
    private final Animation expand = new Animation(16, 0);
    private boolean open;
    private String previous;
    private long changedAt;
    private int direction = 1;

    public ModeComponent(ModeSetting s) {
        super(s);
        this.s = s;
    }

    @Override
    public float baseHeight() { return 14 + (s.getModes().size() * 11 + 2) * expand.get(); }

    @Override
    protected void render(DrawContext c, int mx, int my) {
        expand.setTarget(open ? 1 : 0);
        float e = expand.update();
        boolean headerHover = RenderUtil.hovered(mx, my, x, y, w, 14);
        RenderUtil.text(c, s.getName(), x + 2, y + 3, ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, headerHover ? 0.6f : 0));

        // value with a slide animation when it changes
        float t = Easing.outCubic((System.currentTimeMillis() - changedAt) / 220f);
        String val = s.get();
        float vw = RenderUtil.width(val);
        float boxW = vw + 8, bx = x + w - boxW - 1;
        RenderUtil.round(c, bx, y + 1.5f, boxW, 11, 3, ColorUtil.lerp(0xFF1E1E1E, 0xFF2A2A2A, headerHover ? 1 : 0));
        RenderUtil.scissor(c, bx, y + 1, boxW, 12);
        float off = (1 - t) * 8 * direction;
        float base = RenderUtil.globalAlpha;
        RenderUtil.globalAlpha = base * t;
        RenderUtil.text(c, val, bx + 4 + off, y + 3, Theme.TEXT);
        if (previous != null && t < 1) {
            RenderUtil.globalAlpha = base * (1 - t);
            RenderUtil.text(c, previous, bx + 4 + off - 8 * direction, y + 3, Theme.TEXT);
        }
        RenderUtil.globalAlpha = base;
        RenderUtil.endScissor(c);

        if (e > 0.01f) {
            float ly = y + 14;
            RenderUtil.scissor(c, x, ly, w, (s.getModes().size() * 11 + 2) * e);
            RenderUtil.round(c, x + 2, ly, w - 4, s.getModes().size() * 11 + 1, 3, 0xFF141414);
            for (String m : s.getModes()) {
                boolean sel = s.is(m);
                boolean hov = RenderUtil.hovered(mx, my, x + 2, ly, w - 4, 11);
                if (sel) RenderUtil.round(c, x + 3, ly + 0.5f, w - 6, 10, 2.5f, ColorUtil.withAlpha(Theme.accent(), 0x30));
                else if (hov) RenderUtil.round(c, x + 3, ly + 0.5f, w - 6, 10, 2.5f, 0x18FFFFFF);
                if (sel) RenderUtil.circle(c, x + 8, ly + 5.5f, 1.6f, Theme.accent());
                RenderUtil.text(c, m, x + 13, ly + 2, sel ? Theme.TEXT : Theme.TEXT_DIM);
                ly += 11;
            }
            RenderUtil.endScissor(c);
        }
    }

    private void change(int dir) {
        previous = s.get();
        direction = dir;
        s.cycle(dir);
        changedAt = System.currentTimeMillis();
        ClickGuiScreen.click(1.1f);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (RenderUtil.hovered(mx, my, x, y, w, 14)) {
            if (button == 0) change(1);
            else if (button == 1) { open = !open; ClickGuiScreen.click(open ? 1.2f : 0.9f); }
            else if (button == 2) change(-1);
            return true;
        }
        if (open && expand.get() > 0.9f) {
            float ly = y + 14;
            for (String m : s.getModes()) {
                if (RenderUtil.hovered(mx, my, x + 2, ly, w - 4, 11)) {
                    if (!s.is(m)) {
                        previous = s.get();
                        direction = s.getModes().indexOf(m) > s.index() ? 1 : -1;
                        s.set(m);
                        changedAt = System.currentTimeMillis();
                        ClickGuiScreen.click(1.1f);
                    }
                    return true;
                }
                ly += 11;
            }
        }
        return false;
    }

    @Override
    public String tooltip() {
        String d = super.tooltip();
        return (d == null || d.isEmpty() ? "" : d + "  ") + "§8[LMB next | RMB list]";
    }
}
