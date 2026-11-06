package pl.firen.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;
import pl.firen.client.FirenClient;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.module.modules.ClickGui;
import pl.firen.client.render.Fonts;
import pl.firen.client.render.Render2D;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.ModeSetting;
import pl.firen.client.setting.NumberSetting;
import pl.firen.client.setting.Setting;
import pl.firen.client.util.Anim;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

import static pl.firen.client.gui.Theme.*;

/** Nowoczesne ClickGUI: okno z bocznym menu kategorii i kartami modułów. */
public class ClickGuiScreen extends Screen {
    // Stan zapamiętywany między otwarciami (i w configu)
    public static Category category = Category.COMBAT;
    public static float winX = -1, winY = -1;
    private static String query = "";

    private static final float SIDEBAR_W = 128;
    private static final float CAT_ROW = 26;
    private static final float CARD_HEAD = 38;
    private static final float ROW_BOOL = 20, ROW_NUM = 28, ROW_MODE = 34, ROW_BIND = 20;

    private record Hit(float x1, float y1, float x2, float y2, IntConsumer action) {}

    private final List<Hit> hits = new ArrayList<>();
    private final long openedAt = System.nanoTime();
    private long closingAt = -1;
    private long lastFrame = System.nanoTime();

    private float s = 1f;   // skala: 1 jednostka = 2 px (ostre czcionki i kształty)
    private int vw, vh;
    private float catAnim = -1f;
    private float scroll, scrollTarget, maxScroll;

    private boolean draggingWindow;
    private float dragOffX, dragOffY;
    private NumberSetting slider;
    private float sliderX, sliderW;
    private Module binding;

    public ClickGuiScreen() {
        super(Text.literal("FirenClient"));
    }

    @Override
    protected void init() {
        double factor = client.getWindow().getScaleFactor();
        int target = client.getWindow().getFramebufferWidth() >= 1280 ? 2 : 1;
        s = (float) (target / factor);
        vw = Math.round(width / s);
        vh = Math.round(height / s);
    }

    public static String keyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return "Brak";
        return InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString().toUpperCase(Locale.ROOT);
    }

    private void hit(float x1, float y1, float x2, float y2, IntConsumer action) {
        hits.add(new Hit(x1, y1, x2, y2, action));
    }

    private float progress() {
        float open = Math.min(1f, (System.nanoTime() - openedAt) / 260_000_000f);
        if (closingAt < 0) return Anim.easeOutCubic(open);
        float close = Math.min(1f, (System.nanoTime() - closingAt) / 160_000_000f);
        return Anim.easeOutCubic(open) * (1f - close);
    }

    // ------------------------------------------------------------------ render

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // vanilla: rozmycie świata (albo panorama w menu głównym)
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
        ctx.fillGradient(0, 0, width, height, Anim.withAlpha(0x66000000, p), Anim.withAlpha(0xAA000000, p));

        hits.clear();
        Render2D.alpha = p;

        float W = Math.min(580, vw - 32), H = Math.min(350, vh - 32);
        if (winX < 0) { winX = (vw - W) / 2f; winY = (vh - H) / 2f; }
        if (draggingWindow) { winX = mx - dragOffX; winY = my - dragOffY; }
        winX = Math.max(4, Math.min(vw - W - 4, winX));
        winY = Math.max(4, Math.min(vh - H - 4, winY));
        float x = winX, y = winY;

        var m = ctx.getMatrices();
        m.push();
        m.scale(s, s, 1f);
        float zoom = 0.94f + 0.06f * p;
        m.translate(x + W / 2f, y + H / 2f + (1 - p) * 8f, 0);
        m.scale(zoom, zoom, 1f);
        m.translate(-(x + W / 2f), -(y + H / 2f), 0);

        // Okno
        Render2D.shadow(ctx, x, y + 6, W, H, 16, 30, 0xB0000000);
        Render2D.gradientV(ctx, x, y, W, H, 16, WINDOW_TOP, WINDOW_BOTTOM);
        Render2D.outline(ctx, x, y, W, H, 16, 0.5f, 0x1EFFFFFF);
        // delikatna poświata u góry okna
        Render2D.gradientV(ctx, x + 1, y + 1, W - 2, 60, 15, 0x0CFFFFFF, 0x00FFFFFF);

        renderSidebar(ctx, x, y, H, mx, my, dt);
        renderContent(ctx, x + SIDEBAR_W + 20, y + 16, W - SIDEBAR_W - 34, H - 26, mx, my, dt);

        m.pop();
        Render2D.alpha = 1f;
    }

    private void renderSidebar(DrawContext ctx, float x, float y, float H, float mx, float my, float dt) {
        float sx = x + 8, sy = y + 8, sw = SIDEBAR_W, sh = H - 16;
        Render2D.round(ctx, sx, sy, sw, sh, 12, SIDEBAR);
        Render2D.outline(ctx, sx, sy, sw, sh, 12, 0.5f, STROKE);

        // Logo
        Render2D.shadow(ctx, sx + 10, sy + 11, 24, 24, 8, 10, 0x40FFFFFF);
        Render2D.gradientV(ctx, sx + 10, sy + 10, 24, 24, 8, 0xFFFFFFFF, 0xFFD6D6DC);
        Fonts.drawCentered(ctx, Fonts.I_FIRE, Fonts.ICONS, sx + 22, sy + 17.5f, BLACK);
        Fonts.draw(ctx, "FirenClient", Fonts.SEMIBOLD, sx + 41, sy + 12, TEXT);
        Fonts.draw(ctx, "v" + FirenClient.VERSION, Fonts.SMALL, sx + 41, sy + 23, TEXT_3);
        hit(sx, sy, sx + sw, sy + 40, b -> {
            if (b == 0) { draggingWindow = true; dragOffX = mx - winX; dragOffY = my - winY; }
        });

        Fonts.draw(ctx, "KATEGORIE", Fonts.SMALL, sx + 14, sy + 52, TEXT_3);

        Category[] cats = Category.values();
        float listY = sy + 64;
        int active = query.isEmpty() ? category.ordinal() : -1;
        float targetY = active < 0 ? catAnim : active * CAT_ROW;
        catAnim = catAnim < 0 ? targetY : Anim.approach(catAnim, targetY, dt, 18f);

        if (active >= 0) {
            float py = listY + catAnim;
            Render2D.shadow(ctx, sx + 8, py + 1, sw - 16, CAT_ROW - 4, 8, 10, 0x30FFFFFF);
            Render2D.round(ctx, sx + 8, py, sw - 16, CAT_ROW - 4, 8, WHITE);
        }

        for (int i = 0; i < cats.length; i++) {
            Category c = cats[i];
            float ry = listY + i * CAT_ROW;
            boolean hover = mx >= sx + 8 && mx < sx + sw - 8 && my >= ry && my < ry + CAT_ROW - 4;
            float t = active < 0 ? 0f : Math.max(0f, 1f - Math.abs(catAnim - i * CAT_ROW) / CAT_ROW);
            if (hover && i != active) Render2D.round(ctx, sx + 8, ry, sw - 16, CAT_ROW - 4, 8, 0x0DFFFFFF);
            int col = Anim.lerpColor(hover ? TEXT : TEXT_2, BLACK, t);
            Fonts.drawCentered(ctx, c.icon, Fonts.ICONS, sx + 23, ry + 7.5f, col);
            Fonts.draw(ctx, c.display, Fonts.SEMIBOLD, sx + 36, ry + 7, col);

            long on = FirenClient.modules().byCategory(c).stream().filter(Module::isEnabled).count();
            if (on > 0) {
                String n = String.valueOf(on);
                float bw = Math.max(14, Fonts.width(n, Fonts.SMALL) + 8);
                float bx = sx + sw - 14 - bw;
                Render2D.round(ctx, bx, ry + 5, bw, 12, 6, Anim.lerpColor(0x1AFFFFFF, 0xFF000000, t));
                Fonts.drawCentered(ctx, n, Fonts.SMALL, bx + bw / 2f, ry + 6.5f, Anim.lerpColor(TEXT, WHITE, t));
            }
            hit(sx + 8, ry, sx + sw - 8, ry + CAT_ROW - 4, b -> {
                if (category != c || !query.isEmpty()) { category = c; query = ""; scrollTarget = 0; }
            });
        }

        // Karta użytkownika
        float uy = sy + sh - 42;
        Render2D.round(ctx, sx + 8, uy, sw - 16, 34, 10, 0x0AFFFFFF);
        Render2D.outline(ctx, sx + 8, uy, sw - 16, 34, 10, 0.5f, STROKE);
        Render2D.gradientV(ctx, sx + 15, uy + 7, 20, 20, 10, 0xFF3A3A42, 0xFF1E1E24);
        Fonts.drawCentered(ctx, Fonts.I_USER, Fonts.ICONS, sx + 25, uy + 13, TEXT);
        String name = Fonts.trim(client.getSession().getUsername(), Fonts.SEMIBOLD, sw - 60);
        Fonts.draw(ctx, name, Fonts.SEMIBOLD, sx + 42, uy + 8, TEXT);
        Fonts.draw(ctx, "RShift · zamknij", Fonts.SMALL, sx + 42, uy + 19, TEXT_3);
    }

    private void renderContent(DrawContext ctx, float cx, float cy, float cw, float ch, float mx, float my, float dt) {
        String q = query.toLowerCase(Locale.ROOT);
        List<Module> modules = q.isEmpty()
                ? FirenClient.modules().byCategory(category)
                : FirenClient.modules().all().stream()
                    .filter(md -> md.getName().toLowerCase(Locale.ROOT).contains(q)).toList();

        // Nagłówek
        String title = q.isEmpty() ? category.display : "Wyniki";
        Fonts.draw(ctx, title, Fonts.TITLE, cx, cy + 1, WHITE);
        long on = modules.stream().filter(Module::isEnabled).count();
        Fonts.draw(ctx, modules.size() + " modułów  ·  " + on + " aktywnych", Fonts.SMALL, cx, cy + 18, TEXT_3);

        // Wyszukiwarka
        float sw = 150, sh = 24, sx = cx + cw - sw, sy = cy + 1;
        Render2D.round(ctx, sx, sy, sw, sh, 9, FIELD);
        Render2D.outline(ctx, sx, sy, sw, sh, 9, 0.5f, q.isEmpty() ? STROKE : 0x80FFFFFF);
        Fonts.draw(ctx, Fonts.I_SEARCH, Fonts.ICONS, sx + 10, sy + 8.5f, q.isEmpty() ? TEXT_3 : WHITE);
        boolean caret = (System.currentTimeMillis() / 530) % 2 == 0;
        if (q.isEmpty()) Fonts.draw(ctx, "Szukaj modułu…", Fonts.REGULAR, sx + 25, sy + 8, TEXT_3);
        else Fonts.draw(ctx, query + (caret ? "|" : ""), Fonts.REGULAR, sx + 25, sy + 8, TEXT);

        // Karty modułów (dwie kolumny, układ "masonry")
        float top = cy + 36, bottom = cy + ch;
        float gap = 10, colW = (cw - gap) / 2f;
        float[] colH = {0, 0};
        scroll = Anim.approach(scroll, scrollTarget, dt, 14f);

        // DrawContext sam przelicza scissor przez aktualną macierz (skala + animacja)
        ctx.enableScissor((int) (cx - 8), (int) top, (int) Math.ceil(cx + cw + 8), (int) Math.ceil(bottom));
        for (Module md : modules) {
            md.expandAnim = Anim.approach(md.expandAnim, md.expanded ? 1f : 0f, dt, 16f);
            float h = CARD_HEAD + settingsHeight(md) * md.expandAnim;
            int col = colH[0] <= colH[1] ? 0 : 1;
            float kx = cx + col * (colW + gap);
            float ky = top + colH[col] - scroll;
            colH[col] += h + gap;
            if (ky > bottom || ky + h < top) {
                md.hoverAnim = 0;
                continue;
            }
            renderCard(ctx, md, kx, ky, colW, h, mx, my, dt, top, bottom);
        }
        ctx.disableScissor();

        float contentH = Math.max(colH[0], colH[1]) - gap;
        maxScroll = Math.max(0, contentH - (bottom - top));
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));

        // Pasek przewijania
        if (maxScroll > 0) {
            float trackH = bottom - top;
            float barH = Math.max(20, trackH * trackH / (trackH + maxScroll));
            float barY = top + (trackH - barH) * (scroll / maxScroll);
            Render2D.round(ctx, cx + cw + 6, barY, 3, barH, 1.5f, 0x40FFFFFF);
        }

        if (modules.isEmpty()) {
            Fonts.drawCentered(ctx, "Brak modułów dla „" + query + "”", Fonts.REGULAR, cx + cw / 2f, top + 40, TEXT_3);
        }
    }

    private static float settingsHeight(Module m) {
        float h = 10;
        for (Setting<?> st : m.getSettings()) {
            if (st instanceof BooleanSetting) h += ROW_BOOL;
            else if (st instanceof NumberSetting) h += ROW_NUM;
            else if (st instanceof ModeSetting) h += ROW_MODE;
        }
        return h + ROW_BIND + 4;
    }

    private void renderCard(DrawContext ctx, Module md, float x, float y, float w, float h,
                            float mx, float my, float dt, float clipTop, float clipBottom) {
        boolean inClip = my >= clipTop && my < clipBottom;
        boolean hover = inClip && mx >= x && mx < x + w && my >= y && my < y + CARD_HEAD;
        md.hoverAnim = Anim.approach(md.hoverAnim, hover ? 1f : 0f, dt, 18f);
        md.toggleAnim = Anim.approach(md.toggleAnim, md.isEnabled() ? 1f : 0f, dt, 16f);
        float t = md.toggleAnim;

        if (t > 0.01f) Render2D.shadow(ctx, x, y + 2, w, h, 12, 14, Anim.withAlpha(0x2EFFFFFF, t));
        Render2D.shadow(ctx, x, y + 3, w, h, 12, 8, 0x50000000);
        Render2D.gradientV(ctx, x, y, w, h, 12,
                Anim.lerpColor(CARD, CARD_HOVER, md.hoverAnim), Anim.lerpColor(0xFF0F0F12, 0xFF141418, md.hoverAnim));
        Render2D.outline(ctx, x, y, w, h, 12, 0.5f, Anim.lerpColor(STROKE, 0x66FFFFFF, t));

        // Nazwa + opis
        Fonts.draw(ctx, md.getName(), Fonts.SEMIBOLD, x + 13, y + 10, TEXT);
        String desc = Fonts.trim(md.getDescription(), Fonts.SMALL, w - 82);
        Fonts.draw(ctx, desc, Fonts.SMALL, x + 13, y + 22, TEXT_3);

        // Przełącznik
        float tw = 24, th = 13, tx = x + w - 13 - tw, ty = y + 12.5f;
        Render2D.round(ctx, tx, ty, tw, th, th / 2f, Anim.lerpColor(TRACK, WHITE, t));
        if (t > 0.01f) Render2D.shadow(ctx, tx, ty, tw, th, th / 2f, 8, Anim.withAlpha(0x55FFFFFF, t));
        float knob = tx + 6.5f + t * 11f;
        Render2D.circle(ctx, knob, ty + th / 2f, 4.5f, Anim.lerpColor(0xFFB5B5BD, BLACK, t));

        // Strzałka rozwijania
        float ax = tx - 16, ay = y + 19;
        boolean arrowHover = inClip && mx >= ax - 7 && mx < ax + 7 && my >= ay - 7 && my < ay + 7;
        if (arrowHover) Render2D.circle(ctx, ax, ay, 8, 0x14FFFFFF);
        var m = ctx.getMatrices();
        m.push();
        m.translate(ax, ay, 0);
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180f * md.expandAnim));
        Fonts.drawCentered(ctx, Fonts.I_CHEVRON, Fonts.ICONS, 0, -3.5f, arrowHover ? TEXT : TEXT_3);
        m.pop();

        float headBottom = Math.min(y + CARD_HEAD, clipBottom), headTop = Math.max(y, clipTop);
        if (headBottom > headTop) {
            hit(ax - 8, Math.max(ay - 8, clipTop), ax + 8, Math.min(ay + 8, clipBottom), b -> md.expanded = !md.expanded);
            hit(x, headTop, x + w, headBottom, b -> {
                if (b == 0) md.toggle();
                else if (b == 1) md.expanded = !md.expanded;
                else if (b == 2) binding = binding == md ? null : md;
            });
        }

        if (md.expandAnim < 0.01f) return;

        // Ustawienia
        float sTop = y + CARD_HEAD, sBottom = y + h;
        ctx.enableScissor((int) x, (int) Math.max(sTop, clipTop), (int) Math.ceil(x + w), (int) Math.ceil(Math.min(sBottom, clipBottom)));
        Render2D.round(ctx, x + 13, sTop - 1, w - 26, 0.5f, 0, 0x14FFFFFF);

        float ix = x + 13, iw = w - 26;
        float ry = sTop + 8;
        for (Setting<?> st : md.getSettings()) {
            float rowTop = ry;
            boolean rowVisible = rowTop < Math.min(sBottom, clipBottom) && rowTop > clipTop - 40;
            if (st instanceof BooleanSetting b) {
                b.anim = Anim.approach(b.anim, b.get() ? 1f : 0f, dt, 18f);
                Fonts.draw(ctx, b.getName(), Fonts.REGULAR, ix, ry + 5, TEXT_2);
                float bw = 18, bh = 10, bx = ix + iw - bw, by = ry + 4;
                Render2D.round(ctx, bx, by, bw, bh, 5, Anim.lerpColor(TRACK, WHITE, b.anim));
                Render2D.circle(ctx, bx + 5 + b.anim * 8, by + 5, 3.5f, Anim.lerpColor(0xFFB5B5BD, BLACK, b.anim));
                if (rowVisible) hit(ix - 4, clampY(ry, clipTop, clipBottom), ix + iw + 4, clampY(ry + ROW_BOOL, clipTop, clipBottom), k -> b.toggle());
                ry += ROW_BOOL;
            } else if (st instanceof NumberSetting n) {
                if (slider == n) n.setFromPercent((mx - sliderX) / sliderW);
                float pct = (float) n.getPercent();
                n.anim = n.anim < 0 ? pct : Anim.approach(n.anim, pct, dt, 22f);
                Fonts.draw(ctx, n.getName(), Fonts.REGULAR, ix, ry + 3, TEXT_2);
                Fonts.drawRight(ctx, n.display(), Fonts.SEMIBOLD, ix + iw, ry + 3, TEXT);
                float ty2 = ry + 17;
                Render2D.round(ctx, ix, ty2, iw, 4, 2, TRACK);
                Render2D.gradientH(ctx, ix, ty2, Math.max(4, iw * n.anim), 4, 2, 0xFF8C8C94, WHITE);
                float kx = ix + iw * n.anim;
                Render2D.shadow(ctx, kx - 5, ty2 - 3, 10, 10, 5, 5, 0x60000000);
                Render2D.circle(ctx, kx, ty2 + 2, slider == n ? 5.5f : 4.5f, WHITE);
                if (rowVisible) hit(ix - 4, clampY(ry, clipTop, clipBottom), ix + iw + 4, clampY(ry + ROW_NUM, clipTop, clipBottom), k -> {
                    if (k == 0) { slider = n; sliderX = ix; sliderW = iw; }
                });
                ry += ROW_NUM;
            } else if (st instanceof ModeSetting md2) {
                List<String> modes = md2.getModes();
                Fonts.draw(ctx, md2.getName(), Fonts.REGULAR, ix, ry + 3, TEXT_2);
                float segY = ry + 14, segH = 16, segW = iw / modes.size();
                md2.anim = md2.anim < 0 ? md2.index() : Anim.approach(md2.anim, md2.index(), dt, 18f);
                Render2D.round(ctx, ix, segY, iw, segH, 7, 0xFF1C1C21);
                Render2D.round(ctx, ix + 2 + md2.anim * segW, segY + 2, segW - 4, segH - 4, 5, WHITE);
                for (int i = 0; i < modes.size(); i++) {
                    float active = Math.max(0f, 1f - Math.abs(md2.anim - i));
                    Fonts.drawCentered(ctx, modes.get(i), Fonts.SMALL, ix + segW * i + segW / 2f, segY + 4.5f, Anim.lerpColor(TEXT_2, BLACK, active));
                    final int idx = i;
                    if (rowVisible) hit(ix + segW * i, clampY(segY, clipTop, clipBottom), ix + segW * (i + 1), clampY(segY + segH, clipTop, clipBottom), k -> md2.setIndex(idx));
                }
                ry += ROW_MODE;
            }
        }

        // Bind
        boolean waiting = binding == md;
        String key = waiting ? "Naciśnij klawisz…" : keyName(md.getKey());
        Fonts.draw(ctx, Fonts.I_KEYBOARD, Fonts.ICONS, ix, ry + 5.5f, TEXT_3);
        Fonts.draw(ctx, "Bind", Fonts.REGULAR, ix + 14, ry + 5, TEXT_2);
        float kw = Fonts.width(key, Fonts.SMALL) + 14;
        float kx = ix + iw - kw;
        Render2D.round(ctx, kx, ry + 2, kw, 14, 6, waiting ? WHITE : 0xFF1F1F25);
        Fonts.drawCentered(ctx, key, Fonts.SMALL, kx + kw / 2f, ry + 5.5f, waiting ? BLACK : TEXT);
        if (ry < Math.min(sBottom, clipBottom)) hit(ix - 4, clampY(ry, clipTop, clipBottom), ix + iw + 4, clampY(ry + ROW_BIND, clipTop, clipBottom),
                k -> binding = binding == md ? null : md);

        ctx.disableScissor();
    }

    private static float clampY(float v, float top, float bottom) {
        return Math.max(top, Math.min(bottom, v));
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double rawX, double rawY, int button) {
        if (closingAt > 0) return true;
        double mx = rawX / s, my = rawY / s;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (mx >= h.x1 && mx < h.x2 && my >= h.y1 && my < h.y2) {
                h.action.accept(button);
                return true;
            }
        }
        return super.mouseClicked(rawX, rawY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingWindow = false;
        slider = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scrollTarget = (float) Math.max(0, Math.min(maxScroll, scrollTarget - vertical * 32));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding != null) {
            boolean clear = keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE;
            binding.setKey(clear ? GLFW.GLFW_KEY_UNKNOWN : keyCode);
            binding = null;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
            query = query.substring(0, query.length() - 1);
            scrollTarget = 0;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == FirenClient.modules().get(ClickGui.class).getKey()) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (binding == null && query.length() < 20 && (Character.isLetterOrDigit(chr) || chr == ' ')) {
            query += chr;
            scrollTarget = 0;
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
