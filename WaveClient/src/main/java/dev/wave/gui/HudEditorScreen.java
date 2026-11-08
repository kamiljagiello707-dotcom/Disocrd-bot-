package dev.wave.gui;

import dev.wave.WaveClient;
import dev.wave.hud.HudElement;
import dev.wave.module.Module;
import dev.wave.module.client.HudEditor;
import dev.wave.util.Animation;
import dev.wave.util.ColorUtil;
import dev.wave.util.Easing;
import dev.wave.util.RenderUtil;
import dev.wave.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HudEditorScreen extends Screen {
    private final Screen parent;
    private HudElement dragging;
    private float offX, offY;
    private Float guideX, guideY;
    private final Animation open = new Animation(10, 0);
    private final Map<HudElement, Animation> hover = new HashMap<>();

    public HudEditorScreen(Screen parent) {
        super(Text.literal("HUD Editor"));
        this.parent = parent;
        open.setTarget(1);
    }

    private List<HudElement> elements() {
        List<HudElement> list = new ArrayList<>();
        for (Module m : WaveClient.INSTANCE.modules.getModules()) if (m instanceof HudElement h) list.add(h);
        return list;
    }

    private HudEditor cfg() { return WaveClient.INSTANCE.modules.get(HudEditor.class); }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        float a = Easing.outCubic(open.update());
        c.fill(0, 0, width, height, ColorUtil.withAlpha(0, (int) (90 * a)));
        RenderUtil.globalAlpha = a;

        if (cfg().grid.isOn()) {
            for (int gx = 0; gx < width; gx += 20) RenderUtil.rect(c, gx, 0, 0.5f, height, 0x10FFFFFF);
            for (int gy = 0; gy < height; gy += 20) RenderUtil.rect(c, 0, gy, width, 0.5f, 0x10FFFFFF);
        }
        RenderUtil.rect(c, width / 2f - 0.25f, 0, 0.5f, height, 0x22FFFFFF);
        RenderUtil.rect(c, 0, height / 2f - 0.25f, width, 0.5f, 0x22FFFFFF);

        if (dragging != null) {
            float nx = mouseX - offX, ny = mouseY - offY;
            guideX = null;
            guideY = null;
            if (cfg().snapping.isOn()) {
                float r = cfg().snapRange.getFloat();
                float[] xs = {0, width / 2f - dragging.getWidth() / 2, width - dragging.getWidth()};
                float[] gxs = {0, width / 2f, width};
                for (int i = 0; i < xs.length; i++) if (Math.abs(nx - xs[i]) < r) { nx = xs[i]; guideX = gxs[i]; }
                float[] ys = {0, height / 2f - dragging.getHeight() / 2, height - dragging.getHeight()};
                float[] gys = {0, height / 2f, height};
                for (int i = 0; i < ys.length; i++) if (Math.abs(ny - ys[i]) < r) { ny = ys[i]; guideY = gys[i]; }
                for (HudElement o : elements()) {
                    if (o == dragging || !o.isEnabled()) continue;
                    if (Math.abs(nx - o.getX()) < r) { nx = o.getX(); guideX = o.getX(); }
                    if (Math.abs(nx + dragging.getWidth() - (o.getX() + o.getWidth())) < r) { nx = o.getX() + o.getWidth() - dragging.getWidth(); guideX = o.getX() + o.getWidth(); }
                    if (Math.abs(ny - (o.getY() + o.getHeight() + 2)) < r) { ny = o.getY() + o.getHeight() + 2; guideY = ny; }
                }
            }
            dragging.setPosition(nx, ny);
        }

        for (HudElement h : elements()) {
            if (!h.isEnabled()) continue;
            h.prepare(width, height);
            h.render(c, delta, true);
            Animation an = hover.computeIfAbsent(h, k -> new Animation(18, 0));
            boolean hov = RenderUtil.hovered(mouseX, mouseY, h.getX(), h.getY(), h.getWidth(), h.getHeight());
            an.setTarget(hov || h == dragging ? 1 : 0);
            float hv = an.update();
            RenderUtil.roundOutline(c, h.getX() - 2, h.getY() - 2, h.getWidth() + 4, h.getHeight() + 4, 3, 0.5f,
                    ColorUtil.lerp(0x40FFFFFF, Theme.accent(), hv));
            if (hv > 0.02f) {
                float prev = RenderUtil.globalAlpha;
                RenderUtil.globalAlpha = prev * hv;
                String n = h.getName();
                float lw = RenderUtil.width(n) + 6;
                float ly = h.getY() - 14 < 0 ? h.getY() + h.getHeight() + 3 : h.getY() - 14;
                RenderUtil.round(c, h.getX() - 2, ly, lw, 11, 3, Theme.accent());
                RenderUtil.text(c, n, h.getX() + 1, ly + 1.5f, Theme.onAccent(), false);
                RenderUtil.globalAlpha = prev;
            }
        }

        if (guideX != null) RenderUtil.rect(c, guideX - 0.5f, 0, 1, height, ColorUtil.alpha(Theme.accent(), 0.8f));
        if (guideY != null) RenderUtil.rect(c, 0, guideY - 0.5f, width, 1, ColorUtil.alpha(Theme.accent(), 0.8f));

        renderToggleBar(c, mouseX, mouseY);
        RenderUtil.globalAlpha = 1f;
    }

    private final Map<HudElement, Animation> chipAnim = new HashMap<>();

    private void renderToggleBar(DrawContext c, int mx, int my) {
        List<HudElement> els = elements();
        float total = 0;
        for (HudElement h : els) total += RenderUtil.width(h.getName()) + 14;
        float bx = width / 2f - total / 2, by = height / 2f + 30;
        String title = "§lHUD Editor";
        RenderUtil.centered(c, title, width / 2f, by - 26, Theme.TEXT);
        RenderUtil.centered(c, "Drag elements §8•§7 click a chip to toggle §8•§7 Esc to go back", width / 2f, by - 14, Theme.TEXT_DIM);
        for (HudElement h : els) {
            float w = RenderUtil.width(h.getName()) + 10;
            Animation an = chipAnim.computeIfAbsent(h, k -> new Animation(16, h.isEnabled() ? 1 : 0));
            an.setTarget(h.isEnabled() ? 1 : 0);
            float t = an.update();
            boolean hov = RenderUtil.hovered(mx, my, bx, by, w, 14);
            RenderUtil.round(c, bx, by, w, 14, 7, ColorUtil.lerp(hov ? 0xF0202020 : 0xF0101010, Theme.accent(), t));
            RenderUtil.roundOutline(c, bx, by, w, 14, 7, 0.5f, Theme.BORDER);
            RenderUtil.text(c, h.getName(), bx + 5, by + 3, ColorUtil.lerp(Theme.TEXT_DIM, Theme.onAccent(), t), false);
            bx += w + 4;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        List<HudElement> els = elements();
        float total = 0;
        for (HudElement h : els) total += RenderUtil.width(h.getName()) + 14;
        float bx = width / 2f - total / 2, by = height / 2f + 30;
        for (HudElement h : els) {
            float w = RenderUtil.width(h.getName()) + 10;
            if (RenderUtil.hovered(mx, my, bx, by, w, 14)) {
                h.toggle();
                ClickGuiScreen.click(h.isEnabled() ? 1.2f : 0.9f);
                return true;
            }
            bx += w + 4;
        }
        for (int i = els.size() - 1; i >= 0; i--) {
            HudElement h = els.get(i);
            if (!h.isEnabled()) continue;
            if (RenderUtil.hovered(mx, my, h.getX() - 2, h.getY() - 2, h.getWidth() + 4, h.getHeight() + 4)) {
                if (button == 0) {
                    dragging = h;
                    offX = (float) mx - h.getX();
                    offY = (float) my - h.getY();
                } else if (button == 1) {
                    h.setEnabled(false);
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        guideX = null;
        guideY = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        WaveClient.INSTANCE.config.save();
        MinecraftClient.getInstance().setScreen(parent instanceof ClickGuiScreen ? ClickGuiScreen.get() : parent);
    }
}
