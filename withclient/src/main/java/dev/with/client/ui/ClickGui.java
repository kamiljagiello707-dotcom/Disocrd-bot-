package dev.with.client.ui;

import dev.with.client.WithClient;
import dev.with.client.module.Category;
import dev.with.client.module.Module;
import dev.with.client.module.Modules;
import dev.with.client.module.Setting;
import dev.with.client.render.Fonts;
import dev.with.client.render.Fonts.Font;
import dev.with.client.render.R2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static dev.with.client.ui.Ui.*;

/**
 * ClickGUI WithClient 1.1 – kompaktowe, biało-czarne okno:
 * nawigacja (logo, 10 kategorii, strony klienta, profil) | nagłówek (tytuł, wyszukiwarka, filtr) |
 * siatka modułów, która przy wyborze modułu płynnie przelewa się w jedną kolumnę, a z prawej
 * wysuwa się panel ustawień (grupy zwijane, suwaki, zakresy, listy, wielokrotny wybór, kolory, tekst).
 * Efekty: cząsteczki w tle, animowana ramka, powiadomienia, podpowiedzi, przejścia stron.
 * Moduły to atrapy z opcjami – bez działania w grze.
 */
public class ClickGui extends Screen {
    private enum Page { MODULES, APPEARANCE, CONFIGS, FRIENDS }

    private static Page page = Page.MODULES;
    private static Category category = Category.COMBAT;
    private static Module selected;
    private static int filter; // 0 wszystkie, 1 włączone, 2 ulubione

    private static final float NAV = 132, PW = 212;

    private final long openTime = System.currentTimeMillis();
    private long closeTime = -1, listTime = System.currentTimeMillis(), pageTime = System.currentTimeMillis(), setTime;
    private final Map<String, Anim> anims = new HashMap<>();
    private record Hit(float x, float y, float w, float h, IntConsumer action) {
        boolean in(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; }
    }
    private final List<Hit> hits = new ArrayList<>();
    private final Deque<float[]> clips = new ArrayDeque<>();
    private int mx, my;
    private String query = "", friendInput = "";
    private int focus; // 0 nic, 1 szukaj, 2 znajomy, 3 pole tekstowe ustawienia
    private Setting.Text editText;
    private Module binding;
    private Object dragging;
    private Runnable dragApply;
    private float listScroll, listScrollT, listMax, setScroll, setScrollT, setMax, appScroll, appScrollT, appMax;
    private float x0, y0, W, H, sc = 1, cx0, cx1;
    private Module shown; // moduł w panelu (zostaje podczas chowania)
    private final Anim panel = new Anim(selected != null ? 1 : 0, 13).springy();
    private final Anim catAnim = new Anim(-1, 17).springy();
    private final Map<String, Float> groupH = new HashMap<>();
    // lista rozwijana
    private Setting.Mode drop;
    private String dropKey;
    private float dropX, dropY, dropW;
    private final Anim dropAnim = new Anim(0, 18);
    // color picker
    private Setting.Color picking;
    private float[] pickHSV = new float[3];
    private int pickDrag;
    private float pickAlpha = 1, pickAnchorX, pickAnchorY, pickX, pickY;
    private static final float PICK_SQ = 96;
    // podpowiedź
    private Module tipModule;
    private long tipStart;
    // powiadomienia
    private record Toast(String icon, String title, boolean on, long time) {}
    private static final List<Toast> TOASTS = new ArrayList<>();
    // cząsteczki (wspólne między otwarciami)
    private static final int PN = 38;
    private static final float[] PX = new float[PN], PY = new float[PN], VX = new float[PN], VY = new float[PN];
    private static boolean pInit;
    private long lastFrame = System.nanoTime();

    public ClickGui() {
        super(Text.literal("WithClient"));
        shown = selected;
        if (!pInit) {
            Random r = new Random();
            for (int i = 0; i < PN; i++) { PX[i] = r.nextFloat(); PY[i] = r.nextFloat(); VX[i] = (r.nextFloat() - 0.5f) * 0.02f; VY[i] = (r.nextFloat() - 0.5f) * 0.02f; }
            pInit = true;
        }
    }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }

    // ================================================================== pomocnicze

    private Anim anim(String k) { return anims.computeIfAbsent(k, s -> new Anim(0, 16)); }
    private float hover(String k, float x, float y, float w, float h) { return anim(k).animate(over(x, y, w, h) ? 1 : 0); }
    private float state(String k, float sp, float tgt) { return anims.computeIfAbsent(k, s -> new Anim(tgt, sp)).animate(tgt); }
    private float pos(String k, float tgt) { return anims.computeIfAbsent(k, s -> new Anim(tgt, 18).springy()).animate(tgt); }

    private boolean over(float x, float y, float w, float h) {
        if (!clips.isEmpty()) { float[] c = clips.peek(); if (mx < c[0] || my < c[1] || mx >= c[2] || my >= c[3]) return false; }
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void hit(float x, float y, float w, float h, IntConsumer a) {
        float x1 = x, y1 = y, x2 = x + w, y2 = y + h;
        if (!clips.isEmpty()) { float[] c = clips.peek(); x1 = Math.max(x1, c[0]); y1 = Math.max(y1, c[1]); x2 = Math.min(x2, c[2]); y2 = Math.min(y2, c[3]); }
        if (x2 > x1 && y2 > y1) hits.add(new Hit(x1, y1, x2 - x1, y2 - y1, a));
    }

    private void clip(DrawContext ctx, float x, float y, float w, float h) {
        h = Math.max(0, h);
        R2D.scissor(ctx, x, y, w, h);
        float[] c = {x, y, x + w, y + h};
        if (!clips.isEmpty()) { float[] p = clips.peek(); c = new float[]{Math.max(c[0], p[0]), Math.max(c[1], p[1]), Math.min(c[2], p[2]), Math.min(c[3], p[3])}; }
        clips.push(c);
    }
    private void unclip(DrawContext ctx) { R2D.endScissor(ctx); clips.pop(); }

    /** tło karty/wiersza: pełne (cząsteczki nie prześwitują pod tekstem), w stylu Szkło lekko przezroczyste */
    private static int cardBg(float hv) {
        int c = R2D.blend(0xFF151518, 0xFF1C1C21, hv);
        return Theme.style.equals("Glass") ? R2D.withAlpha(c, 0.78f) : c;
    }

    private void click() {
        if (client != null && Theme.sounds) client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.7f, 0.18f));
    }

    private List<Module> list() {
        List<Module> base;
        if (!query.isEmpty()) {
            String q = query.toLowerCase(Locale.ROOT);
            base = Modules.all().stream().filter(m -> m.name.toLowerCase(Locale.ROOT).contains(q) || m.description().toLowerCase(Locale.ROOT).contains(q)).toList();
        } else base = Modules.of(category);
        if (filter == 1) return base.stream().filter(Module::isEnabled).toList();
        if (filter == 2) return base.stream().filter(m -> m.favorite).toList();
        return base;
    }

    private static String keyName(int key) {
        if (key < 0) return "";
        String s = InputUtil.fromKeyCode(key, 0).getLocalizedText().getString();
        return s.length() > 9 ? s.substring(0, 9) : s.toUpperCase(Locale.ROOT);
    }

    private void toggleModule(Module m) {
        if (m.name.equals("HUDEditor")) { openHudEditor(); return; }
        m.toggle();
        Modules.save();
        if (Theme.toasts) {
            TOASTS.add(new Toast(m.icon, m.name, m.isEnabled(), System.currentTimeMillis()));
            while (TOASTS.size() > 4) TOASTS.remove(0);
        }
    }

    private void openHudEditor() {
        if (client != null && client.player != null) { Modules.save(); client.setScreen(new dev.with.client.hud.HudEditor(this)); }
    }

    private void goPage(Page p) {
        if (page != p) { page = p; pageTime = System.currentTimeMillis(); appScroll = appScrollT = 0; }
    }

    /** przesunięcie wjeżdżającej treści strony */
    private float slideIn(long since, float dist) {
        return (1 - Anim.enter((System.currentTimeMillis() - since) / (300f / Theme.motionSpeed()))) * dist;
    }
    private float fadeIn(long since) { return Anim.easeOutCubic((System.currentTimeMillis() - since) / (240f / Theme.motionSpeed())); }

    // ================================================================== render

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (client != null && client.world != null && Theme.blur) applyBlur(delta);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hits.clear();
        clips.clear();
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (System.nanoTime() - lastFrame) / 1e9f);
        lastFrame = System.nanoTime();
        float a = closeTime < 0 ? Anim.easeOutCubic((now - openTime) / (280f / Theme.motionSpeed())) : 1 - Anim.easeInCubic((now - closeTime) / (170f / Theme.motionSpeed()));
        if (closeTime >= 0 && a <= 0) { if (client != null) client.setScreen(null); return; }
        renderBackground(ctx, mouseX, mouseY, delta);
        R2D.alpha = 1;
        R2D.radiusMul = 1;
        R2D.rect(ctx, 0, 0, width, height, R2D.withAlpha(0xFF000000, Theme.dim * a));
        if (Theme.vignette) {
            R2D.gradientV(ctx, 0, 0, width, height * 0.3f, R2D.withAlpha(0xFF000000, 0.35f * a), 0x00000000);
            R2D.gradientV(ctx, 0, height * 0.6f, width, height * 0.4f, 0x00000000, R2D.withAlpha(0xFF000000, 0.45f * a));
        }

        // rozmiar niezależny od skali GUI Minecrafta (zawsze ta sama wielkość na ekranie)
        double gs = client != null ? client.getWindow().getScaleFactor() : 2;
        if (!"scale".equals(appDragKey)) liveScale = Theme.scale;
        float s = liveScale * (float) (2.0 / gs);
        W = Math.min(560, width / s - 20);
        H = Math.min(350, height / s - 22);
        x0 = (width - W) / 2f;
        y0 = (height - H) / 2f;
        sc = s * (0.95f + 0.05f * a);
        float lift = closeTime < 0 ? (1 - a) * 10 : 0;
        mx = (int) Math.floor(toLocalX(mouseX)); my = (int) Math.floor(toLocalY(mouseY) - lift);
        if (dragApply != null) dragApply.run();

        ctx.getMatrices().push();
        ctx.getMatrices().translate(width / 2f, height / 2f, 0);
        ctx.getMatrices().scale(sc, sc, 1);
        ctx.getMatrices().translate(-width / 2f, -height / 2f + lift, 0);
        R2D.alpha = a;
        R2D.radiusMul = Theme.radius;

        drawWindow(ctx, now, dt);

        clip(ctx, x0, y0, W, H);
        navR = x0 + NAV;
        cx0 = navR + 12; cx1 = x0 + W - 12;
        drawNav(ctx);
        switch (page) {
            case MODULES -> drawModulesPage(ctx);
            case APPEARANCE -> drawAppearance(ctx);
            case CONFIGS -> drawConfigs(ctx);
            case FRIENDS -> drawFriends(ctx);
        }
        drawToasts(ctx, now);
        unclip(ctx);
        drawDropdown(ctx);
        drawColorPicker(ctx);
        drawTooltip(ctx, now);
        drawHints(ctx);
        ctx.getMatrices().pop();
        R2D.alpha = 1;
        R2D.radiusMul = 1;
    }

    private float navR, liveScale = Theme.scale;

    private float toLocalX(double sx) { return width / 2f + ((float) sx - width / 2f) / sc; }
    private float toLocalY(double sy) { return height / 2f + ((float) sy - height / 2f) / sc; }

    // ------------------------------------------------------------------ okno + efekty

    private void drawWindow(DrawContext ctx, long now, float dt) {
        int acc = Theme.accent();
        boolean minimal = Theme.style.equals("Minimal"), glass = Theme.style.equals("Glass");
        if (Theme.shadow && !minimal) R2D.shadow(ctx, x0, y0 + 8, W, H, 30, 0xCC000000);
        if (!minimal) R2D.glow(ctx, x0 - 40, y0 - 40, W + 80, H + 80, R2D.withAlpha(acc, Theme.mono() ? 0.035f : 0.06f));
        float r = 14;
        if (glass) {
            R2D.roundRectV(ctx, x0, y0, W, H, r, 0xC2141418, 0xCC08080A);
            R2D.roundRectV(ctx, x0, y0, W, H * 0.45f, r, 0x10FFFFFF, 0x00FFFFFF);
        } else if (minimal) {
            R2D.roundRect(ctx, x0, y0, W, H, r, 0xFA0B0B0D);
        } else {
            R2D.roundRectV(ctx, x0, y0, W, H, r, 0xF7111114, 0xF908080A);
        }
        if (Theme.particles) drawParticles(ctx, dt);
        R2D.roundOutline(ctx, x0, y0, W, H, r, 0.7f, minimal ? 0x1EFFFFFF : 0x2AFFFFFF);
        if (!minimal) {
            R2D.gradientH(ctx, x0 + 30, y0 + 0.6f, (W - 60) / 2f, 0.6f, 0x00FFFFFF, R2D.withAlpha(acc, 0.5f));
            R2D.gradientH(ctx, x0 + W / 2f, y0 + 0.6f, (W - 60) / 2f, 0.6f, R2D.withAlpha(acc, 0.5f), 0x00FFFFFF);
        }
        if (Theme.border) drawBorderComet(ctx, now);
    }

    /** siatka cząsteczek w tle okna – łączą się liniami ze sobą i z kursorem */
    private void drawParticles(DrawContext ctx, float dt) {
        float spd = Math.min(Theme.motionSpeed(), 2f);
        for (int i = 0; i < PN; i++) {
            PX[i] += VX[i] * dt * spd; PY[i] += VY[i] * dt * spd;
            if (PX[i] < 0) PX[i] += 1; if (PX[i] > 1) PX[i] -= 1;
            if (PY[i] < 0) PY[i] += 1; if (PY[i] > 1) PY[i] -= 1;
        }
        float maxD = 64, maxD2 = maxD * maxD;
        int[] cols = new int[PN * PN / 2 + PN];
        float[] seg = new float[cols.length * 4];
        int n = 0;
        int base = Theme.mono() ? 0xFFFFFFFF : Theme.accent();
        float[] xs = new float[PN], ys = new float[PN];
        float ax = x0 + NAV + 6, aw = W - NAV - 12;
        for (int i = 0; i < PN; i++) { xs[i] = ax + PX[i] * aw; ys[i] = y0 + 6 + PY[i] * (H - 12); }
        for (int i = 0; i < PN; i++) {
            for (int j = i + 1; j < PN; j++) {
                float dx = xs[i] - xs[j], dy = ys[i] - ys[j], d2 = dx * dx + dy * dy;
                if (d2 > maxD2) continue;
                float k = 1 - (float) Math.sqrt(d2) / maxD;
                seg[n * 4] = xs[i]; seg[n * 4 + 1] = ys[i]; seg[n * 4 + 2] = xs[j]; seg[n * 4 + 3] = ys[j];
                cols[n++] = R2D.withAlpha(base, 0.09f * k);
                if (n >= cols.length - PN) break;
            }
            float dx = xs[i] - mx, dy = ys[i] - my, d2 = dx * dx + dy * dy;
            if (d2 < 90 * 90 && n < cols.length) {
                float k = 1 - (float) Math.sqrt(d2) / 90;
                seg[n * 4] = xs[i]; seg[n * 4 + 1] = ys[i]; seg[n * 4 + 2] = mx; seg[n * 4 + 3] = my;
                cols[n++] = R2D.withAlpha(base, 0.16f * k);
            }
        }
        R2D.lines(ctx, seg, cols, n, 0.5f);
        for (int i = 0; i < PN; i++) R2D.circle(ctx, xs[i], ys[i], 0.9f, R2D.withAlpha(base, 0.3f));
    }

    /** świetlny punkt okrążający ramkę okna */
    private void drawBorderComet(DrawContext ctx, long now) {
        float per = 2 * (W + H);
        for (int c = 0; c < 2; c++) {
            float t = ((now % 7000) / 7000f + c * 0.5f) % 1f * per;
            float bx, by; boolean horiz;
            if (t < W) { bx = x0 + t; by = y0; horiz = true; }
            else if (t < W + H) { bx = x0 + W; by = y0 + (t - W); horiz = false; }
            else if (t < 2 * W + H) { bx = x0 + W - (t - W - H); by = y0 + H; horiz = true; }
            else { bx = x0; by = y0 + H - (t - 2 * W - H); horiz = false; }
            int col = Theme.mono() ? 0xFFFFFFFF : Theme.accent();
            float k = c == 0 ? 1 : 0.55f, len = 46;
            R2D.glow(ctx, bx - 22, by - 22, 44, 44, R2D.withAlpha(col, 0.16f * k));
            if (horiz) {
                float lx = Math.max(x0 + 12, bx - len), rx = Math.min(x0 + W - 12, bx + len);
                if (bx > lx) R2D.gradientH(ctx, lx, by - 0.4f, bx - lx, 0.9f, 0x00FFFFFF & col, R2D.withAlpha(col, 0.7f * k));
                if (rx > bx) R2D.gradientH(ctx, bx, by - 0.4f, rx - bx, 0.9f, R2D.withAlpha(col, 0.7f * k), 0x00FFFFFF & col);
            } else {
                float ty = Math.max(y0 + 12, by - len), byy = Math.min(y0 + H - 12, by + len);
                if (by > ty) R2D.gradientV(ctx, bx - 0.4f, ty, 0.9f, by - ty, 0x00FFFFFF & col, R2D.withAlpha(col, 0.7f * k));
                if (byy > by) R2D.gradientV(ctx, bx - 0.4f, by, 0.9f, byy - by, R2D.withAlpha(col, 0.7f * k), 0x00FFFFFF & col);
            }
        }
    }

    // ------------------------------------------------------------------ nawigacja

    private void drawNav(DrawContext ctx) {
        float prev = R2D.alpha;
        R2D.rect(ctx, x0 + NAV, y0 + 12, 0.5f, H - 24, 0x12FFFFFF);
        R2D.rect(ctx, x0 + 1, y0 + 1, NAV - 1, H - 2, 0x06FFFFFF);
        // logo
        float lx = x0 + 12, ly = y0 + 12;
        logo(ctx, lx, ly, 24, 0.6f);
        float tx = lx + 31;
        float ww = Fonts.width("With", Font.BOLD, 0.8f);
        Fonts.draw(ctx, "With", tx, ly + 3.5f, TEXT, Font.BOLD, 0.8f);
        Fonts.draw(ctx, "Client", tx + ww, ly + 3.5f, DIM, Font.SEMIBOLD, 0.8f);
        Fonts.draw(ctx, "v" + WithClient.VERSION, tx, ly + 14.5f, FAINT, Font.SEMIBOLD, 0.52f);

        Category[] cats = Category.values();
        String[][] pages = {{"l_palette", t("Wygląd", "Appearance")}, {"l_folder", t("Konfiguracje", "Configs")}, {"l_users", t("Znajomi", "Friends")}};
        float top = y0 + 46, bottom = y0 + H - 36;
        float rh = Math.min(19, (bottom - top - 26) / (cats.length + pages.length));
        float y = top;
        spaced(ctx, t("KATEGORIE", "CATEGORIES"), x0 + 14, y, FAINT, Font.BOLD, 0.42f, 1.2f); y += 9;
        List<float[]> rows = new ArrayList<>();
        for (int i = 0; i < cats.length; i++) { rows.add(new float[]{y, i}); y += rh; }
        y += 5;
        spaced(ctx, t("KLIENT", "CLIENT"), x0 + 14, y, FAINT, Font.BOLD, 0.42f, 1.2f); y += 9;
        for (int i = 0; i < pages.length; i++) { rows.add(new float[]{y, cats.length + i}); y += rh; }

        int active = page == Page.MODULES ? (query.isEmpty() ? category.ordinal() : -1) : cats.length + (page == Page.APPEARANCE ? 0 : page == Page.CONFIGS ? 1 : 2);
        float px = x0 + 8, pw = NAV - 15;
        float ty = -1;
        for (float[] r : rows) if ((int) r[1] == active) ty = r[0];
        if (ty >= 0 && catAnim.raw() < 0) catAnim.snap(ty);
        float pa = anim("navPill").animate(active >= 0 ? 1 : 0);
        float cy = catAnim.animate(ty >= 0 ? ty : catAnim.target());
        if (pa > 0.01f) {
            R2D.alpha = prev * pa;
            R2D.glow(ctx, px - 6, cy - 5, pw + 12, rh + 8, R2D.withAlpha(Theme.accent(), Theme.mono() ? 0.07f : 0.14f));
            R2D.roundRectV(ctx, px, cy + 1, pw, rh - 3, 6, Theme.accentTop(), Theme.accentBottom());
            R2D.alpha = prev;
        }
        for (float[] r : rows) {
            int idx = (int) r[1]; float ry = r[0];
            boolean isCat = idx < cats.length;
            String icon = isCat ? cats[idx].icon : pages[idx - cats.length][0];
            String name = isCat ? cats[idx].title() : pages[idx - cats.length][1];
            float near = pa * Anim.clamp01(1 - Math.abs(cy - ry) / rh);
            float hv = anim("nav" + idx).animate(over(px, ry, pw, rh - 2) && idx != active ? 1 : 0);
            if (hv > 0.01f) R2D.roundRect(ctx, px, ry + 1, pw, rh - 3, 6, R2D.withAlpha(0xFFFFFFFF, 0.045f * hv));
            int fg = R2D.blend(R2D.blend(0xFF9A9AA4, TEXT, hv), Theme.onAccent(), near);
            float shift = 2.5f * hv;
            float mid = ry + 1 + (rh - 3) / 2f;
            R2D.icon(ctx, icon, px + 7 + shift, mid - 4.5f, 9, fg);
            Fonts.drawV(ctx, name, px + 22 + shift, mid + 0.5f, fg, near > 0.5f ? Font.BOLD : Font.SEMIBOLD, 0.64f);
            if (isCat) {
                long en = Modules.enabledIn(cats[idx]);
                if (en > 0) {
                    String c = String.valueOf(en);
                    float cw = Math.max(10, Fonts.width(c, Font.BOLD, 0.46f) + 6);
                    R2D.roundRect(ctx, px + pw - 5 - cw, mid - 4.5f, cw, 9, 4.5f, R2D.blend(0x14FFFFFF, R2D.withAlpha(Theme.onAccent(), 0.14f), near));
                    Fonts.drawV(ctx, c, px + pw - 5 - cw + (cw - Fonts.width(c, Font.BOLD, 0.46f)) / 2f, mid + 0.5f, R2D.blend(DIM, Theme.onAccent(), near), Font.BOLD, 0.46f);
                }
            }
            final int fi = idx;
            hit(px, ry, pw, rh - 1, b -> {
                if (b != 0) return;
                click(); query = ""; focus = 0; drop = null;
                if (fi < cats.length) {
                    if (page != Page.MODULES || category != cats[fi]) { listTime = System.currentTimeMillis(); listScroll = listScrollT = 0; }
                    goPage(Page.MODULES); category = cats[fi];
                } else goPage(new Page[]{Page.APPEARANCE, Page.CONFIGS, Page.FRIENDS}[fi - cats.length]);
            });
        }

        // profil
        float by = y0 + H - 30;
        R2D.rect(ctx, x0 + 12, by - 4, NAV - 24, 0.5f, 0x12FFFFFF);
        head(ctx, x0 + 12, by + 3, 16);
        String nick = client != null ? client.getSession().getUsername() : "Player";
        Fonts.draw(ctx, Fonts.trim(nick, Font.BOLD, 0.62f, NAV - 46), x0 + 33, by + 3, TEXT, Font.BOLD, 0.62f);
        long on = Modules.all().stream().filter(Module::isEnabled).count();
        Fonts.draw(ctx, on + " / " + Modules.all().size() + t(" aktywnych", " active"), x0 + 33, by + 12.5f, FAINT, Font.REGULAR, 0.5f);
        R2D.alpha = prev;
    }

    // ------------------------------------------------------------------ nagłówek

    private void drawHeader(DrawContext ctx, String title, String sub, float slide) {
        Fonts.draw(ctx, title, cx0 + slide, y0 + 13, TEXT, Font.BOLD, 0.92f);
        Fonts.draw(ctx, sub, cx0 + slide, y0 + 26, FAINT, Font.REGULAR, 0.52f);
        R2D.rect(ctx, cx0, y0 + 42, cx1 - cx0, 0.5f, 0x12FFFFFF);
    }

    // ------------------------------------------------------------------ moduły

    private void drawModulesPage(DrawContext ctx) {
        List<Module> list = list();
        String title = !query.isEmpty() ? t("Wyniki wyszukiwania", "Search results") : category.title();
        long en = list.stream().filter(Module::isEnabled).count();
        drawHeader(ctx, title, list.size() + t(" modułów · ", " modules · ") + en + t(" aktywnych", " active"), query.isEmpty() ? slideIn(pageTime, 8) : 0);

        // filtr (segmenty z ikonami) + wyszukiwarka
        String[] fIcons = {"l_layout_grid", "l_power", "l_star"};
        String[] fTips = {t("Wszystkie", "All"), t("Aktywne", "Active"), t("Ulubione", "Favorites")};
        float segW = 20, segH = 16, fx = cx1 - segW * 3 - 4, fy = y0 + 13;
        R2D.roundRect(ctx, fx - 2, fy - 2, segW * 3 + 4, segH + 4, 7, 0x0EFFFFFF);
        R2D.roundOutline(ctx, fx - 2, fy - 2, segW * 3 + 4, segH + 4, 7, 0.5f, 0x16FFFFFF);
        float ix = pos("filterX", fx + filter * segW);
        R2D.roundRectV(ctx, ix, fy, segW, segH, 5.5f, Theme.accentTop(), Theme.accentBottom());
        for (int i = 0; i < 3; i++) {
            float bx = fx + i * segW;
            float near = Anim.clamp01(1 - Math.abs(ix - bx) / segW);
            float hv = hover("flt" + i, bx, fy, segW, segH);
            R2D.icon(ctx, fIcons[i], bx + segW / 2f - 4, fy + segH / 2f - 4, 8, R2D.blend(R2D.blend(FAINT, TEXT, hv), Theme.onAccent(), near));
            final int fi = i;
            hit(bx, fy, segW, segH, b -> { if (b == 0 && filter != fi) { filter = fi; listTime = System.currentTimeMillis(); listScroll = listScrollT = 0; click(); } });
            if (hv > 0.6f) { tipText = fTips[i]; }
        }
        float sOpen = anim("searchW").animate(focus == 1 || !query.isEmpty() ? 1 : 0);
        float sw = Anim.lerp(104, 150, sOpen), sx = fx - 10 - sw;
        field(ctx, 1, sx, fy - 1, sw, 18, query, t("Szukaj…", "Search…"), "l_search");
        if (category == Category.HUD && query.isEmpty()) {
            String el = t("Edytuj HUD", "Edit HUD");
            float ebw = Fonts.width(el, Font.BOLD, 0.6f) + 26, ebx = sx - 8 - ebw;
            button(ctx, ebx, fy - 1, ebw, 18, "l_move", el, true, hover("hudEdit", ebx, fy - 1, ebw, 18), true);
            hit(ebx, fy - 1, ebw, 18, b -> { if (b == 0) { click(); openHudEditor(); } });
        }
        if (!query.isEmpty()) {
            float bx = sx + sw - 15;
            R2D.icon(ctx, "l_x", bx, fy + 4.5f, 8, over(bx - 3, fy - 1, 15, 18) ? TEXT : FAINT);
            hit(bx - 3, fy - 1, 15, 18, b -> { query = ""; listTime = System.currentTimeMillis(); });
        }

        // panel ustawień wysuwany z prawej
        if (selected != null) shown = selected;
        float e = Anim.clamp01(panel.animate(selected != null ? 1 : 0));
        float eRaw = panel.raw();
        float top = y0 + 50, bottom = y0 + H - 12;
        float fullW = cx1 - cx0, openW = fullW - PW - 10;
        drawModuleGrid(ctx, list, cx0, top, fullW, openW, bottom - top, e);
        if (eRaw > 0.002f && shown != null) {
            float px = cx1 - PW + (1 - eRaw) * (PW + 24);
            float pa = R2D.alpha; R2D.alpha = pa * Anim.clamp01(e * 1.4f);
            drawPanel(ctx, shown, px, top, PW, bottom - top);
            R2D.alpha = pa;
        } else if (eRaw <= 0.002f) shown = null;
    }

    private String tipText;

    private void drawModuleGrid(DrawContext ctx, List<Module> list, float x, float top, float fullW, float openW, float areaH, float e) {
        float gap = 6;
        float cw2 = (fullW - gap) / 2f;
        boolean desc = Theme.descriptions;
        float rhC = desc ? 34 : 26, rhO = 26;
        int n = list.size();
        float hClosed = ((n + 1) / 2) * (rhC + gap), hOpen = n * (rhO + gap);
        float contentH = Anim.lerp(hClosed, hOpen, e);
        listMax = Math.max(0, contentH - areaH);
        listScrollT = Math.max(0, Math.min(listMax, listScrollT));
        listScroll += (listScrollT - listScroll) * 0.3f;
        float lw = Anim.lerp(fullW, openW, e);
        clip(ctx, x - 4, top - 2, lw + 8, areaH + 4);
        long now = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            float xc = x + (i % 2) * (cw2 + gap), yc = top + (i / 2) * (rhC + gap);
            float yo = top + i * (rhO + gap);
            float rx = Anim.lerp(xc, x, e), ry = Anim.lerp(yc, yo, e) - listScroll, rw = Anim.lerp(cw2, openW, e), rh = Anim.lerp(rhC, rhO, e);
            if (ry + rh < top - 2 || ry > top + areaH + 2) continue;
            float st = Anim.enter((now - listTime - Math.min(i, 16) * 16) / (300f / Theme.motionSpeed()));
            float pv = R2D.alpha; R2D.alpha = pv * Anim.clamp01(st);
            drawRow(ctx, list.get(i), rx + (1 - st) * 10, ry, rw, rh, desc ? Anim.clamp01((rh - 28) / 5f) : 0);
            R2D.alpha = pv;
        }
        unclip(ctx);
        if (list.isEmpty()) {
            float ccx = x + lw / 2f, ccy = top + areaH / 2f - 10;
            R2D.icon(ctx, filter == 2 ? "l_star" : "l_search", ccx - 9, ccy - 20, 18, FAINT);
            Fonts.drawCentered(ctx, filter == 2 ? t("Brak ulubionych", "No favorites") : filter == 1 ? t("Nic nie jest włączone", "Nothing is enabled") : t("Nic nie znaleziono", "Nothing found"), ccx, ccy + 4, DIM, Font.SEMIBOLD, 0.68f);
        }
        if (listMax > 0) {
            float th = Math.max(18, areaH * areaH / contentH), tyy = top + (areaH - th) * (listScroll / listMax);
            R2D.roundRect(ctx, x + lw + 2, tyy, 1.5f, th, 0.75f, 0x44FFFFFF);
        }
    }

    private void drawRow(DrawContext ctx, Module m, float x, float y, float w, float h, float descA) {
        boolean sel = selected == m;
        boolean ov = over(x, y, w, h);
        float hv = anim("lh" + m.name).animate(ov ? 1 : 0);
        float on = state("lon" + m.name, 16, m.isEnabled() ? 1 : 0);
        float sa = anim("lsel" + m.name).animate(sel ? 1 : 0);
        int acc = Theme.accent();
        R2D.roundRect(ctx, x, y, w, h, 8, cardBg(hv));
        if (sa > 0.01f) R2D.roundRect(ctx, x, y, w, h, 8, R2D.withAlpha(acc, (Theme.mono() ? 0.06f : 0.1f) * sa));
        R2D.roundOutline(ctx, x, y, w, h, 8, 0.55f, R2D.blend(R2D.blend(0x10FFFFFF, 0x1EFFFFFF, hv), R2D.withAlpha(acc, 0.6f), sa));
        // kafelek ikony
        float ts = 18, tx = x + 7, tyy = y + h / 2f - ts / 2f;
        if (on > 0.01f) R2D.glow(ctx, tx - 6, tyy - 6, ts + 12, ts + 12, R2D.withAlpha(acc, (Theme.mono() ? 0.12f : 0.22f) * on));
        R2D.roundRectV(ctx, tx, tyy, ts, ts, 5.5f, R2D.blend(0xFF1D1D22, Theme.accentTop(), on), R2D.blend(0xFF131317, Theme.accentBottom(), on));
        if (on < 0.99f) R2D.roundOutline(ctx, tx, tyy, ts, ts, 5.5f, 0.5f, R2D.withAlpha(0x1CFFFFFF, 1 - on));
        R2D.icon(ctx, m.icon, tx + 4.5f, tyy + 4.5f, 9, R2D.blend(R2D.blend(0xFFB4B4BC, 0xFFE4E4E7, hv), Theme.onAccent(), on));
        // prawa strona: przełącznik, klawisz, ulubiony
        float right = x + w - 26;
        Ui.toggle(ctx, right, y + h / 2f - 5, on, hv);
        float rx = right - 5;
        String kn = binding == m ? "…" : (Theme.keysInList ? keyName(m.key) : "");
        if (!kn.isEmpty()) {
            float kw = Math.max(12, Fonts.width(kn, Font.BOLD, 0.46f) + 7);
            rx -= kw;
            R2D.roundRect(ctx, rx, y + h / 2f - 5.5f, kw, 11, 3.5f, binding == m ? R2D.withAlpha(acc, 0.45f) : 0x16FFFFFF);
            Fonts.drawV(ctx, kn, rx + (kw - Fonts.width(kn, Font.BOLD, 0.46f)) / 2f, y + h / 2f + 0.5f, binding == m ? Theme.onAccent() : DIM, Font.BOLD, 0.46f);
            rx -= 4;
        }
        if (m.favorite) { rx -= 8; R2D.icon(ctx, "l_star", rx, y + h / 2f - 4, 8, R2D.withAlpha(0xFFFFFFFF, 0.55f)); rx -= 3; }
        // nazwa + opis
        float nx = tx + ts + 7, maxW = rx - nx - 4;
        int nameCol = R2D.blend(0xFFDCDCE0, 0xFFFFFFFF, Math.max(on, hv));
        if (descA > 0.01f) {
            float ny = y + h / 2f - 4.5f * descA;
            Fonts.drawV(ctx, Fonts.trim(m.name, Font.BOLD, 0.68f, maxW), nx, ny + 0.5f, nameCol, Font.BOLD, 0.68f);
            float pa = R2D.alpha; R2D.alpha = pa * descA;
            Fonts.drawV(ctx, Fonts.trim(m.description(), Font.REGULAR, 0.52f, maxW), nx, ny + 10.5f, FAINT, Font.REGULAR, 0.52f);
            R2D.alpha = pa;
        } else {
            Fonts.drawV(ctx, Fonts.trim(m.name, Font.BOLD, 0.68f, maxW), nx, y + h / 2f + 0.5f, nameCol, Font.BOLD, 0.68f);
        }
        if (m.changed() > 0) R2D.circle(ctx, x + w - 4, y + 4, 1.3f, R2D.withAlpha(acc, 0.8f));
        if (ov) { if (tipModule != m) { tipModule = m; tipStart = System.currentTimeMillis(); } tipSeen = true; }
        hit(x, y, w, h, b -> {
            if (b == 0) {
                if (over(right - 4, y, 30, h)) toggleModule(m);
                else selectModule(selected == m ? null : m);
            } else if (b == 1) selectModule(m);
            else if (b == 2) binding = m;
            click();
        });
    }
    private boolean tipSeen;

    private void selectModule(Module m) {
        if (m != null && selected != m) { setScroll = setScrollT = 0; setTime = System.currentTimeMillis(); }
        drop = null;
        selected = m;
    }

    // ------------------------------------------------------------------ panel ustawień

    private void drawPanel(DrawContext ctx, Module m, float x, float y, float w, float h) {
        R2D.shadow(ctx, x, y + 3, w, h, 16, 0x66000000);
        if (Theme.style.equals("Glass")) R2D.roundRectV(ctx, x, y, w, h, 11, 0xD8141418, 0xE00E0E11);
        else R2D.roundRectV(ctx, x, y, w, h, 11, 0xFF141418, 0xFF0E0E11);
        R2D.roundOutline(ctx, x, y, w, h, 11, 0.6f, 0x1CFFFFFF);
        clip(ctx, x, y, w, h);
        float slide = slideIn(setTime, 12), fade = fadeIn(setTime);
        float pa = R2D.alpha; R2D.alpha = pa * fade;
        float px = x + 10 + slide, pw = w - 20;
        float on = state("pon" + m.name, 16, m.isEnabled() ? 1 : 0);
        // nagłówek
        float ty = y + 10;
        if (on > 0.01f) R2D.glow(ctx, px - 6, ty - 6, 36, 36, R2D.withAlpha(Theme.accent(), (Theme.mono() ? 0.12f : 0.22f) * on));
        R2D.roundRectV(ctx, px, ty, 24, 24, 7, R2D.blend(0xFF1F1F24, Theme.accentTop(), on), R2D.blend(0xFF141418, Theme.accentBottom(), on));
        R2D.icon(ctx, m.icon, px + 6, ty + 6, 12, R2D.blend(0xFFD4D4D8, Theme.onAccent(), on));
        Fonts.draw(ctx, Fonts.trim(m.name, Font.BOLD, 0.8f, pw - 52), px + 31, ty + 2, TEXT, Font.BOLD, 0.8f);
        String cat = m.category.title().toUpperCase(Locale.ROOT) + " · " + m.optionCount() + t(" OPCJI", " OPTIONS");
        Fonts.draw(ctx, cat, px + 31, ty + 14, FAINT, Font.BOLD, 0.42f);
        float cbx = x + w - 10 - 14 + slide;
        float ch = hover("pclose", cbx, ty, 14, 14);
        R2D.roundRect(ctx, cbx, ty, 14, 14, 7, R2D.blend(0x10FFFFFF, 0x26FFFFFF, ch));
        R2D.icon(ctx, "l_x", cbx + 3.5f, ty + 3.5f, 7, R2D.blend(DIM, TEXT, ch));
        hit(cbx, ty, 14, 14, b -> { if (b == 0) { selectModule(null); click(); } });
        // przycisk włącz/wyłącz
        float by = ty + 30, bh = 18;
        float bhv = hover("pPower", px, by, pw, bh);
        if (on > 0.01f) R2D.glow(ctx, px - 6, by - 6, pw + 12, bh + 12, R2D.withAlpha(Theme.accent(), (Theme.mono() ? 0.08f : 0.16f) * on));
        R2D.roundRectV(ctx, px, by, pw, bh, 6, R2D.blend(R2D.blend(0x10FFFFFF, 0x1CFFFFFF, bhv), Theme.accentTop(), on), R2D.blend(R2D.blend(0x0CFFFFFF, 0x16FFFFFF, bhv), Theme.accentBottom(), on));
        if (on < 0.99f) R2D.roundOutline(ctx, px, by, pw, bh, 6, 0.5f, R2D.withAlpha(0x22FFFFFF, 1 - on));
        String pl = m.isEnabled() ? t("WŁĄCZONY", "ENABLED") : t("WYŁĄCZONY", "DISABLED");
        int pfg = R2D.blend(TEXT, Theme.onAccent(), on);
        float plw = Fonts.width(pl, Font.BOLD, 0.56f) + 12;
        float plx = px + (pw - plw) / 2f;
        R2D.icon(ctx, "l_power", plx, by + bh / 2f - 4, 8, pfg);
        Fonts.drawV(ctx, pl, plx + 12, by + bh / 2f + 0.5f, pfg, Font.BOLD, 0.56f);
        hit(px, by, pw, bh, b -> { if (b == 0) { toggleModule(m); click(); } });
        // opis
        float dy = by + bh + 7;
        List<String> lines = Fonts.wrap(m.description(), Font.REGULAR, 0.56f, pw);
        for (int i = 0; i < Math.min(2, lines.size()); i++) { Fonts.draw(ctx, lines.get(i), px, dy, DIM, Font.REGULAR, 0.56f); dy += 8.5f; }
        dy += 4;
        R2D.rect(ctx, px, dy, pw, 0.5f, 0x12FFFFFF);
        dy += 6;

        // przewijana lista ustawień
        float barH = 24, areaTop = dy, areaH = y + h - barH - 6 - areaTop;
        clip(ctx, x + 2, areaTop, w - 4, areaH);
        float cy = areaTop + 2 - setScroll;
        if (m.settings.isEmpty()) {
            Fonts.drawCentered(ctx, t("Ten moduł nie ma opcji", "This module has no options"), px + pw / 2f, areaTop + 16, FAINT, Font.SEMIBOLD, 0.6f);
            cy += 30;
        }
        cy = drawSettings(ctx, m, px, cy, pw);
        setMax = Math.max(0, cy + setScroll - areaTop - areaH + 4);
        setScrollT = Math.max(0, Math.min(setMax, setScrollT));
        setScroll += (setScrollT - setScroll) * 0.3f;
        unclip(ctx);
        if (setMax > 0) {
            float th = Math.max(16, areaH * areaH / (areaH + setMax)), tyy = areaTop + (areaH - th) * (setScroll / setMax);
            R2D.roundRect(ctx, x + w - 4, tyy, 1.5f, th, 0.75f, 0x44FFFFFF);
            if (setScroll > 1) R2D.gradientV(ctx, x + 1, areaTop, w - 2, 10, 0xFF131317, 0x00131317);
            if (setScroll < setMax - 1) R2D.gradientV(ctx, x + 1, areaTop + areaH - 10, w - 2, 10, 0x000E0E11, 0xFF0E0E11);
        }

        // dolny pasek: klawisz, ulubiony, reset
        float fy = y + h - barH - 2;
        R2D.rect(ctx, px, fy - 2, pw, 0.5f, 0x12FFFFFF);
        float bh2 = 16, byy = fy + 3;
        String kn = binding == m ? t("Naciśnij…", "Press…") : m.key < 0 ? t("Brak klawisza", "No key") : keyName(m.key);
        float kw = pw - 2 * (bh2 + 5);
        float khv = hover("pKey", px, byy, kw, bh2);
        boolean bnd = binding == m;
        R2D.roundRect(ctx, px, byy, kw, bh2, 5.5f, bnd ? Theme.accent() : R2D.blend(0x10FFFFFF, 0x1EFFFFFF, khv));
        R2D.icon(ctx, "l_keyboard", px + 6, byy + 4, 8, bnd ? Theme.onAccent() : DIM);
        Fonts.drawV(ctx, kn, px + 18, byy + bh2 / 2f + 0.5f, bnd ? Theme.onAccent() : TEXT, Font.BOLD, 0.54f);
        hit(px, byy, kw, bh2, b -> { if (b == 0) { binding = binding == m ? null : m; click(); } });
        float sx = px + kw + 5;
        float fv = state("pfav" + m.name, 16, m.favorite ? 1 : 0), shv = hover("pStar", sx, byy, bh2, bh2);
        R2D.roundRect(ctx, sx, byy, bh2, bh2, 5.5f, R2D.blend(0x10FFFFFF, 0x1EFFFFFF, shv));
        R2D.icon(ctx, "l_star", sx + 4, byy + 4, 8, R2D.blend(FAINT, 0xFFFFFFFF, Math.max(fv, shv * 0.5f)));
        hit(sx, byy, bh2, bh2, b -> { if (b == 0) { m.favorite = !m.favorite; Modules.save(); click(); } });
        float rx = sx + bh2 + 5;
        float rhv = hover("pReset", rx, byy, bh2, bh2);
        R2D.roundRect(ctx, rx, byy, bh2, bh2, 5.5f, R2D.blend(0x10FFFFFF, 0x1EFFFFFF, rhv));
        R2D.icon(ctx, "l_rotate_ccw", rx + 4, byy + 4, 8, R2D.blend(FAINT, TEXT, rhv));
        hit(rx, byy, bh2, bh2, b -> { if (b == 0) { m.reset(); Modules.save(); click(); } });
        if (rhv > 0.6f) tipText = t("Przywróć domyślne", "Reset to default");
        if (shv > 0.6f) tipText = t("Ulubiony", "Favorite");
        R2D.alpha = pa;
        unclip(ctx);
    }

    private float drawSettings(DrawContext ctx, Module m, float x, float y, float w) {
        List<Setting<?>> ss = m.settings;
        int i = 0;
        while (i < ss.size() && !(ss.get(i) instanceof Setting.Group)) y = drawOne(ctx, m, ss.get(i++), x, y, w);
        while (i < ss.size()) {
            Setting.Group g = (Setting.Group) ss.get(i++);
            int start = i;
            while (i < ss.size() && !(ss.get(i) instanceof Setting.Group)) i++;
            String key = m.name + "#" + g.name;
            int count = 0;
            for (int k = start; k < i; k++) if (ss.get(k).visible()) count++;
            // nagłówek grupy
            float open = state("g" + key, 14, g.get() ? 1 : 0);
            float hv = hover("gh" + key, x, y, w, 15);
            String gt = g.name.toUpperCase(Locale.ROOT);
            float gw = Ui.spacedWidth(gt, Font.BOLD, 0.44f, 1.1f);
            spaced(ctx, gt, x, y + 4, R2D.blend(FAINT, TEXT, hv), Font.BOLD, 0.44f, 1.1f);
            String cs = String.valueOf(count);
            Fonts.draw(ctx, cs, x + gw + 5, y + 4, R2D.withAlpha(0xFFFFFFFF, 0.25f), Font.BOLD, 0.42f);
            float lx = x + gw + 12 + Fonts.width(cs, Font.BOLD, 0.42f);
            R2D.rect(ctx, lx, y + 7, Math.max(0, x + w - 14 - lx), 0.5f, 0x14FFFFFF);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(x + w - 4.5f, y + 7, 0);
            ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90 * (1 - open)));
            ctx.getMatrices().translate(-(x + w - 4.5f), -(y + 7), 0);
            R2D.icon(ctx, "l_chevron_down", x + w - 8.5f, y + 3, 8, R2D.blend(FAINT, TEXT, hv));
            ctx.getMatrices().pop();
            hit(x, y, w, 15, b -> { if (b == 0) { g.toggle(); Modules.save(); click(); } });
            y += 17;
            float measured = groupH.getOrDefault(key, 0f);
            if (open > 0.004f) {
                float vis = measured * open;
                clip(ctx, x - 6, y - 2, w + 12, vis + 2);
                float pa = R2D.alpha; R2D.alpha = pa * open;
                float cy = y - (1 - open) * 6, sy = cy;
                for (int k = start; k < i; k++) cy = drawOne(ctx, m, ss.get(k), x, cy, w);
                R2D.alpha = pa;
                unclip(ctx);
                groupH.put(key, cy - sy);
                y += vis;
            }
            y += 4;
        }
        return y;
    }

    private float drawOne(DrawContext ctx, Module m, Setting<?> s, float x, float y, float w) {
        float v = state("vis" + m.name + "." + s.name, 16, s.visible() ? 1 : 0);
        if (v < 0.01f) return y;
        float pa = R2D.alpha; R2D.alpha = pa * v;
        float ny = drawSetting(ctx, m, s, x, y, w);
        R2D.alpha = pa;
        return y + (ny - y) * v;
    }

    private float drawSetting(DrawContext ctx, Module m, Setting<?> s, float x, float y, float w) {
        String key = m.name + "." + s.name;
        int acc = Theme.accent();
        if (s instanceof Setting.Bool bo) {
            float hv = hover("h" + key, x, y, w, 18);
            float on = state("b" + key, 18, bo.get() ? 1 : 0);
            Fonts.drawV(ctx, s.name, x, y + 9, R2D.blend(DIM, TEXT, Math.max(hv, on)), Font.SEMIBOLD, 0.6f);
            Ui.toggle(ctx, x + w - 18, y + 4, on, hv);
            hit(x, y, w, 18, b -> { if (b == 0) { bo.toggle(); Modules.save(); click(); } });
            return y + 20;
        }
        if (s instanceof Setting.Num n) {
            float hv = anim("sh" + key).animate(over(x, y, w, 24) || dragging == n ? 1 : 0);
            Fonts.drawV(ctx, s.name, x, y + 6, R2D.blend(DIM, TEXT, hv), Font.SEMIBOLD, 0.6f);
            valueChip(ctx, n.text(), x + w, y + 1, hv);
            float ty = y + 17, f = state("v" + key, 22, n.fraction());
            track(ctx, x, ty, w, 0, f, hv, false);
            final float fx = x, fw = w;
            hit(x - 4, y, w + 8, 24, b -> { if (b == 0) { dragging = n; dragApply = () -> n.set(n.min + (n.max - n.min) * Anim.clamp01((mx - fx) / fw)); dragApply.run(); } });
            return y + 26;
        }
        if (s instanceof Setting.Range r) {
            float hv = anim("sh" + key).animate(over(x, y, w, 24) || dragging == r ? 1 : 0);
            Fonts.drawV(ctx, s.name, x, y + 6, R2D.blend(DIM, TEXT, hv), Font.SEMIBOLD, 0.6f);
            valueChip(ctx, r.text(), x + w, y + 1, hv);
            float ty = y + 17, fl = state("vl" + key, 22, r.fLo()), fh = state("vh" + key, 22, r.fHi());
            track(ctx, x, ty, w, fl, fh, hv, true);
            final float fx = x, fw = w;
            hit(x - 4, y, w + 8, 24, b -> {
                if (b != 0) return;
                float f = Anim.clamp01((mx - fx) / fw);
                boolean hi = Math.abs(f - r.fHi()) < Math.abs(f - r.fLo()) || (r.fHi() == r.fLo() && f > r.fHi());
                dragging = r;
                dragApply = hi ? () -> r.setHi(r.min + (r.max - r.min) * Anim.clamp01((mx - fx) / fw))
                               : () -> r.setLo(r.min + (r.max - r.min) * Anim.clamp01((mx - fx) / fw));
                dragApply.run();
            });
            return y + 26;
        }
        if (s instanceof Setting.Mode o) {
            float labelW = Fonts.width(s.name, Font.SEMIBOLD, 0.6f);
            if (o.modes.size() <= 3) {
                float segW = 0;
                for (String md : o.modes) segW += Fonts.width(md, Font.BOLD, 0.52f) + 12;
                boolean inline = segW <= w - labelW - 10;
                float sx = inline ? x + w - segW : x, sy = inline ? y + 1 : y + 13;
                Fonts.drawV(ctx, s.name, x, y + 8, DIM, Font.SEMIBOLD, 0.6f);
                if (!inline) segW = w;
                float scale = inline ? 1 : w / Math.max(1, segW0(o));
                R2D.roundRect(ctx, sx, sy, segW, 15, 5.5f, 0x0EFFFFFF);
                R2D.roundOutline(ctx, sx, sy, segW, 15, 5.5f, 0.5f, 0x14FFFFFF);
                float cx = sx;
                float selX = sx, selW = 0;
                float[] xs = new float[o.modes.size()], ws = new float[o.modes.size()];
                for (int i = 0; i < o.modes.size(); i++) {
                    float mw = (Fonts.width(o.modes.get(i), Font.BOLD, 0.52f) + 12) * scale;
                    xs[i] = cx; ws[i] = mw;
                    if (o.get().equals(o.modes.get(i))) { selX = cx; selW = mw; }
                    cx += mw;
                }
                float ax = pos("segx" + key, selX), aw = pos("segw" + key, selW);
                R2D.roundRectV(ctx, ax + 1.5f, sy + 1.5f, Math.max(0, aw - 3), 12, 4.5f, Theme.accentTop(), Theme.accentBottom());
                for (int i = 0; i < o.modes.size(); i++) {
                    String md = o.modes.get(i);
                    float near = Anim.clamp01(1 - Math.abs(ax - xs[i]) / Math.max(1, ws[i]));
                    float hv = hover("mh" + key + md, xs[i], sy, ws[i], 15);
                    float tw = Fonts.width(md, Font.BOLD, 0.52f);
                    Fonts.drawV(ctx, md, xs[i] + (ws[i] - tw) / 2f, sy + 8, R2D.blend(R2D.blend(FAINT, TEXT, hv), Theme.onAccent(), near), Font.BOLD, 0.52f);
                    hit(xs[i], sy, ws[i], 15, b -> { if (b == 0) { o.set(md); Modules.save(); click(); } });
                }
                return sy + 15 + 5;
            }
            // lista rozwijana
            float hv = hover("dd" + key, x, y, w, 18);
            Fonts.drawV(ctx, s.name, x, y + 9, R2D.blend(DIM, TEXT, hv), Font.SEMIBOLD, 0.6f);
            float bw = Math.min(104, w - labelW - 10), bx = x + w - bw;
            boolean openD = drop == o;
            float oa = state("ddo" + key, 18, openD ? 1 : 0);
            R2D.roundRect(ctx, bx, y + 1.5f, bw, 15, 5.5f, R2D.blend(0x10FFFFFF, 0x1CFFFFFF, Math.max(hv, oa)));
            R2D.roundOutline(ctx, bx, y + 1.5f, bw, 15, 5.5f, 0.5f, R2D.blend(0x16FFFFFF, R2D.withAlpha(acc, 0.7f), oa));
            Fonts.drawV(ctx, Fonts.trim(o.get(), Font.BOLD, 0.54f, bw - 22), bx + 7, y + 9.5f, TEXT, Font.BOLD, 0.54f);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(bx + bw - 9, y + 9, 0);
            ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180 * oa));
            ctx.getMatrices().translate(-(bx + bw - 9), -(y + 9), 0);
            R2D.icon(ctx, "l_chevron_down", bx + bw - 13, y + 5, 8, DIM);
            ctx.getMatrices().pop();
            if (openD) { dropX = bx; dropY = y + 17.5f; dropW = bw; dropKey = key; }
            hit(x, y, w, 18, b -> { if (b == 0) { if (drop == o) drop = null; else { drop = o; dropRect = null; dropAnim.snap(0); } click(); } });
            return y + 20;
        }
        if (s instanceof Setting.Multi mu) {
            Fonts.drawV(ctx, s.name, x, y + 6, DIM, Font.SEMIBOLD, 0.6f);
            String cnt = mu.get().size() + "/" + mu.options.size();
            Fonts.drawV(ctx, cnt, x + w - Fonts.width(cnt, Font.BOLD, 0.48f), y + 6, FAINT, Font.BOLD, 0.48f);
            float cx = x, cy = y + 13;
            for (String op : mu.options) {
                boolean act = mu.has(op);
                float tw = Fonts.width(op, Font.BOLD, 0.5f) + (act ? 19 : 12);
                if (cx + tw > x + w && cx > x) { cx = x; cy += 17; }
                float sa = state("mu" + key + op, 16, act ? 1 : 0), hv = hover("muh" + key + op, cx, cy, tw, 14);
                R2D.roundRect(ctx, cx, cy, tw, 14, 7, R2D.blend(R2D.blend(0x0EFFFFFF, 0x1CFFFFFF, hv), R2D.withAlpha(acc, Theme.mono() ? 0.9f : 0.85f), sa));
                if (sa < 0.99f) R2D.roundOutline(ctx, cx, cy, tw, 14, 7, 0.5f, R2D.withAlpha(0x1AFFFFFF, 1 - sa));
                int fg = R2D.blend(R2D.blend(DIM, TEXT, hv), Theme.onAccent(), sa);
                float ox = cx + 6;
                if (sa > 0.01f) { float pa = R2D.alpha; R2D.alpha = pa * sa; R2D.icon(ctx, "l_check", ox - 0.5f, cy + 3.5f, 7, fg); R2D.alpha = pa; ox += 7 * sa; }
                Fonts.drawV(ctx, op, ox, cy + 7.5f, fg, Font.BOLD, 0.5f);
                hit(cx, cy, tw, 14, b -> { if (b == 0) { mu.flip(op); Modules.save(); click(); } });
                cx += tw + 4;
            }
            return cy + 14 + 6;
        }
        if (s instanceof Setting.Color c) {
            float hv = hover("ch" + key, x, y, w, 18);
            Fonts.drawV(ctx, s.name, x, y + 9, R2D.blend(DIM, TEXT, hv), Font.SEMIBOLD, 0.6f);
            float sw = 26, sx = x + w - sw;
            String hex = String.format("#%06X", c.get() & 0xFFFFFF);
            Fonts.drawV(ctx, hex, sx - 5 - Fonts.width(hex, Font.BOLD, 0.48f), y + 9, FAINT, Font.BOLD, 0.48f);
            R2D.roundRect(ctx, sx, y + 3, sw, 12, 4, 0xFF2A2A2E);
            R2D.roundRect(ctx, sx + sw / 2f, y + 3, sw / 2f, 12, 4, 0xFF46464C);
            R2D.roundRect(ctx, sx, y + 3, sw, 12, 4, c.get());
            R2D.roundOutline(ctx, sx, y + 3, sw, 12, 4, 0.6f, picking == c ? 0xCCFFFFFF : R2D.blend(0x30FFFFFF, 0x66FFFFFF, hv));
            final float ax = sx, ay = y + 16;
            if (picking == c) { pickAnchorX = ax; pickAnchorY = ay; }
            hit(x, y, w, 18, b -> { if (b == 0) { if (picking == c) picking = null; else { openPicker(c); pickAnchorX = ax; pickAnchorY = ay; } click(); } });
            return y + 20;
        }
        if (s instanceof Setting.Text tx) {
            float hv = hover("th" + key, x, y, w, 18);
            boolean f = focus == 3 && editText == tx;
            Fonts.drawV(ctx, s.name, x, y + 9, R2D.blend(DIM, TEXT, Math.max(hv, f ? 1 : 0)), Font.SEMIBOLD, 0.6f);
            float bw = Math.min(112, w * 0.58f), bx = x + w - bw;
            float fa = state("tf" + key, 18, f ? 1 : 0);
            R2D.roundRect(ctx, bx, y + 1.5f, bw, 15, 5.5f, R2D.blend(0x0EFFFFFF, 0x18FFFFFF, Math.max(hv * 0.6f, fa)));
            R2D.roundOutline(ctx, bx, y + 1.5f, bw, 15, 5.5f, 0.5f, R2D.blend(0x16FFFFFF, R2D.withAlpha(acc, 0.8f), fa));
            String val = tx.get();
            String shownTxt = Fonts.trim(val.isEmpty() ? "…" : val, Font.REGULAR, 0.56f, bw - 14);
            Fonts.drawV(ctx, shownTxt, bx + 6, y + 9.5f, val.isEmpty() ? FAINT : TEXT, Font.REGULAR, 0.56f);
            if (f && (System.currentTimeMillis() / 500) % 2 == 0) {
                float cx = bx + 6 + (val.isEmpty() ? 0 : Fonts.width(shownTxt, Font.REGULAR, 0.56f) + 0.5f);
                R2D.rect(ctx, cx, y + 5, 0.6f, 9, TEXT);
            }
            hit(x, y, w, 18, b -> { if (b == 0) { focus = 3; editText = tx; click(); } });
            return y + 20;
        }
        return y;
    }

    private static float segW0(Setting.Mode o) {
        float sw = 0;
        for (String md : o.modes) sw += Fonts.width(md, Font.BOLD, 0.52f) + 12;
        return sw;
    }

    private void valueChip(DrawContext ctx, String v, float right, float y, float hv) {
        float vw = Fonts.width(v, Font.BOLD, 0.52f) + 9;
        R2D.roundRect(ctx, right - vw, y, vw, 11, 5.5f, R2D.blend(0x12FFFFFF, 0x22FFFFFF, hv));
        Fonts.drawV(ctx, v, right - vw + 4.5f, y + 6, TEXT, Font.BOLD, 0.52f);
    }

    /** suwak: wypełnienie od f0 do f1 (zakres) albo od 0 do f1 (liczba) */
    private void track(DrawContext ctx, float x, float ty, float w, float f0, float f1, float hv, boolean twoKnobs) {
        float th = 3;
        R2D.roundRect(ctx, x, ty, w, th, 1.5f, 0x18FFFFFF);
        float a0 = x + w * f0, a1 = x + w * f1;
        int acc = Theme.accent();
        R2D.roundRectH(ctx, a0, ty, Math.max(th, a1 - a0), th, 1.5f, R2D.blend(acc, 0xFF000000, Theme.mono() ? 0.45f : 0.25f), acc);
        float[] knobs = twoKnobs ? new float[]{a0, a1} : new float[]{a1};
        for (float k : knobs) {
            if (hv > 0.01f) R2D.glow(ctx, k - 8, ty + th / 2f - 8, 16, 16, R2D.withAlpha(acc, 0.35f * hv));
            R2D.circle(ctx, k, ty + th / 2f, 3.8f + 0.8f * hv, 0xFFFFFFFF);
            R2D.circle(ctx, k, ty + th / 2f, 1.4f, 0xFF0A0A0C);
        }
    }

    // ------------------------------------------------------------------ lista rozwijana (nakładka)

    private void drawDropdown(DrawContext ctx) {
        if (drop == null) return;
        if (page != Page.MODULES || shown == null || selected == null) { drop = null; return; }
        float e = dropAnim.animate(1);
        float ih = 15, fullH = drop.modes.size() * ih + 6;
        float y = dropY + 2, x = dropX, w = dropW;
        boolean up = y + fullH > y0 + H - 6;
        float h = fullH * Anim.easeOutCubic(e);
        float top = up ? dropY - 19 - h : y;
        float pa = R2D.alpha; R2D.alpha = pa * Anim.clamp01(e * 2);
        R2D.shadow(ctx, x, top + 3, w, h, 14, 0x99000000);
        R2D.roundRectV(ctx, x, top, w, h, 6, 0xFC1A1A1F, 0xFE121215);
        R2D.roundOutline(ctx, x, top, w, h, 6, 0.6f, 0x2AFFFFFF);
        clip(ctx, x, top, w, h);
        float iy = (up ? top + h - fullH : top) + 3;
        for (String md : drop.modes) {
            boolean act = drop.get().equals(md);
            float hv = hover("ddi" + dropKey + md, x + 3, iy, w - 6, ih);
            if (act || hv > 0.01f) R2D.roundRect(ctx, x + 3, iy, w - 6, ih, 4.5f, act ? R2D.withAlpha(Theme.accent(), 0.9f) : R2D.withAlpha(0xFFFFFFFF, 0.07f * hv));
            int fg = act ? Theme.onAccent() : R2D.blend(DIM, TEXT, hv);
            Fonts.drawV(ctx, md, x + 9 + 2 * hv, iy + ih / 2f + 0.5f, fg, act ? Font.BOLD : Font.SEMIBOLD, 0.54f);
            if (act) R2D.icon(ctx, "l_check", x + w - 15, iy + 4, 7, fg);
            final Setting.Mode dm = drop;
            hit(x + 3, iy, w - 6, ih, b -> { if (b == 0) { dm.set(md); Modules.save(); drop = null; click(); } });
            iy += ih;
        }
        unclip(ctx);
        R2D.alpha = pa;
        dropRect = new float[]{x, top, w, h};
    }
    private float[] dropRect;

    // ------------------------------------------------------------------ color picker

    private void openPicker(Setting.Color c) {
        picking = c;
        drop = null;
        int v = c.get();
        pickAlpha = ((v >>> 24) & 0xFF) / 255f;
        pickHSV = rgbToHsv(v);
        anim("pickOpen").snap(0);
    }

    private void drawColorPicker(DrawContext ctx) {
        if (picking == null) return;
        if (page != Page.MODULES || selected == null) { picking = null; return; }
        float e = anim("pickOpen").animate(1);
        float pw = 158, ph = 150;
        float px = Math.max(x0 + 8, Math.min(x0 + W - pw - 8, pickAnchorX - pw + 26));
        float py = pickAnchorY + 4;
        if (py + ph > y0 + H - 6) py = pickAnchorY - 20 - ph;
        py = Math.max(y0 + 6, py);
        pickX = px; pickY = py;
        float pa = R2D.alpha; R2D.alpha = pa * Anim.clamp01(e * 1.5f);
        py += (1 - Anim.easeOutCubic(e)) * 6;
        R2D.shadow(ctx, px, py + 4, pw, ph, 20, 0xCC000000);
        R2D.roundRectV(ctx, px, py, pw, ph, 10, 0xFC17171B, 0xFE0E0E11);
        R2D.roundOutline(ctx, px, py, pw, ph, 10, 0.6f, 0x30FFFFFF);
        Fonts.drawV(ctx, picking.name, px + 10, py + 11, TEXT, Font.BOLD, 0.62f);
        float cbx = px + pw - 10 - 12, cby = py + 5;
        float ch = hover("pickClose", cbx, cby, 12, 12);
        R2D.roundRect(ctx, cbx, cby, 12, 12, 6, R2D.blend(0x14FFFFFF, 0x30FFFFFF, ch));
        R2D.icon(ctx, "l_x", cbx + 3, cby + 3, 6, TEXT);
        hit(cbx, cby, 12, 12, b -> picking = null);
        float sq = PICK_SQ, sqx = px + 10, sqy = py + 22;
        pickSqX = sqx; pickSqY = sqy;
        drawSVSquare(ctx, sqx, sqy, sq, pickHSV[0]);
        float dotx = sqx + pickHSV[1] * sq, doty = sqy + (1 - pickHSV[2]) * sq;
        R2D.circle(ctx, dotx, doty, 3.6f, 0xFFFFFFFF); R2D.circle(ctx, dotx, doty, 2.3f, hsvToRgb(pickHSV[0], pickHSV[1], pickHSV[2], 1));
        hit(sqx, sqy, sq, sq, b -> { if (b == 0) { pickDrag = 1; updatePick(); } });
        float hbx = sqx + sq + 8, hbw = 10;
        drawHueBar(ctx, hbx, sqy, hbw, sq);
        R2D.roundRect(ctx, hbx - 1, sqy + pickHSV[0] * sq - 1.5f, hbw + 2, 3, 1.5f, 0xFFFFFFFF);
        hit(hbx, sqy, hbw, sq, b -> { if (b == 0) { pickDrag = 2; updatePick(); } });
        float abx = hbx + hbw + 7;
        int solid = hsvToRgb(pickHSV[0], pickHSV[1], pickHSV[2], 1);
        R2D.roundRect(ctx, abx, sqy, hbw, sq, 3, 0xFF2A2A2E);
        R2D.gradientV(ctx, abx, sqy, hbw, sq, solid, solid & 0x00FFFFFF);
        R2D.roundOutline(ctx, abx, sqy, hbw, sq, 3, 0.5f, 0x22FFFFFF);
        R2D.roundRect(ctx, abx - 1, sqy + (1 - pickAlpha) * sq - 1.5f, hbw + 2, 3, 1.5f, 0xFFFFFFFF);
        hit(abx, sqy, hbw, sq, b -> { if (b == 0) { pickDrag = 3; updatePick(); } });
        int col = hsvToRgb(pickHSV[0], pickHSV[1], pickHSV[2], pickAlpha);
        float pry = sqy + sq + 7;
        R2D.roundRect(ctx, px + 10, pry, 20, 12, 4, col);
        R2D.roundOutline(ctx, px + 10, pry, 20, 12, 4, 0.5f, 0x33FFFFFF);
        Fonts.drawV(ctx, String.format("#%08X", col), px + 36, pry + 6.5f, DIM, Font.BOLD, 0.54f);
        // szybkie kolory (biało-czarne)
        int[] quick = {0xFFFFFFFF, 0xFFB8B8C0, 0xFF6E6E78, 0xFF2A2A30};
        float qx = px + pw - 10 - quick.length * 13;
        for (int qc : quick) {
            float qh = hover("pq" + qc, qx, pry, 10, 12);
            R2D.roundRect(ctx, qx, pry + 1, 10, 10, 3, qc);
            R2D.roundOutline(ctx, qx, pry + 1, 10, 10, 3, 0.5f, R2D.blend(0x30FFFFFF, 0xAAFFFFFF, qh));
            hit(qx, pry, 10, 12, b -> { if (b == 0) { pickHSV = rgbToHsv(qc); pickAlpha = 1; } });
            qx += 13;
        }
        picking.set(col);
        R2D.alpha = pa;
    }

    private void drawSVSquare(DrawContext ctx, float x, float y, float s, float hue) {
        R2D.rect(ctx, x, y, s, s, hsvToRgb(hue, 1, 1, 1));
        R2D.gradientH(ctx, x, y, s, s, 0xFFFFFFFF, 0x00FFFFFF);
        R2D.gradientV(ctx, x, y, s, s, 0x00000000, 0xFF000000);
        R2D.roundOutline(ctx, x, y, s, s, 2, 0.6f, 0x22FFFFFF);
    }
    private void drawHueBar(DrawContext ctx, float x, float y, float w, float h) {
        int[] cols = {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
        float seg = h / 6f;
        for (int i = 0; i < 6; i++) R2D.gradientV(ctx, x, y + i * seg, w, seg, cols[i], cols[i + 1]);
        R2D.roundOutline(ctx, x, y, w, h, 2, 0.5f, 0x22FFFFFF);
    }

    private static float[] rgbToHsv(int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f, g = ((argb >> 8) & 0xFF) / 255f, b = (argb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float h = 0;
        if (d != 0) {
            if (max == r) h = ((g - b) / d) % 6;
            else if (max == g) h = (b - r) / d + 2;
            else h = (r - g) / d + 4;
            h /= 6; if (h < 0) h += 1;
        }
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }
    private static int hsvToRgb(float h, float s, float v, float a) {
        float r, g, b;
        int i = (int) (h * 6) % 6; float f = h * 6 - (int) (h * 6);
        float p = v * (1 - s), q = v * (1 - f * s), tt = v * (1 - (1 - f) * s);
        switch (i) { case 0 -> { r = v; g = tt; b = p; } case 1 -> { r = q; g = v; b = p; } case 2 -> { r = p; g = v; b = tt; }
            case 3 -> { r = p; g = q; b = v; } case 4 -> { r = tt; g = p; b = v; } default -> { r = v; g = p; b = q; } }
        return (Math.round(a * 255) << 24) | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    // ------------------------------------------------------------------ strona: Wygląd

    private void drawAppearance(DrawContext ctx) {
        float slide = slideIn(pageTime, 14), fade = fadeIn(pageTime);
        drawHeader(ctx, t("Wygląd", "Appearance"), t("Kolory, styl okna, animacje i efekty", "Colors, window style, motion and effects"), slide);
        float pa = R2D.alpha; R2D.alpha = pa * fade;
        float top = y0 + 50, areaH = y0 + H - 10 - top;
        float gap = 8, colW = (cx1 - cx0 - gap) / 2f;
        clip(ctx, cx0 - 4, top - 2, cx1 - cx0 + 8, areaH + 4);
        float lx = cx0 + slide, rx = cx0 + colW + gap + slide;
        float ly = top - appScroll, ry = top - appScroll;

        // ---- lewa kolumna
        ly = card(ctx, lx, ly, colW, "l_palette", t("Kolor akcentu", "Accent color"), (x, y, w) -> {
            float r = 6.2f, gapC = (w - Theme.ACCENTS.length * r * 2) / (Theme.ACCENTS.length - 1);
            gapC = Math.max(4, Math.min(8, gapC));
            float sx = x, sy = y;
            for (int i = 0; i < Theme.ACCENTS.length; i++) {
                if (sx + r * 2 > x + w + 0.5f) { sx = x; sy += r * 2 + gapC; }
                float ccx = sx + r, ccy = sy + r;
                boolean sel = Theme.accentIndex == i;
                float hv = hover("ac" + i, ccx - r, ccy - r, r * 2, r * 2), sa = state("acs" + i, 16, sel ? 1 : 0);
                if (sa > 0.01f) { R2D.circle(ctx, ccx, ccy, r + 2.4f * sa, 0xFFFFFFFF); R2D.circle(ctx, ccx, ccy, r + 1.1f * sa, 0xFF101013); }
                R2D.circle(ctx, ccx, ccy, r + 0.8f * hv, Theme.ACCENTS[i]);
                if (sel) R2D.icon(ctx, "l_check", ccx - 4, ccy - 4, 8, Theme.contrast(Theme.ACCENTS[i]));
                if (hv > 0.6f) tipText = pl() ? Theme.ACCENT_PL[i] : Theme.ACCENT_EN[i];
                final int fi = i;
                hit(ccx - r, ccy - r, r * 2, r * 2, b -> { if (b == 0) { Theme.accentIndex = fi; Theme.save(); click(); } });
                sx += r * 2 + gapC;
            }
            return sy + r * 2 + 2 - y;
        });
        ly = card(ctx, lx, ly, colW, "l_app_window", t("Styl okna", "Window style"), (x, y, w) ->
                segmented(ctx, "style", x, y, w, new String[]{t("Pełny", "Solid"), t("Szkło", "Glass"), t("Minimal", "Minimal")},
                        Arrays.asList(Theme.STYLES).indexOf(Theme.style), i -> { Theme.style = Theme.STYLES[i]; Theme.save(); }));
        ly = card(ctx, lx, ly, colW, "l_scaling", t("Wymiary", "Size"), (x, y, w) -> {
            float h = 0;
            h += appSlider(ctx, "scale", x, y + h, w, t("Skala okna", "Window scale"), Math.round(Theme.scale * 100) + " %", (Theme.scale - 0.8f) / 0.6f, f -> Theme.scale = Math.round((0.8f + 0.6f * f) * 20) / 20f);
            h += appSlider(ctx, "radius", x, y + h, w, t("Zaokrąglenie", "Roundness"), Math.round(Theme.radius * 100) + " %", (Theme.radius - 0.3f) / 1.3f, f -> Theme.radius = Math.round((0.3f + 1.3f * f) * 20) / 20f);
            h += appSlider(ctx, "dim", x, y + h, w, t("Przyciemnienie tła", "Background dim"), Math.round(Theme.dim * 100) + " %", Theme.dim / 0.8f, f -> Theme.dim = Math.round(0.8f * f * 20) / 20f);
            return h - 4;
        });

        // ---- prawa kolumna
        ry = card(ctx, rx, ry, colW, "l_activity", t("Animacje", "Motion"), (x, y, w) -> {
            float h = 0;
            Fonts.drawV(ctx, t("Szybkość", "Speed"), x, y + 5, DIM, Font.SEMIBOLD, 0.56f); h += 11;
            h += segmented(ctx, "motion", x, y + h, w, new String[]{t("Wolne", "Slow"), t("Normalne", "Normal"), t("Szybkie", "Fast"), t("Wył.", "Off")},
                    Arrays.asList(Theme.MOTIONS).indexOf(Theme.motion), i -> { Theme.motion = Theme.MOTIONS[i]; Theme.save(); }) + 7;
            Fonts.drawV(ctx, t("Krzywa", "Easing"), x, y + h + 5, DIM, Font.SEMIBOLD, 0.56f); h += 11;
            h += segmented(ctx, "easing", x, y + h, w, new String[]{t("Płynna", "Smooth"), t("Sprężysta", "Spring"), t("Liniowa", "Linear")},
                    Arrays.asList(Theme.EASINGS).indexOf(Theme.easing), i -> { Theme.easing = Theme.EASINGS[i]; Theme.save(); });
            return h;
        });
        ry = card(ctx, rx, ry, colW, "l_sparkles", t("Efekty", "Effects"), (x, y, w) -> {
            float yy = y;
            yy = appToggle(ctx, "particles", x, yy, w, t("Cząsteczki w tle", "Background particles"), Theme.particles, () -> Theme.particles = !Theme.particles);
            yy = appToggle(ctx, "border", x, yy, w, t("Animowana ramka", "Animated border"), Theme.border, () -> Theme.border = !Theme.border);
            yy = appToggle(ctx, "vignette", x, yy, w, t("Winieta", "Vignette"), Theme.vignette, () -> Theme.vignette = !Theme.vignette);
            yy = appToggle(ctx, "blur", x, yy, w, t("Rozmycie świata", "World blur"), Theme.blur, () -> Theme.blur = !Theme.blur);
            yy = appToggle(ctx, "shadow", x, yy, w, t("Cień okna", "Window shadow"), Theme.shadow, () -> Theme.shadow = !Theme.shadow);
            return yy - y - 2;
        });
        ry = card(ctx, rx, ry, colW, "l_sliders_horizontal", t("Interfejs", "Interface"), (x, y, w) -> {
            float yy = y;
            yy = appToggle(ctx, "desc", x, yy, w, t("Opisy na liście", "Descriptions in list"), Theme.descriptions, () -> Theme.descriptions = !Theme.descriptions);
            yy = appToggle(ctx, "keys", x, yy, w, t("Klawisze na liście", "Keys in list"), Theme.keysInList, () -> Theme.keysInList = !Theme.keysInList);
            yy = appToggle(ctx, "tips", x, yy, w, t("Podpowiedzi", "Tooltips"), Theme.tooltips, () -> Theme.tooltips = !Theme.tooltips);
            yy = appToggle(ctx, "toasts", x, yy, w, t("Powiadomienia", "Notifications"), Theme.toasts, () -> Theme.toasts = !Theme.toasts);
            yy = appToggle(ctx, "sounds", x, yy, w, t("Dźwięki", "Sounds"), Theme.sounds, () -> Theme.sounds = !Theme.sounds);
            return yy - y - 2;
        });
        float end = Math.max(ly, ry);
        appMax = Math.max(0, end + appScroll - top - areaH);
        appScrollT = Math.max(0, Math.min(appMax, appScrollT));
        appScroll += (appScrollT - appScroll) * 0.3f;
        unclip(ctx);
        R2D.alpha = pa;
    }

    private interface CardBody { float draw(float x, float y, float w); }

    private float card(DrawContext ctx, float x, float y, float w, String icon, String title, CardBody body) {
        Float hPrev = cardH.get(title);
        float h = hPrev == null ? 60 : hPrev;
        R2D.roundRect(ctx, x, y, w, h, 9, cardBg(0));
        R2D.roundOutline(ctx, x, y, w, h, 9, 0.5f, 0x14FFFFFF);
        R2D.icon(ctx, icon, x + 9, y + 8, 9, TEXT);
        Fonts.drawV(ctx, title, x + 23, y + 13, TEXT, Font.BOLD, 0.66f);
        float bh = body.draw(x + 9, y + 25, w - 18);
        float total = 25 + bh + 10;
        cardH.put(title, total);
        return y + total + 8;
    }
    private final Map<String, Float> cardH = new HashMap<>();

    private float segmented(DrawContext ctx, String key, float x, float y, float w, String[] labels, int sel, IntConsumer pick) {
        float h = 15, sw = w / labels.length;
        R2D.roundRect(ctx, x, y, w, h, 5.5f, 0x0EFFFFFF);
        R2D.roundOutline(ctx, x, y, w, h, 5.5f, 0.5f, 0x14FFFFFF);
        float ax = pos("aseg" + key, x + Math.max(0, sel) * sw);
        R2D.roundRectV(ctx, ax + 1.5f, y + 1.5f, sw - 3, h - 3, 4.5f, Theme.accentTop(), Theme.accentBottom());
        for (int i = 0; i < labels.length; i++) {
            float bx = x + i * sw;
            float near = Anim.clamp01(1 - Math.abs(ax - bx) / sw);
            float hv = hover("asg" + key + i, bx, y, sw, h);
            String l = Fonts.trim(labels[i], Font.BOLD, 0.5f, sw - 4);
            Fonts.drawV(ctx, l, bx + (sw - Fonts.width(l, Font.BOLD, 0.5f)) / 2f, y + h / 2f + 0.5f, R2D.blend(R2D.blend(FAINT, TEXT, hv), Theme.onAccent(), near), Font.BOLD, 0.5f);
            final int fi = i;
            hit(bx, y, sw, h, b -> { if (b == 0) { pick.accept(fi); click(); } });
        }
        return h;
    }

    private float appToggle(DrawContext ctx, String k, float x, float y, float w, String label, boolean val, Runnable flip) {
        float hv = hover("at" + k, x, y, w, 17), on = state("av" + k, 18, val ? 1 : 0);
        Fonts.drawV(ctx, label, x, y + 8.5f, R2D.blend(DIM, TEXT, Math.max(hv, on)), Font.SEMIBOLD, 0.58f);
        Ui.toggle(ctx, x + w - 18, y + 3.5f, on, hv);
        hit(x, y, w, 17, b -> { if (b == 0) { flip.run(); Theme.save(); click(); } });
        return y + 18;
    }

    private float appSlider(DrawContext ctx, String k, float x, float y, float w, String label, String val, float frac, Consumer<Float> set) {
        float hv = anim("ash" + k).animate(over(x, y, w, 24) || k.equals(appDragKey) ? 1 : 0);
        Fonts.drawV(ctx, label, x, y + 6, R2D.blend(DIM, TEXT, hv), Font.SEMIBOLD, 0.58f);
        valueChip(ctx, val, x + w, y + 1, hv);
        float f = state("asv" + k, 22, Anim.clamp01(frac));
        track(ctx, x, y + 17, w, 0, f, hv, false);
        final float fx = x, fw = w;
        hit(x - 4, y, w + 8, 24, b -> { if (b == 0) { appDragKey = k; dragging = DUMMY; dragApply = () -> set.accept(Anim.clamp01((mx - fx) / fw)); dragApply.run(); } });
        return 27;
    }
    private String appDragKey;
    private static final Object DUMMY = new Object();

    // ------------------------------------------------------------------ strona: Konfiguracje

    private void drawConfigs(DrawContext ctx) {
        float slide = slideIn(pageTime, 14), fade = fadeIn(pageTime);
        List<Modules.Config> list = Modules.configs();
        drawHeader(ctx, t("Konfiguracje", "Configs"), list.size() + t(" zapisanych · aktywna: ", " saved · active: ") + Theme.config, slide);
        float bw = 92, bx = cx1 - bw, by = y0 + 13;
        button(ctx, bx, by, bw, 18, "l_plus", t("Zapisz nową", "Save new"), true, hover("cfgNew", bx, by, bw, 18), true);
        hit(bx, by, bw, 18, b -> { if (b == 0) { String n = Modules.newConfigName(); Modules.saveConfig(n); Theme.config = n; Theme.save(); click(); } });
        float fbx = bx - 6 - 18;
        button(ctx, fbx, by, 18, 18, "l_folder_open", null, false, hover("cfgDir", fbx, by, 18, 18), true);
        if (over(fbx, by, 18, 18)) tipText = t("Otwórz folder", "Open folder");
        hit(fbx, by, 18, 18, b -> { if (b == 0) { try { java.nio.file.Files.createDirectories(Modules.configDir()); Util.getOperatingSystem().open(Modules.configDir().toFile()); } catch (Exception ignored) {} click(); } });
        float pa = R2D.alpha; R2D.alpha = pa * fade;
        float top = y0 + 50, areaH = y0 + H - 10 - top, rh = 32, gap = 5;
        float x = cx0 + slide, w = cx1 - cx0;
        clip(ctx, cx0 - 4, top - 2, w + 8, areaH + 4);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pl() ? "d MMM, HH:mm" : "MMM d, HH:mm", pl() ? Locale.forLanguageTag("pl-PL") : Locale.ENGLISH);
        float y = top - appScroll;
        for (Modules.Config c : list) {
            boolean active = c.name().equals(Theme.config);
            float hv = hover("cf" + c.name(), x, y, w, rh);
            R2D.roundRect(ctx, x, y, w, rh, 8, cardBg(hv));
            R2D.roundOutline(ctx, x, y, w, rh, 8, 0.55f, active ? R2D.withAlpha(Theme.accent(), 0.6f) : 0x12FFFFFF);
            R2D.roundRectV(ctx, x + 7, y + 7, 18, 18, 5.5f, active ? Theme.accentTop() : 0xFF1D1D22, active ? Theme.accentBottom() : 0xFF131317);
            R2D.icon(ctx, "l_file_cog", x + 11.5f, y + 11.5f, 9, active ? Theme.onAccent() : 0xFFCFCFD6);
            Fonts.draw(ctx, c.name(), x + 32, y + 7.5f, TEXT, Font.BOLD, 0.68f);
            Fonts.draw(ctx, (active ? t("Aktywna · ", "Active · ") : "") + t("zapisano ", "saved ") + fmt.format(Instant.ofEpochMilli(c.modified()).atZone(ZoneId.systemDefault())), x + 32, y + 18, FAINT, Font.REGULAR, 0.5f);
            float rx = x + w - 7;
            boolean armed = c.name().equals(delName) && System.currentTimeMillis() - delArm < 3000;
            float dw = armed ? Fonts.width(t("Na pewno?", "Sure?"), Font.BOLD, 0.54f) + 14 : 18;
            rx -= dw;
            float dh = hover("cd" + c.name(), rx, y + 7, dw, 18);
            if (armed) { R2D.roundRect(ctx, rx, y + 7, dw, 18, 9, R2D.blend(0xFFDC2626, 0xFFEF4444, dh)); Fonts.drawV(ctx, t("Na pewno?", "Sure?"), rx + 7, y + 16.5f, 0xFFFFFFFF, Font.BOLD, 0.54f); }
            else button(ctx, rx, y + 7, dw, 18, "l_trash_2", null, false, dh, true);
            hit(rx, y + 7, dw, 18, b -> { if (b != 0) return; if (armed) { Modules.deleteConfig(c.name()); delName = null; if (Theme.config.equals(c.name())) { Theme.config = "default"; Theme.save(); } } else { delName = c.name(); delArm = System.currentTimeMillis(); } click(); });
            rx -= 23; button(ctx, rx, y + 7, 18, 18, "l_save", null, false, hover("cs" + c.name(), rx, y + 7, 18, 18), true);
            hit(rx, y + 7, 18, 18, b -> { if (b == 0) { Modules.saveConfig(c.name()); Theme.config = c.name(); Theme.save(); click(); } });
            float lw = Fonts.width(t("Wczytaj", "Load"), Font.BOLD, 0.6f) + 26; rx -= lw + 5;
            button(ctx, rx, y + 7, lw, 18, "l_download", t("Wczytaj", "Load"), !active, hover("cl" + c.name(), rx, y + 7, lw, 18), true);
            hit(rx, y + 7, lw, 18, b -> { if (b == 0) { Modules.loadConfig(c.name()); Theme.config = c.name(); Theme.save(); click(); } });
            y += rh + gap;
        }
        if (list.isEmpty()) {
            R2D.icon(ctx, "l_folder", x + w / 2f - 9, top + areaH / 2f - 26, 18, FAINT);
            Fonts.drawCentered(ctx, t("Brak konfiguracji – kliknij „Zapisz nową”", "No configs – click \"Save new\""), x + w / 2f, top + areaH / 2f, DIM, Font.SEMIBOLD, 0.64f);
        }
        appMax = Math.max(0, list.size() * (rh + gap) - gap - areaH);
        appScrollT = Math.max(0, Math.min(appMax, appScrollT));
        appScroll += (appScrollT - appScroll) * 0.3f;
        unclip(ctx);
        R2D.alpha = pa;
    }
    private String delName; private long delArm;

    // ------------------------------------------------------------------ strona: Znajomi

    private void drawFriends(DrawContext ctx) {
        float slide = slideIn(pageTime, 14), fade = fadeIn(pageTime);
        List<String> list = Friends.all();
        drawHeader(ctx, t("Znajomi", "Friends"), list.size() + (pl() ? " znajomych · nie są celem modułów" : " friends · never targeted"), slide);
        float pa = R2D.alpha; R2D.alpha = pa * fade;
        float x = cx0 + slide, w = cx1 - cx0;
        float top = y0 + 50;
        float bw = 74, fw = w - bw - 6;
        field(ctx, 2, x, top, fw, 18, friendInput, t("Wpisz nick…", "Type a name…"), "l_user_plus");
        button(ctx, x + fw + 6, top, bw, 18, "l_plus", t("Dodaj", "Add"), true, hover("frAdd", x + fw + 6, top, bw, 18), !friendInput.isBlank());
        hit(x + fw + 6, top, bw, 18, b -> addFriend());
        top += 26;
        float areaH = y0 + H - 10 - top, rh = 26, gap = 5;
        int cols = 2; float cw = (w - gap) / cols;
        clip(ctx, cx0 - 4, top - 2, w + 8, areaH + 4);
        for (int i = 0; i < list.size(); i++) {
            String f = list.get(i);
            float cx = x + (i % cols) * (cw + gap), cy = top + (i / cols) * (rh + gap) - appScroll;
            float hv = hover("fr" + f, cx, cy, cw, rh);
            R2D.roundRect(ctx, cx, cy, cw, rh, 8, cardBg(hv));
            R2D.roundOutline(ctx, cx, cy, cw, rh, 8, 0.5f, 0x12FFFFFF);
            monogram(ctx, cx + 5, cy + 4, 18, f);
            Fonts.drawV(ctx, f, cx + 29, cy + rh / 2f + 0.5f, TEXT, Font.BOLD, 0.64f);
            float rx = cx + cw - 22; float dh = hover("fd" + f, rx, cy + 5, 16, 16);
            R2D.roundRect(ctx, rx, cy + 5, 16, 16, 8, R2D.blend(0x00FFFFFF, 0xFFDC2626, dh));
            R2D.icon(ctx, "l_trash_2", rx + 4, cy + 9, 8, R2D.blend(R2D.withAlpha(0xFFFFFFFF, 0.3f + 0.5f * hv), 0xFFFFFFFF, dh));
            hit(rx, cy + 5, 16, 16, b -> { if (b == 0) { Friends.remove(f); click(); } });
        }
        if (list.isEmpty()) {
            R2D.icon(ctx, "l_users", x + w / 2f - 9, top + areaH / 2f - 26, 18, FAINT);
            Fonts.drawCentered(ctx, t("Brak znajomych – wpisz nick i kliknij „Dodaj”", "No friends – type a name and click \"Add\""), x + w / 2f, top + areaH / 2f, DIM, Font.SEMIBOLD, 0.62f);
        }
        int rows = (list.size() + cols - 1) / cols;
        appMax = Math.max(0, rows * (rh + gap) - gap - areaH);
        appScrollT = Math.max(0, Math.min(appMax, appScrollT));
        appScroll += (appScrollT - appScroll) * 0.3f;
        unclip(ctx);
        R2D.alpha = pa;
    }
    private void addFriend() { if (Friends.add(friendInput)) { friendInput = ""; click(); } }

    // ------------------------------------------------------------------ powiadomienia, podpowiedzi, skróty

    private void drawToasts(DrawContext ctx, long now) {
        TOASTS.removeIf(tt -> now - tt.time() > 2200);
        float y = y0 + H - 12;
        for (int i = TOASTS.size() - 1; i >= 0; i--) {
            Toast tt = TOASTS.get(i);
            float age = now - tt.time();
            float in = Anim.enter(age / (320f / Theme.motionSpeed())), out = Anim.easeInCubic((age - 1800) / 400f);
            float e = Anim.clamp01(in) * (1 - out);
            if (e <= 0.01f) continue;
            String sub = tt.on() ? t("włączony", "enabled") : t("wyłączony", "disabled");
            float w = Math.max(Fonts.width(tt.title(), Font.BOLD, 0.62f), Fonts.width(sub, Font.REGULAR, 0.5f)) + 40, h = 24;
            float x = cx0 + 2 - (1 - in) * 30;
            y -= (h + 5) * e;
            float pa = R2D.alpha; R2D.alpha = pa * e;
            R2D.shadow(ctx, x, y + 2, w, h, 12, 0x99000000);
            R2D.roundRectV(ctx, x, y, w, h, 8, 0xFC1C1C21, 0xFE121215);
            R2D.roundOutline(ctx, x, y, w, h, 8, 0.6f, 0x2AFFFFFF);
            R2D.roundRectV(ctx, x + 5, y + 5, 14, 14, 4.5f, tt.on() ? Theme.accentTop() : 0xFF26262B, tt.on() ? Theme.accentBottom() : 0xFF1A1A1F);
            R2D.icon(ctx, tt.icon(), x + 8, y + 8, 8, tt.on() ? Theme.onAccent() : DIM);
            Fonts.draw(ctx, tt.title(), x + 25, y + 4.5f, TEXT, Font.BOLD, 0.62f);
            Fonts.draw(ctx, sub, x + 25, y + 13.5f, tt.on() ? DIM : FAINT, Font.REGULAR, 0.5f);
            float life = Anim.clamp01(age / 2000f);
            R2D.roundRect(ctx, x + 6, y + h - 2, (w - 12) * (1 - life), 1, 0.5f, R2D.withAlpha(Theme.accent(), 0.5f));
            R2D.alpha = pa;
        }
    }

    private void drawTooltip(DrawContext ctx, long now) {
        String txt = tipText;
        tipText = null;
        if (!tipSeen) tipModule = null;
        tipSeen = false;
        if (!Theme.tooltips) return;
        if (txt == null && tipModule != null && page == Page.MODULES && now - tipStart > 450) {
            boolean descShown = Theme.descriptions && panel.raw() < 0.3f;
            if (!descShown) txt = tipModule.description();
        }
        if (txt == null || dragging != null) { anim("tipA").snap(0); return; }
        float ta = anim("tipA").animate(1);
        float w = Fonts.width(txt, Font.SEMIBOLD, 0.54f) + 12, h = 15;
        float x = Math.min(x0 + W - w - 4, mx + 9), y = my + 12;
        if (y + h > y0 + H + 20) y = my - h - 6;
        float pa = R2D.alpha; R2D.alpha = pa * ta;
        R2D.shadow(ctx, x, y + 2, w, h, 10, 0x99000000);
        R2D.roundRect(ctx, x, y, w, h, 5, 0xFC1F1F24);
        R2D.roundOutline(ctx, x, y, w, h, 5, 0.5f, 0x30FFFFFF);
        Fonts.drawV(ctx, txt, x + 6, y + h / 2f + 0.5f, TEXT, Font.SEMIBOLD, 0.54f);
        R2D.alpha = pa;
    }

    private void drawHints(DrawContext ctx) {
        float hy = y0 + H + 8;
        if (hy + 12 > toLocalY(height) - 2) return;
        String[][] tips = page == Page.MODULES
                ? new String[][]{{"LPM", t("wybierz", "select")}, {"PPM", t("ustawienia", "settings")}, {"ŚPM", t("klawisz", "keybind")}, {"Esc", t("zamknij", "close")}}
                : new String[][]{{"Esc", t("zamknij", "close")}};
        float tw = 0;
        for (String[] tp : tips) tw += Math.max(12, Fonts.width(tp[0], Font.BOLD, 0.52f) + 8) + 4 + Fonts.width(tp[1], Font.REGULAR, 0.54f) + 11;
        float tx = x0 + W / 2f - (tw - 11) / 2f;
        for (String[] tp : tips) {
            tx += keyCap(ctx, tp[0], tx, hy) + 4;
            Fonts.drawV(ctx, tp[1], tx, hy + 6, 0x88FFFFFF, Font.REGULAR, 0.54f);
            tx += Fonts.width(tp[1], Font.REGULAR, 0.54f) + 11;
        }
    }

    // ------------------------------------------------------------------ pole tekstowe

    private void field(DrawContext ctx, int which, float x, float y, float w, float h, String text, String placeholder, String icon) {
        boolean f = focus == which;
        float fa = anim("field" + which).animate(f || !text.isEmpty() ? 1 : 0), hv = hover("fieldh" + which, x, y, w, h);
        R2D.roundRect(ctx, x, y, w, h, 6, R2D.blend(0x0CFFFFFF, 0x16FFFFFF, Math.max(fa, hv * 0.6f)));
        R2D.roundOutline(ctx, x, y, w, h, 6, 0.55f, R2D.blend(0x16FFFFFF, R2D.withAlpha(Theme.accent(), 0.8f), f ? 1 : fa * 0.35f));
        R2D.icon(ctx, icon, x + 6, y + h / 2f - 4, 8, R2D.blend(FAINT, TEXT, fa));
        String shownTxt = text.isEmpty() ? placeholder : text;
        Fonts.drawV(ctx, Fonts.trim(shownTxt, Font.REGULAR, 0.58f, w - 34), x + 18, y + h / 2f + 0.5f, text.isEmpty() ? FAINT : TEXT, Font.REGULAR, 0.58f);
        if (f && (System.currentTimeMillis() / 500) % 2 == 0) {
            float cx = x + 18 + (text.isEmpty() ? 0 : Fonts.width(Fonts.trim(text, Font.REGULAR, 0.58f, w - 34), Font.REGULAR, 0.58f) + 1);
            R2D.rect(ctx, cx, y + 4.5f, 0.6f, h - 9, TEXT);
        }
        hit(x, y, w, h, b -> focus = which);
    }

    // ================================================================== wejście

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mx = (int) Math.floor(toLocalX(mouseX)); my = (int) Math.floor(toLocalY(mouseY));
        if (closeTime >= 0) return true;
        if (binding != null && button != 2) binding = null;
        int prevFocus = focus;
        focus = 0;
        // nakładki: klik poza nimi zamyka je
        if (drop != null && dropRect != null && !inRect(dropRect)) {
            boolean onOwner = mx >= dropX && mx < dropX + dropW && my >= dropY - 18 && my < dropY;
            if (!onOwner) { drop = null; return true; }
        }
        if (picking != null && !(mx >= pickX && mx < pickX + 158 && my >= pickY - 2 && my < pickY + 160)) {
            boolean onSwatch = Math.abs(mx - (pickAnchorX + 13)) < 16 && my >= pickAnchorY - 16 && my < pickAnchorY;
            if (!onSwatch) { picking = null; return true; }
        }
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.in(mx, my)) { h.action().accept(button); if (focus == 0 && prevFocus == 3 && editText != null) Modules.save(); return true; }
        }
        if (prevFocus == 3) Modules.save();
        return true;
    }

    private boolean inRect(float[] r) { return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]; }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dX, double dY) {
        mx = (int) Math.floor(toLocalX(mouseX)); my = (int) Math.floor(toLocalY(mouseY));
        if (pickDrag > 0 && picking != null) updatePick();
        if (dragApply != null) dragApply.run();
        return true;
    }

    private void updatePick() {
        float sq = PICK_SQ;
        if (pickDrag == 1) { pickHSV[1] = Anim.clamp01((mx - pickSqX) / sq); pickHSV[2] = 1 - Anim.clamp01((my - pickSqY) / sq); }
        else if (pickDrag == 2) pickHSV[0] = Math.min(0.999f, Anim.clamp01((my - pickSqY) / sq));
        else if (pickDrag == 3) pickAlpha = 1 - Anim.clamp01((my - pickSqY) / sq);
    }
    private float pickSqX, pickSqY;

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) { if (dragApply != null) dragApply.run(); dragging = null; dragApply = null; appDragKey = null; Modules.save(); Theme.save(); }
        if (pickDrag > 0) { pickDrag = 0; if (picking != null) Modules.save(); }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hh, double v) {
        float lx = toLocalX(mouseX);
        if (drop != null) return true;
        if (page == Page.MODULES) {
            float panelX = cx1 - PW;
            if (selected != null && lx >= panelX) setScrollT = Math.max(0, Math.min(setMax, setScrollT - (float) v * 24));
            else listScrollT = Math.max(0, Math.min(listMax, listScrollT - (float) v * 26));
        } else appScrollT = Math.max(0, Math.min(appMax, appScrollT - (float) v * 26));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding != null) {
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) { binding.key = keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE ? -1 : keyCode; Modules.save(); }
            binding = null; click(); return true;
        }
        if (focus == 3 && editText != null) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !editText.get().isEmpty()) editText.set(editText.get().substring(0, editText.get().length() - 1));
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) { focus = 0; Modules.save(); }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (picking != null) picking = null;
            else if (drop != null) drop = null;
            else if (focus != 0 || !query.isEmpty()) { query = ""; focus = 0; listTime = System.currentTimeMillis(); }
            else close();
            return true;
        }
        if (focus == 1 && keyCode == GLFW.GLFW_KEY_BACKSPACE) { if (!query.isEmpty()) { query = query.substring(0, query.length() - 1); listTime = System.currentTimeMillis(); } return true; }
        if (focus == 2) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !friendInput.isEmpty()) friendInput = friendInput.substring(0, friendInput.length() - 1);
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) addFriend();
            return true;
        }
        if (WithClient.OPEN_GUI != null && WithClient.OPEN_GUI.matchesKey(keyCode, scanCode)) { close(); return true; }
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (binding != null || chr < ' ' || chr == 127) return true;
        if (focus == 3 && editText != null) { if (editText.get().length() < editText.maxLen) editText.set(editText.get() + chr); return true; }
        if (focus == 2) { if ((Character.isLetterOrDigit(chr) || chr == '_') && friendInput.length() < 16) friendInput += chr; return true; }
        if (page != Page.MODULES) return true;
        if (focus != 1 && !Character.isLetterOrDigit(chr)) return true;
        focus = 1;
        if (query.length() < 24) { query += chr; listScroll = listScrollT = 0; listTime = System.currentTimeMillis(); }
        return true;
    }

    @Override
    public void close() { if (closeTime < 0) { closeTime = System.currentTimeMillis(); Modules.save(); Theme.save(); } }
}
