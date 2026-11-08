package dev.wave.hud;

import dev.wave.module.Category;
import dev.wave.module.Module;
import dev.wave.setting.BooleanSetting;
import net.minecraft.client.gui.DrawContext;

public abstract class HudElement extends Module {
    protected float x, y;
    protected float width = 40, height = 12;
    private boolean placed;

    public final BooleanSetting background = new BooleanSetting("Background", "Draw a dark background", true);

    protected HudElement(String name, String description) {
        super(name, description, Category.HUD);
        drawn.set(false);
    }

    /** Render the element at (x, y). {@code editor} is true inside the HUD editor. */
    public abstract void render(DrawContext c, float delta, boolean editor);

    /** Called once when the element has no saved position. */
    protected abstract void defaultPosition(int sw, int sh);

    public void prepare(int sw, int sh) {
        if (!placed) {
            defaultPosition(sw, sh);
            placed = true;
        }
        clamp(sw, sh);
    }

    public void clamp(int sw, int sh) {
        x = Math.max(0, Math.min(x, sw - width));
        y = Math.max(0, Math.min(y, sh - height));
    }

    /** True when the element sits on the right half of the screen (text aligns right). */
    protected boolean rightAligned() { return x + width / 2f > mc.getWindow().getScaledWidth() / 2f; }

    /** Resize while keeping the right edge fixed for right-aligned elements. */
    protected void resize(float w, float h) {
        if (placed && rightAligned()) x += width - w;
        width = w;
        height = h;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public boolean isPlaced() { return placed; }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        this.placed = true;
    }
}
