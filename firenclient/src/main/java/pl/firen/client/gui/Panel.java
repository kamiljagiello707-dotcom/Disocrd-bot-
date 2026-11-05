package pl.firen.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import pl.firen.client.FirenClient;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.ModeSetting;
import pl.firen.client.setting.NumberSetting;
import pl.firen.client.setting.Setting;
import pl.firen.client.util.Anim;

import java.util.List;
import java.util.Locale;

/** Jeden panel kategorii w ClickGUI. */
public class Panel {
    public static final int WIDTH = 116;
    public static final int HEADER = 20;
    public static final int ROW = 16;
    public static final int SET_ROW = 15;

    public final Category category;
    public int x, y;
    public boolean collapsed;
    public boolean positioned;
    private float collapseAnim = 1f;

    public Panel(Category category) {
        this.category = category;
    }

    private static int settingsHeight(Module m) {
        return (m.getSettings().size() + 1) * SET_ROW + 2;
    }

    public void render(DrawContext ctx, ClickGuiScreen screen, int mx, int my, float dt) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String query = screen.query().toLowerCase(Locale.ROOT);
        List<Module> modules = FirenClient.modules().byCategory(category).stream()
                .filter(m -> query.isEmpty() || m.getName().toLowerCase(Locale.ROOT).contains(query))
                .toList();

        collapseAnim = Anim.approach(collapseAnim, collapsed ? 0f : 1f, dt, 16f);

        // Wysokość zawartości
        int content = 2;
        for (Module m : modules) {
            m.expandAnim = Anim.approach(m.expandAnim, m.expanded ? 1f : 0f, dt, 16f);
            content += ROW + Math.round(settingsHeight(m) * m.expandAnim);
        }
        content += 2;
        int bodyH = Math.round(content * collapseAnim);

        int x2 = x + WIDTH;
        int bottom = y + HEADER + bodyH;

        // Cień + obramowanie
        ctx.fill(x + 2, y + 3, x2 + 2, bottom + 3, 0x50000000);
        Draw.round(ctx, x - 1, y - 1, x2 + 1, bottom + 1, Theme.BORDER);
        Draw.round(ctx, x, y, x2, bottom, Theme.BODY);

        // Nagłówek
        ctx.fill(x, y, x2, y + HEADER, Theme.HEADER);
        ctx.drawText(tr, Text.literal(category.display).formatted(Formatting.BOLD), x + 7, y + 6, Theme.WHITE, false);
        String count = String.valueOf(modules.stream().filter(Module::isEnabled).count()) + "/" + modules.size();
        ctx.drawText(tr, count, x2 - 7 - tr.getWidth(count), y + 6, Theme.TEXT_DIM, false);
        ctx.fill(x + 6, y + HEADER - 1, x2 - 6, y + HEADER, Anim.withAlpha(Theme.WHITE, 0.18f + 0.6f * collapseAnim));
        screen.hit(x, y, x2, y + HEADER, button -> {
            if (button == 0) screen.startPanelDrag(this, mx, my);
            else if (button == 1) collapsed = !collapsed;
        });

        if (bodyH <= 0) return;
        screen.scissor(ctx, x, y + HEADER, x2, bottom);

        int cy = y + HEADER + 2;
        for (Module m : modules) {
            boolean hover = mx >= x && mx < x2 && my >= cy && my < cy + ROW && my < bottom;
            m.hoverAnim = Anim.approach(m.hoverAnim, hover ? 1f : 0f, dt, 20f);
            m.toggleAnim = Anim.approach(m.toggleAnim, m.isEnabled() ? 1f : 0f, dt, 18f);
            if (hover) screen.setTooltip(m.getDescription());

            int bg = Anim.lerpColor(Anim.lerpColor(Theme.ROW, Theme.ROW_HOVER, m.hoverAnim), Theme.WHITE, m.toggleAnim);
            int fg = Anim.lerpColor(Anim.lerpColor(Theme.TEXT, Theme.WHITE, m.hoverAnim), Theme.BLACK, m.toggleAnim);
            Draw.round(ctx, x + 3, cy + 1, x2 - 3, cy + ROW - 1, bg);
            int textX = x + 8 + Math.round(m.hoverAnim * 2);
            ctx.drawText(tr, m.getName(), textX, cy + 4, fg, false);

            String right = screen.bindingModule() == m ? "..." : (m.getSettings().isEmpty() && !m.expanded ? "" : (m.expanded ? "-" : "+"));
            if (!right.isEmpty()) {
                int rc = Anim.lerpColor(Theme.TEXT_DIM, 0xFF555555, m.toggleAnim);
                ctx.drawText(tr, right, x2 - 9 - tr.getWidth(right), cy + 4, rc, false);
            }

            final int rowY = cy;
            screen.hit(x, rowY, x2, Math.min(rowY + ROW, bottom), button -> {
                if (button == 0) m.toggle();
                else if (button == 1) m.expanded = !m.expanded;
                else if (button == 2) screen.startBinding(m);
            });
            cy += ROW;

            int sh = Math.round(settingsHeight(m) * m.expandAnim);
            if (sh > 0) {
                int sTop = cy;
                int sBottom = Math.min(cy + sh, bottom);
                screen.scissor(ctx, x, sTop, x2, sBottom);
                renderSettings(ctx, screen, tr, m, x, cy, mx, my, dt, sBottom);
                ctx.disableScissor(); // wraca do scissora panelu
                cy += sh;
            }
        }
        ctx.disableScissor();
    }

    private void renderSettings(DrawContext ctx, ClickGuiScreen screen, TextRenderer tr, Module m,
                                int x, int top, int mx, int my, float dt, int clipBottom) {
        int x2 = x + WIDTH;
        int h = settingsHeight(m);
        ctx.fill(x + 3, top, x2 - 3, top + h - 1, Theme.SETTINGS_BG);
        ctx.fill(x + 3, top, x + 4, top + h - 1, Anim.withAlpha(Theme.WHITE, 0.85f));

        int sy = top + 1;
        for (Setting<?> s : m.getSettings()) {
            int rowY = sy;
            boolean visible = rowY < clipBottom;
            if (s instanceof BooleanSetting b) {
                b.anim = Anim.approach(b.anim, b.get() ? 1f : 0f, dt, 20f);
                ctx.drawText(tr, b.getName(), x + 9, rowY + 4, Theme.TEXT, false);
                int bx = x2 - 22, by = rowY + 4;
                // przełącznik
                Draw.round(ctx, bx, by, bx + 14, by + 7, Anim.lerpColor(Theme.TRACK, Theme.WHITE, b.anim));
                int knob = bx + 1 + Math.round(b.anim * 7);
                Draw.round(ctx, knob, by + 1, knob + 5, by + 6, Anim.lerpColor(Theme.WHITE, Theme.BLACK, b.anim));
                if (visible) screen.hit(x, rowY, x2, rowY + SET_ROW, button -> b.toggle());
            } else if (s instanceof NumberSetting n) {
                String val = n.display();
                ctx.drawText(tr, n.getName(), x + 9, rowY + 2, Theme.TEXT, false);
                ctx.drawText(tr, val, x2 - 8 - tr.getWidth(val), rowY + 2, Theme.WHITE, false);
                int tx1 = x + 9, tx2 = x2 - 8, ty = rowY + SET_ROW - 3;
                if (screen.isDragging(n)) n.setFromPercent((mx - tx1) / (double) (tx2 - tx1));
                ctx.fill(tx1, ty, tx2, ty + 1, Theme.TRACK);
                int fillX = tx1 + (int) Math.round((tx2 - tx1) * n.getPercent());
                ctx.fill(tx1, ty, fillX, ty + 1, Theme.WHITE);
                ctx.fill(fillX - 1, ty - 1, fillX + 1, ty + 2, Theme.WHITE);
                if (visible) screen.hit(x, rowY, x2, rowY + SET_ROW, button -> {
                    if (button == 0) screen.startSliderDrag(n);
                });
            } else if (s instanceof ModeSetting md) {
                String val = md.get();
                ctx.drawText(tr, md.getName(), x + 9, rowY + 4, Theme.TEXT, false);
                ctx.drawText(tr, val, x2 - 8 - tr.getWidth(val), rowY + 4, Theme.WHITE, false);
                if (visible) screen.hit(x, rowY, x2, rowY + SET_ROW, button -> md.cycle(button == 1 ? -1 : 1));
            }
            sy += SET_ROW;
        }

        // Bind
        String key = screen.bindingModule() == m ? "Naciśnij klawisz..." : ClickGuiScreen.keyName(m.getKey());
        ctx.drawText(tr, "Bind", x + 9, sy + 4, Theme.TEXT, false);
        ctx.drawText(tr, key, x2 - 8 - tr.getWidth(key), sy + 4, Theme.TEXT_DIM, false);
        if (sy < clipBottom) screen.hit(x, sy, x2, sy + SET_ROW, button -> screen.startBinding(m));
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx < x + WIDTH && my >= y && my < y + HEADER;
    }
}
