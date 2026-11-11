package tech.fuzir.client.hud.elements;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import tech.fuzir.client.FuzirClient;
import tech.fuzir.client.gui.Theme;
import tech.fuzir.client.hud.HudElement;
import tech.fuzir.client.render.Fonts;
import tech.fuzir.client.setting.BoolSetting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * "✿ Fuzir ︲ Player ︲ 144 fps ︲ 23 ms ︲ 14:05" card.
 */
public final class Watermark extends HudElement {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final float PADDING = 6f;
    private static final float HEIGHT = 17f;
    private static final float GAP = 5f;
    private static final String ICON = "\u273F";

    private final BoolSetting username = bool("Username", true);
    private final BoolSetting fps = bool("FPS", true);
    private final BoolSetting ping = bool("Ping", true);
    private final BoolSetting time = bool("Time", true);

    private final List<String> parts = new ArrayList<>();

    public Watermark() {
        super("Watermark", "Client name with live FPS, ping and time.", Anchor.START, Anchor.START, 4f, 4f);
    }

    private String brand() {
        return FuzirClient.NAME;
    }

    private void collect() {
        parts.clear();
        if (username.isEnabled()) parts.add(mc.getSession().getUsername());
        if (fps.isEnabled()) parts.add(mc.getCurrentFps() + " fps");
        if (ping.isEnabled()) parts.add(currentPing() + " ms");
        if (time.isEnabled()) parts.add(LocalTime.now().format(TIME));
    }

    private int currentPing() {
        if (mc.player == null || mc.getNetworkHandler() == null) return 0;
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }

    /** Width of the brand as drawn letter by letter (no kerning between glyphs). */
    private float brandWidth() {
        String brand = brand();
        float total = 0f;
        for (int i = 0; i < brand.length(); i++) total += Fonts.width(String.valueOf(brand.charAt(i)), Fonts.SEMIBOLD);
        return total;
    }

    @Override
    public void measure(boolean editor) {
        collect();
        float w = PADDING + Fonts.width(ICON) + 4f + brandWidth();
        float separator = Fonts.width(Theme.SEPARATOR_GLYPH);
        for (String part : parts) w += GAP * 2f + separator + Fonts.width(part);
        width = w + PADDING;
        height = HEIGHT;
    }

    @Override
    public void render(DrawContext context, float x, float y, float tickDelta, boolean editor) {
        drawPanel(context, x, y, width, height);
        drawAccentLine(context, x + 4f, y, width - 8f);

        float textY = y + (height - Fonts.HEIGHT) / 2f + 0.5f;
        float cursor = x + PADDING;
        Fonts.draw(context, ICON, cursor, textY, Theme.accentWave(0));
        cursor += Fonts.width(ICON) + 4f;

        // Brand name with a per-letter traveling accent wave.
        String brand = brand();
        for (int i = 0; i < brand.length(); i++) {
            String letter = String.valueOf(brand.charAt(i));
            Fonts.drawBold(context, letter, cursor, textY, Theme.accentWave(i * 160.0));
            cursor += Fonts.width(letter, Fonts.SEMIBOLD);
        }

        for (String part : parts) {
            cursor += GAP;
            Fonts.draw(context, Theme.SEPARATOR_GLYPH, cursor, textY, Theme.TEXT_MUTED);
            cursor += Fonts.width(Theme.SEPARATOR_GLYPH) + GAP;
            Fonts.draw(context, part, cursor, textY, Theme.TEXT);
            cursor += Fonts.width(part);
        }
    }
}
