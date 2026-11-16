package dev.vesc.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.vesc.client.VescClient;
import dev.vesc.client.lang.Lang;
import dev.vesc.client.render.Fonts;
import dev.vesc.client.render.Fonts.Font;
import dev.vesc.client.render.R2D;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Menu główne VescClient: wyśrodkowana, zaokrąglona karta w stylu klienta (logo + zegar w nagłówku,
 * przyciski jako wiersze, stopka z awatarem). Animowane tło z falami i świecącymi cząsteczkami.
 */
public class MainMenu extends Screen {
    private static final Identifier WAVES = R2D.id("textures/ui/waves.png");
    private static long firstOpen = 0;

    private final long openTime = System.currentTimeMillis();
    private final float[][] particles = new float[56][];
    private long lastFrame = System.nanoTime();

    // przyciski menu
    private static final String[] ICONS = {"player", "users", "world", "settings", "power"};
    private static final String[] LABELS = {"menu.singleplayer", "menu.multiplayer", "menu.realms", "menu.options", "menu.quit"};
    private final Anim[] hover = new Anim[ICONS.length];
    private final List<float[]> rects = new ArrayList<>(); // {x,y,w,h,index}

    public MainMenu() {
        super(Text.literal("VescClient"));
        if (firstOpen == 0) firstOpen = System.currentTimeMillis();
        for (int i = 0; i < hover.length; i++) hover[i] = new Anim(0, 16);
        Random r = new Random();
        for (int i = 0; i < particles.length; i++)
            particles[i] = new float[]{r.nextFloat(), r.nextFloat(), (r.nextFloat() - 0.5f) * 0.01f, -0.004f - r.nextFloat() * 0.01f,
                    0.5f + r.nextFloat() * 1.8f, r.nextFloat() * 6.28f};
    }

    @Override public boolean shouldCloseOnEsc() { return false; }

    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    // ================================================================== tło

    private void drawBackground(DrawContext ctx) {
        R2D.alpha = 1;
        R2D.rect(ctx, 0, 0, width, height, 0xFF060608);
        float t = (System.currentTimeMillis() - firstOpen) / 1000f;
        float aspect = width / (float) height;
        float s1 = 0.9f, s2 = 1.6f;
        R2D.textureUV(ctx, WAVES, 0, 0, width, height, t * 0.004f, t * 0.002f, t * 0.004f + s1 * aspect, t * 0.002f + s1, 0x1AFFFFFF);
        R2D.textureUV(ctx, WAVES, 0, 0, width, height, -t * 0.006f + 0.37f, t * 0.003f + 0.11f, -t * 0.006f + 0.37f + s2 * aspect, t * 0.003f + 0.11f + s2, 0x10FFFFFF);
        int glow = mono() ? 0xFFFFFFFF : Theme.accentWave(0);
        R2D.glow(ctx, width / 2f - width * 0.45f, height * 0.12f, width * 0.9f, height * 0.75f, R2D.withAlpha(glow, 0.06f));
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        for (float[] p : particles) {
            p[0] += p[2] * dt; p[1] += p[3] * dt;
            if (p[1] < -0.02f) { p[1] = 1.02f; p[0] = (float) Math.random(); }
            if (p[0] < -0.02f) p[0] = 1.02f; else if (p[0] > 1.02f) p[0] = -0.02f;
            float tw = 0.5f + 0.5f * (float) Math.sin(t * 1.4f + p[5]);
            int c = mono() ? 0xFFFFFFFF : Theme.accentWave(p[5]);
            R2D.circle(ctx, p[0] * width, p[1] * height, p[4], R2D.withAlpha(c, 0.07f + 0.2f * tw));
        }
    }

    private static boolean mono() { return Theme.accentPreset() == Theme.Accent.MONO; }

    // ================================================================== render

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        drawBackground(ctx);
        float intro = Anim.easeOutCubic((System.currentTimeMillis() - openTime) / 650f);
        R2D.alpha = intro;
        int accent = mono() ? 0xFFFFFFFF : Theme.accentWave(0);

        // --- karta ---
        float cw = Math.min(330, width - 36);
        float rowH = 30, gap = 7, headH = 54, footH = 30;
        float ch = Math.min(height - 30, headH + ICONS.length * rowH + (ICONS.length - 1) * gap + footH + 24);
        float x = (width - cw) / 2f, y = (height - ch) / 2f + (1 - intro) * 12;

        R2D.shadow(ctx, x, y + 3, cw, ch, 22, R2D.withAlpha(0xFF000000, 0.55f));
        R2D.glow(ctx, x - 30, y - 30, cw + 60, ch + 60, R2D.withAlpha(accent, 0.10f));
        R2D.roundRect(ctx, x, y, cw, ch, 18, 0xF2121216);
        R2D.roundRectV(ctx, x, y, cw, 64, 18, R2D.withAlpha(accent, 0.10f), R2D.withAlpha(accent, 0f));
        R2D.roundOutline(ctx, x, y, cw, ch, 18, 0.7f, R2D.withAlpha(0xFFFFFFFF, 0.10f));

        // --- nagłówek: logo + nazwa + zegar ---
        float hx = x + 16, hy = y + 14;
        R2D.glow(ctx, hx - 4, hy - 4, 36, 36, R2D.withAlpha(accent, 0.4f));
        R2D.roundRectV(ctx, hx, hy, 30, 30, 10, R2D.blend(accent, 0xFFFFFFFF, 0.12f), accent);
        R2D.icon(ctx, "logo_v", hx + 7, hy + 7, 16, Theme.contrast(accent));
        Fonts.draw(ctx, "VescClient", hx + 40, hy + 3, 0xFFF4F3F8, Font.BOLD, 1.12f);
        Fonts.draw(ctx, "v" + VescClient.VERSION + "  •  Minecraft " + SharedConstants.getGameVersion().getName(), hx + 40, hy + 18, 0xFF8A8A96, Font.REGULAR, 0.62f);
        LocalDateTime dt = LocalDateTime.now();
        String time = dt.format(DateTimeFormatter.ofPattern("HH:mm"));
        Locale loc = Lang.pl() ? Locale.forLanguageTag("pl-PL") : Locale.ENGLISH;
        String date = dt.format(DateTimeFormatter.ofPattern(Lang.pl() ? "EEE, d MMM" : "EEE, MMM d", loc));
        date = date.substring(0, 1).toUpperCase(loc) + date.substring(1);
        float clockR = x + cw - 16;
        Fonts.draw(ctx, time, clockR - Fonts.width(time, Font.BOLD, 1.05f), hy + 2, 0xFFF4F3F8, Font.BOLD, 1.05f);
        Fonts.draw(ctx, date, clockR - Fonts.width(date, Font.SEMIBOLD, 0.6f), hy + 19, 0xFF8A8A96, Font.SEMIBOLD, 0.6f);

        // separator
        R2D.rect(ctx, x + 16, y + headH - 4, cw - 32, 0.5f, 0x18FFFFFF);

        // --- przyciski ---
        rects.clear();
        float by = y + headH + 2;
        for (int i = 0; i < ICONS.length; i++) {
            float bx = x + 16, bw = cw - 32;
            boolean hov = intro >= 1 && mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + rowH;
            float h = hover[i].animate(hov ? 1 : 0);
            boolean primary = i == 0;
            int bg = primary ? R2D.blend(accent, R2D.blend(accent, 0xFFFFFFFF, 0.18f), h)
                             : R2D.blend(0x14FFFFFF, 0x2EFFFFFF, h);
            R2D.roundRect(ctx, bx + h * 0, by, bw, rowH, 10, bg);
            if (primary) R2D.glow(ctx, bx - 6, by - 4, bw + 12, rowH + 8, R2D.withAlpha(accent, 0.18f + 0.12f * h));
            else R2D.roundOutline(ctx, bx, by, bw, rowH, 10, 0.5f, R2D.withAlpha(0xFFFFFFFF, 0.08f + 0.1f * h));
            int fg = primary ? Theme.contrast(accent) : R2D.blend(0xFFD6D6DC, 0xFFFFFFFF, h);
            // kafelek ikony
            float ix = bx + 8, iy = by + rowH / 2f - 9;
            if (!primary) R2D.roundRect(ctx, ix, iy, 18, 18, 6, R2D.withAlpha(0xFFFFFFFF, 0.08f));
            R2D.icon(ctx, ICONS[i], ix + 4, iy + 4, 10, fg);
            Fonts.drawV(ctx, Lang.get(LABELS[i]), bx + 34, by + rowH / 2f + 0.5f, fg, Font.BOLD, 0.82f);
            // strzałka po prawej
            R2D.icon(ctx, "arrow_left", bx + bw - 16 + h * 2, by + rowH / 2f - 4, 8, R2D.withAlpha(fg, 0.5f + 0.5f * h));
            rects.add(new float[]{bx, by, bw, rowH, i});
            by += rowH + gap;
        }

        // --- stopka: awatar + nick ---
        float fy = y + ch - footH + 4;
        R2D.rect(ctx, x + 16, fy - 4, cw - 32, 0.5f, 0x14FFFFFF);
        drawHead(ctx, x + 16, fy + 1, 18);
        R2D.circle(ctx, x + 16 + 15, fy + 1 + 15, 2.6f, 0xF2121216);
        R2D.circle(ctx, x + 16 + 15, fy + 1 + 15, 1.7f, 0xFF4ADE80);
        Fonts.draw(ctx, dev.vesc.client.module.Legit.name(), x + 40, fy + 6, 0xFFF4F3F8, Font.BOLD, 0.74f);
        String sub = Lang.pl() ? "Zalogowano" : "Signed in";
        Fonts.draw(ctx, sub, x + cw - 16 - Fonts.width(sub, Font.SEMIBOLD, 0.6f), fy + 8, 0xFF7A7A86, Font.SEMIBOLD, 0.6f);

        R2D.alpha = 1;
    }

    /** rysuje głowę gracza (skin) w (x, y) o rozmiarze size */
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

    // ================================================================== wejście

    private void click() {
        if (client != null) client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.6f, 0.25f));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (float[] r : rects) {
            if (mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3]) {
                click();
                open((int) r[4]);
                return true;
            }
        }
        return true;
    }

    private void open(int i) {
        if (client == null) return;
        switch (i) {
            case 0 -> client.setScreen(new SelectWorldScreen(this));
            case 1 -> client.setScreen(client.options.skipMultiplayerWarning ? new MultiplayerScreen(this) : new MultiplayerWarningScreen(this));
            case 2 -> client.setScreen(new RealmsMainScreen(this));
            case 3 -> client.setScreen(new OptionsScreen(this, client.options));
            case 4 -> client.scheduleStop();
        }
    }
}
