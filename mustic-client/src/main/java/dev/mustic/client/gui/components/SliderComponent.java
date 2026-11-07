package dev.mustic.client.gui.components;

import dev.mustic.client.setting.NumberSetting;
import dev.mustic.client.util.Anim;
import dev.mustic.client.util.Render2D;
import dev.mustic.client.util.Theme;
import net.minecraft.client.gui.DrawContext;

public class SliderComponent extends Component {
    private final NumberSetting setting;
    private final Anim anim;
    private boolean dragging;

    public SliderComponent(NumberSetting setting) {
        this.setting = setting;
        this.anim = new Anim(16f, fraction());
    }

    private float fraction() {
        return (float) ((setting.get() - setting.getMin()) / (setting.getMax() - setting.getMin()));
    }

    @Override public int height() { return 19; }
    @Override public boolean visible() { return setting.isVisible(); }

    @Override
    protected void draw(DrawContext ctx, int mx, int my, boolean hovered) {
        int barX = x + 8, barW = w - 14, barY = y + 13;
        if (dragging) {
            double f = Math.max(0, Math.min(1, (mx - barX) / (double) barW));
            setting.set(setting.getMin() + f * (setting.getMax() - setting.getMin()));
        }
        float f = anim.update(fraction());

        ctx.drawText(font(), setting.getName(), x + 8, y + 3, hovered || dragging ? Theme.TEXT : Theme.TEXT_DIM, false);
        String val = setting.format();
        ctx.drawText(font(), val, x + w - 6 - font().getWidth(val), y + 3, Theme.TEXT, false);

        Render2D.rect(ctx, barX, barY, barW, 2, 0xFF2A2A38);
        int fill = (int) (barW * f);
        Render2D.horizontalGradient(ctx, barX, barY, Math.max(1, fill), 2, Theme.accent(y * 8L), Theme.accent(y * 8L + 500));
        Render2D.softRect(ctx, barX + fill - 2, barY - 1, 4, 4, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (hovered(mx, my) && button == 0) {
            dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mx, double my, int button) {
        dragging = false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!hovered(mx, my)) return false;
        setting.set(setting.get() + Math.signum(amount) * setting.getStep());
        return true;
    }
}
