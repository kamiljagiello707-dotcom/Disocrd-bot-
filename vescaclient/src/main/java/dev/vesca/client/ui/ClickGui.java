package dev.vesca.client.ui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.vesca.client.VescaClient;
import dev.vesca.client.module.Category;
import dev.vesca.client.module.Module;
import dev.vesca.client.module.Modules;
import dev.vesca.client.module.Setting;
import dev.vesca.client.render.Fonts;
import dev.vesca.client.render.Fonts.Font;
import dev.vesca.client.render.R2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.IntConsumer;

import static dev.vesca.client.ui.Ui.*;

/**
 * ClickGUI VescaClient.
 * <ul>
 *   <li>pasek boczny z ikonami – po najechaniu myszką płynnie się wysuwa (kategorie, ulubione, konfiguracje, znajomi, motyw)</li>
 *   <li>strona modułów: siatka kart + wysuwany panel ustawień modułu</li>
 *   <li>strony: Motyw (akcent, animacje, rozmiar, pasek boczny, efekty, 3D), Konfiguracje, Znajomi</li>
 *   <li>domyślnie płaskie; moduł "3D GUI" (Wygląd) rysuje okno jako pochylony panel z perspektywą</li>
 * </ul>
 * LPM – włącz/wyłącz, PPM – ustawienia, środkowy – przypisz klawisz, pisanie – szukaj, Esc – zamknij.
 */
public class ClickGui extends Screen {
    private enum Page { MODULES, CONFIGS, FRIENDS, THEME }

    // zapamiętane między otwarciami
    private static Page page = Page.MODULES;
    private static Category category = Category.COMBAT;
    private static boolean favorites;
    private static Module inspected;

    private final long openTime = System.currentTimeMillis();
    private long closeTime = -1, listTime = System.currentTimeMillis();
    private final Map<String, Anim> anims = new HashMap<>();
    private record Hit(float x, float y, float w, float h, IntConsumer action) {
        boolean in(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; }
    }
    private final List<Hit> hits = new ArrayList<>();
    private int mx, my;
    private String query = "", friendInput = "";
    /** 0 – nic, 1 – wyszukiwarka, 2 – pole nicku znajomego */
    private int focus;
    private Module binding;
    private Setting.Num dragging;
    private Runnable dragApply;
    private float dragX, dragW;
    private float scroll, scrollTarget, maxScroll, dScroll, dScrollTarget, dMax, pScroll, pScrollTarget, pMax;
    private float gx, gy, gw, gh, dx, sideRight;
    private final Anim drawer = new Anim(0, 16), catY = new Anim(-1, 18), side = new Anim(0, 13);
    private final Anim yawA = new Anim(0, 7), pitchA = new Anim(0, 7);
    private Module lastInspected;
    private long sideLeave, deleteArm;
    private String deleteName;
    private float dragFrac;

    // geometria bieżącej klatki (do mapowania myszy)
    private float x0, y0, W, H, sc = 1;
    private boolean td;
    private float yaw, pitch, D, push;
    private SimpleFramebuffer fb;

    public ClickGui() { super(Text.literal("VescaClient")); }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }

    private Anim anim(String k) { return anims.computeIfAbsent(k, s -> new Anim(0, 16)); }
    private Anim anim(String k, float speed) { return anims.computeIfAbsent(k, s -> new Anim(0, speed)); }
    /** animacja stanu – przy pierwszym użyciu od razu w docelowej wartości */
    private float state(String k, float speed, float target) { return anims.computeIfAbsent(k, s -> new Anim(target, speed)).animate(target); }
    private float hover(String k, float x, float y, float w, float h) { return anim(k).animate(over(x, y, w, h) ? 1 : 0); }
    private boolean over(float x, float y, float w, float h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private void hit(float x, float y, float w, float h, IntConsumer a) { hits.add(new Hit(x, y, w, h, a)); }

    private void click() {
        if (client != null && Theme.sounds) client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.6f, 0.2f));
    }

    private static boolean threeD() { Module m = Modules.get("3D GUI"); return m != null && m.isEnabled(); }

    private List<Module> shown() {
        if (!query.isEmpty()) {
            String q = query.toLowerCase(Locale.ROOT);
            return Modules.all().stream().filter(m -> m.name.toLowerCase(Locale.ROOT).contains(q) || m.description().toLowerCase(Locale.ROOT).contains(q)).toList();
        }
        if (favorites) return Modules.all().stream().filter(m -> m.favorite).toList();
        return Modules.of(category);
    }

    private static String keyName(int key) {
        if (key < 0) return "";
        String s = InputUtil.fromKeyCode(key, 0).getLocalizedText().getString();
        return s.length() > 10 ? s.substring(0, 10) : s.toUpperCase(Locale.ROOT);
    }

    // ================================================================== przestrzeń 3D

    /** rzut punktu (współrzędne płaskie ekranu, już po skali) na ekran przy pochylonym panelu */
    private float[] project(float fx, float fy) {
        float cx = width / 2f, cy = height / 2f;
        float ox = fx - cx, oy = fy - cy;
        float cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch), cyw = (float) Math.cos(yaw), syw = (float) Math.sin(yaw);
        float x1 = ox, y1 = oy * cp, z1 = oy * sp;
        float x2 = x1 * cyw + z1 * syw, z2 = -x1 * syw + z1 * cyw;
        float wz = z2 + push;
        float k = D / (D + wz);
        return new float[]{cx + x2 * k, cy + y1 * k};
    }

    /** odwrotność {@link #project}: punkt ekranu → współrzędne płaskie panelu */
    private float[] unproject(float sx, float sy) {
        float cx = width / 2f, cy = height / 2f;
        float cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch), cyw = (float) Math.cos(yaw), syw = (float) Math.sin(yaw);
        float[] U = {cyw, 0, -syw}, V = {sp * syw, cp, sp * cyw}, N = {cp * syw, -sp, cp * cyw};
        float[] d = {sx - cx, sy - cy, D};
        float[] ce = {0, 0, push + D}; // C - E (oko w (cx, cy, -D), środek panelu w (cx, cy, push))
        float dn = d[0] * N[0] + d[1] * N[1] + d[2] * N[2];
        if (Math.abs(dn) < 1e-4f) return new float[]{-9999, -9999};
        float t = (ce[0] * N[0] + ce[1] * N[1] + ce[2] * N[2]) / dn;
        float[] p = {t * d[0] - ce[0], t * d[1] - ce[1], t * d[2] - ce[2]}; // P - C
        return new float[]{cx + p[0] * U[0] + p[1] * U[1] + p[2] * U[2], cy + p[0] * V[0] + p[1] * V[1] + p[2] * V[2]};
    }

    /** mysz na ekranie → współrzędne GUI (cofa 3D i skalę) */
    private float[] toLocal(double sx, double sy) {
        float px = (float) sx, py = (float) sy;
        if (td) { float[] u = unproject(px, py); px = u[0]; py = u[1]; }
        float cx = width / 2f, cy = height / 2f;
        return new float[]{cx + (px - cx) / sc, cy + (py - cy) / sc};
    }

    private void composite(DrawContext ctx, float a) {
        float cx = width / 2f, cy = height / 2f;
        float fx0 = cx + (x0 - 40 - cx) * sc, fx1 = cx + (x0 + W + 40 - cx) * sc;
        float fy0 = cy + (y0 - 34 - cy) * sc, fy1 = cy + (y0 + H + 34 - cy) * sc;
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, fb.getColorAttachment());
        int c = (int) (a * 255);
        int col = (c << 24) | (c << 16) | (c << 8) | c;
        int nx = 28, ny = 18;
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int i = 0; i < nx; i++) for (int j = 0; j < ny; j++) {
            float ax = fx0 + (fx1 - fx0) * i / nx, bx = fx0 + (fx1 - fx0) * (i + 1) / nx;
            float ay = fy0 + (fy1 - fy0) * j / ny, by = fy0 + (fy1 - fy0) * (j + 1) / ny;
            vtx(b, m, ax, ay, col); vtx(b, m, ax, by, col); vtx(b, m, bx, by, col); vtx(b, m, bx, ay, col);
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
    }

    private void vtx(BufferBuilder b, Matrix4f m, float fx, float fy, int col) {
        float[] p = project(fx, fy);
        b.vertex(m, p[0], p[1], 0).texture(fx / width, 1 - fy / height).color(col);
    }

    // ================================================================== render

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (client != null && client.world != null) { if (Theme.blur) applyBlur(delta); }
        else Scene.render(ctx, width, height, mouseX, mouseY, 1, 0.3f);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hits.clear();
        long now = System.currentTimeMillis();
        float a = closeTime < 0 ? Anim.easeOutCubic((now - openTime) / (260f / Theme.motionSpeed())) : 1 - Anim.easeInCubic((now - closeTime) / (170f / Theme.motionSpeed()));
        if (closeTime >= 0 && a <= 0) { if (client != null) client.setScreen(null); return; }
        renderBackground(ctx, mouseX, mouseY, delta);
        R2D.alpha = 1;
        R2D.rect(ctx, 0, 0, width, height, R2D.withAlpha(0xFF000000, Theme.dim * a));
        R2D.gradientV(ctx, 0, height * 0.6f, width, height * 0.4f, 0x00000000, R2D.withAlpha(0xFF000000, 0.3f * a));

        // ---- geometria
        float s = Theme.scale;
        W = Math.min(640, width / s - 32);
        H = Math.min(380, height / s - 44);
        x0 = (width - W) / 2f;
        y0 = (height - H) / 2f - 6;
        sc = s * (0.95f + 0.05f * a);
        td = threeD();
        if (td) {
            Module m3 = Modules.get("3D GUI");
            float tilt = ((Setting.Num) m3.settings.get(0)).get().floatValue();
            boolean left = ((Setting.Mode) m3.settings.get(1)).get().equals("Left");
            boolean follow = ((Setting.Bool) m3.settings.get(2)).get();
            float fy = follow ? (mouseX / (float) width - 0.5f) * 7f : 0, fp = follow ? -(mouseY / (float) height - 0.5f) * 5f : 0;
            float baseYaw = (left ? -tilt : tilt) + (1 - a) * (left ? -14 : 14);
            yaw = (float) Math.toRadians(yawA.animate(baseYaw + fy));
            pitch = (float) Math.toRadians(pitchA.animate(fp + 2));
            D = Math.max(width, height) * 1.15f;
            push = Math.abs((float) Math.sin(yaw)) * W * sc / 2f * 0.55f;
        }
        float[] l = toLocal(mouseX, mouseY);
        mx = (int) Math.floor(l[0]); my = (int) Math.floor(l[1]);
        if (dragging != null && dragApply != null) dragApply.run();

        if (td) {
            int fw = client.getWindow().getFramebufferWidth(), fh = client.getWindow().getFramebufferHeight();
            if (fb == null) { fb = new SimpleFramebuffer(fw, fh, true, MinecraftClient.IS_SYSTEM_MAC); fb.setTexFilter(GL11.GL_LINEAR); }
            else if (fb.textureWidth != fw || fb.textureHeight != fh) { fb.resize(fw, fh, MinecraftClient.IS_SYSTEM_MAC); fb.setTexFilter(GL11.GL_LINEAR); }
            ctx.draw();
            fb.setClearColor(0, 0, 0, 0);
            fb.clear(MinecraftClient.IS_SYSTEM_MAC);
            fb.beginWrite(true);
        }
        ctx.getMatrices().push();
        ctx.getMatrices().translate(width / 2f, height / 2f, 0);
        ctx.getMatrices().scale(sc, sc, 1);
        ctx.getMatrices().translate(-width / 2f, -height / 2f, 0);
        R2D.alpha = td ? 1 : a;
        drawPanel(ctx);
        ctx.getMatrices().pop();
        if (td) {
            ctx.draw();
            client.getFramebuffer().beginWrite(true);
            composite(ctx, a);
        }
        R2D.alpha = 1;
    }

    @Override
    public void removed() {
        if (fb != null) { fb.delete(); fb = null; }
    }

    private void drawPanel(DrawContext ctx) {
        if (Theme.shadow) R2D.shadow(ctx, x0, y0 + 6, W, H, 30, 0xCC000000);
        R2D.glow(ctx, x0 - 40, y0 - 40, W + 80, H + 80, 0x0DFFFFFF);
        R2D.roundRectV(ctx, x0, y0, W, H, 14, 0xF5111114, 0xF80A0A0C);
        R2D.roundOutline(ctx, x0, y0, W, H, 14, 0.8f, 0x3CFFFFFF);
        R2D.gradientH(ctx, x0 + 20, y0 + 0.6f, (W - 40) / 2f, 0.6f, 0x00FFFFFF, 0x55FFFFFF);
        R2D.gradientH(ctx, x0 + W / 2f, y0 + 0.6f, (W - 40) / 2f, 0.6f, 0x55FFFFFF, 0x00FFFFFF);

        float rail = 44, full = Math.max(112, Math.min(134, W * 0.22f));
        boolean pinned = Theme.sidebar.equals("Expanded"), never = Theme.sidebar.equals("Collapsed");
        // wysuwanie paska po najechaniu
        float cur = rail + (full - rail) * side.raw();
        boolean overSide = mx >= x0 && mx < x0 + cur && my >= y0 && my < y0 + H;
        long now = System.currentTimeMillis();
        if (overSide) sideLeave = now;
        float target = pinned ? 1 : never ? 0 : (overSide || now - sideLeave < 140) && dragging == null ? 1 : 0;
        float e = side.animate(target);
        float contentX = x0 + (pinned ? full : rail);

        R2D.scissor(ctx, x0, y0, W, H);
        switch (page) {
            case MODULES -> { drawModules(ctx, contentX, W - (contentX - x0)); drawDrawer(ctx); }
            case CONFIGS -> drawConfigs(ctx, contentX, W - (contentX - x0));
            case FRIENDS -> drawFriends(ctx, contentX, W - (contentX - x0));
            case THEME -> drawTheme(ctx, contentX, W - (contentX - x0));
        }
        // przyciemnienie treści pod wysuniętym paskiem
        if (!pinned && e > 0.01f) {
            float sw = rail + (full - rail) * e;
            R2D.rect(ctx, x0 + sw, y0, W - sw, H, R2D.withAlpha(0xFF000000, 0.32f * e));
        }
        drawSidebar(ctx, rail, full, pinned ? 1 : e, pinned);
        R2D.endScissor(ctx);

        // podpowiedzi pod oknem
        float hy = y0 + H + 10;
        if (hy + 12 < height) {
            String[][] tips = page == Page.MODULES
                    ? new String[][]{{t("LPM", "LMB"), t("włącz", "toggle")}, {t("PPM", "RMB"), t("ustawienia", "settings")}, {t("ŚPM", "MMB"), t("klawisz", "keybind")}, {"Esc", t("zamknij", "close")}}
                    : new String[][]{{"Esc", t("zamknij", "close")}};
            float tw = 0;
            for (String[] tp : tips) tw += Math.max(12, Fonts.width(tp[0], Font.BOLD, 0.52f) + 8) + 4 + Fonts.width(tp[1], Font.REGULAR, 0.56f) + 12;
            float tx = width / 2f - (tw - 12) / 2f;
            for (String[] tp : tips) {
                tx += keyCap(ctx, tp[0], tx, hy) + 4;
                Fonts.drawV(ctx, tp[1], tx, hy + 6, 0x99FFFFFF, Font.REGULAR, 0.56f);
                tx += Fonts.width(tp[1], Font.REGULAR, 0.56f) + 12;
            }
        }
    }

    // ------------------------------------------------------------------ pasek boczny

    private void drawSidebar(DrawContext ctx, float rail, float full, float e, boolean pinned) {
        float sw = rail + (full - rail) * e;
        sideRight = x0 + sw;
        float prev = R2D.alpha;
        // tło paska: przy wysuwaniu nad treścią – pełne, z cieniem; przypięty – lekko jaśniejszy pas z linią
        if (!pinned && e > 0.01f) {
            R2D.shadow(ctx, x0, y0, sw, H, 18, R2D.withAlpha(0xFF000000, 0.6f * e));
            R2D.roundRect(ctx, x0, y0, sw + 14, H, 14, 0xFF111114);
        }
        R2D.scissor(ctx, x0, y0, sw, H);
        R2D.roundRect(ctx, x0, y0, sw + 14, H, 14, 0x07FFFFFF);
        R2D.endScissor(ctx);
        R2D.rect(ctx, x0 + sw, y0 + 12, 0.5f, H - 24, 0x14FFFFFF);
        hit(x0, y0, sw, H, b -> {});

        // logo + nazwa + wersja
        float lx = x0 + 13, ly = y0 + 13;
        if (e < 0.5f) { lx = x0 + sw / 2f - 10; }
        R2D.glow(ctx, lx - 8, ly - 8, 36, 36, R2D.withAlpha(Theme.accent(), 0.18f));
        R2D.icon(ctx, "vesca_logo", lx, ly, 20, 0xFFFFFFFF);
        if (e > 0.01f) {
            R2D.alpha = prev * e;
            Fonts.draw(ctx, "VescaClient", lx + 26 + (1 - e) * -6, ly + 1, TEXT, Font.BOLD, 0.8f);
            Fonts.draw(ctx, t("Wersja ", "Version ") + VescaClient.VERSION, lx + 26 + (1 - e) * -6, ly + 12, FAINT, Font.REGULAR, 0.55f);
            R2D.alpha = prev;
        }

        Category[] cats = Category.values();
        int n = cats.length + 4;
        String[] icons = new String[n], names = new String[n];
        for (int i = 0; i < cats.length; i++) { icons[i] = cats[i].icon; names[i] = cats[i].title(); }
        icons[cats.length] = "g_star"; names[cats.length] = t("Ulubione", "Favorites");
        icons[cats.length + 1] = "g_friends"; names[cats.length + 1] = t("Znajomi", "Friends");
        icons[cats.length + 2] = "g_configs"; names[cats.length + 2] = t("Konfiguracje", "Configs");
        icons[cats.length + 3] = "g_theme"; names[cats.length + 3] = t("Motyw", "Theme");
        float top = y0 + 46, profileH = 30;
        float headH = 13;
        float rh = Math.max(14, Math.min(20, (y0 + H - 8 - profileH - top - headH * 2 - 6) / n));
        boolean showProfile = rh > 14 || (y0 + H - 8 - profileH - top - headH * 2 - 6) / n >= 14;
        float[] ys = new float[n];
        float y = top;
        for (int i = 0; i < n; i++) {
            if (i == 0) { section(ctx, t("FUNKCJE", "FEATURES"), x0, sw, y, e); y += headH; }
            if (i == cats.length) { section(ctx, t("INNE", "OTHER"), x0, sw, y + 4, e); y += headH + 4; }
            ys[i] = y;
            y += rh;
        }
        int active = switch (page) {
            case MODULES -> !query.isEmpty() ? -1 : favorites ? cats.length : category.ordinal();
            case FRIENDS -> cats.length + 1;
            case CONFIGS -> cats.length + 2;
            case THEME -> cats.length + 3;
        };
        if (active >= 0 && catY.raw() < 0) catY.snap(ys[active]);
        float py = catY.animate(active >= 0 ? ys[active] : catY.target());
        float pa = anim("catPill").animate(active >= 0 ? 1 : 0);
        float px = x0 + 8, pw = sw - 16;
        if (pa > 0.01f) {
            R2D.alpha = prev * pa;
            R2D.glow(ctx, px - 8, py - 6, pw + 16, rh + 10, R2D.withAlpha(Theme.accent(), 0.16f));
            R2D.roundRectV(ctx, px, py + 1, pw, rh - 2, 6, Theme.accentTop(), Theme.accentBottom());
            R2D.alpha = prev;
        }
        float ic = e < 0.5f ? x0 + sw / 2f : x0 + 18;
        for (int i = 0; i < n; i++) {
            float ry = ys[i];
            float near = pa * Anim.clamp01(1 - Math.abs(py - ry) / rh);
            float hv = anim("side" + i).animate(over(px, ry, pw, rh) && i != active ? 1 : 0);
            if (hv > 0.01f) R2D.roundRect(ctx, px, ry + 1, pw, rh - 2, 6, R2D.withAlpha(0xFFFFFFFF, 0.06f * hv));
            int fg = R2D.blend(R2D.blend(0xFFB4B4BC, TEXT, hv), Theme.onAccent(), near);
            float is = Math.min(9.5f, rh - 6);
            R2D.icon(ctx, icons[i], ic - is / 2f + (e < 0.5f ? 0 : is / 2f), ry + rh / 2f - is / 2f, is, fg);
            long cnt = i < cats.length ? Modules.enabledIn(cats[i]) : 0;
            if (e > 0.01f) {
                R2D.alpha = prev * e;
                Fonts.drawV(ctx, Fonts.trim(names[i], Font.SEMIBOLD, 0.68f, sw - 54), x0 + 33 + (1 - e) * -6, ry + rh / 2f + 0.5f, fg, near > 0.5f ? Font.BOLD : Font.SEMIBOLD, 0.68f);
                if (cnt > 0) {
                    String c = String.valueOf(cnt);
                    Fonts.drawV(ctx, c, px + pw - 7 - Fonts.width(c, Font.BOLD, 0.52f), ry + rh / 2f + 0.5f, R2D.blend(FAINT, Theme.onAccent(), near), Font.BOLD, 0.52f);
                }
                R2D.alpha = prev;
            } else if (cnt > 0) R2D.circle(ctx, ic + 6, ry + rh / 2f - 4, 1.8f, R2D.blend(TEXT, Theme.onAccent(), near));
            final int fi = i;
            hit(px, ry, pw, rh, b -> {
                if (b != 0) return;
                click();
                query = ""; focus = 0;
                if (fi < cats.length) { page = Page.MODULES; favorites = false; category = cats[fi]; }
                else if (fi == cats.length) { page = Page.MODULES; favorites = true; }
                else { page = new Page[]{Page.FRIENDS, Page.CONFIGS, Page.THEME}[fi - cats.length - 1]; inspected = null; }
                scroll = scrollTarget = 0; pScroll = pScrollTarget = 0;
                listTime = System.currentTimeMillis();
            });
        }

        // profil
        if (!showProfile) return;
        float pby = y0 + H - 8 - profileH;
        R2D.rect(ctx, x0 + 10, pby, sw - 20, 0.5f, 0x12FFFFFF);
        head(ctx, e < 0.5f ? x0 + sw / 2f - 8 : x0 + 12, pby + 8, 16);
        if (e > 0.01f) {
            R2D.alpha = prev * e;
            String nick = client != null ? client.getSession().getUsername() : "Player";
            Fonts.drawV(ctx, Fonts.trim(nick, Font.SEMIBOLD, 0.66f, sw - 44), x0 + 33, pby + 16.5f, DIM, Font.SEMIBOLD, 0.66f);
            R2D.alpha = prev;
        }
    }

    private void section(DrawContext ctx, String label, float x, float sw, float y, float e) {
        float prev = R2D.alpha;
        R2D.alpha = prev * (1 - e);
        R2D.rect(ctx, x + sw / 2f - 8, y + 5, 16, 0.5f, 0x22FFFFFF);
        R2D.alpha = prev * e;
        if (e > 0.01f) spaced(ctx, label, x + 13, y + 2, FAINT, Font.BOLD, 0.46f, 1.4f);
        R2D.alpha = prev;
    }

    // ------------------------------------------------------------------ wspólne elementy stron

    /** nagłówek strony: ikona + tytuł • opis, cienka linia pod spodem */
    private void header(DrawContext ctx, float x, float w, String icon, String title, String sub) {
        float y = y0 + 13;
        R2D.icon(ctx, icon, x, y + 2, 10, TEXT);
        Fonts.draw(ctx, title, x + 16, y + 1, TEXT, Font.BOLD, 0.92f);
        float tw = Fonts.width(title, Font.BOLD, 0.92f);
        if (sub != null && !sub.isEmpty()) {
            Fonts.draw(ctx, "•", x + 16 + tw + 6, y + 3, FAINT, Font.BOLD, 0.6f);
            Fonts.draw(ctx, Fonts.trim(sub, Font.REGULAR, 0.6f, w - tw - 70), x + 16 + tw + 14, y + 3.5f, FAINT, Font.REGULAR, 0.6f);
        }
        R2D.rect(ctx, x, y0 + 36, w, 0.5f, 0x12FFFFFF);
    }

    /** pole tekstowe; which = numer pola (focus) */
    private void field(DrawContext ctx, int which, float x, float y, float w, float h, String text, String placeholder, String icon) {
        boolean f = focus == which;
        float fa = anim("field" + which).animate(f || !text.isEmpty() ? 1 : 0), hv = hover("fieldh" + which, x, y, w, h);
        R2D.roundRect(ctx, x, y, w, h, h / 2f, R2D.blend(0x10FFFFFF, 0x1EFFFFFF, Math.max(fa, hv * 0.6f)));
        R2D.roundOutline(ctx, x, y, w, h, h / 2f, 0.6f, R2D.blend(0x1EFFFFFF, R2D.withAlpha(Theme.accent(), 0.85f), f ? 1 : fa * 0.4f));
        R2D.icon(ctx, icon, x + 8, y + h / 2f - 4, 8, R2D.blend(FAINT, TEXT, fa));
        String shown = text.isEmpty() ? placeholder : text;
        Fonts.drawV(ctx, Fonts.trim(shown, Font.REGULAR, 0.66f, w - 36), x + 21, y + h / 2f + 0.5f, text.isEmpty() ? FAINT : TEXT, Font.REGULAR, 0.66f);
        if (f && (System.currentTimeMillis() / 500) % 2 == 0) {
            float cx = x + 21 + (text.isEmpty() ? 0 : Fonts.width(Fonts.trim(text, Font.REGULAR, 0.66f, w - 36), Font.REGULAR, 0.66f) + 1);
            R2D.rect(ctx, cx, y + 5, 0.7f, h - 10, TEXT);
        }
        hit(x, y, w, h, b -> focus = which);
    }

    /** przełącznik w kolorze akcentu */
    private void toggleAt(DrawContext ctx, float x, float y, float on, float hv) { Ui.toggle(ctx, x, y, on, hv); }

    /** suwak; zwraca wysokość */
    private float slider(DrawContext ctx, String key, float x, float y, float w, String label, String value, float frac, java.util.function.Consumer<Float> set) {
        float hv = anim("sh" + key).animate(over(x, y, w, 30) || dragging != null && key.equals(dragKey) ? 1 : 0);
        Fonts.drawV(ctx, label, x, y + 8, R2D.blend(DIM, TEXT, hv), Font.SEMIBOLD, 0.7f);
        float vw = Fonts.width(value, Font.BOLD, 0.62f) + 10;
        R2D.roundRect(ctx, x + w - vw, y + 2, vw, 12, 6, R2D.blend(0x14FFFFFF, 0x24FFFFFF, hv));
        Fonts.drawV(ctx, value, x + w - vw + 5, y + 8.5f, TEXT, Font.BOLD, 0.62f);
        float ty = y + 21, th = 4;
        float f = state("sv" + key, 22, frac);
        R2D.roundRect(ctx, x, ty, w, th, 2, 0x1AFFFFFF);
        R2D.roundRectH(ctx, x, ty, Math.max(th, w * f), th, 2, R2D.blend(Theme.accent(), 0xFF000000, 0.3f), Theme.accent());
        float kx = x + w * f;
        if (hv > 0.01f) R2D.glow(ctx, kx - 9, ty + th / 2f - 9, 18, 18, R2D.withAlpha(Theme.accent(), 0.4f * hv));
        R2D.circle(ctx, kx, ty + th / 2f, 4.2f + hv * 0.8f, 0xFFFFFFFF);
        R2D.circle(ctx, kx, ty + th / 2f, 1.6f, BLACK);
        hit(x - 4, y, w + 8, 30, b -> {
            if (b != 0) return;
            dragKey = key; dragX = x; dragW = w;
            dragging = DUMMY;
            dragApply = () -> set.accept(Anim.clamp01((mx - dragX) / dragW));
            dragApply.run();
        });
        return 32;
    }
    private String dragKey;
    private static final Setting.Num DUMMY = new Setting.Num("", 0, 0, 1, 1, "");

    /** rząd "chipsów" do wyboru; zwraca wysokość */
    private float chips(DrawContext ctx, String key, float x, float y, float w, String[] labels, int sel, IntConsumer pick) {
        float cx = x, cy = y;
        for (int i = 0; i < labels.length; i++) {
            float tw = Fonts.width(labels[i], Font.BOLD, 0.6f) + 14;
            if (cx + tw > x + w && cx > x) { cx = x; cy += 18; }
            float sa = state("c" + key + i, 16, i == sel ? 1 : 0);
            float hv = hover("ch" + key + i, cx, cy, tw, 15);
            R2D.roundRect(ctx, cx, cy, tw, 15, 7.5f, R2D.blend(R2D.blend(0x12FFFFFF, 0x24FFFFFF, hv), Theme.accent(), sa));
            if (sa < 0.99f) R2D.roundOutline(ctx, cx, cy, tw, 15, 7.5f, 0.5f, R2D.withAlpha(0xFFFFFFFF, 0.12f * (1 - sa)));
            Fonts.drawV(ctx, labels[i], cx + 7, cy + 8, R2D.blend(R2D.blend(DIM, TEXT, hv), Theme.onAccent(), sa), Font.BOLD, 0.6f);
            final int fi = i;
            hit(cx, cy, tw, 15, b -> { if (b == 0) { pick.accept(fi); click(); } });
            cx += tw + 5;
        }
        return cy - y + 15;
    }

    // ------------------------------------------------------------------ strona: moduły

    private Module hoveredModule;

    private void drawModules(DrawContext ctx, float x, float w) {
        float mx0 = x + 14, mw = w - 28;
        List<Module> list = shown();
        String title = !query.isEmpty() ? t("Szukaj", "Search") : favorites ? t("Ulubione", "Favorites") : category.title();
        String icon = !query.isEmpty() ? "g_search" : favorites ? "g_star" : category.icon;
        String sub = hoveredModule != null ? hoveredModule.description()
                : !query.isEmpty() ? t("Wyniki: " + list.size(), "Results: " + list.size())
                : t(list.size() + " modułów, " + list.stream().filter(Module::isEnabled).count() + " włączonych", list.size() + " modules, " + list.stream().filter(Module::isEnabled).count() + " enabled");
        header(ctx, mx0, mw, icon, title, sub);
        // wyszukiwarka: ikonka, po kliknięciu rozwija się w pole
        boolean open = focus == 1 || !query.isEmpty();
        float sw = 20 + 140 * anim("searchOpen", 14).animate(open ? 1 : 0);
        float sx = mx0 + mw - sw, sy = y0 + 9;
        if (sw > 24) {
            field(ctx, 1, sx, sy, sw, 20, query, t("Szukaj…", "Search…"), "g_search");
            if (!query.isEmpty()) {
                float bx = sx + sw - 16;
                R2D.icon(ctx, "g_close", bx, sy + 6.5f, 7, over(bx - 3, sy, 14, 20) ? TEXT : FAINT);
                hit(bx - 3, sy, 14, 20, b -> { query = ""; listTime = System.currentTimeMillis(); });
            }
        } else {
            float hv = hover("searchBtn", sx, sy, 20, 20);
            R2D.roundRect(ctx, sx, sy, 20, 20, 10, R2D.withAlpha(0xFFFFFFFF, 0.04f + 0.08f * hv));
            R2D.icon(ctx, "g_search", sx + 6, sy + 6, 8, R2D.blend(DIM, TEXT, hv));
            hit(sx, sy, 20, 20, b -> focus = 1);
        }

        gx = mx0; gy = y0 + 44; gw = mw; gh = H - 44 - 10;
        int cols = gw >= 560 ? 3 : gw >= 300 ? 2 : 1;
        float gap = 4, colGap = 10, ch = 27, cw = (gw - colGap * (cols - 1)) / cols;
        int rows = (list.size() + cols - 1) / cols;
        float contentH = rows * (ch + gap) - gap;
        maxScroll = Math.max(0, contentH - gh);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scroll += (scrollTarget - scroll) * 0.3f;
        hoveredModule = null;
        R2D.scissor(ctx, gx - 4, gy - 2, gw + 8, gh + 4);
        long now = System.currentTimeMillis();
        for (int i = 0; i < list.size(); i++) {
            // kolejność jak w referencji: najpierw w dół pierwszej kolumny, potem druga
            int col = i / rows, row = i % rows;
            float cx = gx + col * (cw + colGap), cy = gy + row * (ch + gap) - scroll;
            if (cy + ch < gy - 2 || cy > gy + gh + 2) continue;
            float e = Anim.easeOutCubic((now - listTime - i * 18) / (280f / Theme.motionSpeed()));
            float prev = R2D.alpha;
            R2D.alpha = prev * e;
            drawCard(ctx, list.get(i), cx + (1 - e) * 6, cy, cw, ch);
            R2D.alpha = prev;
        }
        R2D.endScissor(ctx);
        if (list.isEmpty()) {
            float cy = gy + gh / 2f;
            R2D.icon(ctx, favorites && query.isEmpty() ? "g_star" : "g_search", gx + gw / 2f - 10, cy - 24, 20, FAINT);
            Fonts.drawCentered(ctx, favorites && query.isEmpty() ? t("Brak ulubionych – dodaj gwiazdką w ustawieniach modułu", "No favorites – star a module in its settings")
                    : t("Nic nie znaleziono", "Nothing found"), gx + gw / 2f, cy + 2, DIM, Font.SEMIBOLD, 0.7f);
        }
        if (maxScroll > 0) {
            float th = Math.max(20, gh * gh / (contentH + 0.01f));
            float ty = gy + (gh - th) * (scroll / maxScroll);
            R2D.roundRect(ctx, gx + gw + 5, ty, 2, th, 1, 0x55FFFFFF);
        }
    }

    /** wiersz modułu: nazwa + opis; włączony – akcent po lewej, jaśniejsza nazwa i kropka stanu */
    private void drawCard(DrawContext ctx, Module m, float x, float y, float w, float h) {
        boolean inGrid = my >= gy && my < gy + gh && mx >= sideRight;
        boolean ov = inGrid && over(x, y, w, h) && drawer.raw() < 0.5f;
        if (ov) hoveredModule = m;
        float hv = anim("hv" + m.name).animate(ov || inspected == m ? 1 : 0);
        float on = state("on" + m.name, 18, m.isEnabled() ? 1 : 0);
        int acc = Theme.accent();
        R2D.roundRect(ctx, x, y, w, h, 6, R2D.blend(R2D.blend(0x05FFFFFF, 0x0FFFFFFF, hv), R2D.withAlpha(acc, 0.09f + 0.04f * hv), on));
        if (on > 0.01f) R2D.roundRect(ctx, x + 1.5f, y + 6, 2, h - 12, 1, R2D.withAlpha(acc, on));
        float tx = x + 9 + 3 * on;
        boolean binding = this.binding == m;
        String kn = binding ? "…" : keyName(m.key);
        float kw = kn.isEmpty() ? 0 : Math.max(13, Fonts.width(kn, Font.BOLD, 0.48f) + 7);
        float right = 16 + (kw > 0 ? kw + 5 : 0);
        int nameCol = R2D.blend(R2D.blend(0xFFBDBDC6, 0xFFFFFFFF, hv), 0xFFFFFFFF, on);
        String name = Fonts.trim(m.name, Font.BOLD, 0.7f, w - (tx - x) - right);
        Fonts.draw(ctx, name, tx, y + 5, nameCol, Font.BOLD, 0.7f);
        if (m.favorite) R2D.icon(ctx, "g_star", tx + Fonts.width(name, Font.BOLD, 0.7f) + 3, y + 5.5f, 5.5f, DIM);
        Fonts.draw(ctx, Fonts.trim(m.description(), Font.REGULAR, 0.54f, w - (tx - x) - right), tx, y + 15.5f, R2D.blend(FAINT, 0xFF8A8A96, hv), Font.REGULAR, 0.54f);
        // kropka stanu
        float dx = x + w - 10, dy = y + h / 2f;
        if (on > 0.01f) R2D.glow(ctx, dx - 7, dy - 7, 14, 14, R2D.withAlpha(acc, 0.5f * on));
        R2D.circle(ctx, dx, dy, 2.6f, R2D.blend(0x26FFFFFF, acc, on));
        if (kw > 0) {
            float kx = dx - 7 - kw, ky = y + h / 2f - 5.5f;
            int bg = binding ? R2D.withAlpha(acc, 0.35f + 0.25f * (float) Math.sin(System.currentTimeMillis() / 150.0)) : 0x16FFFFFF;
            R2D.roundRect(ctx, kx, ky, kw, 11, 3.5f, bg);
            Fonts.drawV(ctx, kn, kx + (kw - Fonts.width(kn, Font.BOLD, 0.48f)) / 2f, ky + 6, DIM, Font.BOLD, 0.48f);
        }
        hit(x, Math.max(y, gy), w, Math.min(y + h, gy + gh) - Math.max(y, gy), b -> {
            if (my < gy || my >= gy + gh) return;
            if (b == 0) { m.toggle(); click(); Modules.save(); }
            else if (b == 1) openDrawer(m);
            else if (b == 2) { this.binding = m; click(); }
        });
    }

    private void openDrawer(Module m) {
        if (inspected != m) dScroll = dScrollTarget = 0;
        inspected = m;
        lastInspected = m;
        click();
    }

    // ------------------------------------------------------------------ panel ustawień modułu

    private void drawDrawer(DrawContext ctx) {
        float da = drawer.animate(inspected != null && page == Page.MODULES ? 1 : 0);
        Module m = inspected != null ? inspected : lastInspected;
        if (da < 0.01f || m == null) return;
        float dw = Math.min(250, W * 0.42f);
        dx = x0 + W - dw * Anim.easeOutCubic(da);
        float dy = y0, dh = H;
        float prev = R2D.alpha;
        R2D.rect(ctx, x0 + 1, y0 + 1, W - dw * da - 1, H - 2, R2D.withAlpha(0xFF000000, 0.35f * da));
        hit(x0, y0, W - dw * da, H, b -> inspected = null);
        R2D.shadow(ctx, dx - 4, dy, dw, dh, 20, R2D.withAlpha(0xFF000000, 0.7f * da));
        R2D.roundRect(ctx, dx, dy, dw, dh, 18, 0xFC0C0C0E);
        R2D.roundOutline(ctx, dx, dy, dw, dh, 18, 0.6f, 0x2AFFFFFF);
        hit(dx, dy, dw, dh, b -> {});

        float px = dx + 14, pw = dw - 28, y = dy + 16;
        float on = state("on" + m.name, 18, m.isEnabled() ? 1 : 0);
        R2D.roundRectV(ctx, px, y, 32, 32, 10, R2D.blend(0xFF202024, Theme.accentTop(), on), R2D.blend(0xFF18181B, Theme.accentBottom(), on));
        R2D.icon(ctx, m.icon, px + 9, y + 9, 14, R2D.blend(0xFFD4D4D8, Theme.onAccent(), on));
        Fonts.draw(ctx, Fonts.trim(m.name, Font.BOLD, 1.0f, pw - 70), px + 42, y + 4, TEXT, Font.BOLD, 1.0f);
        String cat = m.category.title().toUpperCase(Locale.ROOT);
        float cw = Fonts.width(cat, Font.BOLD, 0.5f) + 10;
        R2D.roundRect(ctx, px + 42, y + 18, cw, 11, 5.5f, 0x1CFFFFFF);
        Fonts.drawV(ctx, cat, px + 47, y + 23.5f, DIM, Font.BOLD, 0.5f);
        float cbx = dx + dw - 14 - 20, cby = y + 6;
        float chv = hover("drawerClose", cbx, cby, 20, 20);
        R2D.roundRect(ctx, cbx, cby, 20, 20, 10, R2D.blend(0x14FFFFFF, 0xFFF4F4F5, chv));
        R2D.icon(ctx, "g_close", cbx + 6, cby + 6, 8, R2D.blend(TEXT, BLACK, chv));
        hit(cbx, cby, 20, 20, b -> inspected = null);
        y += 42;
        for (String l : Fonts.wrap(m.description(), Font.REGULAR, 0.66f, pw)) { Fonts.draw(ctx, l, px, y, DIM, Font.REGULAR, 0.66f); y += 10; }
        y += 6;

        float top = y, areaH = dy + dh - 40 - top;
        R2D.scissor(ctx, dx, top, dw, areaH);
        float cy = top - dScroll;
        cy = row(ctx, px, cy, pw, t("Włączony", "Enabled"));
        toggleAt(ctx, px + pw - 20, cy - 16, on, hover("rowOn", px, cy - 22, pw, 22));
        hit(px, cy - 22, pw, 22, b -> { if (b == 0) { m.toggle(); click(); Modules.save(); } });
        cy = row(ctx, px, cy, pw, t("Klawisz", "Keybind"));
        String kn = binding == m ? t("Naciśnij klawisz…", "Press a key…") : m.key < 0 ? t("Brak", "None") : keyName(m.key);
        float kw = Fonts.width(kn, Font.BOLD, 0.6f) + 14, kx = px + pw - kw, ky = cy - 17;
        float kh = anim("rowKey").animate(over(kx, ky, kw, 13) || binding == m ? 1 : 0);
        R2D.roundRect(ctx, kx, ky, kw, 13, 6.5f, R2D.blend(0x18FFFFFF, Theme.accent(), kh));
        Fonts.drawV(ctx, kn, kx + 7, ky + 7, R2D.blend(TEXT, Theme.onAccent(), kh), Font.BOLD, 0.6f);
        hit(px, cy - 22, pw, 22, b -> { binding = binding == m ? null : m; click(); });
        cy = row(ctx, px, cy, pw, t("Ulubiony", "Favorite"));
        float fv = state("fav" + m.name, 16, m.favorite ? 1 : 0);
        R2D.icon(ctx, "g_star", px + pw - 13, cy - 16, 11, R2D.blend(0x44FFFFFF, Theme.accent(), fv));
        hit(px, cy - 22, pw, 22, b -> { m.favorite = !m.favorite; click(); Modules.save(); });
        if (!m.settings.isEmpty()) {
            cy += 8;
            spaced(ctx, t("USTAWIENIA", "SETTINGS"), px, cy, FAINT, Font.BOLD, 0.5f, 1.6f);
            cy += 12;
            for (Setting<?> s : m.settings) cy = setting(ctx, m, s, px, cy, pw);
        }
        dMax = Math.max(0, cy + dScroll - top - areaH + 6);
        dScrollTarget = Math.max(0, Math.min(dMax, dScrollTarget));
        dScroll += (dScrollTarget - dScroll) * 0.3f;
        R2D.endScissor(ctx);

        float bh = 22, by = dy + dh - 32;
        button(ctx, px, by, pw, bh, "g_reset", t("Przywróć domyślne", "Reset to default"), false, hover("reset", px, by, pw, bh), true);
        hit(px, by, pw, bh, b -> { if (b == 0) { m.reset(); click(); Modules.save(); } });
        R2D.alpha = prev;
    }

    private float row(DrawContext ctx, float x, float y, float w, String label) {
        Fonts.drawV(ctx, label, x, y + 11, TEXT, Font.SEMIBOLD, 0.72f);
        R2D.rect(ctx, x, y + 22, w, 0.5f, 0x0DFFFFFF);
        return y + 22;
    }

    private float setting(DrawContext ctx, Module m, Setting<?> s, float x, float y, float w) {
        String key = m.name + "." + s.name;
        if (s instanceof Setting.Bool b) {
            float hv = hover("h" + key, x, y, w, 22);
            float on = state("b" + key, 18, b.get() ? 1 : 0);
            Fonts.drawV(ctx, s.name, x, y + 11, R2D.blend(DIM, TEXT, Math.max(hv, on)), Font.SEMIBOLD, 0.7f);
            toggleAt(ctx, x + w - 20, y + 5.5f, on, hv);
            hit(x, y, w, 22, bt -> { if (bt == 0) { b.toggle(); click(); Modules.save(); } });
            return y + 24;
        }
        if (s instanceof Setting.Num n) {
            return y + slider(ctx, key, x, y, w, s.name, n.text(), n.fraction(), f -> n.set(n.min + (n.max - n.min) * f));
        }
        if (s instanceof Setting.Mode o) {
            Fonts.drawV(ctx, s.name, x, y + 8, DIM, Font.SEMIBOLD, 0.7f);
            float h = chips(ctx, key, x, y + 18, w, o.modes.toArray(new String[0]), o.modes.indexOf(o.get()), i -> { o.set(o.modes.get(i)); Modules.save(); });
            return y + 18 + h + 7;
        }
        return y;
    }

    // ------------------------------------------------------------------ strona: motyw

    private interface CardBody { float draw(float x, float y, float w); }

    private void drawTheme(DrawContext ctx, float x, float w) {
        float mx0 = x + 14, mw = w - 28;
        header(ctx, mx0, mw, "g_theme", t("Motyw", "Theme"), t("Wygląd i zachowanie ClickGUI", "How the ClickGUI looks and behaves"));
        float top = y0 + 46, areaH = H - 46 - 10;
        int cols = mw >= 420 ? 2 : 1;
        float gap = 10, cw = (mw - gap * (cols - 1)) / cols;
        float[] colY = new float[cols];
        R2D.scissor(ctx, mx0 - 4, top - 2, mw + 8, areaH + 4);
        float off = -pScroll;
        Module m3 = Modules.get("3D GUI");
        Object[][] cards = {
                {"g_t_accent", t("Kolor akcentu", "Accent color"), t("Zaznaczenia, przełączniki i suwaki", "Selection, toggles and sliders"), (CardBody) (cx, cy, w2) -> {
                    float startY = cy;
                    float sx = cx;
                    for (int i = 0; i < Theme.ACCENTS.length; i++) {
                        float r = 7.5f, ccx = sx + r, ccy = cy + r + 2;
                        boolean sel = Theme.accentIndex == i;
                        float hv = hover("acc" + i, ccx - r, ccy - r, r * 2, r * 2), sa = state("accs" + i, 16, sel ? 1 : 0);
                        if (sa > 0.01f) R2D.circle(ctx, ccx, ccy, r + 2.6f * sa, R2D.withAlpha(0xFFFFFFFF, 0.9f * sa));
                        if (sa > 0.01f) R2D.circle(ctx, ccx, ccy, r + 1.2f * sa, 0xFF0C0C0E);
                        R2D.circle(ctx, ccx, ccy, r - hv * 0.0f, Theme.ACCENTS[i]);
                        if (sel) R2D.icon(ctx, "g_check", ccx - 4, ccy - 4, 8, Theme.contrast(Theme.ACCENTS[i]));
                        final int fi = i;
                        hit(ccx - r, ccy - r, r * 2, r * 2, b -> { if (b == 0) { Theme.accentIndex = fi; Theme.save(); click(); } });
                        sx += r * 2 + 6;
                        if (sx + r * 2 > cx + w2 && i < Theme.ACCENTS.length - 1) { sx = cx; cy += r * 2 + 6; }
                    }
                    String an = pl() ? Theme.ACCENT_PL[Theme.accentIndex] : Theme.ACCENT_EN[Theme.accentIndex];
                    Fonts.draw(ctx, an, cx, cy + 22, DIM, Font.SEMIBOLD, 0.62f);
                    return cy + 32 - startY;
                }},
                {"g_t_motion", t("Animacje", "GUI motion"), t("Szybkość animacji interfejsu", "Speed of interface animations"), (CardBody) (cx, cy, w2) -> {
                    String[] l = {t("Wolne", "Slow"), t("Normalne", "Normal"), t("Szybkie", "Fast"), t("Wyłączone", "Off")};
                    return chips(ctx, "motion", cx, cy, w2, l, Arrays.asList(Theme.MOTIONS).indexOf(Theme.motion), i -> { Theme.motion = Theme.MOTIONS[i]; Theme.save(); }) + 4;
                }},
                {"g_t_size", t("Rozmiar GUI", "GUI size"), t("Skala okna ClickGUI", "Scale of the ClickGUI window"), (CardBody) (cx, cy, w2) ->
                        slider(ctx, "scale", cx, cy, w2, t("Skala", "Scale"), Math.round(Theme.scale * 100) + " %", (Theme.scale - 0.8f) / 0.4f,
                                f -> { Theme.scale = Math.round((0.8f + 0.4f * f) * 20) / 20f; Theme.save(); })},
                {"g_t_sidebar", t("Pasek boczny", "Sidebar"), t("Kiedy pokazywać nazwy kategorii", "When to show category names"), (CardBody) (cx, cy, w2) -> {
                    String[] l = {t("Po najechaniu", "On hover"), t("Rozwinięty", "Expanded"), t("Zwinięty", "Collapsed")};
                    return chips(ctx, "sidebar", cx, cy, w2, l, Arrays.asList(Theme.SIDEBARS).indexOf(Theme.sidebar), i -> { Theme.sidebar = Theme.SIDEBARS[i]; Theme.save(); }) + 4;
                }},
                {"g_t_effects", t("Efekty", "Effects"), t("Tło, cień i dźwięki", "Background, shadow and sounds"), (CardBody) (cx, cy, w2) -> {
                    float yy = cy;
                    yy = themeToggle(ctx, "blur", cx, yy, w2, t("Rozmycie tła", "Background blur"), Theme.blur, () -> Theme.blur = !Theme.blur);
                    yy = themeToggle(ctx, "shadow", cx, yy, w2, t("Cień okna", "Window shadow"), Theme.shadow, () -> Theme.shadow = !Theme.shadow);
                    yy = themeToggle(ctx, "sounds", cx, yy, w2, t("Dźwięki interfejsu", "Interface sounds"), Theme.sounds, () -> Theme.sounds = !Theme.sounds);
                    yy += 4;
                    yy += slider(ctx, "dim", cx, yy, w2, t("Przyciemnienie tła", "Background dim"), Math.round(Theme.dim * 100) + " %", Theme.dim / 0.8f,
                            f -> { Theme.dim = Math.round(0.8f * f * 20) / 20f; Theme.save(); });
                    return yy - cy;
                }},
                {"g_m_3dgui", "3D GUI", t("Pochylony panel w przestrzeni (też w Wizualne → 3D GUI)", "Tilted panel in space (also in Visuals → 3D GUI)"), (CardBody) (cx, cy, w2) -> {
                    float yy = themeToggle(ctx, "3d", cx, cy, w2, t("Tryb 3D", "3D mode"), m3.isEnabled(), () -> { m3.toggle(); Modules.save(); });
                    if (m3.isEnabled()) {
                        Setting.Num tilt = (Setting.Num) m3.settings.get(0);
                        yy += slider(ctx, "tilt", cx, yy + 2, w2, t("Pochylenie", "Tilt"), tilt.text(), tilt.fraction(), f -> tilt.set(tilt.min + (tilt.max - tilt.min) * f)) + 2;
                    }
                    return yy - cy;
                }},
        };
        for (Object[] c : cards) {
            int ci = 0;
            for (int k = 1; k < cols; k++) if (colY[k] < colY[ci]) ci = k;
            float cx = mx0 + ci * (cw + gap), cy = top + colY[ci] + off;
            float h = themeCard(ctx, cx, cy, cw, (String) c[0], (String) c[1], (String) c[2], (CardBody) c[3]);
            colY[ci] += h + gap;
        }
        float contentH = 0;
        for (float v : colY) contentH = Math.max(contentH, v);
        pMax = Math.max(0, contentH - areaH);
        pScrollTarget = Math.max(0, Math.min(pMax, pScrollTarget));
        pScroll += (pScrollTarget - pScroll) * 0.3f;
        R2D.endScissor(ctx);
    }

    private float themeCard(DrawContext ctx, float x, float y, float w, String icon, String title, String desc, CardBody body) {
        float pad = 12;
        // najpierw liczymy wysokość: tło rysujemy pod spodem, więc rysujemy w dwóch przebiegach
        float hdr = 30;
        float bodyH = cardHeights.getOrDefault(title, 40f);
        float h = hdr + bodyH + pad;
        R2D.roundRect(ctx, x, y, w, h, 12, 0x0BFFFFFF);
        R2D.roundOutline(ctx, x, y, w, h, 12, 0.5f, 0x16FFFFFF);
        R2D.roundRect(ctx, x + pad, y + pad, 18, 18, 6, 0x16FFFFFF);
        R2D.icon(ctx, icon, x + pad + 4.5f, y + pad + 4.5f, 9, TEXT);
        Fonts.draw(ctx, title, x + pad + 26, y + pad + 1, TEXT, Font.BOLD, 0.76f);
        Fonts.draw(ctx, Fonts.trim(desc, Font.REGULAR, 0.58f, w - pad * 2 - 26), x + pad + 26, y + pad + 11, FAINT, Font.REGULAR, 0.58f);
        float used = body.draw(x + pad, y + hdr + pad, w - pad * 2);
        cardHeights.put(title, Math.max(16, used));
        return h;
    }
    private final Map<String, Float> cardHeights = new HashMap<>();

    private float themeToggle(DrawContext ctx, String key, float x, float y, float w, String label, boolean value, Runnable flip) {
        float hv = hover("tt" + key, x, y, w, 22);
        float on = state("tv" + key, 18, value ? 1 : 0);
        Fonts.drawV(ctx, label, x, y + 11, R2D.blend(DIM, TEXT, Math.max(hv, on)), Font.SEMIBOLD, 0.7f);
        toggleAt(ctx, x + w - 20, y + 5.5f, on, hv);
        hit(x, y, w, 22, b -> { if (b == 0) { flip.run(); Theme.save(); click(); } });
        return y + 24;
    }

    // ------------------------------------------------------------------ strona: konfiguracje

    private void drawConfigs(DrawContext ctx, float x, float w) {
        float mx0 = x + 14, mw = w - 28;
        List<Modules.Config> list = Modules.configs();
        header(ctx, mx0, mw, "g_configs", t("Konfiguracje", "Configs"), t(list.size() + " zapisanych, aktywna: " + Theme.config, list.size() + " saved, active: " + Theme.config));
        float bw = 108, bx = mx0 + mw - bw, by = y0 + 9;
        button(ctx, bx, by, bw, 20, "g_save", t("Zapisz nową", "Save new"), true, hover("cfgNew", bx, by, bw, 20), true);
        hit(bx, by, bw, 20, b -> {
            if (b != 0) return;
            String n = Modules.newConfigName();
            Modules.saveConfig(n);
            Theme.config = n; Theme.save();
            click();
        });
        float top = y0 + 46, areaH = H - 46 - 10, rh = 36, gap = 5;
        R2D.scissor(ctx, mx0 - 4, top - 2, mw + 8, areaH + 4);
        float y = top - pScroll;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pl() ? "d MMM yyyy, HH:mm" : "MMM d, yyyy, HH:mm", pl() ? Locale.forLanguageTag("pl-PL") : Locale.ENGLISH);
        for (Modules.Config c : list) {
            boolean active = c.name().equals(Theme.config);
            float hv = hover("cfg" + c.name(), mx0, y, mw, rh);
            R2D.roundRect(ctx, mx0, y, mw, rh, 11, R2D.blend(0x0AFFFFFF, 0x14FFFFFF, hv));
            R2D.roundOutline(ctx, mx0, y, mw, rh, 11, 0.6f, active ? R2D.withAlpha(Theme.accent(), 0.6f) : R2D.blend(0x12FFFFFF, 0x24FFFFFF, hv));
            R2D.roundRectV(ctx, mx0 + 8, y + 7, 26, 26, 8, active ? Theme.accentTop() : 0xFF202024, active ? Theme.accentBottom() : 0xFF18181B);
            R2D.icon(ctx, "g_configs", mx0 + 15, y + 12, 12, active ? Theme.onAccent() : 0xFFD4D4D8);
            Fonts.draw(ctx, c.name(), mx0 + 44, y + 7, TEXT, Font.BOLD, 0.8f);
            float nw = Fonts.width(c.name(), Font.BOLD, 0.8f);
            if (active) {
                String tag = t("AKTYWNA", "ACTIVE");
                float tw = Fonts.width(tag, Font.BOLD, 0.48f) + 10;
                R2D.roundRect(ctx, mx0 + 44 + nw + 6, y + 7, tw, 10, 5, Theme.accent());
                Fonts.drawV(ctx, tag, mx0 + 44 + nw + 11, y + 12, Theme.onAccent(), Font.BOLD, 0.48f);
            }
            Fonts.draw(ctx, t("Zapisano ", "Saved ") + fmt.format(Instant.ofEpochMilli(c.modified()).atZone(ZoneId.systemDefault())), mx0 + 44, y + 20, FAINT, Font.REGULAR, 0.58f);
            // przyciski
            float rx = mx0 + mw - 8;
            boolean armed = c.name().equals(deleteName) && System.currentTimeMillis() - deleteArm < 3000;
            float dw = armed ? Fonts.width(t("Na pewno?", "Sure?"), Font.BOLD, 0.6f) + 20 : 24;
            rx -= dw;
            float dh = hover("cfgDel" + c.name(), rx, y + 7, dw, 22);
            if (armed) {
                R2D.roundRect(ctx, rx, y + 7, dw, 22, 11, R2D.blend(0xFFDC2626, 0xFFEF4444, dh));
                Fonts.drawV(ctx, t("Na pewno?", "Sure?"), rx + 10, y + 18.5f, 0xFFFFFFFF, Font.BOLD, 0.6f);
            } else button(ctx, rx, y + 7, dw, 22, "g_trash", null, false, dh, true);
            hit(rx, y + 7, dw, 22, b -> {
                if (b != 0) return;
                if (armed) { Modules.deleteConfig(c.name()); deleteName = null; if (Theme.config.equals(c.name())) { Theme.config = "default"; Theme.save(); } }
                else { deleteName = c.name(); deleteArm = System.currentTimeMillis(); }
                click();
            });
            rx -= 30;
            button(ctx, rx, y + 7, 24, 22, "g_save", null, false, hover("cfgSave" + c.name(), rx, y + 7, 24, 22), true);
            hit(rx, y + 7, 24, 22, b -> { if (b == 0) { Modules.saveConfig(c.name()); Theme.config = c.name(); Theme.save(); click(); } });
            float lw = Fonts.width(t("Wczytaj", "Load"), Font.BOLD, 0.7f) + 34;
            rx -= lw + 6;
            button(ctx, rx, y + 7, lw, 22, "g_load", t("Wczytaj", "Load"), !active, hover("cfgLoad" + c.name(), rx, y + 7, lw, 22), true);
            hit(rx, y + 7, lw, 22, b -> { if (b == 0) { Modules.loadConfig(c.name()); Theme.config = c.name(); Theme.save(); click(); } });
            y += rh + gap;
        }
        if (list.isEmpty()) {
            float cy = top + areaH / 2f;
            R2D.icon(ctx, "g_configs", mx0 + mw / 2f - 11, cy - 26, 22, FAINT);
            Fonts.drawCentered(ctx, t("Brak zapisanych konfiguracji – kliknij „Zapisz nową”", "No saved configs – click \"Save new\""), mx0 + mw / 2f, cy + 4, DIM, Font.SEMIBOLD, 0.74f);
        }
        pMax = Math.max(0, list.size() * (rh + gap) - gap - areaH);
        pScrollTarget = Math.max(0, Math.min(pMax, pScrollTarget));
        pScroll += (pScrollTarget - pScroll) * 0.3f;
        R2D.endScissor(ctx);
    }

    // ------------------------------------------------------------------ strona: znajomi

    private void drawFriends(DrawContext ctx, float x, float w) {
        float mx0 = x + 14, mw = w - 28;
        List<String> list = Friends.all();
        header(ctx, mx0, mw, "g_friends", t("Znajomi", "Friends"), t(list.size() + (list.size() == 1 ? " znajomy" : " znajomych"), list.size() + (list.size() == 1 ? " friend" : " friends")));
        float top = y0 + 46;
        float bw = 90, fw = mw - bw - 8;
        field(ctx, 2, mx0, top, fw, 22, friendInput, t("Wpisz nick gracza…", "Type a player name…"), "g_userplus");
        button(ctx, mx0 + fw + 8, top, bw, 22, "g_userplus", t("Dodaj", "Add"), true, hover("frAdd", mx0 + fw + 8, top, bw, 22), !friendInput.isBlank());
        hit(mx0 + fw + 8, top, bw, 22, b -> addFriend());
        top += 32;
        float areaH = y0 + H - 12 - top, rh = 32, gap = 5;
        int cols = mw >= 420 ? 2 : 1;
        float cw = (mw - gap * (cols - 1)) / cols;
        R2D.scissor(ctx, mx0 - 4, top - 2, mw + 8, areaH + 4);
        for (int i = 0; i < list.size(); i++) {
            String f = list.get(i);
            float cx = mx0 + (i % cols) * (cw + gap), cy = top + (i / cols) * (rh + gap) - pScroll;
            float hv = hover("fr" + f, cx, cy, cw, rh);
            R2D.roundRect(ctx, cx, cy, cw, rh, 10, R2D.blend(0x0AFFFFFF, 0x14FFFFFF, hv));
            R2D.roundOutline(ctx, cx, cy, cw, rh, 10, 0.5f, R2D.blend(0x12FFFFFF, 0x24FFFFFF, hv));
            monogram(ctx, cx + 6, cy + 5, 22, f);
            Fonts.drawV(ctx, f, cx + 36, cy + rh / 2f + 0.5f, TEXT, Font.BOLD, 0.76f);
            float rx = cx + cw - 28;
            float dh = hover("frDel" + f, rx, cy + 6, 20, 20);
            R2D.roundRect(ctx, rx, cy + 6, 20, 20, 10, R2D.blend(0x00FFFFFF, 0xFFDC2626, dh));
            R2D.icon(ctx, "g_trash", rx + 5.5f, cy + 11.5f, 9, R2D.blend(R2D.withAlpha(0xFFFFFFFF, 0.25f + 0.5f * hv), 0xFFFFFFFF, dh));
            hit(rx, cy + 6, 20, 20, b -> { if (b == 0) { Friends.remove(f); click(); } });
        }
        if (list.isEmpty()) {
            float cy = top + areaH / 2f;
            R2D.icon(ctx, "g_friends", mx0 + mw / 2f - 11, cy - 26, 22, FAINT);
            Fonts.drawCentered(ctx, t("Nie masz jeszcze znajomych – wpisz nick i kliknij „Dodaj”", "No friends yet – type a name and click \"Add\""), mx0 + mw / 2f, cy + 4, DIM, Font.SEMIBOLD, 0.74f);
        }
        int rows = (list.size() + cols - 1) / cols;
        pMax = Math.max(0, rows * (rh + gap) - gap - areaH);
        pScrollTarget = Math.max(0, Math.min(pMax, pScrollTarget));
        pScroll += (pScrollTarget - pScroll) * 0.3f;
        R2D.endScissor(ctx);
    }

    private void addFriend() {
        if (Friends.add(friendInput)) { friendInput = ""; click(); }
    }

    // ================================================================== wejście

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float[] l = toLocal(mouseX, mouseY);
        mx = (int) Math.floor(l[0]); my = (int) Math.floor(l[1]);
        if (closeTime >= 0) return true;
        if (binding != null && button != 2) binding = null;
        focus = 0;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.in(mx, my)) { h.action().accept(button); return true; }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dX, double dY) {
        float[] l = toLocal(mouseX, mouseY);
        mx = (int) Math.floor(l[0]); my = (int) Math.floor(l[1]);
        if (dragApply != null) dragApply.run();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) {
            if (dragApply != null) dragApply.run();
            dragging = null; dragApply = null; dragKey = null;
            Modules.save(); Theme.save();
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        float[] l = toLocal(mouseX, mouseY);
        if (page == Page.MODULES && inspected != null && l[0] >= dx) dScrollTarget = Math.max(0, Math.min(dMax, dScrollTarget - (float) v * 26));
        else if (page == Page.MODULES) scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget - (float) v * 30));
        else pScrollTarget = Math.max(0, Math.min(pMax, pScrollTarget - (float) v * 30));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding != null) {
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                binding.key = keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE ? -1 : keyCode;
                Modules.save();
            }
            binding = null;
            click();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (focus != 0 || !query.isEmpty()) { query = ""; focus = 0; listTime = System.currentTimeMillis(); }
            else if (inspected != null) inspected = null;
            else close();
            return true;
        }
        if (focus == 1 && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!query.isEmpty()) { query = query.substring(0, query.length() - 1); listTime = System.currentTimeMillis(); }
            return true;
        }
        if (focus == 2) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !friendInput.isEmpty()) friendInput = friendInput.substring(0, friendInput.length() - 1);
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) addFriend();
            return true;
        }
        if (VescaClient.OPEN_GUI != null && VescaClient.OPEN_GUI.matchesKey(keyCode, scanCode)) { close(); return true; }
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (binding != null || chr < ' ' || chr == 127) return true;
        if (focus == 2) {
            if ((Character.isLetterOrDigit(chr) || chr == '_') && friendInput.length() < 16) friendInput += chr;
            return true;
        }
        if (page != Page.MODULES) return true;
        if (focus != 1 && !Character.isLetterOrDigit(chr)) return true;
        focus = 1;
        if (query.length() < 24) { query += chr; scroll = scrollTarget = 0; listTime = System.currentTimeMillis(); }
        return true;
    }

    @Override
    public void close() {
        if (closeTime < 0) { closeTime = System.currentTimeMillis(); Modules.save(); Theme.save(); }
    }
}
