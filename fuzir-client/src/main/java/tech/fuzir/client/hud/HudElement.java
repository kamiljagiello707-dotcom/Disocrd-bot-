package tech.fuzir.client.hud;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import tech.fuzir.client.gui.Theme;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.render.RenderUtil;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.util.ColorUtil;

/**
 * Draggable on-screen element. Positions are stored relative to an anchor (start / center / end on each
 * axis) chosen from where the element sits, so elements stay glued to their edge when the window resizes
 * and right/bottom anchored elements grow away from the edge they are attached to.
 */
public abstract class HudElement extends Module {
    public enum Anchor { START, CENTER, END }

    protected final BoolSetting background = bool("Background", true);

    private Anchor anchorX;
    private Anchor anchorY;
    private float offsetX;
    private float offsetY;
    protected float width = 40f;
    protected float height = 12f;

    protected HudElement(String name, String description, Anchor anchorX, Anchor anchorY, float offsetX, float offsetY) {
        super(name, description, Category.HUD);
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        setDrawn(false);
        setEnabledByDefault();
    }

    /** Recomputes {@link #width} / {@link #height}. Called every frame before positioning and rendering. */
    public abstract void measure(boolean editor);

    public abstract void render(DrawContext context, float x, float y, float tickDelta, boolean editor);

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public Anchor getAnchorX() {
        return anchorX;
    }

    public Anchor getAnchorY() {
        return anchorY;
    }

    public float getX(int screenWidth) {
        float x = switch (anchorX) {
            case START -> offsetX;
            case CENTER -> screenWidth / 2f + offsetX - width / 2f;
            case END -> screenWidth - offsetX - width;
        };
        return clamp(x, screenWidth - width);
    }

    public float getY(int screenHeight) {
        float y = switch (anchorY) {
            case START -> offsetY;
            case CENTER -> screenHeight / 2f + offsetY - height / 2f;
            case END -> screenHeight - offsetY - height;
        };
        return clamp(y, screenHeight - height);
    }

    /** Stores an absolute top-left position, re-deriving the anchors from the element's center. */
    public void setPosition(float x, float y, int screenWidth, int screenHeight) {
        x = clamp(x, screenWidth - width);
        y = clamp(y, screenHeight - height);
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        anchorX = anchorFor(centerX, screenWidth);
        anchorY = anchorFor(centerY, screenHeight);
        offsetX = switch (anchorX) {
            case START -> x;
            case CENTER -> centerX - screenWidth / 2f;
            case END -> screenWidth - x - width;
        };
        offsetY = switch (anchorY) {
            case START -> y;
            case CENTER -> centerY - screenHeight / 2f;
            case END -> screenHeight - y - height;
        };
    }

    private static Anchor anchorFor(float center, int size) {
        if (center < size / 3f) return Anchor.START;
        if (center > size * 2f / 3f) return Anchor.END;
        return Anchor.CENTER;
    }

    private static float clamp(float value, float max) {
        return Math.max(0f, Math.min(Math.max(0f, max), value));
    }

    /** Shared glass card used by all elements. */
    protected void drawPanel(DrawContext context, float x, float y, float w, float h) {
        if (!background.isEnabled()) return;
        MatrixStack matrices = context.getMatrices();
        RenderUtil.shadow(matrices, x, y, w, h, 4f, 6f, 0x55000000);
        RenderUtil.roundedRect(matrices, x, y, w, h, 4f, Theme.HUD_BG);
        RenderUtil.roundedOutline(matrices, x, y, w, h, 4f, 0.5f, Theme.BORDER);
    }

    /** Thin accent line that fades out towards both ends. */
    protected void drawAccentLine(DrawContext context, float x, float y, float w) {
        MatrixStack matrices = context.getMatrices();
        int accent = Theme.accent();
        RenderUtil.horizontalGradient(matrices, x, y, w / 2f, 0.8f, ColorUtil.withAlpha(accent, 0), accent);
        RenderUtil.horizontalGradient(matrices, x + w / 2f, y, w / 2f, 0.8f, accent, ColorUtil.withAlpha(Theme.accentSecondary(), 0));
    }

    @Override
    public JsonObject save() {
        JsonObject object = super.save();
        JsonObject position = new JsonObject();
        position.addProperty("anchorX", anchorX.name());
        position.addProperty("anchorY", anchorY.name());
        position.addProperty("x", offsetX);
        position.addProperty("y", offsetY);
        object.add("position", position);
        return object;
    }

    @Override
    public void load(JsonObject object) {
        super.load(object);
        if (!object.has("position") || !object.get("position").isJsonObject()) return;
        JsonObject position = object.getAsJsonObject("position");
        try {
            if (position.has("anchorX")) anchorX = Anchor.valueOf(position.get("anchorX").getAsString());
            if (position.has("anchorY")) anchorY = Anchor.valueOf(position.get("anchorY").getAsString());
        } catch (IllegalArgumentException ignored) {
            // Unknown anchor from a newer/older config: keep defaults.
        }
        if (position.has("x")) offsetX = position.get("x").getAsFloat();
        if (position.has("y")) offsetY = position.get("y").getAsFloat();
    }
}
