package dev.nova.client.gui;

import dev.nova.client.gui.component.*;
import dev.nova.client.module.Module;
import dev.nova.client.setting.*;
import dev.nova.client.util.Animation;
import dev.nova.client.util.ColorUtil;
import dev.nova.client.util.Render2D;
import dev.nova.client.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public class ModuleButton {
    public static final int HEIGHT = 16;

    final Module module;
    private final List<Component> components = new ArrayList<>();
    private final Animation toggleAnim, expandAnim, hoverAnim = new Animation(18, 0);
    private boolean expanded;
    private double x, y, w;
    private boolean rendered;

    public ModuleButton(Module module) {
        this.module = module;
        this.toggleAnim = new Animation(14, module.isEnabled() ? 1 : 0);
        this.expandAnim = new Animation(14, 0);
        for (Setting<?> s : module.getSettings()) {
            if (s instanceof BooleanSetting b) components.add(new BooleanComponent(b));
            else if (s instanceof NumberSetting n) components.add(new NumberComponent(n));
            else if (s instanceof ModeSetting m) components.add(new ModeComponent(m));
            else if (s instanceof ColorSetting c) components.add(new ColorComponent(c));
            else if (s instanceof KeybindSetting k) components.add(new KeybindComponent(k));
        }
    }

    private double settingsHeight() {
        double h = 0;
        for (Component c : components) if (c.setting().isVisible()) h += c.height();
        return h + 2;
    }

    /** Current animated height (header + visible part of the settings). */
    public double height() {
        return HEIGHT + settingsHeight() * expandAnim.setTarget(expanded ? 1 : 0).get();
    }

    public void render(DrawContext ctx, ClickGuiScreen screen, double x, double y, double w, int mx, int my) {
        this.x = x; this.y = y; this.w = w;
        this.rendered = true;
        var font = MinecraftClient.getInstance().textRenderer;

        boolean hov = Render2D.hovered(mx, my, x, y, w, HEIGHT) && screen.canHover();
        double h = hoverAnim.setTarget(hov ? 1 : 0).get();
        double t = toggleAnim.setTarget(module.isEnabled() ? 1 : 0).get();

        Render2D.rect(ctx, x, y, w, HEIGHT, c(ColorUtil.lerp(Theme.MODULE, Theme.MODULE_HOV, h)));
        if (t > 0.01) {
            // Enabled modules get an accent tint fading from the left.
            int accent = Theme.accent(y);
            for (int i = 0; i < w; i += 4) {
                double f = (1 - i / w) * 0.28 * t;
                ctx.fill((int) (x + i), (int) y, (int) Math.min(x + i + 4, x + w), (int) (y + HEIGHT), ColorUtil.alpha(accent, f * ClickGuiScreen.fade));
            }
            Render2D.rect(ctx, x, y + (HEIGHT - HEIGHT * t) / 2, 2, HEIGHT * t, c(accent));
        }

        int textColor = ColorUtil.lerp(Theme.TEXT_OFF, Theme.TEXT, t);
        ctx.drawText(font, module.getName(), (int) (x + 6 + 2 * h), (int) (y + 4), c(textColor), false);

        if (components.size() > 1) {
            double e = expandAnim.get();
            String arrow = e > 0.5 ? "-" : "+";
            ctx.drawText(font, arrow, (int) (x + w - 10), (int) (y + 4), c(ColorUtil.lerp(Theme.TEXT_DIM, Theme.accent(y), e)), false);
        }

        if (hov) screen.setTooltip(module.getDescription());

        double e = expandAnim.get();
        for (Component c : components) c.beginFrame();
        if (e > 0.001) {
            double sh = settingsHeight() * e;
            double sy = y + HEIGHT;
            Render2D.rect(ctx, x, sy, w, sh, c(Theme.SETTING_BG));
            Render2D.rect(ctx, x, sy, 1, sh, c(ColorUtil.alpha(Theme.accent(sy), 0.6)));
            ctx.enableScissor((int) x, (int) sy, (int) (x + w), (int) Math.ceil(sy + sh));
            double cy = sy + 1;
            for (Component c : components) {
                if (!c.setting().isVisible()) continue;
                c.render(ctx, x + 2, cy, w - 2, mx, my);
                cy += c.height();
            }
            ctx.disableScissor();
        }
    }

    public void beginFrame() { rendered = false; }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!rendered) return false;
        // A listening keybind swallows the next click anywhere.
        for (Component c : components) if (c instanceof KeybindComponent k && k.isListening()) return c.mouseClicked(mx, my, button);

        if (Render2D.hovered(mx, my, x, y, w, HEIGHT)) {
            if (button == 0) module.toggle();
            else if (button == 1) expanded = !expanded;
            return true;
        }
        if (expanded && expandAnim.get() > 0.9) {
            for (Component c : components)
                if (c.setting().isVisible() && c.mouseClicked(mx, my, button)) return true;
        }
        return false;
    }

    public void mouseReleased(double mx, double my, int button) {
        for (Component c : components) c.mouseReleased(mx, my, button);
    }

    public boolean keyPressed(int key) {
        for (Component c : components) if (c.keyPressed(key)) return true;
        return false;
    }

    public boolean isListening() {
        for (Component c : components) if (c instanceof KeybindComponent k && k.isListening()) return true;
        return false;
    }

    public boolean matches(String query) {
        return query.isEmpty() || module.getName().toLowerCase().contains(query.toLowerCase());
    }

    private static int c(int color) { return ColorUtil.alpha(color, ClickGuiScreen.fade); }
}
