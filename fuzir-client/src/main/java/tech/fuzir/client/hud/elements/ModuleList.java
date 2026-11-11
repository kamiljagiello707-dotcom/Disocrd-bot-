package tech.fuzir.client.hud.elements;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import tech.fuzir.client.FuzirClient;
import tech.fuzir.client.gui.Theme;
import tech.fuzir.client.hud.HudElement;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.render.Fonts;
import tech.fuzir.client.render.RenderUtil;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.setting.ModeSetting;
import tech.fuzir.client.util.ColorUtil;
import tech.fuzir.client.util.animation.Animation;
import tech.fuzir.client.util.animation.Easing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Animated list of enabled modules. Rows slide in from the attached screen edge, the list aligns to the
 * side it is anchored to and grows upwards when anchored to the bottom.
 */
public final class ModuleList extends HudElement {
    private static final float ROW_HEIGHT = 11f;
    private static final float ROW_PADDING = 4f;
    private static final float BAR_WIDTH = 1.5f;

    private final ModeSetting sorting = mode("Sorting", "Width", "Width", "Alphabetical");
    private final BoolSetting suffixes = bool("Suffixes", true);
    private final BoolSetting sideBar = bool("Side Bar", true);
    private final BoolSetting lowercase = bool("Lowercase", false);

    private final Map<Module, Animation> animations = new HashMap<>();
    private final List<Row> rows = new ArrayList<>();

    private record Row(Module module, String name, String suffix, float width, float progress) {
    }

    public ModuleList() {
        super("ModuleList", "Animated list of active modules.", Anchor.END, Anchor.START, 4f, 4f);
    }

    @Override
    public void measure(boolean editor) {
        rows.clear();
        for (Module module : FuzirClient.get().getModuleManager().getModules()) {
            if (!module.isDrawn()) continue;
            Animation animation = animations.computeIfAbsent(module,
                    m -> new Animation(Easing.OUT_CUBIC, 280, m.isEnabled() ? 1 : 0));
            float progress = animation.animateTo(module.isEnabled()).getFloat();
            if (progress <= 0.01f) continue;
            String name = format(module.getName());
            String suffix = suffixes.isEnabled() && module.getSuffix() != null ? format(module.getSuffix()) : null;
            float w = Fonts.width(name) + (suffix != null ? 4f + Fonts.width(suffix) : 0f) + ROW_PADDING * 2f
                    + (sideBar.isEnabled() ? BAR_WIDTH + 1f : 0f);
            rows.add(new Row(module, name, suffix, w, progress));
        }
        if (sorting.is("Width")) {
            rows.sort(Comparator.comparingDouble(Row::width).reversed().thenComparing(Row::name));
        } else {
            rows.sort(Comparator.comparing(Row::name, String.CASE_INSENSITIVE_ORDER));
        }
        if (getAnchorY() == Anchor.END) Collections.reverse(rows);

        float maxWidth = 0f;
        float totalHeight = 0f;
        for (Row row : rows) {
            maxWidth = Math.max(maxWidth, row.width());
            totalHeight += ROW_HEIGHT * row.progress();
        }
        if (rows.isEmpty() && editor) {
            maxWidth = Fonts.width("ModuleList") + ROW_PADDING * 2f;
            totalHeight = ROW_HEIGHT;
        }
        width = Math.max(maxWidth, 20f);
        height = Math.max(totalHeight, ROW_HEIGHT);
    }

    private String format(String text) {
        return lowercase.isEnabled() ? text.toLowerCase(Locale.ROOT) : text;
    }

    @Override
    public void render(DrawContext context, float x, float y, float tickDelta, boolean editor) {
        MatrixStack matrices = context.getMatrices();
        if (rows.isEmpty()) {
            if (editor) {
                drawPanel(context, x, y, width, height);
                Fonts.draw(context, "ModuleList", x + ROW_PADDING, y + (ROW_HEIGHT - Fonts.HEIGHT) / 2f + 0.5f, Theme.TEXT_MUTED);
            }
            return;
        }

        boolean rightAligned = getAnchorX() == Anchor.END;
        boolean bottomUp = getAnchorY() == Anchor.END;
        float cursor = bottomUp ? y + height : y;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            float rowHeight = ROW_HEIGHT * row.progress();
            float rowY = bottomUp ? cursor - rowHeight : cursor;
            // Slide in from the edge the list is attached to.
            float slide = (1f - row.progress()) * (row.width() + 8f);
            float rowX = rightAligned ? x + width - row.width() + slide : x - slide;
            int accent = Theme.accentWave(i * 220.0);
            float alpha = row.progress();

            if (background.isEnabled()) {
                RenderUtil.rect(matrices, rowX, rowY, row.width(), rowHeight, ColorUtil.multiplyAlpha(Theme.HUD_BG, alpha));
            }
            if (sideBar.isEnabled()) {
                float barX = rightAligned ? rowX + row.width() - BAR_WIDTH : rowX;
                RenderUtil.rect(matrices, barX, rowY, BAR_WIDTH, rowHeight, ColorUtil.multiplyAlpha(accent, alpha));
                RenderUtil.horizontalGradient(matrices,
                        rightAligned ? barX - 6f : barX + BAR_WIDTH, rowY, 6f, rowHeight,
                        ColorUtil.withAlpha(accent, rightAligned ? 0 : (int) (40 * alpha)),
                        ColorUtil.withAlpha(accent, rightAligned ? (int) (40 * alpha) : 0));
            }

            float textX = rowX + ROW_PADDING + (sideBar.isEnabled() && !rightAligned ? BAR_WIDTH + 1f : 0f);
            float textY = rowY + (rowHeight - Fonts.HEIGHT) / 2f + 0.5f;
            Fonts.draw(context, row.name(), textX, textY, ColorUtil.multiplyAlpha(accent, alpha));
            if (row.suffix() != null) {
                Fonts.draw(context, row.suffix(), textX + Fonts.width(row.name()) + 4f, textY,
                        ColorUtil.multiplyAlpha(Theme.TEXT_DIM, alpha));
            }
            cursor = bottomUp ? cursor - rowHeight : cursor + rowHeight;
        }
    }
}
