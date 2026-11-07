package dev.mustic.client.gui;

import com.google.gson.JsonObject;
import dev.mustic.client.module.Category;
import dev.mustic.client.module.Module;
import dev.mustic.client.module.ModuleManager;
import dev.mustic.client.util.Anim;
import dev.mustic.client.util.ColorUtil;
import dev.mustic.client.util.Render2D;
import dev.mustic.client.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public class Panel {
    public static final int WIDTH = 112;
    public static final int HEADER = 18;
    private static final int EMPTY_HEIGHT = 16;

    final Category category;
    int x, y;
    boolean open = true;
    private boolean dragging;
    private int dragX, dragY;
    private final List<ModuleButton> buttons = new ArrayList<>();
    private final Anim heightAnim = new Anim(16f, 0f);
    private final Anim headerHover = new Anim(18f, 0f);
    private int drawY, visibleHeight;

    Panel(Category category, int x, int y) {
        this.category = category;
        this.x = x;
        this.y = y;
        for (Module m : ModuleManager.getByCategory(category)) buttons.add(new ModuleButton(m));
        heightAnim.set(contentHeight());
    }

    private int contentHeight() {
        if (buttons.isEmpty()) return EMPTY_HEIGHT;
        int h = 0;
        for (ModuleButton b : buttons) h += b.getHeight();
        return h;
    }

    boolean inContent(double mx, double my) {
        return Render2D.hovered(mx, my, x, drawY + HEADER, WIDTH, visibleHeight);
    }

    private boolean inHeader(double mx, double my) {
        return Render2D.hovered(mx, my, x, drawY, WIDTH, HEADER);
    }

    void render(DrawContext ctx, int mx, int my, int yOffset, ClickGui gui) {
        if (dragging) {
            x = Math.max(0, Math.min(gui.width - WIDTH, mx - dragX));
            y = Math.max(0, Math.min(gui.height - HEADER, my - dragY));
        }
        drawY = y + yOffset;
        int h = Math.round(heightAnim.update(open ? contentHeight() : 0f));
        visibleHeight = h;
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        Render2D.shadow(ctx, x, drawY, WIDTH, HEADER + h + 1, 5);

        // Naglowek
        float hh = headerHover.update(gui.isInteractive() && inHeader(mx, my) ? 1f : 0f);
        Render2D.rect(ctx, x, drawY, WIDTH, HEADER, ColorUtil.lerp(Theme.BACKGROUND, 0xF0181824, hh));
        Render2D.horizontalGradient(ctx, x, drawY, WIDTH, 2, Theme.accent(x * 4L), Theme.accent(x * 4L + 700));
        ctx.drawText(tr, category.icon, x + 6, drawY + 6, Theme.accent(x * 4L), true);
        ctx.drawText(tr, category.displayName, x + 18, drawY + 6, Theme.TEXT, true);
        String count = String.valueOf(buttons.size());
        ctx.drawText(tr, count, x + WIDTH - 6 - tr.getWidth(count), drawY + 6, Theme.TEXT_DIM, false);

        // Moduly
        if (h > 0) {
            ctx.enableScissor(x, drawY + HEADER, x + WIDTH, drawY + HEADER + h);
            int cy = drawY + HEADER;
            if (buttons.isEmpty()) {
                Render2D.rect(ctx, x, cy, WIDTH, EMPTY_HEIGHT, Theme.MODULE);
                ctx.drawCenteredTextWithShadow(tr, "Wkrotce...", x + WIDTH / 2, cy + 4, Theme.TEXT_DIM);
            }
            for (ModuleButton b : buttons) {
                b.render(ctx, x, cy, mx, my, this, gui);
                cy += b.getHeight();
            }
            ctx.disableScissor();
        }
        Render2D.horizontalGradient(ctx, x, drawY + HEADER + h, WIDTH, 1, Theme.accent(x * 4L + 700), Theme.accent(x * 4L));
    }

    boolean isOver(double mx, double my) {
        return inHeader(mx, my) || inContent(mx, my);
    }

    boolean mouseClicked(double mx, double my, int button) {
        if (inHeader(mx, my)) {
            if (button == 0) {
                dragging = true;
                dragX = (int) mx - x;
                dragY = (int) my - y;
            } else if (button == 1) {
                open = !open;
            }
            return true;
        }
        if (open && inContent(mx, my)) {
            for (ModuleButton b : buttons) if (b.mouseClicked(mx, my, button)) return true;
            return true;
        }
        return false;
    }

    void mouseReleased(double mx, double my, int button) {
        dragging = false;
        for (ModuleButton b : buttons) b.mouseReleased(mx, my, button);
    }

    boolean mouseScrolled(double mx, double my, double amount) {
        if (!open || !inContent(mx, my)) return false;
        for (ModuleButton b : buttons) if (b.mouseScrolled(mx, my, amount)) return true;
        return false;
    }

    boolean keyPressed(int key) {
        for (ModuleButton b : buttons) if (b.keyPressed(key)) return true;
        return false;
    }

    boolean isListening() {
        for (ModuleButton b : buttons) if (b.isListening()) return true;
        return false;
    }

    JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("x", x);
        o.addProperty("y", y);
        o.addProperty("open", open);
        return o;
    }

    void fromJson(JsonObject o) {
        if (o.has("x")) x = o.get("x").getAsInt();
        if (o.has("y")) y = o.get("y").getAsInt();
        if (o.has("open")) open = o.get("open").getAsBoolean();
        heightAnim.set(open ? contentHeight() : 0);
    }
}
