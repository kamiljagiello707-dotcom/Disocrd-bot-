package tech.fuzir.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import tech.fuzir.client.FuzirClient;
import tech.fuzir.client.gui.component.Component;
import tech.fuzir.client.hud.HudElement;
import tech.fuzir.client.hud.HudManager;
import tech.fuzir.client.render.Fonts;
import tech.fuzir.client.render.RenderUtil;
import tech.fuzir.client.util.ColorUtil;
import tech.fuzir.client.util.animation.Animation;
import tech.fuzir.client.util.animation.Easing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Drag & drop HUD layout editor.
 * <ul>
 *     <li>Left drag moves an element, snapping its edges/center to screen edges, the screen center and the
 *     edges of other elements. Hold Alt to place freely.</li>
 *     <li>Right click toggles an element on/off (disabled elements are shown faded).</li>
 *     <li>Arrow keys nudge the selected element by 1px (Shift: 10px). ESC returns to the ClickGUI.</li>
 * </ul>
 */
public final class HudEditorScreen extends Screen {
    private static final float SNAP_DISTANCE = 5f;
    private static final float EDGE_MARGIN = 4f;

    private final Screen parent;
    private final Animation fade = new Animation(Easing.OUT_CUBIC, 220);
    private final Map<HudElement, Animation> hover = new HashMap<>();
    private HudElement dragging;
    private HudElement selected;
    private float dragOffsetX;
    private float dragOffsetY;
    private Float guideX;
    private Float guideY;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("Fuzir HUD Editor"));
        this.parent = parent;
    }

    private List<HudElement> elements() {
        return FuzirClient.get().getHudManager().getElements();
    }

    @Override
    protected void init() {
        fade.set(0).animateTo(1);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public void removed() {
        FuzirClient.get().getConfigManager().save();
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();
        float alpha = fade.getFloat();
        int accent = Theme.accent();

        RenderUtil.rect(matrices, 0, 0, width, height, ColorUtil.withAlpha(0, (int) (0x55 * alpha)));
        drawGrid(matrices, alpha);

        // Measure first so the drag position can be resolved against up-to-date sizes.
        for (HudElement element : elements()) element.measure(true);
        if (dragging != null) applyDrag(mouseX, mouseY);

        HudElement hovered = dragging != null ? dragging : elementAt(mouseX, mouseY);
        for (HudElement element : elements()) {
            boolean enabled = element.isEnabled();
            RenderSystem.setShaderColor(1f, 1f, 1f, enabled ? 1f : 0.35f);
            HudManager.draw(context, element, width, height, delta, true);

            float x = element.getX(width);
            float y = element.getY(height);
            float w = element.getWidth();
            float h = element.getHeight();
            float hoverProgress = hover.computeIfAbsent(element, e -> new Animation(Easing.OUT_QUAD, 140))
                    .animateTo(element == hovered || element == selected).getFloat();
            int outline = ColorUtil.interpolate(0x30FFFFFF, accent, hoverProgress);
            RenderUtil.roundedOutline(matrices, x - 1.5f, y - 1.5f, w + 3f, h + 3f, 4f, 0.7f, outline);
            if (hoverProgress > 0.01f) {
                RenderUtil.roundedRect(matrices, x - 1.5f, y - 1.5f, w + 3f, h + 3f, 4f,
                        ColorUtil.withAlpha(accent, (int) (18 * hoverProgress)));
                drawLabel(context, element, x, y, w, h, hoverProgress);
            }
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        if (dragging != null) drawGuides(matrices, accent);
        drawHelp(context, alpha);
    }

    private void drawGrid(MatrixStack matrices, float alpha) {
        int dot = ColorUtil.withAlpha(0xFFFFFF, (int) (14 * alpha));
        for (int gx = 0; gx < width; gx += 16) {
            for (int gy = 0; gy < height; gy += 16) RenderUtil.rect(matrices, gx, gy, 1f, 1f, dot);
        }
        int center = ColorUtil.withAlpha(0xFFFFFF, (int) (22 * alpha));
        RenderUtil.rect(matrices, width / 2f - 0.25f, 0, 0.5f, height, center);
        RenderUtil.rect(matrices, 0, height / 2f - 0.25f, width, 0.5f, center);
    }

    private void drawGuides(MatrixStack matrices, int accent) {
        int color = ColorUtil.withAlpha(accent, 0xB0);
        if (guideX != null) RenderUtil.rect(matrices, guideX - 0.5f, 0, 1f, height, color);
        if (guideY != null) RenderUtil.rect(matrices, 0, guideY - 0.5f, width, 1f, color);
    }

    private void drawLabel(DrawContext context, HudElement element, float x, float y, float w, float h, float alpha) {
        MatrixStack matrices = context.getMatrices();
        String text = element.getName() + (element.isEnabled() ? "" : "  (off)") + "  " + Theme.SEPARATOR_GLYPH + "  "
                + Math.round(x) + ", " + Math.round(y);
        float tw = Fonts.width(text) + 10f;
        float th = 13f;
        float lx = Math.max(2f, Math.min(width - tw - 2f, x + w / 2f - tw / 2f));
        float ly = y - th - 4f < 2f ? y + h + 4f : y - th - 4f;
        matrices.push();
        matrices.translate(0f, 0f, 300f);
        RenderUtil.roundedRect(matrices, lx, ly, tw, th, 3.5f, ColorUtil.multiplyAlpha(Theme.PANEL, alpha));
        RenderUtil.roundedOutline(matrices, lx, ly, tw, th, 3.5f, 0.5f, ColorUtil.withAlpha(Theme.accent(), (int) (0x80 * alpha)));
        Fonts.draw(context, text, lx + 5f, ly + (th - Fonts.HEIGHT) / 2f + 0.5f, ColorUtil.multiplyAlpha(Theme.TEXT, alpha));
        matrices.pop();
    }

    private void drawHelp(DrawContext context, float alpha) {
        MatrixStack matrices = context.getMatrices();
        String sep = "  " + Theme.SEPARATOR_GLYPH + "  ";
        String text = "▣ HUD Editor" + sep + "Drag to move" + sep + "Right click to toggle" + sep
                + "Alt: free placement" + sep + "ESC: back";
        float w = Fonts.width(text) + 16f;
        float h = 17f;
        float x = (width - w) / 2f;
        float y = height - h - 8f;
        RenderUtil.shadow(matrices, x, y, w, h, 5f, 8f, ColorUtil.withAlpha(0, (int) (0x80 * alpha)));
        RenderUtil.roundedRect(matrices, x, y, w, h, 5f, ColorUtil.multiplyAlpha(Theme.PANEL, alpha));
        RenderUtil.roundedOutline(matrices, x, y, w, h, 5f, 0.6f, ColorUtil.multiplyAlpha(Theme.BORDER, alpha));
        Fonts.draw(context, text, x + 8f, y + (h - Fonts.HEIGHT) / 2f + 0.5f, ColorUtil.multiplyAlpha(Theme.TEXT_DIM, alpha));
    }

    // ------------------------------------------------------------------ dragging & snapping

    private void applyDrag(double mouseX, double mouseY) {
        float w = dragging.getWidth();
        float h = dragging.getHeight();
        float x = (float) mouseX - dragOffsetX;
        float y = (float) mouseY - dragOffsetY;
        guideX = null;
        guideY = null;

        if (!hasAltDown()) {
            SnapResult sx = snap(x, w, width, true);
            SnapResult sy = snap(y, h, height, false);
            x = sx.position;
            y = sy.position;
            guideX = sx.guide;
            guideY = sy.guide;
        }
        x = Math.max(0f, Math.min(width - w, x));
        y = Math.max(0f, Math.min(height - h, y));
        dragging.setPosition(x, y, width, height);
    }

    private record SnapResult(float position, Float guide) {
    }

    /**
     * Finds the closest snap target for any of the element's three reference lines (start, center, end).
     */
    private SnapResult snap(float position, float size, int screenSize, boolean horizontal) {
        float best = SNAP_DISTANCE;
        float result = position;
        Float guide = null;

        float[] own = {position, position + size / 2f, position + size};
        List<Float> targets = new ArrayList<>(List.of(EDGE_MARGIN, screenSize / 2f, screenSize - EDGE_MARGIN));
        for (HudElement other : elements()) {
            if (other == dragging || !other.isEnabled()) continue;
            float start = horizontal ? other.getX(width) : other.getY(height);
            float length = horizontal ? other.getWidth() : other.getHeight();
            targets.add(start);
            targets.add(start + length / 2f);
            targets.add(start + length);
            // Leave a small gap when stacking next to another element.
            targets.add(start - 2f);
            targets.add(start + length + 2f);
        }

        for (int i = 0; i < own.length; i++) {
            for (float target : targets) {
                float distance = Math.abs(own[i] - target);
                if (distance < best) {
                    best = distance;
                    float offset = own[i] - position;
                    result = target - offset;
                    guide = target;
                }
            }
        }
        return new SnapResult(result, guide);
    }

    private HudElement elementAt(double mouseX, double mouseY) {
        List<HudElement> list = elements();
        for (int i = list.size() - 1; i >= 0; i--) {
            HudElement element = list.get(i);
            if (Component.isHovered(mouseX, mouseY, element.getX(width) - 1.5f, element.getY(height) - 1.5f,
                    element.getWidth() + 3f, element.getHeight() + 3f)) {
                return element;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        HudElement element = elementAt(mouseX, mouseY);
        if (element == null) {
            selected = null;
            return false;
        }
        if (button == 0) {
            dragging = element;
            selected = element;
            dragOffsetX = (float) mouseX - element.getX(width);
            dragOffsetY = (float) mouseY - element.getY(height);
            return true;
        }
        if (button == 1) {
            element.toggle();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging != null) {
            dragging = null;
            guideX = null;
            guideY = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selected != null) {
            float step = hasShiftDown() ? 10f : 1f;
            float dx = 0f;
            float dy = 0f;
            switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT -> dx = -step;
                case GLFW.GLFW_KEY_RIGHT -> dx = step;
                case GLFW.GLFW_KEY_UP -> dy = -step;
                case GLFW.GLFW_KEY_DOWN -> dy = step;
                default -> {
                }
            }
            if (dx != 0f || dy != 0f) {
                selected.measure(true);
                selected.setPosition(selected.getX(width) + dx, selected.getY(height) + dy, width, height);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
