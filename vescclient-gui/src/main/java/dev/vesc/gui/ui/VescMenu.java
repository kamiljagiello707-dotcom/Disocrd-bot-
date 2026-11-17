package dev.vesc.gui.ui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.vesc.gui.VescGui;
import dev.vesc.gui.render.Fonts;
import dev.vesc.gui.render.Fonts.Font;
import dev.vesc.gui.render.R2D;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.client.render.*;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Menu główne "Aurora".
 * <ul>
 *     <li>tło: nocne niebo z gwiazdami i spadającymi gwiazdami, animowana zorza polarna (3 wstęgi),
 *     dwie warstwy gór z bloków na dole i unoszące się świetliki – wszystko z paralaksą od myszy</li>
 *     <li>po lewej: logo i lista (Singleplayer, Multiplayer, Realms, Opcje, Wyjście) z przesuwanym podświetleniem,
 *     obsługa klawiatury ↑ ↓ Enter</li>
 *     <li>po prawej: szklana karta z głową skina, powitaniem, dużym zegarem i listą nowości</li>
 * </ul>
 */
public class VescMenu extends Screen {
    // ------------------------------------------------------------------ paleta
    private static final int TEAL = 0xFF3EE6C1, VIOLET = 0xFF8B5CF6, PINK = 0xFFE879F9, BLUE = 0xFF5B8CFF;
    private static final int TEXT = 0xFFF2F3F8, DIM = 0xFFA3A8BC, FAINT = 0xFF60657A, RED = 0xFFFF5470;

    /** start animacji tła – wspólny dla kolejnych otwarć, żeby zorza nie "skakała" */
    private static long epoch = 0;

    private final long openTime = System.currentTimeMillis();
    private final float[][] stars = new float[150][];
    private final float[][] motes = new float[28][];
    private final Random rnd = new Random();
    private long lastFrame = System.nanoTime();
    private float pmx, pmy;
    private float[] shoot; // x, y, vx, vy, życie (0..1)
    private long nextShoot = System.currentTimeMillis() + 2500;

    private static final String[] ICONS = {"player", "users", "sparkles", "settings", "power"};
    private final Anim selY = new Anim(-1, 18), selA = new Anim(0, 14);
    private final Anim[] rowAnim = new Anim[ICONS.length];
    private int hovered = -1, keySel = -1;
    private float lastMx = -1, lastMy = -1;

    private record Hit(float x, float y, float w, float h, Runnable action) {
        boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }
    private final List<Hit> hits = new ArrayList<>();
    private final Anim[] smallHover = {new Anim(0, 16), new Anim(0, 16), new Anim(0, 16)};

    public VescMenu() {
        super(Text.literal("VescClient"));
        if (epoch == 0) epoch = System.currentTimeMillis();
        for (int i = 0; i < stars.length; i++)
            stars[i] = new float[]{rnd.nextFloat(), (float) Math.pow(rnd.nextFloat(), 1.4) * 0.72f, 0.35f + rnd.nextFloat() * (rnd.nextFloat() < 0.12f ? 1.1f : 0.45f),
                    rnd.nextFloat() * 6.28f, 0.6f + rnd.nextFloat() * 2.2f};
        for (int i = 0; i < motes.length; i++) motes[i] = newMote(true);
        for (int i = 0; i < rowAnim.length; i++) rowAnim[i] = new Anim(0, 16);
    }

    private float[] newMote(boolean anywhere) {
        return new float[]{rnd.nextFloat(), anywhere ? 0.55f + rnd.nextFloat() * 0.45f : 1.02f,
                (rnd.nextFloat() - 0.5f) * 0.012f, -0.006f - rnd.nextFloat() * 0.012f, 0.6f + rnd.nextFloat() * 1.4f, rnd.nextFloat() * 6.28f};
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    private boolean pl() { return client != null && client.options.language.startsWith("pl"); }

    private String label(int i) {
        boolean pl = pl();
        return switch (i) {
            case 0 -> pl ? "Tryb jednoosobowy" : "Singleplayer";
            case 1 -> pl ? "Tryb wieloosobowy" : "Multiplayer";
            case 2 -> "Minecraft Realms";
            case 3 -> pl ? "Ustawienia" : "Options";
            default -> pl ? "Wyjdź z gry" : "Quit Game";
        };
    }

    private String desc(int i) {
        boolean pl = pl();
        return switch (i) {
            case 0 -> pl ? "Graj na swoich światach" : "Play on your own worlds";
            case 1 -> pl ? "Dołącz do serwera" : "Join a server";
            case 2 -> pl ? "Twoje serwery Realms" : "Your Realms servers";
            case 3 -> pl ? "Grafika, dźwięk, sterowanie" : "Video, sound and controls";
            default -> pl ? "Do zobaczenia!" : "See you soon!";
        };
    }

    // ================================================================== tło

    private static float sin(double v) { return (float) Math.sin(v); }

    private static int lerpColor(int a, int b, float t) { return R2D.blend(a, b, t); }

    /** nocne niebo: pionowy gradient + delikatna mgławica przy horyzoncie */
    private void sky(DrawContext ctx) {
        R2D.gradientV(ctx, 0, 0, width, height, 0xFF04050A, 0xFF0B0E1D);
        R2D.glow(ctx, -width * 0.2f, height * 0.25f, width * 0.8f, height * 0.8f, R2D.withAlpha(VIOLET, 0.07f));
        R2D.glow(ctx, width * 0.45f, height * 0.15f, width * 0.8f, height * 0.75f, R2D.withAlpha(TEAL, 0.05f));
    }

    private void stars(DrawContext ctx, float t, float ox, float oy) {
        ctx.draw();
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (float[] s : stars) {
            float tw = 0.5f + 0.5f * sin(t * s[4] + s[3]);
            float x = s[0] * width + ox * 0.012f, y = s[1] * height + oy * 0.012f, r = s[2] * 0.5f;
            int c = R2D.withAlpha(s[2] > 0.8f ? 0xFFDDE6FF : 0xFFFFFFFF, (0.18f + 0.62f * tw) * R2D.alpha);
            b.vertex(m, x - r, y - r, 0).color(c);
            b.vertex(m, x - r, y + r, 0).color(c);
            b.vertex(m, x + r, y + r, 0).color(c);
            b.vertex(m, x + r, y - r, 0).color(c);
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
        RenderSystem.defaultBlendFunc();
        // jaśniejsze gwiazdy z poświatą
        for (int i = 0; i < stars.length; i += 11) {
            float[] s = stars[i];
            float tw = 0.5f + 0.5f * sin(t * s[4] + s[3]);
            float x = s[0] * width + ox * 0.012f, y = s[1] * height + oy * 0.012f;
            R2D.glow(ctx, x - 5, y - 5, 10, 10, R2D.withAlpha(0xFFE6ECFF, 0.25f * tw));
        }
    }

    private void shootingStar(DrawContext ctx, float dt) {
        long now = System.currentTimeMillis();
        if (shoot == null && now > nextShoot) {
            float sx = width * (0.25f + rnd.nextFloat() * 0.7f), sy = height * (0.04f + rnd.nextFloat() * 0.22f);
            float sp = 260 + rnd.nextFloat() * 160;
            shoot = new float[]{sx, sy, -sp * 0.86f, sp * 0.5f, 0};
            nextShoot = now + 4000 + rnd.nextInt(6000);
        }
        if (shoot == null) return;
        shoot[4] += dt / 0.9f;
        shoot[0] += shoot[2] * dt; shoot[1] += shoot[3] * dt;
        if (shoot[4] >= 1) { shoot = null; return; }
        float life = shoot[4], a = sin(Math.PI * life);
        float len = 70 * a + 6;
        float vx = shoot[2], vy = shoot[3], vl = (float) Math.sqrt(vx * vx + vy * vy);
        float dx = vx / vl, dy = vy / vl, px = -dy, py = dx, w = 0.7f;
        float hx = shoot[0], hy = shoot[1], tx = hx - dx * len, ty = hy - dy * len;
        ctx.draw();
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int head = R2D.withAlpha(0xFFFFFFFF, a * R2D.alpha), tail = R2D.withAlpha(0xFFB9C8FF, 0);
        b.vertex(m, hx + px * w, hy + py * w, 0).color(head);
        b.vertex(m, tx, ty, 0).color(tail);
        b.vertex(m, tx, ty, 0).color(tail);
        b.vertex(m, hx - px * w, hy - py * w, 0).color(head);
        BufferRenderer.drawWithGlobalProgram(b.end());
        RenderSystem.defaultBlendFunc();
        R2D.glow(ctx, hx - 6, hy - 6, 12, 12, R2D.withAlpha(0xFFFFFFFF, 0.6f * a));
    }

    /** zorza polarna: trzy falujące wstęgi z pionowymi "promieniami", mieszane addytywnie */
    private void aurora(DrawContext ctx, float t, float ox, float intro) {
        ctx.draw();
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        int[][] pal = {{TEAL, 0xFF34D399}, {VIOLET, PINK}, {BLUE, TEAL}};
        float[] base = {0.30f, 0.39f, 0.24f};
        float[] inten = {0.5f, 0.4f, 0.26f};
        float rise = 18 * (1 - intro);
        for (int k = 0; k < 3; k++) {
            float par = ox * (0.025f + k * 0.012f);
            float step = 3;
            // kurtyna (od dolnej krawędzi w górę)
            BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            BufferBuilder e = null;
            float[] ys = new float[(int) ((width + 80) / step) + 2];
            float[] as = new float[ys.length];
            int[] cs = new int[ys.length];
            int n = 0;
            for (float x = -40; x <= width + 40; x += step, n++) {
                double xx = x + par;
                float y = height * base[k] + rise + 20 * sin(xx * 0.0055 + t * 0.22 + k * 1.7) + 9 * sin(xx * 0.017 - t * 0.38 + k)
                        + 4 * sin(xx * 0.041 + t * 0.6 + k * 3);
                float hh = height * (0.17f + 0.05f * k) * (0.62f + 0.38f * sin(xx * 0.0105 + t * 0.31 + k * 2.1));
                float rays = 0.5f + 0.5f * sin(xx * 0.085 + t * 0.7 + k) * sin(xx * 0.029 - t * 0.43 + k * 0.5);
                float al = inten[k] * intro * R2D.alpha * (0.35f + 0.65f * rays) * (0.55f + 0.45f * sin(xx * 0.0042 + t * 0.19 + k));
                int c = lerpColor(pal[k][0], pal[k][1], 0.5f + 0.5f * sin(xx * 0.0031 + t * 0.14 + k * 1.3));
                b.vertex(m, x, y - hh, 0).color(R2D.withAlpha(c, 0));
                b.vertex(m, x, y, 0).color(R2D.withAlpha(c, al));
                ys[n] = y; as[n] = al; cs[n] = c;
            }
            BufferRenderer.drawWithGlobalProgram(b.end());
            // jasna dolna krawędź wstęgi
            e = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            float x = -40;
            for (int i = 0; i < n; i++, x += step) {
                int c = R2D.blend(cs[i], 0xFFFFFFFF, 0.35f);
                e.vertex(m, x, ys[i] - 2, 0).color(R2D.withAlpha(c, as[i] * 0.9f));
                e.vertex(m, x, ys[i] + 9, 0).color(R2D.withAlpha(c, 0));
            }
            BufferRenderer.drawWithGlobalProgram(e.end());
        }
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
    }

    /** wysokość kolumny gór (w blokach) – deterministyczny "szum" z sinusów */
    private static float hill(int i, int layer) {
        double s = layer * 17.3;
        return (float) (Math.sin(i * 0.19 + s) * 2.2 + Math.sin(i * 0.071 + s * 2) * 4.0 + Math.sin(i * 0.53 + s * 3) * 0.9 + Math.sin(i * 0.013 + s) * 3.0);
    }

    private void terrain(DrawContext ctx, float ox, int layer, float intro) {
        float B = layer == 0 ? 6 : 8;
        float base = height * (layer == 0 ? 0.79f : 0.88f) + (1 - intro) * (layer == 0 ? 12 : 22);
        float par = ox * (layer == 0 ? 0.05f : 0.1f);
        int top = layer == 0 ? 0xFF0F1226 : 0xFF07080F;
        int bot = layer == 0 ? 0xFF0B0D1B : 0xFF050609;
        ctx.draw();
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int i0 = (int) Math.floor((-par) / B) - 2, i1 = i0 + (int) (width / B) + 5;
        int rim = R2D.withAlpha(layer == 0 ? VIOLET : TEAL, (layer == 0 ? 0.16f : 0.12f) * R2D.alpha);
        for (int i = i0; i <= i1; i++) {
            float x = i * B + par;
            float hgt = Math.max(0, Math.round(hill(i, layer) + 5)) * B;
            float y = base - hgt;
            int ct = R2D.col(top), cb = R2D.col(bot);
            b.vertex(m, x, y, 0).color(ct);
            b.vertex(m, x, height, 0).color(cb);
            b.vertex(m, x + B, height, 0).color(cb);
            b.vertex(m, x + B, y, 0).color(ct);
            // poświata zorzy na górnej krawędzi bloków
            b.vertex(m, x, y, 0).color(rim);
            b.vertex(m, x, y + 1, 0).color(rim);
            b.vertex(m, x + B, y + 1, 0).color(rim);
            b.vertex(m, x + B, y, 0).color(rim);
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
        if (layer == 0) {
            // mgła między warstwami
            R2D.gradientV(ctx, 0, base - 30, width, 60, R2D.withAlpha(0xFF141833, 0f), R2D.withAlpha(0xFF141833, 0.55f));
        }
    }

    private void motes(DrawContext ctx, float t, float dt, float ox) {
        for (int i = 0; i < motes.length; i++) {
            float[] p = motes[i];
            p[0] += p[2] * dt; p[1] += p[3] * dt;
            if (p[1] < 0.45f) motes[i] = p = newMote(false);
            float life = Anim.clamp01((1.02f - p[1]) / 0.12f) * Anim.clamp01((p[1] - 0.45f) / 0.2f);
            float tw = 0.5f + 0.5f * sin(t * 2.3f + p[5]);
            float x = p[0] * width + ox * 0.08f + 6 * sin(t * 0.7f + p[5]), y = p[1] * height;
            int c = i % 3 == 0 ? VIOLET : TEAL;
            float s = 3 + p[4] * 2.5f;
            R2D.glow(ctx, x - s, y - s, s * 2, s * 2, R2D.withAlpha(c, 0.55f * life * (0.4f + 0.6f * tw)));
            R2D.circle(ctx, x, y, p[4] * 0.45f, R2D.withAlpha(0xFFFFFFFF, 0.8f * life * tw));
        }
    }

    // ================================================================== render

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hits.clear();
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        float t = (System.currentTimeMillis() - epoch) / 1000f;
        float intro = Anim.easeOutCubic((System.currentTimeMillis() - openTime) / 1100f);
        float k = 1 - (float) Math.exp(-4 * dt);
        pmx += ((mouseX - width / 2f) - pmx) * k;
        pmy += ((mouseY - height / 2f) - pmy) * k;
        float ox = -pmx, oy = -pmy;

        R2D.alpha = 1;
        sky(ctx);
        stars(ctx, t, ox, oy);
        shootingStar(ctx, dt);
        aurora(ctx, t, ox, intro);
        terrain(ctx, ox, 0, intro);
        motes(ctx, t, dt, ox);
        terrain(ctx, ox, 1, intro);
        // winieta
        R2D.gradientV(ctx, 0, 0, width, height * 0.25f, 0x99000000, 0x00000000);
        R2D.gradientH(ctx, 0, 0, width * 0.45f, height, 0x88000000, 0x00000000);
        // wejście z czerni
        if (intro < 1) R2D.rect(ctx, 0, 0, width, height, R2D.withAlpha(0xFF000000, 1 - Anim.easeOutCubic((System.currentTimeMillis() - openTime) / 600f)));

        if (mouseX != lastMx || mouseY != lastMy) { keySel = -1; lastMx = mouseX; lastMy = mouseY; }
        drawLeft(ctx, mouseX, mouseY);
        if (width >= 470) drawCard(ctx, mouseX, mouseY);
        drawBottom(ctx, mouseX, mouseY);
        R2D.alpha = 1;
    }

    // ------------------------------------------------------------------ lewa kolumna

    private float rowH() { return Math.max(24, Math.min(34, (height - 150) / 5.6f)); }

    private void drawLeft(DrawContext ctx, int mx, int my) {
        long since = System.currentTimeMillis() - openTime;
        float x = Math.max(22, width * 0.075f);
        float rh = rowH(), rw = Math.min(250, width * 0.42f);
        boolean big = height >= 300;
        float logoH = big ? 62 : 48, gap = big ? 20 : 12;
        float total = logoH + gap + ICONS.length * rh;
        float y = Math.max(16, (height - 26 - total) / 2f);

        // ---- logo
        float la = Anim.easeOutCubic((since - 150) / 700f);
        R2D.alpha = la;
        float ly = y + (1 - la) * 10;
        float ms = big ? 34 : 28;
        R2D.glow(ctx, x - 14, ly - 14, ms + 28, ms + 28, R2D.withAlpha(VIOLET, 0.5f));
        R2D.roundRectH(ctx, x, ly, ms, ms, ms * 0.32f, TEAL, VIOLET);
        R2D.roundOutline(ctx, x, ly, ms, ms, ms * 0.32f, 0.6f, R2D.withAlpha(0xFFFFFFFF, 0.35f));
        R2D.icon(ctx, "logo_v", x + ms * 0.22f, ly + ms * 0.22f, ms * 0.56f, 0xFF0A0B12);
        float tx = x + ms + 12;
        float ds = (big ? 1.0f : 0.8f) * 0.62f;
        // wordmark: VESC (duży) + CLIENT (rozstrzelony), pod spodem wersje
        float capH = Fonts.displayCap(ds);
        float top = ly + (ms - (capH + 6 + 5.5f)) / 2f;
        drawDisplayLeft(ctx, "VESC", tx, top, TEXT, ds);
        float vw = Fonts.width("VESC", Font.DISPLAY, ds);
        spaced(ctx, "CLIENT", tx + vw + 7, top + capH / 2f - 3, R2D.blend(TEAL, VIOLET, 0.5f + 0.5f * sin(System.currentTimeMillis() / 900.0)), Font.BOLD, 0.62f, 2.2f);
        Fonts.draw(ctx, "Minecraft " + SharedConstants.getGameVersion().getName() + "   •   GUI " + VescGui.VERSION, tx, top + capH + 6, FAINT, Font.SEMIBOLD, 0.58f);
        R2D.alpha = 1;

        // ---- lista
        float listY = y + logoH + gap;
        int over = -1;
        for (int i = 0; i < ICONS.length; i++) if (mx >= x && mx < x + rw && my >= listY + i * rh && my < listY + (i + 1) * rh) over = i;
        hovered = over;
        int sel = keySel >= 0 ? keySel : over;
        // przesuwane podświetlenie
        float sa = selA.animate(sel >= 0 ? 1 : 0);
        if (sel >= 0 && selY.raw() < 0) selY.snap(listY + sel * rh);
        float sy = selY.animate(sel >= 0 ? listY + sel * rh : selY.target());
        if (sa > 0.01f && selY.raw() >= 0) {
            boolean quit = sel == ICONS.length - 1 || (sel < 0 && Math.abs(sy - (listY + (ICONS.length - 1) * rh)) < 2);
            int c1 = quit ? RED : TEAL, c2 = quit ? 0xFFFF8A5B : VIOLET;
            R2D.alpha = sa;
            R2D.glow(ctx, x - 20, sy - 10, rw * 0.7f, rh + 20, R2D.withAlpha(c1, 0.16f));
            R2D.roundRectH(ctx, x, sy + 1, rw, rh - 2, (rh - 2) / 2f, R2D.withAlpha(c1, 0.20f), R2D.withAlpha(c2, 0.03f));
            R2D.roundOutlineGrad(ctx, x, sy + 1, rw, rh - 2, (rh - 2) / 2f, 0.6f, R2D.withAlpha(c1, 0.55f), R2D.withAlpha(c2, 0.05f));
            R2D.roundRect(ctx, x + 5, sy + rh / 2f - 6, 2.5f, 12, 1.25f, c1);
            R2D.alpha = 1;
        }
        for (int i = 0; i < ICONS.length; i++) {
            final int idx = i;
            float ry = listY + i * rh;
            float e = Anim.easeOutCubic((since - 260 - i * 70) / 520f);
            float h = rowAnim[i].animate(i == sel ? 1 : 0);
            boolean quit = i == ICONS.length - 1;
            R2D.alpha = e;
            float rx = x + (1 - e) * -18 + h * 4;
            // numer
            Fonts.drawV(ctx, String.format("%02d", i + 1), rx + 14, ry + rh / 2f + 0.5f, R2D.blend(FAINT, quit ? RED : TEAL, h), Font.SEMIBOLD, 0.56f);
            // ikona
            int ic = R2D.blend(DIM, quit ? 0xFFFFB3C0 : TEXT, h);
            R2D.icon(ctx, ICONS[i], rx + 34, ry + rh / 2f - 5, 10, ic);
            // napis + opis
            float lx = rx + 52;
            float lift = h * (rh >= 30 ? 4.5f : 3.5f);
            Fonts.drawV(ctx, label(i), lx, ry + rh / 2f + 0.5f - lift, R2D.blend(0xFFC9CDDB, TEXT, h), Font.BOLD, rh >= 30 ? 0.9f : 0.8f);
            if (h > 0.02f) {
                R2D.alpha = e * h;
                Fonts.drawV(ctx, desc(i), lx, ry + rh / 2f + 0.5f + (rh >= 30 ? 6.5f : 5f), quit ? 0xFFE7A1AE : DIM, Font.REGULAR, 0.58f);
                // strzałka
                R2D.icon(ctx, "arrow_right", x + rw - 22 + (1 - h) * -8, ry + rh / 2f - 4.5f, 9, quit ? RED : TEAL);
            }
            R2D.alpha = 1;
            hits.add(new Hit(x, ry, rw, rh, () -> open(idx)));
        }
        // podpowiedź klawiszy
        R2D.alpha = Anim.easeOutCubic((since - 800) / 600f);
        float hy = listY + ICONS.length * rh + 10;
        if (hy + 10 < height - 28) {
            keyCap(ctx, "↑", x + 6, hy);
            keyCap(ctx, "↓", x + 22, hy);
            keyCap(ctx, "Enter", x + 38, hy);
            Fonts.drawV(ctx, pl() ? "nawigacja klawiaturą" : "keyboard navigation", x + 38 + Fonts.width("Enter", Font.BOLD, 0.52f) + 14, hy + 6, FAINT, Font.REGULAR, 0.56f);
        }
        R2D.alpha = 1;
    }

    private void keyCap(DrawContext ctx, String k, float x, float y) {
        float w = Math.max(12, Fonts.width(k, Font.BOLD, 0.52f) + 8);
        R2D.roundRect(ctx, x, y, w, 12, 3.5f, 0x1AFFFFFF);
        R2D.roundOutline(ctx, x, y, w, 12, 3.5f, 0.5f, 0x22FFFFFF);
        Fonts.drawV(ctx, k, x + (w - Fonts.width(k, Font.BOLD, 0.52f)) / 2f, y + 6.5f, DIM, Font.BOLD, 0.52f);
    }

    /** duży napis wyrównany do lewej (górna krawędź wielkich liter = topY) */
    private static void drawDisplayLeft(DrawContext ctx, String s, float x, float topY, int color, float scale) {
        Fonts.drawDisplay(ctx, s, x + Fonts.width(s, Font.DISPLAY, scale) / 2f, topY, color, scale);
    }

    /** tekst z rozstrzelonymi literami */
    private static void spaced(DrawContext ctx, String s, float x, float y, int color, Font f, float scale, float spacing) {
        for (char ch : s.toCharArray()) {
            String c = String.valueOf(ch);
            Fonts.draw(ctx, c, x, y, color, f, scale);
            x += Fonts.width(c, f, scale) + spacing;
        }
    }

    // ------------------------------------------------------------------ karta po prawej

    private void drawCard(DrawContext ctx, int mx, int my) {
        long since = System.currentTimeMillis() - openTime;
        float ca = Anim.easeOutCubic((since - 450) / 700f);
        if (ca <= 0) return;
        boolean pl = pl();
        boolean showNews = height >= 290;
        float cw = Math.min(196, Math.max(160, width * 0.36f)), chh = showNews ? 170 : 102;
        float cx = width - Math.max(22, width * 0.06f) - cw + (1 - ca) * 20;
        float cy = (height - 26 - chh) / 2f;
        R2D.alpha = ca;
        // poświata + szkło
        R2D.glow(ctx, cx - 30, cy - 30, cw + 60, chh + 60, R2D.withAlpha(VIOLET, 0.10f));
        R2D.shadow(ctx, cx, cy + 4, cw, chh, 18, 0x88000000);
        R2D.roundRectV(ctx, cx, cy, cw, chh, 16, 0xB8121528, 0xC80A0C18);
        R2D.roundOutlineGrad(ctx, cx, cy, cw, chh, 16, 0.7f, R2D.withAlpha(TEAL, 0.45f), R2D.withAlpha(VIOLET, 0.25f));
        R2D.gradientH(ctx, cx + 16, cy + 0.6f, (cw - 32) / 2f, 0.6f, 0x00FFFFFF, 0x40FFFFFF);
        R2D.gradientH(ctx, cx + cw / 2f, cy + 0.6f, (cw - 32) / 2f, 0.6f, 0x40FFFFFF, 0x00FFFFFF);

        float px = cx + 14, py = cy + 14;
        // profil
        R2D.roundRect(ctx, px - 1.5f, py - 1.5f, 27, 27, 8, R2D.withAlpha(TEAL, 0.35f));
        drawHead(ctx, px, py, 24);
        LocalDateTime now = LocalDateTime.now();
        int hr = now.getHour();
        String greet = hr >= 5 && hr < 12 ? (pl ? "Dzień dobry" : "Good morning") : hr < 18 && hr >= 12 ? (pl ? "Miłego dnia" : "Good afternoon")
                : hr >= 18 && hr < 23 ? (pl ? "Dobry wieczór" : "Good evening") : (pl ? "Nocna sesja?" : "Late night session?");
        Fonts.draw(ctx, greet, px + 32, py + 3, DIM, Font.REGULAR, 0.62f);
        String name = client != null ? client.getSession().getUsername() : "Player";
        Fonts.draw(ctx, Fonts.trim(name, Font.BOLD, 0.9f, cw - 60), px + 32, py + 12, TEXT, Font.BOLD, 0.9f);
        // status online
        R2D.circle(ctx, px + 23, py + 23, 3.2f, 0xFF0A0C18);
        R2D.circle(ctx, px + 23, py + 23, 2.2f, 0xFF34D399);

        // zegar
        float clockY = py + 38;
        String hm = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        float cs = height >= 300 ? 0.62f : 0.5f;
        drawDisplayLeft(ctx, hm, px - 1, clockY, TEXT, cs);
        float hmw = Fonts.width(hm, Font.DISPLAY, cs);
        String sec = now.format(DateTimeFormatter.ofPattern("ss"));
        Fonts.draw(ctx, sec, px + hmw + 4, clockY + Fonts.displayCap(cs) - 7, R2D.blend(TEAL, VIOLET, 0.5f), Font.BOLD, 0.7f);
        Locale loc = pl ? Locale.forLanguageTag("pl-PL") : Locale.ENGLISH;
        String date = now.format(DateTimeFormatter.ofPattern(pl ? "EEEE, d MMMM" : "EEEE, MMMM d", loc));
        date = date.substring(0, 1).toUpperCase(loc) + date.substring(1);
        float dy = clockY + Fonts.displayCap(cs) + 6;
        Fonts.draw(ctx, date, px, dy, DIM, Font.SEMIBOLD, 0.66f);
        // pasek postępu dnia
        float day = (now.getHour() * 60 + now.getMinute()) / 1440f;
        float by = dy + 12;
        R2D.roundRect(ctx, px, by, cw - 28, 3, 1.5f, 0x1AFFFFFF);
        R2D.roundRectH(ctx, px, by, Math.max(3, (cw - 28) * day), 3, 1.5f, TEAL, VIOLET);
        R2D.glow(ctx, px + (cw - 28) * day - 5, by - 4, 10, 11, R2D.withAlpha(VIOLET, 0.6f));

        if (showNews) {
            // nowości
            float ny = by + 14;
            R2D.rect(ctx, px, ny, cw - 28, 0.5f, 0x18FFFFFF);
            ny += 9;
            Fonts.draw(ctx, pl ? "Nowości" : "What's new", px, ny, TEXT, Font.BOLD, 0.7f);
            String ver = "v" + VescGui.VERSION;
            float vw = Fonts.width(ver, Font.BOLD, 0.52f) + 8;
            R2D.roundRect(ctx, px + cw - 28 - vw, ny - 1.5f, vw, 10, 5, R2D.withAlpha(TEAL, 0.18f));
            Fonts.drawV(ctx, ver, px + cw - 28 - vw + 4, ny + 3.5f, TEAL, Font.BOLD, 0.52f);
            String[] news = pl
                    ? new String[]{"Nowe menu główne „Aurora”", "Animowana zorza i góry z bloków", "Sterowanie strzałkami i Enter"}
                    : new String[]{"Brand new \"Aurora\" main menu", "Animated aurora and block hills", "Arrow keys and Enter support"};
            int[] dots = {TEAL, VIOLET, PINK};
            for (int i = 0; i < news.length; i++) {
                float iy = ny + 15 + i * 12;
                R2D.circle(ctx, px + 2.5f, iy + 3.5f, 2, dots[i]);
                Fonts.draw(ctx, Fonts.trim(news[i], Font.REGULAR, 0.6f, cw - 40), px + 10, iy, DIM, Font.REGULAR, 0.6f);
            }
        }
        R2D.alpha = 1;
    }

    private void drawHead(DrawContext ctx, float x, float y, int size) {
        if (client == null) return;
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

    // ------------------------------------------------------------------ dół

    private void drawBottom(DrawContext ctx, int mx, int my) {
        R2D.alpha = Anim.easeOutCubic((System.currentTimeMillis() - openTime - 700) / 600f);
        float y = height - 20;
        float x = Math.max(22, width * 0.075f);
        Fonts.drawV(ctx, "VescClient GUI " + VescGui.VERSION, x, y + 7, DIM, Font.SEMIBOLD, 0.58f);
        Fonts.drawV(ctx, "•  Fabric  •  Minecraft " + SharedConstants.getGameVersion().getName(), x + Fonts.width("VescClient GUI " + VescGui.VERSION, Font.SEMIBOLD, 0.58f) + 6, y + 7, FAINT, Font.REGULAR, 0.58f);

        // prawa strona: copyright + małe przyciski
        String cr = "Copyright Mojang AB. Do not distribute!";
        float crw = Fonts.width(cr, Font.REGULAR, 0.56f);
        float rx = width - Math.max(22, width * 0.06f);
        boolean crh = mx >= rx - crw && mx < rx && my >= y && my < y + 14;
        Fonts.drawV(ctx, cr, rx - crw, y + 7, crh ? TEXT : FAINT, Font.REGULAR, 0.56f);
        if (crh) R2D.rect(ctx, rx - crw, y + 11.5f, crw, 0.5f, 0x66FFFFFF);
        hits.add(new Hit(rx - crw, y, crw, 14, () -> client.setScreen(new CreditsAndAttributionScreen(this))));
        float bx = rx - crw - 12;
        String[][] btn = {{"info", "a11y"}, {"globe_lang", "lang"}};
        for (int i = 0; i < btn.length; i++) {
            float s = 16;
            bx -= s;
            boolean hv = mx >= bx && mx < bx + s && my >= y - 1 && my < y - 1 + s;
            float h = smallHover[i].animate(hv ? 1 : 0);
            R2D.roundRect(ctx, bx, y - 1, s, s, s / 2f, R2D.blend(0x14FFFFFF, R2D.withAlpha(TEAL, 0.3f), h));
            R2D.roundOutline(ctx, bx, y - 1, s, s, s / 2f, 0.5f, R2D.blend(0x22FFFFFF, R2D.withAlpha(TEAL, 0.8f), h));
            R2D.icon(ctx, btn[i][0], bx + 4, y + 3, 8, R2D.blend(DIM, TEXT, h));
            final int which = i;
            hits.add(new Hit(bx, y - 1, s, s, () -> {
                if (which == 0) client.setScreen(new AccessibilityOptionsScreen(this, client.options));
                else client.setScreen(new LanguageOptionsScreen(this, client.options, client.getLanguageManager()));
            }));
            bx -= 5;
        }
        R2D.alpha = 1;
    }

    // ================================================================== wejście

    private void click() {
        if (client != null) client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.4f, 0.3f));
    }

    private void open(int i) {
        if (client == null) return;
        click();
        switch (i) {
            case 0 -> client.setScreen(new SelectWorldScreen(this));
            case 1 -> client.setScreen(client.options.skipMultiplayerWarning ? new MultiplayerScreen(this) : new MultiplayerWarningScreen(this));
            case 2 -> client.setScreen(new RealmsMainScreen(this));
            case 3 -> client.setScreen(new OptionsScreen(this, client.options));
            default -> client.scheduleStop();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return true;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.in(mouseX, mouseY)) { h.action().run(); return true; }
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int n = ICONS.length;
        int cur = keySel >= 0 ? keySel : hovered;
        switch (keyCode) {
            case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_TAB -> { keySel = cur < 0 ? 0 : (cur + 1) % n; return true; }
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> { keySel = cur < 0 ? n - 1 : (cur - 1 + n) % n; return true; }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> { if (cur >= 0) open(cur); return true; }
            default -> { return super.keyPressed(keyCode, scanCode, modifiers); }
        }
    }
}
