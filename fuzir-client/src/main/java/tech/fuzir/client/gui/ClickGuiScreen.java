package tech.fuzir.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.SkinTextures;
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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;

public class ClickGuiScreen extends Screen {
    // wymiary okna w jednostkach (przy 1080p jedna jednostka to 2 piksele)
    private static final float W = 500, H = 310, SIDE = 128, HEAD = 40;

    // paleta: tylko czerń, biel i szarości
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BG = 0xF50A0A0A;
    private static final int SIDEBAR = 0xFF0E0E0E;
    private static final int LINE = 0xFF1E1E1E;
    private static final int CARD = 0xFF111111;
    private static final int CARD_HOVER = 0xFF171717;
    private static final int TEXT = 0xFFEDEDED;
    private static final int MUTED = 0xFF7A7A7A;
    private static final int DIM = 0xFF4A4A4A;

    private static Category current = Category.MOVEMENT;
    private static final Map<Category, Float> SAVED_SCROLL = new EnumMap<>(Category.class);

    private final Anim open = new Anim(0);
    private final Anim indicator = new Anim(-1);
    private final Anim scroll = new Anim(0);
    private final Anim searchFocus = new Anim(0);
    private final Anim contentFade = new Anim(1);
    private final List<Hit> hits = new ArrayList<>();

    private boolean closing;
    private float scrollTarget;
    private String query = "";
    private boolean searchActive;
    private KeySetting listening;
    private NumberSetting dragging;
    private float dragX, dragW;
    private float u, wx, wy, mx, my;

    private record Hit(float x, float y, float w, float h, IntConsumer action) {
        boolean in(float px, float py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }

    public ClickGuiScreen() {
        super(Text.literal("Fuzir Client"));
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

    // ================= rysowanie =================

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Anim.speedMul = cfg().speed.f();
        float o = open.to(closing ? 0 : 1, closing ? 16 : 11);
        if (closing && o < 0.02f) {
            client.setScreen(null);
            return;
        }

        if (cfg().blur.on() && o > 0.5f) super.renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(0, 0, width, height, Canvas.argb((int) (110 * o), 0));

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

        // lekki "wjazd" okna: skalowanie od środka i przesunięcie w górę
        float e = Anim.easeOutCubic(o);
        MatrixStack m = ctx.getMatrices();
        m.translate((wx + W / 2) * u, (wy + H / 2) * u, 0);
        float sc = 0.93f + 0.07f * e;
        m.scale(sc, sc, 1);
        m.translate(-(wx + W / 2) * u, -(wy + H / 2) * u + (1 - e) * 14 * u, 0);

        hits.clear();
        drawWindow(cv);
        drawSidebar(cv, ctx);
        drawHeader(cv);
        drawModules(cv);
        cv.end();
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // tło rysujemy sami w render(), żeby dało się je wyciszyć animacją
    }

    private void drawWindow(Canvas cv) {
        for (int i = 1; i <= 7; i++) {
            float s = i * 2.2f;
            cv.round(wx - s, wy - s + 5, W + s * 2, H + s * 2, 12 + s, Canvas.argb(16, 0));
        }
        cv.roundBorder(wx, wy, W, H, 12, BG, 0xFF242424);
        cv.round(wx + 1, wy + 1, SIDE, H - 2, 11, SIDEBAR);
        cv.rect(wx + SIDE - 12, wy + 1, 12, H - 2, SIDEBAR);
        cv.rect(wx + SIDE, wy + 1, 1f / u * Math.max(1, Math.round(u / 2)), H - 2, LINE);
    }

    private void drawSidebar(Canvas cv, DrawContext ctx) {
        float x = wx + 14, y = wy + 16;
        // logo
        cv.round(x, y, 20, 20, 6, WHITE);
        float fw = cv.textWidthBold("F", 1);
        cv.textBold("F", x + 10 - fw / 2 + 0.5f, y + 6, 0xFF000000, 1);
        cv.textBold("FUZIR", x + 28, y + 2, WHITE, 1);
        cv.text("client 1.0", x + 28, y + 12, MUTED, 1);

        cv.text("KATEGORIE", x, wy + 56, DIM, 0.5f);

        Category[] cats = Category.values();
        float rowY = wy + 66, rowH = 26, rx = wx + 8, rw = SIDE - 16;
        int idx = query.isEmpty() ? current.ordinal() : -1;
        if (indicator.get() < 0 && idx >= 0) indicator.set(idx);
        float ind = indicator.to(idx < 0 ? indicator.get() : idx, 18);
        float indAlpha = idx < 0 ? 0.35f : 1f;
        cv.round(rx, rowY + ind * rowH, rw, 22, 7, Canvas.mulAlpha(0xFF1B1B1B, indAlpha));
        cv.round(rx - 3, rowY + ind * rowH + 6, 3, 10, 1.5f, Canvas.mulAlpha(WHITE, indAlpha));

        for (int i = 0; i < cats.length; i++) {
            Category c = cats[i];
            float ry = rowY + i * rowH;
            boolean active = i == idx;
            boolean hover = inside(rx, ry, rw, 22);
            int txt = active ? WHITE : hover ? 0xFFCFCFCF : 0xFF8C8C8C;
            cv.roundBorder(rx + 6, ry + 4, 14, 14, 4, active ? WHITE : 0xFF151515, active ? WHITE : 0xFF2A2A2A);
            float iw = cv.textWidth(c.icon, 1);
            cv.text(c.icon, rx + 13 - iw / 2 + 0.5f, ry + 7, active ? 0xFF000000 : txt, 1);
            cv.text(c.title, rx + 27, ry + 7, txt, 1);
            List<Module> list = ModuleManager.in(c);
            long on = list.stream().filter(mo -> mo.hasToggle() && mo.isEnabled()).count();
            long all = list.stream().filter(Module::hasToggle).count();
            if (all > 0) {
                String cnt = on + "/" + all;
                cv.text(cnt, rx + rw - 7 - cv.textWidth(cnt, 0.5f), ry + 9, active ? 0xFF9A9A9A : DIM, 0.5f);
            }
            hit(rx, ry, rw, 22, b -> selectCategory(c));
        }

        // karta gracza na dole panelu
        float py = wy + H - 42, px = wx + 8, pw = SIDE - 16;
        cv.roundBorder(px, py, pw, 32, 9, 0xFF131313, 0xFF1F1F1F);
        if (cv.alpha > 0.6f) {
            SkinTextures skin = client.getSkinProvider().getSkinTextures(client.getGameProfile());
            PlayerSkinDrawer.draw(ctx, skin, cv.px(px + 7), cv.px(py + 7), cv.px(18));
        }
        String nick = client.getSession().getUsername();
        cv.text(cv.trim(nick, pw - 38, 1), px + 31, py + 7, TEXT, 1);
        String key = FuzirClient.OPEN_KEY.getBoundKeyLocalizedText().getString();
        cv.text(cv.trim(key + " - menu", pw - 38, 0.5f), px + 31, py + 19, MUTED, 0.5f);
    }

    private void drawHeader(Canvas cv) {
        float x = wx + SIDE + 16, y = wy + 14;
        boolean searching = !query.isEmpty();
        List<Module> list = visibleModules();
        String title = searching ? "Wyniki" : current.title;
        cv.textBold(title, x, y, WHITE, 2);
        String sub = searching ? list.size() + " " + plural(list.size()) + " dla \"" + query + "\"" : list.size() + " " + plural(list.size());
        cv.text(sub, x, y + 19, MUTED, 0.5f);

        // pole wyszukiwania
        float sw = 140, sh = 22, sx = wx + W - 16 - sw, sy = wy + 15;
        float f = searchFocus.to(searchActive ? 1 : 0, 16);
        boolean hover = inside(sx, sy, sw, sh);
        int border = Canvas.mix(hover ? 0xFF333333 : 0xFF242424, 0xFFBDBDBD, f);
        cv.roundBorder(sx, sy, sw, sh, 11, 0xFF101010, border);
        // lupa
        float lx = sx + 12, ly = sy + 10;
        cv.circle(lx, ly, 3.6f, Canvas.mix(MUTED, WHITE, f));
        cv.circle(lx, ly, 2.4f, 0xFF101010);
        cv.round(lx + 2.2f, ly + 2.2f, 2.2f, 2.2f, 1.1f, Canvas.mix(MUTED, WHITE, f));
        float tx = sx + 21;
        if (query.isEmpty()) {
            cv.text(searchActive ? "" : "Szukaj...", tx, sy + 7, DIM, 1);
            if (!searchActive) {
                String hint = "Ctrl+F";
                cv.text(hint, sx + sw - 10 - cv.textWidth(hint, 0.5f), sy + 9, 0xFF3A3A3A, 0.5f);
            }
        } else {
            String shown = query;
            while (cv.textWidth(shown, 1) > sw - 32 && shown.length() > 1) shown = shown.substring(1);
            cv.text(shown, tx, sy + 7, TEXT, 1);
            tx += cv.textWidth(shown, 1);
        }
        if (searchActive && (System.currentTimeMillis() / 530) % 2 == 0) cv.rect(tx + 0.5f, sy + 6, 1f, 10, WHITE);
        hit(sx, sy, sw, sh, b -> {
            searchActive = true;
            if (b == 1) query = "";
        });
    }

    private void drawModules(Canvas cv) {
        float x = wx + SIDE + 14, w = W - SIDE - 28;
        float top = wy + 52, h = H - 52 - 10;
        List<Module> list = visibleModules();
        float fade = contentFade.to(1, 10);
        float shift = (1 - fade) * 10;

        float total = 0;
        for (Module mo : list) total += cardHeight(mo, cv) + 6;
        float max = Math.max(0, total - 6 - h + 4);
        scrollTarget = Math.max(0, Math.min(max, scrollTarget));
        float sOff = scroll.to(scrollTarget, 16);

        float prevAlpha = cv.alpha;
        cv.alpha = prevAlpha * fade;
        cv.scissor(x - 2, top, w + 4, h);
        float y = top + 2 - sOff + shift;
        for (Module mo : list) {
            float ch = cardHeight(mo, cv);
            if (y + ch > top - 4 && y < top + h + 4) drawCard(cv, mo, x, y, w, top, h);
            y += ch + 6;
        }
        if (list.isEmpty()) {
            String t = "Nic nie znaleziono";
            cv.textBold(t, x + w / 2 - cv.textWidthBold(t, 1) / 2, top + 60, TEXT, 1);
            String s = "Spróbuj innej nazwy modułu";
            cv.text(s, x + w / 2 - cv.textWidth(s, 0.5f) / 2, top + 74, MUTED, 0.5f);
        }
        cv.endScissor();

        // delikatne wygaszenie przy krawędziach listy
        if (sOff > 1) cv.gradient(x - 2, top, w + 4, 10, 0xF00A0A0A, 0x000A0A0A);
        if (sOff < max - 1) cv.gradient(x - 2, top + h - 10, w + 4, 10, 0x000A0A0A, 0xF00A0A0A);
        // pasek przewijania
        if (max > 0) {
            float barH = Math.max(24, h * h / (total + 4));
            float barY = top + (h - barH) * (sOff / max);
            cv.round(wx + W - 9, barY, 2.5f, barH, 1.25f, 0xFF2C2C2C);
        }
        cv.alpha = prevAlpha;
    }

    private static final float CARD_H = 40;

    private float settingsHeight(Module mo) {
        float sum = 10;
        for (Setting<?> s : settingsOf(mo)) sum += rowHeight(s);
        return sum;
    }

    private float cardHeight(Module mo, Canvas cv) {
        return CARD_H + settingsHeight(mo) * mo.expandAnim.get();
    }

    private static float rowHeight(Setting<?> s) {
        return s instanceof NumberSetting ? 28 : 20;
    }

    private List<Setting<?>> settingsOf(Module mo) {
        List<Setting<?>> out = new ArrayList<>();
        for (Setting<?> s : mo.settings) if (s.isVisible()) out.add(s);
        if (mo.hasToggle()) out.add(mo.bind);
        return out;
    }

    private void drawCard(Canvas cv, Module mo, float x, float y, float w, float clipY, float clipH) {
        float ex = mo.expandAnim.to(mo.expanded ? 1 : 0, 15);
        float setH = settingsHeight(mo);
        float h = CARD_H + setH * ex;
        boolean hovered = inside(x, y, w, CARD_H) && my >= clipY && my <= clipY + clipH;
        float hv = mo.hoverAnim.to(hovered ? 1 : 0, 16);
        float t = mo.toggleAnim.to(mo.isEnabled() && mo.hasToggle() ? 1 : 0, 15);

        int fill = Canvas.mix(CARD, CARD_HOVER, hv);
        int border = Canvas.mix(Canvas.mix(0xFF1D1D1D, 0xFF343434, t), 0xFF3C3C3C, hv);
        cv.roundBorder(x, y, w, h, 9, fill, border);
        // biały znacznik włączonego modułu
        if (t > 0.01f) cv.round(x + 1, y + 13, 2.5f * t, 14, 1.2f, Canvas.mulAlpha(WHITE, t));

        float tx = x + 14;
        cv.textBold(mo.name, tx, y + 10, TEXT, 1);
        float right = x + w - 14;

        // przełącznik
        if (mo.hasToggle()) {
            float sw = 26, sh = 14, sx = right - sw, sy = y + (CARD_H - sh) / 2;
            cv.round(sx, sy, sw, sh, 7, Canvas.mix(0xFF262626, WHITE, t));
            float kx = sx + 2 + (sw - 14) * t;
            cv.circle(kx + 5, sy + 7, 5, Canvas.mix(0xFF8F8F8F, 0xFF0B0B0B, t));
            right = sx - 10;
        }
        // menu ustawień (trzy kropki)
        boolean dotsHover = inside(right - 10, y + 10, 14, 20);
        int dc = Canvas.mix(dotsHover || mo.expanded ? WHITE : 0xFF6A6A6A, WHITE, ex);
        for (int i = 0; i < 3; i++) cv.circle(right - 3, y + 14 + i * 6, 1.4f, dc);
        hitClip(right - 10, y + 8, 16, 24, clipY, clipH, b -> mo.expanded = !mo.expanded);
        right -= 16;
        // przypisany klawisz
        if (mo.bind.isSet()) {
            String k = mo.bind.keyName();
            float kw = cv.textWidth(k, 0.5f) + 10;
            cv.roundBorder(right - kw, y + 13, kw, 14, 4, 0xFF161616, 0xFF2A2A2A);
            cv.text(k, right - kw + 5, y + 18, 0xFFAFAFAF, 0.5f);
            right -= kw + 8;
        }
        cv.text(cv.trim(mo.description, right - tx - 4, 0.5f), tx, y + 24, MUTED, 0.5f);
        hitClip(x, y, w, CARD_H, clipY, clipH, b -> {
            if (b == 1 || !mo.hasToggle()) mo.expanded = !mo.expanded;
            else mo.toggle();
        });

        if (ex < 0.01f) return;
        float sy0 = y + CARD_H;
        float visH = setH * ex;
        float cy0 = Math.max(clipY, sy0), cy1 = Math.min(clipY + clipH, sy0 + visH);
        if (cy1 <= cy0) return;
        cv.scissor(x, cy0, w, cy1 - cy0);
        cv.rect(x + 14, sy0, w - 28, 1f / u * Math.max(1, Math.round(u / 2)), 0xFF1F1F1F);
        float ry = sy0 + 6;
        float prev = cv.alpha;
        cv.alpha = prev * Math.min(1, ex * 1.4f);
        for (Setting<?> s : settingsOf(mo)) {
            drawSetting(cv, s, x + 14, ry, w - 28, cy0, cy1 - cy0);
            ry += rowHeight(s);
        }
        cv.alpha = prev;
        cv.endScissor();
    }

    private void drawSetting(Canvas cv, Setting<?> s, float x, float y, float w, float clipY, float clipH) {
        boolean hover = inside(x - 4, y, w + 8, rowHeight(s)) && my >= clipY && my <= clipY + clipH;
        int label = hover ? TEXT : 0xFFB0B0B0;
        if (s instanceof BoolSetting b) {
            cv.text(s.name, x, y + 6, label, 0.5f);
            float t = b.anim.to(b.on() ? 1 : 0, 16);
            float sw = 20, sh = 11, sx = x + w - sw, sy = y + 4;
            cv.round(sx, sy, sw, sh, 5.5f, Canvas.mix(0xFF262626, WHITE, t));
            cv.circle(sx + 5.5f + (sw - 11) * t, sy + 5.5f, 3.8f, Canvas.mix(0xFF8F8F8F, 0xFF0B0B0B, t));
            hitClip(x - 4, y, w + 8, 18, clipY, clipH, btn -> b.toggle());
        } else if (s instanceof NumberSetting n) {
            cv.text(s.name, x, y + 4, label, 0.5f);
            String v = n.display();
            cv.text(v, x + w - cv.textWidth(v, 0.5f), y + 4, TEXT, 0.5f);
            float r = n.anim.to((float) n.ratio(), 20);
            float ty = y + 17;
            cv.round(x, ty, w, 3, 1.5f, 0xFF262626);
            cv.round(x, ty, Math.max(3, w * r), 3, 1.5f, WHITE);
            boolean active = dragging == n;
            float kr = active ? 5f : hover ? 4.5f : 4f;
            cv.circle(x + w * r, ty + 1.5f, kr, WHITE);
            cv.circle(x + w * r, ty + 1.5f, kr - 2f, 0xFF111111);
            hitClip(x - 4, y + 10, w + 8, 16, clipY, clipH, btn -> {
                dragging = n;
                dragX = x;
                dragW = w;
                n.setRatio((mx - x) / w);
            });
        } else if (s instanceof ModeSetting mo) {
            cv.text(s.name, x, y + 6, label, 0.5f);
            String v = mo.get();
            float pw = cv.textWidth(v, 0.5f) + 26, px = x + w - pw;
            cv.roundBorder(px, y + 2, pw, 15, 5, 0xFF161616, hover ? 0xFF3A3A3A : 0xFF262626);
            cv.text("<", px + 5, y + 6, MUTED, 0.5f);
            cv.text(">", px + pw - 5 - cv.textWidth(">", 0.5f), y + 6, MUTED, 0.5f);
            cv.text(v, px + 13, y + 6, TEXT, 0.5f);
            hitClip(px, y + 2, pw / 2, 15, clipY, clipH, btn -> mo.cycle(-1));
            hitClip(px + pw / 2, y + 2, pw / 2, 15, clipY, clipH, btn -> mo.cycle(btn == 1 ? -1 : 1));
            hitClip(x - 4, y, px - x, 18, clipY, clipH, btn -> mo.cycle(btn == 1 ? -1 : 1));
        } else if (s instanceof KeySetting k) {
            cv.text(s.name, x, y + 6, label, 0.5f);
            boolean wait = listening == k;
            String v = wait ? "Naciśnij klawisz..." : k.keyName();
            float pw = cv.textWidth(v, 0.5f) + 14, px = x + w - pw;
            int border = wait ? Canvas.mix(0xFF3A3A3A, WHITE, (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 160.0))) : hover ? 0xFF3A3A3A : 0xFF262626;
            cv.roundBorder(px, y + 2, pw, 15, 5, 0xFF161616, border);
            cv.text(v, px + 7, y + 6, wait ? WHITE : k.isSet() ? TEXT : MUTED, 0.5f);
            hitClip(x - 4, y, w + 8, 18, clipY, clipH, btn -> {
                if (btn == 1) k.set(GLFW.GLFW_KEY_UNKNOWN);
                else listening = k;
            });
        }
    }

    // ================= pomocnicze =================

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

    /** Obszar kliknięcia przycięty do widocznej części listy. */
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
        scrollTarget = SAVED_SCROLL.getOrDefault(c, 0f);
        scroll.set(scrollTarget);
        contentFade.set(0);
    }

    private void click(float pitch) {
        if (cfg().sounds.on()) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.25f));
        }
    }

    // ================= wejście =================

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
        boolean wasSearch = searchActive;
        searchActive = false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.in(mx, my)) {
                h.action.accept(button);
                click(button == 1 ? 0.8f : 1.1f);
                return true;
            }
        }
        if (wasSearch && !query.isEmpty() && !(mx >= wx && mx <= wx + W && my >= wy && my <= wy + H)) query = "";
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
        scrollTarget -= (float) v * 32;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (closing) return true;
        if (listening != null) {
            boolean clear = key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE;
            listening.set(clear ? GLFW.GLFW_KEY_UNKNOWN : key);
            listening = null;
            click(1.3f);
            Config.save();
            return true;
        }
        boolean ctrl = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
        if (ctrl && key == GLFW.GLFW_KEY_F) {
            searchActive = true;
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
        if (key == GLFW.GLFW_KEY_ESCAPE || FuzirClient.OPEN_KEY.matchesKey(key, scan)) {
            close();
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
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
        }
        if (chr >= ' ' && query.length() < 24) {
            query += chr;
            scrollTarget = 0;
        }
        return true;
    }
}
