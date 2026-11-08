package dev.wave.gui;

import com.google.gson.JsonObject;
import dev.wave.WaveClient;
import dev.wave.gui.components.BindComponent;
import dev.wave.module.Category;
import dev.wave.module.client.ClickGUI;
import dev.wave.util.Animation;
import dev.wave.util.ColorUtil;
import dev.wave.util.Easing;
import dev.wave.util.RenderUtil;
import dev.wave.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {
    private static ClickGuiScreen instance;
    private static JsonObject pendingState;
    public static BindComponent listening;

    private final List<Panel> panels = new ArrayList<>();
    private final Animation openAnim = new Animation(10, 0);
    private final Animation scroll = new Animation(14, 0);
    private final Animation searchFocus = new Animation(16, 0);
    private final Animation tooltipAnim = new Animation(14, 0);
    private final Animation hudButtonHover = new Animation(18, 0);
    private boolean closing;
    private String search = "";
    private boolean searchFocused;
    private String lastTooltip = "";
    private boolean laidOut;

    private ClickGuiScreen() {
        super(Text.literal("WaveClient"));
        float px = 20;
        for (Category c : Category.values()) {
            panels.add(new Panel(c, px, 42));
            px += Panel.WIDTH + 10;
        }
        if (pendingState != null) applyState(pendingState);
    }

    public static ClickGuiScreen get() {
        if (instance == null) instance = new ClickGuiScreen();
        instance.closing = false;
        instance.openAnim.set(0);
        instance.openAnim.setTarget(1);
        return instance;
    }

    private static ClickGUI settings() { return WaveClient.INSTANCE.modules.get(ClickGUI.class); }

    public static void click(float pitch) {
        if (settings().sounds.isOn())
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.25f));
    }

    /** True when no panel is being dragged and no bind is being captured. */
    public static boolean isMouseFree() {
        if (instance == null) return true;
        for (Panel p : instance.panels) if (p.isDragging()) return false;
        return true;
    }

    // ------------------------------------------------------------------ state

    public static JsonObject saveState() {
        JsonObject o = new JsonObject();
        if (instance == null) return pendingState != null ? pendingState : o;
        for (Panel p : instance.panels) {
            JsonObject po = new JsonObject();
            po.addProperty("x", p.x);
            po.addProperty("y", p.y);
            po.addProperty("open", p.open);
            o.add(p.category.name(), po);
        }
        return o;
    }

    public static void loadState(JsonObject o) {
        pendingState = o;
        if (instance != null) instance.applyState(o);
    }

    private void applyState(JsonObject o) {
        for (Panel p : panels) {
            if (!o.has(p.category.name())) continue;
            JsonObject po = o.getAsJsonObject(p.category.name());
            p.x = po.get("x").getAsFloat();
            p.y = po.get("y").getAsFloat();
            p.setOpenInstant(po.get("open").getAsBoolean());
            laidOut = true;
        }
    }

    // ------------------------------------------------------------------ screen

    @Override
    protected void init() {
        if (!laidOut) {
            // centre the panels horizontally on first open
            float total = panels.size() * Panel.WIDTH + (panels.size() - 1) * 10;
            float px = Math.max(8, (width - total) / 2f);
            for (Panel p : panels) {
                p.x = px;
                p.y = 44;
                px += Panel.WIDTH + 10;
            }
            laidOut = true;
        }
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    private float eased() { return closing ? Easing.inCubic(openAnim.get()) : Easing.outBack(openAnim.get()); }

    private float scale() {
        return settings().animation.is("Scale") ? 0.8f + 0.2f * eased() : 1f;
    }

    private float slideY() {
        return settings().animation.is("Slide") ? (1 - eased()) * -height * 0.35f : 0;
    }

    private int tx(double mx) { float s = scale(); return (int) ((mx - width / 2f) / s + width / 2f); }
    private int ty(double my) { float s = scale(); return (int) ((my - height / 2f - slideY()) / s + height / 2f); }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        float a = openAnim.update();
        if (closing && a <= 0.02f) {
            closing = false;
            MinecraftClient.getInstance().setScreen(null);
            return;
        }
        float fade = Easing.clamp01(a);
        ClickGUI cfg = settings();

        // background: blur + vertical vignette
        if (cfg.blur.isOn() && client != null && client.world != null && fade > 0.15f) applyBlur();
        int dim = (int) (cfg.dim.get() * 255 * fade);
        c.fillGradient(0, 0, width, height, ColorUtil.withAlpha(0, (int) (dim * 0.75f)), ColorUtil.withAlpha(0, dim));

        RenderUtil.globalAlpha = fade;
        int mx = tx(mouseX), my = ty(mouseY);

        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(width / 2f, height / 2f + slideY(), 0);
        float s = scale();
        m.scale(s, s, 1);
        m.translate(-width / 2f, -height / 2f, 0);

        scroll.update();
        float sc = scroll.get();
        Panel top = null;
        for (Panel p : panels) {
            if (p.isDragging()) { top = p; continue; }
            p.render(c, mx, my, sc, search);
        }
        if (top != null) top.render(c, mx, my, sc, search);
        m.pop();

        renderTopBar(c, mouseX, mouseY);
        renderBottomBar(c, mouseX, mouseY);
        renderTooltip(c, mx, my);
        RenderUtil.globalAlpha = 1f;

        WaveClient.INSTANCE.notifications.render(c, width, height);
    }

    private void renderTopBar(DrawContext c, int mx, int my) {
        float a = Easing.outCubic(openAnim.get());
        float barY = 8 - (1 - a) * 20;
        // logo
        String logo = "§lWAVE";
        RenderUtil.round(c, 8, barY, RenderUtil.width(logo) + 10, 16, 4, Theme.accent());
        RenderUtil.text(c, logo, 13, barY + 4, Theme.onAccent(), false);
        RenderUtil.text(c, "Client §8v" + WaveClient.VERSION, RenderUtil.width(logo) + 23, barY + 4, Theme.TEXT, true);

        // search box
        searchFocus.setTarget(searchFocused || !search.isEmpty() ? 1 : 0);
        float f = searchFocus.update();
        float sw = 130 + 40 * f, sh = 16;
        float sx = width / 2f - sw / 2, sy = barY;
        RenderUtil.round(c, sx, sy, sw, sh, 8, ColorUtil.lerp(0xE0101010, 0xF0161616, f));
        RenderUtil.roundOutline(c, sx, sy, sw, sh, 8, 0.5f, ColorUtil.lerp(Theme.BORDER, ColorUtil.alpha(Theme.accent(), 0.8f), f));
        // magnifier icon
        RenderUtil.roundOutline(c, sx + 7, sy + 4, 6, 6, 3, 0.8f, Theme.TEXT_DIM);
        RenderUtil.rect(c, sx + 12, sy + 9.5f, 2.5f, 1, Theme.TEXT_DIM);
        if (search.isEmpty() && !searchFocused) {
            RenderUtil.text(c, "Search modules...", sx + 18, sy + 4, Theme.TEXT_DIM, false);
        } else {
            String shown = search;
            while (RenderUtil.width(shown) > sw - 30 && shown.length() > 0) shown = shown.substring(1);
            RenderUtil.text(c, shown, sx + 18, sy + 4, Theme.TEXT, false);
            if (searchFocused && System.currentTimeMillis() / 500 % 2 == 0)
                RenderUtil.rect(c, sx + 19 + RenderUtil.width(shown), sy + 3.5f, 0.5f, 9, Theme.TEXT);
        }

        // hint on the right
        String hint = "§7LMB §ftoggle  §7RMB §fsettings  §7MMB §fbind";
        RenderUtil.text(c, hint, width - 8 - RenderUtil.width(hint), barY + 4, Theme.TEXT, true);
    }

    private float[] hudButton() {
        float a = Easing.outCubic(openAnim.get());
        float bw = 84, bh = 18;
        return new float[]{width / 2f - bw / 2, height - 26 + (1 - a) * 30, bw, bh};
    }

    private void renderBottomBar(DrawContext c, int mx, int my) {
        float[] b = hudButton();
        boolean hov = RenderUtil.hovered(mx, my, b[0], b[1], b[2], b[3]);
        hudButtonHover.setTarget(hov ? 1 : 0);
        float h = hudButtonHover.update();
        RenderUtil.round(c, b[0], b[1], b[2], b[3], 9, ColorUtil.lerp(0xF0101010, Theme.accent(), h));
        RenderUtil.roundOutline(c, b[0], b[1], b[2], b[3], 9, 0.5f, Theme.BORDER);
        String t = "▣ HUD Editor";
        RenderUtil.text(c, t, b[0] + b[2] / 2 - RenderUtil.width(t) / 2f, b[1] + 5, ColorUtil.lerp(Theme.TEXT, Theme.onAccent(), h), false);
    }

    private void renderTooltip(DrawContext c, int mx, int my) {
        String tip = null;
        if (settings().tooltips.isOn() && isMouseFree()) {
            for (Panel p : panels) {
                if (!p.isOver(mx, my)) continue;
                tip = p.tooltip(mx, my);
                if (tip != null) break;
            }
        }
        tooltipAnim.setTarget(tip != null ? 1 : 0);
        float t = tooltipAnim.update();
        if (tip != null) lastTooltip = tip;
        if (t < 0.02f || lastTooltip.isEmpty()) return;
        float prev = RenderUtil.globalAlpha;
        RenderUtil.globalAlpha = prev * t;
        float tw = RenderUtil.width(lastTooltip) + 14;
        float txp = width / 2f - tw / 2, typ = height - 50 + (1 - Easing.outCubic(t)) * 6;
        RenderUtil.round(c, txp, typ, tw, 16, 5, 0xF0101010);
        RenderUtil.roundOutline(c, txp, typ, tw, 16, 5, 0.5f, Theme.BORDER);
        RenderUtil.text(c, lastTooltip, txp + 7, typ + 4, Theme.TEXT, false);
        RenderUtil.globalAlpha = prev;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return false;
        // search box (screen space)
        float a = Easing.outCubic(openAnim.get());
        float sw = 130 + 40 * searchFocus.get(), sx = width / 2f - sw / 2, sy = 8 - (1 - a) * 20;
        if (listening == null && RenderUtil.hovered(mouseX, mouseY, sx, sy, sw, 16)) {
            searchFocused = true;
            if (button == 1) search = "";
            return true;
        }
        searchFocused = false;
        float[] b = hudButton();
        if (listening == null && button == 0 && RenderUtil.hovered(mouseX, mouseY, b[0], b[1], b[2], b[3])) {
            click(1.2f);
            MinecraftClient.getInstance().setScreen(new HudEditorScreen(this));
            return true;
        }

        int mx = tx(mouseX), my = ty(mouseY);
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            if (p.mouseClicked(mx, my, button)) {
                if (p.isDragging()) {
                    panels.remove(i);
                    panels.add(p);
                }
                return true;
            }
        }
        if (listening != null) {
            listening.mouseClicked(mx, my, button);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        int mx = tx(mouseX), my = ty(mouseY);
        for (Panel p : panels) p.mouseReleased(mx, my, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int mx = tx(mouseX), my = ty(mouseY);
        for (Panel p : panels) if (p.isOver(mx, my) && p.mouseScrolled(mx, my, verticalAmount)) return true;
        scroll.setTarget(Math.min(0, scroll.getTarget() + verticalAmount * 24));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening != null) {
            listening.keyPressed(keyCode);
            return true;
        }
        if (searchFocused) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    if (!search.isEmpty()) search = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0 ? "" : search.substring(0, search.length() - 1);
                    return true;
                }
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_ESCAPE -> {
                    searchFocused = false;
                    if (keyCode == GLFW.GLFW_KEY_ESCAPE) search = "";
                    return true;
                }
                default -> {}
            }
        }
        if (keyCode == GLFW.GLFW_KEY_F && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            searchFocused = true;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && !search.isEmpty()) {
            search = "";
            return true;
        }
        if (keyCode == settings().getKey() || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (listening != null) return true;
        if (Character.isLetterOrDigit(chr) || chr == ' ') {
            if (!searchFocused && chr == ' ') return false;
            searchFocused = true;
            if (search.length() < 24) search += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        if (closing) return;
        closing = true;
        searchFocused = false;
        listening = null;
        openAnim.setTarget(0);
        WaveClient.INSTANCE.config.save();
    }

    @Override
    public void removed() {
        listening = null;
        for (Panel p : panels) p.mouseReleased(0, 0, 0);
        WaveClient.INSTANCE.config.save();
    }
}
