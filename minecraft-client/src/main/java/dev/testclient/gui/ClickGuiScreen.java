package dev.testclient.gui;

import com.google.gson.JsonObject;
import dev.testclient.Config;
import dev.testclient.TestClient;
import dev.testclient.module.Category;
import dev.testclient.module.Module;
import dev.testclient.setting.BooleanSetting;
import dev.testclient.setting.NumberSetting;
import dev.testclient.setting.Setting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ClickGUI: panel na kategorię, przeciągany za nagłówek.
 * LPM na module = włącz/wyłącz, PPM = rozwiń ustawienia.
 * PPM na nagłówku = zwiń panel.
 */
public class ClickGuiScreen extends Screen {
    private static final int WIDTH = 112;
    private static final int HEADER_H = 16;
    private static final int ROW_H = 14;

    private static final Map<Category, Panel> PANELS = new EnumMap<>(Category.class);
    private static final Set<Module> EXPANDED = new HashSet<>();

    static {
        int x = 10;
        for (Category c : Category.values()) {
            PANELS.put(c, new Panel(c, x, 10));
            x += WIDTH + 8;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private Panel dragging;
    private int dragOffX, dragOffY;
    private Row activeSlider;
    private Module listeningBind;

    public ClickGuiScreen() {
        super(Text.literal("ClickGUI"));
    }

    // ---------------------------------------------------------------- render

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, width, height, 0x90000000, 0xB0100820);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        TextRenderer tr = textRenderer;
        rows.clear();
        Module hovered = null;

        for (Panel p : PANELS.values()) {
            int x = p.x, y = p.y;

            // nagłówek
            ctx.fill(x - 1, y - 1, x + WIDTH + 1, y + HEADER_H, Theme.OUTLINE);
            ctx.fill(x, y, x + WIDTH, y + HEADER_H, Theme.HEADER);
            ctx.fill(x, y + HEADER_H - 1, x + WIDTH, y + HEADER_H, Theme.ACCENT);
            ctx.drawTextWithShadow(tr, p.category.displayName, x + 5, y + 4, Theme.TEXT);
            ctx.drawTextWithShadow(tr, p.open ? "-" : "+", x + WIDTH - 10, y + 4, Theme.TEXT_DIM);
            rows.add(Row.header(p, x, y, WIDTH, HEADER_H));
            y += HEADER_H;

            if (!p.open) continue;

            for (Module m : TestClient.modules().getByCategory(p.category)) {
                boolean hover = inside(mouseX, mouseY, x, y, WIDTH, ROW_H);
                if (hover) hovered = m;
                ctx.fill(x - 1, y, x + WIDTH + 1, y + ROW_H, Theme.OUTLINE);
                ctx.fill(x, y, x + WIDTH, y + ROW_H, m.isEnabled() ? Theme.ACCENT_DIM : Theme.PANEL);
                if (m.isEnabled()) ctx.fill(x, y, x + 2, y + ROW_H, Theme.ACCENT);
                if (hover) ctx.fill(x, y, x + WIDTH, y + ROW_H, Theme.ROW_HOVER);
                ctx.drawTextWithShadow(tr, m.getName(), x + 6, y + 3, m.isEnabled() ? Theme.TEXT : Theme.TEXT_DIM);
                ctx.drawTextWithShadow(tr, EXPANDED.contains(m) ? "v" : "...", x + WIDTH - 14, y + 3, Theme.TEXT_DIM);
                rows.add(Row.module(m, x, y, WIDTH, ROW_H));
                y += ROW_H;

                if (EXPANDED.contains(m)) y = renderSettings(ctx, m, x, y, mouseX, mouseY);
            }
            ctx.fill(x - 1, y, x + WIDTH + 1, y + 1, Theme.OUTLINE);
        }

        // opis modułu pod kursorem
        if (hovered != null) {
            String desc = hovered.getDescription();
            int w = tr.getWidth(desc) + 8;
            ctx.fill(4, height - 18, 4 + w, height - 4, Theme.HEADER);
            ctx.fill(4, height - 5, 4 + w, height - 4, Theme.ACCENT);
            ctx.drawTextWithShadow(tr, desc, 8, height - 15, Theme.TEXT);
        }
        ctx.drawTextWithShadow(tr, "LPM: toggle  |  PPM: ustawienia  |  ESC: zamknij",
                width - tr.getWidth("LPM: toggle  |  PPM: ustawienia  |  ESC: zamknij") - 6, height - 14, Theme.TEXT_DIM);
    }

    private int renderSettings(DrawContext ctx, Module m, int x, int y, int mouseX, int mouseY) {
        TextRenderer tr = textRenderer;
        for (Setting<?> s : m.getSettings()) {
            ctx.fill(x - 1, y, x + WIDTH + 1, y + ROW_H, Theme.OUTLINE);
            ctx.fill(x, y, x + WIDTH, y + ROW_H, Theme.SETTING_BG);
            if (inside(mouseX, mouseY, x, y, WIDTH, ROW_H)) ctx.fill(x, y, x + WIDTH, y + ROW_H, Theme.ROW_HOVER);

            if (s instanceof BooleanSetting b) {
                ctx.drawTextWithShadow(tr, b.getName(), x + 10, y + 3, Theme.TEXT_DIM);
                int bx = x + WIDTH - 14;
                ctx.fill(bx, y + 3, bx + 8, y + 11, Theme.OUTLINE);
                if (b.get()) ctx.fill(bx + 1, y + 4, bx + 7, y + 10, Theme.ACCENT);
                rows.add(Row.bool(b, x, y, WIDTH, ROW_H));
            } else if (s instanceof NumberSetting n) {
                int sx = x + 6, sw = WIDTH - 12;
                ctx.fill(sx, y + ROW_H - 3, sx + sw, y + ROW_H - 2, Theme.OUTLINE);
                ctx.fill(sx, y + ROW_H - 3, sx + (int) (sw * n.getPercent()), y + ROW_H - 2, Theme.ACCENT);
                ctx.drawTextWithShadow(tr, n.getName(), x + 10, y + 2, Theme.TEXT_DIM);
                String val = String.format("%.2f", n.get());
                ctx.drawTextWithShadow(tr, val, x + WIDTH - tr.getWidth(val) - 6, y + 2, Theme.TEXT);
                rows.add(Row.number(n, x, y, WIDTH, ROW_H, sx, sw));
            }
            y += ROW_H;
        }

        // keybind
        ctx.fill(x - 1, y, x + WIDTH + 1, y + ROW_H, Theme.OUTLINE);
        ctx.fill(x, y, x + WIDTH, y + ROW_H, Theme.SETTING_BG);
        if (inside(mouseX, mouseY, x, y, WIDTH, ROW_H)) ctx.fill(x, y, x + WIDTH, y + ROW_H, Theme.ROW_HOVER);
        String bind = listeningBind == m ? "..." : keyName(m.getKey());
        ctx.drawTextWithShadow(tr, "Bind", x + 10, y + 3, Theme.TEXT_DIM);
        ctx.drawTextWithShadow(tr, bind, x + WIDTH - tr.getWidth(bind) - 6, y + 3,
                listeningBind == m ? Theme.ACCENT : Theme.TEXT);
        rows.add(Row.bind(m, x, y, WIDTH, ROW_H));
        return y + ROW_H;
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // od końca, żeby panel narysowany na wierzchu dostał klik pierwszy
        for (int i = rows.size() - 1; i >= 0; i--) {
            Row r = rows.get(i);
            if (!inside(mouseX, mouseY, r.x, r.y, r.w, r.h)) continue;
            switch (r.type) {
                case HEADER -> {
                    if (button == 0) {
                        dragging = r.panel;
                        dragOffX = (int) mouseX - r.panel.x;
                        dragOffY = (int) mouseY - r.panel.y;
                        // przesuń panel na wierzch
                        PANELS.remove(r.panel.category);
                        PANELS.put(r.panel.category, r.panel);
                    } else if (button == 1) {
                        r.panel.open = !r.panel.open;
                    }
                }
                case MODULE -> {
                    if (button == 0) r.module.toggle();
                    else if (button == 1 && !EXPANDED.remove(r.module)) EXPANDED.add(r.module);
                }
                case BOOL -> { if (button == 0) ((BooleanSetting) r.setting).toggle(); }
                case NUMBER -> {
                    if (button == 0) {
                        activeSlider = r;
                        updateSlider(mouseX);
                    }
                }
                case BIND -> {
                    if (button == 0) listeningBind = listeningBind == r.module ? null : r.module;
                    else if (button == 1) r.module.setKey(-1);
                }
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging != null) {
            dragging.x = (int) mouseX - dragOffX;
            dragging.y = (int) mouseY - dragOffY;
            return true;
        }
        if (activeSlider != null) {
            updateSlider(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = null;
        activeSlider = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateSlider(double mouseX) {
        NumberSetting n = (NumberSetting) activeSlider.setting;
        double pct = Math.max(0, Math.min(1, (mouseX - activeSlider.sliderX) / activeSlider.sliderW));
        n.set(n.getMin() + pct * (n.getMax() - n.getMin()));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listeningBind != null) {
            // ESC / Delete / Backspace = usuń bind
            boolean clear = keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE
                    || keyCode == GLFW.GLFW_KEY_BACKSPACE;
            listeningBind.setKey(clear ? -1 : keyCode);
            listeningBind = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        Config.save();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ---------------------------------------------------------------- utils

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static String keyName(int key) {
        if (key < 0) return "brak";
        return InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString().toUpperCase();
    }

    public static JsonObject savePanels() {
        JsonObject o = new JsonObject();
        for (Panel p : PANELS.values()) {
            JsonObject j = new JsonObject();
            j.addProperty("x", p.x);
            j.addProperty("y", p.y);
            j.addProperty("open", p.open);
            o.add(p.category.name(), j);
        }
        return o;
    }

    public static void loadPanels(JsonObject o) {
        for (Panel p : PANELS.values()) {
            if (!o.has(p.category.name())) continue;
            JsonObject j = o.getAsJsonObject(p.category.name());
            p.x = j.get("x").getAsInt();
            p.y = j.get("y").getAsInt();
            p.open = j.get("open").getAsBoolean();
        }
    }

    // ---------------------------------------------------------------- types

    private static final class Panel {
        final Category category;
        int x, y;
        boolean open = true;

        Panel(Category category, int x, int y) {
            this.category = category;
            this.x = x;
            this.y = y;
        }
    }

    private enum RowType { HEADER, MODULE, BOOL, NUMBER, BIND }

    /** Obszar kliknięcia wyliczony podczas ostatniego renderu. */
    private record Row(RowType type, int x, int y, int w, int h,
                       Panel panel, Module module, Setting<?> setting, int sliderX, int sliderW) {
        static Row header(Panel p, int x, int y, int w, int h) { return new Row(RowType.HEADER, x, y, w, h, p, null, null, 0, 0); }
        static Row module(Module m, int x, int y, int w, int h) { return new Row(RowType.MODULE, x, y, w, h, null, m, null, 0, 0); }
        static Row bool(BooleanSetting s, int x, int y, int w, int h) { return new Row(RowType.BOOL, x, y, w, h, null, null, s, 0, 0); }
        static Row number(NumberSetting s, int x, int y, int w, int h, int sx, int sw) { return new Row(RowType.NUMBER, x, y, w, h, null, null, s, sx, sw); }
        static Row bind(Module m, int x, int y, int w, int h) { return new Row(RowType.BIND, x, y, w, h, null, m, null, 0, 0); }
    }
}
