package dev.wave.gui;

import dev.wave.gui.components.*;
import dev.wave.module.Module;
import dev.wave.setting.*;
import dev.wave.util.Animation;
import dev.wave.util.ColorUtil;
import dev.wave.util.Easing;
import dev.wave.util.KeyUtil;
import dev.wave.util.RenderUtil;
import dev.wave.util.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public class ModuleButton {
    public static final float HEIGHT = 16;

    final Module module;
    private final List<Component> components = new ArrayList<>();
    private final BindComponent bind;
    private boolean expanded;

    private final Animation expand = new Animation(14, 0);
    private final Animation toggle;
    private final Animation hover = new Animation(18, 0);
    final Animation filter = new Animation(14, 1);

    private float x, y, w;

    public ModuleButton(Module module) {
        this.module = module;
        this.toggle = new Animation(14, module.isEnabled() ? 1 : 0);
        for (Setting<?> s : module.getSettings()) add(s);
        bind = new BindComponent(module);
        components.add(bind);
        add(module.bindMode);
        if (!(module instanceof dev.wave.hud.HudElement)) add(module.drawn);
    }

    private void add(Setting<?> s) {
        if (s instanceof BooleanSetting b) components.add(new BoolComponent(b));
        else if (s instanceof NumberSetting n) components.add(new SliderComponent(n));
        else if (s instanceof ModeSetting m) components.add(new ModeComponent(m));
        else if (s instanceof ColorSetting c) components.add(new ColorComponent(c));
    }

    public boolean matches(String search) {
        return search.isEmpty() || module.getName().toLowerCase().contains(search.toLowerCase());
    }

    private float settingsHeight() {
        float h = 3;
        for (Component c : components) h += c.height();
        return h + 2;
    }

    /** Full animated height of the button (including settings and search filter). */
    public float height() {
        float e = Easing.outCubic(expand.get());
        return (HEIGHT + settingsHeight() * e) * filter.get();
    }

    public void render(DrawContext c, float x, float y, float w, int mx, int my, String search) {
        this.x = x;
        this.y = y;
        this.w = w;
        filter.setTarget(matches(search) ? 1 : 0);
        filter.update();
        expand.setTarget(expanded ? 1 : 0);
        expand.update();
        if (filter.get() < 0.01f) return;

        float prevAlpha = RenderUtil.globalAlpha;
        RenderUtil.globalAlpha = prevAlpha * filter.get();

        boolean hovered = RenderUtil.hovered(mx, my, x, y, w, HEIGHT) && ClickGuiScreen.isMouseFree();
        hover.setTarget(hovered ? 1 : 0);
        float hv = hover.update();
        toggle.setTarget(module.isEnabled() ? 1 : 0);
        float t = toggle.update();
        float te = Easing.outCubic(t);

        // background + hover
        RenderUtil.rect(c, x, y, w, HEIGHT, ColorUtil.lerp(Theme.BG, Theme.HOVER, hv));
        // enabled fill grows from the left edge
        if (te > 0.005f) {
            RenderUtil.round(c, x + 2, y + 1.5f, (w - 4) * te, HEIGHT - 3, 3, ColorUtil.alpha(Theme.accent(), 0.92f + 0.08f * hv));
        }
        int textCol = ColorUtil.lerp(ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, hv), Theme.onAccent(), te);
        float nameX = x + 6 + hv * 1.5f;
        String name = module.getName();
        if (!search.isEmpty() && matches(search)) {
            int idx = name.toLowerCase().indexOf(search.toLowerCase());
            name = name.substring(0, idx) + "§n" + name.substring(idx, idx + search.length()) + "§r" + name.substring(idx + search.length());
        }
        RenderUtil.text(c, bind.isListening() ? "Press a key..." : name, nameX, y + 4.5f, textCol);

        // right side: keybind tag + expand indicator
        float rx = x + w - 6;
        float e = Easing.outCubic(expand.get());
        // three dots that rotate into a dash when expanded
        for (int i = 0; i < 3; i++) {
            float dy = (i - 1) * 2.6f * (1 - e);
            float dx = (i - 1) * 2.6f * e;
            RenderUtil.circle(c, rx + dx - 1, y + HEIGHT / 2 + dy, 0.8f, ColorUtil.alpha(textCol, 0.8f));
        }
        if (module.getKey() != KeyUtil.NONE && !bind.isListening()) {
            String k = KeyUtil.name(module.getKey());
            float kw = RenderUtil.width(k) * 0.75f;
            RenderUtil.textScaled(c, k, rx - 7 - kw - (e * 3), y + 5.5f, 0.75f, ColorUtil.alpha(textCol, 0.6f), false);
        }

        // settings
        if (e > 0.01f) {
            float sh = settingsHeight() * e;
            float sy = y + HEIGHT;
            RenderUtil.rect(c, x, sy, w, sh, Theme.BG_DARK);
            RenderUtil.rect(c, x + 2, sy + 1, 1, Math.max(0, sh - 2), ColorUtil.alpha(Theme.accent(), 0.5f * e));
            RenderUtil.scissor(c, x, sy, w, sh);
            float cy = sy + 3;
            for (Component comp : components) {
                comp.setBounds(x + 6, cy, w - 10);
                comp.draw(c, mx, my);
                cy += comp.height();
            }
            RenderUtil.endScissor(c);
        }
        RenderUtil.globalAlpha = prevAlpha;
    }

    public String hoveredTooltip(int mx, int my) {
        if (filter.get() < 0.5f) return null;
        if (RenderUtil.hovered(mx, my, x, y, w, HEIGHT)) return module.getDescription();
        if (expanded && expand.get() > 0.9f) {
            for (Component comp : components) {
                if (comp.active() && comp.isHovered(mx, my)) return comp.tooltip();
            }
        }
        return null;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (filter.get() < 0.5f) return false;
        if (ClickGuiScreen.listening != null) {
            return ClickGuiScreen.listening.mouseClicked(mx, my, button);
        }
        if (RenderUtil.hovered(mx, my, x, y, w, HEIGHT)) {
            switch (button) {
                case 0 -> {
                    module.toggle();
                    ClickGuiScreen.click(module.isEnabled() ? 1.25f : 0.85f);
                }
                case 1 -> {
                    expanded = !expanded;
                    ClickGuiScreen.click(expanded ? 1.15f : 0.95f);
                }
                case 2 -> {
                    ClickGuiScreen.listening = bind;
                    ClickGuiScreen.click(1.3f);
                }
                default -> {}
            }
            return true;
        }
        if (expanded && expand.get() > 0.9f) {
            for (Component comp : components) {
                if (comp.active() && comp.mouseClicked(mx, my, button)) return true;
            }
        }
        return false;
    }

    public void mouseReleased(double mx, double my, int button) {
        for (Component comp : components) comp.mouseReleased(mx, my, button);
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!expanded || expand.get() < 0.9f) return false;
        for (Component comp : components) {
            if (comp instanceof SliderComponent s && comp.active() && s.scroll(mx, my, amount)) return true;
        }
        return false;
    }
}
