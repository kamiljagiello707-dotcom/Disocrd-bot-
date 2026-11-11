package tech.fuzir.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;
import tech.fuzir.client.FuzirClient;
import tech.fuzir.client.config.Config;
import tech.fuzir.client.config.Profiles;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.module.ModuleManager;
import tech.fuzir.client.module.impl.ClickGui;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.setting.KeySetting;
import tech.fuzir.client.setting.ModeSetting;
import tech.fuzir.client.setting.NumberSetting;
import tech.fuzir.client.setting.Setting;
import tech.fuzir.client.util.Anim;
import tech.fuzir.client.util.Canvas;
import tech.fuzir.client.util.Canvas.F;
import tech.fuzir.client.util.Icons;
import tech.fuzir.client.util.Theme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;

public class ClickGuiScreen extends Screen {
    // wymiary w jednostkach (przy 1080p jedna jednostka to 2 piksele)
    private static final float W = 700, H = 420, SIDE = 178;
    private static final float ROW_H = 48, ROW_GAP = 4;

    // ------------------------------------------------- strony menu

    private enum Kind { SEARCH, CATEGORY, CONFIGS, THEMES }

    private record Page(Kind kind, Category category, String title, String subtitle, char icon) {
    }

    private static final List<Page> PAGES = new ArrayList<>();

    static {
        PAGES.add(new Page(Kind.SEARCH, null, "Szukaj", "Wszystkie moduły w jednym miejscu", Icons.SEARCH));
        for (Category c : Category.values()) PAGES.add(new Page(Kind.CATEGORY, c, c.title, c.subtitle, c.icon));
        PAGES.add(new Page(Kind.CONFIGS, null, "Configi", "Zapisuj i wczytuj zestawy ustawień", Icons.FOLDER));
        PAGES.add(new Page(Kind.THEMES, null, "Motywy", "Kolory menu i HUD", Icons.PALETTE));
    }

    private static int page = 1;
    private static final Map<Integer, Float> SAVED_SCROLL = new HashMap<>();

    // ------------------------------------------------- stan

    private enum Focus { NONE, SEARCH, PROFILE }

    private final Anim open = new Anim(0);
    private final Anim indicator = new Anim(-1);
    private final Anim scroll = new Anim(0);
    private final Anim content = new Anim(0);
    private final Anim toastAnim = new Anim(0);
    private final Map<Object, Anim> anims = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();

    private Theme t;
    private boolean closing;
    private float scrollTarget;
    private String query = "", profileName = "";
    private Focus focus = Focus.NONE;
    private Module expanded;
    private KeySetting listening;
    private NumberSetting dragging;
    private float dragX, dragW;
    private String confirmDelete;
    private String toast = "";
    private long toastAt;
    private List<Profiles.Profile> profiles = List.of();
    private float u, wx, wy, mx, my;

    private record Hit(float x, float y, float w, float h, IntConsumer action) {
        boolean in(float px, float py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }

    public ClickGuiScreen() {
        super(Text.literal("Fuzir Client"));
        scrollTarget = SAVED_SCROLL.getOrDefault(page, 0f);
        scroll.set(scrollTarget);
        if (PAGES.get(page).kind == Kind.CONFIGS) profiles = Profiles.list();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        closing = true;
        focus = Focus.NONE;
        listening = null;
    }

    @Override
    public void removed() {
        SAVED_SCROLL.put(page, scrollTarget);
        Config.save();
    }

    private ClickGui cfg() {
        return ModuleManager.get(ClickGui.class);
    }

    private Anim anim(Object key) {
        return anims.computeIfAbsent(key, k -> new Anim(0));
    }

    private float hover(Object key, boolean on) {
        return anim(key).to(on ? 1 : 0, 16);
    }

    // ================================================= rysowanie

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // tło rysujemy w render(), żeby dało się je wyciszyć animacją
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Anim.speedMul = cfg().speed.f();
        float o = open.to(closing ? 0 : 1, closing ? 15 : 10);
        if (closing && o < 0.02f) {
            client.setScreen(null);
            return;
        }
        t = Theme.current();
        if (cfg().blur.on() && o > 0.5f) super.renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(0, 0, width, height, Canvas.argb((int) (100 * o), 0));

        double sf = client.getWindow().getScaleFactor();
        float fbW = client.getWindow().getFramebufferWidth(), fbH = client.getWindow().getFramebufferHeight();
        u = fbH / 540f * cfg().guiScale.f();
        u = Math.min(u, Math.min(fbW * 0.96f / W, fbH * 0.94f / H));

        Canvas cv = Canvas.begin(ctx, u);
        cv.alpha = o;
        wx = (cv.width() - W) / 2f;
        wy = (cv.height() - H) / 2f;
        mx = (float) (mouseX * sf / u);
        my = (float) (mouseY * sf / u);

        float e = Anim.easeOutCubic(o);
        MatrixStack m = ctx.getMatrices();
        float sc = 0.94f + 0.06f * e;
        m.translate((wx + W / 2) * u, (wy + H / 2) * u, 0);
        m.scale(sc, sc, 1);
        m.translate(-(wx + W / 2) * u, -(wy + H / 2) * u + (1 - e) * 16 * u, 0);

        hits.clear();
        cv.shadow(wx, wy + 12, W, H, 20, 40, 0xB4000000);
        cv.round(wx, wy, W, H, 20, t.bg());
        cv.round(wx, wy, SIDE, H, 20, t.sidebar());
        cv.rect(wx + SIDE - 20, wy, 20, H, t.sidebar());
        cv.rect(wx + SIDE, wy, 1f / u, H, t.line());
        cv.outline(wx, wy, W, H, 20, 1f, t.line());

        drawSidebar(cv);
        drawHeader(cv);
        switch (PAGES.get(page).kind) {
            case SEARCH, CATEGORY -> drawModules(cv);
            case CONFIGS -> drawConfigs(cv);
            case THEMES -> drawThemes(cv);
        }
        drawToast(cv);
        cv.end();
    }

    // ------------------------------------------------- logo

    /** Znak projektu: litera F zbudowana z zaokrąglonych belek i iskra w rogu. */
    public static void drawLogo(Canvas cv, float x, float y, float s, int bg, int fg) {
        float k = s / 32f;
        cv.shadow(x, y + 2 * k, s, s, 9 * k, 8, Canvas.mulAlpha(bg, 0.35f));
        cv.roundGradient(x, y, s, s, 9 * k, bg, Canvas.mix(bg, 0xFF000000, 0.12f));
        cv.round(x + 9 * k, y + 7 * k, 4.5f * k, 18 * k, 2.25f * k, fg);
        cv.round(x + 9 * k, y + 7 * k, 14.5f * k, 4.5f * k, 2.25f * k, fg);
        cv.round(x + 9 * k, y + 14.5f * k, 10.5f * k, 4 * k, 2 * k, fg);
        cv.circle(x + 23.5f * k, y + 22.5f * k, 2.4f * k, fg);
    }

    // ------------------------------------------------- panel boczny

    private void drawSidebar(Canvas cv) {
        float x = wx + 20, y = wy + 22;
        drawLogo(cv, x, y, 32, t.accent(), t.onAccent());
        cv.text("Fuzir", x + 42, y + 5, t.text(), F.HEADING);
        float fw = cv.width("Fuzir", F.HEADING);
        cv.text("2.5", x + 45 + fw, y + 3, t.accent(), F.SMALL);
        cv.text("client", x + 42, y + 21, t.muted(), F.SMALL);

        float rowY = wy + 76, step = 28, rx = wx + 12, rw = SIDE - 24, rh = 25;
        if (indicator.get() < 0) indicator.set(slot(page));
        float ind = indicator.to(slot(page), 16);

        for (int i = 0; i < PAGES.size(); i++) {
            float ry = rowY + slot(i) * step;
            float hv = hover("nav" + i, inside(rx, ry, rw, rh));
            if (hv > 0.01f) cv.round(rx, ry, rw, rh, 9, Canvas.mulAlpha(t.surface2(), hv));
        }
        // podświetlenie aktywnej strony przesuwa się płynnie
        float py = rowY + ind * step;
        cv.shadow(rx, py + 3, rw, rh, 9, 9, Canvas.mulAlpha(t.accent(), 0.3f));
        cv.round(rx, py, rw, rh, 9, t.accent());

        for (int i = 0; i < PAGES.size(); i++) {
            Page p = PAGES.get(i);
            float ry = rowY + slot(i) * step, cy = ry + rh / 2;
            if (i == 1) cv.text("MODUŁY", rx + 4, ry - 9, t.dim(), F.SMALL);
            if (p.kind == Kind.CONFIGS) cv.text("KLIENT", rx + 4, ry - 9, t.dim(), F.SMALL);
            float on = Math.max(0, 1 - Math.abs(ind - slot(i)));
            int col = Canvas.mix(Canvas.mix(t.muted(), t.text(), anim("nav" + i).get()), t.onAccent(), on);
            cv.icon(p.icon, rx + 16, cy, col, F.ICON);
            cv.textMid(p.title, rx + 31, cy, col, F.REGULAR);
            if (p.kind == Kind.CATEGORY) {
                List<Module> list = ModuleManager.in(p.category);
                long all = list.stream().filter(Module::hasToggle).count();
                if (all > 0) {
                    long en = list.stream().filter(mo -> mo.hasToggle() && mo.isEnabled()).count();
                    cv.textRight(en + "/" + all, rx + rw - 10, cy, Canvas.mix(t.dim(), Canvas.mulAlpha(t.onAccent(), 0.55f), on), F.SMALL);
                }
            }
            final int idx = i;
            hit(rx, ry, rw, rh, b -> selectPage(idx));
        }

        // edytor HUD
        float bx = wx + 12, by = wy + H - 82, bw = SIDE - 24, bh = 28;
        float eh = hover("edit", inside(bx, by, bw, bh));
        cv.round(bx, by, bw, bh, 9, Canvas.mix(t.surface(), t.surface2(), eh));
        cv.outline(bx, by, bw, bh, 9, 1f, t.line());
        cv.icon(Icons.LAYOUT, bx + 16, by + bh / 2, Canvas.mix(t.muted(), t.text(), eh), F.ICON);
        cv.textMid("Edytuj HUD", bx + 31, by + bh / 2, t.text(), F.REGULAR);
        cv.icon(Icons.CHEVRON, bx + bw - 13 + eh * 2, by + bh / 2, Canvas.mix(t.dim(), t.text(), eh), F.ICON_SM);
        hit(bx, by, bw, bh, b -> client.setScreen(new HudEditorScreen()));

        // gracz
        float uy = wy + H - 44;
        String nick = client.getSession().getUsername();
        cv.circle(wx + 27, uy + 14, 12, t.surface2());
        cv.outline(wx + 15, uy + 2, 24, 24, 12, 1f, t.line());
        cv.textCenter(nick.substring(0, 1).toUpperCase(Locale.ROOT), wx + 27.3f, uy + 14, t.text(), F.BOLD);
        cv.circle(wx + 36, uy + 23, 3.8f, t.sidebar());
        cv.circle(wx + 36, uy + 23, 2.5f, 0xFF5BE38C);
        cv.text(cv.trim(nick, SIDE - 64, F.REGULAR), wx + 46, uy + 7, t.text(), F.REGULAR);
        String key = FuzirClient.OPEN_KEY.getBoundKeyLocalizedText().getString();
        cv.text(cv.trim(key + " otwiera menu", SIDE - 64, F.SMALL), wx + 46, uy + 19, t.muted(), F.SMALL);
    }

    /** Pozycja w pionie z przerwą przed sekcją "Klient". */
    private static float slot(int i) {
        float s = i;
        if (i >= 1) s += 0.45f;
        if (PAGES.get(i).kind == Kind.CONFIGS || PAGES.get(i).kind == Kind.THEMES) s += 0.45f;
        return s;
    }

    // ------------------------------------------------- nagłówek

    private void drawHeader(Canvas cv) {
        Page p = PAGES.get(page);
        float x = wx + SIDE + 26, right = wx + W - 24;
        float c = Anim.easeOutCubic(content.to(1, 9));
        float prev = cv.alpha;
        cv.alpha = prev * c;
        cv.text(p.title, x - (1 - c) * 10, wy + 24, t.text(), F.TITLE);
        String sub = p.subtitle;
        if (p.kind == Kind.CATEGORY) {
            List<Module> list = ModuleManager.in(p.category);
            sub += "  ·  " + list.size() + " " + plural(list.size());
        }
        cv.text(sub, x, wy + 45, t.muted(), F.SMALL);
        cv.alpha = prev;

        // szybkie wyszukiwanie w nagłówku (na stronach kategorii)
        if (p.kind == Kind.CATEGORY) {
            float sw = 170, sh = 28, sx = right - sw, sy = wy + 22;
            boolean h = inside(sx, sy, sw, sh);
            cv.round(sx, sy, sw, sh, 14, h ? t.surface2() : t.surface());
            cv.outline(sx, sy, sw, sh, 14, 1f, t.line());
            cv.icon(Icons.SEARCH, sx + 15, sy + sh / 2, t.muted(), F.ICON_SM);
            cv.textMid("Szukaj modułów", sx + 27, sy + sh / 2, t.dim(), F.REGULAR);
            String hint = "Ctrl F";
            float hw = cv.width(hint, F.SMALL) + 10;
            cv.round(sx + sw - 8 - hw, sy + 7, hw, 14, 5, t.surface2());
            cv.textCenter(hint, sx + sw - 8 - hw / 2, sy + 14, t.muted(), F.SMALL);
            hit(sx, sy, sw, sh, b -> openSearch());
        }
    }

    // ------------------------------------------------- lista modułów

    private List<Module> modules() {
        Page p = PAGES.get(page);
        if (p.kind == Kind.CATEGORY) return ModuleManager.in(p.category);
        if (query.isEmpty()) return ModuleManager.all();
        String q = query.toLowerCase(Locale.ROOT);
        return ModuleManager.all().stream()
                .filter(mo -> mo.name.toLowerCase(Locale.ROOT).contains(q) || mo.description.toLowerCase(Locale.ROOT).contains(q)
                        || mo.category.title.toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    private void drawModules(Canvas cv) {
        Page p = PAGES.get(page);
        float x = wx + SIDE + 26, right = wx + W - 24, w = right - x;
        float top = wy + 66;
        if (p.kind == Kind.SEARCH) {
            drawInput(cv, Focus.SEARCH, query, "Wpisz nazwę modułu, opis albo kategorię...", Icons.SEARCH, x, top, w, 34);
            top += 44;
        }
        float h = wy + H - 14 - top;
        List<Module> list = modules();

        float total = 0;
        for (Module mo : list) total += rowHeight(mo, w) + ROW_GAP;
        float max = Math.max(0, total - h + 6);
        scrollTarget = Math.max(0, Math.min(max, scrollTarget));
        float s = scroll.to(scrollTarget, 16);

        float c = Anim.easeOutCubic(content.get());
        float prev = cv.alpha;
        cv.scissor(x - 6, top, w + 12, h);
        float y = top + 2 - s;
        for (int i = 0; i < list.size(); i++) {
            Module mo = list.get(i);
            float rh = rowHeight(mo, w);
            float delay = Math.max(0, Math.min(1, c * 1.7f - i * 0.09f));
            float yy = y + (1 - Anim.easeOutCubic(delay)) * 12;
            if (yy + rh > top - 4 && yy < top + h + 4) {
                cv.alpha = prev * delay;
                drawRow(cv, mo, x, yy, w, rh, top, h, p.kind == Kind.SEARCH);
                cv.alpha = prev;
            }
            y += rh + ROW_GAP;
        }
        if (list.isEmpty()) {
            float cy = top + h / 2 - 24;
            cv.circle(x + w / 2, cy, 20, t.surface());
            cv.icon(Icons.SEARCH, x + w / 2, cy, t.muted(), F.ICON_LG);
            cv.textCenter("Nic nie znaleziono", x + w / 2, cy + 34, t.text(), F.BOLD);
            cv.textCenter("Spróbuj innej nazwy modułu", x + w / 2, cy + 47, t.muted(), F.SMALL);
        }
        cv.endScissor();
        cv.alpha = prev;
        fades(cv, x, top, w, h, s, max);
        scrollbar(cv, top, h, s, max, total);
    }

    private void fades(Canvas cv, float x, float top, float w, float h, float s, float max) {
        int bg = t.bg() | 0xFF000000;
        if (s > 1) cv.roundGradient(x - 6, top, w + 12, 14, 0, bg, bg & 0x00FFFFFF);
        if (s < max - 1) cv.roundGradient(x - 6, top + h - 14, w + 12, 14, 0, bg & 0x00FFFFFF, bg);
    }

    private void scrollbar(Canvas cv, float top, float h, float s, float max, float total) {
        if (max <= 0) return;
        float barH = Math.max(28, h * h / (total + 6));
        cv.round(wx + W - 11, top + (h - barH) * (s / max), 3, barH, 1.5f, Canvas.mulAlpha(t.text(), 0.18f));
    }

    private List<Setting<?>> settingsOf(Module mo) {
        List<Setting<?>> out = new ArrayList<>();
        for (Setting<?> s : mo.settings) if (s.isVisible()) out.add(s);
        if (mo.hasToggle()) {
            out.add(mo.bind);
            out.add(mo.bindMode);
        }
        return out;
    }

    private static float settingHeight(Setting<?> s) {
        if (s instanceof BoolSetting) return 26;
        if (s instanceof NumberSetting) return 38;
        if (s instanceof ModeSetting) return 44;
        if (s instanceof KeySetting) return 46;
        return 0;
    }

    /** Ustawienia w dwóch kolumnach: wysokość pary to wyższe z dwóch. */
    private float settingsHeight(Module mo) {
        List<Setting<?>> list = settingsOf(mo);
        float h = 14;
        for (int i = 0; i < list.size(); i += 2) {
            float a = settingHeight(list.get(i)), b = i + 1 < list.size() ? settingHeight(list.get(i + 1)) : 0;
            h += Math.max(a, b);
        }
        return h + 4;
    }

    private float rowHeight(Module mo, float w) {
        return ROW_H + settingsHeight(mo) * anim("exp" + mo.name).get();
    }

    private void drawRow(Canvas cv, Module mo, float x, float y, float w, float h, float clipY, float clipH, boolean showCategory) {
        boolean visible = my >= clipY && my <= clipY + clipH;
        boolean hovered = visible && inside(x, y, w, ROW_H);
        float hv = mo.hoverAnim.to(hovered ? 1 : 0, 16);
        float on = mo.toggleAnim.to(mo.isEnabled() && mo.hasToggle() ? 1 : 0, 14);
        float ex = Anim.easeOutCubic(anim("exp" + mo.name).to(expanded == mo ? 1 : 0, 14));

        float bgA = Math.max(hv, ex);
        cv.round(x, y, w, h, 12, Canvas.mix(Canvas.mulAlpha(t.surface(), 0.55f), t.surface(), bgA));
        if (ex > 0.01f || on > 0.01f) cv.outline(x, y, w, h, 12, 1f, Canvas.mulAlpha(t.line(), Math.max(ex, on * 0.8f) * 1.6f));
        // znacznik włączonego modułu
        if (on > 0.01f) {
            cv.shadow(x + 1, y + ROW_H / 2 - 9, 3, 18, 1.5f, 5, Canvas.mulAlpha(t.accent(), on * 0.6f));
            cv.round(x + 1, y + ROW_H / 2 - 9 * on, 3, 18 * on, 1.5f, t.accent());
        }

        float tx = x + 18;
        int nameCol = Canvas.mix(Canvas.mix(t.muted(), t.text(), hv * 0.6f), t.text(), on);
        boolean desc = cfg().descriptions.on();
        float nameY = desc ? y + 13 : y + ROW_H / 2 - 4;
        cv.text(mo.name, tx, nameY, nameCol, F.MEDIUM);
        float after = tx + cv.width(mo.name, F.MEDIUM) + 8;
        if (showCategory) {
            String cat = mo.category.title;
            float cw = cv.width(cat, F.SMALL) + 12;
            cv.round(after, nameY - 1.5f, cw, 12, 4, t.surface2());
            cv.textCenter(cat, after + cw / 2, nameY + 4.5f, t.muted(), F.SMALL);
            after += cw + 6;
        }
        if (!mo.hasToggle()) {
            cv.round(after, nameY - 1.5f, cv.width("ustawienia", F.SMALL) + 12, 12, 4, t.surface2());
            cv.textMid("ustawienia", after + 6, nameY + 4.5f, t.muted(), F.SMALL);
        }

        float right = x + w - 16;
        float cy = y + ROW_H / 2;
        // strzałka rozwijania (obraca się o 90°)
        boolean ah = visible && inside(right - 18, cy - 10, 20, 20);
        float aa = hover("arrow" + mo.name, ah);
        cv.circle(right - 8, cy, 10, Canvas.mulAlpha(t.surface2(), Math.max(aa, ex)));
        drawRotatedIcon(cv, Icons.CHEVRON, right - 8, cy, 90 * ex, Canvas.mix(t.dim(), t.text(), Math.max(aa, ex)), F.ICON_SM);
        hitClip(right - 20, cy - 12, 24, 24, clipY, clipH, b -> toggleExpand(mo));
        right -= 30;
        if (mo.hasToggle()) {
            drawSwitch(cv, right - 30, cy - 8, 30, 16, on);
            hitClip(right - 34, cy - 12, 38, 24, clipY, clipH, b -> mo.toggle());
            right -= 42;
        }
        if (mo.bind.isSet()) {
            String k = mo.bind.keyName();
            float kw = cv.width(k, F.SMALL) + 24;
            cv.round(right - kw, cy - 7.5f, kw, 15, 5, t.surface2());
            cv.icon(Icons.KEYBOARD, right - kw + 9, cy, t.muted(), F.ICON_SM);
            cv.textMid(k, right - kw + 17, cy, t.text(), F.SMALL);
            if (mo.holdMode()) {
                cv.icon(Icons.POINTER, right - kw - 9, cy, t.dim(), F.ICON_SM);
            }
            right -= kw + 10;
        }
        if (desc) cv.text(cv.trim(mo.description, right - tx - 8, F.SMALL), tx, y + 29, t.dim(), F.SMALL);

        if (visible) hitClip(x, y, w, ROW_H, clipY, clipH, b -> {
            if (b == 1 || !mo.hasToggle()) toggleExpand(mo);
            else mo.toggle();
        });

        if (ex < 0.01f) return;
        // ustawienia modułu w dwóch kolumnach
        float sy0 = y + ROW_H, visH = h - ROW_H;
        float cy0 = Math.max(clipY, sy0), cy1 = Math.min(clipY + clipH, sy0 + visH);
        if (cy1 <= cy0) return;
        cv.scissor(x, cy0, w, cy1 - cy0);
        float prev = cv.alpha;
        cv.alpha = prev * Math.min(1, ex * 1.3f);
        cv.hline(x + 16, sy0, w - 32, t.line());
        float colW = (w - 32 - 24) / 2;
        List<Setting<?>> list = settingsOf(mo);
        float ry = sy0 + 12;
        for (int i = 0; i < list.size(); i += 2) {
            Setting<?> a = list.get(i);
            Setting<?> b = i + 1 < list.size() ? list.get(i + 1) : null;
            drawSetting(cv, a, x + 16, ry, colW, cy0, cy1 - cy0);
            if (b != null) drawSetting(cv, b, x + 16 + colW + 24, ry, colW, cy0, cy1 - cy0);
            ry += Math.max(settingHeight(a), b == null ? 0 : settingHeight(b));
        }
        cv.alpha = prev;
        cv.endScissor();
    }

    private void drawRotatedIcon(Canvas cv, char icon, float cx, float cy, float deg, int color, F f) {
        MatrixStack m = cv.ctx.getMatrices();
        cv.ctx.draw();
        m.push();
        m.translate(cx * u, cy * u, 0);
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(deg));
        m.translate(-cx * u, -cy * u, 0);
        cv.icon(icon, cx, cy, color, f);
        cv.ctx.draw();
        m.pop();
    }

    private void toggleExpand(Module mo) {
        expanded = expanded == mo ? null : mo;
        listening = null;
    }

    // ------------------------------------------------- kontrolki

    private void drawSwitch(Canvas cv, float x, float y, float w, float h, float on) {
        cv.round(x, y, w, h, h / 2, Canvas.mix(t.surface2(), t.accent(), on));
        if (on < 0.99f) cv.outline(x, y, w, h, h / 2, 1f, Canvas.mulAlpha(t.line(), 1 - on));
        float kr = h / 2 - 2.5f;
        float kx = x + h / 2 + (w - h) * on;
        cv.shadow(kx - kr, y + h / 2 - kr + 0.5f, kr * 2, kr * 2, kr, 2, 0x60000000);
        cv.circle(kx, y + h / 2, kr, Canvas.mix(t.muted(), t.onAccent(), on));
    }

    private void drawSetting(Canvas cv, Setting<?> s, float x, float y, float w, float clipY, float clipH) {
        if (s instanceof BoolSetting b) {
            boolean h = inside(x - 4, y, w + 8, 22);
            cv.textMid(s.name, x, y + 10, h ? t.text() : Canvas.mix(t.muted(), t.text(), 0.6f), F.REGULAR);
            drawSwitch(cv, x + w - 26, y + 3, 26, 14, b.anim.to(b.on() ? 1 : 0, 16));
            hitClip(x - 4, y, w + 8, 22, clipY, clipH, btn -> b.toggle());
        } else if (s instanceof NumberSetting n) {
            cv.textMid(s.name, x, y + 6, Canvas.mix(t.muted(), t.text(), 0.6f), F.REGULAR);
            String v = n.display();
            float vw = cv.width(v, F.SMALL) + 10;
            cv.round(x + w - vw, y, vw, 13, 4.5f, t.surface2());
            cv.textCenter(v, x + w - vw / 2, y + 6.5f, t.text(), F.SMALL);
            float r = n.anim.to((float) n.ratio(), 20);
            float ty = y + 24;
            cv.round(x, ty - 2, w, 4, 2, t.surface2());
            cv.round(x, ty - 2, Math.max(4, w * r), 4, 2, t.accent());
            boolean active = dragging == n, h = inside(x - 4, y + 14, w + 8, 18);
            float kr = active ? 6.5f : h ? 6f : 5.5f;
            cv.shadow(x + w * r - kr, ty - kr + 1, kr * 2, kr * 2, kr, 3, 0x80000000);
            cv.circle(x + w * r, ty, kr, t.accent());
            cv.circle(x + w * r, ty, kr - 2.6f, t.bg() | 0xFF000000);
            hitClip(x - 6, y + 14, w + 12, 18, clipY, clipH, btn -> {
                dragging = n;
                dragX = x;
                dragW = w;
                n.setRatio((mx - x) / w);
            });
        } else if (s instanceof ModeSetting m) {
            cv.textMid(s.name, x, y + 6, Canvas.mix(t.muted(), t.text(), 0.6f), F.REGULAR);
            drawSegmented(cv, m, x, y + 15, w, clipY, clipH);
        } else if (s instanceof KeySetting k) {
            cv.textMid(s.name, x, y + 6, Canvas.mix(t.muted(), t.text(), 0.6f), F.REGULAR);
            drawKeyField(cv, k, x, y + 15, w, clipY, clipH);
        }
    }

    private void drawSegmented(Canvas cv, ModeSetting m, float x, float y, float w, float clipY, float clipH) {
        int n = m.modes.size();
        float h = 22, segW = (w - 4) / n;
        if (m.anim.get() < 0) m.anim.set(m.index());
        float pos = m.anim.to(m.index(), 16);
        cv.round(x, y, w, h, 8, t.surface2());
        cv.shadow(x + 2 + pos * segW, y + 3, segW, h - 4, 6, 4, 0x50000000);
        cv.round(x + 2 + pos * segW, y + 2, segW, h - 4, 6.5f, t.accent());
        for (int i = 0; i < n; i++) {
            float on = Math.max(0, 1 - Math.abs(pos - i));
            float sx = x + 2 + i * segW;
            boolean h2 = inside(sx, y, segW, h);
            cv.textCenter(m.modes.get(i), sx + segW / 2, y + h / 2, Canvas.mix(h2 ? t.text() : t.muted(), t.onAccent(), on), F.REGULAR);
            final int idx = i;
            hitClip(sx, y, segW, h, clipY, clipH, b -> m.set(m.modes.get(idx)));
        }
    }

    private void drawKeyField(Canvas cv, KeySetting k, float x, float y, float w, float clipY, float clipH) {
        float h = 24;
        boolean wait = listening == k, h2 = inside(x, y, w, h);
        float pulse = wait ? (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 170.0)) : 0;
        cv.round(x, y, w, h, 8, h2 || wait ? t.surface2() : Canvas.mix(t.surface(), t.surface2(), 0.5f));
        cv.outline(x, y, w, h, 8, 1f, wait ? Canvas.mix(Canvas.mulAlpha(t.accent(), 0.3f), t.accent(), pulse) : t.line());
        cv.icon(Icons.KEYBOARD, x + 13, y + h / 2, wait ? t.accent() : t.muted(), F.ICON_SM);
        String text = wait ? "Naciśnij klawisz..." : k.isSet() ? k.keyName() : "Kliknij, aby przypisać";
        cv.textMid(text, x + 25, y + h / 2, wait || k.isSet() ? t.text() : t.dim(), F.REGULAR);
        if (k.isSet() && !wait) {
            float cx = x + w - 12;
            boolean ch = inside(cx - 9, y + 3, 18, 18);
            cv.circle(cx, y + h / 2, 8, ch ? t.surface() : Canvas.mulAlpha(t.surface(), 0.6f));
            cv.icon(Icons.X, cx, y + h / 2, ch ? t.text() : t.muted(), F.ICON_SM);
            hitClip(x, y, w - 24, h, clipY, clipH, b -> listening = k);
            hitClip(cx - 10, y + 2, 20, 20, clipY, clipH, b -> k.set(GLFW.GLFW_KEY_UNKNOWN));
        } else {
            hitClip(x, y, w, h, clipY, clipH, b -> {
                if (b == 1) k.set(GLFW.GLFW_KEY_UNKNOWN);
                else listening = k;
            });
        }
    }

    /** Pole tekstowe (wyszukiwarka, nazwa configu). */
    private void drawInput(Canvas cv, Focus which, String value, String placeholder, char icon, float x, float y, float w, float h) {
        boolean f = focus == which;
        float fa = anim("in" + which).to(f ? 1 : 0, 16);
        boolean hv = inside(x, y, w, h);
        cv.round(x, y, w, h, 11, hv || f ? t.surface2() : t.surface());
        cv.outline(x, y, w, h, 11, 1f, Canvas.mix(t.line(), t.accent(), fa * 0.8f));
        cv.icon(icon, x + 17, y + h / 2, Canvas.mix(t.muted(), t.accent(), fa), F.ICON);
        float tx = x + 32;
        if (value.isEmpty()) {
            if (!f) cv.textMid(placeholder, tx, y + h / 2, t.dim(), F.REGULAR);
        } else {
            String shown = value;
            while (cv.width(shown, F.REGULAR) > w - 70 && shown.length() > 1) shown = shown.substring(1);
            cv.textMid(shown, tx, y + h / 2, t.text(), F.REGULAR);
            tx += cv.width(shown, F.REGULAR) + 1;
            float cx = x + w - 17;
            boolean ch = inside(cx - 9, y + h / 2 - 9, 18, 18);
            cv.circle(cx, y + h / 2, 8, ch ? t.surface() : Canvas.mulAlpha(t.surface(), 0.6f));
            cv.icon(Icons.X, cx, y + h / 2, ch ? t.text() : t.muted(), F.ICON_SM);
            hit(cx - 10, y + h / 2 - 10, 20, 20, b -> {
                if (which == Focus.SEARCH) query = "";
                else profileName = "";
                focus = which;
            });
        }
        if (f && (System.currentTimeMillis() / 530) % 2 == 0) cv.rect(tx, y + h / 2 - 6, 1f, 12, t.accent());
        hit(x, y, w - (value.isEmpty() ? 0 : 30), h, b -> focus = which);
    }

    private void button(Canvas cv, String key, String label, char icon, float x, float y, float w, float h, boolean primary, int danger, IntConsumer action) {
        float hv = hover(key, inside(x, y, w, h));
        int bg = primary ? Canvas.mix(t.accent(), Canvas.mix(t.accent(), 0xFFFFFFFF, 0.25f), hv) : Canvas.mix(t.surface2(), Canvas.mix(t.surface2(), 0xFFFFFFFF, 0.08f), hv);
        if (danger != 0) bg = Canvas.mix(bg, danger, 0.85f);
        int fg = primary ? t.onAccent() : danger != 0 ? 0xFFFFFFFF : t.text();
        cv.round(x, y, w, h, h / 2.6f, bg);
        if (!label.isEmpty()) {
            float cw = (icon != 0 ? 14 : 0) + cv.width(label, F.REGULAR);
            float sx = x + w / 2 - cw / 2;
            if (icon != 0) cv.icon(icon, sx + 5, y + h / 2, fg, F.ICON_SM);
            cv.textMid(label, sx + (icon != 0 ? 14 : 0), y + h / 2, fg, F.REGULAR);
        } else {
            cv.icon(icon, x + w / 2, y + h / 2, fg, F.ICON_SM);
        }
        hit(x, y, w, h, action);
    }

    // ------------------------------------------------- configi

    private void drawConfigs(Canvas cv) {
        float x = wx + SIDE + 26, right = wx + W - 24, w = right - x;
        float top = wy + 66;
        drawInput(cv, Focus.PROFILE, profileName, "Nazwa nowego configu", Icons.PLUS, x, top, w - 104, 32);
        button(cv, "save", "Zapisz", Icons.SAVE, right - 96, top, 96, 32, true, 0, b -> saveProfile(profileName));
        top += 44;

        float h = wy + H - 52 - top;
        float rowH = 46, total = profiles.size() * (rowH + 6);
        float max = Math.max(0, total - h);
        scrollTarget = Math.max(0, Math.min(max, scrollTarget));
        float s = scroll.to(scrollTarget, 16);
        float c = Anim.easeOutCubic(content.get());
        float prev = cv.alpha;
        cv.scissor(x - 6, top, w + 12, h);
        float y = top - s;
        for (int i = 0; i < profiles.size(); i++) {
            Profiles.Profile p = profiles.get(i);
            float delay = Math.max(0, Math.min(1, c * 1.7f - i * 0.1f));
            cv.alpha = prev * delay;
            float yy = y + (1 - Anim.easeOutCubic(delay)) * 12;
            boolean hv = my >= top && my <= top + h && inside(x, yy, w, rowH);
            float ha = hover("prof" + p.name(), hv);
            cv.round(x, yy, w, rowH, 12, Canvas.mix(Canvas.mulAlpha(t.surface(), 0.6f), t.surface(), ha));
            cv.round(x + 10, yy + 9, 28, 28, 9, t.surface2());
            cv.icon(Icons.FOLDER, x + 24, yy + 23, t.text(), F.ICON);
            cv.text(p.name(), x + 48, yy + 12, t.text(), F.MEDIUM);
            cv.text("Zapisano " + p.date() + "  ·  włączone: " + p.enabled(), x + 48, yy + 29, t.dim(), F.SMALL);
            if (my >= top && my <= top + h) {
                float bx = right - 12;
                boolean confirm = p.name().equals(confirmDelete);
                float dw = confirm ? 88 : 28;
                button(cv, "del" + p.name(), confirm ? "Na pewno?" : "", Icons.TRASH, bx - dw, yy + 9, dw, 28, false, confirm ? 0xFFE5484D : 0, b -> {
                    if (confirm) {
                        Profiles.delete(p.name());
                        confirmDelete = null;
                        profiles = Profiles.list();
                        showToast("Usunięto config " + p.name());
                    } else confirmDelete = p.name();
                });
                bx -= dw + 6;
                button(cv, "ovr" + p.name(), "", Icons.SAVE, bx - 28, yy + 9, 28, 28, false, 0, b -> saveProfile(p.name()));
                bx -= 34;
                button(cv, "load" + p.name(), "Wczytaj", Icons.DOWNLOAD, bx - 88, yy + 9, 88, 28, true, 0, b -> {
                    if (Profiles.load(p.name())) showToast("Wczytano config " + p.name());
                });
            }
            y += rowH + 6;
        }
        if (profiles.isEmpty()) {
            float cy = top + h / 2 - 20;
            cv.circle(x + w / 2, cy, 22, t.surface());
            cv.icon(Icons.FOLDER_OPEN, x + w / 2, cy, t.muted(), F.ICON_LG);
            cv.textCenter("Brak zapisanych configów", x + w / 2, cy + 36, t.text(), F.BOLD);
            cv.textCenter("Wpisz nazwę i kliknij Zapisz, żeby zachować obecne ustawienia", x + w / 2, cy + 49, t.muted(), F.SMALL);
        }
        cv.endScissor();
        cv.alpha = prev;
        fades(cv, x, top, w, h, s, max);

        float fy = wy + H - 44;
        cv.text(profiles.size() + " " + (profiles.size() == 1 ? "config" : "configów") + "  ·  config/fuzir-client/profiles", x, fy + 10, t.dim(), F.SMALL);
        button(cv, "folder", "Otwórz folder", Icons.FOLDER_OPEN, right - 130, fy, 130, 28, false, 0, b -> {
            try {
                java.nio.file.Files.createDirectories(Profiles.folder());
            } catch (java.io.IOException ignored) {
            }
            Util.getOperatingSystem().open(Profiles.folder().toFile());
        });
    }

    private void saveProfile(String name) {
        String clean = Profiles.clean(name);
        if (clean.isEmpty()) {
            showToast("Wpisz nazwę configu");
            focus = Focus.PROFILE;
            return;
        }
        if (Profiles.save(clean)) {
            showToast("Zapisano config " + clean);
            profileName = "";
            focus = Focus.NONE;
            profiles = Profiles.list();
        }
    }

    // ------------------------------------------------- motywy

    private void drawThemes(Canvas cv) {
        float x = wx + SIDE + 26, right = wx + W - 24, w = right - x;
        float top = wy + 70, gap = 14;
        float cw = (w - gap * 2) / 3, ch = 128;
        float c = Anim.easeOutCubic(content.get());
        float prev = cv.alpha;
        List<Theme> all = Theme.ALL;
        for (int i = 0; i < all.size(); i++) {
            Theme th = all.get(i);
            float delay = Math.max(0, Math.min(1, c * 1.7f - i * 0.1f));
            cv.alpha = prev * delay;
            float cx = x + (i % 3) * (cw + gap), cy = top + (i / 3) * (ch + gap) + (1 - Anim.easeOutCubic(delay)) * 12;
            boolean sel = Theme.target() == th;
            float hv = hover("theme" + i, inside(cx, cy, cw, ch));
            float sa = anim("sel" + i).to(sel ? 1 : 0, 14);
            cv.round(cx, cy, cw, ch, 14, Canvas.mix(t.surface(), t.surface2(), hv * 0.6f));
            cv.outline(cx, cy, cw, ch, 14, 1f + sa, Canvas.mix(t.line(), t.accent(), sa));
            // podgląd okna w kolorach motywu
            float px = cx + 10, py = cy + 10, pw = cw - 20, ph = 74;
            cv.round(px, py, pw, ph, 9, th.bg() | 0xFF000000);
            cv.round(px, py, pw * 0.3f, ph, 9, th.sidebar());
            cv.rect(px + pw * 0.3f - 9, py, 9, ph, th.sidebar());
            cv.round(px + 7, py + 8, 10, 10, 3, th.accent());
            cv.round(px + 6, py + 26, pw * 0.3f - 12, 9, 3, th.accent());
            for (int r = 0; r < 3; r++) cv.round(px + 6, py + 40 + r * 10, pw * 0.3f - 18 - r * 4, 5, 2.5f, th.surface2());
            for (int r = 0; r < 3; r++) {
                float ry = py + 10 + r * 21;
                cv.round(px + pw * 0.3f + 8, ry, pw * 0.7f - 16, 16, 5, th.surface());
                cv.round(px + pw * 0.3f + 13, ry + 4, 30 - r * 6, 3.5f, 1.75f, th.text());
                cv.round(px + pw * 0.3f + 13, ry + 10, 46 - r * 8, 2.5f, 1.25f, th.dim());
                cv.round(px + pw - 26, ry + 5, 12, 6, 3, r == 1 ? th.surface2() : th.accent());
            }
            cv.circle(cx + 20, cy + ch - 21, 6, th.accent());
            cv.text(th.name(), cx + 32, cy + ch - 26, t.text(), F.BOLD);
            if (sa > 0.01f) {
                float ka = Anim.easeOutCubic(sa);
                cv.circle(cx + cw - 20, cy + ch - 21, 8 * ka, t.accent());
                cv.icon(Icons.CHECK, cx + cw - 20, cy + ch - 21, Canvas.mulAlpha(t.onAccent(), ka), F.ICON_SM);
            }
            hit(cx, cy, cw, ch, b -> {
                Theme.select(th.name());
                cfg().theme.set(th.name());
                Config.save();
            });
        }
        cv.alpha = prev;
        cv.text("Motyw zmienia kolory menu, HUD, KeyBinds i powiadomień.", x, wy + H - 34, t.dim(), F.SMALL);
    }

    // ------------------------------------------------- powiadomienie w menu

    private void showToast(String text) {
        toast = text;
        toastAt = System.currentTimeMillis();
    }

    private void drawToast(Canvas cv) {
        boolean alive = System.currentTimeMillis() - toastAt < 2200;
        float a = Anim.easeOutCubic(toastAnim.to(alive ? 1 : 0, 12));
        if (a < 0.01f) return;
        float w = cv.width(toast, F.REGULAR) + 44, h = 30;
        float x = wx + SIDE + (W - SIDE) / 2 - w / 2, y = wy + H - 46 + (1 - a) * 18;
        float prev = cv.alpha;
        cv.alpha = prev * a;
        cv.shadow(x, y + 3, w, h, 15, 12, 0x90000000);
        cv.round(x, y, w, h, 15, t.accent());
        cv.icon(Icons.CIRCLE_CHECK, x + 16, y + h / 2, t.onAccent(), F.ICON_SM);
        cv.textMid(toast, x + 28, y + h / 2, t.onAccent(), F.REGULAR);
        cv.alpha = prev;
    }

    // ================================================= pomocnicze

    private static String plural(int n) {
        if (n == 1) return "moduł";
        int d = n % 10, tt = n % 100;
        return d >= 2 && d <= 4 && (tt < 12 || tt > 14) ? "moduły" : "modułów";
    }

    private boolean inside(float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void hit(float x, float y, float w, float h, IntConsumer action) {
        hits.add(new Hit(x, y, w, h, action));
    }

    private void hitClip(float x, float y, float w, float h, float clipY, float clipH, IntConsumer action) {
        float y0 = Math.max(y, clipY), y1 = Math.min(y + h, clipY + clipH);
        if (y1 > y0) hits.add(new Hit(x, y0, w, y1 - y0, action));
    }

    private void selectPage(int idx) {
        if (idx == page) return;
        SAVED_SCROLL.put(page, scrollTarget);
        page = idx;
        expanded = null;
        listening = null;
        confirmDelete = null;
        focus = PAGES.get(idx).kind == Kind.SEARCH ? Focus.SEARCH : Focus.NONE;
        scrollTarget = SAVED_SCROLL.getOrDefault(idx, 0f);
        scroll.set(scrollTarget);
        content.set(0);
        if (PAGES.get(idx).kind == Kind.CONFIGS) profiles = Profiles.list();
    }

    private void openSearch() {
        selectPage(0);
        focus = Focus.SEARCH;
    }

    private void click(float pitch) {
        if (cfg().sounds.on()) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.22f));
        }
    }

    // ================================================= wejście

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return false;
        double sf = client.getWindow().getScaleFactor();
        mx = (float) (mouseX * sf / u);
        my = (float) (mouseY * sf / u);
        if (listening != null) {
            listening = null;
            return true;
        }
        Focus before = focus;
        if (PAGES.get(page).kind != Kind.SEARCH) focus = Focus.NONE;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.in(mx, my)) {
                String cd = confirmDelete;
                h.action.accept(button);
                if (cd != null && cd.equals(confirmDelete)) confirmDelete = null;
                click(button == 1 ? 0.85f : 1.15f);
                return true;
            }
        }
        confirmDelete = null;
        if (before == Focus.SEARCH && PAGES.get(page).kind == Kind.SEARCH) focus = Focus.SEARCH;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dX, double dY) {
        if (dragging != null) {
            double sf = client.getWindow().getScaleFactor();
            mx = (float) (mouseX * sf / u);
            dragging.setRatio((mx - dragX) / dragW);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dX, dY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) {
            dragging = null;
            Config.save();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        scrollTarget -= (float) v * 36;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (closing) return true;
        if (listening != null) {
            boolean clear = key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE;
            listening.set(clear ? GLFW.GLFW_KEY_UNKNOWN : key);
            listening = null;
            click(1.35f);
            Config.save();
            return true;
        }
        boolean ctrl = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
        if (ctrl && key == GLFW.GLFW_KEY_F) {
            openSearch();
            return true;
        }
        if (focus != Focus.NONE) {
            boolean search = focus == Focus.SEARCH;
            String v = search ? query : profileName;
            switch (key) {
                case GLFW.GLFW_KEY_BACKSPACE -> v = ctrl ? "" : v.isEmpty() ? v : v.substring(0, v.length() - 1);
                case GLFW.GLFW_KEY_ESCAPE -> {
                    if (v.isEmpty()) {
                        focus = Focus.NONE;
                        if (search) close();
                    }
                    v = "";
                }
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    if (search) {
                        List<Module> list = modules();
                        if (!v.isEmpty() && list.size() == 1 && list.get(0).hasToggle()) list.get(0).toggle();
                    } else {
                        saveProfile(v);
                        return true;
                    }
                }
                default -> {
                }
            }
            if (search) {
                query = v;
                scrollTarget = 0;
            } else profileName = v;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && expanded != null) {
            expanded = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE || FuzirClient.OPEN_KEY.matchesKey(key, scan)) {
            close();
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
            selectPage(Math.floorMod(page + (key == GLFW.GLFW_KEY_DOWN ? 1 : -1), PAGES.size()));
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (closing || listening != null) return true;
        if (focus == Focus.NONE) {
            if (!Character.isLetterOrDigit(chr)) return false;
            openSearch();
        }
        if (chr < ' ') return true;
        if (focus == Focus.SEARCH && query.length() < 28) {
            query += chr;
            scrollTarget = 0;
        } else if (focus == Focus.PROFILE && profileName.length() < 24) {
            profileName += chr;
        }
        return true;
    }

    // ================================================= dostęp z zewnątrz (np. testy)

    /** Przechodzi na stronę o podanym tytule (np. "Wizualne", "Configi"). */
    public void openPage(String title) {
        for (int i = 0; i < PAGES.size(); i++) if (PAGES.get(i).title.equals(title)) selectPage(i);
    }

    /** Rozwija ustawienia modułu na liście. */
    public void openSettings(Module m) {
        expanded = m;
    }

    public void type(String s) {
        for (char ch : s.toCharArray()) charTyped(ch, 0);
    }
}
