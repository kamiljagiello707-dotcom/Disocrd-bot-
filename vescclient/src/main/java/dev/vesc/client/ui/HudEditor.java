package dev.vesc.client.ui;

import dev.vesc.client.VescClient;
import dev.vesc.client.hud.Hud;
import dev.vesc.client.hud.HudElement;
import dev.vesc.client.lang.Lang;
import dev.vesc.client.render.Fonts;
import dev.vesc.client.render.Fonts.Font;
import dev.vesc.client.render.R2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Edytor HUD: wybór elementów z listy, przeciąganie i przyciąganie do krawędzi / środka ekranu. */
public class HudEditor extends Screen {
    private static final float SNAP = 5;
    private final VescClient rc = VescClient.get();
    private final Hud hud = rc.hud;
    private final long openTime = System.currentTimeMillis();
    private HudElement drag;
    private float grabX, grabY;
    private boolean snapX, snapY;
    private float snapLineX, snapLineY;
    private float doneX, doneY, resetX, toolbarW = 260, toolbarH = 26;

    // panel wyboru elementów (lewy górny róg)
    private boolean pickerOpen = true;
    private final Anim pickerAnim = new Anim(1, 12);
    private static final float PICK_X = 10, PICK_Y = 40, PICK_W = 150, HEAD_H = 22, ROW_H = 16;
    private float pickScroll, pickMax;

    public HudEditor() { super(Text.literal("HUD Editor")); }

    @Override public boolean shouldPause() { return false; }

    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float p = Anim.easeOutCubic((System.currentTimeMillis() - openTime) / (300f / Anim.globalSpeed));
        R2D.alpha = 1;
        R2D.rect(ctx, 0, 0, width, height, R2D.withAlpha(0xFF000000, 0.35f * p));
        // siatka
        R2D.alpha = p;
        int grid = 20;
        for (int gx = grid; gx < width; gx += grid) R2D.rect(ctx, gx, 0, 0.5f, height, R2D.withAlpha(Theme.text(), 0.05f));
        for (int gy = grid; gy < height; gy += grid) R2D.rect(ctx, 0, gy, width, 0.5f, R2D.withAlpha(Theme.text(), 0.05f));
        R2D.rect(ctx, width / 2f, 0, 0.5f, height, R2D.withAlpha(Theme.text(), 0.12f));
        R2D.rect(ctx, 0, height / 2f, width, 0.5f, R2D.withAlpha(Theme.text(), 0.12f));

        if (drag != null) {
            float nx = mouseX - grabX, ny = mouseY - grabY;
            snapX = snapY = false;
            float[][] xs = {{SNAP + 1, SNAP + 1}, {width / 2f - drag.w / 2f, width / 2f}, {width - drag.w - SNAP - 1, width - SNAP - 1}};
            for (float[] c : xs) if (Math.abs(nx - c[0]) < SNAP) { nx = c[0]; snapX = true; snapLineX = c[1]; }
            float[][] ys = {{SNAP + 1, SNAP + 1}, {height / 2f - drag.h / 2f, height / 2f}, {height - drag.h - SNAP - 1, height - SNAP - 1}};
            for (float[] c : ys) if (Math.abs(ny - c[0]) < SNAP) { ny = c[0]; snapY = true; snapLineY = c[1]; }
            drag.setPosition(nx, ny, width, height);
            if (snapX) R2D.rect(ctx, snapLineX - 0.5f, 0, 1, height, Theme.accent());
            if (snapY) R2D.rect(ctx, 0, snapLineY - 0.5f, width, 1, Theme.accent());
        }

        // elementy
        HudElement hovered = null;
        java.util.List<float[]> placed = hud.reserved(width, height);
        for (HudElement e : hud.elements) {
            if (!e.enabled.get()) continue;
            float[] pos = hud.place(e, width, height, placed);
            R2D.alpha = 1;
            e.render(ctx, pos[0], pos[1], true);
            e.lastX = pos[0]; e.lastY = pos[1];
            boolean hov = drag == e || (drag == null && !inPicker(mouseX, mouseY) && inside(mouseX, mouseY, pos[0], pos[1], e.w, e.h));
            if (hov) hovered = e;
            R2D.alpha = p;
            R2D.roundOutline(ctx, pos[0] - 2, pos[1] - 2, e.w + 4, e.h + 4, Hud.R + 2, hov ? 1f : 0.6f,
                    hov ? Theme.accent() : R2D.withAlpha(Theme.text(), 0.35f));
        }
        if (hovered != null) {
            String n = hovered.getName();
            float tw = Fonts.width(n, Font.BOLD, 0.62f) + 14;
            float ty = hovered.lastY - 14 < 2 ? hovered.lastY + hovered.h + 4 : hovered.lastY - 14;
            R2D.roundRect(ctx, hovered.lastX - 2, ty, tw, 10, 4, Theme.accent());
            R2D.icon(ctx, hovered.icon, hovered.lastX + 1, ty + 2, 6, Theme.accentText());
            Fonts.drawV(ctx, n, hovered.lastX + 9, ty + 5.3f, Theme.accentText(), Font.BOLD, 0.62f);
        }

        R2D.alpha = p;
        drawPicker(ctx, mouseX, mouseY);
        drawToolbar(ctx, mouseX, mouseY, p);
        R2D.alpha = 1;
    }

    // ---------------------------------------------------------------- panel elementów

    private void drawPicker(DrawContext ctx, int mouseX, int mouseY) {
        long on = hud.elements.stream().filter(e -> e.enabled.get()).count();
        // nagłówek-przycisk (zawsze widoczny)
        boolean hh = inside(mouseX, mouseY, PICK_X, PICK_Y, PICK_W, HEAD_H);
        R2D.shadow(ctx, PICK_X, PICK_Y, PICK_W, HEAD_H, 8, Theme.shadow());
        R2D.roundRect(ctx, PICK_X, PICK_Y, PICK_W, HEAD_H, 8, hh ? Theme.cardHover() : Theme.bg());
        R2D.roundOutline(ctx, PICK_X, PICK_Y, PICK_W, HEAD_H, 8, 0.5f, Theme.stroke());
        R2D.icon(ctx, "list", PICK_X + 9, PICK_Y + HEAD_H / 2f - 4, 8, Theme.accent());
        Fonts.drawV(ctx, Lang.get("editor.elements"), PICK_X + 22, PICK_Y + HEAD_H / 2f + 0.5f, Theme.text(), Font.BOLD, 0.68f);
        String cnt = on + "/" + hud.elements.size();
        Fonts.drawV(ctx, cnt, PICK_X + PICK_W - 20 - Fonts.width(cnt, Font.BOLD, 0.56f), PICK_Y + HEAD_H / 2f + 0.5f, Theme.textDim(), Font.BOLD, 0.56f);
        // strzałka rozwijania
        R2D.icon(ctx, pickerOpen ? "arrow_left" : "arrow_left", PICK_X + PICK_W - 15, PICK_Y + HEAD_H / 2f - 4, 8, Theme.textFaint());

        float a = pickerAnim.animate(pickerOpen ? 1 : 0);
        if (a < 0.01f) return;
        float listY = PICK_Y + HEAD_H + 3;
        float maxH = height - 46 - listY;
        float fullH = hud.elements.size() * ROW_H + 8;
        float listH = Math.min(maxH, fullH) * a;
        pickMax = Math.max(0, fullH - Math.min(maxH, fullH));
        pickScroll = Math.max(0, Math.min(pickMax, pickScroll));

        R2D.shadow(ctx, PICK_X, listY, PICK_W, listH, 9, Theme.shadow());
        R2D.roundRect(ctx, PICK_X, listY, PICK_W, listH, 8, Theme.bg());
        R2D.roundOutline(ctx, PICK_X, listY, PICK_W, listH, 8, 0.5f, Theme.stroke());

        R2D.scissor(ctx, PICK_X, listY + 2, PICK_W, listH - 4);
        float ry = listY + 5 - pickScroll;
        for (HudElement e : hud.elements) {
            boolean en = e.enabled.get();
            boolean hv = inside(mouseX, mouseY, PICK_X + 4, ry, PICK_W - 8, ROW_H - 2) && inside(mouseX, mouseY, PICK_X, listY, PICK_W, listH);
            if (hv) R2D.roundRect(ctx, PICK_X + 4, ry, PICK_W - 8, ROW_H - 2, 6, R2D.withAlpha(Theme.text(), 0.06f));
            int fg = en ? Theme.text() : Theme.textFaint();
            R2D.icon(ctx, e.icon, PICK_X + 10, ry + (ROW_H - 2) / 2f - 4, 8, en ? Theme.accent() : Theme.textFaint());
            Fonts.drawV(ctx, Fonts.trim(e.getName(), Font.SEMIBOLD, 0.62f, PICK_W - 58), PICK_X + 22, ry + (ROW_H - 2) / 2f + 0.5f, fg, Font.SEMIBOLD, 0.62f);
            // mały przełącznik
            float sw = 18, sh = 10, sx = PICK_X + PICK_W - 10 - sw, sy = ry + (ROW_H - 2) / 2f - sh / 2f;
            R2D.roundRect(ctx, sx, sy, sw, sh, sh / 2f, R2D.blend(Theme.toggleOff(), Theme.accent(), en ? 1 : 0));
            R2D.circle(ctx, sx + sh / 2f + (sw - sh) * (en ? 1 : 0), sy + sh / 2f, sh / 2f - 1.5f, en ? Theme.accentText() : Theme.textDim());
            ry += ROW_H;
        }
        R2D.endScissor(ctx);

        // pasek przewijania
        if (pickMax > 0) {
            float trackH = listH - 8;
            float thumbH = Math.max(18, trackH * (listH / fullH));
            float ty = listY + 4 + (trackH - thumbH) * (pickScroll / pickMax);
            R2D.roundRect(ctx, PICK_X + PICK_W - 4, ty, 2, thumbH, 1, Theme.accent());
        }
    }

    /** czy kursor jest nad panelem elementów (żeby nie łapać elementów pod nim) */
    private boolean inPicker(double mx, double my) {
        if (inside(mx, my, PICK_X, PICK_Y, PICK_W, HEAD_H)) return true;
        if (!pickerOpen) return false;
        float listY = PICK_Y + HEAD_H + 3;
        float listH = Math.min(height - 46 - listY, hud.elements.size() * ROW_H + 8);
        return inside(mx, my, PICK_X, listY, PICK_W, listH);
    }

    /** klik w panelu elementów; zwraca true gdy obsłużono */
    private boolean clickPicker(double mx, double my) {
        if (inside(mx, my, PICK_X, PICK_Y, PICK_W, HEAD_H)) { pickerOpen = !pickerOpen; return true; }
        if (!pickerOpen) return false;
        float listY = PICK_Y + HEAD_H + 3;
        float listH = Math.min(height - 46 - listY, hud.elements.size() * ROW_H + 8);
        if (!inside(mx, my, PICK_X, listY, PICK_W, listH)) return false;
        float ry = listY + 5 - pickScroll;
        for (HudElement e : hud.elements) {
            if (inside(mx, my, PICK_X + 4, ry, PICK_W - 8, ROW_H - 2)) {
                e.enabled.set(!e.enabled.get());
                rc.config.saveClient();
                return true;
            }
            ry += ROW_H;
        }
        return true; // klik w panelu, ale poza wierszem – i tak pochłoń
    }

    // ---------------------------------------------------------------- pasek narzędzi

    private void drawToolbar(DrawContext ctx, int mouseX, int mouseY, float p) {
        float tx = (width - toolbarW) / 2f, ty = height - toolbarH - 34 + (1 - p) * 20;
        R2D.shadow(ctx, tx, ty, toolbarW, toolbarH, 10, Theme.shadow());
        R2D.roundRect(ctx, tx, ty, toolbarW, toolbarH, 9, Theme.bg());
        R2D.roundOutline(ctx, tx, ty, toolbarW, toolbarH, 9, 0.5f, Theme.stroke());
        R2D.icon(ctx, "move", tx + 9, ty + toolbarH / 2f - 4, 8, Theme.accent());
        Fonts.drawV(ctx, Lang.get("editor.hint"), tx + 22, ty + toolbarH / 2f + 0.5f, Theme.textDim(), Font.SEMIBOLD, 0.62f);
        doneX = tx + toolbarW - 52; doneY = ty + 5;
        resetX = doneX - 24;
        boolean hr = inside(mouseX, mouseY, resetX, doneY, 20, 16);
        R2D.roundRect(ctx, resetX, doneY, 20, 16, 6, hr ? Theme.cardHover() : Theme.card());
        R2D.icon(ctx, "reset", resetX + 6, doneY + 4, 8, Theme.text());
        boolean hd = inside(mouseX, mouseY, doneX, doneY, 46, 16);
        R2D.roundRect(ctx, doneX, doneY, 46, 16, 7, hd ? R2D.blend(Theme.accent(), Theme.textDim(), 0.25f) : Theme.accent());
        Fonts.drawCentered(ctx, Lang.get("editor.done"), doneX + 23, doneY + 8 - Fonts.height(0.66f) / 2f + 0.5f, Theme.accentText(), Font.BOLD, 0.66f);
    }

    private static boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        if (pickerOpen && inPicker(mx, my)) { pickScroll = Math.max(0, Math.min(pickMax, pickScroll - (float) vAmount * 22)); return true; }
        return super.mouseScrolled(mx, my, hAmount, vAmount);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (inside(mx, my, doneX, doneY, 46, 16)) { close(); return true; }
        if (inside(mx, my, resetX, doneY, 20, 16)) {
            for (HudElement e : hud.elements) e.resetPosition();
            rc.config.saveClient();
            return true;
        }
        if (clickPicker(mx, my)) return true;
        for (int i = hud.elements.size() - 1; i >= 0; i--) {
            HudElement e = hud.elements.get(i);
            if (!e.enabled.get() || !inside(mx, my, e.lastX, e.lastY, e.w, e.h)) continue;
            if (button == 0) { drag = e; grabX = (float) mx - e.lastX; grabY = (float) my - e.lastY; }
            else if (button == 1) { e.enabled.set(false); rc.config.saveClient(); }
            else if (button == 2) { e.resetPosition(); rc.config.saveClient(); }
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (drag != null) rc.config.saveClient();
        drag = null;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        rc.config.saveClient();
        if (client != null) client.setScreen(new ClickGui());
    }
}
