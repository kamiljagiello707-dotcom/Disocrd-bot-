package dev.nova.client.gui.component;

import dev.nova.client.gui.ClickGuiScreen;
import dev.nova.client.setting.Setting;
import dev.nova.client.util.ColorUtil;
import dev.nova.client.util.Render2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** A single setting row inside an expanded module. Bounds are recorded on every render for hit-testing. */
public abstract class Component {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();
    protected double x, y, w;
    protected boolean rendered;

    public abstract Setting<?> setting();
    public abstract double height();
    protected abstract void draw(DrawContext ctx, int mx, int my);

    public final void render(DrawContext ctx, double x, double y, double w, int mx, int my) {
        this.x = x; this.y = y; this.w = w;
        this.rendered = true;
        draw(ctx, mx, my);
    }

    public void beginFrame() { rendered = false; }

    public boolean isHovered(double mx, double my) {
        return rendered && Render2D.hovered(mx, my, x, y, w, height());
    }

    public boolean mouseClicked(double mx, double my, int button) { return false; }
    public void mouseReleased(double mx, double my, int button) {}
    public void mouseDragged(double mx, double my) {}
    public boolean keyPressed(int key) { return false; }

    protected static TextRenderer font() { return mc.textRenderer; }

    /** Apply the global GUI fade to a color. */
    protected static int c(int color) { return ColorUtil.alpha(color, ClickGuiScreen.fade); }

    protected void text(DrawContext ctx, String s, double tx, double ty, int color) {
        ctx.drawText(font(), s, (int) tx, (int) ty, c(color), false);
    }
}
