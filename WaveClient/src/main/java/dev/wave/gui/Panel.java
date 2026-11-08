package dev.wave.gui;

import dev.wave.WaveClient;
import dev.wave.module.Category;
import dev.wave.module.Module;
import dev.wave.util.Animation;
import dev.wave.util.ColorUtil;
import dev.wave.util.Easing;
import dev.wave.util.RenderUtil;
import dev.wave.util.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public class Panel {
    public static final float WIDTH = 116, HEADER = 20;

    final Category category;
    float x, y;
    boolean open = true;
    private boolean dragging;
    private float dragX, dragY;
    final List<ModuleButton> buttons = new ArrayList<>();
    private final Animation openAnim = new Animation(14, 1);
    private final Animation headerHover = new Animation(18, 0);
    private float renderY;

    public Panel(Category category, float x, float y) {
        this.category = category;
        this.x = x;
        this.y = y;
        for (Module m : WaveClient.INSTANCE.modules.getByCategory(category)) buttons.add(new ModuleButton(m));
    }

    public void setOpenInstant(boolean open) {
        this.open = open;
        openAnim.set(open ? 1 : 0);
    }

    public boolean isDragging() { return dragging; }

    public void render(DrawContext c, int mx, int my, float scroll, String search) {
        if (dragging) {
            x = mx - dragX;
            y = my - dragY - scroll;
        }
        renderY = y + scroll;
        float py = renderY;
        openAnim.setTarget(open ? 1 : 0);
        float o = Easing.outCubic(openAnim.update());

        float content = 0;
        for (ModuleButton b : buttons) content += b.height();
        boolean anyMatch = search.isEmpty() || buttons.stream().anyMatch(b -> b.matches(search));
        float shown = content * o;

        // soft shadow + outline
        float total = HEADER + shown + 4;
        RenderUtil.round(c, x - 1.5f, py - 1.5f, WIDTH + 3, total + 3, 6, 0x22000000);
        RenderUtil.round(c, x - 0.5f, py - 0.5f, WIDTH + 1, total + 1, 5, 0x18FFFFFF);

        // header
        boolean hov = RenderUtil.hovered(mx, my, x, py, WIDTH, HEADER) && ClickGuiScreen.isMouseFree();
        headerHover.setTarget(hov || dragging ? 1 : 0);
        float hh = headerHover.update();
        RenderUtil.round(c, x, py, WIDTH, HEADER, 5, ColorUtil.lerp(0xFA0A0A0A, 0xFA141414, hh), true, false);
        float prev = RenderUtil.globalAlpha;
        if (!anyMatch) RenderUtil.globalAlpha = prev * 0.4f;
        RenderUtil.text(c, category.icon, x + 7, py + 6, Theme.accent(), false);
        RenderUtil.text(c, "§l" + category.displayName, x + 19, py + 6, Theme.TEXT, false);
        long enabled = buttons.stream().filter(b -> b.module.isEnabled()).count();
        String count = enabled + "/" + buttons.size();
        RenderUtil.textScaled(c, count, x + WIDTH - 8 - RenderUtil.width(count) * 0.75f, py + 7, 0.75f, Theme.TEXT_DIM, false);
        RenderUtil.globalAlpha = prev;
        // accent line under header, width follows open state
        float lw = (WIDTH - 12) * (0.25f + 0.75f * o);
        RenderUtil.round(c, x + WIDTH / 2 - lw / 2, py + HEADER - 1.5f, lw, 1, 0.5f, ColorUtil.alpha(Theme.accent(), 0.35f + 0.4f * o));

        // modules
        float by = py + HEADER;
        RenderUtil.rect(c, x, by, WIDTH, shown, Theme.BG);
        if (shown > 0.5f) {
            RenderUtil.scissor(c, x, by, WIDTH, shown);
            float cy = by;
            for (ModuleButton b : buttons) {
                float h = b.height();
                if (cy + h >= by && cy <= by + shown) b.render(c, x, cy, WIDTH, mx, my, search);
                else b.render(c, x, -10000, WIDTH, mx, my, search); // keep animations ticking
                cy += h;
            }
            RenderUtil.endScissor(c);
        }
        RenderUtil.round(c, x, by + shown, WIDTH, 4, 4, Theme.BG, false, true);
    }

    public String tooltip(int mx, int my) {
        if (openAnim.get() < 0.95f) return null;
        for (ModuleButton b : buttons) {
            String t = b.hoveredTooltip(mx, my);
            if (t != null && !t.isEmpty()) return t;
        }
        return null;
    }

    public boolean isOverHeader(double mx, double my) { return RenderUtil.hovered(mx, my, x, renderY, WIDTH, HEADER); }

    public boolean isOver(double mx, double my) {
        float content = 0;
        for (ModuleButton b : buttons) content += b.height();
        return RenderUtil.hovered(mx, my, x, renderY, WIDTH, HEADER + content * openAnim.get() + 4);
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (ClickGuiScreen.listening == null && isOverHeader(mx, my)) {
            if (button == 0) {
                dragging = true;
                dragX = (float) mx - x;
                dragY = (float) my - renderY;
            } else if (button == 1) {
                open = !open;
                ClickGuiScreen.click(open ? 1.1f : 0.9f);
            }
            return true;
        }
        if (!open || openAnim.get() < 0.9f) return false;
        for (ModuleButton b : buttons) if (b.mouseClicked(mx, my, button)) return true;
        return false;
    }

    public void mouseReleased(double mx, double my, int button) {
        dragging = false;
        for (ModuleButton b : buttons) b.mouseReleased(mx, my, button);
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!open) return false;
        for (ModuleButton b : buttons) if (b.mouseScrolled(mx, my, amount)) return true;
        return false;
    }
}
