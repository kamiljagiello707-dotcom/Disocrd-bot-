package dev.nova.client.gui.component;

import dev.nova.client.setting.ColorSetting;
import dev.nova.client.setting.Setting;
import dev.nova.client.util.ColorUtil;
import dev.nova.client.util.Render2D;
import dev.nova.client.util.Theme;
import net.minecraft.client.gui.DrawContext;

/** Hue slider rendered as a rainbow strip. */
public class ColorComponent extends Component {
    private final ColorSetting setting;
    private boolean dragging;

    public ColorComponent(ColorSetting s) { this.setting = s; }

    @Override public Setting<?> setting() { return setting; }
    @Override public double height() { return 22; }

    @Override
    protected void draw(DrawContext ctx, int mx, int my) {
        if (dragging) mouseDragged(mx, my);
        text(ctx, setting.getName(), x + 6, y + 2, Theme.TEXT);
        Render2D.roundRect(ctx, x + w - 16, y + 2, 10, 8, 2, c(setting.rgb()));

        double bx = x + 6, by = y + 14, bw = w - 12;
        for (int i = 0; i < bw; i++) {
            ctx.fill((int) (bx + i), (int) by, (int) (bx + i + 1), (int) by + 4, c(ColorUtil.hsb((float) (i / bw), 0.75f, 1f)));
        }
        double kx = bx + bw * setting.get();
        Render2D.rect(ctx, kx - 1, by - 1, 2, 6, c(0xFFFFFFFF));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (isHovered(mx, my) && button == 0) { dragging = true; mouseDragged(mx, my); return true; }
        return false;
    }

    @Override
    public void mouseDragged(double mx, double my) {
        if (dragging) setting.set((float) Math.max(0, Math.min(0.999, (mx - (x + 6)) / (w - 12))));
    }

    @Override public void mouseReleased(double mx, double my, int button) { dragging = false; }
}
