package dev.wave.gui.components;

import dev.wave.gui.ClickGuiScreen;
import dev.wave.setting.BooleanSetting;
import dev.wave.util.Animation;
import dev.wave.util.ColorUtil;
import dev.wave.util.RenderUtil;
import dev.wave.util.Theme;
import net.minecraft.client.gui.DrawContext;

public class BoolComponent extends Component {
    private final BooleanSetting s;
    private final Animation knob;

    public BoolComponent(BooleanSetting s) {
        super(s);
        this.s = s;
        this.knob = new Animation(16, s.isOn() ? 1 : 0);
    }

    @Override public float baseHeight() { return 19; }

    @Override
    protected void render(DrawContext c, int mx, int my) {
        knob.setTarget(s.isOn() ? 1 : 0);
        float k = knob.update();
        RenderUtil.text(c, s.getName(), x + 2, y + 5.5f, ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, Math.max(k, hover.get() * 0.6f)));
        float sw = 16, sh = 8, sx = x + w - sw - 2, sy = y + 5.5f;
        RenderUtil.round(c, sx, sy, sw, sh, 4, ColorUtil.lerp(0xFF2A2A2A, Theme.accent(), k));
        RenderUtil.circle(c, sx + 4 + k * 8, sy + 4, 2.8f + hover.get() * 0.4f, ColorUtil.lerp(0xFF8A8A8A, Theme.onAccent(), k));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!isHovered(mx, my) || button != 0) return false;
        s.toggle();
        ClickGuiScreen.click(s.isOn() ? 1.2f : 0.9f);
        return true;
    }
}
