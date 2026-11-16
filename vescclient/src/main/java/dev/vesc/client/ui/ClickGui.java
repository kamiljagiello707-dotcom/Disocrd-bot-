package dev.vesc.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.vesc.client.VescClient;
import dev.vesc.client.config.ClientSettings;
import dev.vesc.client.hud.Keys;
import dev.vesc.client.lang.Lang;
import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;
import dev.vesc.client.render.Fonts;
import dev.vesc.client.render.Fonts.Font;
import dev.vesc.client.render.R2D;
import dev.vesc.client.setting.BoolSetting;
import dev.vesc.client.setting.ModeSetting;
import dev.vesc.client.setting.NumberSetting;
import dev.vesc.client.setting.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.IntConsumer;

/**
 * Nowoczesne biało-czarne ClickGUI VescClient.
 * Działa w trybie "immediate": podczas renderu zapisywane są obszary kliknięć (Hit),
 * a mouseClicked sprawdza je w odwrotnej kolejności.
 */
public class ClickGui extends Screen {
    // wymiary okna (przed skalowaniem)
    private static final float SB = 158, PAD = 18, IW = 214;
    /** rozmiar okna – zmieniany przeciąganiem prawego dolnego rogu i zapisywany w konfiguracji */
    private float W = 640, H = 420;

    // ---- zaawansowane funkcje GUI
    private enum Filter { ALL, ENABLED, DISABLED, BOUND }
    private enum Sort { DEFAULT, NAME, ENABLED }
    private static Filter filter = Filter.ALL;
    private static Sort sort = Sort.DEFAULT;
    private static boolean listView = false;
    private static Module inspected;
    private Module lastInspected;
    private final Anim inspectorA = new Anim(0, 14);
    private final Anim inspScroll = new Anim(0, 16);
    private float inspScrollTarget, inspMax;
    private float inspX, inspY, inspW, inspH;
    private long listTime = System.currentTimeMillis();
    private final Map<Module, long[]> ripples = new HashMap<>();
    private Module tooltipModule, hoveredCard;
    private long tooltipStart;
    private int focusIndex = -1, lastCols = 2;
    private List<Module> shownModules = new ArrayList<>();
    private NumberSetting editing;
    private String editBuffer = "";
    private dev.vesc.client.setting.ColorSetting openColor, draggingColor;
    private int colorPart;
    private float cpX, cpY, cpW, cpH;
    private boolean palette, paletteDrawing;
    private String paletteQuery = "";
    private int paletteSel;
    private final Anim paletteA = new Anim(0, 18);
    private List<PItem> paletteItems = new ArrayList<>();
    private record PItem(String icon, String title, String sub, Runnable action, Module module, boolean keepOpen) {}
    private boolean resizing;
    private float rsW, rsH, rsMx, rsMy;

    // ---- układ Panels (3.5)
    private static final float PW = 128, PHEAD = 22;
    private static final List<Category> panelOrder = new ArrayList<>(List.of(Category.values()));
    private Category draggingPanel;
    private float panGrabX, panGrabY, vLeft, vTop, vW, vH;
    private final Map<Category, Anim> panelScroll = new HashMap<>();
    private final Map<Category, float[]> panelScrollT = new HashMap<>(); // {cel, max}
    private final Map<Category, float[]> panelBody = new HashMap<>();    // {x, y, w, h}
    private final Map<Module, Float> setHeights = new HashMap<>();

    private boolean panelMode() { return rc.settings.layout.get().equals("Panels"); }

    private float[] panelPos(Category c) {
        int idx = c.ordinal(), n = Category.values().length;
        float[] pp = rc.settings.panels.computeIfAbsent(c.name(), k -> {
            // domyślnie: jeden rząd dopasowany do szerokości ekranu, a gdy się nie mieści – dwa rzędy
            // (liczone od docelowej skali, nie od skali w trakcie animacji otwierania)
            float vW = width / rc.settings.guiScale();
            float gap = Math.min(12, (vW - 24 - n * PW) / (n - 1));
            if (gap >= 4) {
                float total = n * PW + (n - 1) * gap;
                return new float[]{(vW - total) / 2f + idx * (PW + gap), 46, 0};
            }
            int perRow = (n + 1) / 2;
            float g2 = Math.max(4, Math.min(12, (vW - 24 - perRow * PW) / (perRow - 1)));
            float total = perRow * PW + (perRow - 1) * g2;
            return new float[]{(vW - total) / 2f + (idx % perRow) * (PW + g2), 46 + (idx / perRow) * 190, 0};
        });
        pp[0] = Math.max(0, Math.min(vW - PW, pp[0]));
        pp[1] = Math.max(0, Math.min(vH - PHEAD, pp[1]));
        return pp;
    }
    private static final long OPEN_MS = 320, CLOSE_MS = 200;

    private enum Page { HOME, CATEGORY, FAVORITES, FRIENDS, HUD, THEMES, CONFIG, PROFILE, SETTINGS }

    // nawigacja zapamiętana między otwarciami
    private static Page page = Page.HOME;
    private static Category category = Category.COMBAT;
    private static float dragX = 0, dragY = 0;

    private final VescClient rc = VescClient.get();
    private final Map<String, Anim> anims = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private final List<Object[]> sliders = new ArrayList<>();
    private String friendInput = "";
    private boolean friendFocused;
    private final Anim scroll = new Anim(0, 16);
    private final Anim indicatorY = new Anim(-1, 16);
    private final Anim indicatorA = new Anim(0, 14);
    private float scrollTarget = 0, maxScroll = 0;
    private long openTime, closeTime, pageTime;
    private boolean closing;

    private String search = "";
    private boolean searchFocused;
    private Module binding;
    private NumberSetting dragging;
    private float dragTrackX, dragTrackW;
    private boolean draggingWindow;
    private float dragStartMx, dragStartMy, dragStartX, dragStartY;

    // transformacja okna
    private float x0, y0, scale = 1, centerX, centerY;
    private float mx, my;      // mysz w przestrzeni okna
    private float[] clip;      // aktualny obszar przycięcia (dla hover/hit)

    private record Hit(float x, float y, float w, float h, float[] clip, IntConsumer action) {}

    public ClickGui() {
        super(Text.literal("VescClient"));
        openTime = System.currentTimeMillis();
        pageTime = openTime;
    }

    @Override public boolean shouldPause() { return false; }

    // ================================================================== render

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float speed = Anim.globalSpeed;
        float p = Anim.easeOutCubic((now - openTime) / (OPEN_MS / speed));
        if (closing) {
            float c = (now - closeTime) / (CLOSE_MS / speed);
            p = Math.min(p, 1 - Anim.easeInCubic(c));
            if (c >= 1) { R2D.alpha = 1; finishClose(); return; }
        }

        // tło
        if (rc.settings.blur.get() && client != null && client.world != null && p > 0.6f && !closing && !spatial()) applyBlur(delta);
        R2D.alpha = 1;
        boolean sp = spatial();
        R2D.rect(ctx, 0, 0, width, height, R2D.withAlpha(0xFF000000, (sp ? 0.16f : 0.45f) * p));
        if (sp) spatialM = buildSpatial(p);

        // skala i pozycja
        ClientSettings s = rc.settings;
        W = s.guiW; H = s.guiH;
        centerX = width / 2f; centerY = height / 2f;
        if (panelMode()) {
            scale = s.guiScale() * (0.94f + 0.06f * p);
            vW = width / scale; vH = height / scale; vLeft = centerX - vW / 2f; vTop = centerY - vH / 2f;
            W = 640; H = Math.max(300, Math.min(440, vH - 80));
            x0 = vLeft + (vW - W) / 2f; y0 = vTop + 44;
        } else {
            float fit = Math.min(1f, Math.min((width - 24) / W, (height - 50) / H));
            float base = Math.min(fit, s.guiScale() * Math.min(1.25f, Math.max(0.85f, Math.min(width / 600f, height / 380f))));
            base = Math.min(base, fit);
            scale = base * (0.9f + 0.1f * p);
            x0 = (width - W) / 2f + dragX;
            y0 = (height - H) / 2f + dragY + (1 - p) * 14;
        }
        float[] fm = flatMouse(mouseX, mouseY);
        mx = (fm[0] - centerX) / scale + centerX;
        my = (fm[1] - centerY) / scale + centerY;

        if (draggingWindow) {
            dragX = dragStartX + (mx - dragStartMx) * 1f;
            dragY = dragStartY + (my - dragStartMy) * 1f;
        }
        if (resizing) {
            s.guiW = Math.max(560, Math.min(1100, rsW + (mx - rsMx) * 2));
            s.guiH = Math.max(410, Math.min(700, rsH + (my - rsMy) * 2));
            W = s.guiW; H = s.guiH;
            x0 = (width - W) / 2f + dragX;
            y0 = (height - H) / 2f + dragY;
        }
        if (draggingPanel != null) {
            float[] pp = panelPos(draggingPanel);
            pp[0] = Math.max(0, Math.min(vW - PW, mx - vLeft - panGrabX));
            pp[1] = Math.max(0, Math.min(vH - PHEAD, my - vTop - panGrabY));
        }
        if (draggingColor != null) {
            if (colorPart == 0) draggingColor.setHsb(draggingColor.hue, Anim.clamp01((mx - cpX) / cpW), 1 - Anim.clamp01((my - cpY) / cpH));
            else draggingColor.setHsb(Anim.clamp01((my - cpY) / cpH), draggingColor.sat, draggingColor.bri);
        }
        if (dragging != null) {
            dragging.set(dragging.min + (dragging.max - dragging.min) * Anim.clamp01((mx - dragTrackX) / dragTrackW));
            if (dragging == s.animSpeed) Anim.globalSpeed = s.animSpeed.get().floatValue();
        }

        hits.clear();
        sliders.clear();
        clip = null;
        R2D.alpha = p;
        R2D.radiusMul = Theme.radiusScale();
        if (sp) beginSpatial(ctx);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(centerX, centerY, 0);
        ctx.getMatrices().scale(scale, scale, 1);
        ctx.getMatrices().translate(-centerX, -centerY, 0);

        if (panelMode()) drawPanels(ctx); else drawWindow(ctx);

        ctx.getMatrices().pop();
        if (sp) endSpatial(ctx);
        R2D.alpha = 1;
        R2D.radiusMul = 1f;
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private static final float[][] PARTICLES = new float[46][];
    static {
        Random r = new Random(7);
        for (int i = 0; i < PARTICLES.length; i++)
            PARTICLES[i] = new float[]{r.nextFloat(), r.nextFloat(), (r.nextFloat() - 0.5f) * 0.012f, -0.004f - r.nextFloat() * 0.012f, 0.6f + r.nextFloat() * 1.6f, r.nextFloat() * 6.28f};
    }
    private long lastParticle = System.nanoTime();

    private void drawBackgroundFx(DrawContext ctx) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastParticle) / 1e9f);
        lastParticle = now;
        // poświata za oknem
        R2D.shadow(ctx, x0 + 40, y0 + 40, W - 80, H - 80, 120, R2D.withAlpha(Theme.accentWave(0), 0.06f));
        float t = (System.currentTimeMillis() % 100000) / 1000f;
        for (float[] p : PARTICLES) {
            p[0] += p[2] * dt; p[1] += p[3] * dt;
            if (p[1] < -0.02f) { p[1] = 1.02f; p[0] = (float) Math.random(); }
            if (p[0] < -0.02f) p[0] = 1.02f; else if (p[0] > 1.02f) p[0] = -0.02f;
            float px = p[0] * width, py = p[1] * height;
            // przestrzeń okna (przeciw skali GUI)
            px = (px - centerX) / scale + centerX; py = (py - centerY) / scale + centerY;
            float tw = 0.5f + 0.5f * (float) Math.sin(t * 1.3f + p[5]);
            R2D.circle(ctx, px, py, p[4], R2D.withAlpha(Theme.accentWave(p[5]), 0.12f + 0.25f * tw));
        }
    }

    private void drawWindow(DrawContext ctx) {
        if (rc.settings.particles.get()) drawBackgroundFx(ctx);
        // cień + tło
        R2D.shadow(ctx, x0, y0, W, H, 26, Theme.shadow());
        R2D.roundRect(ctx, x0, y0, W, H, 14, Theme.bg());
        // sidebar (przycięty scissorem – bez nakładania warstw, ważne przy półprzezroczystym szkle)
        R2D.scissor(ctx, x0, y0, SB, H);
        R2D.roundRect(ctx, x0, y0, SB + 14, H, 14, Theme.sidebar());
        R2D.endScissor(ctx);
        R2D.rect(ctx, x0 + SB, y0 + 12, 0.5f, H - 24, Theme.stroke());
        float g = Theme.glass();
        if (g > 0.01f) {
            // połysk szkła u góry i poświaty w kolorach akcentu
            R2D.roundRectV(ctx, x0, y0, W, 70, 14, R2D.withAlpha(0xFFFFFFFF, 0.06f * g), 0x00FFFFFF);
            R2D.scissor(ctx, x0, y0, W, H);
            R2D.glow(ctx, x0 - 80, y0 - 90, 260, 220, R2D.withAlpha(Theme.accentWave(0), 0.10f * g));
            R2D.glow(ctx, x0 + W - 200, y0 + H - 150, 280, 240, R2D.withAlpha(Theme.accent2(), 0.08f * g));
            R2D.endScissor(ctx);
        }
        R2D.roundOutline(ctx, x0, y0, W, H, 14, 0.6f, Theme.stroke());
        if (g > 0.01f) R2D.roundOutlineGrad(ctx, x0, y0, W, H, 14, 0.8f, R2D.withAlpha(Theme.accentWave(0), 0.55f * g), R2D.withAlpha(Theme.accent2(), 0.35f * g));
        float n = Theme.neon();
        if (n > 0.01f) {
            // Neon: świecący obrys z gradientem, poświata dookoła okna i linia akcentu pod logo
            R2D.roundOutlineGrad(ctx, x0, y0, W, H, 14, 1.1f, R2D.withAlpha(Theme.accentWave(0), n), R2D.withAlpha(Theme.accent2(), 0.8f * n));
            R2D.scissor(ctx, x0, y0, W, H);
            R2D.glow(ctx, x0 - 120, y0 - 120, 360, 300, R2D.withAlpha(Theme.accentWave(0), 0.13f * n));
            R2D.glow(ctx, x0 + W - 260, y0 + H - 200, 380, 320, R2D.withAlpha(Theme.accent2(), 0.10f * n));
            R2D.endScissor(ctx);
            R2D.gradientH(ctx, x0 + 14, y0 + 50, SB - 28, 0.8f, R2D.withAlpha(Theme.accentWave(0), 0.9f * n), R2D.withAlpha(Theme.accent2(), 0f));
        }

        drawSidebar(ctx);
        drawContent(ctx);
        drawFooter(ctx);
        drawResizeGrip(ctx);
        drawTooltip(ctx);
        drawPalette(ctx);
    }

    // ================================================================== sidebar

    private void drawSidebar(DrawContext ctx) {
        float x = x0, y = y0;
        // logo – przeciągnięcie okna
        R2D.icon(ctx, "logo", x + 16, y + 16, 24, Theme.accentWave(0));
        Fonts.draw(ctx, "VescClient", x + 46, y + 17, Theme.text(), Font.BOLD, 1.12f);
        Fonts.draw(ctx, "v" + VescClient.VERSION + "  •  Fabric 1.21.1", x + 46, y + 29, Theme.textFaint(), Font.REGULAR, 0.68f);
        hit(x, y, SB, 50, b -> {
            if (b == 0) { draggingWindow = true; dragStartMx = mx; dragStartMy = my; dragStartX = dragX; dragStartY = dragY; }
        });

        float iy = y + 54;
        float itemH = 19;
        float selY = -1;
        List<Runnable> drawLater = new ArrayList<>();
        List<Object[]> entries = new ArrayList<>();
        entries.add(new Object[]{"section", Lang.get("section.overview")});
        entries.add(new Object[]{"misc", Lang.get("page.home"), Page.HOME, null});
        entries.add(new Object[]{"star", Lang.get("cat.favorites"), Page.FAVORITES, null});
        entries.add(new Object[]{"section", Lang.get("section.modules")});
        for (Category c : Category.values()) entries.add(new Object[]{c.icon, c.getName(), Page.CATEGORY, c});

        int fi = 0;
        for (Object[] e : entries) {
            if (e[0].equals("section")) {
                iy += 5;
                Fonts.draw(ctx, (String) e[1], x + 18, iy, Theme.textFaint(), Font.SEMIBOLD, 0.58f);
                iy += 10;
                continue;
            }
            Page pg = (Page) e[2];
            Category cat = (Category) e[3];
            boolean selected = page == pg && ((pg != Page.CATEGORY && pg != Page.FAVORITES) || ((pg == Page.FAVORITES || cat == category) && search.isEmpty()));
            if (selected) selY = iy;
            float fy = iy;
            final int idx = fi++;
            drawLater.add(() -> {
                boolean hov = hovered(x + 10, fy, SB - 20, itemH);
                float ha = anim("nav" + idx, 18).animate(hov && !selected ? 1 : 0);
                R2D.roundRect(ctx, x + 10, fy, SB - 20, itemH, 7, R2D.withAlpha(Theme.cardHover(), ha));
                float near = 1 - Anim.clamp01(Math.abs(indicatorY.raw() - fy) / itemH);
                near *= indicatorA.raw();
                int fg = R2D.blend(R2D.blend(Theme.textDim(), Theme.text(), ha), Theme.accentText(), near);
                float off = anim("navoff" + idx, 16).animate(selected ? 2 : (hov ? 1 : 0));
                // kafelek ikony w kolorze kategorii
                int tint = cat != null ? catColor(cat) : Theme.accent();
                float tx = x + 16 + off, ty = fy + 2.5f;
                // kafelek ikony: wygaszony kolor kategorii, przy zaznaczeniu wypełniony pełnym kolorem
                if (near > 0.3f) R2D.glow(ctx, tx - 4, ty - 4, 22, 22, R2D.withAlpha(tint, 0.3f * near));
                R2D.roundRectV(ctx, tx, ty, 14, 14, 5,
                        R2D.blend(R2D.withAlpha(tint, 0.18f + 0.1f * ha), R2D.blend(tint, 0xFFFFFFFF, 0.12f), near),
                        R2D.blend(R2D.withAlpha(tint, 0.18f + 0.1f * ha), tint, near));
                R2D.icon(ctx, (String) e[0], tx + 3, ty + 3, 8, R2D.blend(R2D.blend(tint, 0xFFFFFFFF, 0.2f), Theme.contrast(tint), near));
                Fonts.drawV(ctx, (String) e[1], tx + 19, fy + itemH / 2f + 0.5f, fg, selected ? Font.BOLD : Font.SEMIBOLD, 0.82f);
                if (pg == Page.CATEGORY || pg == Page.FAVORITES) {
                    List<Module> ms = cat != null ? rc.modules.byCategory(cat) : favorites();
                    long en = ms.stream().filter(Module::isEnabled).count();
                    String t = en > 0 ? String.valueOf(en) : String.valueOf(ms.size());
                    float tw = Fonts.width(t, Font.BOLD, 0.6f) + 7;
                    float bx = x + SB - 16 - tw;
                    if (en > 0) {
                        R2D.roundRect(ctx, bx, fy + 4.5f, tw, 10, 5, R2D.blend(R2D.withAlpha(tint, 0.22f), Theme.accentText(), near));
                        Fonts.drawV(ctx, t, bx + 3.5f, fy + 9.5f, R2D.blend(R2D.blend(tint, 0xFFFFFFFF, 0.25f), Theme.accent(), near), Font.BOLD, 0.6f);
                    } else Fonts.drawV(ctx, t, bx + 3.5f, fy + 9.5f, R2D.blend(Theme.textFaint(), Theme.accentText(), near * 0.7f), Font.SEMIBOLD, 0.6f);
                }
            });
            hit(x + 10, iy, SB - 20, itemH, b -> {
                if (b != 0) return;
                setPage(pg);
                if (cat != null) category = cat;
                if (pg == Page.CATEGORY || pg == Page.FAVORITES) { search = ""; searchFocused = false; }
            });
            iy += itemH + 1;
        }

        // wskaźnik zaznaczenia (płynnie przesuwana pigułka)
        if (selY >= 0 && indicatorY.raw() < 0) indicatorY.snap(selY);
        float ia = indicatorA.animate(selY >= 0 ? 1 : 0);
        float iyv = indicatorY.animate(selY >= 0 ? selY : indicatorY.target());
        if (Theme.neon() > 0.01f) R2D.glow(ctx, x + 2, iyv - 10, SB - 4, itemH + 20, R2D.withAlpha(Theme.accentWave(0), 0.35f * ia * Theme.neon()));
        R2D.shadow(ctx, x + 12, iyv + 2, SB - 24, itemH, 8, R2D.withAlpha(Theme.accent(), 0.35f * ia));
        R2D.roundRectV(ctx, x + 10, iyv, SB - 20, itemH, 7, R2D.withAlpha(R2D.blend(Theme.accent(), 0xFFFFFFFF, 0.12f), ia), R2D.withAlpha(Theme.accent(), ia));
        for (Runnable r : drawLater) r.run();

        drawDockBar(ctx, x + 10, y0 + H - 46 - 30, SB - 20, 24);
        drawProfileCard(ctx, x + 10, y0 + H - 46, SB - 20, 36);
    }

    /** pasek szybkich skrótów nad profilem: Znajomi, HUD, Motywy, Konfiguracje */
    private void drawDockBar(DrawContext ctx, float x, float y, float w, float h) {
        Object[][] items = {{"users", "page.friends", Page.FRIENDS}, {"hud", "page.hud", Page.HUD}, {"themes", "page.themes", Page.THEMES}, {"config", "page.config", Page.CONFIG}};
        R2D.roundRect(ctx, x, y, w, h, 9, Theme.card());
        R2D.roundOutline(ctx, x, y, w, h, 9, 0.5f, Theme.stroke());
        float bw = (w - 6) / items.length;
        String label = null;
        float lx = 0;
        for (int i = 0; i < items.length; i++) {
            Page pg = (Page) items[i][2];
            float bx = x + 3 + i * bw;
            boolean act = page == pg, hv = hovered(bx, y + 3, bw, h - 6);
            float aa = anim("dock" + i, 16).animate(act ? 1 : 0), ha = anim("dockh" + i, 18).animate(hv ? 1 : 0);
            R2D.roundRect(ctx, bx + 1, y + 3, bw - 2, h - 6, 6, R2D.blend(R2D.withAlpha(Theme.cardHover(), ha), Theme.accent(), aa));
            R2D.icon(ctx, (String) items[i][0], bx + bw / 2f - 4.5f, y + h / 2f - 4.5f, 9, R2D.blend(R2D.blend(Theme.textDim(), Theme.text(), ha), Theme.accentText(), aa));
            if (hv) { label = Lang.get((String) items[i][1]); lx = bx + bw / 2f; }
            hit(bx, y + 3, bw, h - 6, b -> { if (b == 0) setPage(pg); });
        }
        if (label != null) {
            float tw = Fonts.width(label, Font.SEMIBOLD, 0.62f) + 12;
            float tx = Math.max(x, Math.min(x + w - tw, lx - tw / 2f));
            R2D.shadow(ctx, tx, y - 18, tw, 14, 6, Theme.shadow());
            R2D.roundRect(ctx, tx, y - 18, tw, 14, 7, opaque(Theme.bg()));
            R2D.roundOutline(ctx, tx, y - 18, tw, 14, 7, 0.5f, Theme.stroke());
            Fonts.drawV(ctx, label, tx + 6, y - 11, Theme.text(), Font.SEMIBOLD, 0.62f);
        }
    }

    /** kolor tła bez przezroczystości (dymki, menu – żeby nic nie prześwitywało) */
    private static int opaque(int c) { return 0xF7000000 | (c & 0xFFFFFF); }

    private void drawProfileCard(DrawContext ctx, float x, float y, float w, float h) {
        boolean hov = hovered(x, y, w, h);
        boolean active = page == Page.PROFILE || page == Page.SETTINGS;
        float ha = anim("profile", 16).animate(hov || active ? 1 : 0);
        R2D.roundRect(ctx, x, y, w, h, 9, R2D.blend(Theme.card(), Theme.cardHover(), ha));
        R2D.roundOutline(ctx, x, y, w, h, 9, 0.5f, R2D.blend(Theme.stroke(), Theme.textFaint(), active ? 1 : 0));
        drawHead(ctx, x + 7, y + 7, 22);
        R2D.circle(ctx, x + 7 + 21, y + 7 + 21, 3.2f, Theme.card());
        R2D.circle(ctx, x + 7 + 21, y + 7 + 21, 2.2f, Theme.accent());
        String name = dev.vesc.client.module.Legit.name();
        Fonts.draw(ctx, Fonts.trim(name, Font.BOLD, 0.86f, w - 52), x + 36, y + 9, Theme.text(), Font.BOLD, 0.86f);
        Fonts.draw(ctx, Lang.format("profile.modules", rc.modules.enabledCount()), x + 36, y + 20, Theme.textFaint(), Font.REGULAR, 0.62f);
        R2D.icon(ctx, "settings", x + w - 15, y + h / 2f - 4.5f, 9, R2D.blend(Theme.textFaint(), Theme.text(), ha));
        hit(x, y, w, h, b -> {
            if (b == 0) setPage(Page.PROFILE);
            else if (b == 1) setPage(Page.SETTINGS);
        });
    }

    private void drawHead(DrawContext ctx, float x, float y, int size) {
        ctx.draw();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, R2D.alpha);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        PlayerSkinDrawer.draw(ctx, client.getSkinProvider().getSkinTextures(client.getGameProfile()), 0, 0, size);
        ctx.getMatrices().pop();
        ctx.draw();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    // ================================================================== content

    private void drawContent(DrawContext ctx) {
        float cx = x0 + SB, cw = W - SB;
        long now = System.currentTimeMillis();
        float pa = Anim.easeOutCubic((now - pageTime) / (260f / Anim.globalSpeed));

        // nagłówek
        String title, sub;
        switch (page) {
            case HOME -> { title = Lang.get("page.home"); sub = Lang.get("home.desc"); }
            case THEMES -> { title = Lang.get("page.themes"); sub = Lang.get("themes.desc"); }
            case HUD -> { title = Lang.get("page.hud"); sub = Lang.get("hud.desc"); }
            case FRIENDS -> { title = Lang.get("page.friends"); sub = Lang.get("friends.desc"); }
            case CONFIG -> { title = Lang.get("page.config"); sub = Lang.get("config.desc"); }
            case PROFILE -> { title = Lang.get("page.profile"); sub = Lang.get("profile.desc"); }
            case SETTINGS -> { title = Lang.get("page.settings"); sub = Lang.get("settings.desc"); }
            default -> {
                List<Module> list = currentModules();
                title = !search.isEmpty() ? "„" + search + "”" : page == Page.FAVORITES ? Lang.get("cat.favorites") : category.getName();
                sub = page == Page.CATEGORY && search.isEmpty() ? category.getDescription() + "  •  " + Lang.get("rmb.settings")
                        : Lang.format("modules.count", list.size(), list.stream().filter(Module::isEnabled).count()) + "  •  " + Lang.get("rmb.settings");
            }
        }
        float prev = R2D.alpha;
        R2D.alpha = prev * pa;
        Fonts.draw(ctx, title, cx + PAD + (1 - pa) * 8, y0 + 17, Theme.text(), Font.BOLD, 1.45f);
        Fonts.draw(ctx, Fonts.trim(sub, Font.REGULAR, 0.72f, cw - PAD * 2 - (page == Page.CATEGORY || page == Page.FAVORITES ? 268 : 130)), cx + PAD + (1 - pa) * 8, y0 + 34, Theme.textDim(), Font.REGULAR, 0.72f);
        R2D.alpha = prev;

        // prawa strona nagłówka
        boolean modulesPage = page == Page.CATEGORY || page == Page.FAVORITES;
        if (modulesPage) drawSearch(ctx, cx + cw - PAD - 168, y0 + 16, 140, 22);
        // przycisk palety komend (Ctrl+K)
        {
            float bx = cx + cw - PAD - (modulesPage ? 168 + 52 : 22 + 52), byy = y0 + 16, bw = 46;
            boolean hv = hovered(bx, byy, bw, 22);
            float hca = anim("palbtn", 18).animate(hv ? 1 : 0);
            R2D.roundRect(ctx, bx, byy, bw, 22, 11, R2D.blend(Theme.card(), Theme.cardHover(), hca));
            R2D.roundOutline(ctx, bx, byy, bw, 22, 11, 0.5f, R2D.blend(Theme.stroke(), Theme.accent(), hca * 0.6f));
            R2D.icon(ctx, "keybind", bx + 8, byy + 7, 8, Theme.textDim());
            Fonts.drawV(ctx, "Ctrl K", bx + 19, byy + 11.5f, Theme.textDim(), Font.BOLD, 0.6f);
            hit(bx, byy, bw, 22, b -> { if (b == 0) openPalette(); });
            float lx = bx - 28;
            boolean hl = hovered(lx, byy, 22, 22);
            float hla = anim("layoutbtn", 18).animate(hl ? 1 : 0);
            R2D.roundRect(ctx, lx, byy, 22, 22, 11, R2D.blend(Theme.card(), Theme.cardHover(), hla));
            R2D.roundOutline(ctx, lx, byy, 22, 22, 11, 0.5f, R2D.blend(Theme.stroke(), Theme.accent(), hla * 0.6f));
            R2D.icon(ctx, "layers", lx + 7, byy + 7, 8, R2D.blend(Theme.textDim(), Theme.text(), hla));
            hit(lx, byy, 22, 22, b -> { if (b == 0) { rc.settings.layout.cycle(1); rc.config.saveClient(); } });
        }
        // przycisk zamknięcia
        {
            float bx = cx + cw - PAD - 22, byy = y0 + 16;
            boolean hv = hovered(bx, byy, 22, 22);
            float hca = anim("closebtn", 18).animate(hv ? 1 : 0);
            R2D.roundRect(ctx, bx, byy, 22, 22, 11, R2D.blend(Theme.card(), Theme.cardHover(), hca));
            R2D.roundOutline(ctx, bx, byy, 22, 22, 11, 0.5f, Theme.stroke());
            R2D.icon(ctx, "cross", bx + 7, byy + 7, 8, R2D.blend(Theme.textDim(), Theme.text(), hca));
            hit(bx, byy, 22, 22, b -> { if (b == 0) close(); });
        }
        if (page == Page.CONFIG) {
            float bw = 104;
            button(ctx, "newcfg", cx + cw - PAD - bw, y0 + 16, bw, 22, "plus", Lang.get("config.new"), true, () -> {
                String n = rc.config.newName();
                rc.config.save(n);
                rc.notifications.info("disk", Lang.get("config.created"), n);
            });
            float fw = 26;
            button(ctx, "openfolder", cx + cw - PAD - bw - 6 - fw, y0 + 16, fw, 22, "folder", null, false,
                    () -> Util.getOperatingSystem().open(rc.config.folder().toFile()));
        }

        R2D.rect(ctx, cx + PAD, y0 + 50, cw - PAD * 2, 0.5f, Theme.stroke());

        // inspektor modułu (panel z prawej) – zwęża obszar treści
        if (inspected != null) lastInspected = inspected;
        float ia = Anim.easeOutCubic(inspectorA.animate(inspected != null && modulesPage ? 1 : 0));
        float contentW = cw - IW * ia;

        // pasek narzędzi: filtry, sortowanie, widok
        float barH = 0;
        if (modulesPage) { drawToolbar(ctx, cx + PAD, y0 + 56, contentW - PAD * 2 + 6 * ia); barH = 22; }

        // obszar przewijany
        float vy = y0 + 51 + barH, vh = H - 51 - barH - 6;
        float sc = scroll.animate(scrollTarget);
        R2D.scissor(ctx, cx, vy, contentW, vh);
        float[] oldClip = clip;
        clip = new float[]{cx, vy, contentW, vh};
        hoveredCard = null;
        R2D.alpha = prev * pa;
        float top = vy + 10 - sc + (1 - pa) * 12;
        float contentH = switch (page) {
            case CATEGORY, FAVORITES -> drawModules(ctx, cx + PAD, top, contentW - PAD * 2 + 6 * ia);
            case HUD -> drawHudPage(ctx, cx + PAD, top, cw - PAD * 2);
            case FRIENDS -> drawFriends(ctx, cx + PAD, top, cw - PAD * 2);
            case HOME -> drawHome(ctx, cx + PAD, top, cw - PAD * 2);
            case THEMES -> drawStyleRow(ctx, cx + PAD, top, cw - PAD * 2) + drawThemes(ctx, cx + PAD, top + 34, cw - PAD * 2);
            case CONFIG -> drawConfigs(ctx, cx + PAD, top, cw - PAD * 2);
            case PROFILE -> drawProfile(ctx, cx + PAD, top, cw - PAD * 2);
            case SETTINGS -> drawSettings(ctx, cx + PAD, top, cw - PAD * 2);
        };
        R2D.alpha = prev;
        clip = oldClip;
        R2D.endScissor(ctx);

        maxScroll = Math.max(0, contentH + 20 - vh);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));

        // gradient na krawędziach i pasek przewijania
        if (maxScroll > 0) {
            float trackH = vh - 16;
            float thumbH = Math.max(24, trackH * vh / (contentH + 20));
            float ty = vy + 8 + (trackH - thumbH) * (sc / maxScroll);
            R2D.roundRect(ctx, cx + contentW - 7, ty, 2.5f, thumbH, 1.25f, Theme.accentWave(0));
        }
        if (hoveredCard != tooltipModule) { tooltipModule = hoveredCard; tooltipStart = System.currentTimeMillis(); }
        if (ia > 0.01f && lastInspected != null) drawInspector(ctx, cx + cw - IW * ia, y0 + 51, IW, H - 57, ia);
        else inspX = inspW = 0;
    }

    private void drawSearch(DrawContext ctx, float x, float y, float w, float h) {
        boolean hov = hovered(x, y, w, h);
        float fa = anim("searchfocus", 16).animate(searchFocused ? 1 : 0);
        float ha = anim("searchhover", 16).animate(hov ? 1 : 0);
        R2D.roundRect(ctx, x, y, w, h, h / 2f, R2D.blend(R2D.blend(Theme.card(), Theme.cardHover(), ha * 0.7f), R2D.withAlpha(Theme.accent(), 0.22f), fa));
        R2D.roundOutline(ctx, x, y, w, h, 8, 0.6f, R2D.blend(Theme.stroke(), Theme.accent(), fa));
        R2D.icon(ctx, "search", x + 8, y + h / 2f - 4.5f, 9, R2D.blend(Theme.textFaint(), Theme.text(), fa));
        String shown = search.isEmpty() ? Lang.get("search") : search;
        int c = search.isEmpty() ? Theme.textFaint() : Theme.text();
        Fonts.drawV(ctx, Fonts.trim(shown, Font.REGULAR, 0.78f, w - 34), x + 22, y + h / 2f + 0.5f, c, Font.REGULAR, 0.78f);
        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            float tx = x + 22 + (search.isEmpty() ? 0 : Fonts.width(Fonts.trim(search, Font.REGULAR, 0.78f, w - 34), Font.REGULAR, 0.78f) + 1);
            R2D.rect(ctx, tx, y + 6, 0.7f, h - 12, Theme.text());
        }
        if (!search.isEmpty()) {
            float bx = x + w - 16;
            R2D.icon(ctx, "cross", bx, y + h / 2f - 3.5f, 7, hovered(bx - 3, y, 14, h) ? Theme.text() : Theme.textFaint());
            hit(bx - 3, y, 14, h, b -> { search = ""; scrollTarget = 0; });
        }
        hit(x, y, w, h, b -> searchFocused = true);
    }

    // ------------------------------------------------------------------ moduły

    private List<Module> favorites() {
        List<Module> out = new ArrayList<>();
        for (Module m : rc.modules.all()) if (m.favorite) out.add(m);
        return out;
    }

    private List<Module> currentModules() {
        if (search.isEmpty()) return page == Page.FAVORITES ? favorites() : rc.modules.byCategory(category);
        String q = search.toLowerCase(Locale.ROOT);
        List<Module> out = new ArrayList<>();
        for (Module m : rc.modules.all())
            if (m.getName().toLowerCase(Locale.ROOT).contains(q) || m.getDescription().toLowerCase(Locale.ROOT).contains(q)) out.add(m);
        return out;
    }

    private List<Module> filtered(List<Module> in) {
        List<Module> out = new ArrayList<>();
        for (Module m : in) {
            boolean ok = switch (filter) {
                case ENABLED -> m.isEnabled();
                case DISABLED -> !m.isEnabled();
                case BOUND -> m.hasKey();
                default -> true;
            };
            if (ok) out.add(m);
        }
        if (sort == Sort.NAME) out.sort(Comparator.comparing(m -> m.getName().toLowerCase(Locale.ROOT)));
        else if (sort == Sort.ENABLED) out.sort(Comparator.comparing((Module m) -> !m.isEnabled()).thenComparing(m -> m.getName().toLowerCase(Locale.ROOT)));
        return out;
    }

    /** wybrana podgrupa w każdej kategorii (null = wszystkie) */
    private static final Map<Category, String> groupSel = new java.util.EnumMap<>(Category.class);

    private float drawModules(DrawContext ctx, float x, float y, float w) {
        boolean catPage = page == Page.CATEGORY && search.isEmpty();
        float startY = y;
        String grp = catPage ? groupSel.get(category) : null;
        if (catPage) {
            y += drawCategoryHero(ctx, x, y, w) + 8;
            y += drawGroupChips(ctx, x, y, w) + 10;
        }
        List<Module> base = currentModules();
        if (grp != null) base.removeIf(m -> !grp.equals(m.getGroup()));
        List<Module> list = filtered(base);
        shownModules = list;
        if (focusIndex >= list.size()) focusIndex = list.size() - 1;
        if (list.isEmpty()) {
            boolean fav = page == Page.FAVORITES && search.isEmpty();
            R2D.icon(ctx, fav ? "star" : "search", x + w / 2f - 12, y + 40, 24, Theme.textFaint());
            Fonts.drawCentered(ctx, fav ? Lang.get("fav.empty") : Lang.pl() ? "Nic nie znaleziono" : "Nothing found", x + w / 2f, y + 72, Theme.textDim(), Font.SEMIBOLD, 0.9f);
            return y - startY + 110;
        }
        int cols = listView ? 1 : (w > 560 ? 3 : w > 330 ? 2 : 1);
        lastCols = cols;
        boolean grouped = catPage && grp == null && sort == Sort.DEFAULT;
        float cy = y;
        if (!grouped) cy = drawGrid(ctx, list, 0, x, cy, w, cols);
        else {
            int idx = 0;
            for (String g : category.groups) {
                List<Module> sub = new ArrayList<>();
                for (Module m : list) if (g.equals(m.getGroup())) sub.add(m);
                if (sub.isEmpty()) continue;
                // nagłówek podgrupy
                String gn = Category.groupName(g).toUpperCase(Locale.ROOT);
                Fonts.drawV(ctx, gn, x + 2, cy + 5, Theme.textFaint(), Font.BOLD, 0.58f);
                float gw = Fonts.width(gn, Font.BOLD, 0.58f);
                String cnt = String.valueOf(sub.size());
                Fonts.drawV(ctx, cnt, x + 2 + gw + 5, cy + 5, R2D.blend(catColor(category), Theme.textFaint(), 0.35f), Font.BOLD, 0.58f);
                float lx = x + 2 + gw + 5 + Fonts.width(cnt, Font.BOLD, 0.58f) + 7;
                R2D.gradientH(ctx, lx, cy + 5, x + w - lx, 0.5f, Theme.stroke(), R2D.withAlpha(Theme.stroke(), 0f));
                cy += 13;
                cy = drawGrid(ctx, sub, idx, x, cy, w, cols) + 4;
                idx += sub.size();
            }
        }
        return cy - startY;
    }

    /** siatka kart modułów; zwraca y pod siatką */
    private float drawGrid(DrawContext ctx, List<Module> list, int startIndex, float x, float y, float w, int cols) {
        float gap = listView ? 5 : 8, colW = (w - gap * (cols - 1)) / cols;
        long now = System.currentTimeMillis();
        float cy = y;
        for (int i = 0; i < list.size(); i++) {
            int col = i % cols;
            int gi = startIndex + i;
            // wejście kart z opóźnieniem (stagger)
            float e = Anim.easeOutCubic((now - listTime - gi * 22) / (260f / Anim.globalSpeed));
            float prev = R2D.alpha;
            R2D.alpha = prev * e;
            drawModuleCard(ctx, list.get(i), x + col * (colW + gap), cy + (1 - e) * 10, colW, gi);
            R2D.alpha = prev;
            if (col == cols - 1 || i == list.size() - 1) cy += (listView ? 30 : 34) + gap;
        }
        return cy;
    }

    /** nagłówek strony kategorii: kafelek, statystyki, pasek aktywności */
    /** kolor kategorii: w trybie biało-czarnym (MONO) biel/szarość, inaczej własny kolor kategorii */
    private static int catColor(Category c) {
        return Theme.accentPreset() == Theme.Accent.MONO ? 0xFFFFFFFF : c.color;
    }

    private float drawCategoryHero(DrawContext ctx, float x, float y, float w) {
        Category c = category;
        int col = catColor(c);
        float h = 58;
        List<Module> ms = rc.modules.byCategory(c);
        long en = ms.stream().filter(Module::isEnabled).count(), bound = ms.stream().filter(Module::hasKey).count();
        float pct = ms.isEmpty() ? 0 : (float) en / ms.size();
        // tło: ciemna karta z poświatą kategorii wychodzącą z lewego rogu
        R2D.roundRect(ctx, x, y, w, h, 13, Theme.card());
        R2D.scissor(ctx, x, y, w, h);
        R2D.glow(ctx, x - 60, y - 36, 240, h + 72, R2D.withAlpha(col, 0.16f));
        R2D.endScissor(ctx);
        R2D.roundRectH(ctx, x, y, w, h, 13, R2D.withAlpha(col, 0.12f), R2D.withAlpha(col, 0f));
        R2D.roundOutline(ctx, x, y, w, h, 13, 0.6f, R2D.withAlpha(col, 0.3f));
        // pionowy pasek akcentu po lewej
        R2D.roundRect(ctx, x + 8, y + 11, 3, h - 22, 1.5f, col);
        // kafelek z ikoną kategorii
        float tx = x + 18, ty = y + (h - 38) / 2f;
        R2D.shadow(ctx, tx, ty + 2, 38, 38, 9, R2D.withAlpha(col, 0.42f));
        R2D.roundRectV(ctx, tx, ty, 38, 38, 12, R2D.blend(col, 0xFFFFFFFF, 0.24f), R2D.blend(col, 0xFF000000, 0.12f));
        R2D.roundOutline(ctx, tx, ty, 38, 38, 12, 0.6f, R2D.withAlpha(0xFFFFFFFF, 0.3f));
        R2D.icon(ctx, c.icon, tx + 10, ty + 10, 18, Theme.contrast(col));
        // nazwa + opis kategorii w nagłówku
        float nx = tx + 50;
        Fonts.draw(ctx, c.getName(), nx, y + 13, Theme.text(), Font.BOLD, 1.32f);
        Fonts.draw(ctx, Fonts.trim(c.getDescription(), Font.REGULAR, 0.66f, w - (nx - x) - 176), nx, y + 33, Theme.textDim(), Font.REGULAR, 0.66f);
        // mini-statystyki po prawej: Moduły / Aktywne / Klawisze
        String[][] stats = {
                {String.valueOf(ms.size()), Lang.pl() ? "MODUŁY" : "MODULES", "0"},
                {String.valueOf(en), Lang.pl() ? "AKTYWNE" : "ACTIVE", "1"},
                {String.valueOf(bound), Lang.pl() ? "KLAWISZE" : "KEYS", "0"},
        };
        float chipH = 36, gap = 6, total = 0;
        float[] cw = new float[stats.length];
        for (int i = 0; i < stats.length; i++) { cw[i] = Math.max(40, Fonts.width(stats[i][1], Font.BOLD, 0.5f) + 14); total += cw[i] + (i > 0 ? gap : 0); }
        float sx = x + w - 12 - total, sy = y + (h - chipH) / 2f;
        for (int i = 0; i < stats.length; i++) {
            boolean hot = stats[i][2].equals("1") && en > 0;
            R2D.roundRect(ctx, sx, sy, cw[i], chipH, 9, hot ? R2D.withAlpha(col, 0.18f) : R2D.withAlpha(Theme.text(), 0.05f));
            if (hot) R2D.roundOutline(ctx, sx, sy, cw[i], chipH, 9, 0.6f, R2D.withAlpha(col, 0.5f));
            Fonts.drawCentered(ctx, stats[i][0], sx + cw[i] / 2f, sy + 7, hot ? R2D.blend(col, 0xFFFFFFFF, 0.4f) : Theme.text(), Font.BOLD, 1.02f);
            Fonts.drawCentered(ctx, stats[i][1], sx + cw[i] / 2f, sy + chipH - 10, Theme.textFaint(), Font.BOLD, 0.5f);
            sx += cw[i] + gap;
        }
        // cienki pasek aktywności wtopiony w dolną krawędź nagłówka
        float pa = anim("heroPct" + c, 10).animate(pct);
        R2D.roundRect(ctx, x + 13, y + h - 5, w - 26, 2, 1, R2D.withAlpha(col, 0.14f));
        if (pa > 0.005f) R2D.roundRectH(ctx, x + 13, y + h - 5, Math.max(4, (w - 26) * pa), 2, 1, R2D.blend(col, 0xFFFFFFFF, 0.3f), col);
        return h;
    }

    /** chipy podgrup („Wszystkie”, „Aura”, „Assist” …) */
    private float drawGroupChips(DrawContext ctx, float x, float y, float w) {
        Category c = category;
        String sel = groupSel.get(c);
        List<String> gs = new ArrayList<>();
        gs.add(null);
        gs.addAll(c.groups);
        float h = 18, pad = 3;
        // policz, ile chipów mieści się w jednym torze (segmented control)
        List<String> fit = new ArrayList<>();
        List<float[]> box = new ArrayList<>();
        float cx = x + pad;
        for (String g : gs) {
            String label = g == null ? Category.groupName("all") : Category.groupName(g);
            float cw = Fonts.width(label, Font.SEMIBOLD, 0.64f) + Fonts.width(String.valueOf(rc.modules.byGroup(c, g).size()), Font.BOLD, 0.54f) + 17;
            if (cx + cw + pad > x + w) break;
            fit.add(g); box.add(new float[]{cx, cw});
            cx += cw + 2;
        }
        float trackW = cx - x - 2 + pad;
        // tor segmentowy – jedna zaokrąglona wnęka, aktywny chip jako wypełniona pigułka
        R2D.roundRect(ctx, x, y, trackW, h + pad * 2, (h + pad * 2) / 2f, R2D.withAlpha(Theme.toggleOff(), 0.4f));
        R2D.roundOutline(ctx, x, y, trackW, h + pad * 2, (h + pad * 2) / 2f, 0.5f, Theme.stroke());
        for (int i = 0; i < fit.size(); i++) {
            String g = fit.get(i);
            float bx = box.get(i)[0], bw = box.get(i)[1], by = y + pad;
            String label = g == null ? Category.groupName("all") : Category.groupName(g);
            String cnt = String.valueOf(rc.modules.byGroup(c, g).size());
            boolean act = java.util.Objects.equals(sel, g);
            boolean hv = hovered(bx, by, bw, h);
            float aa = anim("grp" + c + g, 16).animate(act ? 1 : 0), ha = anim("grph" + c + g, 18).animate(hv && !act ? 1 : 0);
            if (aa > 0.01f) {
                R2D.glow(ctx, bx - 6, by - 6, bw + 12, h + 12, R2D.withAlpha(catColor(c), 0.3f * aa));
                R2D.roundRectV(ctx, bx, by, bw, h, h / 2f, R2D.blend(Theme.toggleOff(), R2D.blend(catColor(c), 0xFFFFFFFF, 0.14f), aa), R2D.blend(Theme.toggleOff(), catColor(c), aa));
            } else if (ha > 0.01f) {
                R2D.roundRect(ctx, bx, by, bw, h, h / 2f, R2D.withAlpha(Theme.text(), 0.07f * ha));
            }
            Fonts.drawV(ctx, label, bx + 8, by + h / 2f + 0.5f, R2D.blend(Theme.textDim(), Theme.contrast(catColor(c)), aa), act ? Font.BOLD : Font.SEMIBOLD, 0.64f);
            Fonts.drawV(ctx, cnt, bx + bw - 7 - Fonts.width(cnt, Font.BOLD, 0.54f), by + h / 2f + 0.5f, R2D.blend(Theme.textFaint(), R2D.withAlpha(Theme.contrast(catColor(c)), 0.8f), aa), Font.BOLD, 0.54f);
            hit(bx, by, bw, h, b -> { if (b == 0) { if (g == null) groupSel.remove(c); else groupSel.put(c, g); listTime = System.currentTimeMillis(); focusIndex = -1; } });
        }
        return h + pad * 2;
    }

    private float settingsHeight(Module m) {
        float h = 8 + 20; // separator + keybind
        for (Setting<?> s : m.getSettings()) h += s instanceof NumberSetting ? 28 : 20;
        return h + 6;
    }

    private float drawModuleCard(DrawContext ctx, Module m, float x, float y, float w, int index) {
        float baseH = listView ? 30 : 34;
        float ex = 0;
        float h = baseH;
        boolean hov = hovered(x, y, w, baseH);
        if (hov) hoveredCard = m;
        float ha = m.hoverAnim.animate(hov ? 1 : 0);
        float on = m.toggleAnim.animate(m.isEnabled() ? 1 : 0);
        boolean sel = inspected == m;
        float sa = anim("sel" + m.getName(), 16).animate(sel ? 1 : 0);

        R2D.roundRect(ctx, x, y, w, h, 7, R2D.blend(R2D.blend(Theme.card(), Theme.cardHover(), ha), R2D.withAlpha(Theme.accent(), 0.16f), sa * 0.6f));
        R2D.roundOutline(ctx, x, y, w, h, 7, 0.5f + sa * 0.5f, R2D.blend(R2D.blend(Theme.stroke(), Theme.textFaint(), on * 0.5f), Theme.accentWave(0), sa));
        if (index == focusIndex) R2D.roundOutline(ctx, x - 1.5f, y - 1.5f, w + 3, h + 3, 8.5f, 1, Theme.accentWave(0));
        float nn = Theme.neon() * on;
        if (nn > 0.01f) {
            R2D.glow(ctx, x - 8, y - 8, w + 16, h + 16, R2D.withAlpha(Theme.accentWave(0), 0.12f * nn));
            R2D.roundOutlineGrad(ctx, x, y, w, h, 7, 0.8f, R2D.withAlpha(Theme.accentWave(0), 0.9f * nn), R2D.withAlpha(Theme.accent2(), 0.3f * nn));
        }
        // fala po kliknięciu (ripple)
        long[] rp = ripples.get(m);
        if (rp != null) {
            float t = (System.currentTimeMillis() - rp[2]) / 450f;
            if (t >= 1) ripples.remove(m);
            else {
                R2D.scissor(ctx, x, y, w, h);
                R2D.circle(ctx, rp[0] / 100f, rp[1] / 100f, w * 1.1f * Anim.easeOutCubic(t), R2D.withAlpha(Theme.accentWave(0), 0.22f * (1 - t)));
                R2D.endScissor(ctx);
            }
        }
        // pasek aktywności po lewej
        if (on > 0.01f) R2D.roundRect(ctx, x + 3, y + 10, 1.5f, (baseH - 20) * on, 0.75f, Theme.accent());

        // mała ikona + nazwa / opis
        int nameCol = R2D.blend(Theme.textDim(), Theme.text(), on);
        R2D.icon(ctx, m.getIcon(), x + 10, y + 8.5f, 8.5f, R2D.blend(Theme.textFaint(), Theme.accent(), on));
        float tx = x + 23;
        Fonts.draw(ctx, m.getName(), tx, y + 8, nameCol, Font.BOLD, 0.86f);
        float right = 60;
        float nx = tx + Fonts.width(m.getName(), Font.BOLD, 0.86f) + 4;
        if (binding == m) {
            String k = Lang.get("bind.listening");
            float kw = Fonts.width(k, Font.BOLD, 0.6f) + 8;
            R2D.roundRect(ctx, nx, y + 7.5f, kw, 8.5f, 4.25f, Theme.accentWave(0));
            Fonts.drawV(ctx, k, nx + 4, y + 11.8f, Theme.accentText(), Font.BOLD, 0.6f);
            nx += kw + 4;
        } else if (m.hasKey()) {
            String k = Keys.name(m.getKey());
            float kw = Fonts.width(k, Font.BOLD, 0.62f) + 7;
            R2D.roundRect(ctx, nx, y + 7.5f, kw, 8.5f, 2.5f, Theme.toggleOff());
            Fonts.drawV(ctx, k, nx + 3.5f, y + 11.8f, Theme.textDim(), Font.BOLD, 0.62f);
            nx += kw + 4;
        }
        String vals = summary(m);
        if (!vals.isEmpty()) {
            float maxW = x + w - right - nx;
            if (maxW > 12) Fonts.drawV(ctx, Fonts.trim(vals, Font.SEMIBOLD, 0.62f, maxW), nx + 1, y + 11.8f, Theme.accentWave(0), Font.SEMIBOLD, 0.62f);
        }
        Fonts.draw(ctx, Fonts.trim(m.getDescription(), Font.REGULAR, 0.66f, w - (tx - x) - right), tx, y + 19, Theme.textFaint(), Font.REGULAR, 0.66f);

        // przełącznik
        drawSwitch(ctx, x + w - 30, y + baseH / 2f - 5.5f, on);

        // strzałka rozwinięcia
        float rot = sa * -2;
        ctx.getMatrices().push();
        float ax = x + w - 39, ay = y + baseH / 2f;
        ctx.getMatrices().translate(ax, ay, 0);
        ctx.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(180 + 90 * rot));
        R2D.icon(ctx, "arrow_left", -3.5f, -3.5f, 7, R2D.withAlpha(Theme.textFaint(), m.getSettings().isEmpty() ? 0.6f : 1f));
        ctx.getMatrices().pop();

        hit(x, y, w, baseH, b -> {
            focusIndex = index;
            if (b == 0) { m.toggle(); ripples.put(m, new long[]{(long) (mx * 100), (long) (my * 100), System.currentTimeMillis()}); }
            else if (b == 1) inspect(m);
            else if (b == 2) binding = binding == m ? null : m;
        });
        // strzałka – otwiera inspektor
        hit(ax - 6, ay - 7, 12, 14, b -> { if (b == 0 || b == 1) inspect(m); });

        // gwiazdka ulubionych (widoczna po najechaniu lub gdy ulubiony)
        float fa = m.favAnim.animate(m.favorite ? 1 : 0);
        float sx = x + w - 52, sy0 = y + baseH / 2f - 3.5f;
        boolean sh = hovered(sx - 2, sy0 - 2, 11, 11);
        float vis = Math.max(fa, ha * 0.7f);
        if (vis > 0.02f) {
            float sc = 1 + (sh ? 0.15f : 0);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(sx + 3.5f, sy0 + 3.5f, 0);
            ctx.getMatrices().scale(sc, sc, 1);
            R2D.icon(ctx, "star", -3.5f, -3.5f, 7, R2D.withAlpha(R2D.blend(sh ? Theme.textDim() : Theme.textFaint(), Theme.accentWave(0), fa), vis));
            ctx.getMatrices().pop();
        }
        hit(sx - 2, sy0 - 2, 11, 11, b -> { if (b == 0) { m.favorite = !m.favorite; rc.config.save(rc.config.current()); } });

        if (ex > 0.01f) {
            R2D.scissor(ctx, x, y + baseH, w, h - baseH);
            float[] oc = clip;
            clip = intersect(clip, new float[]{x, y + baseH, w, h - baseH});
            float prev = R2D.alpha;
            R2D.alpha = prev * ex;
            float sy = y + baseH;
            R2D.rect(ctx, x + 10, sy, w - 20, 0.5f, Theme.stroke());
            sy += 6;
            sy = drawBindRow(ctx, m, x + 10, sy, w - 20);
            for (Setting<?> s : m.getSettings()) sy = drawSetting(ctx, s, x + 10, sy, w - 20);
            R2D.alpha = prev;
            clip = oc;
            R2D.endScissor(ctx);
        }
        return h;
    }

    private float drawBindRow(DrawContext ctx, Module m, float x, float y, float w) {
        float h = 20;
        R2D.icon(ctx, "keybind", x, y + h / 2f - 4, 8, Theme.textDim());
        Fonts.drawV(ctx, Lang.get("bind"), x + 13, y + h / 2f + 0.5f, Theme.textDim(), Font.SEMIBOLD, 0.75f);
        boolean listening = binding == m;
        String label = listening ? Lang.get("bind.listening") : Keys.name(m.getKey());
        float bw = Fonts.width(label, Font.BOLD, 0.7f) + 12;
        float bx = x + w - bw;
        float la = anim("bind" + m.getName(), 16).animate(listening ? 1 : 0);
        boolean hov = hovered(bx, y + 3, bw, h - 6);
        R2D.roundRect(ctx, bx, y + 3, bw, h - 6, 5, R2D.blend(hov ? Theme.cardHover() : Theme.toggleOff(), Theme.accent(), la));
        Fonts.drawV(ctx, label, bx + 6, y + h / 2f + 0.5f, R2D.blend(Theme.text(), Theme.accentText(), la), Font.BOLD, 0.7f);
        hit(bx, y + 3, bw, h - 6, b -> {
            if (b == 0) binding = binding == m ? null : m;
            else if (b == 1) { m.setKey(GLFW.GLFW_KEY_UNKNOWN); binding = null; }
        });
        return y + h;
    }

    private float drawSetting(DrawContext ctx, Setting<?> s, float x, float y, float w) {
        if (s instanceof BoolSetting b) {
            float h = 20;
            Fonts.drawV(ctx, s.getName(), x, y + h / 2f + 0.5f, Theme.textDim(), Font.SEMIBOLD, 0.75f);
            float on = b.anim.animate(b.get() ? 1 : 0);
            drawSwitch(ctx, x + w - 20, y + h / 2f - 5.5f, on);
            hit(x + w - 24, y, 26, h, bt -> b.toggle());
            return y + h;
        }
        if (s instanceof NumberSetting n) {
            float h = 28;
            Fonts.draw(ctx, s.getName(), x, y + 4, Theme.textDim(), Font.SEMIBOLD, 0.75f);
            String v = n.display();
            Fonts.draw(ctx, v, x + w - Fonts.width(v, Font.BOLD, 0.72f), y + 4, Theme.text(), Font.BOLD, 0.72f);
            drawSlider(ctx, n, x, y + 18, w);
            return y + h;
        }
        if (s instanceof ModeSetting md) {
            float h = 20;
            Fonts.drawV(ctx, s.getName(), x, y + h / 2f + 0.5f, Theme.textDim(), Font.SEMIBOLD, 0.75f);
            String v = md.get();
            float bw = Fonts.width(v, Font.BOLD, 0.7f) + 26;
            float bx = x + w - bw;
            boolean hov = hovered(bx, y + 3, bw, h - 6);
            R2D.roundRect(ctx, bx, y + 3, bw, h - 6, 5, hov ? Theme.cardHover() : Theme.toggleOff());
            R2D.icon(ctx, "arrow_left", bx + 4, y + h / 2f - 2.5f, 5, Theme.textFaint());
            ctx.getMatrices().push();
            ctx.getMatrices().translate(bx + bw - 6.5f, y + h / 2f, 0);
            ctx.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(180));
            R2D.icon(ctx, "arrow_left", -2.5f, -2.5f, 5, Theme.textFaint());
            ctx.getMatrices().pop();
            Fonts.drawCentered(ctx, v, bx + bw / 2f, y + h / 2f - Fonts.height(0.7f) / 2f + 0.5f, Theme.text(), Font.BOLD, 0.7f);
            hit(bx, y + 3, bw, h - 6, bt -> {
                md.cycle(bt == 1 ? -1 : 1);
                for (Module mm : rc.modules.all())
                    if (mm instanceof dev.vesc.client.module.cosmetics.HitSound hs && hs.sound == md) hs.play();
            });
            return y + h;
        }
        return y;
    }

    // ================================================================== układ Panels

    private void drawPanels(DrawContext ctx) {
        if (rc.settings.particles.get()) drawBackgroundFx(ctx);
        hoveredCard = null;
        panelBody.clear();
        drawDock(ctx);
        for (Category c : new ArrayList<>(panelOrder)) drawPanel(ctx, c);
        if (hoveredCard != tooltipModule) { tooltipModule = hoveredCard; tooltipStart = System.currentTimeMillis(); }
        // podpowiedź na dole
        String hint = Lang.get("panels.hint");
        float hw = Fonts.width(hint, Font.REGULAR, 0.6f) + 20;
        R2D.roundRect(ctx, vLeft + (vW - hw) / 2f, vTop + vH - 20, hw, 13, 6.5f, Theme.bg());
        Fonts.drawCentered(ctx, hint, vLeft + vW / 2f, vTop + vH - 13.5f - Fonts.height(0.6f) / 2f + 0.5f, Theme.textFaint(), Font.REGULAR, 0.6f);
        drawTooltip(ctx);
        drawPalette(ctx);
    }

    private void dockButton(DrawContext ctx, String key, float x, float y, String icon, Runnable action) {
        boolean hv = hovered(x, y, 20, 20);
        float ha = anim("dock" + key, 18).animate(hv ? 1 : 0);
        R2D.roundRect(ctx, x, y, 20, 20, 10, R2D.blend(Theme.card(), Theme.cardHover(), ha));
        R2D.roundOutline(ctx, x, y, 20, 20, 10, 0.5f, R2D.blend(Theme.stroke(), Theme.accentWave(0), ha));
        R2D.icon(ctx, icon, x + 6, y + 6, 8, R2D.blend(Theme.textDim(), Theme.text(), ha));
        hit(x, y, 20, 20, b -> { if (b == 0) action.run(); });
    }

    private void drawDock(DrawContext ctx) {
        float w = 430, h = 28, x = vLeft + (vW - w) / 2f, y = vTop + 8;
        R2D.shadow(ctx, x, y, w, h, 14, Theme.shadow());
        R2D.roundRect(ctx, x, y, w, h, h / 2f, Theme.bg());
        float g = Theme.glass();
        R2D.roundOutline(ctx, x, y, w, h, h / 2f, 0.6f, Theme.stroke());
        if (g > 0.01f) R2D.roundOutlineGrad(ctx, x, y, w, h, h / 2f, 0.8f, R2D.withAlpha(Theme.accentWave(0), 0.6f * g), R2D.withAlpha(Theme.accent2(), 0.35f * g));
        R2D.glow(ctx, x - 4, y - 14, 50, 56, R2D.withAlpha(Theme.accentWave(0), 0.35f));
        R2D.icon(ctx, "logo", x + 9, y + 7, 14, Theme.accentWave(0));
        Fonts.drawV(ctx, "VescClient", x + 28, y + h / 2f + 0.5f, Theme.text(), Font.BOLD, 0.82f);
        String v = VescClient.VERSION.substring(0, 3);
        float tw = Fonts.width("VescClient", Font.BOLD, 0.82f);
        Fonts.draw(ctx, v, x + 30 + tw, y + 6, Theme.accentWave(1), Font.BOLD, 0.52f);
        drawSearch(ctx, x + 112, y + 4, 168, 20);
        float bx = x + w - 8 - 20;
        dockButton(ctx, "close", bx, y + 4, "cross", this::close);
        bx -= 24;
        dockButton(ctx, "pal", bx, y + 4, "keybind", this::openPalette);
        bx -= 24;
        dockButton(ctx, "hud", bx, y + 4, "move", () -> { if (client != null) client.setScreen(new HudEditor()); });
        bx -= 24;
        dockButton(ctx, "win", bx, y + 4, "misc", () -> { rc.settings.layout.set("Window"); rc.config.saveClient(); setPage(Page.HOME); });
        bx -= 24;
        dockButton(ctx, "reset", bx, y + 4, "reset", () -> { rc.settings.panels.clear(); rc.config.saveClient(); });
    }

    private List<Module> panelModules(Category c) {
        List<Module> out = new ArrayList<>();
        String q = search.toLowerCase(Locale.ROOT);
        for (Module m : rc.modules.byCategory(c))
            if (q.isEmpty() || m.getName().toLowerCase(Locale.ROOT).contains(q) || m.getDescription().toLowerCase(Locale.ROOT).contains(q)) out.add(m);
        return out;
    }

    private void drawPanel(DrawContext ctx, Category c) {
        float[] pp = panelPos(c);
        float x = vLeft + pp[0], y = vTop + pp[1], w = PW;
        boolean collapsed = pp[2] > 0.5f;
        float open = Anim.easeOutCubic(anim("pcol" + c, 14).animate(collapsed ? 0 : 1));
        List<Module> mods = panelModules(c);
        boolean dim = !search.isEmpty() && mods.isEmpty();
        float prevA = R2D.alpha;
        R2D.alpha = prevA * anim("pdim" + c, 14).animate(dim ? 0.35f : 1f);

        // wysokość treści
        float rowH = 19, content = 4;
        for (Module m : mods) content += rowH + setHeights.getOrDefault(m, 0f) * Anim.easeOutCubic(m.expandAnim.animate(m.expanded ? 1 : 0));
        float maxBody = Math.max(60, vTop + vH - 30 - (y + PHEAD));
        float bodyH = Math.min(content, maxBody) * open;
        float h = PHEAD + bodyH + (bodyH > 1 ? 4 : 0);

        // tło panelu
        R2D.shadow(ctx, x, y, w, h, 16, Theme.shadow());
        R2D.roundRect(ctx, x, y, w, h, 10, Theme.bg());
        float g = Theme.glass();
        if (g > 0.01f) R2D.roundRectV(ctx, x, y, w, Math.min(h, 40), 10, R2D.withAlpha(0xFFFFFFFF, 0.06f * g), 0x00FFFFFF);
        R2D.roundOutline(ctx, x, y, w, h, 10, 0.6f, Theme.stroke());
        if (g > 0.01f) R2D.roundOutlineGrad(ctx, x, y, w, h, 10, 0.7f, R2D.withAlpha(Theme.accentWave(c.ordinal() * 0.5f), 0.55f * g), R2D.withAlpha(Theme.accent2(), 0.25f * g));

        // nagłówek
        long en = mods.stream().filter(Module::isEnabled).count();
        boolean hh = hovered(x, y, w, PHEAD);
        float hha = anim("phead" + c, 18).animate(hh || draggingPanel == c ? 1 : 0);
        R2D.roundRect(ctx, x + 2, y + 2, w - 4, PHEAD - 4, 8, R2D.withAlpha(Theme.cardHover(), hha * 0.7f));
        int ac = Theme.accentWave(c.ordinal() * 0.5f);
        R2D.glow(ctx, x - 2, y - 6, 30, 32, R2D.withAlpha(ac, 0.35f));
        R2D.icon(ctx, c.icon, x + 8, y + PHEAD / 2f - 4.5f, 9, ac);
        Fonts.drawV(ctx, c.getName(), x + 22, y + PHEAD / 2f + 0.5f, Theme.text(), Font.BOLD, 0.8f);
        String cnt = en + "/" + mods.size();
        float cw = Fonts.width(cnt, Font.BOLD, 0.54f) + 8;
        R2D.roundRect(ctx, x + w - 22 - cw, y + PHEAD / 2f - 4.5f, cw, 9, 4.5f, R2D.withAlpha(ac, en > 0 ? 0.28f : 0.12f));
        Fonts.drawV(ctx, cnt, x + w - 22 - cw + 4, y + PHEAD / 2f + 0.4f, en > 0 ? ac : Theme.textFaint(), Font.BOLD, 0.54f);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + w - 12, y + PHEAD / 2f, 0);
        ctx.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(180 + 90 * open));
        R2D.icon(ctx, "arrow_left", -3.5f, -3.5f, 7, Theme.textFaint());
        ctx.getMatrices().pop();
        hit(x, y, w, PHEAD, b -> {
            panelOrder.remove(c); panelOrder.add(c);
            if (b == 0) { draggingPanel = c; panGrabX = mx - x; panGrabY = my - y; }
            else if (b == 1) { pp[2] = pp[2] > 0.5f ? 0 : 1; rc.config.saveClient(); }
        });

        if (bodyH > 1) {
            float by = y + PHEAD;
            panelBody.put(c, new float[]{x, by, w, bodyH});
            float[] st = panelScrollT.computeIfAbsent(c, k -> new float[2]);
            st[1] = Math.max(0, content - bodyH);
            st[0] = Math.min(st[0], st[1]);
            float sc = panelScroll.computeIfAbsent(c, k -> new Anim(0, 16)).animate(st[0]);
            R2D.scissor(ctx, x, by, w, bodyH);
            float[] oc = clip;
            clip = new float[]{x, by, w, bodyH};
            float cy = by + 2 - sc;
            for (Module m : mods) {
                boolean hv = hovered(x + 4, cy, w - 8, rowH - 1);
                if (hv) hoveredCard = m;
                float ha = m.hoverAnim.animate(hv ? 1 : 0);
                float on = m.toggleAnim.animate(m.isEnabled() ? 1 : 0);
                int mc = Theme.accentWave(c.ordinal() * 0.5f + cy * 0.01f);
                R2D.roundRect(ctx, x + 4, cy, w - 8, rowH - 1, 6, R2D.blend(R2D.withAlpha(Theme.cardHover(), ha), R2D.withAlpha(mc, 0.2f + 0.08f * ha), on));
                if (on > 0.01f) R2D.roundRect(ctx, x + 6, cy + 5, 1.6f, (rowH - 11) * on, 0.8f, mc);
                R2D.icon(ctx, m.getIcon(), x + 11, cy + rowH / 2f - 3.75f, 7, R2D.blend(Theme.textFaint(), mc, on));
                String name = binding == m ? Lang.get("bind.listening") : m.getName();
                Fonts.drawV(ctx, Fonts.trim(name, Font.SEMIBOLD, 0.72f, w - 48), x + 22, cy + rowH / 2f, binding == m ? mc : R2D.blend(Theme.textDim(), Theme.text(), Math.max(on, ha * 0.6f)), Font.SEMIBOLD, 0.72f);
                if (m.hasKey() && binding != m) {
                    String k = Keys.name(m.getKey());
                    float kw = Fonts.width(k, Font.BOLD, 0.52f) + 6;
                    R2D.roundRect(ctx, x + w - 20 - kw, cy + rowH / 2f - 4, kw, 8, 2.5f, Theme.toggleOff());
                    Fonts.drawV(ctx, k, x + w - 20 - kw + 3, cy + rowH / 2f, Theme.textDim(), Font.BOLD, 0.52f);
                }
                float ex = Anim.easeOutCubic(m.expandAnim.raw());
                if (!m.getSettings().isEmpty()) {
                    ctx.getMatrices().push();
                    ctx.getMatrices().translate(x + w - 12, cy + rowH / 2f, 0);
                    ctx.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(180 + 90 * ex));
                    R2D.icon(ctx, "arrow_left", -2.5f, -2.5f, 5, Theme.textFaint());
                    ctx.getMatrices().pop();
                }
                long[] rp = ripples.get(m);
                if (rp != null) {
                    float t = (System.currentTimeMillis() - rp[2]) / 450f;
                    if (t >= 1) ripples.remove(m);
                    else {
                        R2D.scissor(ctx, x + 4, cy, w - 8, rowH - 1);
                        R2D.circle(ctx, rp[0] / 100f, rp[1] / 100f, w * Anim.easeOutCubic(t), R2D.withAlpha(mc, 0.25f * (1 - t)));
                        R2D.endScissor(ctx);
                    }
                }
                final float ry = cy;
                hit(x + 4, cy, w - 8, rowH - 1, b -> {
                    if (b == 0) { m.toggle(); ripples.put(m, new long[]{(long) (mx * 100), (long) (my * 100), System.currentTimeMillis()}); }
                    else if (b == 1) m.expanded = !m.expanded;
                    else if (b == 2) binding = binding == m ? null : m;
                });
                cy += rowH;
                // rozwinięte ustawienia w panelu
                if (ex > 0.005f) {
                    float full = setHeights.getOrDefault(m, 60f);
                    float vis = full * ex;
                    R2D.scissor(ctx, x, cy, w, vis);
                    float[] oc2 = clip;
                    clip = intersect(clip, new float[]{x, cy, w, vis});
                    float sy = cy + 2;
                    // klawisz
                    String key = binding == m ? Lang.get("bind.listening") : Keys.name(m.getKey());
                    Fonts.drawV(ctx, Lang.get("bind"), x + 10, sy + 7, Theme.textDim(), Font.SEMIBOLD, 0.66f);
                    float kw = Fonts.width(key, Font.BOLD, 0.58f) + 10;
                    boolean kh = hovered(x + w - 10 - kw, sy + 1, kw, 12);
                    R2D.roundRect(ctx, x + w - 10 - kw, sy + 1, kw, 12, 6, binding == m ? mc : kh ? Theme.cardHover() : Theme.toggleOff());
                    Fonts.drawV(ctx, key, x + w - 10 - kw + 5, sy + 7.3f, binding == m ? Theme.accentText() : Theme.text(), Font.BOLD, 0.58f);
                    hit(x + w - 10 - kw, sy + 1, kw, 12, b -> { if (b == 0) binding = binding == m ? null : m; else if (b == 1) { m.setKey(GLFW.GLFW_KEY_UNKNOWN); binding = null; } });
                    sy += 16;
                    for (Setting<?> stt : m.getSettings()) sy = drawInspSetting(ctx, stt, x + 10, sy, w - 20);
                    setHeights.put(m, sy - cy + 4);
                    R2D.rect(ctx, x + 10, cy + vis - 1, w - 20, 0.5f, R2D.withAlpha(Theme.stroke(), ex));
                    clip = oc2;
                    R2D.endScissor(ctx);
                    cy += vis;
                }
            }
            clip = oc;
            R2D.endScissor(ctx);
            if (st[1] > 0) {
                float th = Math.max(14, bodyH * bodyH / content);
                R2D.roundRect(ctx, x + w - 3, by + (bodyH - th) * (sc / st[1]), 1.6f, th, 0.8f, ac);
            }
        }
        R2D.alpha = prevA;
    }

    // ================================================================== strona Home

    private float drawStyleRow(DrawContext ctx, float x, float y, float w) {
        float h = 26;
        R2D.roundRect(ctx, x, y, w, h, 8, Theme.card());
        R2D.roundOutline(ctx, x, y, w, h, 8, 0.5f, Theme.stroke());
        R2D.icon(ctx, "sparkles", x + 9, y + 9, 8, Theme.accentWave(0));
        Fonts.drawV(ctx, Lang.get("style.title"), x + 22, y + h / 2f + 0.5f, Theme.text(), Font.BOLD, 0.8f);
        Fonts.drawV(ctx, Lang.get("style.desc"), x + 22 + Fonts.width(Lang.get("style.title"), Font.BOLD, 0.8f) + 8, y + h / 2f + 0.5f, Theme.textFaint(), Font.REGULAR, 0.62f);
        segmented(ctx, "style", x + w - 174, y + 4, 168, 18, new String[]{"Solid", "Glass", "Neon"}, Theme.style().ordinal(), i -> {
            Theme.setStyle(Theme.Style.values()[i]); rc.config.saveClient();
        });
        return 34;
    }

    private String ago(long t) {
        long s = (System.currentTimeMillis() - t) / 1000;
        if (s < 60) return s + "s";
        if (s < 3600) return (s / 60) + "m";
        return (s / 3600) + "h";
    }

    private float drawHome(DrawContext ctx, float x, float y, float w) {
        // baner powitalny
        float bh = 64;
        R2D.roundRect(ctx, x, y, w, bh, 12, Theme.card());
        R2D.scissor(ctx, x, y, w, bh);
        R2D.glow(ctx, x - 60, y - 70, 240, 200, R2D.withAlpha(Theme.accentWave(0), 0.45f));
        R2D.glow(ctx, x + w - 170, y - 50, 230, 180, R2D.withAlpha(Theme.accent2(), 0.3f));
        R2D.endScissor(ctx);
        R2D.roundOutlineGrad(ctx, x, y, w, bh, 12, 0.7f, R2D.withAlpha(Theme.accentWave(0), 0.7f), R2D.withAlpha(Theme.accent2(), 0.3f));
        int hour = java.time.LocalTime.now().getHour();
        String greet = Lang.get(hour < 5 ? "home.night" : hour < 12 ? "home.morning" : hour < 18 ? "home.afternoon" : hour < 22 ? "home.evening" : "home.night");
        String name = dev.vesc.client.module.Legit.name();
        Fonts.draw(ctx, greet + ", " + name, x + 16, y + 15, Theme.text(), Font.BOLD, 1.25f);
        java.util.Locale loc = Lang.pl() ? java.util.Locale.forLanguageTag("pl-PL") : java.util.Locale.ENGLISH;
        String date = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern(Lang.pl() ? "EEEE, d MMMM  •  HH:mm" : "EEEE, MMMM d  •  HH:mm", loc));
        Fonts.draw(ctx, date.substring(0, 1).toUpperCase(loc) + date.substring(1), x + 16, y + 34, Theme.textDim(), Font.REGULAR, 0.72f);
        String ver = "VescClient " + VescClient.VERSION;
        float vw = Fonts.width(ver, Font.BOLD, 0.6f) + 12;
        R2D.roundRect(ctx, x + 16, y + 46, vw, 11, 5.5f, R2D.withAlpha(Theme.accentWave(0), 0.25f));
        Fonts.drawV(ctx, ver, x + 22, y + 51.8f, Theme.accentWave(0), Font.BOLD, 0.6f);
        R2D.glow(ctx, x + w - 66, y + 2, 60, 60, R2D.withAlpha(Theme.accentWave(0), 0.5f));
        R2D.icon(ctx, "logo", x + w - 50, y + 18, 28, Theme.accentWave(0));
        float cy = y + bh + 10;

        // kafelki statystyk
        long sess = (System.currentTimeMillis() - rc.startTime) / 1000;
        String[][] tiles = {
                {"layers", rc.modules.enabledCount() + "/" + rc.modules.all().size(), Lang.get("home.enabled")},
                {"keyboard", String.valueOf(rc.modules.all().stream().filter(Module::hasKey).count()), Lang.get("home.binds")},
                {"clock", String.format("%d:%02d", sess / 3600, (sess / 60) % 60), Lang.get("home.session")},
                {"gauge", client.getCurrentFps() + "", "FPS"},
        };
        float tg = 8, tw = (w - tg * 3) / 4f, th = 42;
        for (int i = 0; i < 4; i++) {
            float tx = x + i * (tw + tg);
            boolean hv = hovered(tx, cy, tw, th);
            float ha = anim("htile" + i, 18).animate(hv ? 1 : 0);
            R2D.roundRect(ctx, tx, cy - ha, tw, th, 9, R2D.blend(Theme.card(), Theme.cardHover(), ha));
            R2D.roundOutline(ctx, tx, cy - ha, tw, th, 9, 0.5f, Theme.stroke());
            R2D.roundRect(ctx, tx + 8, cy - ha + 8, 14, 14, 4.5f, R2D.withAlpha(Theme.accentWave(i * 0.6f), 0.22f));
            R2D.icon(ctx, tiles[i][0], tx + 11, cy - ha + 11, 8, Theme.accentWave(i * 0.6f));
            Fonts.draw(ctx, tiles[i][1], tx + 27, cy - ha + 8, Theme.text(), Font.BOLD, 0.95f);
            Fonts.draw(ctx, Fonts.trim(tiles[i][2], Font.REGULAR, 0.6f, tw - 14), tx + 8, cy - ha + 28, Theme.textFaint(), Font.REGULAR, 0.6f);
        }
        cy += th + 10;

        // ulubione + ostatnio używane
        float cg = 8, colW = (w - cg) / 2f, ch = 130;
        for (int c = 0; c < 2; c++) {
            float bx = x + c * (colW + cg);
            R2D.roundRect(ctx, bx, cy, colW, ch, 10, Theme.card());
            R2D.roundOutline(ctx, bx, cy, colW, ch, 10, 0.5f, Theme.stroke());
            R2D.icon(ctx, c == 0 ? "star" : "clock", bx + 10, cy + 9, 8, Theme.accentWave(c));
            Fonts.drawV(ctx, Lang.get(c == 0 ? "home.favorites" : "home.recent"), bx + 23, cy + 13.5f, Theme.text(), Font.BOLD, 0.78f);
            float ry = cy + 26;
            List<Module> list = new ArrayList<>();
            List<Long> times = new ArrayList<>();
            if (c == 0) list.addAll(favorites());
            else for (Object[] o : rc.modules.recent) { list.add((Module) o[0]); times.add((Long) o[1]); }
            if (list.isEmpty())
                Fonts.drawCentered(ctx, Lang.get(c == 0 ? "home.nofav" : "home.norecent"), bx + colW / 2f, cy + ch / 2f + 4, Theme.textFaint(), Font.REGULAR, 0.66f);
            for (int i = 0; i < Math.min(5, list.size()); i++) {
                Module m = list.get(i);
                boolean hv = hovered(bx + 6, ry, colW - 12, 19);
                float ha = anim("hrow" + c + m.getName(), 18).animate(hv ? 1 : 0);
                R2D.roundRect(ctx, bx + 6, ry, colW - 12, 19, 6, R2D.withAlpha(Theme.cardHover(), ha));
                float on = m.toggleAnim.animate(m.isEnabled() ? 1 : 0);
                R2D.icon(ctx, m.getIcon(), bx + 12, ry + 5.5f, 8, R2D.blend(Theme.textFaint(), Theme.accentWave(0), on));
                Fonts.drawV(ctx, m.getName(), bx + 25, ry + 10, R2D.blend(Theme.textDim(), Theme.text(), on), Font.SEMIBOLD, 0.72f);
                if (c == 0) drawSwitch(ctx, bx + colW - 32, ry + 4, on);
                else {
                    String st = (m.isEnabled() ? "ON" : "OFF") + "  •  " + ago(times.get(i));
                    Fonts.drawV(ctx, st, bx + colW - 12 - Fonts.width(st, Font.SEMIBOLD, 0.56f), ry + 10, m.isEnabled() ? Theme.accentWave(0) : Theme.textFaint(), Font.SEMIBOLD, 0.56f);
                }
                hit(bx + 6, ry, colW - 12, 19, b -> { if (b == 0) m.toggle(); else if (b == 1) { setPage(Page.CATEGORY); category = m.getCategory(); inspected = m; } });
                ry += 20;
            }
        }
        cy += ch + 10;

        // szybkie akcje
        String[][] acts = {{"move", "hud.editor.open"}, {"themes", "page.themes"}, {"config", "page.config"}, {"keybind", "home.palette"}};
        float ag = 8, aw = (w - ag * 3) / 4f;
        for (int i = 0; i < 4; i++) {
            final int fi = i;
            button(ctx, "hact" + i, x + i * (aw + ag), cy, aw, 22, acts[i][0], Lang.get(acts[i][1]), i == 0, () -> {
                switch (fi) {
                    case 0 -> { if (client != null) client.setScreen(new HudEditor()); }
                    case 1 -> setPage(Page.THEMES);
                    case 2 -> setPage(Page.CONFIG);
                    default -> openPalette();
                }
            });
        }
        return cy + 22 - y + 6;
    }

    // ================================================================== zaawansowane: inspektor, toolbar, paleta

    private void inspect(Module m) {
        if (inspected == m) { inspected = null; return; }
        inspected = m;
        inspScrollTarget = 0;
        inspScroll.snap(0);
        openColor = null;
        editing = null;
    }

    private void drawToolbar(DrawContext ctx, float x, float y, float w) {
        List<Module> base = currentModules();
        Filter[] fs = Filter.values();
        // gdy brakuje miejsca (np. otwarty inspektor) – chipy bez liczników, sortowanie jako ikona
        float full = 0;
        for (Filter f : fs) full += Fonts.width(Lang.get("filter." + f.name().toLowerCase(Locale.ROOT)), Font.SEMIBOLD, 0.64f) + 30;
        String slFull = Lang.get("sort." + sort.name().toLowerCase(Locale.ROOT));
        boolean compact = full + Fonts.width(slFull, Font.SEMIBOLD, 0.62f) + 22 + 46 > w;
        float cx = x;
        for (Filter f : fs) {
            long count = switch (f) {
                case ENABLED -> base.stream().filter(Module::isEnabled).count();
                case DISABLED -> base.stream().filter(m -> !m.isEnabled()).count();
                case BOUND -> base.stream().filter(Module::hasKey).count();
                default -> base.size();
            };
            String label = Lang.get("filter." + f.name().toLowerCase(Locale.ROOT));
            String cnt = compact ? "" : String.valueOf(count);
            float cw = compact ? Fonts.width(label, Font.SEMIBOLD, 0.64f) + 12 : Fonts.width(label, Font.SEMIBOLD, 0.64f) + Fonts.width(cnt, Font.BOLD, 0.56f) + 18;
            boolean act = filter == f;
            boolean hv = hovered(cx, y, cw, 15);
            float aa = anim("flt" + f, 16).animate(act ? 1 : 0), ha = anim("flth" + f, 18).animate(hv ? 1 : 0);
            R2D.roundRect(ctx, cx, y, cw, 15, 7.5f, R2D.blend(R2D.blend(Theme.card(), Theme.cardHover(), ha), Theme.accent(), aa));
            if (aa < 0.5f) R2D.roundOutline(ctx, cx, y, cw, 15, 7.5f, 0.5f, Theme.stroke());
            Fonts.drawV(ctx, label, cx + 7, y + 8, R2D.blend(Theme.textDim(), Theme.accentText(), aa), Font.SEMIBOLD, 0.64f);
            if (!compact) Fonts.drawV(ctx, cnt, cx + cw - 7 - Fonts.width(cnt, Font.BOLD, 0.56f), y + 8, R2D.blend(Theme.textFaint(), R2D.withAlpha(Theme.accentText(), 0.7f), aa), Font.BOLD, 0.56f);
            final Filter ff = f;
            hit(cx, y, cw, 15, b -> { if (b == 0) { filter = ff; listTime = System.currentTimeMillis(); focusIndex = -1; scrollTarget = 0; } });
            cx += cw + (compact ? 3 : 5);
        }
        // widok siatka / lista
        float vx = x + w - 34;
        for (int i = 0; i < 2; i++) {
            boolean act = (i == 1) == listView;
            float bx = vx + i * 17;
            boolean hv = hovered(bx, y, 15, 15);
            float aa = anim("view" + i, 16).animate(act ? 1 : 0);
            R2D.roundRect(ctx, bx, y, 15, 15, 4.5f, R2D.blend(hv ? Theme.cardHover() : Theme.card(), Theme.accent(), aa));
            R2D.icon(ctx, i == 0 ? "misc" : "list", bx + 3.5f, y + 3.5f, 8, R2D.blend(Theme.textDim(), Theme.accentText(), aa));
            final boolean lv = i == 1;
            hit(bx, y, 15, 15, b -> { if (b == 0 && listView != lv) { listView = lv; listTime = System.currentTimeMillis(); } });
        }
        // sortowanie
        String sl = compact ? "" : slFull;
        float sw = compact ? 15 : Fonts.width(sl, Font.SEMIBOLD, 0.62f) + 22;
        float sx = vx - 6 - sw;
        boolean hv = hovered(sx, y, sw, 15);
        R2D.roundRect(ctx, sx, y, sw, 15, 7.5f, hv ? Theme.cardHover() : Theme.card());
        R2D.roundOutline(ctx, sx, y, sw, 15, 7.5f, 0.5f, Theme.stroke());
        R2D.icon(ctx, "layers", sx + (compact ? 4 : 5), y + 4, 7, Theme.accentWave(0));
        if (!compact) Fonts.drawV(ctx, sl, sx + 15, y + 8, Theme.textDim(), Font.SEMIBOLD, 0.62f);
        hit(sx, y, sw, 15, b -> {
            Sort[] ss = Sort.values();
            sort = ss[Math.floorMod(sort.ordinal() + (b == 1 ? -1 : 1), ss.length)];
            listTime = System.currentTimeMillis();
        });
    }

    private void drawInspector(DrawContext ctx, float x, float y, float w, float h, float a) {
        Module m = lastInspected;
        inspX = x; inspY = y; inspW = w; inspH = h;
        float prev = R2D.alpha;
        R2D.alpha = prev * a;
        R2D.shadow(ctx, x, y, w, h, 14, Theme.shadow());
        R2D.roundRect(ctx, x, y + 4, w - 8, h - 4, 10, Theme.sidebar());
        R2D.roundOutline(ctx, x, y + 4, w - 8, h - 4, 10, 0.6f, R2D.blend(Theme.stroke(), Theme.accent(), 0.25f));
        float pw = w - 8;
        hit(x, y + 4, pw, h - 4, b -> {}); // blokada kliknięć pod panelem

        R2D.scissor(ctx, x, y + 4, pw, h - 4);
        float[] oc = clip;
        clip = new float[]{x, y + 4, pw, h - 4};
        float sc = inspScroll.animate(inspScrollTarget);
        float ix = x + 12, iw = pw - 24, cy = y + 16 - sc;

        // nagłówek
        float on = m.toggleAnim.animate(m.isEnabled() ? 1 : 0);
        R2D.roundRect(ctx, ix, cy, 30, 30, 9, R2D.blend(Theme.toggleOff(), Theme.accentWave(0), on));
        R2D.icon(ctx, m.getIcon(), ix + 7, cy + 7, 16, R2D.blend(Theme.textDim(), Theme.accentText(), on));
        Fonts.draw(ctx, Fonts.trim(m.getName(), Font.BOLD, 1.05f, iw - 60), ix + 38, cy + 3, Theme.text(), Font.BOLD, 1.05f);
        String cat = m.getCategory().getName();
        float chw = Fonts.width(cat, Font.SEMIBOLD, 0.56f) + 10;
        R2D.roundRect(ctx, ix + 38, cy + 17, chw, 10, 5, R2D.withAlpha(Theme.accentWave(0), 0.18f));
        Fonts.drawV(ctx, cat, ix + 43, cy + 22.3f, Theme.accentWave(0), Font.SEMIBOLD, 0.56f);
        // zamknij
        float bx = ix + iw - 14;
        boolean hvx = hovered(bx, cy, 16, 16);
        R2D.roundRect(ctx, bx, cy, 16, 16, 8, hvx ? Theme.cardHover() : Theme.card());
        R2D.icon(ctx, "cross", bx + 4.5f, cy + 4.5f, 7, hvx ? Theme.text() : Theme.textFaint());
        hit(bx, cy, 16, 16, b -> inspected = null);
        cy += 38;
        for (String line : Fonts.wrap(m.getDescription(), Font.REGULAR, 0.68f, iw)) {
            Fonts.draw(ctx, line, ix, cy, Theme.textDim(), Font.REGULAR, 0.68f);
            cy += 9;
        }
        cy += 6;

        // akcje: włącz, klawisz, ulubione, reset
        cy = inspRow(ctx, ix, cy, iw, "power", Lang.get("insp.enabled"), () -> {}, null);
        drawSwitch(ctx, ix + iw - 28, cy - 22 + 11 - 5.5f, on);
        final float rowY = cy - 22;
        hit(ix, rowY, iw, 20, b -> { if (b == 0) m.toggle(); });

        boolean listening = binding == m;
        String key = listening ? Lang.get("bind.listening") : Keys.name(m.getKey());
        cy = inspRow(ctx, ix, cy, iw, "keybind", Lang.get("insp.keybind"), () -> {}, null);
        float kw = Fonts.width(key, Font.BOLD, 0.62f) + 12;
        float la = anim("ibind", 16).animate(listening ? 1 : 0);
        R2D.roundRect(ctx, ix + iw - 8 - kw, cy - 22 + 4, kw, 12, 6, R2D.blend(Theme.toggleOff(), Theme.accentWave(0), la));
        Fonts.drawV(ctx, key, ix + iw - 8 - kw + 6, cy - 22 + 10.3f, R2D.blend(Theme.text(), Theme.accentText(), la), Font.BOLD, 0.62f);
        final float kRow = cy - 22;
        hit(ix, kRow, iw, 20, b -> {
            if (b == 0) binding = binding == m ? null : m;
            else if (b == 1) { m.setKey(GLFW.GLFW_KEY_UNKNOWN); binding = null; }
        });

        // mini przyciski
        float half = (iw - 6) / 2f;
        float fa = m.favAnim.animate(m.favorite ? 1 : 0);
        button(ctx, "ifav", ix, cy, half, 20, "star", Lang.get(m.favorite ? "insp.unfav" : "insp.fav"), fa > 0.5f, () -> {
            m.favorite = !m.favorite;
            rc.config.save(rc.config.current());
        });
        button(ctx, "ireset", ix + half + 6, cy, half, 20, "reset", Lang.get("insp.reset"), false, () -> {
            for (Setting<?> st : m.getSettings()) st.reset();
            rc.notifications.info("reset", Lang.get("insp.resetdone"), m.getName());
        });
        cy += 28;

        // ustawienia w sekcjach
        String[] sections = {"insp.modes", "insp.values", "insp.toggles", "insp.colors"};
        boolean any = false;
        for (int sIdx = 0; sIdx < 4; sIdx++) {
            List<Setting<?>> group = new ArrayList<>();
            for (Setting<?> st : m.getSettings()) {
                int g = st instanceof ModeSetting ? 0 : st instanceof NumberSetting ? 1 : st instanceof BoolSetting ? 2 : 3;
                if (g == sIdx) group.add(st);
            }
            if (group.isEmpty()) continue;
            any = true;
            Fonts.draw(ctx, Lang.get(sections[sIdx]) + "  " + group.size(), ix, cy, Theme.textFaint(), Font.SEMIBOLD, 0.58f);
            R2D.gradientH(ctx, ix + Fonts.width(Lang.get(sections[sIdx]) + "  " + group.size(), Font.SEMIBOLD, 0.58f) + 6, cy + 3, iw / 2f, 0.5f, Theme.stroke(), R2D.withAlpha(Theme.stroke(), 0));
            cy += 10;
            for (Setting<?> st : group) cy = drawInspSetting(ctx, st, ix, cy, iw);
            cy += 6;
        }
        if (!any) {
            Fonts.drawCentered(ctx, Lang.get("insp.nosettings"), ix + iw / 2f, cy + 6, Theme.textFaint(), Font.REGULAR, 0.7f);
            cy += 20;
        }
        float contentH = cy + sc - (y + 16);
        inspMax = Math.max(0, contentH - (h - 24));
        inspScrollTarget = Math.max(0, Math.min(inspMax, inspScrollTarget));
        clip = oc;
        R2D.endScissor(ctx);
        if (inspMax > 0) {
            float trackH = h - 24, thumbH = Math.max(18, trackH * (h - 24) / contentH);
            R2D.roundRect(ctx, x + pw - 5, y + 14 + (trackH - thumbH) * (sc / inspMax), 2, thumbH, 1, Theme.accentWave(0));
        }
        R2D.alpha = prev;
    }

    /** wiersz w inspektorze z ikoną i etykietą (zwraca y po wierszu) */
    private float inspRow(DrawContext ctx, float x, float y, float w, String icon, String label, Runnable unused, String unused2) {
        boolean hv = hovered(x, y, w, 20);
        float ha = anim("irow" + label, 18).animate(hv ? 1 : 0);
        R2D.roundRect(ctx, x, y, w, 20, 6, R2D.blend(Theme.card(), Theme.cardHover(), ha));
        R2D.icon(ctx, icon, x + 7, y + 6, 8, Theme.textDim());
        Fonts.drawV(ctx, label, x + 20, y + 10.5f, Theme.text(), Font.SEMIBOLD, 0.7f);
        return y + 22;
    }

    private float drawInspSetting(DrawContext ctx, Setting<?> s, float x, float y, float w) {
        boolean changed = s.changed();
        float nx = x + (changed ? 8 : 0);
        if (changed) {
            boolean hv = hovered(x - 2, y, 9, 18);
            R2D.circle(ctx, x + 2.5f, y + 9, hv ? 2.6f : 1.9f, Theme.accentWave(0));
            hit(x - 2, y, 9, 18, b -> s.reset());
        }
        if (s instanceof BoolSetting b) {
            float h = 18;
            Fonts.drawV(ctx, s.getName(), nx, y + h / 2f + 0.5f, Theme.textDim(), Font.SEMIBOLD, 0.7f);
            drawSwitch(ctx, x + w - 20, y + h / 2f - 5.5f, b.anim.animate(b.get() ? 1 : 0));
            hit(x + 10, y, w - 10, h, bt -> { if (bt == 0) b.toggle(); else if (bt == 2) b.reset(); });
            return y + h + 2;
        }
        if (s instanceof ModeSetting md) {
            float h = 18;
            Fonts.drawV(ctx, s.getName(), nx, y + h / 2f + 0.5f, Theme.textDim(), Font.SEMIBOLD, 0.7f);
            String v = md.get();
            float bw = Math.min(w * 0.55f, Fonts.width(v, Font.BOLD, 0.64f) + 24);
            float bx = x + w - bw;
            boolean hov = hovered(bx, y + 2, bw, h - 4);
            R2D.roundRect(ctx, bx, y + 2, bw, h - 4, 7, hov ? Theme.cardHover() : Theme.toggleOff());
            R2D.icon(ctx, "arrow_left", bx + 4, y + h / 2f - 2.5f, 5, Theme.textFaint());
            ctx.getMatrices().push();
            ctx.getMatrices().translate(bx + bw - 6.5f, y + h / 2f, 0);
            ctx.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(180));
            R2D.icon(ctx, "arrow_left", -2.5f, -2.5f, 5, Theme.textFaint());
            ctx.getMatrices().pop();
            Fonts.drawCentered(ctx, Fonts.trim(v, Font.BOLD, 0.64f, bw - 18), bx + bw / 2f, y + h / 2f - Fonts.height(0.64f) / 2f + 0.5f, Theme.text(), Font.BOLD, 0.64f);
            hit(bx, y + 2, bw, h - 4, bt -> {
                if (bt == 2) md.reset(); else md.cycle(bt == 1 ? -1 : 1);
                for (Module mm : rc.modules.all())
                    if (mm instanceof dev.vesc.client.module.cosmetics.HitSound hs && hs.sound == md) hs.play();
            });
            return y + h + 2;
        }
        if (s instanceof NumberSetting n) {
            float h = 28;
            Fonts.draw(ctx, s.getName(), nx, y + 3, Theme.textDim(), Font.SEMIBOLD, 0.7f);
            boolean ed = editing == n;
            String v = ed ? editBuffer : n.display();
            float vw = Fonts.width(v.isEmpty() ? "0" : v, Font.BOLD, 0.68f) + 10;
            float vx = x + w - vw;
            boolean hv = hovered(vx, y, vw, 12);
            float ea = anim("edit" + System.identityHashCode(n), 18).animate(ed ? 1 : hv ? 0.5f : 0);
            R2D.roundRect(ctx, vx, y, vw, 12, 4, R2D.withAlpha(R2D.blend(Theme.toggleOff(), Theme.accentWave(0), ed ? 0.35f : 0), ea));
            if (ed) R2D.roundOutline(ctx, vx, y, vw, 12, 4, 0.6f, Theme.accentWave(0));
            Fonts.drawV(ctx, v, vx + 5, y + 6.5f, Theme.text(), Font.BOLD, 0.68f);
            if (ed && (System.currentTimeMillis() / 500) % 2 == 0) R2D.rect(ctx, vx + 5 + Fonts.width(v, Font.BOLD, 0.68f) + 0.5f, y + 2.5f, 0.6f, 7, Theme.text());
            hit(vx, y, vw, 12, b -> { if (b == 0) { editing = n; editBuffer = n.display(); } else if (b == 2) n.reset(); });
            drawSlider(ctx, n, x, y + 19, w);
            return y + h + 2;
        }
        if (s instanceof dev.vesc.client.setting.ColorSetting c) {
            float h = 18;
            Fonts.drawV(ctx, s.getName(), nx, y + h / 2f + 0.5f, Theme.textDim(), Font.SEMIBOLD, 0.7f);
            String hex = c.hex();
            float hw = Fonts.width(hex, Font.SEMIBOLD, 0.6f);
            float sx = x + w - 22;
            R2D.roundRect(ctx, sx, y + 3, 22, 12, 4, c.get());
            R2D.roundOutline(ctx, sx, y + 3, 22, 12, 4, 0.6f, Theme.stroke());
            Fonts.drawV(ctx, hex, sx - 5 - hw, y + h / 2f + 0.5f, Theme.textFaint(), Font.SEMIBOLD, 0.6f);
            hit(sx - 8 - hw, y, w - (sx - 8 - hw - x), h, b -> { if (b == 0) openColor = openColor == c ? null : c; else if (b == 2) c.reset(); });
            float oa = Anim.easeOutCubic(anim("cp" + System.identityHashCode(c), 14).animate(openColor == c ? 1 : 0));
            float ph = 76 * oa;
            if (ph > 1) {
                R2D.scissor(ctx, x, y + h, w, ph);
                float[] oc = clip;
                clip = intersect(clip, new float[]{x, y + h, w, ph});
                float px = x, py = y + h + 4, sqW = w - 18, sqH = 64;
                // pole saturacja / jasność
                R2D.gradientH(ctx, px, py, sqW, sqH, 0xFFFFFFFF, c.hueColor());
                R2D.gradientV(ctx, px, py, sqW, sqH, 0x00000000, 0xFF000000);
                R2D.roundOutline(ctx, px, py, sqW, sqH, 3, 0.5f, Theme.stroke());
                float kx = px + c.sat * sqW, ky = py + (1 - c.bri) * sqH;
                R2D.circle(ctx, kx, ky, 3.4f, 0xFFFFFFFF);
                R2D.circle(ctx, kx, ky, 2.4f, c.get());
                // pasek odcienia
                float hx = px + sqW + 6, hwid = 12;
                int[] hues = {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
                for (int i = 0; i < 6; i++) R2D.gradientV(ctx, hx, py + sqH * i / 6f, hwid, sqH / 6f + 0.5f, hues[i], hues[i + 1]);
                R2D.roundOutline(ctx, hx, py, hwid, sqH, 3, 0.5f, Theme.stroke());
                float hy = py + c.hue * sqH;
                R2D.roundRect(ctx, hx - 1.5f, hy - 1.5f, hwid + 3, 3, 1.5f, 0xFFFFFFFF);
                final float fpx = px, fpy = py, fsw = sqW, fsh = sqH, fhx = hx;
                hit(px, py, sqW, sqH, b -> { draggingColor = c; colorPart = 0; cpX = fpx; cpY = fpy; cpW = fsw; cpH = fsh; });
                hit(hx - 2, py, hwid + 4, sqH, b -> { draggingColor = c; colorPart = 1; cpX = fhx; cpY = fpy; cpW = hwid; cpH = fsh; });
                clip = oc;
                R2D.endScissor(ctx);
            }
            return y + h + 2 + ph;
        }
        return y;
    }

    private void commitEdit() {
        if (editing == null) return;
        try { editing.set(Double.parseDouble(editBuffer.isEmpty() ? "0" : editBuffer)); } catch (NumberFormatException ignored) {}
        if (editing == rc.settings.animSpeed) Anim.globalSpeed = editing.get().floatValue();
        editing = null;
    }

    private void drawResizeGrip(DrawContext ctx) {
        float gx = x0 + W - 14, gy = y0 + H - 14;
        boolean hv = hovered(gx - 2, gy - 2, 14, 14) || resizing;
        float ha = anim("grip", 18).animate(hv ? 1 : 0);
        int c = R2D.blend(Theme.textFaint(), Theme.accentWave(0), ha);
        for (int i = 0; i < 3; i++)
            for (int j = 0; j <= i; j++) R2D.circle(ctx, gx + 8 - j * 3.2f, gy + 8 - (i - j) * 3.2f, 0.9f, c);
        hit(gx - 2, gy - 2, 14, 14, b -> { if (b == 0) { resizing = true; rsW = W; rsH = H; rsMx = mx; rsMy = my; } });
    }

    private void drawTooltip(DrawContext ctx) {
        Module m = tooltipModule;
        if (m == null || palette || System.currentTimeMillis() - tooltipStart < 650) return;
        float a = Anim.clamp01((System.currentTimeMillis() - tooltipStart - 650) / 150f);
        float prev = R2D.alpha;
        R2D.alpha = prev * a;
        float tw = 190;
        List<String> lines = Fonts.wrap(m.getDescription(), Font.REGULAR, 0.66f, tw - 16);
        String meta = m.getCategory().getName() + "  •  " + Lang.format("tip.settings", m.getSettings().size())
                + (m.hasKey() ? "  •  " + Keys.name(m.getKey()) : "");
        float th = 22 + lines.size() * 9 + 12;
        float maxX = panelMode() ? vLeft + vW : x0 + W, maxY = panelMode() ? vTop + vH : y0 + H;
        float tx = Math.min(mx + 12, maxX - tw - 6), ty = Math.min(my + 14, maxY - th - 6);
        R2D.shadow(ctx, tx, ty, tw, th, 10, Theme.shadow());
        R2D.roundRect(ctx, tx, ty, tw, th, 8, opaque(Theme.bg()));
        R2D.roundOutline(ctx, tx, ty, tw, th, 8, 0.6f, R2D.blend(Theme.stroke(), Theme.accent(), 0.35f));
        R2D.icon(ctx, m.getIcon(), tx + 8, ty + 7, 8, Theme.accentWave(0));
        Fonts.draw(ctx, m.getName(), tx + 20, ty + 7, Theme.text(), Font.BOLD, 0.78f);
        float ly = ty + 19;
        for (String l : lines) { Fonts.draw(ctx, l, tx + 8, ly, Theme.textDim(), Font.REGULAR, 0.66f); ly += 9; }
        Fonts.draw(ctx, meta, tx + 8, ly + 2, Theme.textFaint(), Font.SEMIBOLD, 0.58f);
        R2D.alpha = prev;
    }

    // ------------------------------------------------------------------ paleta komend (Ctrl+K)

    private void openPalette() {
        palette = true;
        paletteQuery = "";
        paletteSel = 0;
        searchFocused = false;
        friendFocused = false;
        editing = null;
    }

    /** dopasowanie "fuzzy": litery zapytania po kolei w tekście; wyższy wynik = lepiej (-1 = brak) */
    private static int fuzzy(String q, String t) {
        if (q.isEmpty()) return 1;
        q = q.toLowerCase(Locale.ROOT); t = t.toLowerCase(Locale.ROOT);
        int idx = t.indexOf(q);
        if (idx >= 0) return 1000 - idx * 4 - (t.length() - q.length());
        int score = 0, ti = 0, streak = 0;
        for (int qi = 0; qi < q.length(); qi++) {
            char c = q.charAt(qi);
            int found = t.indexOf(c, ti);
            if (found < 0) return -1;
            streak = found == ti ? streak + 1 : 0;
            score += 10 + streak * 6 - (found - ti);
            if (found == 0 || t.charAt(found - 1) == ' ') score += 8;
            ti = found + 1;
        }
        return score;
    }

    private List<PItem> buildPalette() {
        List<PItem> all = new ArrayList<>();
        String go = Lang.get("pal.goto");
        for (Module m : rc.modules.all())
            all.add(new PItem(m.getIcon(), m.getName(), m.getCategory().getName(), m::toggle, m, true));
        for (Category c : Category.values())
            all.add(new PItem(c.icon, go + ": " + c.getName(), Lang.get("pal.page"), () -> { setPage(Page.CATEGORY); category = c; search = ""; }, null, false));
        Object[][] pages = {{"star", "cat.favorites", Page.FAVORITES}, {"users", "page.friends", Page.FRIENDS}, {"hud", "page.hud", Page.HUD},
                {"themes", "page.themes", Page.THEMES}, {"config", "page.config", Page.CONFIG}, {"profile", "page.profile", Page.PROFILE}, {"settings", "page.settings", Page.SETTINGS}};
        for (Object[] pg : pages)
            all.add(new PItem((String) pg[0], go + ": " + Lang.get((String) pg[1]), Lang.get("pal.page"), () -> setPage((Page) pg[2]), null, false));
        for (Theme.Preset tp : Theme.Preset.values())
            all.add(new PItem("themes", Lang.get("pal.theme") + ": " + tp.getName(), Lang.get("pal.action"), () -> { Theme.set(tp); rc.config.saveClient(); }, null, true));
        for (Theme.Accent ac : Theme.Accent.values())
            all.add(new PItem("accent", Lang.get("pal.accent") + ": " + ac.getName(), Lang.get("pal.action"), () -> { Theme.setAccent(ac); rc.config.saveClient(); }, null, true));
        for (Lang.Language l : Lang.Language.values())
            all.add(new PItem("globe_lang", Lang.get("pal.language") + ": " + l.display, Lang.get("pal.action"), () -> { Lang.set(l); rc.config.saveClient(); }, null, true));
        all.add(new PItem("move", Lang.get("hud.editor.open"), Lang.get("pal.action"), () -> { if (client != null) client.setScreen(new HudEditor()); }, null, false));
        all.add(new PItem("disk", Lang.get("pal.save"), Lang.get("pal.action"), () -> {
            rc.config.save(rc.config.current());
            rc.notifications.info("disk", Lang.get("config.saved"), rc.config.current());
        }, null, false));
        all.add(new PItem("power", Lang.get("pal.disableall"), Lang.get("pal.action"), () -> {
            for (Module m : rc.modules.all()) if (m.isEnabled()) m.setEnabledSilent(false);
        }, null, true));
        // filtrowanie + sortowanie wynikiem
        List<Object[]> scored = new ArrayList<>();
        for (PItem it : all) {
            int sc = Math.max(fuzzy(paletteQuery, it.title()), fuzzy(paletteQuery, it.sub()) - 200);
            if (sc >= 0) scored.add(new Object[]{it, sc});
        }
        if (!paletteQuery.isEmpty()) scored.sort((p1, p2) -> Integer.compare((int) p2[1], (int) p1[1]));
        List<PItem> out = new ArrayList<>();
        for (Object[] o : scored) out.add((PItem) o[0]);
        return out;
    }

    private void runPalette(PItem it) {
        it.action().run();
        if (!it.keepOpen()) palette = false;
    }

    private void paletteKey(int key) {
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> palette = false;
            case GLFW.GLFW_KEY_DOWN -> paletteSel = Math.min(paletteItems.size() - 1, paletteSel + 1);
            case GLFW.GLFW_KEY_UP -> paletteSel = Math.max(0, paletteSel - 1);
            case GLFW.GLFW_KEY_BACKSPACE -> { if (!paletteQuery.isEmpty()) { paletteQuery = Screen.hasControlDown() ? "" : paletteQuery.substring(0, paletteQuery.length() - 1); paletteSel = 0; } }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (paletteSel >= 0 && paletteSel < paletteItems.size()) {
                    PItem it = paletteItems.get(paletteSel);
                    if (Screen.hasShiftDown() && it.module() != null) { palette = false; inspect(it.module()); }
                    else runPalette(it);
                }
            }
            default -> {}
        }
    }

    private void drawPalette(DrawContext ctx) {
        float pa = Anim.easeOutCubic(paletteA.animate(palette ? 1 : 0));
        if (pa < 0.01f) return;
        paletteDrawing = true;
        paletteItems = buildPalette();
        if (paletteSel >= paletteItems.size()) paletteSel = Math.max(0, paletteItems.size() - 1);
        float prev = R2D.alpha;
        R2D.alpha = prev * pa;
        if (panelMode()) { R2D.rect(ctx, vLeft, vTop, vW, vH, R2D.withAlpha(0xFF000000, 0.45f)); hit(vLeft, vTop, vW, vH, b -> palette = false); }
        else { R2D.roundRect(ctx, x0, y0, W, H, 14, R2D.withAlpha(0xFF000000, 0.45f)); hit(x0, y0, W, H, b -> palette = false); }

        float pw = Math.min(400, W - 80), px = x0 + (W - pw) / 2f, py = y0 + 54 - (1 - pa) * 12;
        int visible = Math.min(8, paletteItems.size());
        int start = Math.max(0, Math.min(paletteSel - 3, paletteItems.size() - visible));
        float rowH = 24, ph = 34 + Math.max(1, visible) * rowH + 26;
        R2D.shadow(ctx, px, py, pw, ph, 22, Theme.shadow());
        R2D.glow(ctx, px - 40, py - 40, pw + 80, ph + 80, R2D.withAlpha(Theme.accentWave(0), 0.08f));
        R2D.roundRect(ctx, px, py, pw, ph, 12, Theme.bg());
        R2D.roundOutline(ctx, px, py, pw, ph, 12, 0.7f, R2D.blend(Theme.stroke(), Theme.accentWave(0), 0.5f));
        hit(px, py, pw, ph, b -> {});

        // pole wpisywania
        R2D.icon(ctx, "search", px + 13, py + 12, 10, Theme.accentWave(0));
        String shown = paletteQuery.isEmpty() ? Lang.get("pal.placeholder") : paletteQuery;
        Fonts.drawV(ctx, shown, px + 30, py + 17.5f, paletteQuery.isEmpty() ? Theme.textFaint() : Theme.text(), Font.SEMIBOLD, 0.86f);
        if ((System.currentTimeMillis() / 500) % 2 == 0)
            R2D.rect(ctx, px + 30 + (paletteQuery.isEmpty() ? 0 : Fonts.width(paletteQuery, Font.SEMIBOLD, 0.86f) + 1), py + 11, 0.7f, 13, Theme.text());
        float ew = Fonts.width("ESC", Font.BOLD, 0.56f) + 10;
        R2D.roundRect(ctx, px + pw - 12 - ew, py + 11, ew, 13, 4, Theme.toggleOff());
        Fonts.drawV(ctx, "ESC", px + pw - 12 - ew + 5, py + 17.8f, Theme.textDim(), Font.BOLD, 0.56f);
        R2D.rect(ctx, px + 10, py + 33, pw - 20, 0.5f, Theme.stroke());

        float ry = py + 37;
        if (paletteItems.isEmpty())
            Fonts.drawCentered(ctx, Lang.pl() ? "Brak wyników" : "No results", px + pw / 2f, ry + 7, Theme.textFaint(), Font.SEMIBOLD, 0.75f);
        for (int i = start; i < start + visible; i++) {
            PItem it = paletteItems.get(i);
            boolean sel = i == paletteSel;
            boolean hv = hovered(px + 6, ry, pw - 12, rowH - 2);
            if (hv && !sel) paletteSel = i;
            float sa = anim("pal" + i, 22).animate(sel ? 1 : 0);
            R2D.roundRect(ctx, px + 6, ry, pw - 12, rowH - 2, 7, R2D.withAlpha(Theme.accentWave(0), 0.18f * sa));
            if (sa > 0.5f) R2D.roundRect(ctx, px + 6, ry + 6, 2, rowH - 14, 1, Theme.accentWave(0));
            boolean on = it.module() != null && it.module().isEnabled();
            R2D.roundRect(ctx, px + 14, ry + 3, 16, 16, 5, on ? Theme.accentWave(0) : Theme.toggleOff());
            R2D.icon(ctx, it.icon(), px + 18, ry + 7, 8, on ? Theme.accentText() : Theme.textDim());
            Fonts.drawV(ctx, it.title(), px + 37, ry + rowH / 2f, Theme.text(), Font.SEMIBOLD, 0.76f);
            String sub = it.module() != null ? it.sub() + "  •  " + (on ? "ON" : "OFF") : it.sub();
            float sw = Fonts.width(sub, Font.REGULAR, 0.6f);
            Fonts.drawV(ctx, sub, px + pw - 16 - sw, ry + rowH / 2f, on ? Theme.accentWave(0) : Theme.textFaint(), Font.REGULAR, 0.6f);
            final PItem fit = it;
            hit(px + 6, ry, pw - 12, rowH - 2, b -> {
                if (b == 1 && fit.module() != null) { palette = false; inspect(fit.module()); }
                else runPalette(fit);
            });
            ry += rowH;
        }
        // podpowiedzi
        float hy = py + ph - 16;
        R2D.rect(ctx, px + 10, hy - 4, pw - 20, 0.5f, Theme.stroke());
        String hint = Lang.get("pal.hint");
        Fonts.drawV(ctx, hint, px + 14, hy + 5, Theme.textFaint(), Font.REGULAR, 0.58f);
        String count = paletteItems.size() + "";
        Fonts.drawV(ctx, count, px + pw - 14 - Fonts.width(count, Font.BOLD, 0.58f), hy + 5, Theme.textFaint(), Font.BOLD, 0.58f);
        R2D.alpha = prev;
        paletteDrawing = false;
    }

    // ------------------------------------------------------------------ widgety

    private void drawSwitch(DrawContext ctx, float x, float y, float on) {
        float w = 20, h = 11;
        R2D.roundRect(ctx, x, y, w, h, h / 2f, R2D.blend(Theme.toggleOff(), Theme.accent(), on));
        float kx = x + h / 2f + (w - h) * on;
        R2D.circle(ctx, kx, y + h / 2f, h / 2f - 2, R2D.blend(Theme.textDim(), Theme.accentText(), on));
    }

    private void drawSlider(DrawContext ctx, NumberSetting n, float x, float y, float w) {
        float pct = n.anim.animate((float) n.percent());
        boolean hov = hovered(x - 2, y - 4, w + 4, 10) || dragging == n;
        float ha = anim("slider" + System.identityHashCode(n), 18).animate(hov ? 1 : 0);
        R2D.roundRect(ctx, x, y, w, 3, 1.5f, Theme.toggleOff());
        R2D.roundRect(ctx, x, y, Math.max(3, w * pct), 3, 1.5f, Theme.accent());
        float r = 3.6f + ha * 0.9f;
        R2D.circle(ctx, x + w * pct, y + 1.5f, r + 1, Theme.card());
        R2D.circle(ctx, x + w * pct, y + 1.5f, r, Theme.accent());
        sliders.add(new Object[]{n, x - 4, y - 6, w + 8, 14f, clip});
        hit(x - 4, y - 5, w + 8, 13, b -> {
            if (b != 0) return;
            dragging = n; dragTrackX = x; dragTrackW = w;
        });
    }

    private void button(DrawContext ctx, String key, float x, float y, float w, float h, String icon, String label, boolean primary, Runnable action) {
        boolean hov = hovered(x, y, w, h);
        float ha = anim("btn" + key, 18).animate(hov ? 1 : 0);
        int bg = primary ? R2D.blend(Theme.accent(), Theme.textDim(), ha * 0.25f) : R2D.blend(Theme.card(), Theme.cardHover(), ha);
        int fg = primary ? Theme.accentText() : Theme.text();
        R2D.roundRect(ctx, x, y, w, h, 7, bg);
        if (!primary) R2D.roundOutline(ctx, x, y, w, h, 7, 0.5f, Theme.stroke());
        float contentW = (icon != null ? 9 : 0) + (label != null ? Fonts.width(label, Font.SEMIBOLD, 0.75f) + (icon != null ? 5 : 0) : 0);
        float cx = x + (w - contentW) / 2f;
        if (icon != null) { R2D.icon(ctx, icon, cx, y + h / 2f - 4.5f, 9, fg); cx += 14; }
        if (label != null) Fonts.drawV(ctx, label, cx, y + h / 2f + 0.5f, fg, Font.SEMIBOLD, 0.75f);
        hit(x, y, w, h, b -> { if (b == 0) action.run(); });
    }

    private void segmented(DrawContext ctx, String key, float x, float y, float w, float h, String[] labels, int selected, IntConsumer onSelect) {
        R2D.roundRect(ctx, x, y, w, h, 7, Theme.toggleOff());
        float sw = (w - 4) / labels.length;
        Anim a = anim("seg" + key, 16);
        if (a.raw() == 0 && a.target() == 0) a.snap(selected);
        float pos = a.animate(selected);
        R2D.roundRect(ctx, x + 2 + sw * pos, y + 2, sw, h - 4, 5.5f, Theme.accent());
        for (int i = 0; i < labels.length; i++) {
            float near = 1 - Anim.clamp01(Math.abs(pos - i));
            Fonts.drawCentered(ctx, labels[i], x + 2 + sw * i + sw / 2f, y + h / 2f - Fonts.height(0.72f) / 2f + 0.5f,
                    R2D.blend(Theme.textDim(), Theme.accentText(), near), Font.SEMIBOLD, 0.72f);
            final int fi = i;
            hit(x + 2 + sw * i, y, sw, h, b -> { if (b == 0) onSelect.accept(fi); });
        }
    }

    // ------------------------------------------------------------------ motywy

    private float drawThemes(DrawContext ctx, float x, float y, float w) {
        Theme.Preset[] presets = Theme.Preset.values();
        float gap = 12, cardW = (w - gap * (presets.length - 1)) / presets.length, cardH = 100 * ((cardW - 20) / 120f) + 56;
        for (int i = 0; i < presets.length; i++) {
            Theme.Preset p = presets[i];
            float cx = x + i * (cardW + gap);
            boolean active = Theme.preset() == p;
            boolean hov = hovered(cx, y, cardW, cardH);
            float aa = anim("theme" + i, 14).animate(active ? 1 : 0);
            float ha = anim("themeh" + i, 18).animate(hov ? 1 : 0);
            float lift = ha * 2;
            R2D.roundRect(ctx, cx, y - lift, cardW, cardH, 12, R2D.blend(Theme.card(), Theme.cardHover(), ha * 0.6f));
            R2D.roundOutline(ctx, cx, y - lift, cardW, cardH, 12, 0.6f + aa * 0.8f, R2D.blend(Theme.stroke(), Theme.accent(), aa));

            // mini podgląd GUI w kolorach motywu
            float px0 = cx + 10, py0 = y - lift + 10, pwReal = cardW - 20;
            float ps = pwReal / 120f, ph = 100 * ps;
            ctx.getMatrices().push();
            ctx.getMatrices().translate(px0, py0, 0);
            ctx.getMatrices().scale(ps, ps, 1);
            float px = 0, py = 0, pw = 120;
            {
            float ph0 = 100;
            R2D.roundRect(ctx, px, py, pw, ph0, 8, p.color(0));
            R2D.roundRect(ctx, px, py, pw * 0.3f + 8, ph0, 8, p.color(1));
            R2D.rect(ctx, px + pw * 0.3f, py, 8, ph0, p.color(0));
            R2D.roundRect(ctx, px + 6, py + 6, 8, 8, 2.5f, p.color(8));
            R2D.roundRect(ctx, px + 17, py + 8, pw * 0.3f - 22, 4, 2, p.color(6));
            for (int k = 0; k < 5; k++) {
                int c = k == 1 ? p.color(8) : p.color(3);
                R2D.roundRect(ctx, px + 5, py + 22 + k * 12, pw * 0.3f - 10, 8, 3, c);
            }
            for (int k = 0; k < 4; k++) {
                float bx = px + pw * 0.3f + 8 + (k % 2) * ((pw * 0.7f - 20) / 2f + 4);
                float by = py + 12 + (k / 2) * 44;
                float bw = (pw * 0.7f - 20) / 2f, bh = 38;
                R2D.roundRect(ctx, bx, by, bw, bh, 5, p.color(2));
                R2D.roundRect(ctx, bx + 5, by + 6, 10, 10, 3, k == 0 || k == 3 ? p.color(8) : p.color(10));
                R2D.roundRect(ctx, bx + 19, by + 8, bw * 0.4f, 3, 1.5f, p.color(5));
                R2D.roundRect(ctx, bx + 19, by + 14, bw * 0.3f, 2.5f, 1.25f, p.color(6));
                R2D.roundRect(ctx, bx + bw - 15, by + bh - 10, 10, 5, 2.5f, k == 0 || k == 3 ? p.color(8) : p.color(10));
            }
            R2D.roundOutline(ctx, px, py, pw, 100, 8, 0.5f, p.color(4));
            }
            ctx.getMatrices().pop();

            Fonts.draw(ctx, p.getName(), cx + 12, y - lift + ph + 22, Theme.text(), Font.BOLD, 1f);
            if (aa > 0.02f) {
                float prev = R2D.alpha;
                R2D.alpha = prev * aa;
                String t = Lang.get("theme.active");
                float tw = Fonts.width(t, Font.BOLD, 0.65f) + 18;
                float tx = cx + cardW - 14 - tw, tyb = y - lift + 14;
                R2D.shadow(ctx, tx, tyb, tw, 12, 5, Theme.shadow());
                R2D.roundRect(ctx, tx, tyb, tw, 12, 6, Theme.accent());
                R2D.icon(ctx, "check", tx + 4, tyb + 2.5f, 7, Theme.accentText());
                Fonts.drawV(ctx, t, tx + 13, tyb + 6.5f, Theme.accentText(), Font.BOLD, 0.65f);
                R2D.alpha = prev;
            }
            String desc = switch (p) {
                case DARK -> Lang.pl() ? "Grafit i biel" : "Graphite & white";
                case MIDNIGHT -> Lang.pl() ? "Granat nocy" : "Night navy";
                case LIGHT -> Lang.pl() ? "Biel i czerń" : "White & black";
                case AMOLED -> Lang.pl() ? "Czysta czerń" : "Pure black";
                case CHARCOAL -> Lang.pl() ? "Grafit i fiolet" : "Charcoal & violet";
            };
            Fonts.draw(ctx, desc, cx + 12, y - lift + ph + 35, Theme.textDim(), Font.REGULAR, 0.72f);
            hit(cx, y, cardW, cardH, b -> {
                if (b == 0) { Theme.set(p); rc.config.saveClient(); }
            });
        }
        // kolor akcentu
        float py = y + cardH + 18;
        Fonts.draw(ctx, Lang.get("themes.accent"), x, py, Theme.text(), Font.BOLD, 0.86f);
        Fonts.draw(ctx, Lang.get("themes.accent.desc"), x, py + 11, Theme.textFaint(), Font.REGULAR, 0.66f);
        Theme.Accent[] accents = Theme.Accent.values();
        float ag = 8, aw = (w - ag * (accents.length - 1)) / accents.length, ah = 46, ay = py + 26;
        for (int i = 0; i < accents.length; i++) {
            Theme.Accent ac = accents[i];
            float ax = x + i * (aw + ag);
            boolean act = Theme.accentPreset() == ac;
            boolean hov = hovered(ax, ay, aw, ah);
            float sa = anim("acc" + i, 16).animate(act ? 1 : 0);
            float ha = anim("acch" + i, 18).animate(hov ? 1 : 0);
            R2D.roundRect(ctx, ax, ay - ha * 1.5f, aw, ah, 8, R2D.blend(Theme.card(), Theme.cardHover(), ha));
            R2D.roundOutline(ctx, ax, ay - ha * 1.5f, aw, ah, 8, 0.6f + sa * 0.6f, R2D.blend(Theme.stroke(), Theme.accent(), sa));
            int c1 = ac.first(Theme.preset()), c2 = ac.second(Theme.preset());
            float cx = ax + aw / 2f, cy = ay - ha * 1.5f + 17;
            R2D.circle(ctx, cx, cy, 9, c2);
            R2D.scissor(ctx, cx - 9, cy - 9, 9, 18);
            R2D.circle(ctx, cx, cy, 9, c1);
            R2D.endScissor(ctx);
            if (sa > 0.02f) {
                float prev = R2D.alpha;
                R2D.alpha = prev * sa;
                R2D.icon(ctx, "check", cx - 4, cy - 4, 8, 0.299f * ((c1 >> 16) & 0xFF) + 0.587f * ((c1 >> 8) & 0xFF) + 0.114f * (c1 & 0xFF) > 150 ? 0xFF0A0A0B : 0xFFFFFFFF);
                R2D.alpha = prev;
            }
            Fonts.drawCentered(ctx, ac.getName(), cx, ay - ha * 1.5f + 32, act ? Theme.text() : Theme.textDim(), act ? Font.BOLD : Font.SEMIBOLD, 0.62f);
            hit(ax, ay, aw, ah, bt -> { if (bt == 0) { Theme.setAccent(ac); rc.config.saveClient(); } });
        }
        return cardH + 18 + 26 + ah + 10;
    }

    // ------------------------------------------------------------------ config

    private float drawConfigs(DrawContext ctx, float x, float y, float w) {
        List<String> list = rc.config.list();
        if (list.isEmpty()) {
            R2D.icon(ctx, "folder", x + w / 2f - 12, y + 40, 24, Theme.textFaint());
            Fonts.drawCentered(ctx, Lang.get("config.empty"), x + w / 2f, y + 72, Theme.textDim(), Font.SEMIBOLD, 0.9f);
            return 110;
        }
        float rowH = 34, gap = 6, cy = y;
        for (String name : list) {
            boolean current = name.equals(rc.config.current());
            boolean hov = hovered(x, cy, w, rowH);
            float ha = anim("cfg" + name, 18).animate(hov ? 1 : 0);
            R2D.roundRect(ctx, x, cy, w, rowH, 7, R2D.blend(Theme.card(), Theme.cardHover(), ha));
            R2D.roundOutline(ctx, x, cy, w, rowH, 7, 0.5f, current ? Theme.textFaint() : Theme.stroke());
            R2D.icon(ctx, "disk", x + 10, cy + 10, 8.5f, current ? Theme.accent() : Theme.textFaint());
            Fonts.draw(ctx, name, x + 24, cy + 10, Theme.text(), Font.BOLD, 0.86f);
            java.nio.file.Path file = rc.config.folder().resolve(name + ".json");
            String info;
            try {
                info = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(java.nio.file.Files.getLastModifiedTime(file).toMillis()));
            } catch (Exception e) { info = "-"; }
            Fonts.draw(ctx, info, x + 24, cy + 21, Theme.textFaint(), Font.REGULAR, 0.66f);
            if (current) {
                String t = Lang.get("config.current");
                float tw = Fonts.width(t, Font.BOLD, 0.62f) + 8;
                float tx = x + 24 + Fonts.width(name, Font.BOLD, 0.86f) + 6;
                R2D.roundRect(ctx, tx, cy + 10.5f, tw, 10, 5, Theme.toggleOff());
                Fonts.drawV(ctx, t, tx + 4, cy + 15.5f, Theme.textDim(), Font.BOLD, 0.62f);
            }
            float bx = x + w - 8;
            bx -= 22; button(ctx, "del" + name, bx, cy + 7, 20, 20, "trash", null, false, () -> {
                rc.config.delete(name);
                rc.notifications.info("trash", Lang.get("config.deleted"), name);
            });
            bx -= 24; button(ctx, "save" + name, bx, cy + 7, 20, 20, "disk", null, false, () -> {
                rc.config.save(name);
                rc.notifications.info("disk", Lang.get("config.saved"), name);
            });
            float lw = Fonts.width(Lang.get("config.load"), Font.SEMIBOLD, 0.75f) + 30;
            bx -= lw + 4; button(ctx, "load" + name, bx, cy + 7, lw, 20, "folder", Lang.get("config.load"), true, () -> {
                rc.config.load(name);
                rc.notifications.info("folder", Lang.get("config.loaded"), name);
            });
            cy += rowH + gap;
        }
        return cy - y;
    }

    // ------------------------------------------------------------------ profil

    private float drawProfile(DrawContext ctx, float x, float y, float w) {
        float h = 84;
        R2D.roundRect(ctx, x, y, w, h, 12, Theme.card());
        R2D.roundOutline(ctx, x, y, w, h, 12, 0.6f, Theme.stroke());
        R2D.roundRect(ctx, x + 14, y + 14, 56, 56, 12, Theme.toggleOff());
        drawHead(ctx, x + 18, y + 18, 48);
        String name = dev.vesc.client.module.Legit.name();
        Fonts.draw(ctx, name, x + 84, y + 22, Theme.text(), Font.BOLD, 1.6f);
        Fonts.draw(ctx, "VescClient  •  v" + VescClient.VERSION, x + 84, y + 42, Theme.textDim(), Font.REGULAR, 0.78f);
        float bw = 96;
        button(ctx, "opensettings", x + w - bw - 14, y + h / 2f - 11, bw, 22, "settings", Lang.pl() ? "Ustawienia" : "Settings", true, () -> setPage(Page.SETTINGS));

        // kafelki statystyk
        long sess = (System.currentTimeMillis() - rc.startTime) / 1000;
        String server;
        if (client.isInSingleplayer()) server = Lang.get("profile.singleplayer");
        else {
            ServerInfo si = client.getCurrentServerEntry();
            server = si != null ? si.address : Lang.get("profile.menu");
        }
        String uuid = client.getSession().getUuidOrNull() != null ? client.getSession().getUuidOrNull().toString() : "-";
        String[][] tiles = {
                {"profile", Lang.get("profile.uuid"), uuid},
                {"world", Lang.get("profile.server"), server},
                {"clock", Lang.get("profile.session"), String.format("%02d:%02d:%02d", sess / 3600, (sess / 60) % 60, sess % 60)},
                {"layers", Lang.get("profile.enabled"), rc.modules.enabledCount() + " / " + rc.modules.all().size()},
                {"keyboard", Lang.get("profile.binds"), String.valueOf(rc.modules.all().stream().filter(Module::hasKey).count())},
                {"globe_lang", Lang.get("profile.lang"), Lang.current().display},
        };
        float gap = 8, tw = (w - gap * 2) / 3f, th = 40;
        float ty = y + h + 12;
        for (int i = 0; i < tiles.length; i++) {
            float tx = x + (i % 3) * (tw + gap), tyy = ty + (i / 3) * (th + gap);
            boolean hov = hovered(tx, tyy, tw, th);
            float ha = anim("tile" + i, 18).animate(hov ? 1 : 0);
            R2D.roundRect(ctx, tx, tyy, tw, th, 7, R2D.blend(Theme.card(), Theme.cardHover(), ha));
            R2D.roundOutline(ctx, tx, tyy, tw, th, 7, 0.5f, Theme.stroke());
            R2D.icon(ctx, tiles[i][0], tx + 10, tyy + 9, 8, Theme.textDim());
            Fonts.draw(ctx, tiles[i][1], tx + 22, tyy + 9.5f, Theme.textFaint(), Font.SEMIBOLD, 0.66f);
            Fonts.draw(ctx, Fonts.trim(tiles[i][2], Font.BOLD, 0.84f, tw - 20), tx + 10, tyy + 23, Theme.text(), Font.BOLD, 0.84f);
        }
        float hy = ty + (th + gap) * 2 + 2;
        R2D.icon(ctx, "info", x, hy, 9, Theme.textFaint());
        Fonts.draw(ctx, Lang.get("profile.hint"), x + 14, hy + 1, Theme.textFaint(), Font.REGULAR, 0.72f);
        return hy - y + 16;
    }

    /** krótki opis ustawień modułu (wartości trybów i suwaków) */
    private static String summary(Module m) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (Setting<?> s : m.getSettings()) {
            String v = null;
            if (s instanceof ModeSetting md) v = md.get();
            else if (s instanceof NumberSetting num) v = num.display();
            if (v == null) continue;
            if (sb.length() > 0) sb.append("  ");
            sb.append(v);
            if (++n >= 2) break;
        }
        return sb.toString();
    }

    private void drawFooter(DrawContext ctx) {
        boolean pl = Lang.pl();
        String[][] items = {{pl ? "LPM" : "LMB", "footer.toggle"}, {pl ? "PPM / >" : "RMB / >", "footer.settings"}, {pl ? "ŚPM" : "MMB", "footer.bind"},
                {Lang.pl() ? "Kółko" : "Wheel", "footer.wheel"}, {"RShift / Esc", "footer.close"}};
        float s = 0.62f, gap = 12, total = 0;
        for (String[] it : items) total += Fonts.width(it[0], Font.BOLD, s) + 4 + Fonts.width(Lang.get(it[1]), Font.REGULAR, s) + gap;
        total -= gap;
        float fx = x0 + W / 2f - total / 2f - 10, fy = y0 + H + 8;
        R2D.roundRect(ctx, fx, fy, total + 20, 15, 7.5f, R2D.withAlpha(Theme.bg(), 0.85f));
        R2D.roundOutline(ctx, fx, fy, total + 20, 15, 7.5f, 0.5f, Theme.stroke());
        float cx = fx + 10;
        for (String[] it : items) {
            Fonts.drawV(ctx, it[0], cx, fy + 8, Theme.text(), Font.BOLD, s);
            cx += Fonts.width(it[0], Font.BOLD, s) + 4;
            String d = Lang.get(it[1]);
            Fonts.drawV(ctx, d, cx, fy + 8, Theme.textFaint(), Font.REGULAR, s);
            cx += Fonts.width(d, Font.REGULAR, s) + gap;
        }
    }

    // ------------------------------------------------------------------ znajomi

    private void addFriend() {
        String n = friendInput.trim();
        if (dev.vesc.client.config.Friends.validName(n) && rc.friends.add(n)) {
            rc.notifications.info("users", Lang.get("friends.added"), n);
            rc.config.saveClient();
        }
        friendInput = "";
    }

    private float drawFriends(DrawContext ctx, float x, float y, float w) {
        // pole dodawania
        float h = 24, bw = 90;
        boolean hov = hovered(x, y, w - bw - 8, h);
        float fa = anim("friendfocus", 16).animate(friendFocused ? 1 : 0);
        R2D.roundRect(ctx, x, y, w - bw - 8, h, h / 2f, R2D.blend(hov ? Theme.cardHover() : Theme.card(), R2D.withAlpha(Theme.accent(), 0.18f), fa));
        R2D.roundOutline(ctx, x, y, w - bw - 8, h, h / 2f, 0.6f, R2D.blend(Theme.stroke(), Theme.accent(), fa));
        R2D.icon(ctx, "users", x + 10, y + h / 2f - 4.5f, 9, Theme.textFaint());
        String shown = friendInput.isEmpty() ? Lang.get("friends.placeholder") : friendInput;
        Fonts.drawV(ctx, shown, x + 25, y + h / 2f + 0.5f, friendInput.isEmpty() ? Theme.textFaint() : Theme.text(), Font.REGULAR, 0.78f);
        if (friendFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            float cxp = x + 25 + (friendInput.isEmpty() ? 0 : Fonts.width(friendInput, Font.REGULAR, 0.78f) + 1);
            R2D.rect(ctx, cxp, y + 7, 0.7f, h - 14, Theme.text());
        }
        hit(x, y, w - bw - 8, h, b -> { friendFocused = true; searchFocused = false; });
        button(ctx, "addfriend", x + w - bw, y, bw, h, "plus", Lang.get("friends.add"), true, this::addFriend);

        float cy = y + h + 12;
        java.util.List<String> list = new java.util.ArrayList<>(rc.friends.all());
        if (list.isEmpty()) {
            R2D.icon(ctx, "users", x + w / 2f - 12, cy + 30, 24, Theme.textFaint());
            Fonts.drawCentered(ctx, Lang.get("friends.empty"), x + w / 2f, cy + 62, Theme.textDim(), Font.SEMIBOLD, 0.85f);
            return cy + 90 - y;
        }
        float gap = 8, colW = (w - gap) / 2f, rh = 30;
        for (int i = 0; i < list.size(); i++) {
            String name = list.get(i);
            float fx = x + (i % 2) * (colW + gap), fy = cy + (i / 2) * (rh + gap);
            boolean hv = hovered(fx, fy, colW, rh);
            float ha = anim("fr" + name, 18).animate(hv ? 1 : 0);
            R2D.roundRect(ctx, fx, fy, colW, rh, 8, R2D.blend(Theme.card(), Theme.cardHover(), ha));
            R2D.roundOutline(ctx, fx, fy, colW, rh, 8, 0.5f, Theme.stroke());
            R2D.circle(ctx, fx + 15, fy + rh / 2f, 7, Theme.toggleOff());
            Fonts.drawCentered(ctx, name.substring(0, 1).toUpperCase(), fx + 15, fy + rh / 2f - Fonts.height(0.7f) / 2f + 0.5f, Theme.accentWave(i * 0.4f), Font.BOLD, 0.7f);
            Fonts.drawV(ctx, name, fx + 28, fy + rh / 2f + 0.5f, Theme.text(), Font.BOLD, 0.82f);
            float bx = fx + colW - 24;
            boolean hb = hovered(bx, fy + 7, 16, 16);
            R2D.roundRect(ctx, bx, fy + 7, 16, 16, 8, R2D.withAlpha(Theme.cardHover(), hb ? 1 : ha));
            R2D.icon(ctx, "trash", bx + 4, fy + 11, 8, hb ? 0xFFFF5A5A : Theme.textFaint());
            hit(bx, fy + 7, 16, 16, b -> { if (b == 0) { rc.friends.remove(name); rc.config.saveClient(); } });
        }
        return cy + ((list.size() + 1) / 2) * (rh + gap) - y;
    }

    // ------------------------------------------------------------------ HUD

    private float drawHudPage(DrawContext ctx, float x, float y, float w) {
        // styl HUD
        {
            float sh = 26;
            R2D.roundRect(ctx, x, y, w, sh, 8, Theme.card());
            R2D.roundOutline(ctx, x, y, w, sh, 8, 0.5f, Theme.stroke());
            R2D.icon(ctx, "sparkles", x + 9, y + 9, 8, Theme.accentWave(0));
            Fonts.drawV(ctx, Lang.get("hudstyle.title"), x + 22, y + sh / 2f + 0.5f, Theme.text(), Font.BOLD, 0.8f);
            Fonts.drawV(ctx, Lang.get("hudstyle.desc"), x + 22 + Fonts.width(Lang.get("hudstyle.title"), Font.BOLD, 0.8f) + 8, y + sh / 2f + 0.5f, Theme.textFaint(), Font.REGULAR, 0.62f);
            var hs = rc.settings.hudStyle;
            segmented(ctx, "hudstyle", x + w - 170, y + 4, 164, 18, new String[]{"Glass", "Neon", "Minimal"}, hs.modes.indexOf(hs.get()), i -> {
                hs.set(hs.modes.get(i)); rc.config.saveClient();
            });
            y += sh + 8;
        }
        float yStart0 = y;
        // karta edytora
        float eh = 52;
        boolean hov = hovered(x, y, w, eh);
        float ha = anim("hudeditor", 18).animate(hov ? 1 : 0);
        R2D.roundRect(ctx, x, y, w, eh, 9, R2D.blend(Theme.card(), Theme.cardHover(), ha));
        R2D.roundOutline(ctx, x, y, w, eh, 9, 0.6f, R2D.blend(Theme.stroke(), Theme.accent(), ha * 0.6f));
        // mini podgląd ekranu
        float px = x + 10, pyy = y + 9, pw = 56, ph = 34;
        R2D.roundRect(ctx, px, pyy, pw, ph, 5, Theme.toggleOff());
        R2D.roundRect(ctx, px + 4, pyy + 4, 16, 10, 2, Theme.accentWave(0));
        R2D.roundRect(ctx, px + 4, pyy + 16, 12, 7, 2, Theme.textFaint());
        R2D.roundRect(ctx, px + pw - 22, pyy + 4, 18, 4, 1.5f, Theme.accentWave(1));
        R2D.roundRect(ctx, px + pw - 18, pyy + ph - 10, 14, 6, 2, Theme.textFaint());
        float t = (System.currentTimeMillis() % 3000) / 3000f;
        float mxp = px + 20 + (float) Math.sin(t * Math.PI * 2) * 10, myp = pyy + 18 + (float) Math.cos(t * Math.PI * 2) * 5;
        R2D.icon(ctx, "move", mxp, myp, 7, Theme.text());
        Fonts.draw(ctx, Lang.get("hud.editor"), x + 76, y + 12, Theme.text(), Font.BOLD, 0.95f);
        Fonts.draw(ctx, Fonts.trim(Lang.get("hud.editor.desc"), Font.REGULAR, 0.66f, w - 76 - 120), x + 76, y + 25, Theme.textFaint(), Font.REGULAR, 0.66f);
        float bw = 100;
        button(ctx, "openeditor", x + w - bw - 10, y + eh / 2f - 10, bw, 20, "move", Lang.get("hud.editor.open"), true,
                () -> { if (client != null) client.setScreen(new HudEditor()); });
        hit(x, y, w - bw - 20, eh, b -> { if (b == 0 && client != null) client.setScreen(new HudEditor()); });

        float cy = y + eh + 10, gap = 6;
        // presety układu
        Fonts.draw(ctx, Lang.get("hud.presets"), x, cy + 1, Theme.textFaint(), Font.SEMIBOLD, 0.62f);
        cy += 11;
        String[] presets = {"classic", "minimal", "pvp", "full"};
        String[] picons = {"layers", "check", "target", "sparkles"};
        float pg = 6, presW = (w - pg * 3) / 4f;
        for (int i = 0; i < 4; i++) {
            final String pr = presets[i];
            button(ctx, "preset" + i, x + i * (presW + pg), cy, presW, 20, picons[i], Lang.get("preset." + pr), false, () -> {
                rc.hud.applyPreset(pr);
                rc.config.saveClient();
                rc.notifications.info("hud", Lang.get("hud.presets"), Lang.get("preset." + pr));
            });
        }
        cy += 28;
        float colW = (w - gap) / 2f;
        int i = 0;
        for (dev.vesc.client.hud.HudElement e : rc.hud.elements) {
            float ex = x + (i % 2) * (colW + gap), ey = cy + (i / 2) * (34 + gap);
            settingRowAt(ctx, ex, ey, colW, e.icon, e.getName(), e.getDesc(), 90);
            drawSwitch(ctx, ex + colW - 30, ey + 17 - 5.5f, e.enabled.anim.animate(e.enabled.get() ? 1 : 0));
            hit(ex, ey, colW, 34, b -> { if (b == 0) { e.enabled.toggle(); rc.config.saveClient(); } });
            i++;
        }
        float by = cy + ((rc.hud.elements.size() + 1) / 2) * (34 + gap) + 2;
        button(ctx, "hudreset", x, by, 130, 20, "reset", Lang.get("hud.reset"), false, () -> {
            for (dev.vesc.client.hud.HudElement e : rc.hud.elements) e.resetPosition();
            rc.config.saveClient();
        });
        return by + 24 - y + 34;
    }

    private void settingRowAt(DrawContext ctx, float x, float y, float w, String icon, String title, String desc, float rightSpace) {
        float h = 34;
        boolean hov = hovered(x, y, w, h);
        float ha = anim("rowat" + title, 18).animate(hov ? 1 : 0);
        R2D.roundRect(ctx, x, y, w, h, 7, R2D.blend(Theme.card(), Theme.cardHover(), ha));
        R2D.roundOutline(ctx, x, y, w, h, 7, 0.5f, Theme.stroke());
        R2D.icon(ctx, icon, x + 10, y + 8.5f, 8.5f, Theme.accentWave(0));
        Fonts.draw(ctx, title, x + 23, y + 8, Theme.text(), Font.BOLD, 0.86f);
        Fonts.draw(ctx, Fonts.trim(desc, Font.REGULAR, 0.62f, w - 23 - 40), x + 23, y + 19, Theme.textFaint(), Font.REGULAR, 0.62f);
    }

    // ------------------------------------------------------------------ ustawienia klienta

    private float settingRow(DrawContext ctx, float x, float y, float w, String icon, String title, String desc) {
        float h = 34;
        boolean hov = hovered(x, y, w, h);
        float ha = anim("row" + title, 18).animate(hov ? 1 : 0);
        R2D.roundRect(ctx, x, y, w, h, 7, R2D.blend(Theme.card(), Theme.cardHover(), ha));
        R2D.roundOutline(ctx, x, y, w, h, 7, 0.5f, Theme.stroke());
        R2D.icon(ctx, icon, x + 10, y + 8.5f, 8.5f, Theme.text());
        Fonts.draw(ctx, title, x + 23, y + 8, Theme.text(), Font.BOLD, 0.86f);
        Fonts.draw(ctx, Fonts.trim(desc, Font.REGULAR, 0.66f, w - 23 - 170), x + 23, y + 19, Theme.textFaint(), Font.REGULAR, 0.66f);
        return h;
    }

    private float drawSettings(DrawContext ctx, float x, float y, float w) {
        ClientSettings s = rc.settings;
        float cy = y, gap = 6, h;

        h = settingRow(ctx, x, cy, w, "globe_lang", Lang.get("set.language"), Lang.get("set.language.desc"));
        segmented(ctx, "lang", x + w - 150, cy + 8, 140, 18, new String[]{"Polski", "English"}, Lang.current().ordinal(), i -> {
            Lang.set(Lang.Language.values()[i]); rc.config.saveClient();
        });
        cy += h + gap;

        h = settingRow(ctx, x, cy, w, "layers", Lang.get("layout.title"), Lang.get("layout.desc"));
        segmented(ctx, "layout", x + w - 170, cy + 8, 160, 18, new String[]{"Window", "3D", "Panels"}, s.layout.modes.indexOf(s.layout.get()), i -> {
            s.layout.set(s.layout.modes.get(i)); rc.config.saveClient();
        });
        cy += h + gap;

        h = settingRow(ctx, x, cy, w, "layers", Lang.get("set.guisize"), Lang.get("set.guisize.desc"));
        String[] sizes = {"size.small", "size.normal", "size.large"};
        int sel = Arrays.asList(sizes).indexOf(s.guiSize.get());
        segmented(ctx, "size", x + w - 150, cy + 8, 140, 18, new String[]{Lang.get("size.small"), Lang.get("size.normal"), Lang.get("size.large")}, sel, i -> {
            s.guiSize.set(sizes[i]); rc.config.saveClient();
        });
        cy += h + gap;

        h = settingRow(ctx, x, cy, w, "bolt", Lang.get("set.anim"), Lang.get("set.anim.desc"));
        String v = s.animSpeed.display() + "x";
        Fonts.drawV(ctx, v, x + w - 10 - Fonts.width(v, Font.BOLD, 0.75f), cy + h / 2f + 0.5f, Theme.text(), Font.BOLD, 0.75f);
        drawSlider(ctx, s.animSpeed, x + w - 160, cy + h / 2f - 1.5f, 116);
        cy += h + gap;

        Object[][] toggles = {
                {"render", "set.blur", s.blur},
                {"sparkles", "set.particles", s.particles},
                {"sound", "set.sounds", s.sounds},
                {"star", "set.welcome", s.welcome},
                {"hud", "set.menu", s.customMenu},
        };
        for (Object[] t : toggles) {
            BoolSetting b = (BoolSetting) t[2];
            h = settingRow(ctx, x, cy, w, (String) t[0], Lang.get((String) t[1]), Lang.get(t[1] + ".desc"));
            drawSwitch(ctx, x + w - 30, cy + h / 2f - 5.5f, b.anim.animate(b.get() ? 1 : 0));
            float fy = cy;
            hit(x, fy, w, h, bt -> { if (bt == 0) { b.toggle(); rc.config.saveClient(); } });
            cy += h + gap;
        }

        button(ctx, "reset", x, cy + 2, 140, 22, "power", Lang.get("set.reset"), false, () -> {
            s.reset(); Anim.globalSpeed = 1f; rc.config.saveClient();
        });
        return cy + 26 - y;
    }

    // ================================================================== pomocnicze

    private Anim anim(String key, float speed) { return anims.computeIfAbsent(key, k -> new Anim(0, speed)); }

    private void setPage(Page p) {
        if (page == p && p != Page.CATEGORY) return;
        page = p;
        pageTime = System.currentTimeMillis();
        listTime = pageTime;
        inspected = null;
        focusIndex = -1;
        scrollTarget = 0;
        scroll.snap(0);
        binding = null;
        searchFocused = false;
    }

    private static float[] intersect(float[] a, float[] b) {
        if (a == null) return b;
        float x = Math.max(a[0], b[0]), y = Math.max(a[1], b[1]);
        float x2 = Math.min(a[0] + a[2], b[0] + b[2]), y2 = Math.min(a[1] + a[3], b[1] + b[3]);
        return new float[]{x, y, Math.max(0, x2 - x), Math.max(0, y2 - y)};
    }

    private static boolean inside(float px, float py, float x, float y, float w, float h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    private boolean hovered(float x, float y, float w, float h) {
        if (closing || draggingWindow || resizing) return false;
        if (palette && !paletteDrawing) return false;
        if (clip != null && !inside(mx, my, clip[0], clip[1], clip[2], clip[3])) return false;
        return inside(mx, my, x, y, w, h);
    }

    private void hit(float x, float y, float w, float h, IntConsumer action) {
        hits.add(new Hit(x, y, w, h, clip, action));
    }

    // ================================================================== wejście

    private void updateMouse(double mouseX, double mouseY) {
        float[] fm = flatMouse((float) mouseX, (float) mouseY);
        mx = (fm[0] - centerX) / scale + centerX;
        my = (fm[1] - centerY) / scale + centerY;
    }

    // ================================================================== spatial (GUI 3D)

    private static net.minecraft.client.gl.SimpleFramebuffer spatialFbo;
    private org.joml.Matrix4f spatialM;
    private boolean changedPerspective;
    private static final Anim camShiftAnim = new Anim(0, 7);

    /** GUI przestrzenne: panel 3D w perspektywie, kamera z trzeciej osoby (tylko w świecie) */
    private boolean spatial() {
        return rc.settings.layout.get().equals("Spatial") && client != null && client.world != null && client.player != null;
    }

    @Override
    protected void init() {
        super.init();
        if (spatial() && !changedPerspective && client.options.getPerspective().isFirstPerson()) {
            client.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_BACK);
            changedPerspective = true;
        }
    }

    @Override
    public void removed() {
        if (changedPerspective && client != null && client.options.getPerspective() == net.minecraft.client.option.Perspective.THIRD_PERSON_BACK)
            client.options.setPerspective(net.minecraft.client.option.Perspective.FIRST_PERSON);
        changedPerspective = false;
        super.removed();
    }

    /** przesunięcie kamery w bok (0..1) – postać po lewej, panel po prawej; wywoływane co klatkę z Camera */
    public static float camShift() {
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        boolean on = mc.currentScreen instanceof ClickGui g && g.spatial() && !g.closing;
        return Anim.easeOutCubic(camShiftAnim.animate(on ? 1 : 0));
    }

    /** macierz: płaski obraz GUI -> ekran (obrót 3D + perspektywa) */
    private org.joml.Matrix4f buildSpatial(float p) {
        float ang = (float) Math.toRadians(13 + 14 * (1 - p)), tilt = (float) Math.toRadians(3.5f);
        org.joml.Matrix4f persp = new org.joml.Matrix4f().m23(-1f / 900f);
        return new org.joml.Matrix4f()
                .translate(width * 0.5f + width * 0.16f, height * 0.5f + height * 0.01f, 0)
                .mul(persp)
                .rotateY(-ang).rotateX(tilt)
                .scale(0.78f)
                .translate(-width * 0.5f, -height * 0.5f, 0);
    }

    /** pozycja myszy w płaskim obrazie GUI (odwrotność homografii panelu 3D) */
    private float[] flatMouse(float sx, float sy) {
        if (spatialM == null || !spatial()) return new float[]{sx, sy};
        org.joml.Matrix4f c = spatialM;
        org.joml.Matrix3f hm = new org.joml.Matrix3f(c.m00(), c.m01(), c.m03(), c.m10(), c.m11(), c.m13(), c.m30(), c.m31(), c.m33()).invert();
        org.joml.Vector3f v = hm.transform(new org.joml.Vector3f(sx, sy, 1));
        return new float[]{v.x / v.z, v.y / v.z};
    }

    private void beginSpatial(DrawContext ctx) {
        ctx.draw();
        var win = client.getWindow();
        int fw = win.getFramebufferWidth(), fh = win.getFramebufferHeight();
        if (spatialFbo == null) {
            spatialFbo = new net.minecraft.client.gl.SimpleFramebuffer(fw, fh, true, net.minecraft.client.MinecraftClient.IS_SYSTEM_MAC);
            spatialFbo.setTexFilter(org.lwjgl.opengl.GL11.GL_LINEAR);
        } else if (spatialFbo.textureWidth != fw || spatialFbo.textureHeight != fh) {
            spatialFbo.resize(fw, fh, net.minecraft.client.MinecraftClient.IS_SYSTEM_MAC);
            spatialFbo.setTexFilter(org.lwjgl.opengl.GL11.GL_LINEAR);
        }
        spatialFbo.setClearColor(0, 0, 0, 0);
        spatialFbo.clear(net.minecraft.client.MinecraftClient.IS_SYSTEM_MAC);
        spatialFbo.beginWrite(true);
    }

    private void endSpatial(DrawContext ctx) {
        ctx.draw();
        client.getFramebuffer().beginWrite(true);
        // obraz GUI (z przezroczystością premultiplied) jako panel 3D
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(com.mojang.blaze3d.platform.GlStateManager.SrcFactor.ONE, com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.SrcFactor.ONE, com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(net.minecraft.client.render.GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, spatialFbo.getColorAttachment());
        RenderSystem.setShaderColor(1, 1, 1, 1);
        org.joml.Matrix4f m = new org.joml.Matrix4f(ctx.getMatrices().peek().getPositionMatrix()).mul(spatialM);
        var b = net.minecraft.client.render.Tessellator.getInstance().begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_TEXTURE);
        b.vertex(m, 0, 0, 0).texture(0, 1);
        b.vertex(m, 0, height, 0).texture(0, 0);
        b.vertex(m, width, height, 0).texture(1, 0);
        b.vertex(m, width, 0, 0).texture(1, 1);
        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(b.end());
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return true;
        updateMouse(mouseX, mouseY);
        boolean wasFocused = searchFocused;
        searchFocused = false;
        friendFocused = false;
        if (editing != null) commitEdit();
        if (binding != null && button > 1) { // przypisanie przycisku myszy nie jest wspierane – anuluj
            binding = null;
            return true;
        }
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.clip != null && !inside(mx, my, h.clip[0], h.clip[1], h.clip[2], h.clip[3])) continue;
            if (inside(mx, my, h.x, h.y, h.w, h.h)) {
                Module before = binding;
                h.action.accept(button);
                if (rc.settings.sounds.get() && client != null)
                    client.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(
                            net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(), button == 1 ? 1.4f : 1.8f, 0.18f));
                if (before != null && binding == before && !wasFocused) { /* klik w ten sam przycisk */ }
                return true;
            }
        }
        binding = null;
        // klik poza oknem zamyka GUI
        if (!panelMode() && !inside(mx, my, x0, y0, W, H)) close();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null || resizing || draggingColor != null || draggingPanel != null) { rc.config.saveClient(); }
        draggingPanel = null;
        resizing = false;
        draggingColor = null;
        dragging = null;
        draggingWindow = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        updateMouse(mouseX, mouseY);
        if (palette) {
            paletteSel = Math.max(0, Math.min(paletteItems.size() - 1, paletteSel - (int) Math.signum(verticalAmount)));
            return true;
        }
        for (Object[] sl : sliders) {
            float[] cl = (float[]) sl[5];
            if (cl != null && !inside(mx, my, cl[0], cl[1], cl[2], cl[3])) continue;
            if (inside(mx, my, (float) sl[1], (float) sl[2], (float) sl[3], (float) sl[4])) {
                NumberSetting n = (NumberSetting) sl[0];
                n.set(n.get() + n.step * Math.signum(verticalAmount));
                if (n == rc.settings.animSpeed) Anim.globalSpeed = n.get().floatValue();
                return true;
            }
        }
        if (panelMode()) {
            for (var e : panelBody.entrySet()) {
                float[] r = e.getValue();
                if (inside(mx, my, r[0], r[1], r[2], r[3])) {
                    float[] t = panelScrollT.computeIfAbsent(e.getKey(), k -> new float[2]);
                    t[0] = (float) Math.max(0, Math.min(t[1], t[0] - verticalAmount * 24));
                    return true;
                }
            }
            return true;
        }
        if (inspW > 0 && inside(mx, my, inspX, inspY, inspW, inspH)) {
            inspScrollTarget = (float) Math.max(0, Math.min(inspMax, inspScrollTarget - verticalAmount * 28));
            return true;
        }
        scrollTarget = (float) Math.max(0, Math.min(maxScroll, scrollTarget - verticalAmount * 32));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (closing) return true;
        if (palette) { paletteKey(keyCode); return true; }
        if (editing != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) commitEdit();
            else if (keyCode == GLFW.GLFW_KEY_ESCAPE) editing = null;
            else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !editBuffer.isEmpty()) editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
            return true;
        }
        if ((keyCode == GLFW.GLFW_KEY_K || keyCode == GLFW.GLFW_KEY_P) && Screen.hasControlDown() && binding == null) { openPalette(); return true; }
        if (binding != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE)
                binding.setKey(GLFW.GLFW_KEY_UNKNOWN);
            else binding.setKey(keyCode);
            binding = null;
            return true;
        }
        if (friendFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !friendInput.isEmpty()) friendInput = friendInput.substring(0, friendInput.length() - 1);
            else if (keyCode == GLFW.GLFW_KEY_ENTER) addFriend();
            else if (keyCode == GLFW.GLFW_KEY_ESCAPE) friendFocused = false;
            return true;
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = Screen.hasControlDown() ? "" : search.substring(0, search.length() - 1);
                scrollTarget = 0;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) { searchFocused = false; return true; }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && inspected != null) { inspected = null; return true; }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        // nawigacja strzałkami po modułach
        if (!panelMode() && (page == Page.CATEGORY || page == Page.FAVORITES) && !shownModules.isEmpty()) {
            int n = shownModules.size();
            int f = focusIndex;
            switch (keyCode) {
                case GLFW.GLFW_KEY_RIGHT -> f = f < 0 ? 0 : Math.min(n - 1, f + 1);
                case GLFW.GLFW_KEY_LEFT -> f = f < 0 ? 0 : Math.max(0, f - 1);
                case GLFW.GLFW_KEY_DOWN -> f = f < 0 ? 0 : Math.min(n - 1, f + lastCols);
                case GLFW.GLFW_KEY_UP -> f = f < 0 ? 0 : Math.max(0, f - lastCols);
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { if (f >= 0) shownModules.get(f).toggle(); return true; }
                case GLFW.GLFW_KEY_SPACE -> { if (f >= 0) inspect(shownModules.get(f)); return true; }
                default -> f = Integer.MIN_VALUE;
            }
            if (f != Integer.MIN_VALUE) {
                focusIndex = f;
                // przewiń, aby fokus był widoczny
                float rowH = (listView ? 35 : 42);
                float rowY = (f / lastCols) * rowH;
                float vh = H - 51 - 22 - 6;
                if (rowY < scrollTarget) scrollTarget = rowY;
                else if (rowY + rowH > scrollTarget + vh - 10) scrollTarget = rowY + rowH - vh + 10;
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_F && Screen.hasControlDown()) {
            if (page != Page.CATEGORY && page != Page.FAVORITES) setPage(Page.CATEGORY);
            searchFocused = true; return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            Category[] cs = Category.values();
            int dir = Screen.hasShiftDown() ? -1 : 1;
            category = page == Page.CATEGORY ? cs[Math.floorMod(category.ordinal() + dir, cs.length)] : category;
            search = "";
            setPage(Page.CATEGORY);
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (closing || binding != null) return true;
        if (palette) {
            if (chr >= ' ' && chr != 127 && paletteQuery.length() < 32) { paletteQuery += chr; paletteSel = 0; }
            return true;
        }
        if (editing != null) {
            if ((Character.isDigit(chr) || chr == '.' || chr == '-' || chr == ',') && editBuffer.length() < 10) editBuffer += chr == ',' ? '.' : chr;
            return true;
        }
        if (friendFocused) {
            if ((Character.isLetterOrDigit(chr) || chr == '_') && friendInput.length() < 16) friendInput += chr;
            return true;
        }
        if ((panelMode() || page == Page.CATEGORY || page == Page.FAVORITES) && chr >= ' ' && chr != 127) {
            if (!searchFocused && Character.isLetterOrDigit(chr)) searchFocused = true;
            if (searchFocused && search.length() < 24) { search += chr; scrollTarget = 0; }
        }
        return true;
    }

    @Override
    public void close() {
        if (closing) return;
        closing = true;
        closeTime = System.currentTimeMillis();
        binding = null;
        dragging = null;
    }

    private void finishClose() {
        rc.config.save(rc.config.current());
        if (client != null) client.setScreen(null);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}
}
