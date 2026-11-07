package dev.mustic.client.gui.components;

import dev.mustic.client.setting.ModeSetting;
import dev.mustic.client.util.Theme;
import net.minecraft.client.gui.DrawContext;

public class ModeComponent extends Component {
    private final ModeSetting setting;

    public ModeComponent(ModeSetting setting) {
        this.setting = setting;
    }

    @Override public int height() { return 13; }
    @Override public boolean visible() { return setting.isVisible(); }

    @Override
    protected void draw(DrawContext ctx, int mx, int my, boolean hovered) {
        ctx.drawText(font(), setting.getName(), x + 8, y + 3, hovered ? Theme.TEXT : Theme.TEXT_DIM, false);
        String val = setting.get();
        int vx = x + w - 6 - font().getWidth(val);
        ctx.drawText(font(), val, vx, y + 3, Theme.accent(y * 8L), false);
        if (hovered) {
            ctx.drawText(font(), "‹", vx - 7, y + 3, Theme.TEXT_DIM, false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!hovered(mx, my)) return false;
        if (button == 0) setting.cycle(true);
        else if (button == 1) setting.cycle(false);
        else return false;
        return true;
    }
}
