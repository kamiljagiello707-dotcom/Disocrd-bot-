package tech.fuzir.client.gui.component;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import tech.fuzir.client.gui.Theme;
import tech.fuzir.client.render.Fonts;
import tech.fuzir.client.render.RenderUtil;
import tech.fuzir.client.render.Scissor;
import tech.fuzir.client.setting.ModeSetting;
import tech.fuzir.client.util.ColorUtil;
import tech.fuzir.client.util.animation.Animation;
import tech.fuzir.client.util.animation.Easing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mode selector with an animated drop-down. Left click opens the list, right click cycles forward.
 */
public final class ModeComponent extends SettingComponent<ModeSetting> {
    private static final float HEADER = 15f;
    private static final float OPTION = 12f;
    private static final float LIST_PADDING = 3f;

    private final Animation expand = new Animation(Easing.OUT_CUBIC, 240);
    private final Animation hover = new Animation(Easing.OUT_QUAD, 150);
    private final Map<String, Animation> optionHover = new HashMap<>();
    private final Map<String, Animation> optionSelected = new HashMap<>();
    private boolean open;

    public ModeComponent(ModeSetting setting) {
        super(setting);
        for (String mode : setting.getModes()) {
            optionHover.put(mode, new Animation(Easing.OUT_QUAD, 120));
            optionSelected.put(mode, new Animation(Easing.OUT_CUBIC, 200, setting.is(mode) ? 1 : 0));
        }
    }

    private float listHeight() {
        return setting.getModes().size() * OPTION + LIST_PADDING * 2f;
    }

    @Override
    protected float getBaseHeight() {
        return HEADER + listHeight() * expand.animateTo(open).getFloat();
    }

    @Override
    public void render(DrawContext context, double mouseX, double mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();
        float expanded = expand.animateTo(open).getFloat();
        boolean headerHovered = isHovered(mouseX, mouseY, x, y, width, HEADER);
        float hovered = hover.animateTo(headerHovered).getFloat();
        int accent = Theme.accent();

        Fonts.draw(context, setting.getName(), x + PADDING, y + (HEADER - Fonts.HEIGHT) / 2f,
                ColorUtil.interpolate(Theme.TEXT_DIM, Theme.TEXT, hovered * 0.8f));

        // Rotating chevron on the far right, current mode just before it.
        float chevronX = x + width - PADDING - 3f;
        float chevronY = y + HEADER / 2f;
        matrices.push();
        matrices.translate(chevronX, chevronY, 0f);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90f * expanded));
        Fonts.draw(context, "›", -Fonts.width("›") / 2f, -Fonts.HEIGHT / 2f,
                ColorUtil.interpolate(Theme.TEXT_MUTED, accent, Math.max(expanded, hovered)));
        matrices.pop();

        String current = setting.get();
        Fonts.draw(context, current, chevronX - 7f - Fonts.width(current), y + (HEADER - Fonts.HEIGHT) / 2f,
                ColorUtil.interpolate(Theme.TEXT, accent, 0.6f + 0.4f * hovered));

        if (expanded <= 0.01f) return;

        float listY = y + HEADER;
        float listH = listHeight() * expanded;
        float boxX = x + PADDING - 2f;
        float boxW = width - PADDING * 2f + 4f;
        Scissor.push(context, x, listY, width, listH);
        RenderUtil.roundedRect(matrices, boxX, listY, boxW, listHeight() - 1f, 3f,
                ColorUtil.multiplyAlpha(0xFF121216, expanded));
        RenderUtil.roundedOutline(matrices, boxX, listY, boxW, listHeight() - 1f, 3f, 0.6f,
                ColorUtil.multiplyAlpha(Theme.BORDER, expanded));

        List<String> modes = setting.getModes();
        for (int i = 0; i < modes.size(); i++) {
            String mode = modes.get(i);
            float oy = listY + LIST_PADDING + i * OPTION;
            boolean optionHovered = isHovered(mouseX, mouseY, boxX, oy, boxW, OPTION) && mouseY < listY + listH;
            float oh = optionHover.get(mode).animateTo(optionHovered).getFloat();
            float selected = optionSelected.get(mode).animateTo(setting.is(mode)).getFloat();

            if (oh > 0.01f) {
                RenderUtil.roundedRect(matrices, boxX + 2f, oy, boxW - 4f, OPTION, 2.5f,
                        ColorUtil.multiplyAlpha(Theme.ELEMENT_HOVER, oh * expanded));
            }
            if (selected > 0.01f) {
                RenderUtil.circle(matrices, boxX + 7f, oy + OPTION / 2f, 1.6f * selected,
                        ColorUtil.multiplyAlpha(accent, expanded));
            }
            int color = ColorUtil.interpolate(Theme.TEXT_DIM, accent, selected);
            color = ColorUtil.interpolate(color, Theme.TEXT, oh * (1f - selected) * 0.7f);
            Fonts.draw(context, mode, boxX + 11f + 2f * selected, oy + (OPTION - Fonts.HEIGHT) / 2f,
                    ColorUtil.multiplyAlpha(color, expanded));
        }
        Scissor.pop(context);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, x, y, width, HEADER)) {
            if (button == 0) open = !open;
            else if (button == 1) setting.cycle(1);
            else return false;
            return true;
        }
        if (open && button == 0 && expand.getFloat() > 0.9f) {
            List<String> modes = setting.getModes();
            for (int i = 0; i < modes.size(); i++) {
                float oy = y + HEADER + LIST_PADDING + i * OPTION;
                if (isHovered(mouseX, mouseY, x, oy, width, OPTION)) {
                    setting.set(modes.get(i));
                    open = false;
                    return true;
                }
            }
        }
        return false;
    }
}
