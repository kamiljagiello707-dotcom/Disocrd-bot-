package dev.vesca.client.ui;

import dev.vesca.client.render.Fonts;
import dev.vesca.client.render.Fonts.Font;
import dev.vesca.client.render.R2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static dev.vesca.client.ui.Ui.*;

/**
 * Baza ekranów z listą (serwery, światy): wspólne tło, szklany panel, nagłówek z powrotem i wyszukiwarką,
 * przewijana lista z zaznaczeniem (mysz, dwuklik, ↑ ↓ Enter, Esc) i rząd przycisków na dole.
 */
public abstract class VescaScreen extends Screen {
    protected final Screen parent;
    protected final MinecraftClient mc = MinecraftClient.getInstance();
    private final Map<String, Anim> anims = new HashMap<>();
    private final long openTime = System.currentTimeMillis();

    protected int selected = -1;
    protected String query = "";
    private boolean searchFocused;
    protected int mx, my;
    private float listX, listY, listW, listH, scroll, scrollTarget, maxScroll;
    private final List<Integer> visible = new ArrayList<>();
    private long lastClickTime;
    private int lastClickRow = -2;

    private record Hit(float x, float y, float w, float h, int row, Runnable action) {
        boolean in(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; }
    }
    private final List<Hit> hits = new ArrayList<>();

    /** przycisk w stopce */
    protected record Btn(String key, String icon, String label, boolean primary, boolean enabled, Runnable action) {}

    protected VescaScreen(Screen parent, String title) {
        super(Text.literal(title));
        this.parent = parent;
    }

    // ------------------------------------------------------------------ do nadpisania

    protected abstract String subtitle();
    protected abstract String emptyText();
    protected abstract String emptyIcon();
    protected abstract int rowCount();
    /** tekst, po którym działa wyszukiwanie */
    protected abstract String rowSearch(int i);
    protected abstract void drawRow(DrawContext ctx, int i, float x, float y, float w, float h, float hover, boolean sel);
    /** dwuklik albo Enter na wierszu */
    protected abstract void activate(int i);
    protected abstract List<Btn> buttons();
    protected float rowHeight() { return 40; }
    /** podpowiedź pod panelem */
    protected String hint() { return t("wybierz • dwuklik dołącza", "select • double-click joins"); }
    protected void rightClick(int i) {}

    protected Anim anim(String k) { return anims.computeIfAbsent(k, s -> new Anim(0, 16)); }
    protected boolean hasSelection() { return selected >= 0 && selected < rowCount(); }

    @Override public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    // ------------------------------------------------------------------ render

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        mx = mouseX; my = mouseY;
        hits.clear();
        Scene.render(ctx, width, height, mouseX, mouseY, 1, 0.35f);
        float intro = Anim.easeOutCubic((System.currentTimeMillis() - openTime) / 380f);
        R2D.alpha = intro;

        float pw = Math.min(560, width - 36);
        float ph = height - 54;
        float px = (width - pw) / 2f, py = 16 + (1 - intro) * 10;
        R2D.shadow(ctx, px, py + 4, pw, ph, 22, 0xAA000000);
        R2D.roundRectV(ctx, px, py, pw, ph, 18, 0xD0111114, 0xE0070709);
        R2D.roundOutlineGrad(ctx, px, py, pw, ph, 18, 0.7f, 0x50FFFFFF, 0x12FFFFFF);
        R2D.gradientH(ctx, px + 18, py + 0.6f, (pw - 36) / 2f, 0.6f, 0x00FFFFFF, 0x50FFFFFF);
        R2D.gradientH(ctx, px + pw / 2f, py + 0.6f, (pw - 36) / 2f, 0.6f, 0x50FFFFFF, 0x00FFFFFF);

        // ---- nagłówek
        float hx = px + 14, hy = py + 13, bs = 24;
        float hb = anim("back").animate(inside(hx, hy, bs, bs) ? 1 : 0);
        R2D.roundRect(ctx, hx, hy, bs, bs, bs / 2f, R2D.blend(0x18FFFFFF, 0xFFF4F4F5, hb));
        R2D.roundOutline(ctx, hx, hy, bs, bs, bs / 2f, 0.5f, R2D.blend(0x26FFFFFF, 0xFFFFFFFF, hb));
        R2D.icon(ctx, "arrow_left", hx + bs / 2f - 5, hy + bs / 2f - 5, 10, R2D.blend(TEXT, BLACK, hb));
        hits.add(new Hit(hx, hy, bs, bs, -1, this::back));
        float tx = hx + bs + 10;
        logo(ctx, tx, hy + 3, 18, 0);
        tx += 26;
        Fonts.draw(ctx, getTitle().getString(), tx, hy + 1, TEXT, Font.BOLD, 1.05f);
        Fonts.draw(ctx, Fonts.trim(subtitle(), Font.REGULAR, 0.62f, pw - 230), tx, hy + 15, DIM, Font.REGULAR, 0.62f);
        // wyszukiwarka
        float sw = Math.min(150, pw * 0.3f), sh = 20, sx = px + pw - 14 - sw, sy = hy + 2;
        float sf = anim("search").animate(searchFocused ? 1 : 0), shv = anim("searchHover").animate(inside(sx, sy, sw, sh) ? 1 : 0);
        R2D.roundRect(ctx, sx, sy, sw, sh, sh / 2f, R2D.blend(0x12FFFFFF, 0x22FFFFFF, Math.max(sf, shv * 0.6f)));
        R2D.roundOutline(ctx, sx, sy, sw, sh, sh / 2f, 0.6f, R2D.blend(0x22FFFFFF, 0xCCFFFFFF, sf));
        R2D.icon(ctx, "search", sx + 8, sy + sh / 2f - 4, 8, R2D.blend(FAINT, TEXT, sf));
        String shown = query.isEmpty() ? t("Szukaj…", "Search…") : query;
        Fonts.drawV(ctx, Fonts.trim(shown, Font.REGULAR, 0.66f, sw - 30), sx + 21, sy + sh / 2f + 0.5f, query.isEmpty() ? FAINT : TEXT, Font.REGULAR, 0.66f);
        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            float cx = sx + 21 + (query.isEmpty() ? 0 : Fonts.width(Fonts.trim(query, Font.REGULAR, 0.66f, sw - 30), Font.REGULAR, 0.66f) + 1);
            R2D.rect(ctx, cx, sy + 5, 0.7f, sh - 10, TEXT);
        }
        hits.add(new Hit(sx, sy, sw, sh, -1, () -> searchFocused = true));
        R2D.rect(ctx, px + 14, py + 46, pw - 28, 0.5f, 0x18FFFFFF);

        // ---- lista
        float footH = 40;
        listX = px + 10; listY = py + 52; listW = pw - 20; listH = ph - 52 - footH;
        visible.clear();
        String q = query.toLowerCase(Locale.ROOT);
        for (int i = 0; i < rowCount(); i++) if (q.isEmpty() || rowSearch(i).toLowerCase(Locale.ROOT).contains(q)) visible.add(i);
        float rh = rowHeight(), gap = 4;
        float contentH = visible.size() * (rh + gap);
        maxScroll = Math.max(0, contentH - listH);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scroll += (scrollTarget - scroll) * 0.3f;
        R2D.scissor(ctx, listX, listY, listW, listH);
        float ry = listY - scroll;
        for (int vi = 0; vi < visible.size(); vi++) {
            int i = visible.get(vi);
            if (ry + rh >= listY - 2 && ry <= listY + listH + 2) {
                boolean over = inside(listX, ry, listW, rh) && my >= listY && my < listY + listH;
                float hv = anim("row" + i).animate(over ? 1 : 0);
                float e = Anim.easeOutCubic((System.currentTimeMillis() - openTime - vi * 35) / 320f);
                float prev = R2D.alpha;
                R2D.alpha = prev * e;
                boolean sel = i == selected;
                float sa = anim("sel" + i).animate(sel ? 1 : 0);
                R2D.roundRect(ctx, listX, ry, listW - 6, rh, 11, R2D.blend(R2D.withAlpha(0xFFFFFFFF, 0.05f * hv), 0x1FFFFFFF, sa));
                if (sa > 0.01f) {
                    R2D.roundOutline(ctx, listX, ry, listW - 6, rh, 11, 0.7f, R2D.withAlpha(0xFFFFFFFF, 0.55f * sa));
                    R2D.roundRect(ctx, listX + 2.5f, ry + rh / 2f - 8, 2.5f, 16, 1.25f, R2D.withAlpha(0xFFFFFFFF, sa));
                }
                drawRow(ctx, i, listX + (1 - e) * 10, ry, listW - 6, rh, hv, sel);
                R2D.alpha = prev;
                final int fi = i;
                hits.add(new Hit(listX, Math.max(listY, ry), listW, Math.min(rh, listY + listH - Math.max(listY, ry)), fi, null));
            }
            ry += rh + gap;
        }
        R2D.endScissor(ctx);
        if (visible.isEmpty()) {
            float cy = listY + listH / 2f;
            R2D.icon(ctx, emptyIcon(), listX + listW / 2f - 11, cy - 24, 22, FAINT);
            Fonts.drawCentered(ctx, query.isEmpty() ? emptyText() : t("Nic nie znaleziono", "Nothing found"), listX + listW / 2f, cy + 6, DIM, Font.SEMIBOLD, 0.8f);
        }
        if (maxScroll > 0) {
            float th = Math.max(20, listH * listH / contentH);
            float ty = listY + (listH - th) * (scroll / maxScroll);
            R2D.roundRect(ctx, px + pw - 8, ty, 2.5f, th, 1.25f, 0x99FFFFFF);
        }

        // ---- stopka
        R2D.rect(ctx, px + 14, py + ph - footH, pw - 28, 0.5f, 0x18FFFFFF);
        List<Btn> btns = buttons();
        float fx = px + 14, fw = pw - 28, fy = py + ph - footH + 9, fh = 22, g = 6;
        float weights = 0;
        for (Btn b : btns) weights += b.primary() ? 1.6f : 1f;
        float unit = (fw - g * (btns.size() - 1)) / weights;
        boolean iconsOnly = unit < 64;
        for (Btn b : btns) {
            float bw = unit * (b.primary() ? 1.6f : 1f);
            float hv = anim("btn" + b.key()).animate(b.enabled() && inside(fx, fy, bw, fh) ? 1 : 0);
            button(ctx, fx, fy, bw, fh, b.icon(), iconsOnly && !b.primary() ? null : b.label(), b.primary(), hv, b.enabled());
            if (b.enabled()) hits.add(new Hit(fx, fy, bw, fh, -1, b.action()));
            fx += bw + g;
        }

        // ---- podpowiedź pod panelem
        float by = py + ph + 9;
        float kx = px + 4;
        kx += keyCap(ctx, "↑", kx, by) + 4;
        kx += keyCap(ctx, "↓", kx, by) + 4;
        kx += keyCap(ctx, "Enter", kx, by) + 6;
        Fonts.drawV(ctx, hint(), kx, by + 6, FAINT, Font.REGULAR, 0.56f);
        String esc = t("wróć", "back");
        float ew = Fonts.width(esc, Font.REGULAR, 0.56f);
        Fonts.drawV(ctx, esc, px + pw - 4 - ew, by + 6, FAINT, Font.REGULAR, 0.56f);
        keyCap(ctx, "Esc", px + pw - 4 - ew - 6 - Math.max(12, Fonts.width("Esc", Font.BOLD, 0.52f) + 8), by);
        R2D.alpha = 1;
    }

    protected boolean inside(float x, float y, float w, float h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    // ------------------------------------------------------------------ wejście

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mx = (int) mouseX; my = (int) mouseY;
        boolean wasSearch = searchFocused;
        searchFocused = false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.in(mouseX, mouseY)) continue;
            if (h.row() >= 0) {
                long now = System.currentTimeMillis();
                boolean dbl = button == 0 && h.row() == lastClickRow && now - lastClickTime < 300;
                lastClickTime = now; lastClickRow = h.row();
                selected = h.row();
                if (button == 1) rightClick(h.row());
                else if (dbl) activate(h.row());
            } else if (button == 0) {
                h.action().run();
            }
            return true;
        }
        if (wasSearch && query.isEmpty()) searchFocused = false;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget - (float) v * 28));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (!query.isEmpty() || searchFocused) { query = ""; searchFocused = false; } else back();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && searchFocused) {
            if (!query.isEmpty()) query = query.substring(0, query.length() - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_UP) {
            if (visible.isEmpty()) return true;
            int pos = visible.indexOf(selected);
            pos = pos < 0 ? (keyCode == GLFW.GLFW_KEY_DOWN ? 0 : visible.size() - 1)
                    : Math.max(0, Math.min(visible.size() - 1, pos + (keyCode == GLFW.GLFW_KEY_DOWN ? 1 : -1)));
            selected = visible.get(pos);
            float rh = rowHeight() + 4, top = pos * rh;
            if (top < scrollTarget) scrollTarget = top;
            else if (top + rh > scrollTarget + listH) scrollTarget = top + rh - listH;
            return true;
        }
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && hasSelection()) {
            activate(selected);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr < ' ' || chr == 127) return false;
        if (!searchFocused && Character.isLetterOrDigit(chr)) searchFocused = true;
        if (searchFocused && query.length() < 32) { query += chr; scrollTarget = 0; }
        return true;
    }

    protected void back() { if (mc != null) mc.setScreen(parent); }

    @Override public void close() { back(); }
}
