package tech.fuzir.client.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import tech.fuzir.client.FuzirClient;
import tech.fuzir.client.gui.component.Component;
import tech.fuzir.client.gui.panel.Panel;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.module.ModuleManager;
import tech.fuzir.client.module.misc.ClickGuiModule;
import tech.fuzir.client.render.Blur;
import tech.fuzir.client.render.Fonts;
import tech.fuzir.client.render.RenderUtil;
import tech.fuzir.client.render.Scissor;
import tech.fuzir.client.util.ColorUtil;
import tech.fuzir.client.util.animation.Animation;
import tech.fuzir.client.util.animation.Easing;

import java.util.ArrayList;
import java.util.List;

/**
 * Glass ClickGUI. Panels live in a "virtual" coordinate space that is uniformly scaled around the screen
 * center (user scale × open/close pop), so mouse input is mapped through {@link #toVirtual}.
 */
public final class ClickGuiScreen extends Screen {
    private static final float BAR_HEIGHT = 18f;
    private static final float SEARCH_WIDTH = 150f;
    private static final float HUD_BUTTON_WIDTH = 82f;
    private static final float MIN_SCALE = 0.6f;
    private static final float MAX_SCALE = 1.6f;

    private final ClickGuiModule settings;
    private final List<Panel> panels = new ArrayList<>();
    private final Animation openAnimation = new Animation(Easing.OUT_CUBIC, 260);
    private final Animation searchFocus = new Animation(Easing.OUT_CUBIC, 180);
    private final Animation hudButtonHover = new Animation(Easing.OUT_QUAD, 150);
    private final Animation description = new Animation(Easing.OUT_QUAD, 160);
    private final StringBuilder search = new StringBuilder();
    private boolean searchFocused;
    private boolean closing;
    private float scale = 1f;
    private String lastDescription = "";

    public ClickGuiScreen(ModuleManager modules, JsonObject savedLayout) {
        super(Text.literal("Fuzir ClickGUI"));
        this.settings = modules.get(ClickGuiModule.class);
        float panelX = 16f;
        for (Category category : Category.values()) {
            List<Module> list = modules.getModules(category);
            if (list.isEmpty()) continue;
            Panel panel = new Panel(this, category, list, panelX, 16f);
            if (savedLayout != null) {
                JsonElement state = savedLayout.get(category.name());
                if (state != null && state.isJsonObject()) panel.load(state.getAsJsonObject());
            }
            panels.add(panel);
            panelX += Panel.WIDTH + 8f;
        }
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void init() {
        if (!closing) {
            openAnimation.set(0).animateTo(1);
        }
        closing = false;
    }

    @Override
    public void close() {
        if (closing) return;
        closing = true;
        searchFocused = false;
        openAnimation.animateTo(0);
    }

    @Override
    public void removed() {
        FuzirClient.get().getConfigManager().save();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ------------------------------------------------------------------ coordinate space

    private float baseScale() {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, settings.scale.getFloat()));
    }

    private float toVirtualX(double mouseX) {
        return (float) ((mouseX - width / 2.0) / scale + width / 2.0);
    }

    private float toVirtualY(double mouseY) {
        return (float) ((mouseY - height / 2.0) / scale + height / 2.0);
    }

    public float getVirtualLeft() {
        return width / 2f - width / (2f * baseScale());
    }

    public float getVirtualRight() {
        return width / 2f + width / (2f * baseScale());
    }

    public float getVirtualTop() {
        return height / 2f - height / (2f * baseScale());
    }

    public float getVirtualBottom() {
        return height / 2f + height / (2f * baseScale()) - (BAR_HEIGHT + 14f) / baseScale();
    }

    public String getSearch() {
        return search.toString();
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float progress = openAnimation.getFloat();
        if (closing && progress <= 0.01f) {
            closing = false;
            if (client != null) client.setScreen(null);
            return;
        }

        renderBackground(context, progress, delta);

        scale = baseScale() * (0.9f + 0.1f * (float) Easing.OUT_BACK.apply(progress));
        double vx = toVirtualX(mouseX);
        double vy = toVirtualY(mouseY);

        RenderSystem.setShaderColor(1f, 1f, 1f, progress);
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(width / 2f, height / 2f, 0f);
        matrices.scale(scale, scale, 1f);
        matrices.translate(-width / 2f, -height / 2f, 0f);
        Scissor.setTransform(scale, width / 2f, height / 2f);

        boolean blocked = closing;
        for (Panel panel : panels) {
            panel.render(context, blocked ? -1 : vx, blocked ? -1 : vy, delta);
        }

        Scissor.resetTransform();
        matrices.pop();

        renderBottomBar(context, mouseX, mouseY, progress);
        renderDescription(context, vx, vy, progress);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void renderBackground(DrawContext context, float progress, float delta) {
        if (client != null && client.world != null && settings.blur.isEnabled()) {
            Blur.render(settings.blurStrength.getFloat() * progress, delta);
        }
        MatrixStack matrices = context.getMatrices();
        int alpha = (int) ((client != null && client.world != null ? 0x70 : 0xD0) * progress);
        RenderUtil.rect(matrices, 0, 0, width, height, ColorUtil.withAlpha(Theme.OVERLAY, alpha));
        // Subtle accent vignette rising from the bottom edge.
        int glow = ColorUtil.withAlpha(Theme.accent(), (int) (0x16 * progress));
        RenderUtil.roundedGradient(matrices, 0, height * 0.55f, width, height * 0.45f, 0f,
                ColorUtil.withAlpha(glow, 0), glow, false);
    }

    private float barY() {
        return height - BAR_HEIGHT - 10f;
    }

    private float searchX() {
        return (width - SEARCH_WIDTH - HUD_BUTTON_WIDTH - 6f) / 2f;
    }

    private float hudButtonX() {
        return searchX() + SEARCH_WIDTH + 6f;
    }

    private void renderBottomBar(DrawContext context, int mouseX, int mouseY, float progress) {
        MatrixStack matrices = context.getMatrices();
        float slide = (1f - progress) * 20f;
        float y = barY() + slide;
        int accent = Theme.accent();

        float focus = searchFocus.animateTo(searchFocused || search.length() > 0).getFloat();
        float sx = searchX();
        RenderUtil.shadow(matrices, sx, y, SEARCH_WIDTH, BAR_HEIGHT, 5f, 8f, 0x80000000);
        RenderUtil.roundedRect(matrices, sx, y, SEARCH_WIDTH, BAR_HEIGHT, 5f, Theme.PANEL);
        RenderUtil.roundedOutline(matrices, sx, y, SEARCH_WIDTH, BAR_HEIGHT, 5f, 0.6f,
                ColorUtil.interpolate(Theme.BORDER, ColorUtil.withAlpha(accent, 0x90), focus));
        float textY = y + (BAR_HEIGHT - Fonts.HEIGHT) / 2f;
        Fonts.draw(context, "⌕", sx + 7f, textY, ColorUtil.interpolate(Theme.TEXT_MUTED, accent, focus));
        float textX = sx + 18f;
        if (search.length() == 0) {
            Fonts.draw(context, "Search modules...", textX, textY, Theme.TEXT_MUTED);
        } else {
            String text = search.toString();
            float maxWidth = SEARCH_WIDTH - 26f;
            while (Fonts.width(text) > maxWidth && text.length() > 1) text = text.substring(1);
            Fonts.draw(context, text, textX, textY, Theme.TEXT);
            textX += Fonts.width(text);
        }
        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            RenderUtil.rect(matrices, textX + 1f, y + 5f, 0.8f, BAR_HEIGHT - 10f, accent);
        }

        float hx = hudButtonX();
        boolean hovered = Component.isHovered(mouseX, mouseY, hx, y, HUD_BUTTON_WIDTH, BAR_HEIGHT);
        float hover = hudButtonHover.animateTo(hovered).getFloat();
        RenderUtil.shadow(matrices, hx, y, HUD_BUTTON_WIDTH, BAR_HEIGHT, 5f, 8f,
                ColorUtil.interpolate(0x80000000, ColorUtil.withAlpha(accent, 0x50), hover));
        RenderUtil.roundedRect(matrices, hx, y, HUD_BUTTON_WIDTH, BAR_HEIGHT, 5f,
                ColorUtil.interpolate(Theme.PANEL, accent, hover));
        RenderUtil.roundedOutline(matrices, hx, y, HUD_BUTTON_WIDTH, BAR_HEIGHT, 5f, 0.6f, Theme.BORDER);
        String label = "▣  HUD Editor";
        Fonts.drawCentered(context, label, hx + HUD_BUTTON_WIDTH / 2f, textY,
                ColorUtil.interpolate(Theme.TEXT, Theme.onAccent(), hover), Fonts.SEMIBOLD);
    }

    private void renderDescription(DrawContext context, double vx, double vy, float progress) {
        Module hovered = null;
        if (settings.descriptions.isEnabled() && !closing) {
            for (int i = panels.size() - 1; i >= 0 && hovered == null; i--) {
                Panel panel = panels.get(i);
                if (panel.isDragging()) break;
                if (panel.isMouseOver(vx, vy)) {
                    hovered = panel.getHoveredModule(vx, vy);
                    break;
                }
            }
        }
        if (hovered != null) lastDescription = hovered.getName() + "  " + Theme.SEPARATOR_GLYPH + "  " + hovered.getDescription();
        float alpha = description.animateTo(hovered != null).getFloat() * progress;
        if (alpha <= 0.02f || lastDescription.isEmpty()) return;

        MatrixStack matrices = context.getMatrices();
        float textWidth = Fonts.width(lastDescription);
        float w = textWidth + 16f;
        float h = 16f;
        float x = (width - w) / 2f;
        float y = barY() - h - 6f + (1f - alpha) * 4f;
        RenderUtil.roundedRect(matrices, x, y, w, h, 4f, ColorUtil.multiplyAlpha(Theme.PANEL, alpha));
        RenderUtil.roundedOutline(matrices, x, y, w, h, 4f, 0.6f, ColorUtil.multiplyAlpha(Theme.BORDER, alpha));
        Fonts.draw(context, lastDescription, x + 8f, y + (h - Fonts.HEIGHT) / 2f, ColorUtil.multiplyAlpha(Theme.TEXT_DIM, alpha));
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return false;
        float y = barY();
        if (Component.isHovered(mouseX, mouseY, hudButtonX(), y, HUD_BUTTON_WIDTH, BAR_HEIGHT) && button == 0) {
            if (client != null) client.setScreen(new HudEditorScreen(this));
            return true;
        }
        searchFocused = Component.isHovered(mouseX, mouseY, searchX(), y, SEARCH_WIDTH, BAR_HEIGHT);
        if (searchFocused) {
            if (button == 1) search.setLength(0);
            return true;
        }

        double vx = toVirtualX(mouseX);
        double vy = toVirtualY(mouseY);
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel panel = panels.get(i);
            if (panel.mouseClicked(vx, vy, button)) {
                // Bring the interacted panel to the front.
                panels.remove(i);
                panels.add(panel);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double vx = toVirtualX(mouseX);
        double vy = toVirtualY(mouseY);
        for (Panel panel : panels) panel.mouseReleased(vx, vy, button);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (closing) return false;
        if (hasControlDown()) {
            settings.scale.set(settings.scale.get() + Math.signum(amount) * settings.scale.getStep());
            return true;
        }
        double vx = toVirtualX(mouseX);
        double vy = toVirtualY(mouseY);
        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).mouseScrolled(vx, vy, amount)) return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (closing) return true;
        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (search.length() > 0) {
                    if (hasControlDown()) search.setLength(0);
                    else search.deleteCharAt(search.length() - 1);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) search.setLength(0);
                return true;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || settings.getBind().matches(keyCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (closing || anyListening()) return false;
        if (Character.isISOControl(chr)) return false;
        if (!searchFocused && Character.isWhitespace(chr)) return false;
        searchFocused = true;
        if (search.length() < 32) search.append(chr);
        return true;
    }

    private boolean anyListening() {
        for (Panel panel : panels) if (panel.isListening()) return true;
        return false;
    }

    /** Persisted panel layout keyed by category. */
    public JsonObject saveLayout() {
        JsonObject object = new JsonObject();
        for (Panel panel : panels) object.add(panel.getCategory().name(), panel.save());
        return object;
    }
}
