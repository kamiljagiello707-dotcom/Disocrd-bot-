package dev.nova.client.gui.component;

import dev.nova.client.setting.BooleanSetting;
import dev.nova.client.setting.Setting;
import dev.nova.client.util.Animation;
import dev.nova.client.util.ColorUtil;
import dev.nova.client.util.Render2D;
import dev.nova.client.util.Theme;
import net.minecraft.client.gui.DrawContext;

public class BooleanComponent extends Component {
    private final BooleanSetting setting;
    private final Animation anim;

    public BooleanComponent(BooleanSetting s) {
        this.setting = s;
        this.anim = new Animation(14, s.get() ? 1 : 0);
    }

    @Override public Setting<?> setting() { return setting; }
    @Override public double height() { return 14; }

    @Override
    protected void draw(DrawContext ctx, int mx, int my) {
        double t = anim.setTarget(setting.get() ? 1 : 0).get();
        text(ctx, setting.getName(), x + 6, y + 3, ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, t));

        double sw = 16, sh = 8, sx = x + w - sw - 6, sy = y + 3;
        Render2D.roundRect(ctx, sx, sy, sw, sh, 4, c(ColorUtil.lerp(0xFF2E2E3A, Theme.accent(sx), t)));
        double knob = sx + 1 + (sw - 8) * t;
        Render2D.roundRect(ctx, knob, sy + 1, 6, 6, 3, c(0xFFFFFFFF));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (isHovered(mx, my) && button == 0) { setting.toggle(); return true; }
        return false;
    }
}
