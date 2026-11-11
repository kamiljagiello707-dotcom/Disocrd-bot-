package tech.fuzir.client.gui.component;

import net.minecraft.client.gui.DrawContext;

/**
 * Base GUI element. The parent assigns bounds every frame through {@link #setBounds} before rendering,
 * input handlers then hit-test against the last rendered bounds.
 */
public abstract class Component {
    protected float x;
    protected float y;
    protected float width;

    public void setBounds(float x, float y, float width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    /** Current (possibly animated) height. */
    public abstract float getHeight();

    public abstract void render(DrawContext context, double mouseX, double mouseY, float delta);

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean isVisible() {
        return true;
    }

    protected boolean isHovered(double mouseX, double mouseY) {
        return isHovered(mouseX, mouseY, x, y, width, getHeight());
    }

    public static boolean isHovered(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
