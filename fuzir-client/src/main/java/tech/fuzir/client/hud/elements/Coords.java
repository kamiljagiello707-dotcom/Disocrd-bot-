package tech.fuzir.client.hud.elements;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Direction;
import tech.fuzir.client.gui.Theme;
import tech.fuzir.client.hud.HudElement;
import tech.fuzir.client.render.Fonts;
import tech.fuzir.client.setting.BoolSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Position card: XYZ, the matching coordinates in the other dimension and the facing direction.
 */
public final class Coords extends HudElement {
    private static final float PADDING = 5f;
    private static final float LINE = 11f;

    private final BoolSetting otherDimension = bool("Nether / Overworld", true);
    private final BoolSetting direction = bool("Direction", true);

    private final List<String[]> lines = new ArrayList<>();

    public Coords() {
        super("Coords", "Your coordinates and facing direction.", Anchor.START, Anchor.END, 4f, 4f);
    }

    @Override
    public void measure(boolean editor) {
        lines.clear();
        double x = 0, y = 0, z = 0;
        boolean nether = false;
        Direction facing = Direction.NORTH;
        if (mc.player != null && mc.world != null) {
            x = mc.player.getX();
            y = mc.player.getY();
            z = mc.player.getZ();
            nether = mc.world.getDimension().coordinateScale() > 1.0;
            facing = mc.player.getHorizontalFacing();
        }
        lines.add(new String[]{"XYZ", String.format(Locale.ROOT, "%.1f  %.1f  %.1f", x, y, z)});
        if (otherDimension.isEnabled()) {
            double scale = nether ? 8.0 : 0.125;
            lines.add(new String[]{nether ? "Overworld" : "Nether",
                    String.format(Locale.ROOT, "%.1f  %.1f", x * scale, z * scale)});
        }
        if (direction.isEnabled()) {
            lines.add(new String[]{"Facing", capitalize(facing.getName()) + " (" + axis(facing) + ")"});
        }

        float labelWidth = 0f;
        float valueWidth = 0f;
        for (String[] line : lines) {
            labelWidth = Math.max(labelWidth, Fonts.width(line[0], Fonts.SEMIBOLD));
            valueWidth = Math.max(valueWidth, Fonts.width(line[1]));
        }
        width = PADDING * 2f + labelWidth + 14f + valueWidth;
        height = PADDING * 2f + lines.size() * LINE - 2f;
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String axis(Direction direction) {
        return switch (direction) {
            case NORTH -> "-Z";
            case SOUTH -> "+Z";
            case WEST -> "-X";
            case EAST -> "+X";
            default -> "";
        };
    }

    @Override
    public void render(DrawContext context, float x, float y, float tickDelta, boolean editor) {
        drawPanel(context, x, y, width, height);
        float labelWidth = 0f;
        for (String[] line : lines) labelWidth = Math.max(labelWidth, Fonts.width(line[0], Fonts.SEMIBOLD));
        float cursorY = y + PADDING;
        for (int i = 0; i < lines.size(); i++) {
            String[] line = lines.get(i);
            Fonts.drawBold(context, line[0], x + PADDING, cursorY, Theme.accentWave(i * 250.0));
            float sepX = x + PADDING + labelWidth + 4f;
            Fonts.draw(context, Theme.SEPARATOR_GLYPH, sepX, cursorY, Theme.TEXT_MUTED);
            Fonts.draw(context, line[1], sepX + 10f, cursorY, Theme.TEXT);
            cursorY += LINE;
        }
    }
}
