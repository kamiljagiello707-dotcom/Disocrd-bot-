package dev.mustic.client.gui.components;

import dev.mustic.client.util.Render2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public abstract class Component {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();
    protected int x, y, w;

    public abstract int height();

    public boolean visible() { return true; }

    public void render(DrawContext ctx, int x, int y, int w, int mx, int my, boolean canHover) {
        this.x = x;
        this.y = y;
        this.w = w;
        draw(ctx, mx, my, canHover && hovered(mx, my));
    }

    protected abstract void draw(DrawContext ctx, int mx, int my, boolean hovered);

    public boolean mouseClicked(double mx, double my, int button) { return false; }
    public void mouseReleased(double mx, double my, int button) {}
    public boolean keyPressed(int key) { return false; }
    public boolean mouseScrolled(double mx, double my, double amount) { return false; }

    protected boolean hovered(double mx, double my) {
        return Render2D.hovered(mx, my, x, y, w, height());
    }

    protected TextRenderer font() { return mc.textRenderer; }
}
