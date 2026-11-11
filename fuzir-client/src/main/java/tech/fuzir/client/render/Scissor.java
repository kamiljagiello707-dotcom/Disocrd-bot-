package tech.fuzir.client.render;

import net.minecraft.client.gui.DrawContext;

/**
 * Clipping helper that understands the uniform scale applied by the ClickGUI around its pivot point.
 * {@link DrawContext#enableScissor} works in unscaled GUI pixels, so virtual coordinates are mapped back first.
 * Nested calls intersect through DrawContext's scissor stack.
 */
public final class Scissor {
    private static float scale = 1f;
    private static float pivotX;
    private static float pivotY;

    private Scissor() {
    }

    public static void setTransform(float newScale, float newPivotX, float newPivotY) {
        scale = newScale;
        pivotX = newPivotX;
        pivotY = newPivotY;
    }

    public static void resetTransform() {
        setTransform(1f, 0f, 0f);
    }

    public static void push(DrawContext context, float x, float y, float width, float height) {
        int x1 = (int) Math.floor(toScreenX(x));
        int y1 = (int) Math.floor(toScreenY(y));
        int x2 = (int) Math.ceil(toScreenX(x + Math.max(0f, width)));
        int y2 = (int) Math.ceil(toScreenY(y + Math.max(0f, height)));
        context.enableScissor(x1, y1, Math.max(x1, x2), Math.max(y1, y2));
    }

    public static void pop(DrawContext context) {
        context.disableScissor();
    }

    private static float toScreenX(float x) {
        return (x - pivotX) * scale + pivotX;
    }

    private static float toScreenY(float y) {
        return (y - pivotY) * scale + pivotY;
    }
}
