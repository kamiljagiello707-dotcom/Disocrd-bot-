package tech.fuzir.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import tech.fuzir.client.config.Config;
import tech.fuzir.client.hud.Hud;
import tech.fuzir.client.module.HudModule;
import tech.fuzir.client.module.ModuleManager;
import tech.fuzir.client.util.Anim;
import tech.fuzir.client.util.Canvas;
import tech.fuzir.client.util.Canvas.F;
import tech.fuzir.client.util.Icons;

import java.util.HashMap;
import java.util.Map;

/** Przeciąganie elementów HUD myszką, z przyciąganiem do krawędzi i środka ekranu. */
public class HudEditorScreen extends Screen {
    private static final float SNAP = 5;
    private final Anim open = new Anim(0);
    private final Map<HudModule, Anim> hover = new HashMap<>();
    private final Anim guideX = new Anim(0), guideY = new Anim(0);
    private HudModule dragging;
    private float offX, offY, u, mx, my;
    private boolean snapX, snapY;
    private float resetX, resetY, resetW, resetH;

    public HudEditorScreen() {
        super(Text.literal("Edytor HUD"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float o = open.to(1, 10);
        u = Hud.scale();
        double sf = client.getWindow().getScaleFactor();
        mx = (float) (mouseX * sf / u);
        my = (float) (mouseY * sf / u);
        Canvas cv = Canvas.begin(ctx, u);
        cv.alpha = o;
        float sw = cv.width(), sh = cv.height();

        // delikatna siatka
        cv.rect(0, 0, sw, sh, 0x38000000);
        for (float gx = 20; gx < sw; gx += 20) cv.rect(gx, 0, 1f / u, sh, 0x08FFFFFF);
        for (float gy = 20; gy < sh; gy += 20) cv.rect(0, gy, sw, 1f / u, 0x08FFFFFF);

        // linie pomocnicze przy przyciąganiu do środka
        float gxA = guideX.to(dragging != null && snapX ? 1 : 0, 18);
        float gyA = guideY.to(dragging != null && snapY ? 1 : 0, 18);
        if (gxA > 0.01f) cv.rect(sw / 2 - 0.5f / u, 0, 1f / u, sh, Canvas.mulAlpha(0xB0FFFFFF, gxA));
        if (gyA > 0.01f) cv.rect(0, sh / 2 - 0.5f / u, sw, 1f / u, Canvas.mulAlpha(0xB0FFFFFF, gyA));

        for (HudModule m : ModuleManager.hud()) {
            if (!m.isEnabled() || m.lastW <= 0) continue;
            boolean h = dragging == m || (dragging == null && in(m));
            float a = hover.computeIfAbsent(m, k -> new Anim(0)).to(h ? 1 : 0, 16);
            float pad = 3 + a;
            cv.round(m.lastX - pad, m.lastY - pad, m.lastW + pad * 2, m.lastH + pad * 2, 10, Canvas.mulAlpha(0x14FFFFFF, a));
            cv.outline(m.lastX - pad, m.lastY - pad, m.lastW + pad * 2, m.lastH + pad * 2, 10, 1f, Canvas.mix(0x40FFFFFF, 0xE0FFFFFF, a));
            if (a > 0.02f) {
                float lw = cv.width(m.name, F.SMALL) + 22;
                float ly = m.lastY - pad - 17 > 2 ? m.lastY - pad - 17 : m.lastY + m.lastH + pad + 3;
                float lx = Math.max(2, Math.min(sw - lw - 2, m.lastX));
                float pa = cv.alpha;
                cv.alpha = pa * a;
                cv.round(lx, ly, lw, 14, 5, 0xFFFFFFFF);
                cv.icon(Icons.GRIP, lx + 7, ly + 7, 0xFF000000, F.ICON_SM);
                cv.textMid(m.name, lx + 14, ly + 7, 0xFF000000, F.SMALL);
                cv.alpha = pa;
            }
        }

        // pasek z podpowiedzią
        String tip = "Przeciągnij elementy myszką  ·  Esc wraca do menu";
        float tw = cv.width(tip, F.REGULAR) + 40 + 70;
        float tx = sw / 2 - tw / 2, ty = 14 - (1 - Anim.easeOutCubic(o)) * 20;
        Hud.panel(cv, tx, ty, tw, 28, 14);
        cv.icon(Icons.LAYOUT, tx + 16, ty + 14, 0xFFFFFFFF, F.ICON);
        cv.textMid(tip, tx + 30, ty + 14, 0xFFE6E6EA, F.REGULAR);
        resetW = 62;
        resetH = 20;
        resetX = tx + tw - resetW - 4;
        resetY = ty + 4;
        boolean rh = mx >= resetX && mx <= resetX + resetW && my >= resetY && my <= resetY + resetH;
        cv.round(resetX, resetY, resetW, resetH, 10, rh ? 0xFFFFFFFF : 0xFF222227);
        cv.textCenter("Resetuj", resetX + resetW / 2, resetY + resetH / 2, rh ? 0xFF000000 : 0xFFE6E6EA, F.SMALL);
        cv.end();
    }

    private boolean in(HudModule m) {
        return mx >= m.lastX - 3 && mx <= m.lastX + m.lastW + 3 && my >= m.lastY - 3 && my <= m.lastY + m.lastH + 3;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double sf = client.getWindow().getScaleFactor();
        mx = (float) (mouseX * sf / u);
        my = (float) (mouseY * sf / u);
        if (mx >= resetX && mx <= resetX + resetW && my >= resetY && my <= resetY + resetH) {
            for (HudModule m : ModuleManager.hud()) {
                m.fx = m.defaultFx;
                m.fy = m.defaultFy;
            }
            return true;
        }
        var list = ModuleManager.hud();
        for (int i = list.size() - 1; i >= 0; i--) {
            HudModule m = list.get(i);
            if (m.isEnabled() && m.lastW > 0 && in(m)) {
                dragging = m;
                offX = mx - m.lastX;
                offY = my - m.lastY;
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dX, double dY) {
        if (dragging == null) return true;
        double sf = client.getWindow().getScaleFactor();
        mx = (float) (mouseX * sf / u);
        my = (float) (mouseY * sf / u);
        float sw = client.getWindow().getFramebufferWidth() / u, sh = client.getWindow().getFramebufferHeight() / u;
        float w = dragging.lastW, h = dragging.lastH, m = Hud.MARGIN;
        float x = mx - offX, y = my - offY;
        snapX = Math.abs(x + w / 2 - sw / 2) < SNAP;
        snapY = Math.abs(y + h / 2 - sh / 2) < SNAP;
        if (snapX) x = sw / 2 - w / 2;
        if (snapY) y = sh / 2 - h / 2;
        float freeX = Math.max(1, sw - w - m * 2), freeY = Math.max(1, sh - h - m * 2);
        dragging.fx = Math.max(0, Math.min(1, (x - m) / freeX));
        dragging.fy = Math.max(0, Math.min(1, (y - m) / freeY));
        // przyciąganie do krawędzi
        if (dragging.fx * freeX < SNAP) dragging.fx = 0;
        if ((1 - dragging.fx) * freeX < SNAP) dragging.fx = 1;
        if (dragging.fy * freeY < SNAP) dragging.fy = 0;
        if ((1 - dragging.fy) * freeY < SNAP) dragging.fy = 1;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = null;
        snapX = snapY = false;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            client.setScreen(new ClickGuiScreen());
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void removed() {
        Config.save();
    }

    /** Dla testów: ustawia pozycję elementu. */
    public static void place(HudModule m, float fx, float fy) {
        m.fx = fx;
        m.fy = fy;
    }
}
