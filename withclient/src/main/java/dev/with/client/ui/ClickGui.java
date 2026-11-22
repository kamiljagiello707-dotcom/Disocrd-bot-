package dev.with.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
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
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.IntConsumer;

import static dev.with.client.ui.Ui.*;

/**
 * Duże, trójkolumnowe ClickGUI WithClient: nawigacja (logo, kategorie, strony, profil) | lista modułów
 * (wyszukiwarka, przełączniki, wybór) | panel ustawień wybranego modułu (przełączniki, suwaki, tryby,
 * kolory z pickerem, klawisz). Dodatkowe strony: Wygląd, Konfiguracje, Znajomi. Moduły to atrapy z opcjami.
 */
public class ClickGui extends Screen {
    private enum Page { MODULES, APPEARANCE, CONFIGS, FRIENDS }

    private static Page page = Page.MODULES;
    private static Category category = Category.COMBAT;
    private static Module selected;

    private final long openTime = System.currentTimeMillis();
    private long closeTime = -1, listTime = System.currentTimeMillis();
    private final Map<String, Anim> anims = new HashMap<>();
    private record Hit(float x, float y, float w, float h, IntConsumer action) {
        boolean in(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; }
    }
    private final List<Hit> hits = new ArrayList<>();
    private int mx, my;
    private String query = "", friendInput = "";
    private int focus; // 0 nic, 1 szukaj, 2 znajomy
    private Module binding;
    private Object dragging; // Setting.Num podczas przeciągania suwaka
    private Runnable dragApply;
    private float listScroll, listScrollT, listMax, setScroll, setScrollT, setMax, appScroll, appScrollT, appMax;
    private float x0, y0, W, H, sc = 1;
    private float navR, listR; // prawe krawędzie kolumn
    // color picker
    private Setting.Color picking;
    private float[] pickHSV = new float[3];
    private int pickDrag; // 1 sv, 2 hue, 3 alpha
    private float pickAlpha = 1;
    private final Anim selAnim = new Anim(-1, 18), catAnim = new Anim(-1, 18);

    public ClickGui() { super(Text.literal("WithClient")); }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }

    private Anim anim(String k) { return anims.computeIfAbsent(k, s -> new Anim(0, 16)); }
    private float hover(String k, float x, float y, float w, float h) { return anim(k).animate(over(x, y, w, h) ? 1 : 0); }
    private float state(String k, float sp, float tgt) { return anims.computeIfAbsent(k, s -> new Anim(tgt, sp)).animate(tgt); }
    private boolean over(float x, float y, float w, float h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private void hit(float x, float y, float w, float h, IntConsumer a) { hits.add(new Hit(x, y, w, h, a)); }

    private void click() {
        if (client != null && Theme.sounds) client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.6f, 0.2f));
    }

    private List<Module> list() {
        if (!query.isEmpty()) {
            String q = query.toLowerCase(Locale.ROOT);
            return Modules.all().stream().filter(m -> m.name.toLowerCase(Locale.ROOT).contains(q) || m.description().toLowerCase(Locale.ROOT).contains(q)).toList();
        }
        return Modules.of(category);
    }

    private static String keyName(int key) {
        if (key < 0) return "";
        String s = InputUtil.fromKeyCode(key, 0).getLocalizedText().getString();
        return s.length() > 9 ? s.substring(0, 9) : s.toUpperCase(Locale.ROOT);
    }

    // ================================================================== render

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (client != null && client.world != null) { if (Theme.blur) applyBlur(delta); }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hits.clear();
        long now = System.currentTimeMillis();
        float a = closeTime < 0 ? Anim.easeOutCubic((now - openTime) / (260f / Theme.motionSpeed())) : 1 - Anim.easeInCubic((now - closeTime) / (170f / Theme.motionSpeed()));
        if (closeTime >= 0 && a <= 0) { if (client != null) client.setScreen(null); return; }
        renderBackground(ctx, mouseX, mouseY, delta);
        R2D.alpha = 1;
        R2D.rect(ctx, 0, 0, width, height, R2D.withAlpha(0xFF000000, Theme.dim * a));
        R2D.gradientV(ctx, 0, height * 0.55f, width, height * 0.45f, 0x00000000, R2D.withAlpha(0xFF000000, 0.3f * a));

        float s = Theme.scale;
        W = Math.min(880, width / s - 24);
        H = Math.min(540, height / s - 30);
        x0 = (width - W) / 2f;
        y0 = (height - H) / 2f;
        sc = s * (0.96f + 0.04f * a);
        float lx = toLocalX(mouseX), ly = toLocalY(mouseY);
        mx = (int) Math.floor(lx); my = (int) Math.floor(ly);
        if (dragApply != null) dragApply.run();

        ctx.getMatrices().push();
        ctx.getMatrices().translate(width / 2f, height / 2f, 0);
        ctx.getMatrices().scale(sc, sc, 1);
        ctx.getMatrices().translate(-width / 2f, -height / 2f, 0);
        R2D.alpha = a;

        // okno
        R2D.shadow(ctx, x0, y0 + 8, W, H, 34, 0xCC000000);
        R2D.glow(ctx, x0 - 50, y0 - 50, W + 100, H + 100, R2D.withAlpha(Theme.accent(), 0.06f));
        R2D.roundRectV(ctx, x0, y0, W, H, 16, 0xF5101013, 0xF808080A);
        R2D.roundOutline(ctx, x0, y0, W, H, 16, 0.8f, 0x30FFFFFF);
        R2D.gradientH(ctx, x0 + 24, y0 + 0.7f, (W - 48) / 2f, 0.7f, 0x00FFFFFF, R2D.withAlpha(Theme.accent(), 0.55f));
        R2D.gradientH(ctx, x0 + W / 2f, y0 + 0.7f, (W - 48) / 2f, 0.7f, R2D.withAlpha(Theme.accent(), 0.55f), 0x00FFFFFF);

        R2D.scissor(ctx, x0, y0, W, H);
        float nav = Math.min(188, W * 0.23f);
        navR = x0 + nav;
        drawNav(ctx, nav);
        switch (page) {
            case MODULES -> drawModulesPage(ctx);
            case APPEARANCE -> drawAppearance(ctx, navR, x0 + W - navR);
            case CONFIGS -> drawConfigs(ctx, navR, x0 + W - navR);
            case FRIENDS -> drawFriends(ctx, navR, x0 + W - navR);
        }
        R2D.endScissor(ctx);
        drawColorPicker(ctx);
        ctx.getMatrices().pop();

        // podpowiedzi pod oknem
        float hy = y0 + H + 9;
        if (hy + 12 < height / s) {
            R2D.alpha = a;
            String[][] tips = page == Page.MODULES
                    ? new String[][]{{"LPM", t("włącz", "toggle")}, {t("klik", "click"), t("ustawienia", "settings")}, {"MMB", t("klawisz", "keybind")}, {"Esc", t("zamknij", "close")}}
                    : new String[][]{{"Esc", t("zamknij", "close")}};
            float tw = 0;
            for (String[] tp : tips) tw += Math.max(12, Fonts.width(tp[0], Font.BOLD, 0.52f) + 8) + 4 + Fonts.width(tp[1], Font.REGULAR, 0.56f) + 12;
            float tx = x0 + W / 2f - (tw - 12) / 2f;
            for (String[] tp : tips) {
                tx += keyCap(ctx, tp[0], tx, hy) + 4;
                Fonts.drawV(ctx, tp[1], tx, hy + 6, 0x99FFFFFF, Font.REGULAR, 0.56f);
                tx += Fonts.width(tp[1], Font.REGULAR, 0.56f) + 12;
            }
        }
        R2D.alpha = 1;
    }

    private float toLocalX(double sx) { return width / 2f + ((float) sx - width / 2f) / sc; }
    private float toLocalY(double sy) { return height / 2f + ((float) sy - height / 2f) / sc; }

    // ------------------------------------------------------------------ nawigacja

    private void drawNav(DrawContext ctx, float nav) {
        float prev = R2D.alpha;
        R2D.rect(ctx, x0 + nav, y0 + 10, 0.5f, H - 20, 0x14FFFFFF);
        // logo
        float lx = x0 + 14, ly = y0 + 14;
        R2D.glow(ctx, lx - 4, ly - 4, 38, 38, R2D.withAlpha(Theme.accent(), 0.2f));
        R2D.roundRectV(ctx, lx, ly, 30, 30, 9, 0xFF1A1A1F, 0xFF0C0C0E);
        R2D.roundOutline(ctx, lx, ly, 30, 30, 9, 0.6f, 0x24FFFFFF);
        R2D.icon(ctx, "with_logo", lx + 4, ly + 4, 22, 0xFFFFFFFF);
        Fonts.draw(ctx, "WithClient", lx + 38, ly + 5, TEXT, Font.BOLD, 0.92f);
        Fonts.draw(ctx, "v" + WithClient.VERSION, lx + 38, ly + 17, FAINT, Font.SEMIBOLD, 0.6f);

        float y = y0 + 56;
        spaced(ctx, t("KATEGORIE", "CATEGORIES"), x0 + 16, y, FAINT, Font.BOLD, 0.48f, 1.4f); y += 12;
        Category[] cats = Category.values();
        int items = cats.length + 4; // +Wygląd, Konfiguracje, Znajomi (+1 nagłówek)
        float rh = Math.min(23, (y0 + H - 48 - y - 14) / items);
        float pillY = -1;
        List<float[]> rows = new ArrayList<>();
        for (int i = 0; i < cats.length; i++) { rows.add(new float[]{y, i}); y += rh; }
        y += 6;
        spaced(ctx, t("KLIENT", "CLIENT"), x0 + 16, y, FAINT, Font.BOLD, 0.48f, 1.4f); y += 12;
        float pStart = y;
        String[][] pages = {{"g_render", t("Wygląd", "Appearance")}, {"g_configs", t("Konfiguracje", "Configs")}, {"g_friends", t("Znajomi", "Friends")}};
        for (int i = 0; i < pages.length; i++) { rows.add(new float[]{y, cats.length + i}); y += rh; }

        int active = page == Page.MODULES ? (query.isEmpty() ? category.ordinal() : -1) : cats.length + (page == Page.APPEARANCE ? 0 : page == Page.CONFIGS ? 1 : 2);
        float px = x0 + 10, pw = nav - 18;
        // pigułka
        float ty = -1;
        for (float[] r : rows) if ((int) r[1] == active) ty = r[0];
        if (ty >= 0 && catAnim.raw() < 0) catAnim.snap(ty);
        float pa = anim("navPill").animate(active >= 0 ? 1 : 0);
        float cy = catAnim.animate(ty >= 0 ? ty : catAnim.target());
        if (pa > 0.01f) {
            R2D.alpha = prev * pa;
            R2D.glow(ctx, px - 6, cy - 4, pw + 12, rh, R2D.withAlpha(Theme.accent(), 0.14f));
            R2D.roundRectV(ctx, px, cy + 1, pw, rh - 4, 6, Theme.accentTop(), Theme.accentBottom());
            R2D.alpha = prev;
        }
        for (float[] r : rows) {
            int idx = (int) r[1]; float ry = r[0];
            boolean isCat = idx < cats.length;
            String icon = isCat ? cats[idx].icon : pages[idx - cats.length][0];
            String name = isCat ? cats[idx].title() : pages[idx - cats.length][1];
            float near = pa * Anim.clamp01(1 - Math.abs(cy - ry) / rh);
            float hv = anim("nav" + idx).animate(over(px, ry, pw, rh - 3) && idx != active ? 1 : 0);
            if (hv > 0.01f) R2D.roundRect(ctx, px, ry + 1, pw, rh - 4, 6, R2D.withAlpha(0xFFFFFFFF, 0.05f * hv));
            int fg = R2D.blend(R2D.blend(0xFFB4B4BC, TEXT, hv), Theme.onAccent(), near);
            R2D.icon(ctx, icon, px + 9, ry + (rh - 4) / 2f - 4.5f, 10, fg);
            Fonts.drawV(ctx, name, px + 26, ry + (rh - 4) / 2f + 0.5f, fg, near > 0.5f ? Font.BOLD : Font.SEMIBOLD, 0.72f);
            if (isCat) {
                long en = Modules.enabledIn(cats[idx]);
                if (en > 0) {
                    String c = String.valueOf(en);
                    Fonts.drawV(ctx, c, px + pw - 8 - Fonts.width(c, Font.BOLD, 0.52f), ry + (rh - 4) / 2f + 0.5f, R2D.blend(FAINT, Theme.onAccent(), near), Font.BOLD, 0.52f);
                }
            }
            final int fi = idx;
            hit(px, ry, pw, rh - 2, b -> {
                if (b != 0) return;
                click(); query = ""; focus = 0;
                if (fi < cats.length) { page = Page.MODULES; category = cats[fi]; listTime = System.currentTimeMillis(); listScroll = listScrollT = 0; }
                else page = new Page[]{Page.APPEARANCE, Page.CONFIGS, Page.FRIENDS}[fi - cats.length];
            });
        }

        // profil
        float by = y0 + H - 44;
        R2D.rect(ctx, x0 + 14, by, nav - 28, 0.5f, 0x14FFFFFF);
        head(ctx, x0 + 14, by + 12, 22);
        String nick = client != null ? client.getSession().getUsername() : "Player";
        Fonts.draw(ctx, Fonts.trim(nick, Font.BOLD, 0.76f, nav - 52), x0 + 42, by + 13, TEXT, Font.BOLD, 0.76f);
        long on = Modules.all().stream().filter(Module::isEnabled).count();
        Fonts.draw(ctx, on + t(" modułów", " modules"), x0 + 42, by + 24, FAINT, Font.REGULAR, 0.58f);
        R2D.alpha = prev;
    }

    // ------------------------------------------------------------------ strona modułów (lista + ustawienia)

    private void drawModulesPage(DrawContext ctx) {
        float listW = Math.max(210, (x0 + W - navR) * 0.42f);
        listR = navR + listW;
        drawModuleList(ctx, navR, listW);
        R2D.rect(ctx, listR, y0 + 10, 0.5f, H - 20, 0x14FFFFFF);
        drawSettings(ctx, listR, x0 + W - listR);
    }

    private void drawModuleList(DrawContext ctx, float x, float w) {
        float mx0 = x + 14, mw = w - 24;
        List<Module> list = list();
        String title = !query.isEmpty() ? t("Wyniki", "Results") : category.title();
        Fonts.draw(ctx, title, mx0, y0 + 16, TEXT, Font.BOLD, 1.0f);
        Fonts.draw(ctx, list.size() + t(" modułów, ", " modules, ") + list.stream().filter(Module::isEnabled).count() + t(" włączonych", " on"), mx0, y0 + 30, FAINT, Font.REGULAR, 0.58f);
        // szukaj
        float sh = 22, sy = y0 + 42, sw = mw;
        field(ctx, 1, mx0, sy, sw, sh, query, t("Szukaj modułu…", "Search modules…"), "g_search");
        if (!query.isEmpty()) {
            float bx = mx0 + sw - 16;
            R2D.icon(ctx, "g_close", bx, sy + sh / 2f - 3.5f, 7, over(bx - 3, sy, 14, sh) ? TEXT : FAINT);
            hit(bx - 3, sy, 14, sh, b -> { query = ""; listTime = System.currentTimeMillis(); });
        }
        float top = sy + sh + 8, listH = y0 + H - 12 - top;
        float rh = 30, gap = 4;
        float contentH = list.size() * (rh + gap);
        listMax = Math.max(0, contentH - listH);
        listScrollT = Math.max(0, Math.min(listMax, listScrollT));
        listScroll += (listScrollT - listScroll) * 0.3f;
        R2D.scissor(ctx, mx0 - 4, top - 2, mw + 8, listH + 4);
        long now = System.currentTimeMillis();
        for (int i = 0; i < list.size(); i++) {
            float ry = top + i * (rh + gap) - listScroll;
            if (ry + rh < top - 2 || ry > top + listH + 2) continue;
            float e = Anim.easeOutCubic((now - listTime - i * 14) / (260f / Theme.motionSpeed()));
            float pv = R2D.alpha; R2D.alpha = pv * e;
            drawListRow(ctx, list.get(i), mx0 + (1 - e) * 6, ry, mw, rh, top, listH);
            R2D.alpha = pv;
        }
        R2D.endScissor(ctx);
        if (list.isEmpty()) Fonts.drawCentered(ctx, t("Nic nie znaleziono", "Nothing found"), mx0 + mw / 2f, top + listH / 2f, DIM, Font.SEMIBOLD, 0.74f);
        if (listMax > 0) {
            float th = Math.max(20, listH * listH / contentH), tyy = top + (listH - th) * (listScroll / listMax);
            R2D.roundRect(ctx, mx0 + mw + 4, tyy, 2, th, 1, 0x55FFFFFF);
        }
    }

    private void drawListRow(DrawContext ctx, Module m, float x, float y, float w, float h, float clipTop, float clipH) {
        boolean sel = selected == m;
        float hv = anim("lh" + m.name).animate(over(x, Math.max(y, clipTop), w, Math.min(h, clipTop + clipH - y)) && my >= clipTop && my < clipTop + clipH ? 1 : 0);
        float on = state("lon" + m.name, 18, m.isEnabled() ? 1 : 0);
        float sa = anim("lsel" + m.name).animate(sel ? 1 : 0);
        int acc = Theme.accent();
        R2D.roundRect(ctx, x, y, w, h, 8, R2D.blend(R2D.blend(0x08FFFFFF, 0x12FFFFFF, hv), R2D.withAlpha(acc, 0.1f), sa));
        R2D.roundOutline(ctx, x, y, w, h, 8, 0.6f, R2D.blend(0x12FFFFFF, R2D.withAlpha(acc, 0.55f), sa));
        if (on > 0.01f) R2D.roundRect(ctx, x + 2, y + h / 2f - 7, 2, 14, 1, R2D.withAlpha(acc, on));
        R2D.roundRectV(ctx, x + 8, y + h / 2f - 10, 20, 20, 6, R2D.blend(0xFF1C1C21, Theme.accentTop(), on), R2D.blend(0xFF141418, Theme.accentBottom(), on));
        R2D.icon(ctx, m.icon, x + 13, y + h / 2f - 5, 10, R2D.blend(0xFFCFCFD6, Theme.onAccent(), on));
        Fonts.drawV(ctx, Fonts.trim(m.name, Font.BOLD, 0.76f, w - 90), x + 34, y + h / 2f + 0.5f, R2D.blend(0xFFE2E2E6, 0xFFFFFFFF, on), Font.BOLD, 0.76f);
        if (m.favorite) R2D.icon(ctx, "g_star", x + 34 + Fonts.width(m.name, Font.BOLD, 0.76f) + 4, y + h / 2f - 3.5f, 6, DIM);
        String kn = binding == m ? "…" : keyName(m.key);
        float tx = x + w - 34;
        if (!kn.isEmpty()) {
            float kw = Math.max(13, Fonts.width(kn, Font.BOLD, 0.5f) + 8);
            R2D.roundRect(ctx, tx - kw - 4, y + h / 2f - 6, kw, 12, 3.5f, binding == m ? R2D.withAlpha(acc, 0.4f) : 0x18FFFFFF);
            Fonts.drawV(ctx, kn, tx - kw - 4 + (kw - Fonts.width(kn, Font.BOLD, 0.5f)) / 2f, y + h / 2f + 0.5f, DIM, Font.BOLD, 0.5f);
        }
        toggle(ctx, x + w - 28, y + h / 2f - 5.5f, on, hv);
        hit(x, Math.max(y, clipTop), w, Math.min(h, clipTop + clipH - Math.max(y, clipTop)), b -> {
            if (my < clipTop || my >= clipTop + clipH) return;
            if (b == 0) { if (over(x + w - 30, y, 30, h)) { m.toggle(); Modules.save(); } else selectModule(m); }
            else if (b == 1) selectModule(m);
            else if (b == 2) { binding = m; }
            click();
        });
    }

    private void selectModule(Module m) {
        if (selected != m) { setScroll = setScrollT = 0; }
        selected = m;
    }

    // ------------------------------------------------------------------ panel ustawień

    private void drawSettings(DrawContext ctx, float x, float w) {
        Module m = selected;
        if (m == null || (!query.isEmpty() ? false : m.category != category) && query.isEmpty() && !Modules.of(category).contains(m)) {
            // jeśli wybrany moduł nie należy do bieżącej kategorii, wybierz pierwszy
        }
        if (m == null) { drawNoSelection(ctx, x, w); return; }
        float px = x + 16, pw = w - 32, y = y0 + 16;
        float on = state("son" + m.name, 18, m.isEnabled() ? 1 : 0);
        R2D.roundRectV(ctx, px, y, 36, 36, 10, R2D.blend(0xFF1C1C21, Theme.accentTop(), on), R2D.blend(0xFF141418, Theme.accentBottom(), on));
        R2D.icon(ctx, m.icon, px + 10, y + 10, 16, R2D.blend(0xFFCFCFD6, Theme.onAccent(), on));
        Fonts.draw(ctx, Fonts.trim(m.name, Font.BOLD, 1.1f, pw - 100), px + 46, y + 3, TEXT, Font.BOLD, 1.1f);
        String cat = m.category.title().toUpperCase(Locale.ROOT);
        float cw = Fonts.width(cat, Font.BOLD, 0.5f) + 10;
        R2D.roundRect(ctx, px + 46, y + 19, cw, 11, 5.5f, R2D.withAlpha(Theme.accent(), 0.18f));
        Fonts.drawV(ctx, cat, px + 51, y + 24.5f, Theme.accent(), Font.BOLD, 0.5f);
        // główny przełącznik
        float bw = 58, bx = px + pw - bw;
        float bh2 = hover("sEnable", bx, y + 6, bw, 22);
        R2D.roundRect(ctx, bx, y + 6, bw, 22, 11, on > 0.5f ? Theme.accent() : R2D.blend(0x18FFFFFF, 0x26FFFFFF, bh2));
        Fonts.drawCentered(ctx, m.isEnabled() ? t("WŁ.", "ON") : t("WYŁ.", "OFF"), bx + bw / 2f, y + 17 - Fonts.height(0.58f) / 2f + 0.5f, on > 0.5f ? Theme.onAccent() : TEXT, Font.BOLD, 0.58f);
        hit(bx, y + 6, bw, 22, b -> { if (b == 0) { m.toggle(); Modules.save(); click(); } });
        y += 42;
        for (String l : Fonts.wrap(m.description(), Font.REGULAR, 0.64f, pw)) { Fonts.draw(ctx, l, px, y, DIM, Font.REGULAR, 0.64f); y += 10; }
        y += 4;
        R2D.rect(ctx, px, y, pw, 0.5f, 0x12FFFFFF); y += 10;

        float resetY = y0 + H - 14 - 24;
        float top = y, areaH = resetY - 8 - top;
        R2D.scissor(ctx, x, top, w, areaH);
        float cy = top - setScroll;
        // klawisz + ulubiony w jednej linii
        cy = settingRow(ctx, px, cy, pw, t("Klawisz", "Keybind"), () -> {
            String kn = binding == m ? t("Naciśnij…", "Press…") : m.key < 0 ? t("Brak", "None") : keyName(m.key);
            float kw = Fonts.width(kn, Font.BOLD, 0.6f) + 14, kx = px + pw - kw, ky = -1;
            return new Object[]{kn, kw};
        }, b -> { binding = binding == m ? null : m; });
        cy = settingRowStar(ctx, px, cy, pw, m);
        // ustawienia
        for (Setting<?> st : m.settings) cy = drawSetting(ctx, m, st, px, cy, pw);
        cy += 6;
        setMax = Math.max(0, cy + setScroll - top - areaH + 6);
        setScrollT = Math.max(0, Math.min(setMax, setScrollT));
        setScroll += (setScrollT - setScroll) * 0.3f;
        R2D.endScissor(ctx);
        // reset (na nieprzezroczystym pasku, żeby opcje nie prześwitywały)
        float ry = resetY;
        R2D.rect(ctx, x + 1, ry - 6, w - 2, y0 + H - 2 - (ry - 6), 0xF5101013);
        R2D.rect(ctx, px, ry - 6, pw, 0.5f, 0x12FFFFFF);
        button(ctx, px, ry + 2, pw, 22, "g_reset", t("Przywróć domyślne", "Reset to default"), false, hover("reset", px, ry + 2, pw, 22), true);
        hit(px, ry + 2, pw, 22, b -> { if (b == 0) { m.reset(); Modules.save(); click(); } });
    }

    private void drawNoSelection(DrawContext ctx, float x, float w) {
        float cx = x + w / 2f, cy = y0 + H / 2f;
        R2D.glow(ctx, cx - 30, cy - 50, 60, 60, R2D.withAlpha(Theme.accent(), 0.08f));
        R2D.roundRectV(ctx, cx - 26, cy - 54, 52, 52, 14, 0xFF18181C, 0xFF0E0E10);
        R2D.icon(ctx, "with_logo", cx - 20, cy - 48, 40, 0xFFFFFFFF);
        Fonts.drawCentered(ctx, t("Wybierz moduł z listy", "Pick a module from the list"), cx, cy + 10, DIM, Font.BOLD, 0.84f);
        Fonts.drawCentered(ctx, t("żeby zobaczyć jego opcje", "to see its options"), cx, cy + 24, FAINT, Font.REGULAR, 0.64f);
    }

    private float settingRow(DrawContext ctx, float x, float y, float w, String label, java.util.function.Supplier<Object[]> val, IntConsumer onClick) {
        float hv = hover("sr" + label, x, y, w, 24);
        Fonts.drawV(ctx, label, x, y + 12, R2D.blend(DIM, TEXT, hv), Font.SEMIBOLD, 0.72f);
        Object[] v = val.get();
        String txt = (String) v[0]; float vw = (float) v[1];
        float bx = x + w - vw;
        R2D.roundRect(ctx, bx, y + 2, vw, 14, 7, R2D.blend(0x14FFFFFF, Theme.accent(), binding == selected ? 1 : hv * 0.4f));
        Fonts.drawV(ctx, txt, bx + 7, y + 9, binding == selected ? Theme.onAccent() : TEXT, Font.BOLD, 0.6f);
        hit(x, y, w, 24, b -> { if (b == 0) { onClick.accept(0); click(); } });
        return y + 26;
    }
    private float settingRowStar(DrawContext ctx, float x, float y, float w, Module m) {
        float hv = hover("srstar", x, y, w, 24);
        Fonts.drawV(ctx, t("Ulubiony", "Favorite"), x, y + 12, R2D.blend(DIM, TEXT, hv), Font.SEMIBOLD, 0.72f);
        float fv = state("sfav" + m.name, 16, m.favorite ? 1 : 0);
        R2D.icon(ctx, "g_star", x + w - 13, y + 5, 11, R2D.blend(0x44FFFFFF, Theme.accent(), fv));
        hit(x, y, w, 24, b -> { if (b == 0) { m.favorite = !m.favorite; Modules.save(); click(); } });
        return y + 26;
    }

    private float drawSetting(DrawContext ctx, Module m, Setting<?> s, float x, float y, float w) {
        String key = m.name + "." + s.name;
        if (s instanceof Setting.Bool bo) {
            float hv = hover("h" + key, x, y, w, 24);
            float on = state("b" + key, 18, bo.get() ? 1 : 0);
            Fonts.drawV(ctx, s.name, x, y + 12, R2D.blend(DIM, TEXT, Math.max(hv, on)), Font.SEMIBOLD, 0.7f);
            toggle(ctx, x + w - 20, y + 6.5f, on, hv);
            hit(x, y, w, 24, b -> { if (b == 0) { bo.toggle(); Modules.save(); click(); } });
            return y + 26;
        }
        if (s instanceof Setting.Num n) {
            float hv = anim("sh" + key).animate(over(x, y, w, 30) || dragging == n ? 1 : 0);
            Fonts.drawV(ctx, s.name, x, y + 8, R2D.blend(DIM, TEXT, hv), Font.SEMIBOLD, 0.7f);
            String v = n.text();
            float vw = Fonts.width(v, Font.BOLD, 0.62f) + 10;
            R2D.roundRect(ctx, x + w - vw, y + 2, vw, 12, 6, R2D.blend(0x14FFFFFF, 0x24FFFFFF, hv));
            Fonts.drawV(ctx, v, x + w - vw + 5, y + 8.5f, TEXT, Font.BOLD, 0.62f);
            float ty = y + 21, th = 4, f = state("v" + key, 22, n.fraction());
            R2D.roundRect(ctx, x, ty, w, th, 2, 0x1AFFFFFF);
            R2D.roundRectH(ctx, x, ty, Math.max(th, w * f), th, 2, R2D.blend(Theme.accent(), 0xFF000000, 0.25f), Theme.accent());
            float kx = x + w * f;
            if (hv > 0.01f) R2D.glow(ctx, kx - 9, ty + th / 2f - 9, 18, 18, R2D.withAlpha(Theme.accent(), 0.4f * hv));
            R2D.circle(ctx, kx, ty + th / 2f, 4.3f + hv, 0xFFFFFFFF);
            R2D.circle(ctx, kx, ty + th / 2f, 1.7f, 0xFF0A0A0C);
            final float fx = x, fw = w;
            hit(x - 4, y, w + 8, 30, b -> { if (b == 0) { dragging = n; dragApply = () -> n.set(n.min + (n.max - n.min) * Anim.clamp01((mx - fx) / fw)); dragApply.run(); } });
            return y + 32;
        }
        if (s instanceof Setting.Mode o) {
            Fonts.drawV(ctx, s.name, x, y + 8, DIM, Font.SEMIBOLD, 0.7f);
            float cx = x, cyy = y + 18;
            for (String mode : o.modes) {
                float tw = Fonts.width(mode, Font.BOLD, 0.58f) + 14;
                if (cx + tw > x + w && cx > x) { cx = x; cyy += 18; }
                boolean act = o.get().equals(mode);
                float sa = state("m" + key + mode, 16, act ? 1 : 0), hv = hover("mh" + key + mode, cx, cyy, tw, 15);
                R2D.roundRect(ctx, cx, cyy, tw, 15, 7.5f, R2D.blend(R2D.blend(0x12FFFFFF, 0x22FFFFFF, hv), Theme.accent(), sa));
                Fonts.drawV(ctx, mode, cx + 7, cyy + 8, R2D.blend(R2D.blend(DIM, TEXT, hv), Theme.onAccent(), sa), Font.BOLD, 0.58f);
                final String fm = mode;
                hit(cx, cyy, tw, 15, b -> { if (b == 0) { o.set(fm); Modules.save(); click(); } });
                cx += tw + 5;
            }
            return cyy + 22;
        }
        if (s instanceof Setting.Color c) {
            float hv = hover("ch" + key, x, y, w, 20);
            Fonts.drawV(ctx, s.name, x, y + 10, R2D.blend(DIM, TEXT, hv), Font.SEMIBOLD, 0.7f);
            float sw = 34, sx = x + w - sw;
            R2D.roundRect(ctx, sx - 1, y + 2, sw + 2, 16, 5, 0x22FFFFFF);
            // szachownica alfy
            R2D.roundRect(ctx, sx, y + 3, sw, 14, 4, 0xFF2A2A2E);
            R2D.roundRect(ctx, sx, y + 3, sw, 14, 4, c.get());
            R2D.roundOutline(ctx, sx, y + 3, sw, 14, 4, 0.6f, 0x33FFFFFF);
            hit(sx - 1, y, sw + 4, 20, b -> { if (b == 0) { openPicker(c); click(); } });
            return y + 24;
        }
        return y;
    }

    // ------------------------------------------------------------------ color picker

    private void openPicker(Setting.Color c) {
        picking = c;
        int v = c.get();
        pickAlpha = ((v >>> 24) & 0xFF) / 255f;
        float[] hsv = rgbToHsv(v);
        pickHSV = hsv;
    }

    private void drawColorPicker(DrawContext ctx) {
        if (picking == null) return;
        float pw = 190, ph = 180, px = Math.min(x0 + W - pw - 16, mx + 8), py = Math.min(y0 + H - ph - 10, my + 8);
        px = Math.max(x0 + 8, px); py = Math.max(y0 + 8, py);
        R2D.shadow(ctx, px, py + 4, pw, ph, 24, 0xCC000000);
        R2D.roundRectV(ctx, px, py, pw, ph, 12, 0xFA141417, 0xFC0C0C0E);
        R2D.roundOutline(ctx, px, py, pw, ph, 12, 0.7f, 0x33FFFFFF);
        Fonts.draw(ctx, picking.name, px + 12, py + 11, TEXT, Font.BOLD, 0.76f);
        float cbx = px + pw - 12 - 16, cby = py + 8;
        float ch = hover("pickClose", cbx, cby, 16, 16);
        R2D.roundRect(ctx, cbx, cby, 16, 16, 8, R2D.blend(0x18FFFFFF, 0xFFF4F4F5, ch));
        R2D.icon(ctx, "g_close", cbx + 5, cby + 5, 6, R2D.blend(TEXT, 0xFF0A0A0C, ch));
        hit(cbx, cby, 16, 16, b -> { picking = null; });
        // SV square
        float sqx = px + 12, sqy = py + 28, sq = 120;
        pickSqX = sqx; pickSqY = sqy;
        drawSVSquare(ctx, sqx, sqy, sq, pickHSV[0]);
        float dotx = sqx + pickHSV[1] * sq, doty = sqy + (1 - pickHSV[2]) * sq;
        R2D.circle(ctx, dotx, doty, 4, 0xFFFFFFFF); R2D.circle(ctx, dotx, doty, 2.6f, hsvToRgb(pickHSV[0], pickHSV[1], pickHSV[2], 1));
        hit(sqx, sqy, sq, sq, b -> { if (b == 0) pickDrag = 1; });
        // hue bar
        float hbx = sqx + sq + 10, hby = sqy, hbw = 12;
        drawHueBar(ctx, hbx, hby, hbw, sq);
        R2D.roundRect(ctx, hbx - 1, hby + pickHSV[0] * sq - 1.5f, hbw + 2, 3, 1.5f, 0xFFFFFFFF);
        hit(hbx, hby, hbw, sq, b -> { if (b == 0) pickDrag = 2; });
        // alpha bar
        float abx = hbx + hbw + 8;
        R2D.roundRect(ctx, abx, hby, hbw, sq, 3, 0xFF2A2A2E);
        R2D.gradientV(ctx, abx, hby, hbw, sq, R2D.withAlpha(hsvToRgb(pickHSV[0], pickHSV[1], pickHSV[2], 1), 1f), R2D.withAlpha(hsvToRgb(pickHSV[0], pickHSV[1], pickHSV[2], 1), 0f));
        R2D.roundOutline(ctx, abx, hby, hbw, sq, 3, 0.5f, 0x22FFFFFF);
        R2D.roundRect(ctx, abx - 1, hby + (1 - pickAlpha) * sq - 1.5f, hbw + 2, 3, 1.5f, 0xFFFFFFFF);
        hit(abx, hby, hbw, sq, b -> { if (b == 0) pickDrag = 3; });
        // podgląd + hex
        int col = hsvToRgb(pickHSV[0], pickHSV[1], pickHSV[2], pickAlpha);
        float pry = sqy + sq + 8;
        R2D.roundRect(ctx, px + 12, pry, 26, 14, 4, col);
        R2D.roundOutline(ctx, px + 12, pry, 26, 14, 4, 0.5f, 0x33FFFFFF);
        String hex = String.format("#%08X", col);
        Fonts.drawV(ctx, hex, px + 44, pry + 7, DIM, Font.BOLD, 0.62f);
        picking.set(col);
    }

    private void drawSVSquare(DrawContext ctx, float x, float y, float s, float hue) {
        int base = hsvToRgb(hue, 1, 1, 1);
        R2D.rect(ctx, x, y, s, s, base);
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
        float r = 0, g = 0, b = 0;
        int i = (int) (h * 6) % 6; float f = h * 6 - (int) (h * 6);
        float p = v * (1 - s), q = v * (1 - f * s), tt = v * (1 - (1 - f) * s);
        switch (i) { case 0 -> { r = v; g = tt; b = p; } case 1 -> { r = q; g = v; b = p; } case 2 -> { r = p; g = v; b = tt; }
            case 3 -> { r = p; g = q; b = v; } case 4 -> { r = tt; g = p; b = v; } default -> { r = v; g = p; b = q; } }
        return ((int) (a * 255) << 24) | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    // ------------------------------------------------------------------ strona: Wygląd

    private void drawAppearance(DrawContext ctx, float x, float w) {
        float mx0 = x + 16, mw = w - 32;
        Fonts.draw(ctx, t("Wygląd", "Appearance"), mx0, y0 + 16, TEXT, Font.BOLD, 1.1f);
        Fonts.draw(ctx, t("Dostosuj kolory, animacje i okno klienta", "Customize colors, motion and the window"), mx0, y0 + 32, FAINT, Font.REGULAR, 0.6f);
        float top = y0 + 48, areaH = y0 + H - 14 - top;
        R2D.scissor(ctx, mx0 - 4, top - 2, mw + 8, areaH + 4);
        float y = top - appScroll;
        // akcent
        y = cardHead(ctx, mx0, y, mw, "g_render", t("Kolor akcentu", "Accent color"));
        float sx = mx0 + 4;
        for (int i = 0; i < Theme.ACCENTS.length; i++) {
            float r = 9, ccx = sx + r, ccy = y + r;
            boolean sel = Theme.accentIndex == i;
            float hv = hover("ac" + i, ccx - r, ccy - r, r * 2, r * 2), sa = state("acs" + i, 16, sel ? 1 : 0);
            if (sa > 0.01f) { R2D.circle(ctx, ccx, ccy, r + 2.8f * sa, 0xFFFFFFFF); R2D.circle(ctx, ccx, ccy, r + 1.3f * sa, 0xFF0C0C0E); }
            R2D.circle(ctx, ccx, ccy, r + hv, Theme.ACCENTS[i]);
            if (sel) R2D.icon(ctx, "g_check", ccx - 5, ccy - 5, 10, Theme.contrast(Theme.ACCENTS[i]));
            final int fi = i;
            hit(ccx - r, ccy - r, r * 2, r * 2, b -> { if (b == 0) { Theme.accentIndex = fi; Theme.save(); click(); } });
            sx += r * 2 + 7;
            if (sx + r * 2 > mx0 + mw) { sx = mx0 + 4; y += r * 2 + 7; }
        }
        y += 30;
        // animacje
        y = cardHead(ctx, mx0, y, mw, "g_hud", t("Animacje", "Motion"));
        y += chips(ctx, "motion", mx0, y, mw, new String[]{t("Wolne", "Slow"), t("Normalne", "Normal"), t("Szybkie", "Fast"), t("Wyłączone", "Off")}, Arrays.asList(Theme.MOTIONS).indexOf(Theme.motion), i -> { Theme.motion = Theme.MOTIONS[i]; Theme.save(); }) + 12;
        // rozmiar
        y = cardHead(ctx, mx0, y, mw, "g_misc", t("Rozmiar okna", "Window size"));
        y += appSlider(ctx, "scale", mx0, y, mw, t("Skala", "Scale"), Math.round(Theme.scale * 100) + " %", (Theme.scale - 0.8f) / 0.4f, f -> { Theme.scale = Math.round((0.8f + 0.4f * f) * 20) / 20f; Theme.save(); }) + 10;
        y += appSlider(ctx, "dim", mx0, y, mw, t("Przyciemnienie tła", "Background dim"), Math.round(Theme.dim * 100) + " %", Theme.dim / 0.8f, f -> { Theme.dim = Math.round(0.8f * f * 20) / 20f; Theme.save(); }) + 12;
        // efekty
        y = cardHead(ctx, mx0, y, mw, "g_gear", t("Efekty", "Effects"));
        y = appToggle(ctx, "blur", mx0, y, mw, t("Rozmycie tła", "Background blur"), Theme.blur, () -> Theme.blur = !Theme.blur);
        y = appToggle(ctx, "shadow", mx0, y, mw, t("Cień okna", "Window shadow"), Theme.shadow, () -> Theme.shadow = !Theme.shadow);
        y = appToggle(ctx, "sounds", mx0, y, mw, t("Dźwięki interfejsu", "Interface sounds"), Theme.sounds, () -> Theme.sounds = !Theme.sounds);
        y += 8;
        appMax = Math.max(0, y + appScroll - top - areaH + 6);
        appScrollT = Math.max(0, Math.min(appMax, appScrollT));
        appScroll += (appScrollT - appScroll) * 0.3f;
        R2D.endScissor(ctx);
    }
    private float cardHead(DrawContext ctx, float x, float y, float w, String icon, String title) {
        R2D.icon(ctx, icon, x, y + 1, 10, Theme.accent());
        Fonts.draw(ctx, title, x + 16, y, TEXT, Font.BOLD, 0.8f);
        return y + 18;
    }
    private float appToggle(DrawContext ctx, String k, float x, float y, float w, String label, boolean val, Runnable flip) {
        float hv = hover("at" + k, x, y, w, 22), on = state("av" + k, 18, val ? 1 : 0);
        Fonts.drawV(ctx, label, x, y + 11, R2D.blend(DIM, TEXT, Math.max(hv, on)), Font.SEMIBOLD, 0.7f);
        toggle(ctx, x + w - 20, y + 5.5f, on, hv);
        hit(x, y, w, 22, b -> { if (b == 0) { flip.run(); Theme.save(); click(); } });
        return y + 24;
    }
    private float appSlider(DrawContext ctx, String k, float x, float y, float w, String label, String val, float frac, java.util.function.Consumer<Float> set) {
        float hv = anim("ash" + k).animate(over(x, y, w, 30) || k.equals(appDragKey) ? 1 : 0);
        Fonts.drawV(ctx, label, x, y + 8, R2D.blend(DIM, TEXT, hv), Font.SEMIBOLD, 0.7f);
        float vw = Fonts.width(val, Font.BOLD, 0.62f) + 10;
        R2D.roundRect(ctx, x + w - vw, y + 2, vw, 12, 6, 0x1AFFFFFF);
        Fonts.drawV(ctx, val, x + w - vw + 5, y + 8.5f, TEXT, Font.BOLD, 0.62f);
        float ty = y + 21, th = 4, f = state("asv" + k, 22, frac);
        R2D.roundRect(ctx, x, ty, w, th, 2, 0x1AFFFFFF);
        R2D.roundRectH(ctx, x, ty, Math.max(th, w * f), th, 2, R2D.blend(Theme.accent(), 0xFF000000, 0.25f), Theme.accent());
        R2D.circle(ctx, x + w * f, ty + th / 2f, 4.3f + hv, 0xFFFFFFFF);
        R2D.circle(ctx, x + w * f, ty + th / 2f, 1.7f, 0xFF0A0A0C);
        final float fx = x, fw = w;
        hit(x - 4, y, w + 8, 30, b -> { if (b == 0) { appDragKey = k; dragging = DUMMY; dragApply = () -> set.accept(Anim.clamp01((mx - fx) / fw)); dragApply.run(); } });
        return 32;
    }
    private String appDragKey;
    private static final Setting.Num DUMMY = new Setting.Num("", 0, 0, 1, 1, "");

    private float chips(DrawContext ctx, String key, float x, float y, float w, String[] labels, int sel, IntConsumer pick) {
        float cx = x, cy = y;
        for (int i = 0; i < labels.length; i++) {
            float tw = Fonts.width(labels[i], Font.BOLD, 0.6f) + 16;
            if (cx + tw > x + w && cx > x) { cx = x; cy += 19; }
            float sa = state("c" + key + i, 16, i == sel ? 1 : 0), hv = hover("ch" + key + i, cx, cy, tw, 16);
            R2D.roundRect(ctx, cx, cy, tw, 16, 8, R2D.blend(R2D.blend(0x12FFFFFF, 0x22FFFFFF, hv), Theme.accent(), sa));
            Fonts.drawV(ctx, labels[i], cx + 8, cy + 8.5f, R2D.blend(R2D.blend(DIM, TEXT, hv), Theme.onAccent(), sa), Font.BOLD, 0.6f);
            final int fi = i;
            hit(cx, cy, tw, 16, b -> { if (b == 0) { pick.accept(fi); click(); } });
            cx += tw + 5;
        }
        return cy - y + 16;
    }

    // ------------------------------------------------------------------ strona: Konfiguracje

    private void drawConfigs(DrawContext ctx, float x, float w) {
        float mx0 = x + 16, mw = w - 32;
        List<Modules.Config> list = Modules.configs();
        Fonts.draw(ctx, t("Konfiguracje", "Configs"), mx0, y0 + 16, TEXT, Font.BOLD, 1.1f);
        Fonts.draw(ctx, list.size() + t(" zapisanych, aktywna: ", " saved, active: ") + Theme.config, mx0, y0 + 32, FAINT, Font.REGULAR, 0.6f);
        float bw = 118, bx = mx0 + mw - bw;
        button(ctx, bx, y0 + 14, bw, 22, "g_plus", t("Zapisz nową", "Save new"), true, hover("cfgNew", bx, y0 + 14, bw, 22), true);
        hit(bx, y0 + 14, bw, 22, b -> { if (b == 0) { String n = Modules.newConfigName(); Modules.saveConfig(n); Theme.config = n; Theme.save(); click(); } });
        float top = y0 + 48, areaH = y0 + H - 14 - top, rh = 40, gap = 6;
        R2D.scissor(ctx, mx0 - 4, top - 2, mw + 8, areaH + 4);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pl() ? "d MMM, HH:mm" : "MMM d, HH:mm", pl() ? Locale.forLanguageTag("pl-PL") : Locale.ENGLISH);
        float y = top - appScroll;
        for (Modules.Config c : list) {
            boolean active = c.name().equals(Theme.config);
            float hv = hover("cf" + c.name(), mx0, y, mw, rh);
            R2D.roundRect(ctx, mx0, y, mw, rh, 10, R2D.blend(0x08FFFFFF, 0x12FFFFFF, hv));
            R2D.roundOutline(ctx, mx0, y, mw, rh, 10, 0.6f, active ? R2D.withAlpha(Theme.accent(), 0.55f) : 0x14FFFFFF);
            R2D.roundRectV(ctx, mx0 + 8, y + 7, 26, 26, 7, active ? Theme.accentTop() : 0xFF1C1C21, active ? Theme.accentBottom() : 0xFF141418);
            R2D.icon(ctx, "g_configs", mx0 + 15, y + 14, 12, active ? Theme.onAccent() : 0xFFCFCFD6);
            Fonts.draw(ctx, c.name(), mx0 + 42, y + 9, TEXT, Font.BOLD, 0.8f);
            Fonts.draw(ctx, t("Zapisano ", "Saved ") + fmt.format(Instant.ofEpochMilli(c.modified()).atZone(ZoneId.systemDefault())), mx0 + 42, y + 22, FAINT, Font.REGULAR, 0.56f);
            float rx = mx0 + mw - 8;
            boolean armed = c.name().equals(delName) && System.currentTimeMillis() - delArm < 3000;
            float dw = armed ? Fonts.width(t("Na pewno?", "Sure?"), Font.BOLD, 0.6f) + 18 : 24;
            rx -= dw;
            float dh = hover("cd" + c.name(), rx, y + 9, dw, 22);
            if (armed) { R2D.roundRect(ctx, rx, y + 9, dw, 22, 11, R2D.blend(0xFFDC2626, 0xFFEF4444, dh)); Fonts.drawV(ctx, t("Na pewno?", "Sure?"), rx + 9, y + 20.5f, 0xFFFFFFFF, Font.BOLD, 0.6f); }
            else button(ctx, rx, y + 9, dw, 22, "g_trash", null, false, dh, true);
            hit(rx, y + 9, dw, 22, b -> { if (b != 0) return; if (armed) { Modules.deleteConfig(c.name()); delName = null; if (Theme.config.equals(c.name())) { Theme.config = "default"; Theme.save(); } } else { delName = c.name(); delArm = System.currentTimeMillis(); } click(); });
            rx -= 30; button(ctx, rx, y + 9, 24, 22, "g_save", null, false, hover("cs" + c.name(), rx, y + 9, 24, 22), true);
            hit(rx, y + 9, 24, 22, b -> { if (b == 0) { Modules.saveConfig(c.name()); Theme.config = c.name(); Theme.save(); click(); } });
            float lw = Fonts.width(t("Wczytaj", "Load"), Font.BOLD, 0.68f) + 32; rx -= lw + 6;
            button(ctx, rx, y + 9, lw, 22, "g_load", t("Wczytaj", "Load"), !active, hover("cl" + c.name(), rx, y + 9, lw, 22), true);
            hit(rx, y + 9, lw, 22, b -> { if (b == 0) { Modules.loadConfig(c.name()); Theme.config = c.name(); Theme.save(); click(); } });
            y += rh + gap;
        }
        if (list.isEmpty()) Fonts.drawCentered(ctx, t("Brak konfiguracji – kliknij „Zapisz nową”", "No configs – click \"Save new\""), mx0 + mw / 2f, top + areaH / 2f, DIM, Font.SEMIBOLD, 0.74f);
        appMax = Math.max(0, list.size() * (rh + gap) - gap - areaH);
        appScrollT = Math.max(0, Math.min(appMax, appScrollT));
        appScroll += (appScrollT - appScroll) * 0.3f;
        R2D.endScissor(ctx);
    }
    private String delName; private long delArm;

    // ------------------------------------------------------------------ strona: Znajomi

    private void drawFriends(DrawContext ctx, float x, float w) {
        float mx0 = x + 16, mw = w - 32;
        List<String> list = Friends.all();
        Fonts.draw(ctx, t("Znajomi", "Friends"), mx0, y0 + 16, TEXT, Font.BOLD, 1.1f);
        Fonts.draw(ctx, list.size() + (pl() ? " znajomych" : " friends"), mx0, y0 + 32, FAINT, Font.REGULAR, 0.6f);
        float top = y0 + 48;
        float bw = 86, fw = mw - bw - 8;
        field(ctx, 2, mx0, top, fw, 22, friendInput, t("Wpisz nick…", "Type a name…"), "g_player");
        button(ctx, mx0 + fw + 8, top, bw, 22, "g_plus", t("Dodaj", "Add"), true, hover("frAdd", mx0 + fw + 8, top, bw, 22), !friendInput.isBlank());
        hit(mx0 + fw + 8, top, bw, 22, b -> addFriend());
        top += 32;
        float areaH = y0 + H - 14 - top, rh = 32, gap = 5;
        int cols = mw >= 360 ? 2 : 1; float cw = (mw - gap * (cols - 1)) / cols;
        R2D.scissor(ctx, mx0 - 4, top - 2, mw + 8, areaH + 4);
        for (int i = 0; i < list.size(); i++) {
            String f = list.get(i);
            float cx = mx0 + (i % cols) * (cw + gap), cy = top + (i / cols) * (rh + gap) - appScroll;
            float hv = hover("fr" + f, cx, cy, cw, rh);
            R2D.roundRect(ctx, cx, cy, cw, rh, 9, R2D.blend(0x08FFFFFF, 0x12FFFFFF, hv));
            R2D.roundOutline(ctx, cx, cy, cw, rh, 9, 0.5f, 0x14FFFFFF);
            monogram(ctx, cx + 6, cy + 5, 22, f);
            Fonts.drawV(ctx, f, cx + 36, cy + rh / 2f + 0.5f, TEXT, Font.BOLD, 0.74f);
            float rx = cx + cw - 26; float dh = hover("fd" + f, rx, cy + 6, 20, 20);
            R2D.roundRect(ctx, rx, cy + 6, 20, 20, 10, R2D.blend(0x00FFFFFF, 0xFFDC2626, dh));
            R2D.icon(ctx, "g_trash", rx + 5.5f, cy + 11.5f, 9, R2D.blend(R2D.withAlpha(0xFFFFFFFF, 0.3f + 0.5f * hv), 0xFFFFFFFF, dh));
            hit(rx, cy + 6, 20, 20, b -> { if (b == 0) { Friends.remove(f); click(); } });
        }
        if (list.isEmpty()) Fonts.drawCentered(ctx, t("Brak znajomych – wpisz nick i kliknij „Dodaj”", "No friends – type a name and click \"Add\""), mx0 + mw / 2f, top + areaH / 2f, DIM, Font.SEMIBOLD, 0.72f);
        int rows = (list.size() + cols - 1) / cols;
        appMax = Math.max(0, rows * (rh + gap) - gap - areaH);
        appScrollT = Math.max(0, Math.min(appMax, appScrollT));
        appScroll += (appScrollT - appScroll) * 0.3f;
        R2D.endScissor(ctx);
    }
    private void addFriend() { if (Friends.add(friendInput)) { friendInput = ""; click(); } }

    // ------------------------------------------------------------------ wspólne

    private void field(DrawContext ctx, int which, float x, float y, float w, float h, String text, String placeholder, String icon) {
        boolean f = focus == which;
        float fa = anim("field" + which).animate(f || !text.isEmpty() ? 1 : 0), hv = hover("fieldh" + which, x, y, w, h);
        R2D.roundRect(ctx, x, y, w, h, h / 2f, R2D.blend(0x0EFFFFFF, 0x1AFFFFFF, Math.max(fa, hv * 0.6f)));
        R2D.roundOutline(ctx, x, y, w, h, h / 2f, 0.6f, R2D.blend(0x1AFFFFFF, R2D.withAlpha(Theme.accent(), 0.85f), f ? 1 : fa * 0.4f));
        R2D.icon(ctx, icon, x + 8, y + h / 2f - 4, 8, R2D.blend(FAINT, TEXT, fa));
        String shown = text.isEmpty() ? placeholder : text;
        Fonts.drawV(ctx, Fonts.trim(shown, Font.REGULAR, 0.66f, w - 36), x + 21, y + h / 2f + 0.5f, text.isEmpty() ? FAINT : TEXT, Font.REGULAR, 0.66f);
        if (f && (System.currentTimeMillis() / 500) % 2 == 0) {
            float cx = x + 21 + (text.isEmpty() ? 0 : Fonts.width(Fonts.trim(text, Font.REGULAR, 0.66f, w - 36), Font.REGULAR, 0.66f) + 1);
            R2D.rect(ctx, cx, y + 5, 0.7f, h - 10, TEXT);
        }
        hit(x, y, w, h, b -> focus = which);
    }

    // ================================================================== wejście

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mx = (int) Math.floor(toLocalX(mouseX)); my = (int) Math.floor(toLocalY(mouseY));
        if (closeTime >= 0) return true;
        if (picking != null) {
            // kliknięcie poza pickerem zamyka (hity pickera dodane ostatnie → mają priorytet)
        }
        if (binding != null && button != 2) binding = null;
        focus = 0;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.in(mx, my)) { h.action().accept(button); return true; }
        }
        if (picking != null) picking = null;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dX, double dY) {
        mx = (int) Math.floor(toLocalX(mouseX)); my = (int) Math.floor(toLocalY(mouseY));
        if (pickDrag > 0 && picking != null) updatePick();
        if (dragApply != null) dragApply.run();
        return true;
    }

    private void updatePick() {
        float sq = 120, sqx = pickSqX, sqy = pickSqY;
        if (pickDrag == 1) { pickHSV[1] = Anim.clamp01((mx - sqx) / sq); pickHSV[2] = 1 - Anim.clamp01((my - sqy) / sq); }
        else if (pickDrag == 2) pickHSV[0] = Anim.clamp01((my - sqy) / sq);
        else if (pickDrag == 3) pickAlpha = 1 - Anim.clamp01((my - sqy) / sq);
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
        if (page == Page.MODULES) {
            if (lx < listR) listScrollT = Math.max(0, Math.min(listMax, listScrollT - (float) v * 28));
            else setScrollT = Math.max(0, Math.min(setMax, setScrollT - (float) v * 26));
        } else appScrollT = Math.max(0, Math.min(appMax, appScrollT - (float) v * 28));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding != null) {
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) { binding.key = keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE ? -1 : keyCode; Modules.save(); }
            binding = null; click(); return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (picking != null) picking = null;
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
        if (focus == 2) { if ((Character.isLetterOrDigit(chr) || chr == '_') && friendInput.length() < 16) friendInput += chr; return true; }
        if (page != Page.MODULES) return true;
        if (focus != 1 && !Character.isLetterOrDigit(chr)) return true;
        focus = 1;
        if (query.length() < 24) { query += chr; listScroll = listScrollT = 0; listTime = System.currentTimeMillis(); }
        return true;
    }

    // uzupełnij pozycje kwadratu pickera (ustawiane przy rysowaniu)
    { }

    @Override
    public void close() { if (closeTime < 0) { closeTime = System.currentTimeMillis(); Modules.save(); Theme.save(); } }
}
