package tech.fuzir.client.gui.panel;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import tech.fuzir.client.gui.ClickGuiScreen;
import tech.fuzir.client.gui.Theme;
import tech.fuzir.client.gui.component.Component;
import tech.fuzir.client.gui.component.ModuleButton;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.render.Fonts;
import tech.fuzir.client.render.RenderUtil;
import tech.fuzir.client.render.Scissor;
import tech.fuzir.client.util.ColorUtil;
import tech.fuzir.client.util.animation.Animation;
import tech.fuzir.client.util.animation.Easing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Draggable, collapsible, scrollable category panel.
 * Left-drag the header to move it, right click the header to collapse, scroll the body when it overflows.
 */
public final class Panel {
    public static final float WIDTH = 118f;
    public static final float HEADER = 22f;
    private static final float RADIUS = 6f;
    private static final float FOOTER = 4f;

    private final ClickGuiScreen screen;
    private final Category category;
    private final List<ModuleButton> buttons = new ArrayList<>();
    private final Animation open = new Animation(Easing.OUT_CUBIC, 300, 1);
    private final Animation scroll = new Animation(Easing.OUT_CUBIC, 220);
    private final Animation headerHover = new Animation(Easing.OUT_QUAD, 150);
    private final Animation dragLift = new Animation(Easing.OUT_CUBIC, 180);

    private float x;
    private float y;
    private boolean expanded = true;
    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    private float scrollTarget;
    private float maxScroll;
    private float bodyTop;
    private float bodyHeight;

    public Panel(ClickGuiScreen screen, Category category, List<Module> modules, float x, float y) {
        this.screen = screen;
        this.category = category;
        this.x = x;
        this.y = y;
        for (Module module : modules) buttons.add(new ModuleButton(module));
    }

    public Category getCategory() {
        return category;
    }

    private List<ModuleButton> visibleButtons() {
        String query = screen.getSearch().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) return buttons;
        List<ModuleButton> filtered = new ArrayList<>();
        for (ModuleButton button : buttons) {
            if (button.getModule().getName().toLowerCase(Locale.ROOT).contains(query)) filtered.add(button);
        }
        return filtered;
    }

    public void render(DrawContext context, double mouseX, double mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();
        if (dragging) {
            x = (float) mouseX - dragOffsetX;
            y = (float) mouseY - dragOffsetY;
        }
        clampToScreen();

        List<ModuleButton> visible = visibleButtons();
        float content = 0f;
        for (ModuleButton button : visible) content += button.getHeight();

        float openProgress = open.animateTo(expanded).getFloat();
        float available = Math.max(40f, screen.getVirtualBottom() - y - HEADER - FOOTER - 6f);
        float body = Math.min(content, available);
        maxScroll = Math.max(0f, content - available);
        scrollTarget = Math.max(0f, Math.min(maxScroll, scrollTarget));
        float scrollOffset = scroll.animateTo(scrollTarget).getFloat();

        bodyTop = y + HEADER;
        bodyHeight = body * openProgress;
        float totalHeight = HEADER + bodyHeight + FOOTER * openProgress;
        float lift = dragLift.animateTo(dragging).getFloat();

        RenderUtil.shadow(matrices, x, y, WIDTH, totalHeight, RADIUS, 10f + 6f * lift,
                ColorUtil.withAlpha(0, (int) (110 + 60 * lift)));
        RenderUtil.roundedRect(matrices, x, y, WIDTH, totalHeight, RADIUS, Theme.PANEL);
        renderHeader(context, mouseX, mouseY, openProgress);

        if (bodyHeight > 0.5f) {
            Scissor.push(context, x, bodyTop, WIDTH, bodyHeight);
            float cursor = bodyTop - scrollOffset;
            boolean mouseInBody = isInBody(mouseX, mouseY);
            for (ModuleButton button : visible) {
                float height = button.getHeight();
                button.setBounds(x, cursor, WIDTH);
                if (cursor + height >= bodyTop && cursor <= bodyTop + bodyHeight) {
                    button.render(context, mouseInBody ? mouseX : -1, mouseInBody ? mouseY : -1, delta);
                }
                cursor += height;
            }
            Scissor.pop(context);
            renderScrollbar(matrices, content, scrollOffset);
        }

        int border = ColorUtil.interpolate(Theme.BORDER, ColorUtil.withAlpha(Theme.accent(), 0x55), lift);
        RenderUtil.roundedOutline(matrices, x, y, WIDTH, totalHeight, RADIUS, 0.6f, border);
    }

    private void renderHeader(DrawContext context, double mouseX, double mouseY, float openProgress) {
        MatrixStack matrices = context.getMatrices();
        float hover = headerHover.animateTo(isOverHeader(mouseX, mouseY)).getFloat();
        int accent = Theme.accent();

        float bottomRadius = RADIUS * (1f - openProgress);
        RenderUtil.roundedRect(matrices, x, y, WIDTH, HEADER, RADIUS, RADIUS, bottomRadius, bottomRadius, Theme.HEADER);
        if (hover > 0.01f) {
            RenderUtil.roundedRect(matrices, x, y, WIDTH, HEADER, RADIUS, RADIUS, bottomRadius, bottomRadius,
                    ColorUtil.multiplyAlpha(Theme.ELEMENT_HOVER, hover));
        }

        float textY = y + (HEADER - Fonts.HEIGHT) / 2f;
        Fonts.draw(context, category.getIcon(), x + 8f, textY, accent);
        float iconWidth = Fonts.width(category.getIcon());
        Fonts.draw(context, Theme.SEPARATOR_GLYPH, x + 8f + iconWidth + 2f, textY, Theme.TEXT_MUTED);
        Fonts.drawBold(context, category.getDisplayName(), x + 8f + iconWidth + 10f, textY, Theme.TEXT);

        int enabled = 0;
        for (ModuleButton button : buttons) if (button.getModule().isEnabled()) enabled++;
        String counter = enabled + "/" + buttons.size();
        Fonts.draw(context, counter, x + WIDTH - 8f - Fonts.width(counter), textY,
                ColorUtil.interpolate(Theme.TEXT_MUTED, accent, enabled > 0 ? 0.65f : 0f));

        if (openProgress > 0.01f) {
            // Separator that grows from the center as the panel opens.
            float lineWidth = (WIDTH - 16f) * openProgress;
            float lineX = x + (WIDTH - lineWidth) / 2f;
            RenderUtil.horizontalGradient(matrices, lineX, y + HEADER - 0.6f, lineWidth / 2f, 0.6f,
                    ColorUtil.withAlpha(accent, 0), ColorUtil.withAlpha(accent, 0x90));
            RenderUtil.horizontalGradient(matrices, lineX + lineWidth / 2f, y + HEADER - 0.6f, lineWidth / 2f, 0.6f,
                    ColorUtil.withAlpha(accent, 0x90), ColorUtil.withAlpha(accent, 0));
        }
    }

    private void renderScrollbar(MatrixStack matrices, float content, float scrollOffset) {
        if (maxScroll <= 0f || content <= 0f) return;
        float trackHeight = bodyHeight - 4f;
        float thumbHeight = Math.max(12f, trackHeight * (bodyHeight / content));
        float thumbY = bodyTop + 2f + (trackHeight - thumbHeight) * (scrollOffset / maxScroll);
        RenderUtil.roundedRect(matrices, x + WIDTH - 3f, thumbY, 1.5f, thumbHeight, 0.75f,
                ColorUtil.withAlpha(Theme.accent(), 0x80));
    }

    private void clampToScreen() {
        float minX = screen.getVirtualLeft();
        float maxX = screen.getVirtualRight() - WIDTH;
        float minY = screen.getVirtualTop();
        float maxY = screen.getVirtualBottom() - HEADER;
        x = Math.max(minX, Math.min(maxX, x));
        y = Math.max(minY, Math.min(maxY, y));
    }

    private boolean isOverHeader(double mouseX, double mouseY) {
        return Component.isHovered(mouseX, mouseY, x, y, WIDTH, HEADER);
    }

    private boolean isInBody(double mouseX, double mouseY) {
        return Component.isHovered(mouseX, mouseY, x, bodyTop, WIDTH, bodyHeight);
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return isOverHeader(mouseX, mouseY) || isInBody(mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isOverHeader(mouseX, mouseY)) {
            if (button == 0) {
                dragging = true;
                dragOffsetX = (float) mouseX - x;
                dragOffsetY = (float) mouseY - y;
            } else if (button == 1) {
                expanded = !expanded;
            }
            return true;
        }
        if (expanded && isInBody(mouseX, mouseY)) {
            for (ModuleButton moduleButton : visibleButtons()) {
                if (moduleButton.mouseClicked(mouseX, mouseY, button)) return true;
            }
            return true;
        }
        // Clicked elsewhere: still forward so key binders waiting for input lose focus.
        for (ModuleButton moduleButton : buttons) {
            if (moduleButton.isListening()) moduleButton.mouseClicked(-1, -1, button);
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
        for (ModuleButton moduleButton : buttons) moduleButton.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!expanded || !isMouseOver(mouseX, mouseY) || maxScroll <= 0f) return false;
        scrollTarget = (float) Math.max(0f, Math.min(maxScroll, scrollTarget - amount * 22f));
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ModuleButton button : visibleButtons()) {
            if (button.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    public boolean isListening() {
        for (ModuleButton button : buttons) if (button.isListening()) return true;
        return false;
    }

    /** Description of the module row under the cursor, if any. */
    public Module getHoveredModule(double mouseX, double mouseY) {
        if (!expanded || !isInBody(mouseX, mouseY)) return null;
        for (ModuleButton button : visibleButtons()) if (button.isHoveredRow()) return button.getModule();
        return null;
    }

    public boolean isDragging() {
        return dragging;
    }

    public JsonObject save() {
        JsonObject object = new JsonObject();
        object.addProperty("x", x);
        object.addProperty("y", y);
        object.addProperty("expanded", expanded);
        return object;
    }

    public void load(JsonObject object) {
        if (object.has("x")) x = object.get("x").getAsFloat();
        if (object.has("y")) y = object.get("y").getAsFloat();
        if (object.has("expanded")) {
            expanded = object.get("expanded").getAsBoolean();
            open.set(expanded ? 1 : 0);
        }
    }
}
