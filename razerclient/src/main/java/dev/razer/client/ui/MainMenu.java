package dev.razer.client.ui;

import dev.razer.client.RazerClient;
import dev.razer.client.lang.Lang;
import dev.razer.client.render.Fonts;
import dev.razer.client.render.Fonts.Font;
import dev.razer.client.render.R2D;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.DrawContext;
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
import java.util.Locale;
import java.util.Random;

/**
 * Menu główne RazerClient: ekran blokady (data + godzina + "kliknij, aby kontynuować"),
 * a po kliknięciu nowoczesne menu z ikonami. Animowane tło z falami dymu i świecącymi cząsteczkami.
 */
public class MainMenu extends Screen {
    private static final Identifier WAVES = R2D.id("textures/ui/waves.png");
    private static final Identifier GLOW = R2D.id("textures/ui/glow.png");
    /** ekran blokady pokazujemy tylko raz na uruchomienie gry */
    private static boolean unlocked = false;
    private static long firstOpen = 0;

    private final long openTime = System.currentTimeMillis();
    private long unlockTime = unlocked ? 1 : 0;
    private final Anim[] hover = {new Anim(0, 16), new Anim(0, 16), new Anim(0, 16), new Anim(0, 16), new Anim(0, 16)};
    private final float[][] particles = new float[60][];
    private long lastFrame = System.nanoTime();
    private float bx, by, bs = 26, bgap = 14;
    private static final String[] ICONS = {"player", "users", "world", "settings", "power"};
    private static final String[] LABELS = {"menu.singleplayer", "menu.multiplayer", "menu.realms", "menu.options", "menu.quit"};

    public MainMenu() {
        super(Text.literal("RazerClient"));
        if (firstOpen == 0) firstOpen = System.currentTimeMillis();
        Random r = new Random();
        for (int i = 0; i < particles.length; i++)
            particles[i] = new float[]{r.nextFloat(), r.nextFloat(), (r.nextFloat() - 0.5f) * 0.01f, -0.004f - r.nextFloat() * 0.01f,
                    0.5f + r.nextFloat() * 1.8f, r.nextFloat() * 6.28f};
    }

    @Override public boolean shouldCloseOnEsc() { return false; }

    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    private float unlockProgress() {
        if (unlockTime == 0) return 0;
        if (unlockTime == 1) return 1;
        return Anim.easeOutCubic((System.currentTimeMillis() - unlockTime) / (700f / Anim.globalSpeed));
    }

    // ================================================================== tło

    private void drawBackground(DrawContext ctx) {
        R2D.alpha = 1;
        R2D.rect(ctx, 0, 0, width, height, 0xFF050507);
        float t = (System.currentTimeMillis() - firstOpen) / 1000f;
        float aspect = width / (float) height;
        // dwie warstwy fal przesuwające się w różnych kierunkach
        float s1 = 0.9f, s2 = 1.6f;
        R2D.textureUV(ctx, WAVES, 0, 0, width, height, t * 0.004f, t * 0.002f, t * 0.004f + s1 * aspect, t * 0.002f + s1, 0x1CFFFFFF);
        R2D.textureUV(ctx, WAVES, 0, 0, width, height, -t * 0.006f + 0.37f, t * 0.003f + 0.11f, -t * 0.006f + 0.37f + s2 * aspect, t * 0.003f + 0.11f + s2, 0x12FFFFFF);
        // poświata w kolorze akcentu
        int glow = Theme.accentPreset() == Theme.Accent.MONO ? 0xFFFFFFFF : Theme.accentWave(0);
        R2D.glow(ctx, width / 2f - width * 0.45f, height * 0.18f, width * 0.9f, height * 0.7f, R2D.withAlpha(glow, 0.07f));
        // winieta
        for (int i = 0; i < 10; i++) {
            float k = i / 10f;
            R2D.rect(ctx, 0, height * (1 - 0.25f * (1 - k)), width, height * 0.025f + 1, R2D.withAlpha(0xFF000000, 0.06f));
            R2D.rect(ctx, 0, height * 0.25f * k - 1, width, height * 0.025f + 1, R2D.withAlpha(0xFF000000, 0.05f * (1 - k)));
        }
        // cząsteczki
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        for (float[] p : particles) {
            p[0] += p[2] * dt; p[1] += p[3] * dt;
            if (p[1] < -0.02f) { p[1] = 1.02f; p[0] = (float) Math.random(); }
            if (p[0] < -0.02f) p[0] = 1.02f; else if (p[0] > 1.02f) p[0] = -0.02f;
            float tw = 0.5f + 0.5f * (float) Math.sin(t * 1.4f + p[5]);
            int c = Theme.accentPreset() == Theme.Accent.MONO ? 0xFFFFFFFF : Theme.accentWave(p[5]);
            R2D.circle(ctx, p[0] * width, p[1] * height, p[4], R2D.withAlpha(c, 0.08f + 0.22f * tw));
        }
    }

    // ================================================================== render

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        drawBackground(ctx);
        float intro = Anim.easeOutCubic((System.currentTimeMillis() - openTime) / 900f);
        float u = unlockProgress();
        LocalDateTime now = LocalDateTime.now();
        String time = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        Locale loc = Lang.pl() ? Locale.forLanguageTag("pl-PL") : Locale.ENGLISH;
        String date = now.format(DateTimeFormatter.ofPattern(Lang.pl() ? "EEEE, d MMMM" : "EEEE, MMMM d", loc));
        date = date.substring(0, 1).toUpperCase(loc) + date.substring(1);
        float cx = width / 2f, cy = height / 2f;

        // ---------------- ekran blokady
        if (u < 1) {
            R2D.alpha = intro * (1 - u);
            float oy = -u * 40 + (1 - intro) * 10;
            drawTitle(ctx, cx, cy - 62 + oy, 1f);
            Fonts.drawCentered(ctx, date, cx, cy - 26 + oy, 0xFF8E8E98, Font.SEMIBOLD, 0.9f);
            drawClock(ctx, time, cx, cy - 12 + oy, 1.75f, 0xFFD2D2D8);
            float pulse = 0.45f + 0.35f * (float) Math.sin(System.currentTimeMillis() / 500.0);
            float hy = height - 30 + oy * 0.5f;
            float bob = (float) Math.sin(System.currentTimeMillis() / 300.0) * 1.5f;
            R2D.icon(ctx, "arrow_left", cx - 3.5f, hy - 12 + bob, 7, R2D.withAlpha(0xFFFFFFFF, pulse * 0.6f));
            Fonts.drawCentered(ctx, Lang.get("menu.continue"), cx, hy, R2D.withAlpha(0xFFFFFFFF, pulse), Font.SEMIBOLD, 0.7f);
            R2D.alpha = 1;
        }

        // ---------------- menu
        if (u > 0) {
            float a = u;
            R2D.alpha = a;
            float oy = (1 - u) * 24;
            drawTitle(ctx, cx, cy - 86 + oy, 1f);
            drawClock(ctx, time, cx, cy - 50 + oy, 1.3f, 0xFFD8D8DE);
            Fonts.drawCentered(ctx, Lang.format("menu.welcome", dev.razer.client.module.Legit.name()), cx, cy - 16 + oy, 0xFF8C8C96, Font.SEMIBOLD, 0.75f);

            // przyciski-ikony
            int n = ICONS.length;
            float total = n * bs + (n - 1) * bgap;
            bx = cx - total / 2f; by = cy + 6 + oy;
            for (int i = 0; i < n; i++) {
                float x = bx + i * (bs + bgap);
                boolean hov = mouseX >= x && mouseX < x + bs && mouseY >= by && mouseY < by + bs && u >= 1;
                float h = hover[i].animate(hov ? 1 : 0);
                float lift = h * 2.5f;
                R2D.shadow(ctx, x, by - lift, bs, bs, 8, R2D.withAlpha(0xFF000000, 0.5f));
                R2D.roundRect(ctx, x, by - lift, bs, bs, bs / 2f, R2D.blend(0x26FFFFFF, 0xF2FFFFFF, h));
                R2D.roundOutline(ctx, x, by - lift, bs, bs, bs / 2f, 0.6f, R2D.blend(0x40FFFFFF, 0x00FFFFFF, h));
                R2D.icon(ctx, ICONS[i], x + bs / 2f - 5, by - lift + bs / 2f - 5, 10, R2D.blend(0xFFE6E6EA, 0xFF0A0A0B, h));
                if (h > 0.02f) {
                    float pa = R2D.alpha;
                    R2D.alpha = pa * h;
                    String l = Lang.get(LABELS[i]);
                    float lw = Fonts.width(l, Font.SEMIBOLD, 0.62f) + 12;
                    R2D.roundRect(ctx, x + bs / 2f - lw / 2f, by + bs + 6 - lift * 0.4f, lw, 12, 6, 0xE6111114);
                    Fonts.drawCentered(ctx, l, x + bs / 2f, by + bs + 12 - lift * 0.4f - Fonts.height(0.62f) / 2f + 0.5f, 0xFFE6E6EA, Font.SEMIBOLD, 0.62f);
                    R2D.alpha = pa;
                }
            }
            // "home indicator"
            R2D.roundRect(ctx, cx - 26, by + bs + 26, 52, 2, 1, 0x66FFFFFF);

            // dół ekranu
            Fonts.draw(ctx, "RazerClient " + RazerClient.VERSION + "  •  Minecraft " + SharedConstants.getGameVersion().getName(), 6, height - 11, 0x66FFFFFF, Font.REGULAR, 0.62f);
            String c = "Mojang AB. Do not distribute!";
            Fonts.draw(ctx, c, width - 6 - Fonts.width(c, Font.REGULAR, 0.62f), height - 11, 0x44FFFFFF, Font.REGULAR, 0.62f);
            R2D.alpha = 1;
        }
    }

    /** y = górna krawędź liter */
    private void drawTitle(DrawContext ctx, float cx, float y, float scale) {
        String t = "RazerClient";
        float s = 0.62f * scale;
        float w = Fonts.width(t, Font.DISPLAY, s), capH = Fonts.displayCap(s);
        int accent = Theme.accentPreset() == Theme.Accent.MONO ? 0xFFFFFFFF : Theme.accentWave(0);
        // świecenie: miękka poświata za napisem + rozmyte kopie
        R2D.glow(ctx, cx - w * 0.8f, y + capH / 2f - 30, w * 1.6f, 60, R2D.withAlpha(accent, 0.12f));
        for (int i = 0; i < 8; i++) {
            double an = Math.PI * 2 * i / 8;
            Fonts.drawDisplay(ctx, t, cx + (float) Math.cos(an) * 1.5f, y + (float) Math.sin(an) * 1.5f, R2D.withAlpha(accent, 0.09f), s);
        }
        Fonts.drawDisplay(ctx, t, cx, y, R2D.blend(0xFFFFFFFF, accent, 0.25f), s);
    }

    private void drawClock(DrawContext ctx, String time, float cx, float y, float scale, int color) {
        Fonts.drawDisplay(ctx, time, cx, y, color, 0.62f * scale);
    }

    // ================================================================== wejście

    private void click() {
        if (client != null) client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.6f, 0.25f));
    }

    private void unlock() {
        if (unlockTime != 0) return;
        unlockTime = System.currentTimeMillis();
        unlocked = true;
        click();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (unlockProgress() < 1) { unlock(); return true; }
        for (int i = 0; i < ICONS.length; i++) {
            float x = bx + i * (bs + bgap);
            if (mouseX >= x && mouseX < x + bs && mouseY >= by && mouseY < by + bs) {
                click();
                open(i);
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (unlockProgress() < 1) { unlock(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
