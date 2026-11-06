package pl.firen.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Gładkie czcionki TTF (Inter + ikony Font Awesome) zamiast pikselowej czcionki Minecrafta. */
public final class Fonts {
    private Fonts() {}

    public static final Identifier REGULAR = Identifier.of("firenclient", "regular");
    public static final Identifier SEMIBOLD = Identifier.of("firenclient", "semibold");
    public static final Identifier SMALL = Identifier.of("firenclient", "small");
    public static final Identifier TITLE = Identifier.of("firenclient", "title");
    public static final Identifier ICONS = Identifier.of("firenclient", "icons");

    // Ikony Font Awesome (solid)
    public static final String I_FIRE = "";
    public static final String I_SEARCH = "";
    public static final String I_CHEVRON = "";
    public static final String I_USER = "";
    public static final String I_GEAR = "";
    public static final String I_KEYBOARD = "";
    public static final String I_BOLT = "";
    public static final String I_CROSSHAIR = "";

    private static TextRenderer tr() { return MinecraftClient.getInstance().textRenderer; }

    public static MutableText text(String s, Identifier font) {
        return Text.literal(s).setStyle(Style.EMPTY.withFont(font));
    }

    public static float width(String s, Identifier font) {
        return tr().getTextHandler().getWidth(text(s, font));
    }

    public static void draw(DrawContext ctx, String s, Identifier font, float x, float y, int color) {
        int c = Render2D.a(color);
        if ((c >>> 24) < 5) c = (c & 0xFFFFFF) | 0x05000000;
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0);
        ctx.drawText(tr(), text(s, font), 0, 0, c, false);
        m.pop();
    }

    public static void drawCentered(DrawContext ctx, String s, Identifier font, float cx, float y, int color) {
        draw(ctx, s, font, cx - width(s, font) / 2f, y, color);
    }

    public static void drawRight(DrawContext ctx, String s, Identifier font, float right, float y, int color) {
        draw(ctx, s, font, right - width(s, font), y, color);
    }

    /** Skraca tekst z "…" tak, by zmieścił się w podanej szerokości. */
    public static String trim(String s, Identifier font, float maxWidth) {
        if (width(s, font) <= maxWidth) return s;
        String out = s;
        while (out.length() > 1 && width(out + "…", font) > maxWidth) out = out.substring(0, out.length() - 1);
        return out + "…";
    }
}
