package tech.fuzir.client.gui.component;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import tech.fuzir.client.gui.Theme;
import tech.fuzir.client.render.Fonts;
import tech.fuzir.client.render.RenderUtil;
import tech.fuzir.client.setting.KeySetting;
import tech.fuzir.client.util.ColorUtil;
import tech.fuzir.client.util.animation.Animation;
import tech.fuzir.client.util.animation.Easing;

/**
 * Key binding capture. Click to listen, press any key to bind, ESC to cancel, Delete/Backspace to unbind.
 */
public final class KeyComponent extends SettingComponent<KeySetting> {
    private static final float HEIGHT = 15f;

    private final Animation listen = new Animation(Easing.OUT_CUBIC, 180);
    private final Animation hover = new Animation(Easing.OUT_QUAD, 150);
    private final Animation boxWidth;
    private boolean listening;

    public KeyComponent(KeySetting setting) {
        super(setting);
        this.boxWidth = new Animation(Easing.OUT_CUBIC, 160, Fonts.width(label()) + 8f);
    }

    private String label() {
        return listening ? "..." : setting.getKeyName();
    }

    public boolean isListening() {
        return listening;
    }

    @Override
    protected float getBaseHeight() {
        return HEIGHT;
    }

    @Override
    public void render(DrawContext context, double mouseX, double mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();
        float listening = listen.animateTo(this.listening).getFloat();
        float hovered = hover.animateTo(isHovered(mouseX, mouseY)).getFloat();
        int accent = Theme.accent();

        Fonts.draw(context, setting.getName(), x + PADDING, y + (HEIGHT - Fonts.HEIGHT) / 2f,
                ColorUtil.interpolate(Theme.TEXT_DIM, Theme.TEXT, hovered * 0.8f));

        String text = label();
        float bw = boxWidth.animateTo(Fonts.width(text) + 8f).getFloat();
        float bh = 11f;
        float bx = x + width - PADDING - bw;
        float by = y + (HEIGHT - bh) / 2f;

        int fill = ColorUtil.interpolate(Theme.TRACK, ColorUtil.withAlpha(accent, 0x40), listening);
        RenderUtil.roundedRect(matrices, bx, by, bw, bh, 3f, fill);
        RenderUtil.roundedOutline(matrices, bx, by, bw, bh, 3f, 0.6f,
                ColorUtil.interpolate(Theme.BORDER, accent, Math.max(listening, hovered * 0.5f)));

        int textColor = setting.isBound() ? Theme.TEXT : Theme.TEXT_MUTED;
        if (this.listening) {
            double pulse = (Math.sin(System.currentTimeMillis() / 140.0) + 1.0) / 2.0;
            textColor = ColorUtil.interpolate(accent, Theme.TEXT, pulse);
        }
        Fonts.draw(context, text, bx + (bw - Fonts.width(text)) / 2f, by + (bh - Fonts.HEIGHT) / 2f + 0.5f, textColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isHovered(mouseX, mouseY)) {
            listening = false;
            return false;
        }
        if (button == 0) {
            listening = !listening;
            return true;
        }
        if (button == 1) {
            setting.set(KeySetting.NONE);
            listening = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!listening) return false;
        if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            setting.set(KeySetting.NONE);
        } else if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
            setting.set(keyCode);
        }
        listening = false;
        return true;
    }
}
