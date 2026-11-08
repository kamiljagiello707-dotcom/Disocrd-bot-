package dev.wave.gui;

import com.google.gson.JsonObject;
import dev.wave.WaveClient;
import dev.wave.gui.components.BindComponent;
import dev.wave.module.Category;
import dev.wave.module.Module;
import dev.wave.module.client.ClickGUI;
import dev.wave.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single-window ClickGUI: category sidebar with icons, module card grid, settings page,
 * search, HUD editor shortcut. Everything is animated.
 */
public class ClickGuiScreen extends Screen {
    private static ClickGuiScreen instance;
    private static JsonObject pendingState;
    public static BindComponent listening;

    private static final float SIDEBAR = 124, CAT_H = 22, GAP = 6;

    private final Map<Module, ModuleCard> cards = new LinkedHashMap<>();
    private final Map<Category, Animation> catHover = new LinkedHashMap<>();
    private final Animation openAnim = new Animation(10, 0);
    private final Animation indicator = new Animation(16, 0);
    private final Animation scroll = new Animation(16, 0);
    private final Animation settingsAnim = new Animation(13, 0);
    private final Animation searchFocus = new Animation(16, 0);
    private final Animation hudHover = new Animation(18, 0);
    private final Animation tipAnim = new Animation(14, 0);

    private Category selected = Category.VISUALS;
    private SettingsView settings, lastSettings;
    private String search = "";
    private boolean searchFocused, closing, dragging;
    private float offX, offY, dragX, dragY;
    private float wx, wy, ww, wh;
    private float contentHeight;
    private String tip = "";
    private long tipSince;
    private Object tipOwner;

    private ClickGuiScreen() {
        super(Text.literal("WaveClient"));
        for (Module m : WaveClient.INSTANCE.modules.getModules()) cards.put(m, new ModuleCard(m));
        for (Category c : Category.values()) catHover.put(c, new Animation(18, 0));
        if (pendingState != null) applyState(pendingState);
        indicator.set(selected.ordinal());
    }

    public static ClickGuiScreen get() {
        if (instance == null) instance = new ClickGuiScreen();
        instance.closing = false;
        instance.openAnim.set(0);
        instance.openAnim.setTarget(1);
        instance.resetCards();
        return instance;
    }

    private static ClickGUI cfg() { return WaveClient.INSTANCE.modules.get(ClickGUI.class); }

    public static void click(float pitch) {
        if (cfg().sounds.isOn())
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.25f));
    }

    public static boolean isMouseFree() { return instance == null || !instance.dragging; }

    // ------------------------------------------------------------------ state

    public static JsonObject saveState() {
        if (instance == null) return pendingState != null ? pendingState : new JsonObject();
        JsonObject o = new JsonObject();
        o.addProperty("category", instance.selected.name());
        o.addProperty("offX", instance.offX);
        o.addProperty("offY", instance.offY);
        return o;
    }

    public static void loadState(JsonObject o) {
        pendingState = o;
        if (instance != null) instance.applyState(o);
    }

    private void applyState(JsonObject o) {
        try {
            if (o.has("category")) selected = Category.valueOf(o.get("category").getAsString());
            if (o.has("offX")) offX = o.get("offX").getAsFloat();
            if (o.has("offY")) offY = o.get("offY").getAsFloat();
        } catch (Exception ignored) {}
    }

    // ------------------------------------------------------------------ helpers

    private List<ModuleCard> visibleCards() {
        List<ModuleCard> list = new ArrayList<>();
        String q = search.toLowerCase();
        for (ModuleCard c : cards.values()) {
            if (q.isEmpty() ? c.module.getCategory() == selected : c.module.getName().toLowerCase().contains(q)) list.add(c);
        }
        return list;
    }

    private void resetCards() {
        int i = 0;
        for (ModuleCard c : visibleCards()) c.resetAppear(i++);
        scroll.set(0);
    }

    void select(Category c) {
        if (c == selected && settings == null && search.isEmpty()) return;
        selected = c;
        search = "";
        closeSettings();
        resetCards();
        click(1.15f);
    }

    void openSettings(Module m) {
        settings = new SettingsView(m);
        lastSettings = settings;
        settingsAnim.setTarget(1);
        scroll.set(0);
        click(1.2f);
    }

    private void closeSettings() {
        if (settings == null) return;
        settings = null;
        listening = null;
        settingsAnim.setTarget(0);
        scroll.set(0);
    }

    private void layout() {
        ww = Math.min(540, width - 20);
        wh = Math.min(330, height - 20);
        offX = Math.max(-(width - ww) / 2f, Math.min((width - ww) / 2f, offX));
        offY = Math.max(-(height - wh) / 2f, Math.min((height - wh) / 2f, offY));
        wx = (width - ww) / 2f + offX;
        wy = (height - wh) / 2f + offY;
    }

    private float eased() { return closing ? Easing.inCubic(openAnim.get()) : Easing.outBack(openAnim.get()); }
    private float scale() { return cfg().animation.is("Scale") ? 0.85f + 0.15f * eased() : 1f; }
    private float slideY() { return cfg().animation.is("Slide") ? (1 - eased()) * height * 0.3f : 0; }
    private int tx(double mx) { return (int) ((mx - width / 2f) / scale() + width / 2f); }
    private int ty(double my) { return (int) ((my - height / 2f - slideY()) / scale() + height / 2f); }

    private float mainX() { return wx + SIDEBAR + 14; }
    private float mainW() { return ww - SIDEBAR - 28; }
    private float contentTop() { return wy + 48; }
    private float contentH() { return wh - 48 - 12; }
    private float[] searchBox() {
        float sw = 110 + 30 * searchFocus.get();
        return new float[]{mainX() + mainW() - sw, wy + 14, sw, 18};
    }
    private float[] hudButton() { return new float[]{wx + 8, wy + wh - 62, SIDEBAR - 16, 20}; }

    // ------------------------------------------------------------------ render

    @Override protected void init() {}
    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        float a = openAnim.update();
        if (closing && a <= 0.02f) {
            closing = false;
            MinecraftClient.getInstance().setScreen(null);
            return;
        }
        float fade = Easing.clamp01(a);
        if (cfg().blur.isOn() && client != null && client.world != null && fade > 0.15f) applyBlur();
        int dim = (int) (cfg().dim.get() * 255 * fade);
        c.fillGradient(0, 0, width, height, ColorUtil.withAlpha(0, (int) (dim * 0.7f)), ColorUtil.withAlpha(0, dim));

        int mx = tx(mouseX), my = ty(mouseY);
        if (dragging) {
            offX = mx - dragX;
            offY = my - dragY;
        }
        layout();

        RenderUtil.globalAlpha = fade;
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(width / 2f, height / 2f + slideY(), 0);
        float s = scale();
        m.scale(s, s, 1);
        m.translate(-width / 2f, -height / 2f, 0);

        // shadow + window
        RenderUtil.round(c, wx - 8, wy - 6, ww + 16, wh + 16, 16, 0x26000000);
        RenderUtil.round(c, wx - 4, wy - 2, ww + 8, wh + 8, 13, 0x40000000);
        RenderUtil.round(c, wx, wy, ww, wh, 10, 0xF70B0B0B);
        RenderUtil.roundOutline(c, wx, wy, ww, wh, 10, 0.5f, 0x26FFFFFF);

        renderSidebar(c, mx, my);
        renderMain(c, mx, my);
        renderTooltip(c, mx, my);
        m.pop();
        RenderUtil.globalAlpha = 1f;

        WaveClient.INSTANCE.notifications.render(c, width, height);
    }

    private void renderSidebar(DrawContext c, int mx, int my) {
        RenderUtil.round(c, wx + 0.5f, wy + 0.5f, SIDEBAR, wh - 1, 10, 0xFF070707);
        RenderUtil.rect(c, wx + SIDEBAR - 10, wy + 0.5f, 10.5f, wh - 1, 0xFF070707);
        RenderUtil.rect(c, wx + SIDEBAR, wy + 10, 0.5f, wh - 20, 0x1EFFFFFF);

        // logo
        RenderUtil.round(c, wx + 12, wy + 12, 24, 24, 7, Theme.accent());
        Icons.draw(c, "watermark", wx + 16, wy + 16, 16, Theme.onAccent());
        RenderUtil.text(c, "§lWaveClient", wx + 42, wy + 14, Theme.TEXT);
        RenderUtil.textScaled(c, "v" + WaveClient.VERSION + "  •  1.21.4", wx + 42, wy + 26, 0.75f, Theme.TEXT_DIM, false);

        RenderUtil.textScaled(c, "CATEGORIES", wx + 14, wy + 50, 0.65f, 0xFF5A5A5A, false);
        float y0 = wy + 58;
        indicator.setTarget(selected.ordinal());
        float ind = indicator.update();
        boolean searching = !search.isEmpty();
        float indAlpha = searching ? 0.25f : 1f;
        RenderUtil.round(c, wx + 8, y0 + ind * (CAT_H + 2), SIDEBAR - 16, CAT_H, 6, ColorUtil.alpha(Theme.accent(), indAlpha));

        for (Category cat : Category.values()) {
            float cy = y0 + cat.ordinal() * (CAT_H + 2);
            boolean hov = RenderUtil.hovered(mx, my, wx + 8, cy, SIDEBAR - 16, CAT_H);
            Animation h = catHover.get(cat);
            h.setTarget(hov ? 1 : 0);
            float hv = h.update();
            float sel = Math.max(0, 1 - Math.abs(ind - cat.ordinal())) * indAlpha;
            if (hv > 0.01f && sel < 0.5f) RenderUtil.round(c, wx + 8, cy, SIDEBAR - 16, CAT_H, 6, ColorUtil.withAlpha(0xFFFFFF, (int) (14 * hv)));
            int col = ColorUtil.lerp(ColorUtil.lerp(0xFF7A7A7A, 0xFFE0E0E0, hv), Theme.onAccent(), sel);
            Icons.draw(c, cat.name(), wx + 16 + hv * 1.5f, cy + 5, 12, col);
            RenderUtil.text(c, cat.displayName, wx + 34 + hv * 1.5f, cy + 7, col);
            long on = WaveClient.INSTANCE.modules.getByCategory(cat).stream().filter(Module::isEnabled).count();
            if (on > 0) {
                String n = String.valueOf(on);
                float nw = RenderUtil.width(n) * 0.75f + 6;
                float nx = wx + SIDEBAR - 14 - nw;
                RenderUtil.round(c, nx, cy + 6, nw, 10, 3, ColorUtil.lerp(0xFF1E1E1E, ColorUtil.withAlpha(Theme.onAccent(), 0x30), sel));
                RenderUtil.textScaled(c, n, nx + 3, cy + 8, 0.75f, col, false);
            }
        }

        // HUD editor button
        float[] b = hudButton();
        hudHover.setTarget(RenderUtil.hovered(mx, my, b[0], b[1], b[2], b[3]) ? 1 : 0);
        float hh = hudHover.update();
        RenderUtil.round(c, b[0], b[1], b[2], b[3], 6, ColorUtil.lerp(0xFF121212, 0xFF1E1E1E, hh));
        RenderUtil.roundOutline(c, b[0], b[1], b[2], b[3], 6, 0.5f, ColorUtil.lerp(0x1EFFFFFF, 0x50FFFFFF, hh));
        Icons.draw(c, "hudeditor", b[0] + 8, b[1] + 5, 10, Theme.TEXT);
        RenderUtil.text(c, "HUD Editor", b[0] + 24, b[1] + 6, Theme.TEXT);
        Icons.draw(c, "arrow", b[2] + b[0] - 14 + hh * 2, b[1] + 6, 8, Theme.TEXT_DIM);

        // profile
        float py = wy + wh - 34;
        RenderUtil.rect(c, wx + 12, py - 4, SIDEBAR - 24, 0.5f, 0x16FFFFFF);
        RenderUtil.round(c, wx + 12, py + 2, 20, 20, 10, 0xFF1A1A1A);
        Icons.draw(c, "player", wx + 16, py + 6, 12, Theme.TEXT);
        String name = MinecraftClient.getInstance().getSession().getUsername();
        RenderUtil.text(c, ModuleCard.trim(name, SIDEBAR - 50), wx + 38, py + 4, Theme.TEXT);
        RenderUtil.circle(c, wx + 40, py + 17, 1.6f, 0xFFFFFFFF);
        RenderUtil.textScaled(c, "Online", wx + 44, py + 15, 0.7f, Theme.TEXT_DIM, false);
    }

    private void renderMain(DrawContext c, int mx, int my) {
        float sa = Easing.inOutCubic(settingsAnim.update());
        float base = RenderUtil.globalAlpha;
        float x0 = mainX(), w0 = mainW();

        // ---- grid page
        if (sa < 0.99f) {
            RenderUtil.globalAlpha = base * (1 - sa);
            float off = -sa * 40;
            String title = search.isEmpty() ? selected.displayName : "Search";
            List<ModuleCard> list = visibleCards();
            long on = list.stream().filter(cd -> cd.module.isEnabled()).count();
            if (search.isEmpty()) Icons.draw(c, selected.name(), x0 + off, wy + 15, 14, Theme.TEXT);
            else Icons.draw(c, "search", x0 + off, wy + 15, 14, Theme.TEXT);
            RenderUtil.textScaled(c, "§l" + title, x0 + 20 + off, wy + 13, 1.35f, Theme.TEXT, false);
            RenderUtil.textScaled(c, list.size() + " modules  •  " + on + " enabled", x0 + 20 + off, wy + 26, 0.75f, Theme.TEXT_DIM, false);
            renderSearch(c, mx, my);

            float top = contentTop(), ch = contentH();
            float colW = (w0 - GAP) / 2;
            int rows = (list.size() + 1) / 2;
            contentHeight = rows * (ModuleCard.HEIGHT + GAP) - GAP;
            RenderUtil.scissor(c, x0 - 2, top - 2, w0 + 4, ch + 4);
            float sc = scroll.update();
            boolean interactive = settings == null && RenderUtil.hovered(mx, my, x0, top, w0, ch) && !dragging;
            for (int i = 0; i < list.size(); i++) {
                ModuleCard cd = list.get(i);
                cd.x = x0 + (i % 2) * (colW + GAP) + off;
                cd.y = top + (i / 2) * (ModuleCard.HEIGHT + GAP) + sc;
                cd.w = colW;
                if (cd.y + ModuleCard.HEIGHT < top - 2 || cd.y > top + ch + 2) { cd.appear.update(); continue; }
                cd.render(c, mx, my, interactive);
            }
            if (list.isEmpty()) {
                Icons.draw(c, "search", x0 + w0 / 2 - 10, top + ch / 2 - 22, 20, 0xFF3A3A3A);
                RenderUtil.centered(c, "No modules found", x0 + w0 / 2, top + ch / 2 + 4, Theme.TEXT_DIM);
            }
            RenderUtil.endScissor(c);
            fades(c, x0, top, w0, ch);
        }

        // ---- settings page
        SettingsView sv = settings != null ? settings : lastSettings;
        if (sa > 0.01f && sv != null) {
            RenderUtil.globalAlpha = base * sa;
            float off = (1 - sa) * 40;
            float sc = scroll.update();
            sv.render(c, x0 + off, wy + 16, w0, wh - 28, sc, mx, my);
            contentHeight = sv.contentHeight();
            fades(c, x0, wy + 42, w0, wh - 54);
        }
        RenderUtil.globalAlpha = base;

        float max = Math.max(0, contentHeight - (settings != null ? wh - 54 : contentH()) + 4);
        if (scroll.getTarget() < -max) scroll.setTarget(-max);
        // scrollbar
        if (max > 0) {
            float trackY = settings != null ? wy + 42 : contentTop(), trackH = settings != null ? wh - 54 : contentH();
            float thumbH = Math.max(16, trackH * trackH / (trackH + max));
            float thumbY = trackY + (trackH - thumbH) * (-scroll.get() / max);
            RenderUtil.round(c, wx + ww - 7, thumbY, 2.5f, thumbH, 1.25f, 0x40FFFFFF);
        }
    }

    /** Soft fade at the top/bottom edge of scrolling areas. */
    private void fades(DrawContext c, float x, float y, float w, float h) {
        if (scroll.get() < -1) RenderUtil.gradientV(c, x - 2, y - 2, w + 4, 10, 0xF70B0B0B, 0x000B0B0B);
        RenderUtil.gradientV(c, x - 2, y + h - 8, w + 4, 10, 0x000B0B0B, 0xF70B0B0B);
    }

    private void renderSearch(DrawContext c, int mx, int my) {
        searchFocus.setTarget(searchFocused || !search.isEmpty() ? 1 : 0);
        float f = searchFocus.update();
        float[] b = searchBox();
        RenderUtil.round(c, b[0], b[1], b[2], b[3], 9, ColorUtil.lerp(0xFF111111, 0xFF171717, f));
        RenderUtil.roundOutline(c, b[0], b[1], b[2], b[3], 9, 0.5f, ColorUtil.lerp(0x22FFFFFF, ColorUtil.alpha(Theme.accent(), 0.7f), f));
        Icons.draw(c, "search", b[0] + 7, b[1] + 5, 8, ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, f));
        if (search.isEmpty() && !searchFocused) {
            RenderUtil.textScaled(c, "Search...", b[0] + 19, b[1] + 6, 0.85f, Theme.TEXT_DIM, false);
        } else {
            String shown = search;
            while (RenderUtil.width(shown) * 0.85f > b[2] - 34 && !shown.isEmpty()) shown = shown.substring(1);
            RenderUtil.textScaled(c, shown, b[0] + 19, b[1] + 6, 0.85f, Theme.TEXT, false);
            if (searchFocused && System.currentTimeMillis() / 500 % 2 == 0)
                RenderUtil.rect(c, b[0] + 20 + RenderUtil.width(shown) * 0.85f, b[1] + 5, 0.5f, 8, Theme.TEXT);
            if (!search.isEmpty()) Icons.draw(c, "close", b[0] + b[2] - 14, b[1] + 5, 8, Theme.TEXT_DIM);
        }
        if (!searchFocused && search.isEmpty())
            RenderUtil.textScaled(c, "Ctrl+F", b[0] + b[2] - 9 - RenderUtil.width("Ctrl+F") * 0.75f, b[1] + 6, 0.75f, 0xFF4A4A4A, false);
    }

    private void renderTooltip(DrawContext c, int mx, int my) {
        if (!cfg().tooltips.isOn()) return;
        String t = null;
        Object owner = null;
        if (settings != null && settingsAnim.get() > 0.9f) {
            t = settings.tooltip(mx, my);
            owner = t;
        } else if (settings == null && RenderUtil.hovered(mx, my, mainX(), contentTop(), mainW(), contentH())) {
            for (ModuleCard cd : visibleCards()) {
                if (RenderUtil.hovered(mx, my, cd.x, cd.y, cd.w, ModuleCard.HEIGHT)) {
                    String desc = cd.module.getDescription();
                    if (RenderUtil.width(desc) * 0.75f > cd.w - 80) { t = desc; owner = cd; }
                    else { t = "LMB toggle  •  RMB settings"; owner = cd; }
                }
            }
        }
        if (owner != tipOwner) { tipOwner = owner; tipSince = System.currentTimeMillis(); }
        boolean show = t != null && !t.isEmpty() && System.currentTimeMillis() - tipSince > 350 && !dragging;
        tipAnim.setTarget(show ? 1 : 0);
        float ta = tipAnim.update();
        if (show) tip = t;
        if (ta < 0.02f || tip.isEmpty()) return;
        float base = RenderUtil.globalAlpha;
        RenderUtil.globalAlpha = base * ta;
        float tw = RenderUtil.width(tip) * 0.85f + 12;
        float bx = Math.min(mx + 8, wx + ww - tw - 4), by = my - 20 - (1 - ta) * 4;
        RenderUtil.round(c, bx, by, tw, 14, 4, 0xF5161616);
        RenderUtil.roundOutline(c, bx, by, tw, 14, 4, 0.5f, 0x30FFFFFF);
        RenderUtil.textScaled(c, tip, bx + 6, by + 4, 0.85f, Theme.TEXT, false);
        RenderUtil.globalAlpha = base;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return false;
        int mx = tx(mouseX), my = ty(mouseY);
        if (listening != null) {
            listening.mouseClicked(mx, my, button);
            return true;
        }
        searchFocused = false;

        // sidebar
        float y0 = wy + 58;
        for (Category cat : Category.values()) {
            if (RenderUtil.hovered(mx, my, wx + 8, y0 + cat.ordinal() * (CAT_H + 2), SIDEBAR - 16, CAT_H)) {
                select(cat);
                return true;
            }
        }
        float[] hb = hudButton();
        if (button == 0 && RenderUtil.hovered(mx, my, hb[0], hb[1], hb[2], hb[3])) {
            click(1.2f);
            MinecraftClient.getInstance().setScreen(new HudEditorScreen(this));
            return true;
        }
        // drag the window by the logo / header strip
        if (button == 0 && (RenderUtil.hovered(mx, my, wx, wy, SIDEBAR, 44) || (settings == null && RenderUtil.hovered(mx, my, mainX(), wy, mainW() - searchBox()[2] - 6, 40)))) {
            dragging = true;
            dragX = mx - offX;
            dragY = my - offY;
            return true;
        }

        if (settings != null) {
            int r = settings.click(mx, my, button);
            if (r == -1) {
                closeSettings();
                click(0.9f);
            }
            return true;
        }

        float[] sb = searchBox();
        if (RenderUtil.hovered(mx, my, sb[0], sb[1], sb[2], sb[3])) {
            if (!search.isEmpty() && mx > sb[0] + sb[2] - 18) {
                search = "";
                resetCards();
            }
            searchFocused = true;
            return true;
        }
        if (RenderUtil.hovered(mx, my, mainX(), contentTop(), mainW(), contentH())) {
            for (ModuleCard cd : visibleCards()) {
                int r = cd.click(mx, my, button);
                if (r == 1) { click(cd.module.isEnabled() ? 1.25f : 0.85f); return true; }
                if (r == 2) { openSettings(cd.module); return true; }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        if (settings != null) settings.released(tx(mouseX), ty(mouseY), button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int mx = tx(mouseX), my = ty(mouseY);
        if (settings != null && settings.scrollSlider(mx, my, verticalAmount)) return true;
        scroll.setTarget(Math.min(0, scroll.getTarget() + verticalAmount * 28));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening != null) {
            listening.keyPressed(keyCode);
            return true;
        }
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!search.isEmpty()) {
                    search = ctrl ? "" : search.substring(0, search.length() - 1);
                    resetCards();
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                List<ModuleCard> v = visibleCards();
                if (v.size() == 1) openSettings(v.get(0).module);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                search = "";
                resetCards();
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_F && ctrl) {
            closeSettings();
            searchFocused = true;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && settings != null) {
            closeSettings();
            click(0.9f);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && !search.isEmpty()) {
            search = "";
            resetCards();
            return true;
        }
        if (keyCode == cfg().getKey() || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        // arrow keys switch categories
        if (settings == null && search.isEmpty() && (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_UP)) {
            int n = Category.values().length;
            select(Category.values()[(selected.ordinal() + (keyCode == GLFW.GLFW_KEY_DOWN ? 1 : n - 1)) % n]);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (listening != null) return true;
        if (Character.isLetterOrDigit(chr) || (chr == ' ' && searchFocused)) {
            if (settings != null && !searchFocused) return true;
            searchFocused = true;
            if (search.length() < 24) {
                search += chr;
                resetCards();
            }
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
        dragging = false;
        openAnim.setTarget(0);
        WaveClient.INSTANCE.config.save();
    }

    @Override
    public void removed() {
        listening = null;
        dragging = false;
        if (settings != null) settings.released(0, 0, 0);
        WaveClient.INSTANCE.config.save();
    }
}
