package dev.razer.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Gładkie czcionki Inter (TTF/OTF) zamiast pikselowej czcionki Minecrafta. */
public final class Fonts {
    private Fonts() {}

    public enum Font {
        REGULAR("regular"), SEMIBOLD("semibold"), BOLD("bold"), DISPLAY("display");
        final Style style;
        Font(String id) { this.style = Style.EMPTY.withFont(Identifier.of("razerclient", id)); }
    }

    /** wysokość linii przy skali 1 */
    public static final float HEIGHT = 9f;

    private static TextRenderer tr() { return MinecraftClient.getInstance().textRenderer; }

    public static Text text(String s, Font f) { return Text.literal(s).setStyle(f.style); }

    public static float width(String s, Font f, float scale) { return tr().getWidth(text(s, f)) * scale; }

    public static float height(float scale) { return HEIGHT * scale; }

    public static void draw(DrawContext ctx, String s, float x, float y, int color, Font f, float scale) {
        int c = R2D.col(color);
        if (((c >>> 24) & 0xFF) < 6 || s.isEmpty()) return;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(scale, scale, 1);
        tr().draw(text(s, f), 0, 0, c, false, ctx.getMatrices().peek().getPositionMatrix(), ctx.getVertexConsumers(),
                TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
        ctx.getMatrices().pop();
        ctx.draw();
    }

    public static void drawCentered(DrawContext ctx, String s, float cx, float y, int color, Font f, float scale) {
        draw(ctx, s, cx - width(s, f, scale) / 2f, y, color, f, scale);
    }

    /** rysuje tekst wyśrodkowany pionowo względem środka cy */
    public static void drawV(DrawContext ctx, String s, float x, float cy, int color, Font f, float scale) {
        draw(ctx, s, x, cy - height(scale) / 2f, color, f, scale);
    }

    /** duża czcionka – rysowana tak, by górna krawędź wielkich liter była w topY */
    public static void drawDisplay(DrawContext ctx, String s, float cx, float topY, int color, float scale) {
        draw(ctx, s, cx - width(s, Font.DISPLAY, scale) / 2f, topY + 19f * scale / 0.62f * 0.62f, color, Font.DISPLAY, scale);
    }

    /** wysokość wielkich liter dużej czcionki */
    public static float displayCap(float scale) { return 26f * scale; }

    /** zawija tekst do szerokości maxW (po słowach) */
    public static java.util.List<String> wrap(String s, Font f, float scale, float maxW) {
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (width(test, f, scale) > maxW && line.length() > 0) { out.add(line.toString()); line = new StringBuilder(word); }
            else line = new StringBuilder(test);
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    public static String trim(String s, Font f, float scale, float maxW) {
        if (width(s, f, scale) <= maxW) return s;
        while (s.length() > 1 && width(s + "…", f, scale) > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
    }
}
