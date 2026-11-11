package tech.fuzir.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import tech.fuzir.client.gui.Theme;
import tech.fuzir.client.util.ColorUtil;

/**
 * Text helpers using the bundled Inter font (falls back to the vanilla font per glyph and globally when the
 * "Custom Font" option is off). Supports sub-pixel positions and arbitrary scale through the matrix stack.
 */
public final class Fonts {
    public static final Identifier MEDIUM = new Identifier("fuzir", "medium");
    public static final Identifier SEMIBOLD = new Identifier("fuzir", "semibold");
    public static final float HEIGHT = 8f;

    private Fonts() {
    }

    private static TextRenderer renderer() {
        return MinecraftClient.getInstance().textRenderer;
    }

    public static Text text(String content, Identifier font) {
        Identifier resolved = Theme.customFont() ? font : Style.DEFAULT_FONT_ID;
        return Text.literal(content).setStyle(Style.EMPTY.withFont(resolved));
    }

    public static float width(String content) {
        return width(content, MEDIUM);
    }

    public static float width(String content, Identifier font) {
        return renderer().getWidth(text(content, font));
    }

    public static void draw(DrawContext context, String content, float x, float y, int color) {
        draw(context, content, x, y, color, MEDIUM, 1f, false);
    }

    public static void drawBold(DrawContext context, String content, float x, float y, int color) {
        draw(context, content, x, y, color, SEMIBOLD, 1f, false);
    }

    public static void draw(DrawContext context, String content, float x, float y, int color, Identifier font,
                            float scale, boolean shadow) {
        // TextRenderer treats alpha < 4 as opaque; skip fully faded text instead of flashing it.
        if (ColorUtil.alpha(color) < 5 || content.isEmpty()) return;
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0f);
        if (scale != 1f) matrices.scale(scale, scale, 1f);
        context.drawText(renderer(), text(content, font), 0, 0, color, shadow);
        matrices.pop();
    }

    public static void drawCentered(DrawContext context, String content, float centerX, float y, int color, Identifier font) {
        draw(context, content, centerX - width(content, font) / 2f, y, color, font, 1f, false);
    }
}
