package pl.firen.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import pl.firen.client.FirenClient;
import pl.firen.client.config.ConfigManager;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.module.modules.ClickGui;
import pl.firen.client.module.modules.Hud;
import pl.firen.client.render.Fonts;
import pl.firen.client.render.Render2D;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.ColorSetting;
import pl.firen.client.setting.ModeSetting;
import pl.firen.client.setting.NumberSetting;
import pl.firen.client.setting.Setting;
import pl.firen.client.util.Anim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;

import static pl.firen.client.gui.Theme.*;

/**
 * FirenClient V2 – GUI: górny pasek zakładek, lista modułów, panel ustawień,
 * zakładki Configs (profile) i Theme, pasek statusu.
 */
public class ClickGuiScreen extends Screen {
    public static float winX = -1, winY = -1;
    public static int tab = 0;
    public static Category category = Category.COMBAT;
    private static final Map<Category, Module> SELECTED = new EnumMap<>(Category.class);
    private static String query = "";

    private static final int TAB_CONFIGS = Category.values().length, TAB_THEME = TAB_CONFIGS + 1;
    private static final float TOP = 48, STATUS = 24, ROW = 30;
    private static final float ROW_BOOL = 22, ROW_NUM = 30, ROW_MODE = 36, ROW_COLOR = 22, COLOR_PICKER = 80, CHIP_H = 17, CHIP_GAP = 5;

    private record Hit(float x1, float y1, float x2, float y2, IntConsumer action) {}

    private final List<Hit> hits = new ArrayList<>();
    private final long openedAt = System.nanoTime();
    private long closingAt = -1, lastFrame = System.nanoTime();
    private float s = 1f;
    private int vw, vh;

    private float tabX = -1, tabW = 0;
    private float listScroll, listTarget, listMax;
    private float panelScroll, panelTarget, panelMax;
    private float panelAnim = 1f;
    private Object lastPanel;
    private float listTop, listBottom, listLeft, listRight;

    private boolean draggingWindow;
    private float dragOffX, dragOffY;
    private NumberSetting slider;
    private float sliderX, sliderW;
    private ColorSetting colorDrag;
    private int colorDragPart;
    private float cdX, cdY, cdW, cdH;
    private Module binding;

    private boolean editingName;
    private String nameField = "";
    private String toast;
    private long toastAt;

    public ClickGuiScreen() {
        super(Text.literal("FirenClient V2"));
    }

    @Override
    protected void init() {
        double factor = client.getWindow().getScaleFactor();
        int target = client.getWindow().getFramebufferWidth() >= 1280 ? 2 : 1;
        s = (float) (target / factor);
        vw = Math.round(width / s);
        vh = Math.round(height / s);
        if (tab < 0 || tab > TAB_THEME) tab = 0;
    }

    /** Otwiera GUI z wybranym modułem (np. z komend / testów). */
    public static void select(Module m) {
        SELECTED.put(m.getCategory(), m);
        tab = m.getCategory().ordinal();
    }

    public static void openTab(int t) {
        tab = t;
    }

    public static String keyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return "Brak";
        return InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString().toUpperCase(Locale.ROOT);
    }

    private void hit(float x1, float y1, float x2, float y2, IntConsumer action) {
        if (y2 > y1 && x2 > x1) hits.add(new Hit(x1, y1, x2, y2, action));
    }

    private void showToast(String text) {
        toast = text;
        toastAt = System.currentTimeMillis();
    }

    private float progress() {
        float open = Math.min(1f, (System.nanoTime() - openedAt) / 280_000_000f);
        if (closingAt < 0) return Anim.easeOutCubic(open);
        return Anim.easeOutCubic(open) * (1f - Math.min(1f, (System.nanoTime() - closingAt) / 160_000_000f));
    }

    private static float clampY(float v, float top, float bottom) {
        return Math.max(top, Math.min(bottom, v));
    }

    // ================================================================== render

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (FirenClient.modules().get(ClickGui.class).blur.get()) super.renderBackground(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void render(DrawContext ctx, int rawX, int rawY, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        if (closingAt > 0 && now - closingAt > 160_000_000L) {
            client.setScreen(null);
            return;
        }
        float mx = rawX / s, my = rawY / s;
        float p = progress();

        renderBackground(ctx, rawX, rawY, delta);
        ctx.fillGradient(0, 0, width, height, Anim.withAlpha(0x70000000, p), Anim.withAlpha(0xB0000000, p));
        hits.clear();
        Render2D.alpha = p;

        float W = Math.min(660, vw - 24), H = Math.min(390, vh - 24);
        if (winX < 0 || winY < 0) { winX = (vw - W) / 2f; winY = (vh - H) / 2f; }
        if (draggingWindow) { winX = mx - dragOffX; winY = my - dragOffY; }
        winX = Math.max(4, Math.min(vw - W - 4, winX));
        winY = Math.max(4, Math.min(vh - H - 4, winY));
        float x = winX, y = winY;

        var m = ctx.getMatrices();
        m.push();
        m.scale(s, s, 1f);
        float zoom = 0.95f + 0.05f * p;
        m.translate(x + W / 2f, y + H / 2f + (1 - p) * 10f, 0);
        m.scale(zoom, zoom, 1f);
        m.translate(-(x + W / 2f), -(y + H / 2f), 0);

        Render2D.shadow(ctx, x, y + 8, W, H, 18, 34, 0xC0000000);
        Render2D.shadow(ctx, x, y, W, H, 18, 26, Theme.accentAlpha(0x1C));
        Render2D.gradientV(ctx, x, y, W, H, 18, 0xF70E0E12, 0xF706060A);
        Render2D.outline(ctx, x, y, W, H, 18, 0.5f, 0x22FFFFFF);
        Render2D.gradientH(ctx, x + 1, y + 1, W * 0.6f, TOP, 17, Theme.accentAlpha(0x16), 0x00000000);

        renderTopBar(ctx, x, y, W, mx, my, dt);
        Render2D.round(ctx, x + 14, y + TOP, W - 28, 0.5f, 0, 0x14FFFFFF);

        float bodyTop = y + TOP + 10, bodyBottom = y + H - STATUS - 6;
        boolean searching = !query.isEmpty();
        if (tab == TAB_CONFIGS && !searching) renderConfigs(ctx, x + 12, bodyTop, W - 24, bodyBottom - bodyTop, mx, my);
        else if (tab == TAB_THEME && !searching) renderThemeTab(ctx, x + 12, bodyTop, W - 24, bodyBottom - bodyTop, mx, my, dt);
        else renderModules(ctx, x + 12, bodyTop, W - 24, bodyBottom - bodyTop, mx, my, dt);

        renderStatus(ctx, x, y + H - STATUS, W);
        renderToast(ctx, x, y, W, H);

        m.pop();
        Render2D.alpha = 1f;
    }

    // ------------------------------------------------------------------ górny pasek

    private void renderTopBar(DrawContext ctx, float x, float y, float W, float mx, float my, float dt) {
        Render2D.shadow(ctx, x + 14, y + 12, 26, 26, 8, 12, Theme.accentAlpha(0x60));
        Render2D.gradientV(ctx, x + 14, y + 11, 26, 26, 8, Theme.accent(), Theme.accent2());
        Fonts.drawCentered(ctx, Fonts.I_FIRE, Fonts.ICONS, x + 27, y + 19, Theme.onAccent());
        Fonts.draw(ctx, "FirenClient", Fonts.SEMIBOLD, x + 47, y + 14, TEXT);
        float vx = x + 47 + Fonts.width("FirenClient", Fonts.SEMIBOLD) + 5;
        Render2D.round(ctx, vx, y + 13, 18, 11, 4, Theme.accentAlpha(0x30));
        Fonts.drawCentered(ctx, "V2", Fonts.SMALL, vx + 9, y + 15.5f, Theme.accent());
        Fonts.draw(ctx, "moduły · configi · motywy", Fonts.SMALL, x + 47, y + 27, TEXT_3);
        hit(x, y, x + 150, y + TOP, b -> {
            if (b == 0) { draggingWindow = true; dragOffX = mx - winX; dragOffY = my - winY; }
        });

        Category[] cats = Category.values();
        int n = TAB_THEME + 1;
        String[] icons = new String[n], labels = new String[n];
        for (int i = 0; i < cats.length; i++) { icons[i] = cats[i].icon; labels[i] = cats[i].display; }
        icons[TAB_CONFIGS] = Fonts.I_SAVE; labels[TAB_CONFIGS] = "Configs";
        icons[TAB_THEME] = Fonts.I_PALETTE; labels[TAB_THEME] = "Theme";

        float searchW = 128, tabsX = x + 168, tabsRight = x + W - 14 - searchW - 10;
        float full = 0;
        for (int i = 0; i < n; i++) full += Fonts.width(labels[i], Fonts.SEMIBOLD) + 34 + (i == TAB_CONFIGS ? 8 : 0);
        boolean compact = full > tabsRight - tabsX;
        float[] tx = new float[n], tw = new float[n];
        float cx = tabsX;
        for (int i = 0; i < n; i++) {
            if (i == TAB_CONFIGS) cx += 8;
            boolean showLabel = !compact || i == tab;
            tw[i] = showLabel ? Fonts.width(labels[i], Fonts.SEMIBOLD) + 34 : 26;
            tx[i] = cx;
            cx += tw[i] + 2;
        }
        int active = query.isEmpty() ? tab : -1;
        if (active >= 0) {
            if (tabX < 0) { tabX = tx[active]; tabW = tw[active]; }
            tabX = Anim.approach(tabX, tx[active], dt, 16f);
            tabW = Anim.approach(tabW, tw[active], dt, 16f);
            Render2D.shadow(ctx, tabX, y + 13, tabW, 24, 9, 10, Theme.accentAlpha(0x50));
            Render2D.gradientV(ctx, tabX, y + 12, tabW, 24, 9, Theme.accent(), Theme.accent2());
        }
        for (int i = 0; i < n; i++) {
            boolean on = i == active;
            boolean hover = mx >= tx[i] && mx < tx[i] + tw[i] && my >= y + 12 && my < y + 36;
            if (hover && !on) Render2D.round(ctx, tx[i], y + 12, tw[i], 24, 9, 0x0FFFFFFF);
            int col = on ? Theme.onAccent() : hover ? TEXT : TEXT_2;
            boolean showLabel = !compact || i == tab;
            Fonts.drawCentered(ctx, icons[i], Fonts.ICONS, tx[i] + (showLabel ? 13 : tw[i] / 2f), y + 20.5f, col);
            if (showLabel) Fonts.draw(ctx, labels[i], Fonts.SEMIBOLD, tx[i] + 22, y + 20, col);
            final int idx = i;
            hit(tx[i], y + 12, tx[i] + tw[i], y + 36, b -> {
                tab = idx;
                query = "";
                editingName = false;
                if (idx < TAB_CONFIGS) category = Category.values()[idx];
                listTarget = 0;
                panelTarget = 0;
            });
        }
        if (compact) Render2D.round(ctx, tx[TAB_CONFIGS] - 6, y + 17, 0.5f, 14, 0, 0x22FFFFFF);

        float sx = x + W - 14 - searchW, sy = y + 12;
        Render2D.round(ctx, sx, sy, searchW, 24, 9, FIELD);
        Render2D.outline(ctx, sx, sy, searchW, 24, 9, 0.5f, query.isEmpty() ? STROKE : Theme.accentAlpha(0x90));
        Fonts.draw(ctx, Fonts.I_SEARCH, Fonts.ICONS, sx + 9, sy + 8.5f, query.isEmpty() ? TEXT_3 : Theme.accent());
        boolean caret = (System.currentTimeMillis() / 530) % 2 == 0;
        if (query.isEmpty()) Fonts.draw(ctx, "Szukaj…", Fonts.REGULAR, sx + 23, sy + 8, TEXT_3);
        else Fonts.draw(ctx, Fonts.trim(query, Fonts.REGULAR, searchW - 34) + (caret && !editingName ? "|" : ""), Fonts.REGULAR, sx + 23, sy + 8, TEXT);
    }

    // ------------------------------------------------------------------ lista modułów + panel

    private List<Module> visibleModules() {
        String q = query.toLowerCase(Locale.ROOT);
        List<Module> list = new ArrayList<>(q.isEmpty()
                ? FirenClient.modules().byCategory(Category.values()[Math.min(tab, TAB_CONFIGS - 1)])
                : FirenClient.modules().all().stream().filter(md -> md.getName().toLowerCase(Locale.ROOT).contains(q)
                    || md.getDescription().toLowerCase(Locale.ROOT).contains(q)).toList());
        list.sort(Comparator.comparing((Module md) -> !md.favorite));
        return list;
    }

    private void renderModules(DrawContext ctx, float x, float y, float w, float h, float mx, float my, float dt) {
        List<Module> mods = visibleModules();
        Category cat = query.isEmpty() ? Category.values()[Math.min(tab, TAB_CONFIGS - 1)] : null;
        Module sel = cat != null ? SELECTED.get(cat) : null;
        if (sel == null || !mods.contains(sel)) sel = mods.isEmpty() ? null : mods.get(0);
        if (cat != null && sel != null) SELECTED.put(cat, sel);

        float lw = 200, lx = x, ly = y;
        Render2D.round(ctx, lx, ly, lw, h, 13, SIDEBAR);
        Render2D.outline(ctx, lx, ly, lw, h, 13, 0.5f, STROKE);
        String title = cat == null ? "Wyniki" : cat.display;
        Fonts.draw(ctx, title.toUpperCase(Locale.ROOT), Fonts.SMALL, lx + 14, ly + 12, TEXT_3);
        long on = mods.stream().filter(Module::isEnabled).count();
        Fonts.drawRight(ctx, on + "/" + mods.size(), Fonts.SMALL, lx + lw - 14, ly + 12, TEXT_3);

        listTop = ly + 28; listBottom = ly + h - 8; listLeft = lx; listRight = lx + lw;
        listScroll = Anim.approach(listScroll, listTarget, dt, 16f);
        ctx.enableScissor((int) lx, (int) listTop, (int) Math.ceil(lx + lw), (int) Math.ceil(listBottom));
        float ry = listTop - listScroll;
        for (Module md : mods) {
            renderRow(ctx, md, md == sel, lx + 7, ry, lw - 14, mx, my, dt, cat);
            ry += ROW + 3;
        }
        ctx.disableScissor();
        listMax = Math.max(0, mods.size() * (ROW + 3) - (listBottom - listTop));
        listTarget = Math.max(0, Math.min(listMax, listTarget));
        if (mods.isEmpty()) Fonts.drawCentered(ctx, "Brak wyników", Fonts.REGULAR, lx + lw / 2f, listTop + 20, TEXT_3);

        float px = lx + lw + 10, pw = w - lw - 10;
        if (sel != lastPanel) { lastPanel = sel; panelAnim = 0f; panelTarget = 0; panelScroll = 0; }
        panelAnim = Anim.approach(panelAnim, 1f, dt, 14f);
        if (sel != null) renderPanel(ctx, sel, px, y, pw, h, mx, my, dt);
    }

    private void renderRow(DrawContext ctx, Module md, boolean selected, float x, float y, float w,
                           float mx, float my, float dt, Category cat) {
        boolean inList = my >= listTop && my < listBottom;
        boolean hover = inList && mx >= x && mx < x + w && my >= y && my < y + ROW;
        md.hoverAnim = Anim.approach(md.hoverAnim, hover ? 1f : 0f, dt, 18f);
        md.toggleAnim = Anim.approach(md.toggleAnim, md.isEnabled() ? 1f : 0f, dt, 16f);
        float t = md.toggleAnim;

        if (selected) {
            Render2D.round(ctx, x, y, w, ROW, 9, 0xFF1B1B21);
            Render2D.outline(ctx, x, y, w, ROW, 9, 0.5f, Theme.accentAlpha(0x55));
            Render2D.round(ctx, x + 3, y + 8, 2.5f, ROW - 16, 1.25f, Theme.accent());
        } else if (md.hoverAnim > 0.01f) {
            Render2D.round(ctx, x, y, w, ROW, 9, Anim.withAlpha(0xFF15151A, md.hoverAnim));
        }

        boolean starHover = hover && mx < x + 22;
        int starCol = md.favorite ? Theme.accent() : starHover ? TEXT_2 : 0xFF34343C;
        Fonts.drawCentered(ctx, Fonts.I_STAR, Fonts.ICONS, x + 13, y + 11.5f, starCol);

        int nameCol = Anim.lerpColor(TEXT_2, TEXT, Math.max(t, selected ? 1f : md.hoverAnim));
        Fonts.draw(ctx, Fonts.trim(md.getName(), Fonts.SEMIBOLD, w - 90), Fonts.SEMIBOLD, x + 24, y + 11, nameCol);
        if (cat == null) Fonts.draw(ctx, md.getCategory().display, Fonts.SMALL, x + 24 + Fonts.width(md.getName(), Fonts.SEMIBOLD) + 5, y + 12, TEXT_3);

        float tw = 20, th = 11, tx = x + w - 10 - tw, ty = y + 9.5f;
        if (md.getKey() != GLFW.GLFW_KEY_UNKNOWN || binding == md) {
            String k = binding == md ? "…" : keyName(md.getKey());
            float kw = Math.max(14, Fonts.width(k, Fonts.SMALL) + 8);
            Render2D.round(ctx, tx - kw - 6, ty, kw, th, 4, binding == md ? Theme.accent() : 0xFF24242B);
            Fonts.drawCentered(ctx, k, Fonts.SMALL, tx - kw / 2f - 6, ty + 2.5f, binding == md ? Theme.onAccent() : TEXT_2);
        }
        Render2D.round(ctx, tx, ty, tw, th, th / 2f, Anim.lerpColor(TRACK, Theme.accent(), t));
        if (t > 0.01f) Render2D.shadow(ctx, tx, ty, tw, th, th / 2f, 6, Anim.withAlpha(Theme.accentAlpha(0x60), t));
        Render2D.circle(ctx, tx + 5.5f + t * 9f, ty + th / 2f, 3.8f, Anim.lerpColor(0xFFB5B5BD, Theme.onAccent(), t));

        float top = clampY(y, listTop, listBottom), bottom = clampY(y + ROW, listTop, listBottom);
        hit(x, top, x + w, bottom, b -> {
            if (b == 0) {
                if (cat != null) SELECTED.put(cat, md);
                else { tab = md.getCategory().ordinal(); SELECTED.put(md.getCategory(), md); query = ""; }
            } else if (b == 1) md.toggle();
            else if (b == 2) binding = binding == md ? null : md;
        });
        hit(x, top, x + 22, bottom, b -> md.favorite = !md.favorite);
        hit(tx - 4, top, tx + tw + 4, bottom, b -> md.toggle());
    }

    private void renderPanel(DrawContext ctx, Module md, float x, float y, float w, float h, float mx, float my, float dt) {
        Render2D.gradientV(ctx, x, y, w, h, 13, CARD, 0xFF0D0D11);
        Render2D.outline(ctx, x, y, w, h, 13, 0.5f, Anim.lerpColor(STROKE, Theme.accentAlpha(0x50), md.toggleAnim));

        float a = Anim.easeOutCubic(panelAnim);
        float baseAlpha = Render2D.alpha;
        Render2D.alpha = baseAlpha * a;
        var m = ctx.getMatrices();
        m.push();
        m.translate((1 - a) * 12f, 0, 0);

        Fonts.draw(ctx, md.getName(), Fonts.TITLE, x + 18, y + 16, WHITE);
        float chipX = x + 18 + Fonts.width(md.getName(), Fonts.TITLE) + 8;
        String cat = md.getCategory().display;
        float cw = Fonts.width(cat, Fonts.SMALL) + 20;
        Render2D.round(ctx, chipX, y + 18, cw, 13, 5, 0xFF1F1F26);
        Fonts.draw(ctx, md.getCategory().icon, Fonts.ICONS, chipX + 5, y + 20.5f, TEXT_3);
        Fonts.draw(ctx, cat, Fonts.SMALL, chipX + 15, y + 21, TEXT_2);
        Fonts.draw(ctx, Fonts.trim(md.getDescription(), Fonts.SMALL, w - 150), Fonts.SMALL, x + 18, y + 38, TEXT_3);

        float t = md.toggleAnim;
        float bw = 34, bh = 18, bx = x + w - 18 - bw, by = y + 17;
        Render2D.round(ctx, bx, by, bw, bh, bh / 2f, Anim.lerpColor(TRACK, Theme.accent(), t));
        if (t > 0.01f) Render2D.shadow(ctx, bx, by, bw, bh, bh / 2f, 10, Anim.withAlpha(Theme.accentAlpha(0x70), t));
        Render2D.circle(ctx, bx + 9 + t * 16f, by + bh / 2f, 6.5f, Anim.lerpColor(0xFFC5C5CC, Theme.onAccent(), t));
        hit(bx - 2, by - 2, bx + bw + 2, by + bh + 2, b -> md.toggle());

        boolean waiting = binding == md;
        String key = waiting ? "Naciśnij…" : keyName(md.getKey());
        float kw = Fonts.width(key, Fonts.SMALL) + 26;
        float kx = bx - 8 - kw, ky = by + 2;
        Render2D.round(ctx, kx, ky, kw, 14, 6, waiting ? Theme.accent() : 0xFF1F1F26);
        Fonts.draw(ctx, Fonts.I_KEYBOARD, Fonts.ICONS, kx + 6, ky + 3.5f, waiting ? Theme.onAccent() : TEXT_3);
        Fonts.draw(ctx, key, Fonts.SMALL, kx + 19, ky + 4, waiting ? Theme.onAccent() : TEXT_2);
        hit(kx, ky, kx + kw, ky + 14, b -> binding = b == 1 ? null : (binding == md ? null : md));
        if (!md.getSettings().isEmpty()) {
            float rx = kx - 22;
            boolean rh = mx >= rx && mx < rx + 16 && my >= ky && my < ky + 14;
            Render2D.round(ctx, rx, ky, 16, 14, 6, rh ? 0xFF2A2A32 : 0xFF1F1F26);
            Fonts.drawCentered(ctx, Fonts.I_RESET, Fonts.ICONS, rx + 8, ky + 3.5f, rh ? TEXT : TEXT_3);
            hit(rx, ky, rx + 16, ky + 14, b -> { md.resetSettings(); showToast("Przywrócono domyślne: " + md.getName()); });
        }

        Render2D.round(ctx, x + 18, y + 54, w - 36, 0.5f, 0, 0x14FFFFFF);

        float top = y + 60, bottom = y + h - 8;
        panelScroll = Anim.approach(panelScroll, panelTarget, dt, 16f);
        ctx.enableScissor((int) x, (int) top, (int) Math.ceil(x + w), (int) Math.ceil(bottom));
        float contentH;
        if (md.getSettings().stream().noneMatch(Setting::isVisible)) {
            Fonts.drawCentered(ctx, "Ten moduł nie ma ustawień – po prostu go włącz.", Fonts.REGULAR, x + w / 2f, top + 24, TEXT_3);
            contentH = 0;
        } else {
            contentH = renderSettings(ctx, md.getSettings(), x + 18, top + 6 - panelScroll, w - 36, mx, my, dt, top, bottom);
        }
        ctx.disableScissor();
        panelMax = Math.max(0, contentH + 12 - (bottom - top));
        panelTarget = Math.max(0, Math.min(panelMax, panelTarget));
        if (panelMax > 0) {
            float trackH = bottom - top - 8;
            float barH = Math.max(18, trackH * trackH / (trackH + panelMax));
            float barY = top + 4 + (trackH - barH) * (panelScroll / panelMax);
            Render2D.round(ctx, x + w - 7, barY, 3, barH, 1.5f, Theme.accentAlpha(0x70));
        }

        m.pop();
        Render2D.alpha = baseAlpha;
    }

    // ------------------------------------------------------------------ ustawienia (wspólne)

    private static boolean useChips(ModeSetting md, float w) {
        float total = 0;
        for (String mode : md.getModes()) total += Fonts.width(mode, Fonts.SMALL) + 16;
        return md.getModes().size() > 4 || total > w;
    }

    private static float[] chipLayout(ModeSetting md, float iw) {
        List<String> modes = md.getModes();
        float[] out = new float[1 + modes.size() * 3];
        float cx = 0, cy = 0;
        for (int i = 0; i < modes.size(); i++) {
            float cw = Fonts.width(modes.get(i), Fonts.SMALL) + 18;
            if (cx > 0 && cx + cw > iw) { cx = 0; cy += CHIP_H + CHIP_GAP; }
            out[1 + i * 3] = cx; out[2 + i * 3] = cy; out[3 + i * 3] = cw;
            cx += cw + CHIP_GAP;
        }
        out[0] = cy + CHIP_H;
        return out;
    }

    /** Rysuje listę ustawień i zwraca jej wysokość. */
    private float renderSettings(DrawContext ctx, List<Setting<?>> settings, float ix, float y, float iw,
                                 float mx, float my, float dt, float clipTop, float clipBottom) {
        float ry = y;
        for (Setting<?> st : settings) {
            if (!st.isVisible()) continue;
            boolean vis = ry < clipBottom && ry > clipTop - 120;
            if (st instanceof BooleanSetting b) {
                b.anim = Anim.approach(b.anim, b.get() ? 1f : 0f, dt, 18f);
                boolean hv = my >= ry && my < ry + ROW_BOOL && mx >= ix && mx < ix + iw && my >= clipTop && my < clipBottom;
                if (hv) Render2D.round(ctx, ix - 6, ry, iw + 12, ROW_BOOL, 7, 0x08FFFFFF);
                Fonts.draw(ctx, b.getName(), Fonts.REGULAR, ix, ry + 7, hv ? TEXT : TEXT_2);
                float bw = 22, bh = 12, bx = ix + iw - bw, by = ry + 5;
                Render2D.round(ctx, bx, by, bw, bh, 6, Anim.lerpColor(TRACK, Theme.accent(), b.anim));
                Render2D.circle(ctx, bx + 6 + b.anim * 10, by + 6, 4.2f, Anim.lerpColor(0xFFB5B5BD, Theme.onAccent(), b.anim));
                if (vis) hit(ix - 6, clampY(ry, clipTop, clipBottom), ix + iw + 6, clampY(ry + ROW_BOOL, clipTop, clipBottom), k -> b.toggle());
                ry += ROW_BOOL;
            } else if (st instanceof NumberSetting n) {
                if (slider == n) n.setFromPercent((mx - sliderX) / sliderW);
                float pct = (float) n.getPercent();
                n.anim = n.anim < 0 ? pct : Anim.approach(n.anim, pct, dt, 22f);
                Fonts.draw(ctx, n.getName(), Fonts.REGULAR, ix, ry + 4, TEXT_2);
                String val = n.display();
                float vw2 = Fonts.width(val, Fonts.SEMIBOLD) + 12;
                Render2D.round(ctx, ix + iw - vw2, ry + 1, vw2, 13, 5, slider == n ? Theme.accentAlpha(0x40) : 0xFF1C1C22);
                Fonts.drawCentered(ctx, val, Fonts.SEMIBOLD, ix + iw - vw2 / 2f, ry + 4, TEXT);
                float ty2 = ry + 21;
                Render2D.round(ctx, ix, ty2, iw, 4, 2, TRACK);
                Render2D.gradientH(ctx, ix, ty2, Math.max(4, iw * n.anim), 4, 2, Theme.accent2(), Theme.accent());
                float kx = ix + iw * n.anim;
                Render2D.shadow(ctx, kx - 5, ty2 - 3, 10, 10, 5, 6, Theme.accentAlpha(0x60));
                Render2D.circle(ctx, kx, ty2 + 2, slider == n ? 6f : 5f, WHITE);
                if (vis) hit(ix - 6, clampY(ry, clipTop, clipBottom), ix + iw + 6, clampY(ry + ROW_NUM, clipTop, clipBottom), k -> {
                    if (k == 0) { slider = n; sliderX = ix; sliderW = iw; }
                });
                ry += ROW_NUM;
            } else if (st instanceof ModeSetting md && useChips(md, iw)) {
                List<String> modes = md.getModes();
                Fonts.draw(ctx, md.getName(), Fonts.REGULAR, ix, ry + 4, TEXT_2);
                float[] lay = chipLayout(md, iw);
                float cy0 = ry + 18;
                for (int i = 0; i < modes.size(); i++) {
                    float chx = ix + lay[1 + i * 3], chy = cy0 + lay[2 + i * 3], chw = lay[3 + i * 3];
                    boolean act = md.index() == i;
                    boolean hv = mx >= chx && mx < chx + chw && my >= chy && my < chy + CHIP_H;
                    Render2D.round(ctx, chx, chy, chw, CHIP_H, 7, act ? Theme.accent() : hv ? 0xFF28282F : 0xFF1C1C22);
                    Fonts.drawCentered(ctx, modes.get(i), Fonts.SMALL, chx + chw / 2f, chy + 5, act ? Theme.onAccent() : TEXT_2);
                    final int idx = i;
                    if (vis) hit(chx, clampY(chy, clipTop, clipBottom), chx + chw, clampY(chy + CHIP_H, clipTop, clipBottom), k -> md.setIndex(idx));
                }
                ry += 18 + lay[0] + 7;
            } else if (st instanceof ModeSetting md) {
                List<String> modes = md.getModes();
                Fonts.draw(ctx, md.getName(), Fonts.REGULAR, ix, ry + 4, TEXT_2);
                float segY = ry + 16, segH = 17;
                float[] segX = new float[modes.size() + 1];
                float total = 0;
                for (String mode : modes) total += Fonts.width(mode, Fonts.SMALL) + 14;
                for (int i = 0; i < modes.size(); i++) segX[i + 1] = segX[i] + (Fonts.width(modes.get(i), Fonts.SMALL) + 14) / total * iw;
                md.anim = md.anim < 0 ? md.index() : Anim.approach(md.anim, md.index(), dt, 18f);
                int lo = (int) Math.floor(md.anim), hi = Math.min(modes.size() - 1, lo + 1);
                float frac = md.anim - lo;
                float pillX = segX[lo] + (segX[hi] - segX[lo]) * frac;
                float pillW = (segX[lo + 1] - segX[lo]) + ((segX[hi + 1] - segX[hi]) - (segX[lo + 1] - segX[lo])) * frac;
                Render2D.round(ctx, ix, segY, iw, segH, 7, 0xFF1C1C22);
                Render2D.gradientV(ctx, ix + pillX + 2, segY + 2, pillW - 4, segH - 4, 5, Theme.accent(), Theme.accent2());
                for (int i = 0; i < modes.size(); i++) {
                    float act = Math.max(0f, 1f - Math.abs(md.anim - i));
                    Fonts.drawCentered(ctx, modes.get(i), Fonts.SMALL, ix + (segX[i] + segX[i + 1]) / 2f, segY + 5, Anim.lerpColor(TEXT_2, Theme.onAccent(), act));
                    final int idx = i;
                    if (vis) hit(ix + segX[i], clampY(segY, clipTop, clipBottom), ix + segX[i + 1], clampY(segY + segH, clipTop, clipBottom), k -> md.setIndex(idx));
                }
                ry += ROW_MODE;
            } else if (st instanceof ColorSetting col) {
                col.openAnim = Anim.approach(col.openAnim, col.open ? 1f : 0f, dt, 16f);
                Fonts.draw(ctx, col.getName(), Fonts.REGULAR, ix, ry + 7, TEXT_2);
                float swW = 30, swH = 14, swX = ix + iw - swW, swY = ry + 4;
                Render2D.round(ctx, swX - 1, swY - 1, swW + 2, swH + 2, 7.5f, 0x40FFFFFF);
                Render2D.round(ctx, swX, swY, swW, swH, 7, col.get() | 0xFF000000);
                Fonts.drawRight(ctx, String.format("#%06X", col.get() & 0xFFFFFF), Fonts.SMALL, swX - 7, ry + 8, TEXT_3);
                if (vis) hit(ix - 6, clampY(ry, clipTop, clipBottom), ix + iw + 6, clampY(ry + ROW_COLOR, clipTop, clipBottom), k -> col.open = !col.open);
                ry += ROW_COLOR;
                if (col.openAnim > 0.01f) {
                    float ph = COLOR_PICKER - 12, pw = iw - 22, px = ix, py = ry + 2;
                    int hueColor = 0xFF000000 | (java.awt.Color.HSBtoRGB(col.hue(), 1f, 1f) & 0xFFFFFF);
                    if (colorDrag == col) {
                        float u = Math.max(0, Math.min(1, (mx - cdX) / cdW)), v = Math.max(0, Math.min(1, (my - cdY) / cdH));
                        if (colorDragPart == 0) col.setHsv(col.hue(), u, 1 - v);
                        else col.setHsv(v, col.sat(), col.bri());
                    }
                    Render2D.gradientH(ctx, px, py, pw, ph, 7, 0xFFFFFFFF, hueColor);
                    Render2D.gradientV(ctx, px, py, pw, ph, 7, 0x00000000, 0xFF000000);
                    Render2D.outline(ctx, px, py, pw, ph, 7, 0.5f, 0x30FFFFFF);
                    float kx = px + col.sat() * pw, ky = py + (1 - col.bri()) * ph;
                    Render2D.circle(ctx, kx, ky, 5f, WHITE);
                    Render2D.circle(ctx, kx, ky, 3.6f, col.get() | 0xFF000000);
                    float hx = px + pw + 8, hw = 14;
                    for (int i = 0; i < 6; i++) {
                        int c1 = 0xFF000000 | (java.awt.Color.HSBtoRGB(i / 6f, 1, 1) & 0xFFFFFF);
                        int c2 = 0xFF000000 | (java.awt.Color.HSBtoRGB((i + 1) / 6f, 1, 1) & 0xFFFFFF);
                        Render2D.gradientV(ctx, hx, py + ph * i / 6f, hw, ph / 6f + 0.5f, 0, c1, c2);
                    }
                    Render2D.outline(ctx, hx, py, hw, ph, 3, 0.5f, 0x30FFFFFF);
                    Render2D.round(ctx, hx - 2, py + col.hue() * ph - 2, hw + 4, 4, 2, WHITE);
                    if (vis && col.open) {
                        final float fpx = px, fpy = py, fpw = pw, fph = ph, fhx = hx, fhw = hw;
                        hit(fpx, clampY(fpy, clipTop, clipBottom), fpx + fpw, clampY(fpy + fph, clipTop, clipBottom), k -> {
                            colorDrag = col; colorDragPart = 0; cdX = fpx; cdY = fpy; cdW = fpw; cdH = fph;
                        });
                        hit(fhx - 2, clampY(fpy, clipTop, clipBottom), fhx + fhw + 2, clampY(fpy + fph, clipTop, clipBottom), k -> {
                            colorDrag = col; colorDragPart = 1; cdX = fhx; cdY = fpy; cdW = fhw; cdH = fph;
                        });
                    }
                    ry += COLOR_PICKER * col.openAnim;
                }
            }
        }
        return ry - y;
    }

    // ------------------------------------------------------------------ Configs

    private void renderConfigs(DrawContext ctx, float x, float y, float w, float h, float mx, float my) {
        ConfigManager cfg = FirenClient.config();
        float lw = 220;
        Render2D.round(ctx, x, y, lw, h, 13, SIDEBAR);
        Render2D.outline(ctx, x, y, lw, h, 13, 0.5f, STROKE);
        Fonts.draw(ctx, "Configi", Fonts.TITLE, x + 16, y + 16, WHITE);
        Fonts.draw(ctx, "Zapisuj i wczytuj zestawy ustawień.", Fonts.SMALL, x + 16, y + 36, TEXT_3);
        Fonts.draw(ctx, "Aktywny zapisuje się automatycznie.", Fonts.SMALL, x + 16, y + 46, TEXT_3);

        float fy = y + 64;
        Fonts.draw(ctx, "NAZWA NOWEGO CONFIGU", Fonts.SMALL, x + 16, fy, TEXT_3);
        float fx = x + 14, fw = lw - 28;
        Render2D.round(ctx, fx, fy + 12, fw, 26, 9, FIELD);
        Render2D.outline(ctx, fx, fy + 12, fw, 26, 9, 0.5f, editingName ? Theme.accentAlpha(0xA0) : STROKE);
        boolean caret = (System.currentTimeMillis() / 530) % 2 == 0;
        if (nameField.isEmpty() && !editingName) Fonts.draw(ctx, "np. pvp, bedwars…", Fonts.REGULAR, fx + 10, fy + 21, TEXT_3);
        else Fonts.draw(ctx, nameField + (editingName && caret ? "|" : ""), Fonts.REGULAR, fx + 10, fy + 21, TEXT);
        hit(fx, fy + 12, fx + fw, fy + 38, b -> editingName = true);

        float by = fy + 46;
        button(ctx, fx, by, fw, 26, Fonts.I_SAVE, "Zapisz jako nowy", true, mx, my, () -> {
            String n = nameField.isBlank() ? "config" + (cfg.list().size() + 1) : nameField;
            if (cfg.saveAs(n)) showToast("Zapisano config: " + ConfigManager.sanitize(n));
            nameField = "";
            editingName = false;
        });
        button(ctx, fx, by + 32, fw, 26, Fonts.I_IMPORT, "Importuj ze schowka", false, mx, my, () -> {
            String n = nameField.isBlank() ? "import" + (cfg.list().size() + 1) : nameField;
            showToast(cfg.importFromClipboard(n) ? "Zaimportowano: " + ConfigManager.sanitize(n) : "W schowku nie ma configu FirenClient");
            nameField = "";
        });
        button(ctx, fx, by + 64, fw, 26, Fonts.I_EXPORT, "Kopiuj obecny do schowka", false, mx, my, () -> {
            cfg.exportToClipboard();
            showToast("Skopiowano config do schowka");
        });
        button(ctx, fx, by + 96, fw, 26, Fonts.I_FOLDER, "Otwórz folder", false, mx, my, () -> {
            try {
                java.nio.file.Files.createDirectories(cfg.folder());
                Util.getOperatingSystem().open(cfg.folder().toFile());
            } catch (Exception ignored) {
            }
        });
        if (by + 96 + 26 + 34 < y + h) {
            Fonts.draw(ctx, "Aktywny config zapisuje się sam", Fonts.SMALL, x + 16, y + h - 30, TEXT_3);
            Fonts.draw(ctx, "przy zamknięciu menu i gry.", Fonts.SMALL, x + 16, y + h - 19, TEXT_3);
        }

        float px = x + lw + 10, pw = w - lw - 10;
        Render2D.gradientV(ctx, px, y, pw, h, 13, CARD, 0xFF0D0D11);
        Render2D.outline(ctx, px, y, pw, h, 13, 0.5f, STROKE);
        List<String> list = cfg.list();
        Fonts.draw(ctx, "TWOJE CONFIGI", Fonts.SMALL, px + 16, y + 14, TEXT_3);
        Fonts.drawRight(ctx, String.valueOf(list.size()), Fonts.SMALL, px + pw - 16, y + 14, TEXT_3);
        float ry = y + 30;
        for (String name : list) {
            if (ry + 34 > y + h - 6) break;
            boolean act = name.equals(cfg.active());
            boolean hv = mx >= px + 10 && mx < px + pw - 10 && my >= ry && my < ry + 34;
            Render2D.round(ctx, px + 10, ry, pw - 20, 34, 10, act ? 0xFF1B1B22 : hv ? 0xFF16161B : 0xFF121216);
            Render2D.outline(ctx, px + 10, ry, pw - 20, 34, 10, 0.5f, act ? Theme.accentAlpha(0x70) : STROKE);
            Render2D.gradientV(ctx, px + 18, ry + 7, 20, 20, 7, act ? Theme.accent() : 0xFF2A2A32, act ? Theme.accent2() : 0xFF1E1E24);
            Fonts.drawCentered(ctx, Fonts.I_LAYERS, Fonts.ICONS, px + 28, ry + 13.5f, act ? Theme.onAccent() : TEXT_2);
            Fonts.draw(ctx, name, Fonts.SEMIBOLD, px + 46, ry + 9, TEXT);
            Fonts.draw(ctx, act ? "aktywny" : "kliknij Wczytaj", Fonts.SMALL, px + 46, ry + 20, act ? Theme.accent() : TEXT_3);
            float bx = px + pw - 18;
            bx = iconButton(ctx, bx, ry + 8, Fonts.I_TRASH, 0xFFFF5C6A, mx, my, () -> {
                if (cfg.delete(name)) showToast("Usunięto: " + name);
            });
            bx = iconButton(ctx, bx, ry + 8, Fonts.I_SAVE, TEXT_2, mx, my, () -> {
                if (cfg.saveAs(name)) showToast("Nadpisano: " + name);
            });
            textButton(ctx, bx, ry + 8, "Wczytaj", act, mx, my, () -> {
                if (cfg.load(name)) showToast("Wczytano: " + name);
            });
            ry += 40;
        }
    }

    private void button(DrawContext ctx, float x, float y, float w, float h, String icon, String label, boolean primary,
                        float mx, float my, Runnable action) {
        boolean hv = mx >= x && mx < x + w && my >= y && my < y + h;
        if (primary) {
            if (hv) Render2D.shadow(ctx, x, y + 1, w, h, 9, 10, Theme.accentAlpha(0x60));
            Render2D.gradientV(ctx, x, y, w, h, 9, Theme.accent(), Theme.accent2());
        } else {
            Render2D.round(ctx, x, y, w, h, 9, hv ? 0xFF22222A : 0xFF18181E);
            Render2D.outline(ctx, x, y, w, h, 9, 0.5f, STROKE);
        }
        int col = primary ? Theme.onAccent() : hv ? TEXT : TEXT_2;
        Fonts.draw(ctx, icon, Fonts.ICONS, x + 12, y + h / 2f - 3.5f, col);
        Fonts.draw(ctx, label, Fonts.SEMIBOLD, x + 28, y + h / 2f - 4, col);
        hit(x, y, x + w, y + h, b -> action.run());
    }

    private float iconButton(DrawContext ctx, float right, float y, String icon, int color, float mx, float my, Runnable action) {
        float x = right - 18;
        boolean hv = mx >= x && mx < x + 18 && my >= y && my < y + 18;
        Render2D.round(ctx, x, y, 18, 18, 6, hv ? 0xFF2A2A32 : 0xFF1C1C22);
        Fonts.drawCentered(ctx, icon, Fonts.ICONS, x + 9, y + 5.5f, hv ? color : TEXT_3);
        hit(x, y, x + 18, y + 18, b -> action.run());
        return x - 5;
    }

    private float textButton(DrawContext ctx, float right, float y, String label, boolean disabled, float mx, float my, Runnable action) {
        float w = Fonts.width(label, Fonts.SEMIBOLD) + 18, x = right - w;
        boolean hv = !disabled && mx >= x && mx < x + w && my >= y && my < y + 18;
        if (disabled) Render2D.round(ctx, x, y, w, 18, 6, 0xFF1C1C22);
        else Render2D.gradientV(ctx, x, y, w, 18, 6, hv ? Theme.accent() : Theme.accentAlpha(0xCC), Theme.accent2());
        Fonts.drawCentered(ctx, label, Fonts.SEMIBOLD, x + w / 2f, y + 5, disabled ? TEXT_3 : Theme.onAccent());
        if (!disabled) hit(x, y, x + w, y + 18, b -> action.run());
        return x - 5;
    }

    // ------------------------------------------------------------------ Theme

    private void renderThemeTab(DrawContext ctx, float x, float y, float w, float h, float mx, float my, float dt) {
        float half = (w - 10) / 2f;
        Module[] mods = {FirenClient.modules().get(ClickGui.class), FirenClient.modules().get(Hud.class)};
        String[] titles = {"Wygląd", "HUD"};
        String[] subs = {"Kolor akcentu, motyw i tło menu.", "Elementy na ekranie gry."};
        for (int i = 0; i < 2; i++) {
            float bx = x + i * (half + 10);
            Render2D.gradientV(ctx, bx, y, half, h, 13, CARD, 0xFF0D0D11);
            Render2D.outline(ctx, bx, y, half, h, 13, 0.5f, STROKE);
            Fonts.draw(ctx, titles[i], Fonts.TITLE, bx + 18, y + 16, WHITE);
            Fonts.draw(ctx, subs[i], Fonts.SMALL, bx + 18, y + 36, TEXT_3);
            Render2D.round(ctx, bx + 18, y + 50, half - 36, 0.5f, 0, 0x14FFFFFF);
            ctx.enableScissor((int) bx, (int) (y + 54), (int) Math.ceil(bx + half), (int) Math.ceil(y + h - 6));
            renderSettings(ctx, mods[i].getSettings(), bx + 18, y + 60, half - 36, mx, my, dt, y + 54, y + h - 6);
            ctx.disableScissor();
        }
        float px = x + half - 70, py = y + 18;
        Render2D.shadow(ctx, px, py, 52, 12, 6, 10, Theme.accentAlpha(0x70));
        Render2D.gradientH(ctx, px, py, 52, 12, 6, Theme.accent2(), Theme.accent());
    }

    // ------------------------------------------------------------------ status + toast

    private void renderStatus(DrawContext ctx, float x, float y, float W) {
        Render2D.round(ctx, x + 14, y, W - 28, 0.5f, 0, 0x10FFFFFF);
        long on = FirenClient.modules().all().stream().filter(Module::isEnabled).count();
        int total = FirenClient.modules().all().size();
        Render2D.circle(ctx, x + 22, y + 12, 2.5f, Theme.accent());
        Fonts.draw(ctx, on + " / " + total + " modułów aktywnych", Fonts.SMALL, x + 30, y + 9, TEXT_2);
        Fonts.drawCentered(ctx, "Config: " + FirenClient.config().active(), Fonts.SMALL, x + W / 2f, y + 9, TEXT_2);
        String right = "FirenClient " + FirenClient.VERSION + "  ·  " + FirenClient.openGuiKey().getBoundKeyLocalizedText().getString();
        Fonts.drawRight(ctx, right, Fonts.SMALL, x + W - 18, y + 9, TEXT_3);
    }

    private void renderToast(DrawContext ctx, float x, float y, float W, float H) {
        if (toast == null) return;
        long age = System.currentTimeMillis() - toastAt;
        if (age > 2600) { toast = null; return; }
        float a = Math.min(1f, Math.min(age / 150f, (2600 - age) / 300f));
        float base = Render2D.alpha;
        Render2D.alpha = base * a;
        float tw = Fonts.width(toast, Fonts.SEMIBOLD) + 40, tx = x + (W - tw) / 2f, ty = y + H - STATUS - 40 + (1 - a) * 8;
        Render2D.shadow(ctx, tx, ty + 2, tw, 26, 10, 12, 0x90000000);
        Render2D.round(ctx, tx, ty, tw, 26, 10, 0xF21A1A20);
        Render2D.outline(ctx, tx, ty, tw, 26, 10, 0.5f, Theme.accentAlpha(0x80));
        Fonts.draw(ctx, Fonts.I_CHECK, Fonts.ICONS, tx + 12, ty + 9.5f, Theme.accent());
        Fonts.draw(ctx, toast, Fonts.SEMIBOLD, tx + 27, ty + 9, TEXT);
        Render2D.alpha = base;
    }

    // ================================================================== input

    @Override
    public boolean mouseClicked(double rawX, double rawY, int button) {
        if (closingAt > 0) return true;
        double mx = rawX / s, my = rawY / s;
        boolean wasEditing = editingName;
        editingName = false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (mx >= h.x1 && mx < h.x2 && my >= h.y1 && my < h.y2) {
                h.action.accept(button);
                return true;
            }
        }
        if (wasEditing) return true;
        return super.mouseClicked(rawX, rawY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingWindow = false;
        slider = null;
        colorDrag = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double rawX, double rawY, double horizontal, double vertical) {
        double mx = rawX / s, my = rawY / s;
        if (mx >= listLeft && mx < listRight && my >= listTop && my < listBottom) {
            listTarget = (float) Math.max(0, Math.min(listMax, listTarget - vertical * 33));
        } else {
            panelTarget = (float) Math.max(0, Math.min(panelMax, panelTarget - vertical * 36));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding != null) {
            boolean clear = keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE;
            binding.setKey(clear ? GLFW.GLFW_KEY_UNKNOWN : keyCode);
            showToast(clear ? "Usunięto bind: " + binding.getName() : "Bind " + binding.getName() + ": " + keyName(keyCode));
            binding = null;
            return true;
        }
        if (editingName) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !nameField.isEmpty()) {
                nameField = nameField.substring(0, nameField.length() - 1);
            } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                String n = nameField.isBlank() ? "config" : nameField;
                if (FirenClient.config().saveAs(n)) showToast("Zapisano config: " + ConfigManager.sanitize(n));
                nameField = "";
                editingName = false;
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editingName = false;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
            query = query.substring(0, query.length() - 1);
            listTarget = 0;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && !query.isEmpty()) {
            query = "";
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || FirenClient.openGuiKey().matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (binding != null) return true;
        if (editingName) {
            if (nameField.length() < 24 && (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-' || chr == ' ')) nameField += chr;
            return true;
        }
        if (query.length() < 20 && (Character.isLetterOrDigit(chr) || chr == ' ')) {
            query += chr;
            listTarget = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        if (closingAt < 0) closingAt = System.nanoTime();
    }

    @Override
    public void removed() {
        Render2D.alpha = 1f;
        FirenClient.config().save();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
