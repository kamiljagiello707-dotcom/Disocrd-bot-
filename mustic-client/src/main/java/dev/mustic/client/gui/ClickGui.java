package dev.mustic.client.gui;

import com.google.gson.JsonObject;
import dev.mustic.client.MusticClient;
import dev.mustic.client.config.ConfigManager;
import dev.mustic.client.module.Category;
import dev.mustic.client.module.Module;
import dev.mustic.client.module.ModuleManager;
import dev.mustic.client.module.modules.ClickGuiModule;
import dev.mustic.client.util.Anim;
import dev.mustic.client.util.ColorUtil;
import dev.mustic.client.util.Render2D;
import dev.mustic.client.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClickGui extends Screen {
    private static ClickGui instance;

    private final List<Panel> panels = new ArrayList<>();
    private final Anim openAnim = new Anim(10f, 0f);
    private boolean closing;
    private String search = "";
    private boolean swallowNextChar;
    private Module hovered;

    public static ClickGui get() {
        if (instance == null) instance = new ClickGui();
        return instance;
    }

    public static boolean exists() {
        return instance != null;
    }

    private ClickGui() {
        super(Text.literal(MusticClient.NAME));
        int x = 12;
        for (Category c : Category.values()) {
            Panel p = new Panel(c, x, 34);
            if (ConfigManager.panels.has(c.name())) p.fromJson(ConfigManager.panels.getAsJsonObject(c.name()));
            panels.add(p);
            x += Panel.WIDTH + 8;
        }
    }

    public JsonObject savePanels() {
        JsonObject o = new JsonObject();
        for (Panel p : panels) o.add(p.category.name(), p.toJson());
        return o;
    }

    @Override
    protected void init() {
        if (!closing && openAnim.get() > 0.99f) return; // resize okna - bez ponownej animacji
        closing = false;
        openAnim.set(0f);
    }

    private ClickGuiModule settings() {
        return ModuleManager.get(ClickGuiModule.class);
    }

    boolean isInteractive() {
        return !closing && openAnim.get() > 0.9f;
    }

    void setHovered(Module m) { hovered = m; }
    String getSearch() { return search; }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float o = openAnim.get();
        if (client != null && client.world == null) renderPanoramaBackground(ctx, delta);
        if (settings().blur.get() && o > 0.3f) applyBlur();
        int a = (int) (255 * settings().dim.get() * o);
        ctx.fillGradient(0, 0, width, height, ColorUtil.withAlpha(0x08080C, a), ColorUtil.withAlpha(0x1A0A24, a));
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float o = openAnim.update(closing ? 0f : 1f);
        if (closing && o < 0.03f) {
            closing = false;
            if (client != null) client.setScreen(null);
            return;
        }
        super.render(ctx, mx, my, delta); // tlo (blur + przyciemnienie)
        hovered = null;
        float ease = 1f - (float) Math.pow(1f - o, 3);

        // Tytul
        ctx.getMatrices().push();
        ctx.getMatrices().translate(width / 2f, 8 - (1 - ease) * 20, 0);
        ctx.getMatrices().scale(1.5f, 1.5f, 1f);
        String a = "Mustic", b = "Client";
        int tw = textRenderer.getWidth(a + b);
        int tx = -tw / 2;
        for (int i = 0; i < a.length(); i++) {
            String ch = String.valueOf(a.charAt(i));
            ctx.drawText(textRenderer, ch, tx, 0, Theme.accent(i * 120L), true);
            tx += textRenderer.getWidth(ch);
        }
        ctx.drawText(textRenderer, b, tx, 0, Theme.TEXT, true);
        ctx.getMatrices().pop();

        // Panele (wjazd z gory, z opoznieniem dla kazdego)
        for (int i = 0; i < panels.size(); i++) {
            int offset = Math.round((1f - ease) * -(60 + i * 25));
            panels.get(i).render(ctx, mx, my, offset, this);
        }

        renderSearch(ctx, ease);

        if (hovered != null && settings().descriptions.get() && isInteractive()) {
            renderTooltip(ctx, hovered.getDescription(), mx + 8, my + 8);
        }
    }

    private void renderSearch(DrawContext ctx, float ease) {
        int w = 180, h = 18;
        int x = width / 2 - w / 2;
        int y = height - 28 + Math.round((1f - ease) * 40);
        Render2D.shadow(ctx, x, y, w, h, 4);
        Render2D.rect(ctx, x, y, w, h, Theme.BACKGROUND);
        Render2D.horizontalGradient(ctx, x, y + h - 1, w, 1, Theme.accent(0), Theme.accent(700));
        ctx.drawText(textRenderer, "⌕", x + 6, y + 5, Theme.accent(), false);
        if (search.isEmpty()) {
            ctx.drawText(textRenderer, "Szukaj modulu...", x + 18, y + 5, Theme.TEXT_DIM, false);
        } else {
            ctx.drawText(textRenderer, search, x + 18, y + 5, Theme.TEXT, false);
            if ((System.currentTimeMillis() / 500) % 2 == 0)
                Render2D.rect(ctx, x + 19 + textRenderer.getWidth(search), y + 4, 1, 10, Theme.TEXT);
        }
        String hint = "LPM toggle • PPM ustawienia • SPM bind";
        ctx.drawText(textRenderer, hint, width / 2 - textRenderer.getWidth(hint) / 2, y - 11,
                ColorUtil.alpha(Theme.TEXT_DIM, 0.7f * ease), false);
    }

    private void renderTooltip(DrawContext ctx, String text, int x, int y) {
        int w = textRenderer.getWidth(text) + 10;
        if (x + w > width) x = width - w - 2;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 400);
        Render2D.shadow(ctx, x, y, w, 15, 3);
        Render2D.rect(ctx, x, y, w, 15, 0xF00C0C12);
        Render2D.rect(ctx, x, y, 1, 15, Theme.accent());
        ctx.drawText(textRenderer, text, x + 5, y + 4, Theme.TEXT, false);
        ctx.getMatrices().pop();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!isInteractive()) return false;
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            if (p.mouseClicked(mx, my, button)) {
                // przenies klikniety panel na wierzch
                panels.remove(i);
                panels.add(p);
                return true;
            }
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
        for (int i = panels.size() - 1; i >= 0; i--)
            if (panels.get(i).mouseScrolled(mx, my, vertical)) return true;
        int dy = (int) Math.signum(vertical) * 14;
        for (Panel p : panels) p.y += dy;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        for (Panel p : panels) {
            if (p.isListening() && p.keyPressed(key)) {
                swallowNextChar = true;
                return true;
            }
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && !search.isEmpty()) {
            search = "";
            return true;
        }
        if (key == settings().getKey() && search.isEmpty()) {
            close();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (swallowNextChar) {
            swallowNextChar = false;
            return true;
        }
        if ((Character.isLetterOrDigit(chr) || chr == ' ') && search.length() < 24) {
            search += chr;
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        if (closing) return;
        closing = true;
        ConfigManager.save();
    }

    @Override
    public void removed() {
        ConfigManager.save();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
