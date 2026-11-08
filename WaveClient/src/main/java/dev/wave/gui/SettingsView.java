package dev.wave.gui;

import dev.wave.gui.components.*;
import dev.wave.hud.HudElement;
import dev.wave.module.Module;
import dev.wave.setting.*;
import dev.wave.util.*;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Full page with all settings of one module, laid out in two columns. */
public class SettingsView {
    final Module module;
    private final List<Component> left = new ArrayList<>(), right = new ArrayList<>();
    final BindComponent bind;
    private final Animation toggle;
    private final Animation backHover = new Animation(18, 0);
    private float x, y, w, h;
    private float contentHeight;

    public SettingsView(Module module) {
        this.module = module;
        this.toggle = new Animation(14, module.isEnabled() ? 1 : 0);
        List<Component> all = new ArrayList<>();
        for (Setting<?> s : module.getSettings()) add(all, s);
        bind = new BindComponent(module);
        List<Component> general = new ArrayList<>();
        general.add(bind);
        add(general, module.bindMode);
        if (!(module instanceof HudElement)) add(general, module.drawn);
        // balance the module settings over two columns, general settings go at the end of the shorter one
        float lh = 0, rh = 0;
        for (Component c : all) {
            if (lh <= rh) { left.add(c); lh += c.baseHeight(); } else { right.add(c); rh += c.baseHeight(); }
        }
        (lh <= rh ? left : right).addAll(general);
    }

    private static void add(List<Component> list, Setting<?> s) {
        if (s instanceof BooleanSetting b) list.add(new BoolComponent(b));
        else if (s instanceof NumberSetting n) list.add(new SliderComponent(n));
        else if (s instanceof ModeSetting m) list.add(new ModeComponent(m));
        else if (s instanceof ColorSetting c) list.add(new ColorComponent(c));
    }

    private float[] backBox() { return new float[]{x, y, 16, 16}; }
    private float[] switchBox() { return new float[]{x + w - 26, y + 3, 24, 12}; }

    public float contentHeight() { return contentHeight; }

    public void render(DrawContext c, float x, float y, float w, float h, float scroll, int mx, int my) {
        this.x = x; this.y = y; this.w = w; this.h = h;
        float[] b = backBox();
        boolean bh = RenderUtil.hovered(mx, my, b[0], b[1], b[2], b[3]);
        backHover.setTarget(bh ? 1 : 0);
        float bv = backHover.update();
        RenderUtil.round(c, b[0], b[1], b[2], b[3], 5, ColorUtil.lerp(0xFF161616, 0xFF262626, bv));
        Icons.draw(c, "back", b[0] + 3 - bv, b[1] + 3, 10, Theme.TEXT);

        toggle.setTarget(module.isEnabled() ? 1 : 0);
        float t = Easing.outCubic(toggle.update());
        RenderUtil.round(c, x + 22, y - 2, 20, 20, 5, ColorUtil.lerp(0xFF1F1F1F, Theme.accent(), t));
        Icons.draw(c, module.getIcon(), x + 26, y + 2, 12, ColorUtil.lerp(Theme.TEXT, Theme.onAccent(), t));
        RenderUtil.textScaled(c, "§l" + module.getName(), x + 48, y - 1, 1.25f, Theme.TEXT, false);
        RenderUtil.textScaled(c, module.getDescription(), x + 48, y + 11, 0.75f, Theme.TEXT_DIM, false);
        float[] s = switchBox();
        RenderUtil.round(c, s[0], s[1], s[2], s[3], 6, ColorUtil.lerp(0xFF2A2A2A, Theme.accent(), t));
        RenderUtil.circle(c, s[0] + 6 + t * 12, s[1] + 6, 4, ColorUtil.lerp(0xFF8A8A8A, Theme.onAccent(), t));

        float top = y + 26;
        RenderUtil.rect(c, x, top - 4, w, 0.5f, 0x20FFFFFF);
        RenderUtil.scissor(c, x - 2, top, w + 4, h - 26);
        float colW = (w - 8) / 2;
        float lh = column(c, left, x, top + scroll, colW, mx, my);
        float rh = column(c, right, x + colW + 8, top + scroll, colW, mx, my);
        contentHeight = Math.max(lh, rh);
        RenderUtil.endScissor(c);
    }

    private float column(DrawContext c, List<Component> comps, float cx, float cy, float cw, int mx, int my) {
        if (comps.isEmpty()) return 0;
        float total = 6;
        for (Component comp : comps) total += comp.height();
        RenderUtil.round(c, cx, cy, cw, total, 6, 0xFF121212);
        RenderUtil.roundOutline(c, cx, cy, cw, total, 6, 0.5f, 0x16FFFFFF);
        float yy = cy + 3;
        for (Component comp : comps) {
            comp.setBounds(cx + 6, yy, cw - 12);
            comp.draw(c, mx, my);
            yy += comp.height();
        }
        return total;
    }

    public String tooltip(int mx, int my) {
        for (List<Component> l : List.of(left, right))
            for (Component comp : l) if (comp.active() && comp.isHovered(mx, my)) return comp.tooltip();
        return null;
    }

    /** @return true if consumed; sets {@code back} via return code -1 */
    public int click(double mx, double my, int button) {
        if (ClickGuiScreen.listening != null) {
            ClickGuiScreen.listening.mouseClicked(mx, my, button);
            return 1;
        }
        float[] b = backBox();
        if (button == 0 && RenderUtil.hovered(mx, my, b[0], b[1], b[2], b[3])) return -1;
        float[] s = switchBox();
        if (button == 0 && RenderUtil.hovered(mx, my, s[0] - 2, s[1] - 2, s[2] + 4, s[3] + 4)) {
            module.toggle();
            ClickGuiScreen.click(module.isEnabled() ? 1.25f : 0.85f);
            return 1;
        }
        if (my < y + 26) return 0;
        for (List<Component> l : List.of(left, right))
            for (Component comp : l) if (comp.active() && comp.mouseClicked(mx, my, button)) return 1;
        return 0;
    }

    public void released(double mx, double my, int button) {
        for (List<Component> l : List.of(left, right)) for (Component comp : l) comp.mouseReleased(mx, my, button);
    }

    public boolean scrollSlider(double mx, double my, double amount) {
        for (List<Component> l : List.of(left, right))
            for (Component comp : l) if (comp instanceof SliderComponent sl && comp.active() && sl.scroll(mx, my, amount)) return true;
        return false;
    }
}
