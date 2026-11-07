package dev.mustic.client.gui;

import dev.mustic.client.gui.components.*;
import dev.mustic.client.module.Module;
import dev.mustic.client.module.modules.ClickGuiModule;
import dev.mustic.client.setting.*;
import dev.mustic.client.util.Anim;
import dev.mustic.client.util.ColorUtil;
import dev.mustic.client.util.Render2D;
import dev.mustic.client.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ModuleButton {
    public static final int HEIGHT = 15;

    final Module module;
    private final List<Component> components = new ArrayList<>();
    private final BindComponent bind;
    private boolean expanded;
    private final Anim toggleAnim, hoverAnim, expandAnim = new Anim(16f, 0f);
    private int x, y;

    ModuleButton(Module module) {
        this.module = module;
        for (Setting<?> s : module.getSettings()) {
            if (s instanceof BooleanSetting b) components.add(new BooleanComponent(b));
            else if (s instanceof NumberSetting n) components.add(new SliderComponent(n));
            else if (s instanceof ModeSetting m) components.add(new ModeComponent(m));
        }
        bind = new BindComponent(module);
        components.add(bind);
        toggleAnim = new Anim(12f, module.isEnabled() ? 1f : 0f);
        hoverAnim = new Anim(18f, 0f);
    }

    private int settingsHeight() {
        int h = 0;
        for (Component c : components) if (c.visible()) h += c.height();
        return h + 2;
    }

    int getHeight() {
        return HEIGHT + Math.round(expandAnim.get());
    }

    boolean matches(String search) {
        return search.isEmpty() || module.getName().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }

    void render(DrawContext ctx, int x, int y, int mx, int my, Panel panel, ClickGui gui) {
        this.x = x;
        this.y = y;
        int w = Panel.WIDTH;
        boolean canHover = panel.inContent(mx, my) && gui.isInteractive();
        boolean hover = canHover && Render2D.hovered(mx, my, x, y, w, HEIGHT);
        if (hover) gui.setHovered(module);

        float t = toggleAnim.update(module.isEnabled() ? 1f : 0f);
        float hv = hoverAnim.update(hover ? 1f : 0f);
        float ex = expandAnim.update(expanded ? settingsHeight() : 0f);
        float vis = matches(gui.getSearch()) ? 1f : 0.3f;
        int accent = Theme.accent(y * 10L);

        Render2D.rect(ctx, x, y, w, HEIGHT, ColorUtil.lerp(Theme.MODULE, Theme.MODULE_HOVER, hv));
        if (t > 0.01f) {
            Render2D.horizontalGradient(ctx, x, y, (int) (w * t), HEIGHT,
                    ColorUtil.alpha(accent, 0.45f * t * vis), ColorUtil.alpha(Theme.accent(y * 10L + 500), 0.05f * t * vis));
            Render2D.rect(ctx, x, y, 2, HEIGHT, ColorUtil.alpha(accent, t * vis));
        }
        if (!gui.getSearch().isEmpty() && vis == 1f) {
            Render2D.outline(ctx, x, y, w, HEIGHT, ColorUtil.alpha(accent, 0.8f));
        }

        int textColor = ColorUtil.alpha(ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, Math.max(t, hv * 0.6f)), vis);
        ctx.drawText(MinecraftClient.getInstance().textRenderer, module.getName(), x + 6 + Math.round(2 * hv), y + 4, textColor, true);
        String indicator = expanded ? "-" : "+";
        ctx.drawText(MinecraftClient.getInstance().textRenderer, indicator, x + w - 10, y + 4,
                ColorUtil.alpha(Theme.TEXT_DIM, vis), false);

        if (ex > 0.5f) {
            int sy = y + HEIGHT;
            int sh = Math.round(ex);
            Render2D.rect(ctx, x, sy, w, sh, Theme.SETTING);
            Render2D.rect(ctx, x + 2, sy, 1, sh, ColorUtil.alpha(accent, 0.7f));
            int cy = sy + 1;
            for (Component c : components) {
                if (!c.visible()) continue;
                c.render(ctx, x, cy, w, mx, my, canHover);
                cy += c.height();
            }
        }
    }

    boolean mouseClicked(double mx, double my, int button) {
        if (Render2D.hovered(mx, my, x, y, Panel.WIDTH, HEIGHT)) {
            switch (button) {
                case 0 -> {
                    if (module instanceof ClickGuiModule) expanded = !expanded;
                    else module.toggle();
                }
                case 1 -> expanded = !expanded;
                case 2 -> {
                    expanded = true;
                    bind.startListening();
                }
                default -> { return false; }
            }
            return true;
        }
        if (expanded) {
            for (Component c : components)
                if (c.visible() && c.mouseClicked(mx, my, button)) return true;
        }
        return false;
    }

    void mouseReleased(double mx, double my, int button) {
        for (Component c : components) c.mouseReleased(mx, my, button);
    }

    boolean mouseScrolled(double mx, double my, double amount) {
        if (!expanded) return false;
        for (Component c : components) if (c.visible() && c.mouseScrolled(mx, my, amount)) return true;
        return false;
    }

    boolean keyPressed(int key) {
        for (Component c : components) if (c.keyPressed(key)) return true;
        return false;
    }

    boolean isListening() {
        return bind.isListening();
    }
}
