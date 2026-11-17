package dev.vesca.client.ui;

import dev.vesca.client.VescaClient;
import dev.vesca.client.module.Category;
import dev.vesca.client.module.Module;
import dev.vesca.client.module.Modules;
import dev.vesca.client.module.Setting;
import dev.vesca.client.render.Fonts;
import dev.vesca.client.render.Fonts.Font;
import dev.vesca.client.render.R2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.IntConsumer;

import static dev.vesca.client.ui.Ui.*;

/**
 * ClickGUI VescaClient (biało-czarne):
 * po lewej kategorie z przesuwaną białą pigułką, w środku siatka kart modułów (ikona, opis, klawisz, przełącznik),
 * z prawej wysuwany panel ustawień (przełączniki, suwaki, tryby, klawisz, ulubione, reset).
 * LPM – włącz/wyłącz, PPM – ustawienia, środkowy – przypisz klawisz, pisanie – szukaj, Esc – zamknij.
 */
public class ClickGui extends Screen {
    // zapamiętane między otwarciami
    private static Category category = Category.COMBAT;
    private static boolean favorites;
    private static Module inspected;

    private final long openTime = System.currentTimeMillis();
    private long closeTime = -1, listTime = System.currentTimeMillis();
    private final Map<String, Anim> anims = new HashMap<>();
    private record Hit(float x, float y, float w, float h, IntConsumer action) {
        boolean in(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; }
    }
    private final List<Hit> hits = new ArrayList<>();
    private int mx, my;
    private String query = "";
    private boolean searchFocused;
    private Module binding;
    private Setting.Num dragging;
    private float dragX, dragW;
    private float scroll, scrollTarget, maxScroll, dScroll, dScrollTarget, dMax;
    private float gx, gy, gw, gh, dx, dy, dw, dh;
    private final Anim drawer = new Anim(0, 16), catY = new Anim(-1, 18);
    private Module lastInspected;

    public ClickGui() { super(Text.literal("VescaClient")); }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }

    private Anim anim(String k) { return anims.computeIfAbsent(k, s -> new Anim(0, 16)); }
    private Anim anim(String k, float speed) { return anims.computeIfAbsent(k, s -> new Anim(0, speed)); }
    /** animacja stanu – przy pierwszym użyciu od razu w docelowej wartości */
    private float state(String k, float speed, float target) { return anims.computeIfAbsent(k, s -> new Anim(target, speed)).animate(target); }
    private boolean over(float x, float y, float w, float h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private void hit(float x, float y, float w, float h, IntConsumer a) { hits.add(new Hit(x, y, w, h, a)); }

    private void click() {
        if (client != null) client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.6f, 0.2f));
    }

    private List<Module> shown() {
        if (!query.isEmpty()) {
            String q = query.toLowerCase(Locale.ROOT);
            return Modules.all().stream().filter(m -> m.name.toLowerCase(Locale.ROOT).contains(q) || m.description().toLowerCase(Locale.ROOT).contains(q)).toList();
        }
        if (favorites) return Modules.all().stream().filter(m -> m.favorite).toList();
        return Modules.of(category);
    }

    private static String keyName(int key) {
        if (key < 0) return "";
        String s = InputUtil.fromKeyCode(key, 0).getLocalizedText().getString();
        return s.length() > 10 ? s.substring(0, 10) : s.toUpperCase(Locale.ROOT);
    }

    // ================================================================== render

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (client != null && client.world != null) applyBlur(delta);
        else Scene.render(ctx, width, height, mouseX, mouseY, 1, 0.3f);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        mx = mouseX; my = mouseY;
        hits.clear();
        long now = System.currentTimeMillis();
        float a = closeTime < 0 ? Anim.easeOutCubic((now - openTime) / 260f) : 1 - Anim.easeInCubic((now - closeTime) / 170f);
        if (closeTime >= 0 && a <= 0) { if (client != null) client.setScreen(null); return; }
        renderBackground(ctx, mouseX, mouseY, delta);
        R2D.alpha = 1;
        R2D.rect(ctx, 0, 0, width, height, R2D.withAlpha(0xFF000000, 0.45f * a));
        R2D.gradientV(ctx, 0, height * 0.6f, width, height * 0.4f, 0x00000000, R2D.withAlpha(0xFF000000, 0.35f * a));

        float W = Math.min(700, width - 32), H = Math.min(420, height - 44);
        float x0 = (width - W) / 2f, y0 = (height - H) / 2f - 6;
        float sc = 0.95f + 0.05f * a;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(width / 2f, height / 2f, 0);
        ctx.getMatrices().scale(sc, sc, 1);
        ctx.getMatrices().translate(-width / 2f, -height / 2f, 0);
        R2D.alpha = a;

        // ---- okno
        R2D.shadow(ctx, x0, y0 + 6, W, H, 30, 0xCC000000);
        R2D.glow(ctx, x0 - 40, y0 - 40, W + 80, H + 80, 0x0DFFFFFF);
        R2D.roundRectV(ctx, x0, y0, W, H, 18, 0xF4121215, 0xF809090B);
        R2D.roundOutlineGrad(ctx, x0, y0, W, H, 18, 0.7f, 0x48FFFFFF, 0x10FFFFFF);
        R2D.gradientH(ctx, x0 + 20, y0 + 0.6f, (W - 40) / 2f, 0.6f, 0x00FFFFFF, 0x55FFFFFF);
        R2D.gradientH(ctx, x0 + W / 2f, y0 + 0.6f, (W - 40) / 2f, 0.6f, 0x55FFFFFF, 0x00FFFFFF);

        boolean compact = W < 560;
        float SB = compact ? 50 : 152;
        drawSidebar(ctx, x0, y0, SB, H, compact);
        R2D.scissor(ctx, x0, y0, W, H);
        drawMain(ctx, x0 + SB, y0, W - SB, H);
        drawDrawer(ctx, x0, y0, W, H);
        R2D.endScissor(ctx);
        ctx.getMatrices().pop();

        // ---- podpowiedzi pod oknem
        float hy = y0 + H + 10;
        if (hy + 12 < height) {
            String[][] tips = {{t("LPM", "LMB"), t("włącz", "toggle")}, {t("PPM", "RMB"), t("ustawienia", "settings")},
                    {t("ŚPM", "MMB"), t("klawisz", "keybind")}, {"Esc", t("zamknij", "close")}};
            float tw = 0;
            for (String[] tp : tips) tw += Math.max(12, Fonts.width(tp[0], Font.BOLD, 0.52f) + 8) + 4 + Fonts.width(tp[1], Font.REGULAR, 0.56f) + 12;
            float tx = width / 2f - (tw - 12) / 2f;
            for (String[] tp : tips) {
                tx += keyCap(ctx, tp[0], tx, hy) + 4;
                Fonts.drawV(ctx, tp[1], tx, hy + 6, 0x99FFFFFF, Font.REGULAR, 0.56f);
                tx += Fonts.width(tp[1], Font.REGULAR, 0.56f) + 12;
            }
        }
        R2D.alpha = 1;
    }

    // ------------------------------------------------------------------ pasek boczny

    private void drawSidebar(DrawContext ctx, float x0, float y0, float SB, float H, boolean compact) {
        R2D.roundRect(ctx, x0 + 6, y0 + 6, SB - 6, H - 12, 13, 0x0BFFFFFF);
        R2D.roundOutline(ctx, x0 + 6, y0 + 6, SB - 6, H - 12, 13, 0.5f, 0x0FFFFFFF);
        float cx = x0 + 6 + (SB - 6) / 2f;
        // logo
        if (compact) logo(ctx, cx - 12, y0 + 16, 24, 0.4f);
        else {
            logo(ctx, x0 + 18, y0 + 18, 24, 0.4f);
            Fonts.draw(ctx, "VESCA", x0 + 49, y0 + 20, TEXT, Font.BOLD, 0.98f);
            spaced(ctx, "CLIENT", x0 + 49, y0 + 32, FAINT, Font.BOLD, 0.5f, 2f);
        }
        float y = y0 + 54;
        if (!compact) { spaced(ctx, t("KATEGORIE", "CATEGORIES"), x0 + 18, y, FAINT, Font.BOLD, 0.5f, 1.6f); y += 10; }
        else y += 4;

        // pozycje: kategorie + ulubione
        Category[] cats = Category.values();
        int n = cats.length + 1;
        float profileH = 40;
        float rh = Math.max(compact ? 18 : 20, Math.min(compact ? 30 : 25, (y0 + H - 6 - profileH - y - 14) / n));
        boolean showProfile = true;
        if ((y0 + H - 6 - profileH) - (y + n * rh + 10) < 0) { showProfile = false; rh = Math.max(16, Math.min(rh, (y0 + H - 14 - y - 10) / n)); }
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) ys[i] = y + i * rh + (i == cats.length ? 10 : 0);
        int active = !query.isEmpty() ? -1 : favorites ? cats.length : category.ordinal();
        if (active >= 0 && catY.raw() < 0) catY.snap(ys[active]);
        float py = catY.animate(active >= 0 ? ys[active] : catY.target());
        float pa = anim("catPill").animate(active >= 0 ? 1 : 0);
        float px = x0 + 12, pw = SB - 18;
        if (pa > 0.01f) {
            float prev = R2D.alpha;
            R2D.alpha = prev * pa;
            R2D.glow(ctx, px - 10, py - 8, pw + 20, rh + 12, 0x1AFFFFFF);
            R2D.roundRectV(ctx, px, py + 1, pw, rh - 2, compact ? 9 : (rh - 2) / 2f, 0xFFF7F7F8, 0xFFDCDCE0);
            R2D.alpha = prev;
        }
        if (!compact) R2D.rect(ctx, x0 + 18, ys[cats.length] - 6, SB - 30, 0.5f, 0x14FFFFFF);
        for (int i = 0; i < n; i++) {
            boolean fav = i == cats.length;
            String icon = fav ? "g_star" : cats[i].icon;
            String name = fav ? t("Ulubione", "Favorites") : cats[i].title();
            long cnt = fav ? Modules.all().stream().filter(m -> m.favorite).count() : Modules.enabledIn(cats[i]);
            float ry = ys[i];
            float near = pa * Anim.clamp01(1 - Math.abs(py - ry) / rh);
            float hv = anim("cat" + i).animate(over(px, ry, pw, rh) && i != active ? 1 : 0);
            if (hv > 0.01f) R2D.roundRect(ctx, px, ry + 1, pw, rh - 2, compact ? 9 : (rh - 2) / 2f, R2D.withAlpha(0xFFFFFFFF, 0.06f * hv));
            int fg = R2D.blend(R2D.blend(DIM, TEXT, hv), BLACK, near);
            if (compact) {
                float is = Math.min(11, rh - 7);
                R2D.icon(ctx, icon, px + pw / 2f - is / 2f, ry + rh / 2f - is / 2f, is, fg);
                if (cnt > 0 && !fav) R2D.circle(ctx, px + pw - 7, ry + 7, 2.2f, R2D.blend(TEXT, BLACK, near));
            } else {
                R2D.icon(ctx, icon, px + 10, ry + rh / 2f - 5, 10, fg);
                Fonts.drawV(ctx, name, px + 27, ry + rh / 2f + 0.5f, fg, near > 0.5f ? Font.BOLD : Font.SEMIBOLD, 0.74f);
                if (cnt > 0) {
                    String c = String.valueOf(cnt);
                    float cw = Math.max(13, Fonts.width(c, Font.BOLD, 0.52f) + 8);
                    R2D.roundRect(ctx, px + pw - 8 - cw, ry + rh / 2f - 5.5f, cw, 11, 5.5f, R2D.blend(0x1FFFFFFF, 0x1F000000, near));
                    Fonts.drawV(ctx, c, px + pw - 8 - cw + (cw - Fonts.width(c, Font.BOLD, 0.52f)) / 2f, ry + rh / 2f + 0.5f, R2D.blend(TEXT, BLACK, near), Font.BOLD, 0.52f);
                }
            }
            final int fi = i;
            hit(px, ry, pw, rh, b -> {
                if (b != 0) return;
                click();
                query = ""; searchFocused = false;
                if (fi == cats.length) favorites = true;
                else { favorites = false; category = cats[fi]; }
                scroll = scrollTarget = 0;
                listTime = System.currentTimeMillis();
            });
        }

        // profil na dole
        float by = y0 + H - 6 - 40;
        if (!showProfile) return;
        if (!compact) {
            R2D.rect(ctx, x0 + 18, by, SB - 30, 0.5f, 0x14FFFFFF);
            head(ctx, x0 + 18, by + 11, 20);
            String nick = client != null ? client.getSession().getUsername() : "Player";
            Fonts.draw(ctx, Fonts.trim(nick, Font.BOLD, 0.72f, SB - 60), x0 + 45, by + 12, TEXT, Font.BOLD, 0.72f);
            long on = Modules.all().stream().filter(Module::isEnabled).count();
            Fonts.draw(ctx, t(on + " włączonych", on + " enabled") + "  •  v" + VescaClient.VERSION, x0 + 45, by + 23, FAINT, Font.REGULAR, 0.56f);
        } else head(ctx, cx - 9, by + 14, 18);
    }

    // ------------------------------------------------------------------ środek: nagłówek + siatka

    private void drawMain(DrawContext ctx, float x, float y0, float w, float H) {
        float mx0 = x + 14, mw = w - 28;
        List<Module> list = shown();
        String title = !query.isEmpty() ? t("Szukaj", "Search") : favorites ? t("Ulubione", "Favorites") : category.title();
        long on = list.stream().filter(Module::isEnabled).count();
        String sub = !query.isEmpty() ? t("Wyniki dla „" + query + "”: " + list.size(), "Results for \"" + query + "\": " + list.size())
                : t(list.size() + " modułów  •  " + on + " włączonych", list.size() + " modules  •  " + on + " enabled");
        Fonts.draw(ctx, title, mx0, y0 + 17, TEXT, Font.BOLD, 1.2f);
        Fonts.draw(ctx, Fonts.trim(sub, Font.REGULAR, 0.62f, mw - 170), mx0, y0 + 33, DIM, Font.REGULAR, 0.62f);

        // wyszukiwarka
        float sw = Math.min(150, mw * 0.42f), sh = 20, sx = mx0 + mw - sw, sy = y0 + 18;
        float sf = anim("search").animate(searchFocused || !query.isEmpty() ? 1 : 0), shv = anim("searchHover").animate(over(sx, sy, sw, sh) ? 1 : 0);
        R2D.roundRect(ctx, sx, sy, sw, sh, sh / 2f, R2D.blend(0x10FFFFFF, 0x1EFFFFFF, Math.max(sf, shv * 0.6f)));
        R2D.roundOutline(ctx, sx, sy, sw, sh, sh / 2f, 0.6f, R2D.blend(0x1EFFFFFF, 0xBBFFFFFF, sf));
        R2D.icon(ctx, "g_search", sx + 8, sy + sh / 2f - 4, 8, R2D.blend(FAINT, TEXT, sf));
        String shown = query.isEmpty() ? t("Szukaj modułu…", "Search modules…") : query;
        Fonts.drawV(ctx, Fonts.trim(shown, Font.REGULAR, 0.66f, sw - 36), sx + 21, sy + sh / 2f + 0.5f, query.isEmpty() ? FAINT : TEXT, Font.REGULAR, 0.66f);
        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            float cx = sx + 21 + (query.isEmpty() ? 0 : Fonts.width(Fonts.trim(query, Font.REGULAR, 0.66f, sw - 36), Font.REGULAR, 0.66f) + 1);
            R2D.rect(ctx, cx, sy + 5, 0.7f, sh - 10, TEXT);
        }
        hit(sx, sy, sw, sh, b -> searchFocused = true);
        if (!query.isEmpty()) {
            float bx = sx + sw - 16;
            R2D.icon(ctx, "g_close", bx, sy + sh / 2f - 3.5f, 7, over(bx - 3, sy, 14, sh) ? TEXT : FAINT);
            hit(bx - 3, sy, 14, sh, b -> { query = ""; listTime = System.currentTimeMillis(); });
        }
        R2D.rect(ctx, mx0, y0 + 50, mw, 0.5f, 0x14FFFFFF);

        // siatka kart
        gx = mx0; gy = y0 + 58; gw = mw; gh = H - 58 - 12;
        int cols = gw >= 380 ? 2 : 1;
        float gap = 8, ch = 46, cw = (gw - gap * (cols - 1)) / cols;
        int rows = (list.size() + cols - 1) / cols;
        float contentH = rows * (ch + gap) - gap;
        maxScroll = Math.max(0, contentH - gh);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scroll += (scrollTarget - scroll) * 0.3f;
        R2D.scissor(ctx, gx - 4, gy - 2, gw + 8, gh + 4);
        long now = System.currentTimeMillis();
        for (int i = 0; i < list.size(); i++) {
            float cx = gx + (i % cols) * (cw + gap), cy = gy + (i / cols) * (ch + gap) - scroll;
            if (cy + ch < gy - 2 || cy > gy + gh + 2) continue;
            float e = Anim.easeOutCubic((now - listTime - i * 25) / 300f);
            float prev = R2D.alpha;
            R2D.alpha = prev * e;
            drawCard(ctx, list.get(i), cx, cy + (1 - e) * 8, cw, ch, cy >= gy - 2 && cy + ch <= gy + gh + 2 || true);
            R2D.alpha = prev;
        }
        R2D.endScissor(ctx);
        if (list.isEmpty()) {
            float cy = gy + gh / 2f;
            R2D.icon(ctx, favorites && query.isEmpty() ? "g_star" : "g_search", gx + gw / 2f - 11, cy - 26, 22, FAINT);
            Fonts.drawCentered(ctx, favorites && query.isEmpty() ? t("Brak ulubionych – dodaj gwiazdką w ustawieniach modułu", "No favorites – star a module in its settings")
                    : t("Nic nie znaleziono", "Nothing found"), gx + gw / 2f, cy + 4, DIM, Font.SEMIBOLD, 0.74f);
        }
        if (maxScroll > 0) {
            float th = Math.max(20, gh * gh / (contentH + 0.01f));
            float ty = gy + (gh - th) * (scroll / maxScroll);
            R2D.roundRect(ctx, gx + gw + 5, ty, 2.5f, th, 1.25f, 0x66FFFFFF);
        }
    }

    private void drawCard(DrawContext ctx, Module m, float x, float y, float w, float h, boolean clickable) {
        boolean inGrid = my >= gy && my < gy + gh;
        float hv = anim("hv" + m.name).animate(inGrid && over(x, y, w, h) && drawer.raw() < 0.5f || inspected == m ? 1 : 0);
        float on = state("on" + m.name, 18, m.isEnabled() ? 1 : 0);
        float sel = anim("sel" + m.name).animate(inspected == m ? 1 : 0);
        R2D.roundRect(ctx, x, y, w, h, 11, R2D.blend(R2D.blend(0x0AFFFFFF, 0x15FFFFFF, hv), 0x1CFFFFFF, on * 0.8f));
        R2D.roundOutline(ctx, x, y, w, h, 11, 0.6f, R2D.blend(R2D.blend(0x12FFFFFF, 0x2AFFFFFF, hv), 0x80FFFFFF, Math.max(on * 0.55f, sel)));
        // ikona w kafelku
        float ts = 30, tx = x + 8, ty = y + (h - ts) / 2f;
        if (on > 0.01f) R2D.glow(ctx, tx - 10, ty - 10, ts + 20, ts + 20, R2D.withAlpha(0xFFFFFFFF, 0.16f * on));
        R2D.roundRectV(ctx, tx, ty, ts, ts, 9, R2D.blend(0xFF202024, 0xFFFFFFFF, on), R2D.blend(0xFF18181B, 0xFFD9D9DE, on));
        R2D.roundOutline(ctx, tx, ty, ts, ts, 9, 0.5f, R2D.blend(0x1CFFFFFF, 0x00FFFFFF, on));
        R2D.icon(ctx, m.icon, tx + ts / 2f - 6.5f, ty + ts / 2f - 6.5f, 13, R2D.blend(0xFFD4D4D8, BLACK, on));
        // tekst
        float lx = tx + ts + 10;
        float right = 40;
        boolean binding = this.binding == m;
        String kn = binding ? "…" : keyName(m.key);
        float kw = kn.isEmpty() ? 0 : Math.max(14, Fonts.width(kn, Font.BOLD, 0.52f) + 8);
        if (kw > 0) right += kw + 6;
        Fonts.draw(ctx, m.name, lx, y + h / 2f - 9.5f, R2D.blend(0xFFE4E4E7, 0xFFFFFFFF, on), Font.BOLD, 0.82f);
        if (m.favorite) R2D.icon(ctx, "g_star", lx + Fonts.width(m.name, Font.BOLD, 0.82f) + 4, y + h / 2f - 8.5f, 6.5f, DIM);
        Fonts.draw(ctx, Fonts.trim(m.description(), Font.REGULAR, 0.6f, w - (lx - x) - right), lx, y + h / 2f + 2.5f, DIM, Font.REGULAR, 0.6f);
        // klawisz
        float sx = x + w - 32;
        if (kw > 0) {
            float kx = sx - 6 - kw, ky = y + h / 2f - 6;
            int bg = binding ? R2D.withAlpha(0xFFFFFFFF, 0.25f + 0.25f * (float) Math.sin(System.currentTimeMillis() / 150.0)) : 0x1CFFFFFF;
            R2D.roundRect(ctx, kx, ky, kw, 12, 4, bg);
            R2D.roundOutline(ctx, kx, ky, kw, 12, 4, 0.5f, 0x26FFFFFF);
            Fonts.drawV(ctx, kn, kx + (kw - Fonts.width(kn, Font.BOLD, 0.52f)) / 2f, ky + 6.5f, TEXT, Font.BOLD, 0.52f);
        }
        // przełącznik
        float swy = y + h / 2f - 5.5f;
        toggle(ctx, sx, swy, on, hv);
        if (!clickable) return;
        hit(x, Math.max(y, gy), w, Math.min(y + h, gy + gh) - Math.max(y, gy), b -> {
            if (my < gy || my >= gy + gh) return;
            if (b == 0) { m.toggle(); click(); Modules.save(); }
            else if (b == 1) openDrawer(m);
            else if (b == 2) { this.binding = m; click(); }
        });
    }

    private void openDrawer(Module m) {
        if (inspected != m) { dScroll = dScrollTarget = 0; }
        inspected = m;
        lastInspected = m;
        click();
    }

    // ------------------------------------------------------------------ panel ustawień

    private void drawDrawer(DrawContext ctx, float x0, float y0, float W, float H) {
        float da = drawer.animate(inspected != null ? 1 : 0);
        Module m = inspected != null ? inspected : lastInspected;
        if (da < 0.01f || m == null) return;
        dw = Math.min(250, W * 0.44f);
        dx = x0 + W - dw * Anim.easeOutCubic(da);
        dy = y0; dh = H;
        float prev = R2D.alpha;
        // przyciemnienie siatki pod panelem
        R2D.rect(ctx, x0 + 1, y0 + 1, W - dw * da - 1, H - 2, R2D.withAlpha(0xFF000000, 0.35f * da));
        hit(x0, y0, W - dw * da, H, b -> inspected = null);
        R2D.shadow(ctx, dx - 4, dy, dw, dh, 20, R2D.withAlpha(0xFF000000, 0.7f * da));
        R2D.roundRect(ctx, dx, dy, dw, dh, 18, 0xFC0C0C0E);
        R2D.roundOutline(ctx, dx, dy, dw, dh, 18, 0.6f, 0x2AFFFFFF);
        hit(dx, dy, dw, dh, b -> {});

        float px = dx + 14, pw = dw - 28;
        float y = dy + 16;
        // nagłówek
        float on = state("on" + m.name, 18, m.isEnabled() ? 1 : 0);
        R2D.roundRectV(ctx, px, y, 32, 32, 10, R2D.blend(0xFF202024, 0xFFFFFFFF, on), R2D.blend(0xFF18181B, 0xFFD9D9DE, on));
        R2D.icon(ctx, m.icon, px + 9, y + 9, 14, R2D.blend(0xFFD4D4D8, BLACK, on));
        Fonts.draw(ctx, Fonts.trim(m.name, Font.BOLD, 1.0f, pw - 70), px + 42, y + 4, TEXT, Font.BOLD, 1.0f);
        String cat = m.category.title().toUpperCase(Locale.ROOT);
        float cw = Fonts.width(cat, Font.BOLD, 0.5f) + 10;
        R2D.roundRect(ctx, px + 42, y + 18, cw, 11, 5.5f, 0x1CFFFFFF);
        Fonts.drawV(ctx, cat, px + 47, y + 23.5f, DIM, Font.BOLD, 0.5f);
        float cbx = dx + dw - 14 - 20, cby = y + 6;
        float ch = anim("drawerClose").animate(over(cbx, cby, 20, 20) ? 1 : 0);
        R2D.roundRect(ctx, cbx, cby, 20, 20, 10, R2D.blend(0x14FFFFFF, 0xFFF4F4F5, ch));
        R2D.icon(ctx, "g_close", cbx + 6, cby + 6, 8, R2D.blend(TEXT, BLACK, ch));
        hit(cbx, cby, 20, 20, b -> inspected = null);
        y += 42;
        for (String l : Fonts.wrap(m.description(), Font.REGULAR, 0.66f, pw)) {
            Fonts.draw(ctx, l, px, y, DIM, Font.REGULAR, 0.66f);
            y += 10;
        }
        y += 6;

        // przewijana zawartość
        float top = y, areaH = dy + dh - 40 - top;
        R2D.scissor(ctx, dx, top, dw, areaH);
        float cy = top - dScroll;
        // stan
        cy = row(ctx, px, cy, pw, t("Włączony", "Enabled"), null);
        Ui.toggle(ctx, px + pw - 20, cy - 16, on, anim("rowOn").animate(over(px, cy - 22, pw, 22) ? 1 : 0));
        hit(px, cy - 22, pw, 22, b -> { if (b == 0) { m.toggle(); click(); Modules.save(); } });
        // klawisz
        cy = row(ctx, px, cy, pw, t("Klawisz", "Keybind"), null);
        String kn = binding == m ? t("Naciśnij klawisz…", "Press a key…") : m.key < 0 ? t("Brak", "None") : keyName(m.key);
        float kw = Fonts.width(kn, Font.BOLD, 0.6f) + 14;
        float kx = px + pw - kw, ky = cy - 17;
        float kh = anim("rowKey").animate(over(kx, ky, kw, 13) || binding == m ? 1 : 0);
        R2D.roundRect(ctx, kx, ky, kw, 13, 6.5f, R2D.blend(0x18FFFFFF, 0xFFF4F4F5, kh));
        Fonts.drawV(ctx, kn, kx + 7, ky + 7, R2D.blend(TEXT, BLACK, kh), Font.BOLD, 0.6f);
        hit(px, cy - 22, pw, 22, b -> { binding = binding == m ? null : m; click(); });
        // ulubiony
        cy = row(ctx, px, cy, pw, t("Ulubiony", "Favorite"), null);
        float fv = state("fav" + m.name, 16, m.favorite ? 1 : 0);
        R2D.icon(ctx, "g_star", px + pw - 13, cy - 16, 11, R2D.blend(0x44FFFFFF, 0xFFFFFFFF, fv));
        hit(px, cy - 22, pw, 22, b -> { m.favorite = !m.favorite; click(); Modules.save(); });

        if (!m.settings.isEmpty()) {
            cy += 8;
            spaced(ctx, t("USTAWIENIA", "SETTINGS"), px, cy, FAINT, Font.BOLD, 0.5f, 1.6f);
            cy += 12;
            for (Setting<?> s : m.settings) cy = setting(ctx, m, s, px, cy, pw);
        }
        float content = cy + dScroll - top;
        dMax = Math.max(0, content - areaH + 6);
        dScrollTarget = Math.max(0, Math.min(dMax, dScrollTarget));
        dScroll += (dScrollTarget - dScroll) * 0.3f;
        R2D.endScissor(ctx);

        // reset na dole
        float bw = pw, bh = 22, bx = px, by = dy + dh - 32;
        float rh = anim("reset").animate(over(bx, by, bw, bh) ? 1 : 0);
        button(ctx, bx, by, bw, bh, "g_reset", t("Przywróć domyślne", "Reset to default"), false, rh, true);
        hit(bx, by, bw, bh, b -> { if (b == 0) { m.reset(); click(); Modules.save(); } });
        R2D.alpha = prev;
    }

    /** wiersz z etykietą; zwraca y pod wierszem */
    private float row(DrawContext ctx, float x, float y, float w, String label, String value) {
        Fonts.drawV(ctx, label, x, y + 11, TEXT, Font.SEMIBOLD, 0.72f);
        if (value != null) Fonts.drawV(ctx, value, x + w - Fonts.width(value, Font.BOLD, 0.66f), y + 11, DIM, Font.BOLD, 0.66f);
        R2D.rect(ctx, x, y + 22, w, 0.5f, 0x0DFFFFFF);
        return y + 22;
    }

    private float setting(DrawContext ctx, Module m, Setting<?> s, float x, float y, float w) {
        String key = m.name + "." + s.name;
        if (s instanceof Setting.Bool b) {
            float hv = anim("h" + key).animate(over(x, y, w, 22) ? 1 : 0);
            float on = state("b" + key, 18, b.get() ? 1 : 0);
            Fonts.drawV(ctx, s.name, x, y + 11, R2D.blend(DIM, TEXT, Math.max(hv, on)), Font.SEMIBOLD, 0.7f);
            Ui.toggle(ctx, x + w - 20, y + 5.5f, on, hv);
            hit(x, y, w, 22, bt -> { if (bt == 0) { b.toggle(); click(); Modules.save(); } });
            return y + 24;
        }
        if (s instanceof Setting.Num n) {
            float hv = anim("h" + key).animate(over(x, y, w, 30) || dragging == n ? 1 : 0);
            Fonts.drawV(ctx, s.name, x, y + 8, R2D.blend(DIM, TEXT, hv), Font.SEMIBOLD, 0.7f);
            String v = n.text();
            float vw = Fonts.width(v, Font.BOLD, 0.62f) + 10;
            R2D.roundRect(ctx, x + w - vw, y + 2, vw, 12, 6, R2D.blend(0x14FFFFFF, 0x24FFFFFF, hv));
            Fonts.drawV(ctx, v, x + w - vw + 5, y + 8.5f, TEXT, Font.BOLD, 0.62f);
            float ty = y + 21, th = 4;
            float f = state("v" + key, 22, n.fraction());
            R2D.roundRect(ctx, x, ty, w, th, 2, 0x1AFFFFFF);
            R2D.roundRectH(ctx, x, ty, Math.max(th, w * f), th, 2, 0xFF9A9AA2, 0xFFFFFFFF);
            float kx = x + w * f;
            if (hv > 0.01f) R2D.glow(ctx, kx - 9, ty + th / 2f - 9, 18, 18, R2D.withAlpha(0xFFFFFFFF, 0.35f * hv));
            R2D.circle(ctx, kx, ty + th / 2f, 4.2f + hv * 0.8f, 0xFFFFFFFF);
            R2D.circle(ctx, kx, ty + th / 2f, 1.6f, BLACK);
            hit(x - 4, y, w + 8, 30, bt -> { if (bt == 0) { dragging = n; dragX = x; dragW = w; updateDrag(); } });
            return y + 32;
        }
        if (s instanceof Setting.Mode o) {
            Fonts.drawV(ctx, s.name, x, y + 8, DIM, Font.SEMIBOLD, 0.7f);
            float cy = y + 18, cx = x;
            for (String mode : o.modes) {
                float tw = Fonts.width(mode, Font.BOLD, 0.6f) + 14;
                if (cx + tw > x + w && cx > x) { cx = x; cy += 18; }
                boolean act = o.get().equals(mode);
                float sa = state("m" + key + mode, 16, act ? 1 : 0);
                float hv = anim("mh" + key + mode).animate(over(cx, cy, tw, 15) ? 1 : 0);
                R2D.roundRect(ctx, cx, cy, tw, 15, 7.5f, R2D.blend(R2D.blend(0x12FFFFFF, 0x24FFFFFF, hv), 0xFFF4F4F5, sa));
                if (sa < 0.99f) R2D.roundOutline(ctx, cx, cy, tw, 15, 7.5f, 0.5f, R2D.withAlpha(0xFFFFFFFF, 0.12f * (1 - sa)));
                Fonts.drawV(ctx, mode, cx + 7, cy + 8, R2D.blend(R2D.blend(DIM, TEXT, hv), BLACK, sa), Font.BOLD, 0.6f);
                hit(cx, cy, tw, 15, bt -> { if (bt == 0) { o.set(mode); click(); Modules.save(); } });
                cx += tw + 5;
            }
            return cy + 22;
        }
        return y;
    }

    private void updateDrag() {
        if (dragging == null) return;
        float f = Anim.clamp01((mx - dragX) / dragW);
        dragging.set(dragging.min + (dragging.max - dragging.min) * f);
    }

    // ================================================================== wejście

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mx = (int) mouseX; my = (int) mouseY;
        if (closeTime >= 0) return true;
        if (binding != null && button != 2) { binding = null; }
        searchFocused = false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.in(mouseX, mouseY)) { h.action().accept(button); return true; }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dX, double dY) {
        mx = (int) mouseX; my = (int) mouseY;
        updateDrag();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) { updateDrag(); dragging = null; Modules.save(); }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        if (inspected != null && mouseX >= dx) dScrollTarget = Math.max(0, Math.min(dMax, dScrollTarget - (float) v * 26));
        else scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget - (float) v * 30));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) binding = null;
            else {
                binding.key = keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE ? -1 : keyCode;
                binding = null;
                Modules.save();
            }
            click();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (searchFocused || !query.isEmpty()) { query = ""; searchFocused = false; listTime = System.currentTimeMillis(); }
            else if (inspected != null) inspected = null;
            else close();
            return true;
        }
        if (searchFocused && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!query.isEmpty()) { query = query.substring(0, query.length() - 1); listTime = System.currentTimeMillis(); }
            return true;
        }
        if (!searchFocused && VescaClient.OPEN_GUI != null && VescaClient.OPEN_GUI.matchesKey(keyCode, scanCode)) { close(); return true; }
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (binding != null || chr < ' ' || chr == 127) return true;
        if (!searchFocused && !Character.isLetterOrDigit(chr)) return true;
        searchFocused = true;
        if (query.length() < 24) { query += chr; scroll = scrollTarget = 0; listTime = System.currentTimeMillis(); }
        return true;
    }

    @Override
    public void close() {
        if (closeTime < 0) { closeTime = System.currentTimeMillis(); Modules.save(); }
    }
}
