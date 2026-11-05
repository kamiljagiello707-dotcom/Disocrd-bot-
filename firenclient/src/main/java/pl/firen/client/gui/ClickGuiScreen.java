package pl.firen.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import pl.firen.client.FirenClient;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.module.modules.ClickGui;
import pl.firen.client.setting.NumberSetting;
import pl.firen.client.util.Anim;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class ClickGuiScreen extends Screen {
    private static final List<Panel> PANELS = new ArrayList<>();
    private static String query = "";

    private record Hit(int x1, int y1, int x2, int y2, IntConsumer action) {}

    private final List<Hit> hits = new ArrayList<>();
    private final long openedAt = System.nanoTime();
    private long lastFrame = System.nanoTime();

    private Panel draggingPanel;
    private int dragOffX, dragOffY;
    private NumberSetting draggingSlider;
    private Module binding;
    private String tooltip;

    /** Własna skala GUI: zawsze ostre piksele i miejsce na wszystkie panele. */
    private float s = 1f;
    private int vw, vh;

    public ClickGuiScreen() {
        super(Text.literal("FirenClient"));
    }

    public static List<Panel> panels() {
        if (PANELS.isEmpty()) {
            for (Category c : Category.values()) PANELS.add(new Panel(c));
        }
        return PANELS;
    }

    @Override
    protected void init() {
        computeScale();
        layoutUnplaced();
    }

    private void computeScale() {
        double factor = client.getWindow().getScaleFactor();
        int windowW = client.getWindow().getFramebufferWidth();
        int target = windowW >= 1280 ? 2 : 1;
        s = (float) (target / factor);
        vw = Math.round(width / s);
        vh = Math.round(height / s);
    }

    /** Scissor w wirtualnych współrzędnych GUI. */
    public void scissor(DrawContext ctx, int x1, int y1, int x2, int y2) {
        ctx.enableScissor((int) Math.floor(x1 * s), (int) Math.floor(y1 * s), (int) Math.ceil(x2 * s), (int) Math.ceil(y2 * s));
    }

    /** Rozkłada panele, które jeszcze nie mają zapisanej pozycji. */
    private void layoutUnplaced() {
        List<Panel> list = panels();
        int gap = 8, step = Panel.WIDTH + gap;
        int cols = Math.max(1, Math.min(list.size(), (vw - 12 + gap) / step));
        int startX = (vw - (cols * step - gap)) / 2;
        for (int i = 0; i < list.size(); i++) {
            Panel p = list.get(i);
            if (p.positioned) continue;
            p.x = startX + (i % cols) * step;
            p.y = 58 + (i / cols) * 170;
            p.positioned = true;
        }
    }

    // --- API dla paneli ---
    public String query() { return query; }
    public Module bindingModule() { return binding; }
    public void hit(int x1, int y1, int x2, int y2, IntConsumer action) { hits.add(new Hit(x1, y1, x2, y2, action)); }
    public void setTooltip(String text) { tooltip = text; }
    public boolean isDragging(NumberSetting s) { return draggingSlider == s; }
    public void startSliderDrag(NumberSetting s) { draggingSlider = s; }
    public void startBinding(Module m) { binding = binding == m ? null : m; }

    public void startPanelDrag(Panel p, int mx, int my) {
        draggingPanel = p;
        dragOffX = mx - p.x;
        dragOffY = my - p.y;
        PANELS.remove(p);
        PANELS.add(p); // na wierzch
    }

    public static String keyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return "Brak";
        return InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString().toUpperCase();
    }

    // --- Render ---
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float open = Anim.easeOutCubic(openProgress());
        boolean dark = FirenClient.modules().get(ClickGui.class).blur.get();
        ctx.fill(0, 0, width, height, Anim.withAlpha(Theme.OVERLAY, open * (dark ? 1f : 0.35f)));
    }

    private float openProgress() {
        return Math.min(1f, (System.nanoTime() - openedAt) / 220_000_000f);
    }

    @Override
    public void render(DrawContext ctx, int rawX, int rawY, float delta) {
        int mx = (int) (rawX / s), my = (int) (rawY / s);
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;

        renderBackground(ctx, rawX, rawY, delta);
        hits.clear();
        tooltip = null;

        if (draggingPanel != null) {
            draggingPanel.x = mx - dragOffX;
            draggingPanel.y = my - dragOffY;
        }

        float open = Anim.easeOutCubic(openProgress());
        var matrices = ctx.getMatrices();
        matrices.push();
        matrices.scale(s, s, 1f);
        matrices.push();
        matrices.translate(0, (1f - open) * -14f, 0);

        renderLogo(ctx);
        for (Panel p : panels()) p.render(ctx, this, mx, my, dt);

        matrices.pop();

        renderSearch(ctx);
        if (tooltip != null && draggingPanel == null) {
            int tw = textRenderer.getWidth(tooltip);
            int tx = (vw - tw) / 2, ty = vh - 46;
            Draw.round(ctx, tx - 6, ty - 4, tx + tw + 6, ty + 12, Theme.BORDER);
            Draw.round(ctx, tx - 5, ty - 3, tx + tw + 5, ty + 11, Theme.HEADER);
            ctx.drawText(textRenderer, tooltip, tx, ty, Theme.TEXT, false);
        }
        matrices.pop();
    }

    private void renderLogo(DrawContext ctx) {
        var m = ctx.getMatrices();
        Text logo = Text.literal("Firen").formatted(Formatting.BOLD)
                .append(Text.literal("Client").formatted(Formatting.GRAY));
        int w = textRenderer.getWidth(logo);
        m.push();
        m.translate(vw / 2f, 12, 0);
        m.scale(2f, 2f, 1f);
        ctx.drawText(textRenderer, logo, -w / 2, 0, Theme.WHITE, false);
        m.pop();
        String sub = "v" + FirenClient.VERSION + "  ·  LPM: włącz  ·  PPM: ustawienia  ·  ŚPM: bind";
        ctx.drawText(textRenderer, sub, (vw - textRenderer.getWidth(sub)) / 2, 36, Theme.TEXT_DIM, false);
    }

    private void renderSearch(DrawContext ctx) {
        int w = 180, h = 16;
        int x = (vw - w) / 2, y = vh - 26;
        Draw.round(ctx, x - 1, y - 1, x + w + 1, y + h + 1, query.isEmpty() ? Theme.BORDER : Theme.WHITE);
        Draw.round(ctx, x, y, x + w, y + h, Theme.HEADER);
        boolean caret = (System.currentTimeMillis() / 500) % 2 == 0;
        String text = query.isEmpty() ? "Szukaj modułu..." : query + (caret ? "_" : "");
        ctx.drawText(textRenderer, text, x + 7, y + 4, query.isEmpty() ? Theme.TEXT_DIM : Theme.WHITE, false);
    }

    // --- Wejście ---
    @Override
    public boolean mouseClicked(double rawX, double rawY, int button) {
        double mouseX = rawX / s, mouseY = rawY / s;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (mouseX >= h.x1 && mouseX < h.x2 && mouseY >= h.y1 && mouseY < h.y2) {
                h.action.accept(button);
                return true;
            }
        }
        return super.mouseClicked(rawX, rawY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingPanel = null;
        draggingSlider = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int dy = (int) Math.round(vertical * 14);
        for (Panel p : panels()) p.y += dy;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding != null) {
            boolean clear = keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE
                    || keyCode == GLFW.GLFW_KEY_BACKSPACE;
            binding.setKey(clear ? GLFW.GLFW_KEY_UNKNOWN : keyCode);
            binding = null;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
            query = query.substring(0, query.length() - 1);
            return true;
        }
        if (keyCode == FirenClient.modules().get(ClickGui.class).getKey()) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (binding == null && query.length() < 24 && (Character.isLetterOrDigit(chr) || chr == ' ')) {
            query += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void removed() {
        FirenClient.config().save();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
