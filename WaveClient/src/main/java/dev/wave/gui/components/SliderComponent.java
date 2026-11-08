package dev.wave.gui.components;

import dev.wave.gui.ClickGuiScreen;
import dev.wave.setting.NumberSetting;
import dev.wave.util.Animation;
import dev.wave.util.ColorUtil;
import dev.wave.util.RenderUtil;
import dev.wave.util.Theme;
import net.minecraft.client.gui.DrawContext;

public class SliderComponent extends Component {
    private final NumberSetting s;
    private final Animation fill;
    private final Animation drag = new Animation(18, 0);
    private boolean dragging;

    public SliderComponent(NumberSetting s) {
        super(s);
        this.s = s;
        this.fill = new Animation(16, pct());
    }

    private float pct() { return (float) ((s.get() - s.getMin()) / (s.getMax() - s.getMin())); }

    @Override public float baseHeight() { return 26; }

    @Override
    protected void render(DrawContext c, int mx, int my) {
        float bx = x + 2, bw = w - 4, by = y + 18;
        if (dragging) {
            float p = Math.max(0, Math.min(1, (mx - bx) / bw));
            s.set(s.getMin() + p * (s.getMax() - s.getMin()));
        }
        fill.setTarget(pct());
        float f = fill.update();
        drag.setTarget(dragging ? 1 : 0);
        float d = drag.update();

        RenderUtil.text(c, s.getName(), x + 2, y + 4, ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, Math.max(d, hover.get() * 0.6f)));
        String val = s.display();
        RenderUtil.text(c, val, x + w - 2 - RenderUtil.width(val), y + 4, Theme.TEXT);

        float bh = 2.5f + d;
        RenderUtil.round(c, bx, by - d / 2, bw, bh, bh / 2, 0xFF2A2A2A);
        RenderUtil.round(c, bx, by - d / 2, Math.max(bh, bw * f), bh, bh / 2, Theme.accent());
        RenderUtil.circle(c, bx + bw * f, by + 1.25f, 2.6f + hover.get() * 0.6f + d * 0.6f, Theme.accent());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!isHovered(mx, my)) return false;
        if (button == 0) {
            dragging = true;
            return true;
        }
        if (button == 1) {
            s.reset();
            ClickGuiScreen.click(0.8f);
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mx, double my, int button) {
        if (dragging) ClickGuiScreen.click(1.4f);
        dragging = false;
    }

    @Override
    public boolean keyPressed(int key) { return false; }

    public boolean scroll(double mx, double my, double amount) {
        if (!isHovered(mx, my)) return false;
        s.set(s.get() + Math.signum(amount) * s.getStep());
        return true;
    }
}
