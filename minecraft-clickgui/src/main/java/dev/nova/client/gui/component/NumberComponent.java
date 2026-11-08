package dev.nova.client.gui.component;

import dev.nova.client.setting.NumberSetting;
import dev.nova.client.setting.Setting;
import dev.nova.client.util.Animation;
import dev.nova.client.util.Render2D;
import dev.nova.client.util.Theme;
import net.minecraft.client.gui.DrawContext;

public class NumberComponent extends Component {
    private final NumberSetting setting;
    private final Animation anim;
    private boolean dragging;

    public NumberComponent(NumberSetting s) {
        this.setting = s;
        this.anim = new Animation(16, fraction());
    }

    private double fraction() { return (setting.get() - setting.min) / (setting.max - setting.min); }

    @Override public Setting<?> setting() { return setting; }
    @Override public double height() { return 20; }

    @Override
    protected void draw(DrawContext ctx, int mx, int my) {
        if (dragging) mouseDragged(mx, my);
        double t = anim.setTarget(fraction()).get();

        text(ctx, setting.getName(), x + 6, y + 2, Theme.TEXT);
        String v = setting.display();
        text(ctx, v, x + w - 6 - font().getWidth(v), y + 2, Theme.TEXT_DIM);

        double bx = x + 6, by = y + 14, bw = w - 12;
        Render2D.roundRect(ctx, bx, by, bw, 3, 1, c(0xFF2E2E3A));
        double fill = Math.max(2, bw * t);
        Render2D.roundRect(ctx, bx, by, fill, 3, 1, c(Theme.accent(bx)));
        Render2D.roundRect(ctx, bx + fill - 2.5, by - 1.5, 5, 6, 2, c(0xFFFFFFFF));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (isHovered(mx, my) && button == 0) { dragging = true; mouseDragged(mx, my); return true; }
        return false;
    }

    @Override
    public void mouseDragged(double mx, double my) {
        if (!dragging) return;
        double f = Math.max(0, Math.min(1, (mx - (x + 6)) / (w - 12)));
        setting.set(setting.min + (setting.max - setting.min) * f);
    }

    @Override public void mouseReleased(double mx, double my, int button) { dragging = false; }
}
