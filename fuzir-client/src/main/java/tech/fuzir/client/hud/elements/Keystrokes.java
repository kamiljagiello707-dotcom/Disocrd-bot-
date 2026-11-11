package tech.fuzir.client.hud.elements;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import tech.fuzir.client.event.events.MouseButtonEvent;
import tech.fuzir.client.gui.Theme;
import tech.fuzir.client.hud.HudElement;
import tech.fuzir.client.render.Fonts;
import tech.fuzir.client.render.RenderUtil;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.util.ColorUtil;
import tech.fuzir.client.util.animation.Animation;
import tech.fuzir.client.util.animation.Easing;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * WASD / mouse / space visualiser with smooth press transitions and live CPS counters.
 */
public final class Keystrokes extends HudElement {
    private static final float KEY = 22f;
    private static final float GAP = 2f;
    private static final float SPACE_HEIGHT = 11f;

    private final BoolSetting mouse = bool("Mouse Buttons", true);
    private final BoolSetting cps = register(new BoolSetting("CPS", true)).visibleWhen(mouse::isEnabled);
    private final BoolSetting space = bool("Space Bar", true);

    private final Deque<Long> leftClicks = new ArrayDeque<>();
    private final Deque<Long> rightClicks = new ArrayDeque<>();
    private final Animation[] keys = new Animation[7];

    public Keystrokes() {
        super("Keystrokes", "Shows movement keys, mouse buttons and CPS.", Anchor.START, Anchor.CENTER, 4f, 0f);
        for (int i = 0; i < keys.length; i++) keys[i] = new Animation(Easing.OUT_CUBIC, 120);
        on(MouseButtonEvent.class, event -> {
            if (event.action() != GLFW.GLFW_PRESS || mc.currentScreen != null) return;
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) leftClicks.addLast(System.currentTimeMillis());
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) rightClicks.addLast(System.currentTimeMillis());
        });
    }

    private static int cps(Deque<Long> clicks) {
        long cutoff = System.currentTimeMillis() - 1000L;
        while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) clicks.pollFirst();
        return clicks.size();
    }

    @Override
    public void measure(boolean editor) {
        width = KEY * 3f + GAP * 2f;
        height = KEY * 2f + GAP;
        if (mouse.isEnabled()) height += GAP + KEY;
        if (space.isEnabled()) height += GAP + SPACE_HEIGHT;
    }

    @Override
    public void render(DrawContext context, float x, float y, float tickDelta, boolean editor) {
        var options = mc.options;
        float col = KEY + GAP;
        drawKey(context, 0, options.forwardKey, "W", x + col, y, KEY, KEY, null);
        drawKey(context, 1, options.leftKey, "A", x, y + col, KEY, KEY, null);
        drawKey(context, 2, options.backKey, "S", x + col, y + col, KEY, KEY, null);
        drawKey(context, 3, options.rightKey, "D", x + col * 2f, y + col, KEY, KEY, null);
        float cursorY = y + col * 2f;
        if (mouse.isEnabled()) {
            float half = (width - GAP) / 2f;
            drawKey(context, 4, options.attackKey, "LMB", x, cursorY, half, KEY, cps.isEnabled() ? cps(leftClicks) + " CPS" : null);
            drawKey(context, 5, options.useKey, "RMB", x + half + GAP, cursorY, half, KEY, cps.isEnabled() ? cps(rightClicks) + " CPS" : null);
            cursorY += KEY + GAP;
        }
        if (space.isEnabled()) {
            drawSpace(context, options.jumpKey, x, cursorY, width, SPACE_HEIGHT);
        }
    }

    private void drawKey(DrawContext context, int index, KeyBinding binding, String label, float x, float y,
                         float w, float h, String subLabel) {
        MatrixStack matrices = context.getMatrices();
        float pressed = keys[index].animateTo(binding.isPressed()).getFloat();
        int accent = Theme.accent();
        if (background.isEnabled() || pressed > 0.01f) {
            int base = background.isEnabled() ? Theme.HUD_BG : ColorUtil.withAlpha(Theme.HUD_BG, 0);
            if (pressed > 0.01f) {
                RenderUtil.shadow(matrices, x, y, w, h, 4f, 5f, ColorUtil.withAlpha(accent, (int) (80 * pressed)));
            }
            RenderUtil.roundedRect(matrices, x, y, w, h, 4f, ColorUtil.interpolate(base, accent, pressed));
            RenderUtil.roundedOutline(matrices, x, y, w, h, 4f, 0.5f, Theme.BORDER);
        }
        int textColor = ColorUtil.interpolate(Theme.TEXT, Theme.onAccent(), pressed);
        if (subLabel == null) {
            Fonts.drawCentered(context, label, x + w / 2f, y + (h - Fonts.HEIGHT) / 2f + 0.5f, textColor, Fonts.SEMIBOLD);
        } else {
            Fonts.drawCentered(context, label, x + w / 2f, y + 4f, textColor, Fonts.SEMIBOLD);
            float scale = 0.7f;
            float subWidth = Fonts.width(subLabel) * scale;
            Fonts.draw(context, subLabel, x + (w - subWidth) / 2f, y + h - 8f, ColorUtil.interpolate(Theme.TEXT_DIM,
                    Theme.onAccent(), pressed), Fonts.MEDIUM, scale, false);
        }
    }

    private void drawSpace(DrawContext context, KeyBinding binding, float x, float y, float w, float h) {
        MatrixStack matrices = context.getMatrices();
        float pressed = keys[6].animateTo(binding.isPressed()).getFloat();
        int accent = Theme.accent();
        int base = background.isEnabled() ? Theme.HUD_BG : ColorUtil.withAlpha(Theme.HUD_BG, 0);
        RenderUtil.roundedRect(matrices, x, y, w, h, 3.5f, ColorUtil.interpolate(base, accent, pressed));
        if (background.isEnabled()) RenderUtil.roundedOutline(matrices, x, y, w, h, 3.5f, 0.5f, Theme.BORDER);
        float lineWidth = w * 0.4f;
        RenderUtil.roundedRect(matrices, x + (w - lineWidth) / 2f, y + h / 2f - 0.6f, lineWidth, 1.2f, 0.6f,
                ColorUtil.interpolate(Theme.TEXT_DIM, Theme.onAccent(), pressed));
    }
}
