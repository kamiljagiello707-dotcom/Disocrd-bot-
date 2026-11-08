package dev.nova.client.gui.component;

import dev.nova.client.setting.ModeSetting;
import dev.nova.client.setting.Setting;
import dev.nova.client.util.Render2D;
import dev.nova.client.util.Theme;
import net.minecraft.client.gui.DrawContext;

/** Left click = next mode, right click = previous mode. */
public class ModeComponent extends Component {
    private final ModeSetting setting;

    public ModeComponent(ModeSetting s) { this.setting = s; }

    @Override public Setting<?> setting() { return setting; }
    @Override public double height() { return 14; }

    @Override
    protected void draw(DrawContext ctx, int mx, int my) {
        text(ctx, setting.getName(), x + 6, y + 3, Theme.TEXT);
        String v = setting.get();
        int vw = font().getWidth(v);
        double px = x + w - vw - 12;
        Render2D.roundRect(ctx, px, y + 1.5, vw + 8, 11, 3, c(isHovered(mx, my) ? 0xFF30303E : 0xFF262632));
        text(ctx, v, px + 4, y + 3, Theme.accent(px));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!isHovered(mx, my)) return false;
        if (button == 0) setting.cycle(1);
        else if (button == 1) setting.cycle(-1);
        else return false;
        return true;
    }
}
