package dev.mustic.client.gui.components;

import dev.mustic.client.setting.BooleanSetting;
import dev.mustic.client.util.Anim;
import dev.mustic.client.util.ColorUtil;
import dev.mustic.client.util.Render2D;
import dev.mustic.client.util.Theme;
import net.minecraft.client.gui.DrawContext;

public class BooleanComponent extends Component {
    private final BooleanSetting setting;
    private final Anim anim;

    public BooleanComponent(BooleanSetting setting) {
        this.setting = setting;
        this.anim = new Anim(14f, setting.get() ? 1f : 0f);
    }

    @Override public int height() { return 13; }
    @Override public boolean visible() { return setting.isVisible(); }

    @Override
    protected void draw(DrawContext ctx, int mx, int my, boolean hovered) {
        float t = anim.update(setting.get() ? 1f : 0f);
        ctx.drawText(font(), setting.getName(), x + 8, y + 3, hovered ? Theme.TEXT : Theme.TEXT_DIM, false);

        int tw = 15, th = 7;
        int tx = x + w - tw - 6, ty = y + 3;
        int track = ColorUtil.lerp(0xFF2A2A38, Theme.accent(y * 8L), t);
        Render2D.softRect(ctx, tx, ty, tw, th, track);
        int kx = (int) (tx + 1 + (tw - 7) * t);
        Render2D.softRect(ctx, kx, ty + 1, 5, 5, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (hovered(mx, my) && button == 0) {
            setting.toggle();
            return true;
        }
        return false;
    }
}
