package tech.fuzir.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import tech.fuzir.client.FuzirClient;
import tech.fuzir.client.config.Config;
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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;

public class ClickGuiScreen extends Screen {
    // wymiary w jednostkach (przy 1080p jedna jednostka to 2 piksele)
    private static final float W = 580, H = 350, SIDE = 158, DRAWER = 250;
    private static final float CARD_H = 72, GAP = 10;

    // paleta: czerń, biel i szarości
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;
    private static final int BG = 0xF50B0B0D;
    private static final int TEXT = 0xFFF2F2F3;
    private static final int MUTED = 0xFF8B8B92;
    private static final int DIM = 0xFF55555C;
    private static final int CARD = 0xFF121215;
    private static final int CARD_HOVER = 0xFF18181C;
    private static final int TILE = 0xFF1D1D22;
    private static final int LINE = 0x12FFFFFF;

    private static Category current = Category.MOVEMENT;
    private static final Map<Category, Float> SAVED_SCROLL = new EnumMap<>(Category.class);

    private final Anim open = new Anim(0);
    private final Anim indicator = new Anim(-1);
    private final Anim indicatorAlpha = new Anim(1);
    private final Anim scroll = new Anim(0);
    private final Anim content = new Anim(0);
    private final Anim searchFocus = new Anim(0);
    private final Anim drawer = new Anim(0);
    private final Anim drawerScroll = new Anim(0);
    private final Anim editBtn = new Anim(0);
    private final Anim[] catHover = new Anim[Category.values().length];
    private final Map<Object, Anim> misc = new java.util.HashMap<>();
    private final List<Hit> hits = new ArrayList<>();

    private boolean closing;
    private float scrollTarget, drawerScrollTarget, drawerMax;
    private String query = "";
    private boolean searchActive;
    private Module selected;
    private boolean drawerOpen;
    private KeySetting listening;
    private NumberSetting dragging;
    private float dragX, dragW;
    private Module rippleModule;
    private float rippleX, rippleY;
    private long rippleAt;
    private float u, wx, wy, mx, my;

    private record Hit(float x, float y, float w, float h, IntConsumer action) {
        boolean in(float px, float py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }

    public ClickGuiScreen() {
        super(Text.literal("Fuzir Client"));
        for (int i = 0; i < catHover.length; i++) catHover[i] = new Anim(0);
        scrollTarget = SAVED_SCROLL.getOrDefault(current, 0f);
        scroll.set(scrollTarget);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        closing = true;
        searchActive = false;
        listening = null;
    }

    @Override
    public void removed() {
        SAVED_SCROLL.put(current, scrollTarget);
        Config.save();
    }

    private ClickGui cfg() {
        return ModuleManager.get(ClickGui.class);
    }

    private Anim anim(Object key) {
        return misc.computeIfAbsent(key, k -> new Anim(0));
    }

    // ===================================================== rysowanie

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
        if (cfg().blur.on() && o > 0.5f) super.renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(0, 0, width, height, Canvas.argb((int) (95 * o), 0));

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

        // wejście okna: skala od środka i lekki ruch w górę
        float e = Anim.easeOutCubic(o);
        MatrixStack m = ctx.getMatrices();
        float sc = 0.94f + 0.06f * e;
        m.translate((wx + W / 2) * u, (wy + H / 2) * u, 0);
        m.scale(sc, sc, 1);
        m.translate(-(wx + W / 2) * u, -(wy + H / 2) * u + (1 - e) * 16 * u, 0);

        hits.clear();
        cv.shadow(wx, wy + 10, W, H, 18, 36, 0xB0000000);
        cv.round(wx, wy, W, H, 18, BG);
        cv.outline(wx, wy, W, H, 18, 1f, 0x17FFFFFF);
        cv.rect(wx + SIDE, wy + 16, 1f / u, H - 32, LINE);

        drawSidebar(cv);
        drawHeader(cv);
        drawGrid(cv);
        drawDrawer(cv);
        cv.end();
    }

    // ----------------------------------------------------- panel boczny

    private void drawSidebar(Canvas cv) {
        float x = wx + 18, y = wy + 20;
        cv.shadow(x, y + 2, 28, 28, 9, 8, 0x40FFFFFF);
        cv.round(x, y, 28, 28, 9, WHITE);
        cv.textCenter("F", x + 14.3f, y + 14, BLACK, F.TITLE);
        cv.text("Fuzir", x + 38, y + 5, WHITE, F.BOLD);
        cv.text("Client 2.0", x + 38, y + 17, MUTED, F.SMALL);

        cv.text("MENU", wx + 22, wy + 70, DIM, F.SMALL);

        Category[] cats = Category.values();
        float rowY = wy + 82, step = 32, rx = wx + 12, rw = SIDE - 24, rh = 28;
        boolean searching = !query.isEmpty();
        int idx = current.ordinal();
        if (indicator.get() < 0) indicator.set(idx);
        float ind = indicator.to(idx, 16);
        float pill = indicatorAlpha.to(searching ? 0.0f : 1f, 14);

        for (int i = 0; i < cats.length; i++) {
            float ry = rowY + i * step;
            float hv = catHover[i].to(inside(rx, ry, rw, rh) ? 1 : 0, 16);
            if (hv > 0.01f) cv.round(rx, ry, rw, rh, 9, Canvas.mulAlpha(0x0EFFFFFF, hv));
        }
        if (pill > 0.01f) {
            float py = rowY + ind * step;
            cv.shadow(rx, py + 2, rw, rh, 9, 8, Canvas.mulAlpha(0x30FFFFFF, pill));
            cv.round(rx, py, rw, rh, 9, Canvas.mulAlpha(WHITE, pill));
        }
        for (int i = 0; i < cats.length; i++) {
            Category c = cats[i];
            float ry = rowY + i * step, cy = ry + rh / 2;
            float on = Math.max(0, 1 - Math.abs(ind - i)) * pill;
            int base = Canvas.mix(MUTED, TEXT, catHover[i].get());
            int col = Canvas.mix(base, BLACK, on);
            cv.icon(c.icon, rx + 15, cy, col, F.ICON);
            cv.textMid(c.title, rx + 29, cy, col, F.REGULAR);
            List<Module> list = ModuleManager.in(c);
            long all = list.stream().filter(Module::hasToggle).count();
            if (all > 0) {
                long en = list.stream().filter(mo -> mo.hasToggle() && mo.isEnabled()).count();
                cv.textRight(en + "/" + all, rx + rw - 10, cy, Canvas.mix(DIM, 0xFF6A6A70, on), F.SMALL);
            }
            hit(rx, ry, rw, rh, b -> selectCategory(c));
        }

        // przycisk edytora HUD
        float bx = wx + 12, by = wy + H - 84, bw = SIDE - 24, bh = 28;
        float eh = editBtn.to(inside(bx, by, bw, bh) ? 1 : 0, 16);
        cv.round(bx, by, bw, bh, 9, Canvas.mix(0xFF141417, 0xFF1C1C21, eh));
        cv.outline(bx, by, bw, bh, 9, 1f, Canvas.mix(0x12FFFFFF, 0x30FFFFFF, eh));
        cv.icon(Icons.LAYOUT, bx + 15, by + bh / 2, Canvas.mix(MUTED, WHITE, eh), F.ICON);
        cv.textMid("Edytuj HUD", bx + 29, by + bh / 2, Canvas.mix(0xFFCFCFD4, WHITE, eh), F.REGULAR);
        cv.icon(Icons.CHEVRON, bx + bw - 12 + eh * 2, by + bh / 2, Canvas.mix(DIM, WHITE, eh), F.ICON_SM);
        hit(bx, by, bw, bh, b -> client.setScreen(new HudEditorScreen()));

        // gracz
        float py = wy + H - 46;
        String nick = client.getSession().getUsername();
        cv.circle(wx + 26, py + 14, 12, WHITE);
        cv.textCenter(nick.substring(0, 1).toUpperCase(Locale.ROOT), wx + 26.3f, py + 14, BLACK, F.BOLD);
        cv.circle(wx + 35, py + 23, 3.6f, BG);
        cv.circle(wx + 35, py + 23, 2.4f, WHITE);
        cv.text(cv.trim(nick, SIDE - 60, F.REGULAR), wx + 44, py + 6, TEXT, F.REGULAR);
        String key = FuzirClient.OPEN_KEY.getBoundKeyLocalizedText().getString();
        cv.text(cv.trim(key + " otwiera menu", SIDE - 60, F.SMALL), wx + 44, py + 18, MUTED, F.SMALL);
    }

    // ----------------------------------------------------- nagłówek

    private void drawHeader(Canvas cv) {
        float x = wx + SIDE + 22, right = wx + W - 20;
        boolean searching = !query.isEmpty();
        List<Module> list = visibleModules();
        float c = Anim.easeOutCubic(content.to(1, 9));
        float prev = cv.alpha;
        cv.alpha = prev * c;
        String title = searching ? "Wyniki" : current.title;
        cv.text(title, x - (1 - c) * 8, wy + 22, WHITE, F.TITLE);
        String sub = searching ? list.size() + " " + plural(list.size()) + " dla \"" + query + "\"" : current.subtitle + "  ·  " + list.size() + " " + plural(list.size());
        cv.text(sub, x, wy + 42, MUTED, F.SMALL);
        cv.alpha = prev;

        // wyszukiwarka
        float sw = 168, sh = 28, sx = right - sw, sy = wy + 19;
        boolean hover = inside(sx, sy, sw, sh);
        float f = searchFocus.to(searchActive ? 1 : 0, 16);
        cv.round(sx, sy, sw, sh, 14, Canvas.mix(hover ? 0xFF17171B : 0xFF131316, 0xFF18181C, f));
        cv.outline(sx, sy, sw, sh, 14, 1f, Canvas.mix(hover ? 0x22FFFFFF : 0x14FFFFFF, 0x80FFFFFF, f));
        cv.icon(Icons.SEARCH, sx + 15, sy + sh / 2, Canvas.mix(MUTED, WHITE, f), F.ICON_SM);
        float tx = sx + 27;
        if (query.isEmpty()) {
            if (!searchActive) cv.textMid("Szukaj modułów", tx, sy + sh / 2, DIM, F.REGULAR);
            String hint = "Ctrl F";
            float hw = cv.width(hint, F.SMALL) + 10;
            float a = 1 - f;
            cv.round(sx + sw - 8 - hw, sy + 7, hw, 14, 5, Canvas.mulAlpha(0xFF1F1F24, a));
            cv.textCenter(hint, sx + sw - 8 - hw / 2, sy + 14, Canvas.mulAlpha(MUTED, a), F.SMALL);
        } else {
            String shown = query;
            while (cv.width(shown, F.REGULAR) > sw - 40 && shown.length() > 1) shown = shown.substring(1);
            cv.textMid(shown, tx, sy + sh / 2, TEXT, F.REGULAR);
            tx += cv.width(shown, F.REGULAR) + 1;
            float cx = sx + sw - 15;
            boolean ch = inside(cx - 8, sy + 6, 16, 16);
            cv.circle(cx, sy + sh / 2, 7, ch ? 0xFF2A2A30 : 0xFF1F1F24);
            cv.icon(Icons.X, cx, sy + sh / 2, ch ? WHITE : MUTED, F.ICON_SM);
            hit(cx - 9, sy + 4, 18, 20, b -> {
                query = "";
                searchActive = true;
            });
        }
        if (searchActive && (System.currentTimeMillis() / 530) % 2 == 0) cv.rect(tx, sy + 8, 1f, 12, WHITE);
        hit(sx, sy, sw - (query.isEmpty() ? 0 : 26), sh, b -> searchActive = true);
    }

    // ----------------------------------------------------- siatka modułów

    private void drawGrid(Canvas cv) {
        float x = wx + SIDE + 22, right = wx + W - 20, w = right - x;
        float top = wy + 64, h = H - 64 - 14;
        float colW = (w - GAP) / 2;
        List<Module> list = visibleModules();
        int rows = (list.size() + 1) / 2;
        float total = rows * (CARD_H + GAP) - GAP;
        float max = Math.max(0, total - h + 8);
        scrollTarget = Math.max(0, Math.min(max, scrollTarget));
        float s = scroll.to(scrollTarget, 16);

        float c = Anim.easeOutCubic(content.get());
        float prev = cv.alpha;
        cv.alpha = prev * c;
        cv.scissor(x - 6, top, w + 12, h);
        for (int i = 0; i < list.size(); i++) {
            int col = i % 2, row = i / 2;
            float cx = x + col * (colW + GAP);
            // karty wjeżdżają po kolei przy zmianie kategorii
            float delay = Math.max(0, Math.min(1, c * 1.6f - i * 0.12f));
            float cy = top + 4 + row * (CARD_H + GAP) - s + (1 - Anim.easeOutCubic(delay)) * 14;
            if (cy + CARD_H < top - 4 || cy > top + h + 4) continue;
            float pa = cv.alpha;
            cv.alpha = pa * delay;
            drawCard(cv, list.get(i), cx, cy, colW, top, h);
            cv.alpha = pa;
        }
        if (list.isEmpty()) {
            float cy = top + h / 2 - 20;
            cv.circle(x + w / 2, cy, 18, 0xFF16161A);
            cv.icon(Icons.SEARCH, x + w / 2, cy, MUTED, F.ICON_LG);
            cv.textCenter("Nic nie znaleziono", x + w / 2, cy + 32, TEXT, F.BOLD);
            cv.textCenter("Spróbuj innej nazwy modułu", x + w / 2, cy + 45, MUTED, F.SMALL);
        }
        cv.endScissor();
        cv.alpha = prev;

        if (s > 1) cv.roundGradient(x - 6, top, w + 12, 14, 0, 0xF00B0B0D, 0x000B0B0D);
        if (s < max - 1) cv.roundGradient(x - 6, top + h - 14, w + 12, 14, 0, 0x000B0B0D, 0xF00B0B0D);
        if (max > 0) {
            float barH = Math.max(28, h * h / (total + 8));
            cv.round(wx + W - 9, top + (h - barH) * (s / max), 3, barH, 1.5f, 0x30FFFFFF);
        }
    }

    private void drawCard(Canvas cv, Module mo, float x, float y, float w, float clipY, float clipH) {
        boolean visible = my >= clipY && my <= clipY + clipH && !drawerOpen;
        boolean hovered = visible && inside(x, y, w, CARD_H);
        float hv = mo.hoverAnim.to(hovered ? 1 : 0, 16);
        float t = mo.toggleAnim.to(mo.isEnabled() && mo.hasToggle() ? 1 : 0, 14);

        cv.round(x, y, w, CARD_H, 13, Canvas.mix(CARD, CARD_HOVER, hv));
        cv.outline(x, y, w, CARD_H, 13, 1f, Canvas.mix(0x0FFFFFFF, 0x34FFFFFF, Math.max(t * 0.7f, hv * 0.6f)));

        // fala po kliknięciu
        if (rippleModule == mo) {
            float p = (System.currentTimeMillis() - rippleAt) / 550f;
            if (p < 1) {
                float yy0 = Math.max(clipY, y), yy1 = Math.min(clipY + clipH, y + CARD_H);
                if (yy1 > yy0) {
                    cv.scissor(x, yy0, w, yy1 - yy0);
                    cv.circle(rippleX, y + rippleY, 12 + Anim.easeOutCubic(p) * w, Canvas.mulAlpha(0x1CFFFFFF, 1 - p));
                    cv.endScissor();
                }
            }
        }

        // ikona
        float ix = x + 12, iy = y + 12;
        if (t > 0.01f) cv.shadow(ix, iy + 2, 32, 32, 10, 8, Canvas.mulAlpha(0x40FFFFFF, t));
        cv.round(ix, iy, 32, 32, 10, Canvas.mix(TILE, WHITE, t));
        cv.icon(mo.icon, ix + 16, iy + 16, Canvas.mix(0xFFA3A3AB, BLACK, t), F.ICON_LG);

        cv.text(cv.trim(mo.name, w - 64, F.BOLD), x + 54, y + 15, TEXT, F.BOLD);
        cv.text(cv.trim(mo.description, w - 66, F.SMALL), x + 54, y + 28, MUTED, F.SMALL);

        float by = y + 54;
        // klawisz
        String k = mo.hasToggle() ? (mo.bind.isSet() ? mo.bind.keyName() : "Brak") : "Ustawienia";
        float kw = cv.width(k, F.SMALL) + 24;
        float kx = x + 54;
        cv.round(kx, by - 7, kw, 14, 5, 0xFF1B1B20);
        cv.icon(mo.hasToggle() ? Icons.KEYBOARD : Icons.SLIDERS, kx + 8, by, mo.bind.isSet() ? TEXT : DIM, F.ICON_SM);
        cv.textMid(k, kx + 16, by, mo.bind.isSet() ? TEXT : DIM, F.SMALL);
        if (mo.holdMode() && mo.bind.isSet()) cv.textMid("hold", kx + kw + 5, by, DIM, F.SMALL);

        float right = x + w - 12;
        if (mo.hasToggle()) {
            float sw = 30, sh = 16, sx = right - sw;
            drawSwitch(cv, sx, by - sh / 2, sw, sh, t);
            right = sx - 8;
        }
        // ustawienia
        float gx = right - 18, gy = by - 9;
        boolean gh = visible && inside(gx, gy, 18, 18);
        float ga = anim("gear" + mo.name).to(gh ? 1 : 0, 18);
        cv.round(gx, gy, 18, 18, 6, Canvas.mulAlpha(0x1AFFFFFF, Math.max(ga, hv * 0.4f)));
        cv.icon(Icons.SETTINGS, gx + 9, gy + 9, Canvas.mix(DIM, WHITE, Math.max(ga, hv * 0.5f)), F.ICON_SM);

        if (!visible) return;
        hitClip(x, y, w, CARD_H, clipY, clipH, b -> {
            rippleModule = mo;
            rippleX = mx;
            rippleY = my - y;
            rippleAt = System.currentTimeMillis();
            if (b == 1 || !mo.hasToggle()) openDrawer(mo);
            else mo.toggle();
        });
        hitClip(gx - 2, gy - 2, 22, 22, clipY, clipH, b -> openDrawer(mo));
    }

    private void drawSwitch(Canvas cv, float x, float y, float w, float h, float t) {
        cv.round(x, y, w, h, h / 2, Canvas.mix(0xFF2A2A30, WHITE, t));
        if (t < 0.99f) cv.outline(x, y, w, h, h / 2, 1f, Canvas.mulAlpha(0x14FFFFFF, 1 - t));
        float kr = h / 2 - 2.5f;
        float kx = x + h / 2 + (w - h) * t;
        cv.shadow(kx - kr, y + h / 2 - kr + 0.5f, kr * 2, kr * 2, kr, 2, 0x60000000);
        cv.circle(kx, y + h / 2, kr, Canvas.mix(0xFF8E8E95, BLACK, t));
    }

    // ----------------------------------------------------- panel ustawień modułu

    private void openDrawer(Module mo) {
        if (selected != mo) {
            drawerScrollTarget = 0;
            drawerScroll.set(0);
        }
        selected = mo;
        drawerOpen = true;
        listening = null;
    }

    private void closeDrawer() {
        drawerOpen = false;
        listening = null;
        dragging = null;
    }

    private void drawDrawer(Canvas cv) {
        float e = Anim.easeOutCubic(drawer.to(drawerOpen ? 1 : 0, 13));
        if (e < 0.005f || selected == null) return;
        Module mo = selected;
        // przyciemnienie siatki
        cv.scissor(wx + SIDE + 1f / u, wy, W - SIDE, H);
        cv.round(wx, wy, W, H, 18, Canvas.mulAlpha(0x8C000000, e));
        hit(wx + SIDE, wy, W - SIDE, H, b -> closeDrawer());

        float dx = wx + W - DRAWER * e;
        cv.shadow(dx, wy, DRAWER + 30, H, 18, 24, Canvas.mulAlpha(0xA0000000, e));
        cv.round(dx, wy, DRAWER + 30, H, 18, 0xFF0F0F12);
        cv.outline(dx, wy, DRAWER + 30, H, 18, 1f, 0x1AFFFFFF);
        hit(dx, wy, DRAWER, H, b -> {
        });

        float x = dx + 18, w = DRAWER - 36;
        // nagłówek
        float t = mo.toggleAnim.get();
        cv.round(x, wy + 18, 34, 34, 11, Canvas.mix(TILE, WHITE, t));
        cv.icon(mo.icon, x + 17, wy + 35, Canvas.mix(0xFFA3A3AB, BLACK, t), F.ICON_LG);
        cv.text(cv.trim(mo.name, w - 70, F.BOLD), x + 44, wy + 24, WHITE, F.BOLD);
        cv.text(mo.category.title, x + 44, wy + 37, MUTED, F.SMALL);
        float cx = x + w - 10, cyy = wy + 35;
        boolean ch = inside(cx - 11, cyy - 11, 22, 22);
        float ca = anim("close").to(ch ? 1 : 0, 18);
        cv.circle(cx, cyy, 11, Canvas.mix(0xFF19191D, 0xFF26262C, ca));
        cv.icon(Icons.X, cx, cyy, Canvas.mix(MUTED, WHITE, ca), F.ICON_SM);
        hit(cx - 12, cyy - 12, 24, 24, b -> closeDrawer());

        // treść z przewijaniem
        float top = wy + 64, h = H - 64 - 10;
        drawerScrollTarget = Math.max(0, Math.min(drawerMax, drawerScrollTarget));
        float s = drawerScroll.to(drawerScrollTarget, 16);
        cv.scissor(dx, top, DRAWER, h);
        float y = top - s;
        for (String line : cv.wrap(mo.description, w, F.SMALL)) {
            cv.text(line, x, y + 2, MUTED, F.SMALL);
            y += 10;
        }
        y += 10;
        if (mo.hasToggle()) {
            y = section(cv, "STAN", x, y, w);
            cv.textMid("Włączony", x, y + 12, TEXT, F.REGULAR);
            drawSwitch(cv, x + w - 30, y + 4, 30, 16, mo.toggleAnim.get());
            hitClip(x - 4, y, w + 8, 24, top, h, b -> mo.toggle());
            y += 30;
        }
        List<Setting<?>> list = new ArrayList<>();
        for (Setting<?> st : mo.settings) if (st.isVisible()) list.add(st);
        if (!list.isEmpty()) {
            y = section(cv, "USTAWIENIA", x, y, w);
            for (Setting<?> st : list) y = drawSetting(cv, st, x, y, w, top, h);
            y += 6;
        }
        if (mo.hasToggle()) {
            y = section(cv, "KLAWISZ", x, y, w);
            y = drawKeyField(cv, mo.bind, x, y, w, top, h);
            y += 4;
            y = drawSegmented(cv, mo.bindMode, x, y, w, top, h);
        }
        cv.endScissor();
        drawerMax = Math.max(0, (y + s) - (top + h) + 10);
    }

    private float section(Canvas cv, String label, float x, float y, float w) {
        cv.text(label, x, y + 2, DIM, F.SMALL);
        cv.hline(x + cv.width(label, F.SMALL) + 8, y + 4.5f, w - cv.width(label, F.SMALL) - 8, LINE);
        return y + 12;
    }

    private float drawSetting(Canvas cv, Setting<?> s, float x, float y, float w, float clipY, float clipH) {
        if (s instanceof BoolSetting b) {
            boolean hover = inside(x - 4, y, w + 8, 24);
            cv.textMid(s.name, x, y + 12, hover ? WHITE : TEXT, F.REGULAR);
            float t = b.anim.to(b.on() ? 1 : 0, 16);
            drawSwitch(cv, x + w - 26, y + 5, 26, 14, t);
            hitClip(x - 4, y, w + 8, 24, clipY, clipH, btn -> b.toggle());
            return y + 26;
        }
        if (s instanceof NumberSetting n) {
            cv.textMid(s.name, x, y + 7, TEXT, F.REGULAR);
            String v = n.display();
            float vw = cv.width(v, F.SMALL) + 10;
            cv.round(x + w - vw, y + 1, vw, 13, 4.5f, 0xFF1C1C21);
            cv.textCenter(v, x + w - vw / 2, y + 7.5f, WHITE, F.SMALL);
            float r = n.anim.to((float) n.ratio(), 20);
            float ty = y + 23;
            cv.round(x, ty - 2, w, 4, 2, 0xFF25252B);
            cv.round(x, ty - 2, Math.max(4, w * r), 4, 2, WHITE);
            boolean active = dragging == n;
            boolean hover = inside(x - 4, y + 14, w + 8, 18);
            float kr = active ? 6.5f : hover ? 6f : 5.5f;
            cv.shadow(x + w * r - kr, ty - kr + 1, kr * 2, kr * 2, kr, 3, 0x80000000);
            cv.circle(x + w * r, ty, kr, WHITE);
            cv.circle(x + w * r, ty, kr - 2.6f, 0xFF0F0F12);
            hitClip(x - 6, y + 14, w + 12, 18, clipY, clipH, btn -> {
                dragging = n;
                dragX = x;
                dragW = w;
                n.setRatio((mx - x) / w);
            });
            return y + 36;
        }
        if (s instanceof ModeSetting m) {
            cv.textMid(s.name, x, y + 7, TEXT, F.REGULAR);
            return drawSegmented(cv, m, x, y + 16, w, clipY, clipH) + 2;
        }
        if (s instanceof KeySetting k) {
            cv.textMid(s.name, x, y + 7, TEXT, F.REGULAR);
            return drawKeyField(cv, k, x, y + 16, w, clipY, clipH);
        }
        return y;
    }

    private float drawSegmented(Canvas cv, ModeSetting m, float x, float y, float w, float clipY, float clipH) {
        int n = m.modes.size();
        float h = 22, segW = (w - 4) / n;
        if (m.anim.get() < 0) m.anim.set(m.index());
        float pos = m.anim.to(m.index(), 16);
        cv.round(x, y, w, h, 8, 0xFF18181C);
        cv.outline(x, y, w, h, 8, 1f, 0x10FFFFFF);
        cv.shadow(x + 2 + pos * segW, y + 3, segW, h - 4, 6, 4, 0x50000000);
        cv.round(x + 2 + pos * segW, y + 2, segW, h - 4, 6.5f, WHITE);
        for (int i = 0; i < n; i++) {
            float on = Math.max(0, 1 - Math.abs(pos - i));
            float sx = x + 2 + i * segW;
            boolean hover = inside(sx, y, segW, h);
            cv.textCenter(m.modes.get(i), sx + segW / 2, y + h / 2, Canvas.mix(hover ? TEXT : MUTED, BLACK, on), F.SMALL);
            final int idx = i;
            hitClip(sx, y, segW, h, clipY, clipH, b -> m.set(m.modes.get(idx)));
        }
        return y + h + 8;
    }

    private float drawKeyField(Canvas cv, KeySetting k, float x, float y, float w, float clipY, float clipH) {
        float h = 26;
        boolean wait = listening == k;
        boolean hover = inside(x, y, w, h);
        float pulse = wait ? (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 170.0)) : 0;
        cv.round(x, y, w, h, 8, hover || wait ? 0xFF1A1A1F : 0xFF16161A);
        cv.outline(x, y, w, h, 8, 1f, wait ? Canvas.mix(0x50FFFFFF, 0xE0FFFFFF, pulse) : hover ? 0x2AFFFFFF : 0x12FFFFFF);
        cv.icon(Icons.KEYBOARD, x + 14, y + h / 2, wait ? WHITE : MUTED, F.ICON_SM);
        String text = wait ? "Naciśnij klawisz..." : k.isSet() ? k.keyName() : "Kliknij, aby przypisać";
        cv.textMid(text, x + 26, y + h / 2, wait || k.isSet() ? WHITE : DIM, F.REGULAR);
        if (k.isSet() && !wait) {
            float cx = x + w - 13;
            boolean ch = inside(cx - 9, y + 4, 18, 18);
            cv.circle(cx, y + h / 2, 8, ch ? 0xFF2A2A30 : 0xFF1F1F24);
            cv.icon(Icons.X, cx, y + h / 2, ch ? WHITE : MUTED, F.ICON_SM);
            hitClip(x, y, w - 24, h, clipY, clipH, b -> listening = k);
            hitClip(cx - 10, y + 3, 20, 20, clipY, clipH, b -> k.set(GLFW.GLFW_KEY_UNKNOWN));
        } else {
            hitClip(x, y, w, h, clipY, clipH, b -> {
                if (b == 1) k.set(GLFW.GLFW_KEY_UNKNOWN);
                else listening = k;
            });
        }
        return y + h + 6;
    }

    // ===================================================== pomocnicze

    private List<Module> visibleModules() {
        if (query.isEmpty()) return ModuleManager.in(current);
        String q = query.toLowerCase(Locale.ROOT);
        return ModuleManager.all().stream()
                .filter(mo -> mo.name.toLowerCase(Locale.ROOT).contains(q) || mo.description.toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    private static String plural(int n) {
        if (n == 1) return "moduł";
        int d = n % 10, t = n % 100;
        return d >= 2 && d <= 4 && (t < 12 || t > 14) ? "moduły" : "modułów";
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

    private void selectCategory(Category c) {
        if (c == current && query.isEmpty()) return;
        SAVED_SCROLL.put(current, scrollTarget);
        current = c;
        query = "";
        searchActive = false;
        closeDrawer();
        scrollTarget = SAVED_SCROLL.getOrDefault(c, 0f);
        scroll.set(scrollTarget);
        content.set(0);
    }

    private void click(float pitch) {
        if (cfg().sounds.on()) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.22f));
        }
    }

    // ===================================================== wejście

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
        searchActive = false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.in(mx, my)) {
                h.action.accept(button);
                click(button == 1 ? 0.85f : 1.15f);
                return true;
            }
        }
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
        if (drawerOpen && mx >= wx + W - DRAWER) drawerScrollTarget -= (float) v * 28;
        else if (!drawerOpen) scrollTarget -= (float) v * 34;
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
            searchActive = true;
            closeDrawer();
            return true;
        }
        if (searchActive) {
            switch (key) {
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    if (ctrl) query = "";
                    else if (!query.isEmpty()) query = query.substring(0, query.length() - 1);
                    scrollTarget = 0;
                }
                case GLFW.GLFW_KEY_ESCAPE -> {
                    if (query.isEmpty()) searchActive = false;
                    else query = "";
                }
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    searchActive = false;
                    List<Module> list = visibleModules();
                    if (list.size() == 1 && list.get(0).hasToggle()) list.get(0).toggle();
                }
                default -> {
                }
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && drawerOpen) {
            closeDrawer();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE || FuzirClient.OPEN_KEY.matchesKey(key, scan)) {
            close();
            return true;
        }
        if (!drawerOpen && (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN)) {
            Category[] c = Category.values();
            selectCategory(c[Math.floorMod(current.ordinal() + (key == GLFW.GLFW_KEY_DOWN ? 1 : -1), c.length)]);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (closing || listening != null) return true;
        if (!searchActive) {
            if (!Character.isLetterOrDigit(chr)) return false;
            searchActive = true;
            closeDrawer();
        }
        if (chr >= ' ' && query.length() < 24) {
            query += chr;
            scrollTarget = 0;
        }
        return true;
    }

    /** Otwiera panel ustawień modułu. */
    public void openSettings(Module m) {
        openDrawer(m);
    }
}
