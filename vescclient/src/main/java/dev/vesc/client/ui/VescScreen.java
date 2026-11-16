package dev.vesc.client.ui;

import dev.vesc.client.render.Fonts;
import dev.vesc.client.render.Fonts.Font;
import dev.vesc.client.render.R2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Wspólna baza ekranów VescClient (wybór serwera / świata): animowane tło, wyśrodkowana karta,
 * nagłówek z przyciskiem powrotu, przewijana lista wierszy i rząd przycisków na dole.
 */
public abstract class VescScreen extends Screen {
    private static final Identifier WAVES = R2D.id("textures/ui/waves.png");
    private static long firstOpen = 0;
    protected final Screen parent;
    protected final MinecraftClient mc = MinecraftClient.getInstance();
    private final Map<String, Anim> anims = new HashMap<>();
    private final long openTime = System.currentTimeMillis();

    protected float px, py, pw, ph;              // karta
    protected float listX, listY, listW, listH;  // obszar listy
    protected float scroll, scrollTarget, maxScroll;
    protected int selected = -1;
    protected int mx, my;

    private record Click(float x, float y, float w, float h, IntConsumer action) {}
    private final List<Click> clicks = new ArrayList<>();
    private long lastClickTime;
    private int lastClickRow = -2;

    protected VescScreen(Screen parent, String title) {
        super(Text.literal(title));
        this.parent = parent;
        if (firstOpen == 0) firstOpen = System.currentTimeMillis();
    }

    protected static boolean mono() { return Theme.accentPreset() == Theme.Accent.MONO; }
    protected static int accent() { return mono() ? 0xFFFFFFFF : Theme.accentWave(0); }

    protected Anim anim(String k) { return anims.computeIfAbsent(k, s -> new Anim(0, 16)); }
    protected boolean hov(float x, float y, float w, float h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    protected void click(float x, float y, float w, float h, IntConsumer a) { clicks.add(new Click(x, y, w, h, a)); }

    @Override public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    // ------------------------------------------------------------------ do nadpisania

    protected abstract String subtitle();
    protected abstract float rowHeight();
    protected abstract int rowCount();
    protected abstract void drawRow(DrawContext ctx, int i, float x, float y, float w, float h, boolean hovered, boolean sel);
    protected abstract void onRow(int i, int button, boolean dbl);
    protected abstract void drawFooter(DrawContext ctx, float x, float y, float w);

    protected void onOpen() {}
    private boolean opened;

    // ------------------------------------------------------------------ render

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!opened) { opened = true; onOpen(); }
        this.mx = mouseX; this.my = mouseY;
        clicks.clear();
        drawBg(ctx);
        float intro = Anim.easeOutCubic((System.currentTimeMillis() - openTime) / 450f);
        R2D.alpha = intro;
        int accent = accent();

        pw = Math.min(470, width - 36);
        ph = Math.min(height - 36, 360);
        px = (width - pw) / 2f;
        py = (height - ph) / 2f + (1 - intro) * 10;

        R2D.shadow(ctx, px, py + 3, pw, ph, 22, R2D.withAlpha(0xFF000000, 0.55f));
        R2D.glow(ctx, px - 30, py - 30, pw + 60, ph + 60, R2D.withAlpha(accent, 0.09f));
        R2D.roundRect(ctx, px, py, pw, ph, 18, 0xF2121216);
        R2D.roundRectV(ctx, px, py, pw, 58, 18, R2D.withAlpha(accent, 0.08f), R2D.withAlpha(accent, 0f));
        R2D.roundOutline(ctx, px, py, pw, ph, 18, 0.7f, R2D.withAlpha(0xFFFFFFFF, 0.1f));

        // nagłówek: powrót + tytuł
        float bkS = 24, bkx = px + 14, bky = py + 14;
        boolean hb = hov(bkx, bky, bkS, bkS);
        float hba = anim("back").animate(hb ? 1 : 0);
        R2D.roundRect(ctx, bkx, bky, bkS, bkS, 8, R2D.blend(0x18FFFFFF, 0x30FFFFFF, hba));
        R2D.icon(ctx, "arrow_left", bkx + bkS / 2f - 5, bky + bkS / 2f - 5, 10, R2D.blend(0xFFD6D6DC, 0xFFFFFFFF, hba));
        click(bkx, bky, bkS, bkS, b -> { if (b == 0) back(); });
        Fonts.draw(ctx, getTitle().getString(), bkx + bkS + 12, py + 15, 0xFFF4F3F8, Font.BOLD, 1.12f);
        Fonts.draw(ctx, subtitle(), bkx + bkS + 12, py + 31, 0xFF8A8A96, Font.REGULAR, 0.64f);

        // obszar listy
        float footH = 40;
        listX = px + 12; listY = py + 56; listW = pw - 24; listH = ph - 56 - footH;
        float rh = rowHeight(), gap = 6;
        int n = rowCount();
        float contentH = n * (rh + gap);
        maxScroll = Math.max(0, contentH - listH);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scroll += (scrollTarget - scroll) * 0.3f;

        R2D.scissor(ctx, listX, listY, listW, listH);
        float ry = listY - scroll;
        for (int i = 0; i < n; i++) {
            if (ry + rh >= listY - 2 && ry <= listY + listH + 2) {
                boolean hovered = hov(listX, Math.max(listY, ry), listW, Math.min(rh, listY + listH - ry)) && my >= listY && my < listY + listH;
                drawRow(ctx, i, listX, ry, listW, rh, hovered, i == selected);
                final int fi = i;
                click(listX, ry, listW, rh, b -> rowClick(fi, b));
            }
            ry += rh + gap;
        }
        R2D.endScissor(ctx);
        if (n == 0) Fonts.drawCentered(ctx, subtitleEmpty(), listX + listW / 2f, listY + listH / 2f, 0xFF6A6A74, Font.SEMIBOLD, 0.8f);

        // pasek przewijania
        if (maxScroll > 0) {
            float th = Math.max(20, listH * listH / contentH);
            float ty = listY + (listH - th) * (scroll / maxScroll);
            R2D.roundRect(ctx, px + pw - 7, ty, 2.5f, th, 1.25f, R2D.withAlpha(accent, 0.6f));
        }

        // stopka
        drawFooter(ctx, px + 12, py + ph - footH + 8, pw - 24);
        R2D.alpha = 1;
    }

    private void rowClick(int i, int button) {
        long now = System.currentTimeMillis();
        boolean dbl = button == 0 && i == lastClickRow && now - lastClickTime < 280;
        lastClickTime = now; lastClickRow = i;
        if (button == 0) selected = i;
        onRow(i, button, dbl);
    }

    protected String subtitleEmpty() { return ""; }

    /** przycisk w stopce; zwraca prawą krawędź do łączenia przycisków */
    protected void footBtn(DrawContext ctx, String key, float x, float y, float w, float h, String icon, String label, boolean primary, boolean danger, Runnable action) {
        boolean hv = hov(x, y, w, h);
        float ha = anim("fb" + key).animate(hv ? 1 : 0);
        int base = danger ? 0xFFFF4D5E : accent();
        int bg = primary ? R2D.blend(base, R2D.blend(base, 0xFFFFFFFF, 0.18f), ha)
                         : R2D.blend(0x14FFFFFF, 0x28FFFFFF, ha);
        R2D.roundRect(ctx, x, y, w, h, h / 2f, bg);
        if (!primary) R2D.roundOutline(ctx, x, y, w, h, h / 2f, 0.5f, R2D.withAlpha(0xFFFFFFFF, 0.1f + 0.1f * ha));
        else R2D.glow(ctx, x - 5, y - 3, w + 10, h + 6, R2D.withAlpha(base, 0.18f * (0.6f + ha)));
        int fg = primary ? (danger ? 0xFFFFFFFF : Theme.contrast(base)) : R2D.blend(0xFFD6D6DC, 0xFFFFFFFF, ha);
        float tw = Fonts.width(label, Font.BOLD, 0.72f);
        float cxs = x + (w - tw - (icon != null ? 13 : 0)) / 2f;
        if (icon != null) { R2D.icon(ctx, icon, cxs, y + h / 2f - 4.5f, 9, fg); cxs += 13; }
        Fonts.drawV(ctx, label, cxs, y + h / 2f + 0.5f, fg, Font.BOLD, 0.72f);
        click(x, y, w, h, b -> { if (b == 0) action.run(); });
    }

    // ------------------------------------------------------------------ tło

    private void drawBg(DrawContext ctx) {
        R2D.alpha = 1;
        R2D.rect(ctx, 0, 0, width, height, 0xFF060608);
        float t = (System.currentTimeMillis() - firstOpen) / 1000f;
        float aspect = width / (float) height;
        R2D.textureUV(ctx, WAVES, 0, 0, width, height, t * 0.004f, t * 0.002f, t * 0.004f + 0.9f * aspect, t * 0.002f + 0.9f, 0x16FFFFFF);
        R2D.glow(ctx, width / 2f - width * 0.4f, height * 0.1f, width * 0.8f, height * 0.7f, R2D.withAlpha(accent(), 0.05f));
    }

    // ------------------------------------------------------------------ wejście

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.mx = (int) mouseX; this.my = (int) mouseY;
        for (int i = clicks.size() - 1; i >= 0; i--) {
            Click c = clicks.get(i);
            if (mouseX >= c.x && mouseX < c.x + c.w && mouseY >= c.y && mouseY < c.y + c.h) { c.action.accept(button); return true; }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        if (mouseY >= listY && mouseY < listY + listH) { scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget - (float) v * 28)); return true; }
        return super.mouseScrolled(mouseX, mouseY, h, v);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { back(); return true; } // ESC
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    protected void back() { if (mc != null) mc.setScreen(parent); }

    /** kafelek z literą-monogramem (zamiast ikony serwera/świata) */
    protected void monogram(DrawContext ctx, float x, float y, float s, String name, int tint) {
        R2D.roundRectV(ctx, x, y, s, s, s * 0.28f, R2D.blend(tint, 0xFFFFFFFF, 0.12f), R2D.blend(tint, 0xFF000000, 0.15f));
        R2D.roundOutline(ctx, x, y, s, s, s * 0.28f, 0.6f, R2D.withAlpha(0xFFFFFFFF, 0.18f));
        String ch = name == null || name.isBlank() ? "?" : name.trim().substring(0, 1).toUpperCase();
        Fonts.drawCentered(ctx, ch, x + s / 2f, y + s / 2f - Fonts.height(1.1f) / 2f + 0.5f, Theme.contrast(tint), Font.BOLD, 1.1f);
    }
}
