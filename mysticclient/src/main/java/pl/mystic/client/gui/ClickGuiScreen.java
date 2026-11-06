package pl.mystic.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import pl.mystic.client.MysticClient;
import pl.mystic.client.config.ConfigManager;
import pl.mystic.client.module.Category;
import pl.mystic.client.module.Module;

import java.util.ArrayList;
import java.util.List;

/** LPM = włącz/wyłącz, PPM = zmień klawisz (Delete/Backspace = brak), przeciągaj nagłówki paneli. */
public class ClickGuiScreen extends Screen {
    private static final int W = 110, HEADER = 16, ROW = 14;
    private static final List<Panel> panels = new ArrayList<>();

    private Panel dragging;
    private int dragDx, dragDy;
    private Module binding;

    private static class Panel {
        final Category category;
        int x, y;
        boolean open = true;
        Panel(Category c, int x, int y) { category = c; this.x = x; this.y = y; }
        List<Module> modules() { return MysticClient.modules.byCategory(category); }
        int height() { return HEADER + (open ? modules().size() * ROW : 0); }
    }

    public ClickGuiScreen() {
        super(Text.literal("MysticClient"));
        if (panels.isEmpty()) {
            int x = 10;
            for (Category c : Category.values()) { panels.add(new Panel(c, x, 24)); x += W + 10; }
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        var tr = textRenderer;

        for (Panel p : panels) {
            ctx.fill(p.x, p.y, p.x + W, p.y + p.height(), 0xC0101018);
            ctx.fill(p.x, p.y, p.x + W, p.y + HEADER, Hud.color(p.x));
            ctx.drawTextWithShadow(tr, p.category.title, p.x + 5, p.y + 4, 0xFFFFFFFF);
            ctx.drawTextWithShadow(tr, p.open ? "-" : "+", p.x + W - 10, p.y + 4, 0xFFFFFFFF);
            if (!p.open) continue;

            int y = p.y + HEADER;
            for (Module m : p.modules()) {
                boolean hover = inside(mouseX, mouseY, p.x, y, W, ROW);
                if (hover) ctx.fill(p.x, y, p.x + W, y + ROW, 0x30FFFFFF);
                if (m.isEnabled()) ctx.fill(p.x, y, p.x + 2, y + ROW, Hud.color(y));
                int c = m.isEnabled() ? Hud.color(y) : 0xFFAAAAAA;
                ctx.drawTextWithShadow(tr, m.getName(), p.x + 6, y + 3, c);

                String key = m == binding ? "..." : keyName(m.getKey());
                ctx.drawTextWithShadow(tr, key, p.x + W - 4 - tr.getWidth(key), y + 3, 0xFF777777);
                if (hover) ctx.drawTooltip(tr, Text.literal(m.getDescription()), mouseX, mouseY + 12);
                y += ROW;
            }
        }

        String help = binding != null
                ? "Wciśnij klawisz dla " + binding.getName() + " (Delete = brak, Esc = anuluj)"
                : "LPM: włącz/wyłącz  •  PPM: ustaw klawisz  •  przeciągaj panele";
        ctx.drawCenteredTextWithShadow(tr, help, width / 2, height - 14, 0xFFBBBBBB);
    }

    private static String keyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return "";
        return InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString().toUpperCase();
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            if (inside(mx, my, p.x, p.y, W, HEADER)) {
                if (button == 0) { dragging = p; dragDx = (int) mx - p.x; dragDy = (int) my - p.y; }
                else if (button == 1) p.open = !p.open;
                return true;
            }
            if (!p.open) continue;
            int y = p.y + HEADER;
            for (Module m : p.modules()) {
                if (inside(mx, my, p.x, y, W, ROW)) {
                    if (button == 0) m.toggle();
                    else if (button == 1) binding = m;
                    return true;
                }
                y += ROW;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) { dragging.x = (int) mx - dragDx; dragging.y = (int) my - dragDy; return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding != null) {
            if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) binding.setKey(GLFW.GLFW_KEY_UNKNOWN);
            else if (keyCode != GLFW.GLFW_KEY_ESCAPE) binding.setKey(keyCode);
            binding = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() { ConfigManager.save(); }

    @Override
    public boolean shouldPause() { return false; }
}
