package dev.nova.client.gui;

import dev.nova.client.NovaClient;
import dev.nova.client.module.Category;
import dev.nova.client.module.impl.ClickGuiModule;
import dev.nova.client.util.Animation;
import dev.nova.client.util.ColorUtil;
import dev.nova.client.util.Render2D;
import dev.nova.client.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {
    /** Global fade (0..1) used by every element so the whole GUI fades in/out together. */
    public static double fade = 1;

    private final List<Panel> panels = new ArrayList<>();
    private final Animation openAnim = new Animation(9, 0);
    private final Animation searchAnim = new Animation(14, 0);
    private boolean closing;
    private String query = "";
    private String tooltip;
    private int lastMx, lastMy;

    public ClickGuiScreen() {
        super(Text.literal("Nova ClickGUI"));
        openAnim.setTarget(1);
        for (Category c : Category.values()) panels.add(new Panel(c, NovaClient.get().panelState(c)));
    }

    @Override
    protected void init() {
        // Lay out panels that have no saved position, wrapping rows to fit the screen.
        int step = Panel.WIDTH + 10;
        int cols = Math.max(1, Math.min(panels.size(), (width - 20) / step));
        int startX = (width - (cols * step - 10)) / 2;
        for (int i = 0; i < panels.size(); i++) {
            PanelState s = panels.get(i).state;
            if (!s.placed) {
                s.x = startX + (i % cols) * step;
                s.y = 34;
                s.anchor = i >= cols ? panels.get(i - cols).category : null;
                s.placed = true;
            }
            // Keep panels reachable after a resolution / GUI scale change.
            s.x = Math.max(0, Math.min(s.x, width - Panel.WIDTH));
            s.y = Math.max(0, Math.min(s.y, height - Panel.HEADER));
        }
    }

    Panel panel(Category c) {
        for (Panel p : panels) if (p.category == c) return p;
        return null;
    }

    public boolean canHover() { return !closing; }
    public void setTooltip(String text) { tooltip = text; }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Drawn manually in render() to avoid the vanilla blur.
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        lastMx = mx; lastMy = my;
        double t = openAnim.get();
        if (closing && t < 0.03) { super.close(); return; }
        fade = Math.min(1, t * 1.2);
        tooltip = null;

        ClickGuiModule gui = NovaClient.get().modules.get(ClickGuiModule.class);
        int dim = (int) (gui.dim.get() / 100.0 * 255 * t);
        ctx.fill(0, 0, width, height, dim << 24);
        // Accent glow rising from the bottom of the screen.
        ctx.fillGradient(0, height - 120, width, height, 0, ColorUtil.alpha(Theme.accent(), 0.18 * t));

        double offset = (1 - Animation.easeOutBack(Math.min(1, t))) * 28;
        for (Panel p : panels) p.render(ctx, this, mx, my, offset, query);

        renderSearch(ctx, t);
        renderWatermark(ctx, t);

        if (tooltip != null && gui.descriptions.get() && !closing) renderTooltip(ctx, tooltip, mx, my);
    }

    private void renderWatermark(DrawContext ctx, double t) {
        int a = (int) (t * 255);
        if (a < 5) return;
        String name = "Nova", ver = " v" + NovaClient.VERSION;
        int x = 8, y = 8;
        int w = textRenderer.getWidth(name + ver) + 12;
        Render2D.roundRect(ctx, x, y, w, 16, 4, ColorUtil.alpha(Theme.BACKGROUND, t));
        Render2D.accentLine(ctx, x + 3, y + 15, w - 6, 1, t);
        int cx = x + 6;
        for (int i = 0; i < name.length(); i++) {
            String ch = String.valueOf(name.charAt(i));
            ctx.drawText(textRenderer, ch, cx, y + 4, ColorUtil.alpha(Theme.accent(i * 25), t), false);
            cx += textRenderer.getWidth(ch);
        }
        ctx.drawText(textRenderer, ver, cx, y + 4, ColorUtil.alpha(Theme.TEXT_DIM, t), false);
    }

    private void renderSearch(DrawContext ctx, double t) {
        double focus = searchAnim.setTarget(query.isEmpty() ? 0 : 1).get();
        int w = (int) (150 + 40 * focus), h = 18;
        double x = (width - w) / 2.0, y = height - 30 + (1 - t) * 20;
        Render2D.shadow(ctx, x, y, w, h, 6, t);
        Render2D.roundRect(ctx, x, y, w, h, 5, ColorUtil.alpha(Theme.BACKGROUND, t));
        Render2D.roundRect(ctx, x + 2, y + h - 2, (w - 4) * focus, 1, 0, ColorUtil.alpha(Theme.accent(), t));
        String shown = query.isEmpty() ? "Type to search..." : query + (System.currentTimeMillis() / 500 % 2 == 0 ? "_" : "");
        int color = query.isEmpty() ? Theme.TEXT_DIM : Theme.TEXT;
        ctx.drawText(textRenderer, "⌕", (int) x + 6, (int) y + 5, ColorUtil.alpha(Theme.accent(), t), false);
        ctx.drawText(textRenderer, shown, (int) x + 18, (int) y + 5, ColorUtil.alpha(color, t), false);
    }

    private void renderTooltip(DrawContext ctx, String text, int mx, int my) {
        int w = textRenderer.getWidth(text) + 10;
        int x = Math.min(mx + 10, width - w - 4), y = my - 16;
        Render2D.roundRect(ctx, x, y, w, 14, 3, 0xF00E0E14);
        Render2D.outline(ctx, x, y, w, 14, ColorUtil.alpha(Theme.accent(), 0.5));
        ctx.drawText(textRenderer, text, x + 5, y + 3, Theme.TEXT, false);
    }

    private Panel top(double mx, double my) {
        for (int i = panels.size() - 1; i >= 0; i--) if (panels.get(i).isOver(mx, my)) return panels.get(i);
        return null;
    }

    private boolean anyListening() {
        for (Panel p : panels) if (p.isListening()) return true;
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (closing) return false;
        for (Panel p : panels) if (p.isListening()) return p.mouseClicked(mx, my, button);
        Panel p = top(mx, my);
        if (p != null) {
            // Bring the clicked panel to the front.
            panels.remove(p);
            panels.add(p);
            return p.mouseClicked(mx, my, button);
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        for (Panel p : panels) p.mouseReleased(mx, my, button);
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        Panel p = top(mx, my);
        if (p != null && p.mouseScrolled(mx, my, vertical)) return true;
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (closing) return true;
        if (anyListening()) {
            for (Panel p : panels) if (p.keyPressed(key)) return true;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (!query.isEmpty()) query = "";
            else close();
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
            query = (mods & GLFW.GLFW_MOD_CONTROL) != 0 ? "" : query.substring(0, query.length() - 1);
            return true;
        }
        if (key == NovaClient.get().modules.get(ClickGuiModule.class).bind.get() && query.isEmpty()) {
            close();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (closing || anyListening()) return false;
        if (chr >= 32 && query.length() < 24) { query += chr; return true; }
        return false;
    }

    @Override
    public void close() {
        if (closing) return;
        closing = true;
        openAnim.setTarget(0);
        NovaClient.get().config.save();
    }

    @Override
    public boolean shouldPause() { return false; }
}
