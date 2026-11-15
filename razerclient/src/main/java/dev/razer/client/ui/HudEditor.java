package dev.razer.client.ui;

import dev.razer.client.RazerClient;
import dev.razer.client.hud.Hud;
import dev.razer.client.hud.HudElement;
import dev.razer.client.lang.Lang;
import dev.razer.client.render.Fonts;
import dev.razer.client.render.Fonts.Font;
import dev.razer.client.render.R2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Edytor HUD: przeciąganie elementów, przyciąganie do krawędzi i środka ekranu. */
public class HudEditor extends Screen {
    private static final float SNAP = 5;
    private final RazerClient rc = RazerClient.get();
    private final Hud hud = rc.hud;
    private final long openTime = System.currentTimeMillis();
    private HudElement drag;
    private float grabX, grabY;
    private boolean snapX, snapY;
    private float snapLineX, snapLineY;
    private float doneX, doneY, resetX, toolbarW = 260, toolbarH = 26;

    public HudEditor() { super(Text.literal("HUD Editor")); }

    @Override public boolean shouldPause() { return false; }

    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float p = Anim.easeOutCubic((System.currentTimeMillis() - openTime) / (300f / Anim.globalSpeed));
        R2D.alpha = 1;
        R2D.rect(ctx, 0, 0, width, height, R2D.withAlpha(0xFF000000, 0.35f * p));
        // siatka
        R2D.alpha = p;
        int grid = 20;
        for (int gx = grid; gx < width; gx += grid) R2D.rect(ctx, gx, 0, 0.5f, height, R2D.withAlpha(Theme.text(), 0.05f));
        for (int gy = grid; gy < height; gy += grid) R2D.rect(ctx, 0, gy, width, 0.5f, R2D.withAlpha(Theme.text(), 0.05f));
        R2D.rect(ctx, width / 2f, 0, 0.5f, height, R2D.withAlpha(Theme.text(), 0.12f));
        R2D.rect(ctx, 0, height / 2f, width, 0.5f, R2D.withAlpha(Theme.text(), 0.12f));

        if (drag != null) {
            float nx = mouseX - grabX, ny = mouseY - grabY;
            snapX = snapY = false;
            float[][] xs = {{SNAP + 1, SNAP + 1}, {width / 2f - drag.w / 2f, width / 2f}, {width - drag.w - SNAP - 1, width - SNAP - 1}};
            for (float[] c : xs) if (Math.abs(nx - c[0]) < SNAP) { nx = c[0]; snapX = true; snapLineX = c[1]; }
            float[][] ys = {{SNAP + 1, SNAP + 1}, {height / 2f - drag.h / 2f, height / 2f}, {height - drag.h - SNAP - 1, height - SNAP - 1}};
            for (float[] c : ys) if (Math.abs(ny - c[0]) < SNAP) { ny = c[0]; snapY = true; snapLineY = c[1]; }
            drag.setPosition(nx, ny, width, height);
            if (snapX) R2D.rect(ctx, snapLineX - 0.5f, 0, 1, height, Theme.accent());
            if (snapY) R2D.rect(ctx, 0, snapLineY - 0.5f, width, 1, Theme.accent());
        }

        // elementy
        HudElement hovered = null;
        for (HudElement e : hud.elements) {
            if (!e.enabled.get()) continue;
            float[] pos = e.position(width, height, hud);
            R2D.alpha = 1;
            e.render(ctx, pos[0], pos[1], true);
            e.lastX = pos[0]; e.lastY = pos[1];
            boolean hov = drag == e || (drag == null && inside(mouseX, mouseY, pos[0], pos[1], e.w, e.h));
            if (hov) hovered = e;
            R2D.alpha = p;
            R2D.roundOutline(ctx, pos[0] - 2, pos[1] - 2, e.w + 4, e.h + 4, Hud.R + 2, hov ? 1f : 0.6f,
                    hov ? Theme.accent() : R2D.withAlpha(Theme.text(), 0.35f));
        }
        if (hovered != null) {
            String n = hovered.getName();
            float tw = Fonts.width(n, Font.BOLD, 0.62f) + 14;
            float ty = hovered.lastY - 14 < 2 ? hovered.lastY + hovered.h + 4 : hovered.lastY - 14;
            R2D.roundRect(ctx, hovered.lastX - 2, ty, tw, 10, 3, Theme.accent());
            R2D.icon(ctx, hovered.icon, hovered.lastX + 1, ty + 2, 6, Theme.accentText());
            Fonts.drawV(ctx, n, hovered.lastX + 9, ty + 5.3f, Theme.accentText(), Font.BOLD, 0.62f);
        }

        // pasek narzędzi
        R2D.alpha = p;
        float tx = (width - toolbarW) / 2f, ty = height - toolbarH - 34 + (1 - p) * 20;
        R2D.shadow(ctx, tx, ty, toolbarW, toolbarH, 10, Theme.shadow());
        R2D.roundRect(ctx, tx, ty, toolbarW, toolbarH, 8, Theme.bg());
        R2D.roundOutline(ctx, tx, ty, toolbarW, toolbarH, 8, 0.5f, Theme.stroke());
        R2D.icon(ctx, "move", tx + 9, ty + toolbarH / 2f - 4, 8, Theme.accent());
        Fonts.drawV(ctx, Lang.get("editor.hint"), tx + 22, ty + toolbarH / 2f + 0.5f, Theme.textDim(), Font.SEMIBOLD, 0.62f);
        doneX = tx + toolbarW - 52; doneY = ty + 5;
        resetX = doneX - 24;
        boolean hr = inside(mouseX, mouseY, resetX, doneY, 20, 16);
        R2D.roundRect(ctx, resetX, doneY, 20, 16, 5, hr ? Theme.cardHover() : Theme.card());
        R2D.icon(ctx, "reset", resetX + 6, doneY + 4, 8, Theme.text());
        boolean hd = inside(mouseX, mouseY, doneX, doneY, 46, 16);
        R2D.roundRect(ctx, doneX, doneY, 46, 16, 5, hd ? R2D.blend(Theme.accent(), Theme.textDim(), 0.25f) : Theme.accent());
        Fonts.drawCentered(ctx, Lang.get("editor.done"), doneX + 23, doneY + 8 - Fonts.height(0.66f) / 2f + 0.5f, Theme.accentText(), Font.BOLD, 0.66f);
        R2D.alpha = 1;
    }

    private static boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (inside(mx, my, doneX, doneY, 46, 16)) { close(); return true; }
        if (inside(mx, my, resetX, doneY, 20, 16)) {
            for (HudElement e : hud.elements) e.resetPosition();
            rc.config.saveClient();
            return true;
        }
        for (int i = hud.elements.size() - 1; i >= 0; i--) {
            HudElement e = hud.elements.get(i);
            if (!e.enabled.get() || !inside(mx, my, e.lastX, e.lastY, e.w, e.h)) continue;
            if (button == 0) { drag = e; grabX = (float) mx - e.lastX; grabY = (float) my - e.lastY; }
            else if (button == 1) { e.enabled.set(false); rc.config.saveClient(); }
            else if (button == 2) { e.resetPosition(); rc.config.saveClient(); }
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (drag != null) rc.config.saveClient();
        drag = null;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        rc.config.saveClient();
        if (client != null) client.setScreen(new ClickGui());
    }
}
