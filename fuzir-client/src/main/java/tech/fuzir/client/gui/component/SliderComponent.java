package tech.fuzir.client.gui.component;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import tech.fuzir.client.gui.Theme;
import tech.fuzir.client.render.Fonts;
import tech.fuzir.client.render.RenderUtil;
import tech.fuzir.client.setting.NumberSetting;
import tech.fuzir.client.util.ColorUtil;
import tech.fuzir.client.util.animation.Animation;
import tech.fuzir.client.util.animation.Easing;

/**
 * Numeric slider.
 * <ul>
 *     <li>Drag: absolute positioning along the track.</li>
 *     <li>Shift + drag: precision mode, the value moves at 10% of the mouse speed relative to where it was.</li>
 *     <li>Arrow keys while hovered: step by one increment. Right click: reset to default.</li>
 * </ul>
 */
public final class SliderComponent extends SettingComponent<NumberSetting> {
    private static final float HEIGHT = 23f;
    private static final float TRACK_HEIGHT = 3f;
    private static final float PRECISION = 0.1f;

    private final Animation fill;
    private final Animation hover = new Animation(Easing.OUT_QUAD, 150);
    private final Animation active = new Animation(Easing.OUT_CUBIC, 180);
    private boolean dragging;
    private boolean hoveredLastFrame;
    private boolean precision;
    private double lastMouseX;
    private double rawProgress;

    public SliderComponent(NumberSetting setting) {
        super(setting);
        this.fill = new Animation(Easing.OUT_CUBIC, 140, setting.getProgress());
    }

    @Override
    protected float getBaseHeight() {
        return HEIGHT;
    }

    private float trackX() {
        return x + PADDING;
    }

    private float trackWidth() {
        return width - PADDING * 2f;
    }

    @Override
    public void render(DrawContext context, double mouseX, double mouseY, float delta) {
        if (dragging) updateDrag(mouseX);

        MatrixStack matrices = context.getMatrices();
        boolean hovered = isHovered(mouseX, mouseY);
        hoveredLastFrame = hovered;
        float hoverProgress = hover.animateTo(hovered || dragging).getFloat();
        float activeProgress = active.animateTo(dragging).getFloat();
        int accent = Theme.accent();

        float textY = y + 4f;
        Fonts.draw(context, setting.getName(), x + PADDING, textY,
                ColorUtil.interpolate(Theme.TEXT_DIM, Theme.TEXT, hoverProgress));
        String value = setting.format();
        Fonts.draw(context, value, x + width - PADDING - Fonts.width(value), textY,
                ColorUtil.interpolate(Theme.TEXT, accent, 0.35f + 0.65f * Math.max(hoverProgress, activeProgress)));

        float tx = trackX();
        float tw = trackWidth();
        float ty = y + HEIGHT - 6.5f;
        float progress = (float) fill.animateTo(setting.getProgress()).get();
        float filled = tw * progress;

        RenderUtil.roundedRect(matrices, tx, ty, tw, TRACK_HEIGHT, TRACK_HEIGHT / 2f, Theme.TRACK);
        if (filled > 0.5f) {
            RenderUtil.roundedGradient(matrices, tx, ty, filled, TRACK_HEIGHT, TRACK_HEIGHT / 2f,
                    accent, Theme.accentSecondary(), true);
        }

        float knobX = tx + filled;
        float knobY = ty + TRACK_HEIGHT / 2f;
        float radius = 2.6f + hoverProgress * 0.9f;
        if (hoverProgress > 0.01f) {
            RenderUtil.shadow(matrices, knobX - radius, knobY - radius, radius * 2f, radius * 2f, radius,
                    3f + 2f * activeProgress, ColorUtil.withAlpha(accent, (int) (90 * hoverProgress)));
        }
        RenderUtil.circle(matrices, knobX, knobY, radius, Theme.KNOB);
    }

    private void updateDrag(double mouseX) {
        boolean shift = Screen.hasShiftDown();
        if (shift != precision) {
            // Switching modes rebases the fine-tune origin so the knob never jumps.
            precision = shift;
            rawProgress = setting.getProgress();
            lastMouseX = mouseX;
        }
        if (precision) {
            rawProgress += (mouseX - lastMouseX) / trackWidth() * PRECISION;
            rawProgress = Math.max(0.0, Math.min(1.0, rawProgress));
            lastMouseX = mouseX;
            setting.setProgress(rawProgress);
        } else {
            setting.setProgress((mouseX - trackX()) / trackWidth());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isHovered(mouseX, mouseY)) return false;
        if (button == 0) {
            dragging = true;
            precision = Screen.hasShiftDown();
            rawProgress = setting.getProgress();
            lastMouseX = mouseX;
            if (!precision) updateDrag(mouseX);
            return true;
        }
        if (button == 1) {
            setting.reset();
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
    }

    /** Arrow keys nudge the slider under the cursor by one increment. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!hoveredLastFrame) return false;
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_DOWN) {
            setting.increment(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_UP) {
            setting.increment(1);
            return true;
        }
        return false;
    }
}
