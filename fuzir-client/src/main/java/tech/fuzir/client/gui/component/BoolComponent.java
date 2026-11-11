package tech.fuzir.client.gui.component;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import tech.fuzir.client.gui.Theme;
import tech.fuzir.client.render.Fonts;
import tech.fuzir.client.render.RenderUtil;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.util.ColorUtil;
import tech.fuzir.client.util.animation.Animation;
import tech.fuzir.client.util.animation.Easing;

/**
 * Label with an iOS-style pill switch whose knob slides and whose track fades into the accent color.
 */
public final class BoolComponent extends SettingComponent<BoolSetting> {
    private static final float HEIGHT = 15f;
    private static final float SWITCH_WIDTH = 17f;
    private static final float SWITCH_HEIGHT = 9f;

    private final Animation state;
    private final Animation hover = new Animation(Easing.OUT_QUAD, 150);

    public BoolComponent(BoolSetting setting) {
        super(setting);
        this.state = new Animation(Easing.OUT_BACK, 260, setting.isEnabled() ? 1 : 0);
    }

    @Override
    protected float getBaseHeight() {
        return HEIGHT;
    }

    @Override
    public void render(DrawContext context, double mouseX, double mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();
        float progress = state.animateTo(setting.isEnabled()).getFloat();
        float clamped = Math.max(0f, Math.min(1f, progress));
        float hovered = hover.animateTo(isHovered(mouseX, mouseY)).getFloat();
        int accent = Theme.accent();

        int label = ColorUtil.interpolate(Theme.TEXT_DIM, Theme.TEXT, Math.max(clamped, hovered * 0.6f));
        Fonts.draw(context, setting.getName(), x + PADDING, y + (HEIGHT - Fonts.HEIGHT) / 2f, label);

        float sx = x + width - PADDING - SWITCH_WIDTH;
        float sy = y + (HEIGHT - SWITCH_HEIGHT) / 2f;
        int track = ColorUtil.interpolate(Theme.TRACK, accent, clamped);
        if (clamped > 0.01f) {
            RenderUtil.shadow(matrices, sx, sy, SWITCH_WIDTH, SWITCH_HEIGHT, SWITCH_HEIGHT / 2f, 4f,
                    ColorUtil.withAlpha(accent, (int) (70 * clamped)));
        }
        RenderUtil.roundedRect(matrices, sx, sy, SWITCH_WIDTH, SWITCH_HEIGHT, SWITCH_HEIGHT / 2f, track);

        float knobRadius = SWITCH_HEIGHT / 2f - 1.5f;
        float travel = SWITCH_WIDTH - SWITCH_HEIGHT;
        float knobX = sx + SWITCH_HEIGHT / 2f + travel * progress;
        RenderUtil.circle(matrices, knobX, sy + SWITCH_HEIGHT / 2f, knobRadius + hovered * 0.4f, Theme.KNOB);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isHovered(mouseX, mouseY) || button != 0) return false;
        setting.toggle();
        return true;
    }
}
