package dev.vesc.client.ui;

import dev.vesc.client.VescClient;
import dev.vesc.client.lang.Lang;
import dev.vesc.client.render.Fonts;
import dev.vesc.client.render.Fonts.Font;
import dev.vesc.client.render.R2D;
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
 * Menu główne VescClient: wielki zegar na środku ekranu i rząd okrągłych przycisków-ikon na dole
 * (Singleplayer, Multiplayer, Realms, Opcje, ClickGUI, Wyjście). Animowane tło z falami i cząsteczkami.
 */
public class MainMenu extends Screen {
    private static final Identifier WAVES = R2D.id("textures/ui/waves.png");
    private static long firstOpen = 0;

    private final long openTime = System.currentTimeMillis();
    private final float[][] particles = new float[56][];
    private long lastFrame = System.nanoTime();

    private static final String[] ICONS = {"player", "users", "world", "settings", "misc", "power"};
    private static final String[] LABELS = {"menu.singleplayer", "menu.multiplayer", "menu.realms", "menu.options", "menu.gui", "menu.quit"};
    private final Anim[] hover = new Anim[ICONS.length];
    private float bx, by, bs = 34, bgap = 16;

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

    private static boolean mono() { return Theme.accentPreset() == Theme.Accent.MONO; }

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
        R2D.glow(ctx, width / 2f - width * 0.45f, height * 0.08f, width * 0.9f, height * 0.7f, R2D.withAlpha(glow, 0.06f));
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

    // ================================================================== render

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        drawBackground(ctx);
        float intro = Anim.easeOutCubic((System.currentTimeMillis() - openTime) / 650f);
        int accent = mono() ? 0xFFFFFFFF : Theme.accentWave(0);
        float cx = width / 2f;

        LocalDateTime dt = LocalDateTime.now();
        String time = dt.format(DateTimeFormatter.ofPattern("HH:mm"));
        Locale loc = Lang.pl() ? Locale.forLanguageTag("pl-PL") : Locale.ENGLISH;
        String date = dt.format(DateTimeFormatter.ofPattern(Lang.pl() ? "EEEE, d MMMM" : "EEEE, MMMM d", loc));
        date = date.substring(0, 1).toUpperCase(loc) + date.substring(1);

        // ---------------- logo + nazwa (nad zegarem)
        R2D.alpha = intro;
        float topY = height * 0.26f + (1 - intro) * 10;
        R2D.glow(ctx, cx - 16, topY - 4, 32, 32, R2D.withAlpha(accent, 0.4f));
        R2D.roundRectV(ctx, cx - 15, topY, 30, 30, 10, R2D.blend(accent, 0xFFFFFFFF, 0.12f), accent);
        R2D.icon(ctx, "logo_v", cx - 8, topY + 7, 16, Theme.contrast(accent));
        Fonts.drawCentered(ctx, "VescClient", cx, topY + 40, 0xFFF4F3F8, Font.BOLD, 1.0f);

        // ---------------- WIELKI zegar
        float clockScale = Math.min(3.2f, width / 320f);
        float clockTop = height * 0.4f + (1 - intro) * 8;
        // miękka poświata za zegarem
        float cw = Fonts.width(time, Font.DISPLAY, 0.62f * clockScale);
        R2D.glow(ctx, cx - cw * 0.7f, clockTop - 6, cw * 1.4f, Fonts.displayCap(0.62f * clockScale) + 28, R2D.withAlpha(accent, 0.12f));
        Fonts.drawDisplay(ctx, time, cx, clockTop, R2D.blend(0xFFFFFFFF, accent, 0.25f), 0.62f * clockScale);
        float clockBottom = clockTop + Fonts.displayCap(0.62f * clockScale);
        Fonts.drawCentered(ctx, date, cx, clockBottom + 10, 0xFF9A9AA6, Font.SEMIBOLD, 0.95f);
        Fonts.drawCentered(ctx, Lang.format("menu.welcome", dev.vesc.client.module.Legit.name()), cx, clockBottom + 28, 0xFF74747E, Font.SEMIBOLD, 0.72f);

        // ---------------- rząd ikon na dole
        int n = ICONS.length;
        float total = n * bs + (n - 1) * bgap;
        bx = cx - total / 2f;
        by = height - 86 + (1 - intro) * 14;
        for (int i = 0; i < n; i++) {
            float x = bx + i * (bs + bgap);
            boolean hov = intro >= 1 && mouseX >= x && mouseX < x + bs && mouseY >= by && mouseY < by + bs;
            float h = hover[i].animate(hov ? 1 : 0);
            float lift = h * 3f;
            boolean danger = i == n - 1;
            R2D.shadow(ctx, x, by - lift + 2, bs, bs, 10, R2D.withAlpha(0xFF000000, 0.5f));
            if (h > 0.02f) R2D.glow(ctx, x - 6, by - lift - 6, bs + 12, bs + 12, R2D.withAlpha(danger ? 0xFFFF5C6A : accent, 0.35f * h));
            int fill = danger ? R2D.blend(0x18FFFFFF, 0xFFFF4D5E, h) : R2D.blend(0x18FFFFFF, accent, h);
            R2D.roundRectV(ctx, x, by - lift, bs, bs, bs / 2f, R2D.blend(fill, 0xFFFFFFFF, h * (danger ? 0 : 0.1f)), fill);
            R2D.roundOutline(ctx, x, by - lift, bs, bs, bs / 2f, 0.6f, R2D.withAlpha(0xFFFFFFFF, 0.1f + 0.1f * (1 - h)));
            int ic = danger ? R2D.blend(0xFFE6E6EA, 0xFFFFFFFF, h) : R2D.blend(0xFFE6E6EA, Theme.contrast(accent), h);
            R2D.icon(ctx, ICONS[i], x + bs / 2f - 6, by - lift + bs / 2f - 6, 12, ic);
            // etykieta nad ikoną przy najechaniu
            if (h > 0.02f) {
                float pa = R2D.alpha;
                R2D.alpha = pa * h;
                String l = Lang.get(LABELS[i]);
                float lw = Fonts.width(l, Font.BOLD, 0.66f) + 14;
                float ly = by - lift - 20;
                R2D.roundRect(ctx, x + bs / 2f - lw / 2f, ly, lw, 15, 7.5f, 0xF2121216);
                R2D.roundOutline(ctx, x + bs / 2f - lw / 2f, ly, lw, 15, 7.5f, 0.5f, R2D.withAlpha(accent, 0.4f));
                Fonts.drawCentered(ctx, l, x + bs / 2f, ly + 7.5f - Fonts.height(0.66f) / 2f + 0.5f, 0xFFF4F3F8, Font.BOLD, 0.66f);
                R2D.alpha = pa;
            }
        }

        // ---------------- stopka
        R2D.alpha = intro;
        Fonts.draw(ctx, "VescClient " + VescClient.VERSION + "  •  Minecraft " + SharedConstants.getGameVersion().getName(), 8, height - 12, 0x66FFFFFF, Font.REGULAR, 0.62f);
        String cpr = "Not affiliated with Mojang AB";
        Fonts.draw(ctx, cpr, width - 8 - Fonts.width(cpr, Font.REGULAR, 0.62f), height - 12, 0x44FFFFFF, Font.REGULAR, 0.62f);
        R2D.alpha = 1;
    }

    // ================================================================== wejście

    private void click() {
        if (client != null) client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.6f, 0.25f));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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

    private void open(int i) {
        if (client == null) return;
        switch (i) {
            case 0 -> client.setScreen(new SelectWorldScreen(this));
            case 1 -> client.setScreen(client.options.skipMultiplayerWarning ? new MultiplayerScreen(this) : new MultiplayerWarningScreen(this));
            case 2 -> client.setScreen(new RealmsMainScreen(this));
            case 3 -> client.setScreen(new OptionsScreen(this, client.options));
            case 4 -> client.setScreen(new ClickGui());
            case 5 -> client.scheduleStop();
        }
    }
}
