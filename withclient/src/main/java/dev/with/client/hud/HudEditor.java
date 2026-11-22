package dev.with.client.hud;

import dev.with.client.module.Category;
import dev.with.client.module.Module;
import dev.with.client.module.Modules;
import dev.with.client.render.Fonts;
import dev.with.client.render.Fonts.Font;
import dev.with.client.render.R2D;
import dev.with.client.ui.Anim;
import dev.with.client.ui.ClickGui;
import dev.with.client.ui.Theme;
import dev.with.client.ui.Ui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.IntConsumer;

import static dev.with.client.ui.Ui.t;

/**
 * Edytor HUD: przeciąganie elementów (przyciąganie do krawędzi, środka, innych elementów i siatki),
 * kółko myszy = skala elementu, PPM = pozycja domyślna, panel z przełącznikami wszystkich elementów.
 * H chowa panel, Esc wraca.
 */
public class HudEditor extends Screen {
    private final Screen parent;
    private String drag;
    private float offX, offY;
    private List<Hud.Box> boxes = List.of();
    private final List<float[]> guides = new ArrayList<>(); // {1 = pionowa / 0 = pozioma, pozycja}
    private static boolean panelOpen = true;
    private final Map<String, Anim> anims = new HashMap<>();
    private record Hit(float x, float y, float w, float h, IntConsumer action) {
        boolean in(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; }
    }
    private final List<Hit> hits = new ArrayList<>();
    private float[] panelRect = {0, 0, 0, 0};
    private int mx, my;
    private final long openTime = System.currentTimeMillis();

    public HudEditor(Screen parent) { super(Text.literal("HUD Editor")); this.parent = parent; }

    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    private Anim anim(String k) { return anims.computeIfAbsent(k, s -> new Anim(0, 16)); }
    private boolean over(float x, float y, float w, float h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private float hover(String k, float x, float y, float w, float h) { return anim(k).animate(over(x, y, w, h) ? 1 : 0); }
    private void hit(float x, float y, float w, float h, IntConsumer a) { hits.add(new Hit(x, y, w, h, a)); }
    private void click() { if (client != null && Theme.sounds) client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.7f, 0.18f)); }

    private static Module ed() { return Modules.get("HUDEditor"); }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hits.clear();
        mx = mouseX; my = mouseY;
        float a = Anim.easeOutCubic((System.currentTimeMillis() - openTime) / 250f);
        R2D.alpha = 1; R2D.radiusMul = 1;
        R2D.rect(ctx, 0, 0, width, height, R2D.withAlpha(0xFF000000, 0.35f * a));
        Module e = ed();
        float g = Hud.num(e, "Rozmiar siatki", 8);
        if (Hud.bo(e, "Siatka") && g >= 2) {
            R2D.alpha = a;
            for (float x = 0; x < width; x += g) R2D.rect(ctx, x, 0, 0.5f, height, 0x0AFFFFFF);
            for (float y = 0; y < height; y += g) R2D.rect(ctx, 0, y, width, 0.5f, 0x0AFFFFFF);
            R2D.rect(ctx, width / 2f - 0.25f, 0, 0.5f, height, 0x1EFFFFFF);
            R2D.rect(ctx, 0, height / 2f - 0.25f, width, 0.5f, 0x1EFFFFFF);
            R2D.alpha = 1;
        }

        boxes = Hud.renderAll(ctx, true);
        String hoverId = null;
        if (drag == null && !inPanel()) for (int i = boxes.size() - 1; i >= 0; i--) { Hud.Box b = boxes.get(i); if (over(b.x(), b.y(), b.w(), b.h())) { hoverId = b.id(); break; } }
        for (Hud.Box b : boxes) {
            boolean dr = b.id().equals(drag), hv = b.id().equals(hoverId);
            float k = anim("b" + b.id()).animate(dr || hv ? 1 : 0);
            R2D.roundRect(ctx, b.x() - 2, b.y() - 2, b.w() + 4, b.h() + 4, 4, R2D.withAlpha(0xFFFFFFFF, 0.04f * k));
            R2D.roundOutline(ctx, b.x() - 2, b.y() - 2, b.w() + 4, b.h() + 4, 4, 0.6f, R2D.blend(0x30FFFFFF, dr ? 0xFFFFFFFF : 0xAAFFFFFF, k));
            if (k > 0.01f) {
                Hud.Layout L = Hud.layout(b.id());
                String tag = b.id() + (L.scale != 1 ? "  " + Math.round(L.scale * 100) + "%" : "");
                float fs = 0.5f * Math.max(0.7f, Hud.base()), th = 18 * fs;
                float tw = Fonts.width(tag, Font.BOLD, fs) + 16 * fs;
                float ty = b.y() - th - 3 < 2 ? b.y() + b.h() + 4 : b.y() - th - 3;
                float pa = R2D.alpha; R2D.alpha = k;
                R2D.roundRect(ctx, b.x() - 2, ty, tw, th, th / 3f, 0xF0FFFFFF);
                Fonts.drawV(ctx, tag, b.x() - 2 + 8 * fs, ty + th / 2f + 0.5f, 0xFF09090B, Font.BOLD, fs);
                R2D.alpha = pa;
            }
        }
        for (float[] gl : guides) {
            if (gl[0] == 1) R2D.rect(ctx, gl[1] - 0.25f, 0, 0.5f, height, 0xCCFFFFFF);
            else R2D.rect(ctx, 0, gl[1] - 0.25f, width, 0.5f, 0xCCFFFFFF);
        }
        drawPanel(ctx, a);
    }

    private boolean inPanel() { return over(panelRect[0], panelRect[1], panelRect[2], panelRect[3]); }

    // ------------------------------------------------------------------ panel

    private static float panelX = Float.NaN, panelY = Float.NaN; // środek górnej krawędzi panelu (ekran)
    private boolean panelDrag;
    private float pdx, pdy, ps = 1, pox, poy;

    /** prostokąt w układzie panelu → klik na ekranie */
    private void phit(float x, float y, float w, float h, IntConsumer a) { hit(pox + x * ps, poy + y * ps, w * ps, h * ps, a); }
    private boolean pover(float x, float y, float w, float h) { return over(pox + x * ps, poy + y * ps, w * ps, h * ps); }
    private float phover(String k, float x, float y, float w, float h) { return anim(k).animate(pover(x, y, w, h) ? 1 : 0); }

    private void drawPanel(DrawContext ctx, float a) {
        List<Module> hud = Modules.of(Category.HUD);
        double gs = client != null ? client.getWindow().getScaleFactor() : 2;
        ps = (float) Math.max(0.5, Math.min(1, 2.0 / gs)) * Theme.scale;
        float open = anim("panelOpen").animate(panelOpen ? 1 : 0);
        float rowH = 13, pw = 236;
        int rows = (int) Math.ceil(hud.size() / 2f);
        float fullH = 34 + rows * rowH + 30;
        float lw = Anim.lerp(122, pw, open), lh = Anim.lerp(22, fullH, open);
        if (Float.isNaN(panelX)) { panelX = width / 2f; panelY = Math.max(6, (height - fullH * ps) / 2f); }
        panelX = Math.max(lw * ps / 2f + 2, Math.min(width - lw * ps / 2f - 2, panelX));
        panelY = Math.max(2, Math.min(height - lh * ps - 2, panelY));
        pox = panelX - lw * ps / 2f; poy = panelY;
        panelRect = new float[]{pox, poy, lw * ps, lh * ps};
        ctx.getMatrices().push();
        ctx.getMatrices().translate(pox, poy, 0);
        ctx.getMatrices().scale(ps, ps, 1);
        R2D.alpha = a;
        R2D.shadow(ctx, 0, 4, lw, lh, 20, 0xAA000000);
        R2D.roundRectV(ctx, 0, 0, lw, lh, 10, 0xF4141418, 0xF60C0C0E);
        R2D.roundOutline(ctx, 0, 0, lw, lh, 10, 0.6f, 0x2EFFFFFF);
        R2D.scissor(ctx, 0, 0, lw, lh);
        // nagłówek (przeciągnij, żeby przesunąć panel)
        Ui.logo(ctx, 5, 4, 14, 0);
        Fonts.drawV(ctx, t("Edytor HUD", "HUD editor"), 24, 11.5f, Ui.TEXT, Font.BOLD, 0.64f);
        float cbx = lw - 19, cby = 4;
        float ch = phover("collapse", cbx, cby, 14, 14);
        R2D.roundRect(ctx, cbx, cby, 14, 14, 7, R2D.blend(0x10FFFFFF, 0x28FFFFFF, ch));
        ctx.getMatrices().push();
        ctx.getMatrices().translate(cbx + 7, cby + 7, 0);
        ctx.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(180 * open));
        ctx.getMatrices().translate(-(cbx + 7), -(cby + 7), 0);
        R2D.icon(ctx, "l_chevron_down", cbx + 3.5f, cby + 3.5f, 7, Ui.TEXT);
        ctx.getMatrices().pop();
        phit(0, 0, lw - 22, 22, b -> { if (b == 0) { panelDrag = true; pdx = mx - panelX; pdy = my - panelY; } });
        phit(cbx, cby, 14, 14, b -> { if (b == 0) { panelOpen = !panelOpen; click(); } });
        if (open > 0.02f) {
            float pa = R2D.alpha; R2D.alpha = a * open;
            Fonts.drawV(ctx, t("przeciągnij · kółko = skala · PPM = reset · H = panel", "drag · scroll = scale · RMB = reset · H = panel"), 24, 21, Ui.FAINT, Font.SEMIBOLD, 0.42f);
            float cw = (pw - 16) / 2f;
            for (int i = 0; i < hud.size(); i++) {
                Module m = hud.get(i);
                float rx = 9 + (i % 2) * (cw + 2), ry = 32 + (i / 2) * rowH;
                float hv = phover("r" + m.name, rx - 3, ry, cw - 2, rowH - 1);
                float on = anims.computeIfAbsent("on" + m.name, k -> new Anim(m.isEnabled() ? 1 : 0, 16)).animate(m.isEnabled() ? 1 : 0);
                if (hv > 0.01f) R2D.roundRect(ctx, rx - 3, ry, cw - 2, rowH - 1, 4, R2D.withAlpha(0xFFFFFFFF, 0.05f * hv));
                R2D.icon(ctx, m.icon, rx, ry + rowH / 2f - 4, 8, R2D.blend(Ui.FAINT, Ui.TEXT, Math.max(on, hv)));
                Fonts.drawV(ctx, m.name, rx + 12, ry + rowH / 2f, R2D.blend(Ui.DIM, Ui.TEXT, Math.max(on, hv)), Font.SEMIBOLD, 0.54f);
                ctx.getMatrices().push();
                ctx.getMatrices().translate(rx + cw - 8 - 14.4f, ry + rowH / 2f - 4, 0); ctx.getMatrices().scale(0.8f, 0.8f, 1);
                Ui.toggle(ctx, 0, 0, on, hv);
                ctx.getMatrices().pop();
                phit(rx - 3, ry, cw - 2, rowH - 1, b -> { if (b == 0) { m.toggle(); Modules.save(); click(); } });
            }
            float by = 32 + rows * rowH + 7, bw = (pw - 24) / 2f;
            Ui.button(ctx, 9, by, bw, 17, "l_rotate_ccw", t("Resetuj układ", "Reset layout"), false, phover("reset", 9, by, bw, 17), true);
            phit(9, by, bw, 17, b -> { if (b == 0) { Hud.resetAll(); click(); } });
            Ui.button(ctx, 15 + bw, by, bw, 17, "l_check", t("Gotowe", "Done"), true, phover("done", 15 + bw, by, bw, 17), true);
            phit(15 + bw, by, bw, 17, b -> { if (b == 0) { click(); close(); } });
            R2D.alpha = pa;
        }
        R2D.endScissor(ctx);
        ctx.getMatrices().pop();
        R2D.alpha = 1;
    }

    // ------------------------------------------------------------------ przesuwanie

    private Hud.Box box(String id) { for (Hud.Box b : boxes) if (b.id().equals(id)) return b; return null; }

    private Hud.Box boxAt(double x, double y) {
        for (int i = boxes.size() - 1; i >= 0; i--) { Hud.Box b = boxes.get(i); if (x >= b.x() && x < b.x() + b.w() && y >= b.y() && y < b.y() + b.h()) return b; }
        return null;
    }

    private void place(String id, float x, float y, float w, float h) {
        Hud.Layout L = Hud.layout(id);
        L.custom = true;
        float cx = x + w / 2f, cy = y + h / 2f;
        L.ax = cx < width / 3f ? 0 : cx > width * 2 / 3f ? 2 : 1;
        L.ay = cy < height / 3f ? 0 : cy > height * 2 / 3f ? 2 : 1;
        L.dx = L.ax == 0 ? x : L.ax == 1 ? cx - width / 2f : width - (x + w);
        L.dy = L.ay == 0 ? y : L.ay == 1 ? cy - height / 2f : height - (y + h);
    }

    private void moveTo(Hud.Box b, float nx, float ny) {
        guides.clear();
        Module e = ed();
        float w = b.w(), h = b.h(), th = 4;
        if (Hud.bo(e, "Przyciąganie")) {
            float[] bestX = snap(nx, w, true, th), bestY = snap(ny, h, false, th);
            if (bestX != null) { nx = bestX[0]; guides.add(new float[]{1, bestX[1]}); }
            if (bestY != null) { ny = bestY[0]; guides.add(new float[]{0, bestY[1]}); }
            float g = Hud.num(e, "Rozmiar siatki", 8);
            if (Hud.bo(e, "Siatka") && g >= 2) {
                if (bestX == null) nx = Math.round(nx / g) * g;
                if (bestY == null) ny = Math.round(ny / g) * g;
            }
        }
        nx = Math.max(0, Math.min(width - w, nx));
        ny = Math.max(0, Math.min(height - h, ny));
        place(b.id(), nx, ny, w, h);
    }

    /** najlepsze przyciągnięcie: {nowa pozycja, położenie linii pomocniczej} */
    private float[] snap(float p, float size, boolean xAxis, float th) {
        float scr = xAxis ? width : height;
        List<float[]> cand = new ArrayList<>(); // {pozycja, linia}
        cand.add(new float[]{4, 4});
        cand.add(new float[]{scr - 4 - size, scr - 4});
        cand.add(new float[]{scr / 2f - size / 2f, scr / 2f});
        for (Hud.Box o : boxes) {
            if (o.id().equals(drag)) continue;
            float s = xAxis ? o.x() : o.y(), l = xAxis ? o.w() : o.h();
            cand.add(new float[]{s, s});
            cand.add(new float[]{s + l - size, s + l});
            cand.add(new float[]{s + l + 4, s + l + 2});
            cand.add(new float[]{s - size - 4, s - 2});
            cand.add(new float[]{s + l / 2f - size / 2f, s + l / 2f});
        }
        float[] best = null; float bd = th;
        for (float[] c : cand) { float d = Math.abs(c[0] - p); if (d < bd) { bd = d; best = c; } }
        return best;
    }

    // ------------------------------------------------------------------ wejście

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        mx = (int) x; my = (int) y;
        for (int i = hits.size() - 1; i >= 0; i--) if (hits.get(i).in(x, y)) { hits.get(i).action().accept(button); return true; }
        if (inPanel()) return true;
        Hud.Box b = boxAt(x, y);
        if (b == null) return true;
        if (button == 0) { drag = b.id(); offX = (float) x - b.x(); offY = (float) y - b.y(); }
        else if (button == 1) { Hud.resetLayout(b.id()); click(); }
        return true;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        mx = (int) x; my = (int) y;
        if (panelDrag) { panelX = (float) x - pdx; panelY = (float) y - pdy; return true; }
        if (drag != null) { Hud.Box b = box(drag); if (b != null) moveTo(b, (float) x - offX, (float) y - offY); }
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        panelDrag = false;
        if (drag != null) { drag = null; guides.clear(); Hud.save(); }
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        Hud.Box b = boxAt(x, y);
        if (b == null || inPanel()) return true;
        Hud.Layout L = Hud.layout(b.id());
        if (!L.custom) place(b.id(), b.x(), b.y(), b.w(), b.h());
        float ns = Math.max(0.5f, Math.min(2.5f, Math.round((L.scale + (float) v * 0.05f) * 20) / 20f));
        // skaluj względem kotwicy (lewa/górna krawędź zostaje dla lewej/górnej kotwicy itd.)
        L.scale = ns;
        Hud.save();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (key == GLFW.GLFW_KEY_H) { panelOpen = !panelOpen; return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void close() {
        Hud.save();
        if (client != null) client.setScreen(parent != null ? new ClickGui() : null);
    }
}
