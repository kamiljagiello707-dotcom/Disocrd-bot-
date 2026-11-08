package dev.wave.gui.components;

import dev.wave.setting.Setting;
import dev.wave.util.Animation;
import dev.wave.util.RenderUtil;
import net.minecraft.client.gui.DrawContext;

public abstract class Component {
    protected final Setting<?> setting;
    protected float x, y, w;
    protected final Animation visibility = new Animation(16, 1);
    protected final Animation hover = new Animation(18, 0);

    protected Component(Setting<?> setting) {
        this.setting = setting;
        if (setting != null && !setting.isVisible()) visibility.set(0);
    }

    public Setting<?> getSetting() { return setting; }

    /** Height when fully visible. */
    public abstract float baseHeight();

    public float height() {
        visibility.setTarget(isVisible() ? 1 : 0);
        return baseHeight() * visibility.update();
    }

    public boolean isVisible() { return setting == null || setting.isVisible(); }

    public void setBounds(float x, float y, float w) {
        this.x = x;
        this.y = y;
        this.w = w;
    }

    public final void draw(DrawContext c, int mx, int my) {
        if (visibility.get() < 0.02f) return;
        hover.setTarget(isHovered(mx, my) ? 1 : 0);
        hover.update();
        float prev = RenderUtil.globalAlpha;
        RenderUtil.globalAlpha = prev * visibility.get();
        render(c, mx, my);
        RenderUtil.globalAlpha = prev;
    }

    protected abstract void render(DrawContext c, int mx, int my);

    public boolean isHovered(double mx, double my) { return RenderUtil.hovered(mx, my, x, y, w, baseHeight()); }

    public boolean active() { return isVisible() && visibility.get() > 0.9f; }

    public boolean mouseClicked(double mx, double my, int button) { return false; }
    public void mouseReleased(double mx, double my, int button) {}
    public boolean keyPressed(int key) { return false; }

    public String tooltip() { return setting == null ? null : setting.getDescription(); }
}
