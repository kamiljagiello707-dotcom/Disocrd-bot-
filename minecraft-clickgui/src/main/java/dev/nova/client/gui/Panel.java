package dev.nova.client.gui;

import dev.nova.client.NovaClient;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.util.Animation;
import dev.nova.client.util.ColorUtil;
import dev.nova.client.util.Render2D;
import dev.nova.client.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Draggable category window. Left-drag header to move, right-click header to collapse, scroll to scroll. */
public class Panel {
    public static final int WIDTH = 114, HEADER = 22, MAX_CONTENT = 300;

    public final Category category;
    public final PanelState state;
    private final List<ModuleButton> buttons = new ArrayList<>();
    private final Animation openAnim, scrollAnim = new Animation(16, 0);
    private boolean dragging;
    private double dragX, dragY, scroll;
    private double drawX, drawY, contentH;

    public Panel(Category category, PanelState state) {
        this.category = category;
        this.state = state;
        this.openAnim = new Animation(12, state.open ? 1 : 0);
        for (Module m : NovaClient.get().modules.byCategory(category)) buttons.add(new ModuleButton(m));
    }

    public void render(DrawContext ctx, ClickGuiScreen screen, int mx, int my, double offsetY, String query) {
        if (dragging) {
            state.x = mx - dragX;
            state.y = my - dragY;
        }
        var font = MinecraftClient.getInstance().textRenderer;
        drawX = state.x;
        drawY = state.y + offsetY;

        List<ModuleButton> visible = buttons.stream().filter(b -> b.matches(query)).toList();
        for (ModuleButton b : buttons) b.beginFrame();

        double total = 0;
        for (ModuleButton b : visible) total += b.height();
        double open = openAnim.setTarget(state.open ? 1 : 0).get();
        contentH = Math.min(total, MAX_CONTENT) * open;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, total - MAX_CONTENT)));
        double sc = scrollAnim.setTarget(scroll).get();

        double fullH = HEADER + contentH + (contentH > 0 ? 3 : 0);
        Render2D.shadow(ctx, drawX, drawY, WIDTH, fullH, 8, ClickGuiScreen.fade);
        Render2D.roundRect(ctx, drawX, drawY, WIDTH, fullH, 5, c(Theme.PANEL));

        // Header
        Render2D.roundRectTop(ctx, drawX, drawY, WIDTH, HEADER, 5, c(Theme.BACKGROUND));
        Render2D.accentLine(ctx, drawX, drawY + HEADER - 1, WIDTH, 1, ClickGuiScreen.fade);
        ctx.drawText(font, category.icon, (int) drawX + 7, (int) drawY + 7, c(Theme.accent(drawX)), false);
        ctx.drawText(font, category.displayName, (int) drawX + 19, (int) drawY + 7, c(Theme.TEXT), false);
        String count = String.valueOf(visible.size());
        ctx.drawText(font, count, (int) (drawX + WIDTH - 8 - font.getWidth(count)), (int) drawY + 7, c(Theme.TEXT_DIM), false);

        if (contentH > 0.5) {
            int top = (int) (drawY + HEADER), bottom = (int) Math.ceil(drawY + HEADER + contentH);
            ctx.enableScissor((int) drawX, top, (int) drawX + WIDTH, bottom);
            double by = drawY + HEADER - sc;
            for (ModuleButton b : visible) {
                double bh = b.height();
                if (by + bh >= top && by <= bottom) b.render(ctx, screen, drawX, by, WIDTH, mx, my);
                by += bh;
            }
            ctx.disableScissor();

            // Scrollbar
            if (total > MAX_CONTENT) {
                double barH = contentH * (MAX_CONTENT / total);
                double barY = top + (contentH - barH) * (sc / (total - MAX_CONTENT));
                Render2D.roundRect(ctx, drawX + WIDTH - 3, barY, 2, barH, 1, c(ColorUtil.alpha(Theme.accent(barY), 0.7)));
            }
        }
    }

    private boolean overHeader(double mx, double my) { return Render2D.hovered(mx, my, drawX, drawY, WIDTH, HEADER); }
    private boolean overContent(double mx, double my) { return Render2D.hovered(mx, my, drawX, drawY + HEADER, WIDTH, contentH); }
    public boolean isOver(double mx, double my) { return overHeader(mx, my) || overContent(mx, my); }

    public boolean mouseClicked(double mx, double my, int button) {
        for (ModuleButton b : buttons) if (b.isListening()) return b.mouseClicked(mx, my, button);
        if (overHeader(mx, my)) {
            if (button == 0) { dragging = true; dragX = mx - state.x; dragY = my - state.y; }
            else if (button == 1) state.open = !state.open;
            return true;
        }
        if (overContent(mx, my)) {
            for (ModuleButton b : buttons) if (b.mouseClicked(mx, my, button)) return true;
            return true;
        }
        return false;
    }

    public void mouseReleased(double mx, double my, int button) {
        dragging = false;
        for (ModuleButton b : buttons) b.mouseReleased(mx, my, button);
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!overContent(mx, my)) return false;
        scroll -= amount * 18;
        return true;
    }

    public boolean keyPressed(int key) {
        for (ModuleButton b : buttons) if (b.keyPressed(key)) return true;
        return false;
    }

    public boolean isListening() {
        for (ModuleButton b : buttons) if (b.isListening()) return true;
        return false;
    }

    private static int c(int color) { return ColorUtil.alpha(color, ClickGuiScreen.fade); }
}
