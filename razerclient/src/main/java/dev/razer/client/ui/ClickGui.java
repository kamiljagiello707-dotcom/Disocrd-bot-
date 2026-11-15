package dev.razer.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.razer.client.RazerClient;
import dev.razer.client.config.ClientSettings;
import dev.razer.client.hud.Keys;
import dev.razer.client.lang.Lang;
import dev.razer.client.module.Category;
import dev.razer.client.module.Module;
import dev.razer.client.render.Fonts;
import dev.razer.client.render.Fonts.Font;
import dev.razer.client.render.R2D;
import dev.razer.client.setting.BoolSetting;
import dev.razer.client.setting.ModeSetting;
import dev.razer.client.setting.NumberSetting;
import dev.razer.client.setting.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.IntConsumer;

/**
 * Nowoczesne biało-czarne ClickGUI RazerClient.
 * Działa w trybie "immediate": podczas renderu zapisywane są obszary kliknięć (Hit),
 * a mouseClicked sprawdza je w odwrotnej kolejności.
 */
public class ClickGui extends Screen {
    // wymiary okna (przed skalowaniem)
    private static final float W = 600, H = 400, SB = 158, PAD = 18;
    private static final long OPEN_MS = 320, CLOSE_MS = 200;

    private enum Page { CATEGORY, FAVORITES, FRIENDS, HUD, THEMES, CONFIG, PROFILE, SETTINGS }

    // nawigacja zapamiętana między otwarciami
    private static Page page = Page.CATEGORY;
    private static Category category = Category.COMBAT;
    private static float dragX = 0, dragY = 0;

    private final RazerClient rc = RazerClient.get();
    private final Map<String, Anim> anims = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private final List<Object[]> sliders = new ArrayList<>();
    private String friendInput = "";
    private boolean friendFocused;
    private final Anim scroll = new Anim(0, 16);
    private final Anim indicatorY = new Anim(-1, 16);
    private final Anim indicatorA = new Anim(0, 14);
    private float scrollTarget = 0, maxScroll = 0;
    private long openTime, closeTime, pageTime;
    private boolean closing;

    private String search = "";
    private boolean searchFocused;
    private Module binding;
    private NumberSetting dragging;
    private float dragTrackX, dragTrackW;
    private boolean draggingWindow;
    private float dragStartMx, dragStartMy, dragStartX, dragStartY;

    // transformacja okna
    private float x0, y0, scale = 1, centerX, centerY;
    private float mx, my;      // mysz w przestrzeni okna
    private float[] clip;      // aktualny obszar przycięcia (dla hover/hit)

    private record Hit(float x, float y, float w, float h, float[] clip, IntConsumer action) {}

    public ClickGui() {
        super(Text.literal("RazerClient"));
        openTime = System.currentTimeMillis();
        pageTime = openTime;
    }

    @Override public boolean shouldPause() { return false; }

    // ================================================================== render

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float speed = Anim.globalSpeed;
        float p = Anim.easeOutCubic((now - openTime) / (OPEN_MS / speed));
        if (closing) {
            float c = (now - closeTime) / (CLOSE_MS / speed);
            p = Math.min(p, 1 - Anim.easeInCubic(c));
            if (c >= 1) { R2D.alpha = 1; finishClose(); return; }
        }

        // tło
        if (rc.settings.blur.get() && client != null && client.world != null && p > 0.6f && !closing) applyBlur(delta);
        R2D.alpha = 1;
        R2D.rect(ctx, 0, 0, width, height, R2D.withAlpha(0xFF000000, 0.45f * p));

        // skala i pozycja
        ClientSettings s = rc.settings;
        float fit = Math.min(1f, Math.min((width - 24) / W, (height - 50) / H));
        float base = Math.min(fit, s.guiScale() * Math.min(1.25f, Math.max(0.85f, Math.min(width / 600f, height / 380f))));
        base = Math.min(base, fit);
        scale = base * (0.9f + 0.1f * p);
        centerX = width / 2f; centerY = height / 2f;
        x0 = (width - W) / 2f + dragX;
        y0 = (height - H) / 2f + dragY + (1 - p) * 14;
        mx = (mouseX - centerX) / scale + centerX;
        my = (mouseY - centerY) / scale + centerY;

        if (draggingWindow) {
            dragX = dragStartX + (mx - dragStartMx) * 1f;
            dragY = dragStartY + (my - dragStartMy) * 1f;
        }
        if (dragging != null) {
            dragging.set(dragging.min + (dragging.max - dragging.min) * Anim.clamp01((mx - dragTrackX) / dragTrackW));
            if (dragging == s.animSpeed) Anim.globalSpeed = s.animSpeed.get().floatValue();
        }

        hits.clear();
        sliders.clear();
        clip = null;
        R2D.alpha = p;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(centerX, centerY, 0);
        ctx.getMatrices().scale(scale, scale, 1);
        ctx.getMatrices().translate(-centerX, -centerY, 0);

        drawWindow(ctx);

        ctx.getMatrices().pop();
        R2D.alpha = 1;
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private static final float[][] PARTICLES = new float[46][];
    static {
        Random r = new Random(7);
        for (int i = 0; i < PARTICLES.length; i++)
            PARTICLES[i] = new float[]{r.nextFloat(), r.nextFloat(), (r.nextFloat() - 0.5f) * 0.012f, -0.004f - r.nextFloat() * 0.012f, 0.6f + r.nextFloat() * 1.6f, r.nextFloat() * 6.28f};
    }
    private long lastParticle = System.nanoTime();

    private void drawBackgroundFx(DrawContext ctx) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastParticle) / 1e9f);
        lastParticle = now;
        // poświata za oknem
        R2D.shadow(ctx, x0 + 40, y0 + 40, W - 80, H - 80, 120, R2D.withAlpha(Theme.accentWave(0), 0.06f));
        float t = (System.currentTimeMillis() % 100000) / 1000f;
        for (float[] p : PARTICLES) {
            p[0] += p[2] * dt; p[1] += p[3] * dt;
            if (p[1] < -0.02f) { p[1] = 1.02f; p[0] = (float) Math.random(); }
            if (p[0] < -0.02f) p[0] = 1.02f; else if (p[0] > 1.02f) p[0] = -0.02f;
            float px = p[0] * width, py = p[1] * height;
            // przestrzeń okna (przeciw skali GUI)
            px = (px - centerX) / scale + centerX; py = (py - centerY) / scale + centerY;
            float tw = 0.5f + 0.5f * (float) Math.sin(t * 1.3f + p[5]);
            R2D.circle(ctx, px, py, p[4], R2D.withAlpha(Theme.accentWave(p[5]), 0.12f + 0.25f * tw));
        }
    }

    private void drawWindow(DrawContext ctx) {
        if (rc.settings.particles.get()) drawBackgroundFx(ctx);
        // cień + tło
        R2D.shadow(ctx, x0, y0, W, H, 26, Theme.shadow());
        R2D.roundRect(ctx, x0, y0, W, H, 14, Theme.bg());
        // sidebar
        R2D.roundRect(ctx, x0, y0, SB + 14, H, 14, Theme.sidebar());
        R2D.rect(ctx, x0 + SB, y0, 14, H, Theme.bg());
        R2D.rect(ctx, x0 + SB, y0 + 12, 0.5f, H - 24, Theme.stroke());
        R2D.roundOutline(ctx, x0, y0, W, H, 14, 0.6f, Theme.stroke());

        drawSidebar(ctx);
        drawContent(ctx);
        drawFooter(ctx);
    }

    // ================================================================== sidebar

    private void drawSidebar(DrawContext ctx) {
        float x = x0, y = y0;
        // logo – przeciągnięcie okna
        R2D.icon(ctx, "logo", x + 16, y + 16, 24, Theme.accentWave(0));
        Fonts.draw(ctx, "RazerClient", x + 46, y + 17, Theme.text(), Font.BOLD, 1.12f);
        Fonts.draw(ctx, "v" + RazerClient.VERSION + "  •  Fabric 1.21.1", x + 46, y + 29, Theme.textFaint(), Font.REGULAR, 0.68f);
        hit(x, y, SB, 50, b -> {
            if (b == 0) { draggingWindow = true; dragStartMx = mx; dragStartMy = my; dragStartX = dragX; dragStartY = dragY; }
        });

        float iy = y + 62;
        Fonts.draw(ctx, Lang.get("section.modules"), x + 18, iy, Theme.textFaint(), Font.SEMIBOLD, 0.62f);
        iy += 10;

        // pozycje nawigacji
        float itemH = 20;
        float selY = -1;
        List<Runnable> drawLater = new ArrayList<>();
        List<float[]> itemPos = new ArrayList<>();
        int catCount = Category.values().length;
        Object[][] entries = new Object[catCount + 5][];
        int idx = 0;
        for (Category c : Category.values()) entries[idx++] = new Object[]{c.icon, c.getName(), Page.CATEGORY, c};
        entries[idx++] = new Object[]{"star", Lang.get("cat.favorites"), Page.FAVORITES, null};
        entries[idx++] = new Object[]{"users", Lang.get("page.friends"), Page.FRIENDS, null};
        entries[idx++] = new Object[]{"hud", Lang.get("page.hud"), Page.HUD, null};
        entries[idx++] = new Object[]{"themes", Lang.get("page.themes"), Page.THEMES, null};
        entries[idx] = new Object[]{"config", Lang.get("page.config"), Page.CONFIG, null};

        for (int i = 0; i < entries.length; i++) {
            if (i == catCount) {
                iy += 6;
                Fonts.draw(ctx, Lang.get("section.client"), x + 18, iy, Theme.textFaint(), Font.SEMIBOLD, 0.62f);
                iy += 10;
            }
            Object[] e = entries[i];
            Page pg = (Page) e[2];
            Category cat = (Category) e[3];
            boolean selected = page == pg && ((pg != Page.CATEGORY && pg != Page.FAVORITES) || ((pg == Page.FAVORITES || cat == category) && search.isEmpty()));
            if (selected) selY = iy;
            itemPos.add(new float[]{iy});
            float fy = iy;
            final int fi = i;
            drawLater.add(() -> {
                boolean hov = hovered(x + 10, fy, SB - 20, itemH);
                float ha = anim("nav" + fi, 18).animate(hov && !selected ? 1 : 0);
                R2D.roundRect(ctx, x + 10, fy, SB - 20, itemH, 7, R2D.withAlpha(Theme.cardHover(), ha));
                float near = 1 - Anim.clamp01(Math.abs(indicatorY.raw() - fy) / itemH);
                near *= indicatorA.raw();
                int fg = R2D.blend(R2D.blend(Theme.textDim(), Theme.text(), ha), Theme.accentText(), near);
                float off = anim("navoff" + fi, 16).animate(selected ? 4 : (hov ? 2 : 0));
                R2D.icon(ctx, (String) e[0], x + 20 + off, fy + 5.5f, 9, fg);
                Fonts.drawV(ctx, (String) e[1], x + 35 + off, fy + itemH / 2f + 0.5f, fg, selected ? Font.BOLD : Font.SEMIBOLD, 0.86f);
                if (pg == Page.CATEGORY || pg == Page.FAVORITES) {
                    long en = (cat != null ? rc.modules.byCategory(cat) : favorites()).stream().filter(Module::isEnabled).count();
                    if (en > 0) {
                        String t = String.valueOf(en);
                        float tw = Fonts.width(t, Font.BOLD, 0.65f) + 7;
                        R2D.roundRect(ctx, x + SB - 18 - tw, fy + 5, tw, 10, 5, R2D.blend(Theme.toggleOff(), Theme.accentText(), near));
                        Fonts.drawV(ctx, t, x + SB - 18 - tw + 3.5f, fy + 10, R2D.blend(Theme.textDim(), Theme.accent(), near), Font.BOLD, 0.65f);
                    }
                }
            });
            hit(x + 10, iy, SB - 20, itemH, b -> {
                if (b != 0) return;
                setPage(pg);
                if (cat != null) category = cat;
                if (pg == Page.CATEGORY || pg == Page.FAVORITES) { search = ""; searchFocused = false; }
            });
            iy += itemH + 1;
        }

        // wskaźnik zaznaczenia (płynnie przesuwana pigułka)
        if (selY >= 0 && indicatorY.raw() < 0) indicatorY.snap(selY);
        float ia = indicatorA.animate(selY >= 0 ? 1 : 0);
        float iyv = indicatorY.animate(selY >= 0 ? selY : indicatorY.target());
        R2D.roundRect(ctx, x + 10, iyv, SB - 20, itemH, 7, R2D.withAlpha(Theme.accent(), ia));
        for (Runnable r : drawLater) r.run();

        drawProfileCard(ctx, x + 10, y0 + H - 46, SB - 20, 36);
    }

    private void drawProfileCard(DrawContext ctx, float x, float y, float w, float h) {
        boolean hov = hovered(x, y, w, h);
        boolean active = page == Page.PROFILE || page == Page.SETTINGS;
        float ha = anim("profile", 16).animate(hov || active ? 1 : 0);
        R2D.roundRect(ctx, x, y, w, h, 9, R2D.blend(Theme.card(), Theme.cardHover(), ha));
        R2D.roundOutline(ctx, x, y, w, h, 9, 0.5f, R2D.blend(Theme.stroke(), Theme.textFaint(), active ? 1 : 0));
        drawHead(ctx, x + 7, y + 7, 22);
        R2D.circle(ctx, x + 7 + 21, y + 7 + 21, 3.2f, Theme.card());
        R2D.circle(ctx, x + 7 + 21, y + 7 + 21, 2.2f, Theme.accent());
        String name = client.getSession().getUsername();
        Fonts.draw(ctx, Fonts.trim(name, Font.BOLD, 0.86f, w - 52), x + 36, y + 9, Theme.text(), Font.BOLD, 0.86f);
        Fonts.draw(ctx, Lang.format("profile.modules", rc.modules.enabledCount()), x + 36, y + 20, Theme.textFaint(), Font.REGULAR, 0.62f);
        R2D.icon(ctx, "settings", x + w - 15, y + h / 2f - 4.5f, 9, R2D.blend(Theme.textFaint(), Theme.text(), ha));
        hit(x, y, w, h, b -> {
            if (b == 0) setPage(Page.PROFILE);
            else if (b == 1) setPage(Page.SETTINGS);
        });
    }

    private void drawHead(DrawContext ctx, float x, float y, int size) {
        ctx.draw();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, R2D.alpha);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        PlayerSkinDrawer.draw(ctx, client.getSkinProvider().getSkinTextures(client.getGameProfile()), 0, 0, size);
        ctx.getMatrices().pop();
        ctx.draw();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    // ================================================================== content

    private void drawContent(DrawContext ctx) {
        float cx = x0 + SB, cw = W - SB;
        long now = System.currentTimeMillis();
        float pa = Anim.easeOutCubic((now - pageTime) / (260f / Anim.globalSpeed));

        // nagłówek
        String title, sub;
        switch (page) {
            case THEMES -> { title = Lang.get("page.themes"); sub = Lang.get("themes.desc"); }
            case HUD -> { title = Lang.get("page.hud"); sub = Lang.get("hud.desc"); }
            case FRIENDS -> { title = Lang.get("page.friends"); sub = Lang.get("friends.desc"); }
            case CONFIG -> { title = Lang.get("page.config"); sub = Lang.get("config.desc"); }
            case PROFILE -> { title = Lang.get("page.profile"); sub = Lang.get("profile.desc"); }
            case SETTINGS -> { title = Lang.get("page.settings"); sub = Lang.get("settings.desc"); }
            default -> {
                List<Module> list = currentModules();
                title = !search.isEmpty() ? "„" + search + "”" : page == Page.FAVORITES ? Lang.get("cat.favorites") : category.getName();
                sub = Lang.format("modules.count", list.size(), list.stream().filter(Module::isEnabled).count()) + "  •  " + Lang.get("rmb.settings");
            }
        }
        float prev = R2D.alpha;
        R2D.alpha = prev * pa;
        Fonts.draw(ctx, title, cx + PAD + (1 - pa) * 8, y0 + 17, Theme.text(), Font.BOLD, 1.45f);
        Fonts.draw(ctx, Fonts.trim(sub, Font.REGULAR, 0.72f, cw - PAD * 2 - 150), cx + PAD + (1 - pa) * 8, y0 + 34, Theme.textDim(), Font.REGULAR, 0.72f);
        R2D.alpha = prev;

        // prawa strona nagłówka
        if (page == Page.CATEGORY || page == Page.FAVORITES) drawSearch(ctx, cx + cw - PAD - 168, y0 + 16, 140, 22);
        // przycisk zamknięcia
        {
            float bx = cx + cw - PAD - 22, byy = y0 + 16;
            boolean hv = hovered(bx, byy, 22, 22);
            float hca = anim("closebtn", 18).animate(hv ? 1 : 0);
            R2D.roundRect(ctx, bx, byy, 22, 22, 11, R2D.blend(Theme.card(), Theme.cardHover(), hca));
            R2D.roundOutline(ctx, bx, byy, 22, 22, 11, 0.5f, Theme.stroke());
            R2D.icon(ctx, "cross", bx + 7, byy + 7, 8, R2D.blend(Theme.textDim(), Theme.text(), hca));
            hit(bx, byy, 22, 22, b -> { if (b == 0) close(); });
        }
        if (page == Page.CONFIG) {
            float bw = 104;
            button(ctx, "newcfg", cx + cw - PAD - bw, y0 + 16, bw, 22, "plus", Lang.get("config.new"), true, () -> {
                String n = rc.config.newName();
                rc.config.save(n);
                rc.notifications.info("disk", Lang.get("config.created"), n);
            });
            float fw = 26;
            button(ctx, "openfolder", cx + cw - PAD - bw - 6 - fw, y0 + 16, fw, 22, "folder", null, false,
                    () -> Util.getOperatingSystem().open(rc.config.folder().toFile()));
        }

        R2D.rect(ctx, cx + PAD, y0 + 50, cw - PAD * 2, 0.5f, Theme.stroke());

        // obszar przewijany
        float vy = y0 + 51, vh = H - 51 - 6;
        float sc = scroll.animate(scrollTarget);
        R2D.scissor(ctx, cx, vy, cw, vh);
        float[] oldClip = clip;
        clip = new float[]{cx, vy, cw, vh};
        R2D.alpha = prev * pa;
        float top = vy + 10 - sc + (1 - pa) * 12;
        float contentH = switch (page) {
            case CATEGORY, FAVORITES -> drawModules(ctx, cx + PAD, top, cw - PAD * 2);
            case HUD -> drawHudPage(ctx, cx + PAD, top, cw - PAD * 2);
            case FRIENDS -> drawFriends(ctx, cx + PAD, top, cw - PAD * 2);
            case THEMES -> drawThemes(ctx, cx + PAD, top, cw - PAD * 2);
            case CONFIG -> drawConfigs(ctx, cx + PAD, top, cw - PAD * 2);
            case PROFILE -> drawProfile(ctx, cx + PAD, top, cw - PAD * 2);
            case SETTINGS -> drawSettings(ctx, cx + PAD, top, cw - PAD * 2);
        };
        R2D.alpha = prev;
        clip = oldClip;
        R2D.endScissor(ctx);

        maxScroll = Math.max(0, contentH + 20 - vh);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));

        // gradient na krawędziach i pasek przewijania
        if (maxScroll > 0) {
            float trackH = vh - 16;
            float thumbH = Math.max(24, trackH * vh / (contentH + 20));
            float ty = vy + 8 + (trackH - thumbH) * (sc / maxScroll);
            R2D.roundRect(ctx, cx + cw - 7, ty, 2.5f, thumbH, 1.25f, Theme.accentWave(0));
        }
    }

    private void drawSearch(DrawContext ctx, float x, float y, float w, float h) {
        boolean hov = hovered(x, y, w, h);
        float fa = anim("searchfocus", 16).animate(searchFocused ? 1 : 0);
        float ha = anim("searchhover", 16).animate(hov ? 1 : 0);
        R2D.roundRect(ctx, x, y, w, h, h / 2f, R2D.blend(R2D.blend(Theme.card(), Theme.cardHover(), ha * 0.7f), R2D.withAlpha(Theme.accent(), 0.22f), fa));
        R2D.roundOutline(ctx, x, y, w, h, 8, 0.6f, R2D.blend(Theme.stroke(), Theme.accent(), fa));
        R2D.icon(ctx, "search", x + 8, y + h / 2f - 4.5f, 9, R2D.blend(Theme.textFaint(), Theme.text(), fa));
        String shown = search.isEmpty() ? Lang.get("search") : search;
        int c = search.isEmpty() ? Theme.textFaint() : Theme.text();
        Fonts.drawV(ctx, Fonts.trim(shown, Font.REGULAR, 0.78f, w - 34), x + 22, y + h / 2f + 0.5f, c, Font.REGULAR, 0.78f);
        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            float tx = x + 22 + (search.isEmpty() ? 0 : Fonts.width(Fonts.trim(search, Font.REGULAR, 0.78f, w - 34), Font.REGULAR, 0.78f) + 1);
            R2D.rect(ctx, tx, y + 6, 0.7f, h - 12, Theme.text());
        }
        if (!search.isEmpty()) {
            float bx = x + w - 16;
            R2D.icon(ctx, "cross", bx, y + h / 2f - 3.5f, 7, hovered(bx - 3, y, 14, h) ? Theme.text() : Theme.textFaint());
            hit(bx - 3, y, 14, h, b -> { search = ""; scrollTarget = 0; });
        }
        hit(x, y, w, h, b -> searchFocused = true);
    }

    // ------------------------------------------------------------------ moduły

    private List<Module> favorites() {
        List<Module> out = new ArrayList<>();
        for (Module m : rc.modules.all()) if (m.favorite) out.add(m);
        return out;
    }

    private List<Module> currentModules() {
        if (search.isEmpty()) return page == Page.FAVORITES ? favorites() : rc.modules.byCategory(category);
        String q = search.toLowerCase(Locale.ROOT);
        List<Module> out = new ArrayList<>();
        for (Module m : rc.modules.all())
            if (m.getName().toLowerCase(Locale.ROOT).contains(q) || m.getDescription().toLowerCase(Locale.ROOT).contains(q)) out.add(m);
        return out;
    }

    private float drawModules(DrawContext ctx, float x, float y, float w) {
        List<Module> list = currentModules();
        if (list.isEmpty()) {
            boolean fav = page == Page.FAVORITES && search.isEmpty();
            R2D.icon(ctx, fav ? "star" : "search", x + w / 2f - 12, y + 50, 24, Theme.textFaint());
            Fonts.drawCentered(ctx, fav ? Lang.get("fav.empty") : Lang.pl() ? "Nic nie znaleziono" : "Nothing found", x + w / 2f, y + 82, Theme.textDim(), Font.SEMIBOLD, 0.9f);
            return 120;
        }
        float gap = 8, colW = (w - gap) / 2f;
        float[] colY = {y, y};
        for (int i = 0; i < list.size(); i++) {
            int col = i % 2;
            float h = drawModuleCard(ctx, list.get(i), x + col * (colW + gap), colY[col], colW);
            colY[col] += h + gap;
        }
        return Math.max(colY[0], colY[1]) - y;
    }

    private float settingsHeight(Module m) {
        float h = 8 + 20; // separator + keybind
        for (Setting<?> s : m.getSettings()) h += s instanceof NumberSetting ? 28 : 20;
        return h + 6;
    }

    private float drawModuleCard(DrawContext ctx, Module m, float x, float y, float w) {
        float baseH = 34;
        float ex = Anim.easeOutCubic(m.expandAnim.animate(m.expanded ? 1 : 0));
        float h = baseH + settingsHeight(m) * ex;
        boolean hov = hovered(x, y, w, baseH);
        float ha = m.hoverAnim.animate(hov ? 1 : 0);
        float on = m.toggleAnim.animate(m.isEnabled() ? 1 : 0);

        R2D.roundRect(ctx, x, y, w, h, 7, R2D.blend(Theme.card(), Theme.cardHover(), ha));
        R2D.roundOutline(ctx, x, y, w, h, 7, 0.5f, R2D.blend(Theme.stroke(), Theme.textFaint(), on * 0.5f));
        // pasek aktywności po lewej
        if (on > 0.01f) R2D.roundRect(ctx, x + 3, y + 10, 1.5f, (baseH - 20) * on, 0.75f, Theme.accent());

        // mała ikona + nazwa / opis
        int nameCol = R2D.blend(Theme.textDim(), Theme.text(), on);
        R2D.icon(ctx, m.getIcon(), x + 10, y + 8.5f, 8.5f, R2D.blend(Theme.textFaint(), Theme.accent(), on));
        float tx = x + 23;
        Fonts.draw(ctx, m.getName(), tx, y + 8, nameCol, Font.BOLD, 0.86f);
        float right = 60;
        float nx = tx + Fonts.width(m.getName(), Font.BOLD, 0.86f) + 4;
        if (binding == m) {
            String k = Lang.get("bind.listening");
            float kw = Fonts.width(k, Font.BOLD, 0.6f) + 8;
            R2D.roundRect(ctx, nx, y + 7.5f, kw, 8.5f, 4.25f, Theme.accentWave(0));
            Fonts.drawV(ctx, k, nx + 4, y + 11.8f, Theme.accentText(), Font.BOLD, 0.6f);
            nx += kw + 4;
        } else if (m.hasKey()) {
            String k = Keys.name(m.getKey());
            float kw = Fonts.width(k, Font.BOLD, 0.62f) + 7;
            R2D.roundRect(ctx, nx, y + 7.5f, kw, 8.5f, 2.5f, Theme.toggleOff());
            Fonts.drawV(ctx, k, nx + 3.5f, y + 11.8f, Theme.textDim(), Font.BOLD, 0.62f);
            nx += kw + 4;
        }
        String vals = summary(m);
        if (!vals.isEmpty()) {
            float maxW = x + w - right - nx;
            if (maxW > 12) Fonts.drawV(ctx, Fonts.trim(vals, Font.SEMIBOLD, 0.62f, maxW), nx + 1, y + 11.8f, Theme.accentWave(0), Font.SEMIBOLD, 0.62f);
        }
        Fonts.draw(ctx, Fonts.trim(m.getDescription(), Font.REGULAR, 0.66f, w - (tx - x) - right), tx, y + 19, Theme.textFaint(), Font.REGULAR, 0.66f);

        // przełącznik
        drawSwitch(ctx, x + w - 30, y + baseH / 2f - 5.5f, on);

        // strzałka rozwinięcia
        float rot = ex;
        ctx.getMatrices().push();
        float ax = x + w - 39, ay = y + baseH / 2f;
        ctx.getMatrices().translate(ax, ay, 0);
        ctx.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(180 + 90 * rot));
        R2D.icon(ctx, "arrow_left", -3.5f, -3.5f, 7, R2D.withAlpha(Theme.textFaint(), m.getSettings().isEmpty() ? 0.6f : 1f));
        ctx.getMatrices().pop();

        hit(x, y, w, baseH, b -> {
            if (b == 0) m.toggle();
            else if (b == 1) m.expanded = !m.expanded;
            else if (b == 2) binding = binding == m ? null : m;
        });
        // strzałka – rozwija opcje
        hit(ax - 6, ay - 7, 12, 14, b -> { if (b == 0 || b == 1) m.expanded = !m.expanded; });

        // gwiazdka ulubionych (widoczna po najechaniu lub gdy ulubiony)
        float fa = m.favAnim.animate(m.favorite ? 1 : 0);
        float sx = x + w - 52, sy0 = y + baseH / 2f - 3.5f;
        boolean sh = hovered(sx - 2, sy0 - 2, 11, 11);
        float vis = Math.max(fa, ha * 0.7f);
        if (vis > 0.02f) {
            float sc = 1 + (sh ? 0.15f : 0);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(sx + 3.5f, sy0 + 3.5f, 0);
            ctx.getMatrices().scale(sc, sc, 1);
            R2D.icon(ctx, "star", -3.5f, -3.5f, 7, R2D.withAlpha(R2D.blend(sh ? Theme.textDim() : Theme.textFaint(), Theme.accentWave(0), fa), vis));
            ctx.getMatrices().pop();
        }
        hit(sx - 2, sy0 - 2, 11, 11, b -> { if (b == 0) { m.favorite = !m.favorite; rc.config.save(rc.config.current()); } });

        if (ex > 0.01f) {
            R2D.scissor(ctx, x, y + baseH, w, h - baseH);
            float[] oc = clip;
            clip = intersect(clip, new float[]{x, y + baseH, w, h - baseH});
            float prev = R2D.alpha;
            R2D.alpha = prev * ex;
            float sy = y + baseH;
            R2D.rect(ctx, x + 10, sy, w - 20, 0.5f, Theme.stroke());
            sy += 6;
            sy = drawBindRow(ctx, m, x + 10, sy, w - 20);
            for (Setting<?> s : m.getSettings()) sy = drawSetting(ctx, s, x + 10, sy, w - 20);
            R2D.alpha = prev;
            clip = oc;
            R2D.endScissor(ctx);
        }
        return h;
    }

    private float drawBindRow(DrawContext ctx, Module m, float x, float y, float w) {
        float h = 20;
        R2D.icon(ctx, "keybind", x, y + h / 2f - 4, 8, Theme.textDim());
        Fonts.drawV(ctx, Lang.get("bind"), x + 13, y + h / 2f + 0.5f, Theme.textDim(), Font.SEMIBOLD, 0.75f);
        boolean listening = binding == m;
        String label = listening ? Lang.get("bind.listening") : Keys.name(m.getKey());
        float bw = Fonts.width(label, Font.BOLD, 0.7f) + 12;
        float bx = x + w - bw;
        float la = anim("bind" + m.getName(), 16).animate(listening ? 1 : 0);
        boolean hov = hovered(bx, y + 3, bw, h - 6);
        R2D.roundRect(ctx, bx, y + 3, bw, h - 6, 5, R2D.blend(hov ? Theme.cardHover() : Theme.toggleOff(), Theme.accent(), la));
        Fonts.drawV(ctx, label, bx + 6, y + h / 2f + 0.5f, R2D.blend(Theme.text(), Theme.accentText(), la), Font.BOLD, 0.7f);
        hit(bx, y + 3, bw, h - 6, b -> {
            if (b == 0) binding = binding == m ? null : m;
            else if (b == 1) { m.setKey(GLFW.GLFW_KEY_UNKNOWN); binding = null; }
        });
        return y + h;
    }

    private float drawSetting(DrawContext ctx, Setting<?> s, float x, float y, float w) {
        if (s instanceof BoolSetting b) {
            float h = 20;
            Fonts.drawV(ctx, s.getName(), x, y + h / 2f + 0.5f, Theme.textDim(), Font.SEMIBOLD, 0.75f);
            float on = b.anim.animate(b.get() ? 1 : 0);
            drawSwitch(ctx, x + w - 20, y + h / 2f - 5.5f, on);
            hit(x + w - 24, y, 26, h, bt -> b.toggle());
            return y + h;
        }
        if (s instanceof NumberSetting n) {
            float h = 28;
            Fonts.draw(ctx, s.getName(), x, y + 4, Theme.textDim(), Font.SEMIBOLD, 0.75f);
            String v = n.display();
            Fonts.draw(ctx, v, x + w - Fonts.width(v, Font.BOLD, 0.72f), y + 4, Theme.text(), Font.BOLD, 0.72f);
            drawSlider(ctx, n, x, y + 18, w);
            return y + h;
        }
        if (s instanceof ModeSetting md) {
            float h = 20;
            Fonts.drawV(ctx, s.getName(), x, y + h / 2f + 0.5f, Theme.textDim(), Font.SEMIBOLD, 0.75f);
            String v = md.get();
            float bw = Fonts.width(v, Font.BOLD, 0.7f) + 26;
            float bx = x + w - bw;
            boolean hov = hovered(bx, y + 3, bw, h - 6);
            R2D.roundRect(ctx, bx, y + 3, bw, h - 6, 5, hov ? Theme.cardHover() : Theme.toggleOff());
            R2D.icon(ctx, "arrow_left", bx + 4, y + h / 2f - 2.5f, 5, Theme.textFaint());
            ctx.getMatrices().push();
            ctx.getMatrices().translate(bx + bw - 6.5f, y + h / 2f, 0);
            ctx.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(180));
            R2D.icon(ctx, "arrow_left", -2.5f, -2.5f, 5, Theme.textFaint());
            ctx.getMatrices().pop();
            Fonts.drawCentered(ctx, v, bx + bw / 2f, y + h / 2f - Fonts.height(0.7f) / 2f + 0.5f, Theme.text(), Font.BOLD, 0.7f);
            hit(bx, y + 3, bw, h - 6, bt -> md.cycle(bt == 1 ? -1 : 1));
            return y + h;
        }
        return y;
    }

    // ------------------------------------------------------------------ widgety

    private void drawSwitch(DrawContext ctx, float x, float y, float on) {
        float w = 20, h = 11;
        R2D.roundRect(ctx, x, y, w, h, h / 2f, R2D.blend(Theme.toggleOff(), Theme.accent(), on));
        float kx = x + h / 2f + (w - h) * on;
        R2D.circle(ctx, kx, y + h / 2f, h / 2f - 2, R2D.blend(Theme.textDim(), Theme.accentText(), on));
    }

    private void drawSlider(DrawContext ctx, NumberSetting n, float x, float y, float w) {
        float pct = n.anim.animate((float) n.percent());
        boolean hov = hovered(x - 2, y - 4, w + 4, 10) || dragging == n;
        float ha = anim("slider" + System.identityHashCode(n), 18).animate(hov ? 1 : 0);
        R2D.roundRect(ctx, x, y, w, 3, 1.5f, Theme.toggleOff());
        R2D.roundRect(ctx, x, y, Math.max(3, w * pct), 3, 1.5f, Theme.accent());
        float r = 3.6f + ha * 0.9f;
        R2D.circle(ctx, x + w * pct, y + 1.5f, r + 1, Theme.card());
        R2D.circle(ctx, x + w * pct, y + 1.5f, r, Theme.accent());
        sliders.add(new Object[]{n, x - 4, y - 6, w + 8, 14f, clip});
        hit(x - 4, y - 5, w + 8, 13, b -> {
            if (b != 0) return;
            dragging = n; dragTrackX = x; dragTrackW = w;
        });
    }

    private void button(DrawContext ctx, String key, float x, float y, float w, float h, String icon, String label, boolean primary, Runnable action) {
        boolean hov = hovered(x, y, w, h);
        float ha = anim("btn" + key, 18).animate(hov ? 1 : 0);
        int bg = primary ? R2D.blend(Theme.accent(), Theme.textDim(), ha * 0.25f) : R2D.blend(Theme.card(), Theme.cardHover(), ha);
        int fg = primary ? Theme.accentText() : Theme.text();
        R2D.roundRect(ctx, x, y, w, h, 7, bg);
        if (!primary) R2D.roundOutline(ctx, x, y, w, h, 7, 0.5f, Theme.stroke());
        float contentW = (icon != null ? 9 : 0) + (label != null ? Fonts.width(label, Font.SEMIBOLD, 0.75f) + (icon != null ? 5 : 0) : 0);
        float cx = x + (w - contentW) / 2f;
        if (icon != null) { R2D.icon(ctx, icon, cx, y + h / 2f - 4.5f, 9, fg); cx += 14; }
        if (label != null) Fonts.drawV(ctx, label, cx, y + h / 2f + 0.5f, fg, Font.SEMIBOLD, 0.75f);
        hit(x, y, w, h, b -> { if (b == 0) action.run(); });
    }

    private void segmented(DrawContext ctx, String key, float x, float y, float w, float h, String[] labels, int selected, IntConsumer onSelect) {
        R2D.roundRect(ctx, x, y, w, h, 7, Theme.toggleOff());
        float sw = (w - 4) / labels.length;
        Anim a = anim("seg" + key, 16);
        if (a.raw() == 0 && a.target() == 0) a.snap(selected);
        float pos = a.animate(selected);
        R2D.roundRect(ctx, x + 2 + sw * pos, y + 2, sw, h - 4, 5.5f, Theme.accent());
        for (int i = 0; i < labels.length; i++) {
            float near = 1 - Anim.clamp01(Math.abs(pos - i));
            Fonts.drawCentered(ctx, labels[i], x + 2 + sw * i + sw / 2f, y + h / 2f - Fonts.height(0.72f) / 2f + 0.5f,
                    R2D.blend(Theme.textDim(), Theme.accentText(), near), Font.SEMIBOLD, 0.72f);
            final int fi = i;
            hit(x + 2 + sw * i, y, sw, h, b -> { if (b == 0) onSelect.accept(fi); });
        }
    }

    // ------------------------------------------------------------------ motywy

    private float drawThemes(DrawContext ctx, float x, float y, float w) {
        Theme.Preset[] presets = Theme.Preset.values();
        float gap = 12, cardW = (w - gap * (presets.length - 1)) / presets.length, cardH = 100 * ((cardW - 20) / 120f) + 56;
        for (int i = 0; i < presets.length; i++) {
            Theme.Preset p = presets[i];
            float cx = x + i * (cardW + gap);
            boolean active = Theme.preset() == p;
            boolean hov = hovered(cx, y, cardW, cardH);
            float aa = anim("theme" + i, 14).animate(active ? 1 : 0);
            float ha = anim("themeh" + i, 18).animate(hov ? 1 : 0);
            float lift = ha * 2;
            R2D.roundRect(ctx, cx, y - lift, cardW, cardH, 12, R2D.blend(Theme.card(), Theme.cardHover(), ha * 0.6f));
            R2D.roundOutline(ctx, cx, y - lift, cardW, cardH, 12, 0.6f + aa * 0.8f, R2D.blend(Theme.stroke(), Theme.accent(), aa));

            // mini podgląd GUI w kolorach motywu
            float px0 = cx + 10, py0 = y - lift + 10, pwReal = cardW - 20;
            float ps = pwReal / 120f, ph = 100 * ps;
            ctx.getMatrices().push();
            ctx.getMatrices().translate(px0, py0, 0);
            ctx.getMatrices().scale(ps, ps, 1);
            float px = 0, py = 0, pw = 120;
            {
            float ph0 = 100;
            R2D.roundRect(ctx, px, py, pw, ph0, 8, p.color(0));
            R2D.roundRect(ctx, px, py, pw * 0.3f + 8, ph0, 8, p.color(1));
            R2D.rect(ctx, px + pw * 0.3f, py, 8, ph0, p.color(0));
            R2D.roundRect(ctx, px + 6, py + 6, 8, 8, 2.5f, p.color(8));
            R2D.roundRect(ctx, px + 17, py + 8, pw * 0.3f - 22, 4, 2, p.color(6));
            for (int k = 0; k < 5; k++) {
                int c = k == 1 ? p.color(8) : p.color(3);
                R2D.roundRect(ctx, px + 5, py + 22 + k * 12, pw * 0.3f - 10, 8, 3, c);
            }
            for (int k = 0; k < 4; k++) {
                float bx = px + pw * 0.3f + 8 + (k % 2) * ((pw * 0.7f - 20) / 2f + 4);
                float by = py + 12 + (k / 2) * 44;
                float bw = (pw * 0.7f - 20) / 2f, bh = 38;
                R2D.roundRect(ctx, bx, by, bw, bh, 5, p.color(2));
                R2D.roundRect(ctx, bx + 5, by + 6, 10, 10, 3, k == 0 || k == 3 ? p.color(8) : p.color(10));
                R2D.roundRect(ctx, bx + 19, by + 8, bw * 0.4f, 3, 1.5f, p.color(5));
                R2D.roundRect(ctx, bx + 19, by + 14, bw * 0.3f, 2.5f, 1.25f, p.color(6));
                R2D.roundRect(ctx, bx + bw - 15, by + bh - 10, 10, 5, 2.5f, k == 0 || k == 3 ? p.color(8) : p.color(10));
            }
            R2D.roundOutline(ctx, px, py, pw, 100, 8, 0.5f, p.color(4));
            }
            ctx.getMatrices().pop();

            Fonts.draw(ctx, p.getName(), cx + 12, y - lift + ph + 22, Theme.text(), Font.BOLD, 1f);
            if (aa > 0.02f) {
                float prev = R2D.alpha;
                R2D.alpha = prev * aa;
                String t = Lang.get("theme.active");
                float tw = Fonts.width(t, Font.BOLD, 0.65f) + 18;
                float tx = cx + cardW - 14 - tw, tyb = y - lift + 14;
                R2D.shadow(ctx, tx, tyb, tw, 12, 5, Theme.shadow());
                R2D.roundRect(ctx, tx, tyb, tw, 12, 6, Theme.accent());
                R2D.icon(ctx, "check", tx + 4, tyb + 2.5f, 7, Theme.accentText());
                Fonts.drawV(ctx, t, tx + 13, tyb + 6.5f, Theme.accentText(), Font.BOLD, 0.65f);
                R2D.alpha = prev;
            }
            String desc = switch (p) {
                case DARK -> Lang.pl() ? "Grafit i biel" : "Graphite & white";
                case MIDNIGHT -> Lang.pl() ? "Granat nocy" : "Night navy";
                case LIGHT -> Lang.pl() ? "Biel i czerń" : "White & black";
                case AMOLED -> Lang.pl() ? "Czysta czerń" : "Pure black";
            };
            Fonts.draw(ctx, desc, cx + 12, y - lift + ph + 35, Theme.textDim(), Font.REGULAR, 0.72f);
            hit(cx, y, cardW, cardH, b -> {
                if (b == 0) { Theme.set(p); rc.config.saveClient(); }
            });
        }
        // kolor akcentu
        float py = y + cardH + 18;
        Fonts.draw(ctx, Lang.get("themes.accent"), x, py, Theme.text(), Font.BOLD, 0.86f);
        Fonts.draw(ctx, Lang.get("themes.accent.desc"), x, py + 11, Theme.textFaint(), Font.REGULAR, 0.66f);
        Theme.Accent[] accents = Theme.Accent.values();
        float ag = 8, aw = (w - ag * (accents.length - 1)) / accents.length, ah = 46, ay = py + 26;
        for (int i = 0; i < accents.length; i++) {
            Theme.Accent ac = accents[i];
            float ax = x + i * (aw + ag);
            boolean act = Theme.accentPreset() == ac;
            boolean hov = hovered(ax, ay, aw, ah);
            float sa = anim("acc" + i, 16).animate(act ? 1 : 0);
            float ha = anim("acch" + i, 18).animate(hov ? 1 : 0);
            R2D.roundRect(ctx, ax, ay - ha * 1.5f, aw, ah, 8, R2D.blend(Theme.card(), Theme.cardHover(), ha));
            R2D.roundOutline(ctx, ax, ay - ha * 1.5f, aw, ah, 8, 0.6f + sa * 0.6f, R2D.blend(Theme.stroke(), Theme.accent(), sa));
            int c1 = ac.first(Theme.preset()), c2 = ac.second(Theme.preset());
            float cx = ax + aw / 2f, cy = ay - ha * 1.5f + 17;
            R2D.circle(ctx, cx, cy, 9, c2);
            R2D.scissor(ctx, cx - 9, cy - 9, 9, 18);
            R2D.circle(ctx, cx, cy, 9, c1);
            R2D.endScissor(ctx);
            if (sa > 0.02f) {
                float prev = R2D.alpha;
                R2D.alpha = prev * sa;
                R2D.icon(ctx, "check", cx - 4, cy - 4, 8, 0.299f * ((c1 >> 16) & 0xFF) + 0.587f * ((c1 >> 8) & 0xFF) + 0.114f * (c1 & 0xFF) > 150 ? 0xFF0A0A0B : 0xFFFFFFFF);
                R2D.alpha = prev;
            }
            Fonts.drawCentered(ctx, ac.getName(), cx, ay - ha * 1.5f + 32, act ? Theme.text() : Theme.textDim(), act ? Font.BOLD : Font.SEMIBOLD, 0.62f);
            hit(ax, ay, aw, ah, bt -> { if (bt == 0) { Theme.setAccent(ac); rc.config.saveClient(); } });
        }
        return cardH + 18 + 26 + ah + 10;
    }

    // ------------------------------------------------------------------ config

    private float drawConfigs(DrawContext ctx, float x, float y, float w) {
        List<String> list = rc.config.list();
        if (list.isEmpty()) {
            R2D.icon(ctx, "folder", x + w / 2f - 12, y + 40, 24, Theme.textFaint());
            Fonts.drawCentered(ctx, Lang.get("config.empty"), x + w / 2f, y + 72, Theme.textDim(), Font.SEMIBOLD, 0.9f);
            return 110;
        }
        float rowH = 34, gap = 6, cy = y;
        for (String name : list) {
            boolean current = name.equals(rc.config.current());
            boolean hov = hovered(x, cy, w, rowH);
            float ha = anim("cfg" + name, 18).animate(hov ? 1 : 0);
            R2D.roundRect(ctx, x, cy, w, rowH, 7, R2D.blend(Theme.card(), Theme.cardHover(), ha));
            R2D.roundOutline(ctx, x, cy, w, rowH, 7, 0.5f, current ? Theme.textFaint() : Theme.stroke());
            R2D.icon(ctx, "disk", x + 10, cy + 10, 8.5f, current ? Theme.accent() : Theme.textFaint());
            Fonts.draw(ctx, name, x + 24, cy + 10, Theme.text(), Font.BOLD, 0.86f);
            java.nio.file.Path file = rc.config.folder().resolve(name + ".json");
            String info;
            try {
                info = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(java.nio.file.Files.getLastModifiedTime(file).toMillis()));
            } catch (Exception e) { info = "-"; }
            Fonts.draw(ctx, info, x + 24, cy + 21, Theme.textFaint(), Font.REGULAR, 0.66f);
            if (current) {
                String t = Lang.get("config.current");
                float tw = Fonts.width(t, Font.BOLD, 0.62f) + 8;
                float tx = x + 24 + Fonts.width(name, Font.BOLD, 0.86f) + 6;
                R2D.roundRect(ctx, tx, cy + 10.5f, tw, 10, 5, Theme.toggleOff());
                Fonts.drawV(ctx, t, tx + 4, cy + 15.5f, Theme.textDim(), Font.BOLD, 0.62f);
            }
            float bx = x + w - 8;
            bx -= 22; button(ctx, "del" + name, bx, cy + 7, 20, 20, "trash", null, false, () -> {
                rc.config.delete(name);
                rc.notifications.info("trash", Lang.get("config.deleted"), name);
            });
            bx -= 24; button(ctx, "save" + name, bx, cy + 7, 20, 20, "disk", null, false, () -> {
                rc.config.save(name);
                rc.notifications.info("disk", Lang.get("config.saved"), name);
            });
            float lw = Fonts.width(Lang.get("config.load"), Font.SEMIBOLD, 0.75f) + 30;
            bx -= lw + 4; button(ctx, "load" + name, bx, cy + 7, lw, 20, "folder", Lang.get("config.load"), true, () -> {
                rc.config.load(name);
                rc.notifications.info("folder", Lang.get("config.loaded"), name);
            });
            cy += rowH + gap;
        }
        return cy - y;
    }

    // ------------------------------------------------------------------ profil

    private float drawProfile(DrawContext ctx, float x, float y, float w) {
        float h = 84;
        R2D.roundRect(ctx, x, y, w, h, 12, Theme.card());
        R2D.roundOutline(ctx, x, y, w, h, 12, 0.6f, Theme.stroke());
        R2D.roundRect(ctx, x + 14, y + 14, 56, 56, 12, Theme.toggleOff());
        drawHead(ctx, x + 18, y + 18, 48);
        String name = client.getSession().getUsername();
        Fonts.draw(ctx, name, x + 84, y + 22, Theme.text(), Font.BOLD, 1.6f);
        Fonts.draw(ctx, "RazerClient  •  v" + RazerClient.VERSION, x + 84, y + 42, Theme.textDim(), Font.REGULAR, 0.78f);
        float bw = 96;
        button(ctx, "opensettings", x + w - bw - 14, y + h / 2f - 11, bw, 22, "settings", Lang.pl() ? "Ustawienia" : "Settings", true, () -> setPage(Page.SETTINGS));

        // kafelki statystyk
        long sess = (System.currentTimeMillis() - rc.startTime) / 1000;
        String server;
        if (client.isInSingleplayer()) server = Lang.get("profile.singleplayer");
        else {
            ServerInfo si = client.getCurrentServerEntry();
            server = si != null ? si.address : Lang.get("profile.menu");
        }
        String uuid = client.getSession().getUuidOrNull() != null ? client.getSession().getUuidOrNull().toString() : "-";
        String[][] tiles = {
                {"profile", Lang.get("profile.uuid"), uuid},
                {"world", Lang.get("profile.server"), server},
                {"clock", Lang.get("profile.session"), String.format("%02d:%02d:%02d", sess / 3600, (sess / 60) % 60, sess % 60)},
                {"layers", Lang.get("profile.enabled"), rc.modules.enabledCount() + " / " + rc.modules.all().size()},
                {"keyboard", Lang.get("profile.binds"), String.valueOf(rc.modules.all().stream().filter(Module::hasKey).count())},
                {"globe_lang", Lang.get("profile.lang"), Lang.current().display},
        };
        float gap = 8, tw = (w - gap * 2) / 3f, th = 40;
        float ty = y + h + 12;
        for (int i = 0; i < tiles.length; i++) {
            float tx = x + (i % 3) * (tw + gap), tyy = ty + (i / 3) * (th + gap);
            boolean hov = hovered(tx, tyy, tw, th);
            float ha = anim("tile" + i, 18).animate(hov ? 1 : 0);
            R2D.roundRect(ctx, tx, tyy, tw, th, 7, R2D.blend(Theme.card(), Theme.cardHover(), ha));
            R2D.roundOutline(ctx, tx, tyy, tw, th, 7, 0.5f, Theme.stroke());
            R2D.icon(ctx, tiles[i][0], tx + 10, tyy + 9, 8, Theme.textDim());
            Fonts.draw(ctx, tiles[i][1], tx + 22, tyy + 9.5f, Theme.textFaint(), Font.SEMIBOLD, 0.66f);
            Fonts.draw(ctx, Fonts.trim(tiles[i][2], Font.BOLD, 0.84f, tw - 20), tx + 10, tyy + 23, Theme.text(), Font.BOLD, 0.84f);
        }
        float hy = ty + (th + gap) * 2 + 2;
        R2D.icon(ctx, "info", x, hy, 9, Theme.textFaint());
        Fonts.draw(ctx, Lang.get("profile.hint"), x + 14, hy + 1, Theme.textFaint(), Font.REGULAR, 0.72f);
        return hy - y + 16;
    }

    /** krótki opis ustawień modułu (wartości trybów i suwaków) */
    private static String summary(Module m) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (Setting<?> s : m.getSettings()) {
            String v = null;
            if (s instanceof ModeSetting md) v = md.get();
            else if (s instanceof NumberSetting num) v = num.display();
            if (v == null) continue;
            if (sb.length() > 0) sb.append("  ");
            sb.append(v);
            if (++n >= 2) break;
        }
        return sb.toString();
    }

    private void drawFooter(DrawContext ctx) {
        String[][] items = {{"LPM", "footer.toggle"}, {"PPM / >", "footer.settings"}, {"ŚPM", "footer.bind"},
                {Lang.pl() ? "Kółko" : "Wheel", "footer.wheel"}, {"RShift / Esc", "footer.close"}};
        float s = 0.62f, gap = 12, total = 0;
        for (String[] it : items) total += Fonts.width(it[0], Font.BOLD, s) + 4 + Fonts.width(Lang.get(it[1]), Font.REGULAR, s) + gap;
        total -= gap;
        float fx = x0 + W / 2f - total / 2f - 10, fy = y0 + H + 8;
        R2D.roundRect(ctx, fx, fy, total + 20, 15, 7.5f, R2D.withAlpha(Theme.bg(), 0.85f));
        R2D.roundOutline(ctx, fx, fy, total + 20, 15, 7.5f, 0.5f, Theme.stroke());
        float cx = fx + 10;
        for (String[] it : items) {
            Fonts.drawV(ctx, it[0], cx, fy + 8, Theme.text(), Font.BOLD, s);
            cx += Fonts.width(it[0], Font.BOLD, s) + 4;
            String d = Lang.get(it[1]);
            Fonts.drawV(ctx, d, cx, fy + 8, Theme.textFaint(), Font.REGULAR, s);
            cx += Fonts.width(d, Font.REGULAR, s) + gap;
        }
    }

    // ------------------------------------------------------------------ znajomi

    private void addFriend() {
        String n = friendInput.trim();
        if (dev.razer.client.config.Friends.validName(n) && rc.friends.add(n)) {
            rc.notifications.info("users", Lang.get("friends.added"), n);
            rc.config.saveClient();
        }
        friendInput = "";
    }

    private float drawFriends(DrawContext ctx, float x, float y, float w) {
        // pole dodawania
        float h = 24, bw = 90;
        boolean hov = hovered(x, y, w - bw - 8, h);
        float fa = anim("friendfocus", 16).animate(friendFocused ? 1 : 0);
        R2D.roundRect(ctx, x, y, w - bw - 8, h, h / 2f, R2D.blend(hov ? Theme.cardHover() : Theme.card(), R2D.withAlpha(Theme.accent(), 0.18f), fa));
        R2D.roundOutline(ctx, x, y, w - bw - 8, h, h / 2f, 0.6f, R2D.blend(Theme.stroke(), Theme.accent(), fa));
        R2D.icon(ctx, "users", x + 10, y + h / 2f - 4.5f, 9, Theme.textFaint());
        String shown = friendInput.isEmpty() ? Lang.get("friends.placeholder") : friendInput;
        Fonts.drawV(ctx, shown, x + 25, y + h / 2f + 0.5f, friendInput.isEmpty() ? Theme.textFaint() : Theme.text(), Font.REGULAR, 0.78f);
        if (friendFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            float cxp = x + 25 + (friendInput.isEmpty() ? 0 : Fonts.width(friendInput, Font.REGULAR, 0.78f) + 1);
            R2D.rect(ctx, cxp, y + 7, 0.7f, h - 14, Theme.text());
        }
        hit(x, y, w - bw - 8, h, b -> { friendFocused = true; searchFocused = false; });
        button(ctx, "addfriend", x + w - bw, y, bw, h, "plus", Lang.get("friends.add"), true, this::addFriend);

        float cy = y + h + 12;
        java.util.List<String> list = new java.util.ArrayList<>(rc.friends.all());
        if (list.isEmpty()) {
            R2D.icon(ctx, "users", x + w / 2f - 12, cy + 30, 24, Theme.textFaint());
            Fonts.drawCentered(ctx, Lang.get("friends.empty"), x + w / 2f, cy + 62, Theme.textDim(), Font.SEMIBOLD, 0.85f);
            return cy + 90 - y;
        }
        float gap = 8, colW = (w - gap) / 2f, rh = 30;
        for (int i = 0; i < list.size(); i++) {
            String name = list.get(i);
            float fx = x + (i % 2) * (colW + gap), fy = cy + (i / 2) * (rh + gap);
            boolean hv = hovered(fx, fy, colW, rh);
            float ha = anim("fr" + name, 18).animate(hv ? 1 : 0);
            R2D.roundRect(ctx, fx, fy, colW, rh, 8, R2D.blend(Theme.card(), Theme.cardHover(), ha));
            R2D.roundOutline(ctx, fx, fy, colW, rh, 8, 0.5f, Theme.stroke());
            R2D.circle(ctx, fx + 15, fy + rh / 2f, 7, Theme.toggleOff());
            Fonts.drawCentered(ctx, name.substring(0, 1).toUpperCase(), fx + 15, fy + rh / 2f - Fonts.height(0.7f) / 2f + 0.5f, Theme.accentWave(i * 0.4f), Font.BOLD, 0.7f);
            Fonts.drawV(ctx, name, fx + 28, fy + rh / 2f + 0.5f, Theme.text(), Font.BOLD, 0.82f);
            float bx = fx + colW - 24;
            boolean hb = hovered(bx, fy + 7, 16, 16);
            R2D.roundRect(ctx, bx, fy + 7, 16, 16, 8, R2D.withAlpha(Theme.cardHover(), hb ? 1 : ha));
            R2D.icon(ctx, "trash", bx + 4, fy + 11, 8, hb ? 0xFFFF5A5A : Theme.textFaint());
            hit(bx, fy + 7, 16, 16, b -> { if (b == 0) { rc.friends.remove(name); rc.config.saveClient(); } });
        }
        return cy + ((list.size() + 1) / 2) * (rh + gap) - y;
    }

    // ------------------------------------------------------------------ HUD

    private float drawHudPage(DrawContext ctx, float x, float y, float w) {
        // karta edytora
        float eh = 52;
        boolean hov = hovered(x, y, w, eh);
        float ha = anim("hudeditor", 18).animate(hov ? 1 : 0);
        R2D.roundRect(ctx, x, y, w, eh, 9, R2D.blend(Theme.card(), Theme.cardHover(), ha));
        R2D.roundOutline(ctx, x, y, w, eh, 9, 0.6f, R2D.blend(Theme.stroke(), Theme.accent(), ha * 0.6f));
        // mini podgląd ekranu
        float px = x + 10, pyy = y + 9, pw = 56, ph = 34;
        R2D.roundRect(ctx, px, pyy, pw, ph, 5, Theme.toggleOff());
        R2D.roundRect(ctx, px + 4, pyy + 4, 16, 10, 2, Theme.accentWave(0));
        R2D.roundRect(ctx, px + 4, pyy + 16, 12, 7, 2, Theme.textFaint());
        R2D.roundRect(ctx, px + pw - 22, pyy + 4, 18, 4, 1.5f, Theme.accentWave(1));
        R2D.roundRect(ctx, px + pw - 18, pyy + ph - 10, 14, 6, 2, Theme.textFaint());
        float t = (System.currentTimeMillis() % 3000) / 3000f;
        float mxp = px + 20 + (float) Math.sin(t * Math.PI * 2) * 10, myp = pyy + 18 + (float) Math.cos(t * Math.PI * 2) * 5;
        R2D.icon(ctx, "move", mxp, myp, 7, Theme.text());
        Fonts.draw(ctx, Lang.get("hud.editor"), x + 76, y + 12, Theme.text(), Font.BOLD, 0.95f);
        Fonts.draw(ctx, Fonts.trim(Lang.get("hud.editor.desc"), Font.REGULAR, 0.66f, w - 76 - 120), x + 76, y + 25, Theme.textFaint(), Font.REGULAR, 0.66f);
        float bw = 100;
        button(ctx, "openeditor", x + w - bw - 10, y + eh / 2f - 10, bw, 20, "move", Lang.get("hud.editor.open"), true,
                () -> { if (client != null) client.setScreen(new HudEditor()); });
        hit(x, y, w - bw - 20, eh, b -> { if (b == 0 && client != null) client.setScreen(new HudEditor()); });

        float cy = y + eh + 10, gap = 6;
        float colW = (w - gap) / 2f;
        int i = 0;
        for (dev.razer.client.hud.HudElement e : rc.hud.elements) {
            float ex = x + (i % 2) * (colW + gap), ey = cy + (i / 2) * (34 + gap);
            settingRowAt(ctx, ex, ey, colW, e.icon, e.getName(), e.getDesc(), 90);
            drawSwitch(ctx, ex + colW - 30, ey + 17 - 5.5f, e.enabled.anim.animate(e.enabled.get() ? 1 : 0));
            hit(ex, ey, colW, 34, b -> { if (b == 0) { e.enabled.toggle(); rc.config.saveClient(); } });
            i++;
        }
        float by = cy + ((rc.hud.elements.size() + 1) / 2) * (34 + gap) + 2;
        button(ctx, "hudreset", x, by, 130, 20, "reset", Lang.get("hud.reset"), false, () -> {
            for (dev.razer.client.hud.HudElement e : rc.hud.elements) e.resetPosition();
            rc.config.saveClient();
        });
        return by + 24 - y;
    }

    private void settingRowAt(DrawContext ctx, float x, float y, float w, String icon, String title, String desc, float rightSpace) {
        float h = 34;
        boolean hov = hovered(x, y, w, h);
        float ha = anim("rowat" + title, 18).animate(hov ? 1 : 0);
        R2D.roundRect(ctx, x, y, w, h, 7, R2D.blend(Theme.card(), Theme.cardHover(), ha));
        R2D.roundOutline(ctx, x, y, w, h, 7, 0.5f, Theme.stroke());
        R2D.icon(ctx, icon, x + 10, y + 8.5f, 8.5f, Theme.accentWave(0));
        Fonts.draw(ctx, title, x + 23, y + 8, Theme.text(), Font.BOLD, 0.86f);
        Fonts.draw(ctx, Fonts.trim(desc, Font.REGULAR, 0.62f, w - 23 - 40), x + 23, y + 19, Theme.textFaint(), Font.REGULAR, 0.62f);
    }

    // ------------------------------------------------------------------ ustawienia klienta

    private float settingRow(DrawContext ctx, float x, float y, float w, String icon, String title, String desc) {
        float h = 34;
        boolean hov = hovered(x, y, w, h);
        float ha = anim("row" + title, 18).animate(hov ? 1 : 0);
        R2D.roundRect(ctx, x, y, w, h, 7, R2D.blend(Theme.card(), Theme.cardHover(), ha));
        R2D.roundOutline(ctx, x, y, w, h, 7, 0.5f, Theme.stroke());
        R2D.icon(ctx, icon, x + 10, y + 8.5f, 8.5f, Theme.text());
        Fonts.draw(ctx, title, x + 23, y + 8, Theme.text(), Font.BOLD, 0.86f);
        Fonts.draw(ctx, Fonts.trim(desc, Font.REGULAR, 0.66f, w - 23 - 170), x + 23, y + 19, Theme.textFaint(), Font.REGULAR, 0.66f);
        return h;
    }

    private float drawSettings(DrawContext ctx, float x, float y, float w) {
        ClientSettings s = rc.settings;
        float cy = y, gap = 6, h;

        h = settingRow(ctx, x, cy, w, "globe_lang", Lang.get("set.language"), Lang.get("set.language.desc"));
        segmented(ctx, "lang", x + w - 150, cy + 8, 140, 18, new String[]{"Polski", "English"}, Lang.current().ordinal(), i -> {
            Lang.set(Lang.Language.values()[i]); rc.config.saveClient();
        });
        cy += h + gap;

        h = settingRow(ctx, x, cy, w, "layers", Lang.get("set.guisize"), Lang.get("set.guisize.desc"));
        String[] sizes = {"size.small", "size.normal", "size.large"};
        int sel = Arrays.asList(sizes).indexOf(s.guiSize.get());
        segmented(ctx, "size", x + w - 150, cy + 8, 140, 18, new String[]{Lang.get("size.small"), Lang.get("size.normal"), Lang.get("size.large")}, sel, i -> {
            s.guiSize.set(sizes[i]); rc.config.saveClient();
        });
        cy += h + gap;

        h = settingRow(ctx, x, cy, w, "bolt", Lang.get("set.anim"), Lang.get("set.anim.desc"));
        String v = s.animSpeed.display() + "x";
        Fonts.drawV(ctx, v, x + w - 10 - Fonts.width(v, Font.BOLD, 0.75f), cy + h / 2f + 0.5f, Theme.text(), Font.BOLD, 0.75f);
        drawSlider(ctx, s.animSpeed, x + w - 160, cy + h / 2f - 1.5f, 116);
        cy += h + gap;

        Object[][] toggles = {
                {"render", "set.blur", s.blur},
                {"sparkles", "set.particles", s.particles},
                {"sound", "set.sounds", s.sounds},
                {"bell", "set.notifications", s.notifications},
                {"star", "set.welcome", s.welcome},
                {"hud", "set.menu", s.customMenu},
        };
        for (Object[] t : toggles) {
            BoolSetting b = (BoolSetting) t[2];
            h = settingRow(ctx, x, cy, w, (String) t[0], Lang.get((String) t[1]), Lang.get(t[1] + ".desc"));
            drawSwitch(ctx, x + w - 30, cy + h / 2f - 5.5f, b.anim.animate(b.get() ? 1 : 0));
            float fy = cy;
            hit(x, fy, w, h, bt -> { if (bt == 0) { b.toggle(); rc.config.saveClient(); } });
            cy += h + gap;
        }

        button(ctx, "reset", x, cy + 2, 140, 22, "power", Lang.get("set.reset"), false, () -> {
            s.reset(); Anim.globalSpeed = 1f; rc.config.saveClient();
        });
        return cy + 26 - y;
    }

    // ================================================================== pomocnicze

    private Anim anim(String key, float speed) { return anims.computeIfAbsent(key, k -> new Anim(0, speed)); }

    private void setPage(Page p) {
        if (page == p && p != Page.CATEGORY) return;
        page = p;
        pageTime = System.currentTimeMillis();
        scrollTarget = 0;
        scroll.snap(0);
        binding = null;
        searchFocused = false;
    }

    private static float[] intersect(float[] a, float[] b) {
        if (a == null) return b;
        float x = Math.max(a[0], b[0]), y = Math.max(a[1], b[1]);
        float x2 = Math.min(a[0] + a[2], b[0] + b[2]), y2 = Math.min(a[1] + a[3], b[1] + b[3]);
        return new float[]{x, y, Math.max(0, x2 - x), Math.max(0, y2 - y)};
    }

    private static boolean inside(float px, float py, float x, float y, float w, float h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    private boolean hovered(float x, float y, float w, float h) {
        if (closing || draggingWindow) return false;
        if (clip != null && !inside(mx, my, clip[0], clip[1], clip[2], clip[3])) return false;
        return inside(mx, my, x, y, w, h);
    }

    private void hit(float x, float y, float w, float h, IntConsumer action) {
        hits.add(new Hit(x, y, w, h, clip, action));
    }

    // ================================================================== wejście

    private void updateMouse(double mouseX, double mouseY) {
        mx = ((float) mouseX - centerX) / scale + centerX;
        my = ((float) mouseY - centerY) / scale + centerY;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return true;
        updateMouse(mouseX, mouseY);
        boolean wasFocused = searchFocused;
        searchFocused = false;
        friendFocused = false;
        if (binding != null && button > 1) { // przypisanie przycisku myszy nie jest wspierane – anuluj
            binding = null;
            return true;
        }
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.clip != null && !inside(mx, my, h.clip[0], h.clip[1], h.clip[2], h.clip[3])) continue;
            if (inside(mx, my, h.x, h.y, h.w, h.h)) {
                Module before = binding;
                h.action.accept(button);
                if (rc.settings.sounds.get() && client != null)
                    client.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(
                            net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(), button == 1 ? 1.4f : 1.8f, 0.18f));
                if (before != null && binding == before && !wasFocused) { /* klik w ten sam przycisk */ }
                return true;
            }
        }
        binding = null;
        // klik poza oknem zamyka GUI
        if (!inside(mx, my, x0, y0, W, H)) close();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) { rc.config.saveClient(); }
        dragging = null;
        draggingWindow = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        updateMouse(mouseX, mouseY);
        for (Object[] sl : sliders) {
            float[] cl = (float[]) sl[5];
            if (cl != null && !inside(mx, my, cl[0], cl[1], cl[2], cl[3])) continue;
            if (inside(mx, my, (float) sl[1], (float) sl[2], (float) sl[3], (float) sl[4])) {
                NumberSetting n = (NumberSetting) sl[0];
                n.set(n.get() + n.step * Math.signum(verticalAmount));
                if (n == rc.settings.animSpeed) Anim.globalSpeed = n.get().floatValue();
                return true;
            }
        }
        scrollTarget = (float) Math.max(0, Math.min(maxScroll, scrollTarget - verticalAmount * 32));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (closing) return true;
        if (binding != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE)
                binding.setKey(GLFW.GLFW_KEY_UNKNOWN);
            else binding.setKey(keyCode);
            binding = null;
            return true;
        }
        if (friendFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !friendInput.isEmpty()) friendInput = friendInput.substring(0, friendInput.length() - 1);
            else if (keyCode == GLFW.GLFW_KEY_ENTER) addFriend();
            else if (keyCode == GLFW.GLFW_KEY_ESCAPE) friendFocused = false;
            return true;
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = Screen.hasControlDown() ? "" : search.substring(0, search.length() - 1);
                scrollTarget = 0;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) { searchFocused = false; return true; }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        if (keyCode == GLFW.GLFW_KEY_F && Screen.hasControlDown()) {
            if (page != Page.CATEGORY && page != Page.FAVORITES) setPage(Page.CATEGORY);
            searchFocused = true; return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            Category[] cs = Category.values();
            int dir = Screen.hasShiftDown() ? -1 : 1;
            category = page == Page.CATEGORY ? cs[Math.floorMod(category.ordinal() + dir, cs.length)] : category;
            search = "";
            setPage(Page.CATEGORY);
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (closing || binding != null) return true;
        if (friendFocused) {
            if ((Character.isLetterOrDigit(chr) || chr == '_') && friendInput.length() < 16) friendInput += chr;
            return true;
        }
        if ((page == Page.CATEGORY || page == Page.FAVORITES) && chr >= ' ' && chr != 127) {
            if (!searchFocused && Character.isLetterOrDigit(chr)) searchFocused = true;
            if (searchFocused && search.length() < 24) { search += chr; scrollTarget = 0; }
        }
        return true;
    }

    @Override
    public void close() {
        if (closing) return;
        closing = true;
        closeTime = System.currentTimeMillis();
        binding = null;
        dragging = null;
    }

    private void finishClose() {
        rc.config.save(rc.config.current());
        if (client != null) client.setScreen(null);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}
}
