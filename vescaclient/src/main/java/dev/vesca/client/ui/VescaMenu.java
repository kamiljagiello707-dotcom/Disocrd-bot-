package dev.vesca.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.vesca.client.VescaClient;
import dev.vesca.client.render.Fonts;
import dev.vesca.client.render.Fonts.Font;
import dev.vesca.client.render.R2D;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static dev.vesca.client.ui.Ui.*;

/**
 * Menu główne VescaClient (biało-czarne):
 * po lewej logo i lista z przesuwaną białą pigułką (obsługa ↑ ↓ Enter), po prawej szklana karta z zegarem,
 * w tle wspólna animowana scena ({@link Scene}).
 */
public class VescaMenu extends Screen {
    /** pierwsze otwarcie gry – pełne, dłuższe wejście z czerni; kolejne są krótkie */
    private static boolean shownOnce;

    private final long openTime = System.currentTimeMillis();
    private final boolean firstIntro;
    private static final String[] ICONS = {"player", "users", "sparkles", "settings", "power"};
    private final Anim selY = new Anim(-1, 18), selA = new Anim(0, 14);
    private final Anim[] rowAnim = new Anim[ICONS.length];
    private final Anim[] smallHover = {new Anim(0, 16), new Anim(0, 16)};
    private int hovered = -1, keySel = -1;
    private float lastMx = -1, lastMy = -1;

    private record Hit(float x, float y, float w, float h, Runnable action) {
        boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }
    private final List<Hit> hits = new ArrayList<>();

    public VescaMenu() {
        super(Text.literal("VescaClient"));
        firstIntro = !shownOnce;
        shownOnce = true;
        for (int i = 0; i < rowAnim.length; i++) rowAnim[i] = new Anim(0, 16);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    private String label(int i) {
        return switch (i) {
            case 0 -> t("Tryb jednoosobowy", "Singleplayer");
            case 1 -> t("Tryb wieloosobowy", "Multiplayer");
            case 2 -> "Minecraft Realms";
            case 3 -> t("Ustawienia", "Options");
            default -> t("Wyjdź z gry", "Quit Game");
        };
    }

    private String desc(int i) {
        return switch (i) {
            case 0 -> t("Graj na swoich światach", "Play on your own worlds");
            case 1 -> t("Wybierz serwer i dołącz", "Pick a server and join");
            case 2 -> t("Twoje serwery Realms", "Your Realms servers");
            case 3 -> t("Grafika, dźwięk, sterowanie", "Video, sound and controls");
            default -> t("Do zobaczenia!", "See you soon!");
        };
    }

    /** postęp wejścia (0..1) z opóźnieniem delay ms; przy kolejnych otwarciach wszystko jest 2.5× szybsze */
    private float in(long delay, float dur) {
        float k = firstIntro ? 1f : 0.4f;
        return Anim.easeOutCubic((System.currentTimeMillis() - openTime - delay * k) / (dur * k));
    }

    private static float sin(double v) { return (float) Math.sin(v); }

    // ================================================================== render

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hits.clear();
        Scene.render(ctx, width, height, mouseX, mouseY, in(0, 1100), 0);
        R2D.gradientH(ctx, 0, 0, width * 0.45f, height, 0x88000000, 0x00000000);
        if (firstIntro) {
            float f = in(0, 600);
            if (f < 1) R2D.rect(ctx, 0, 0, width, height, R2D.withAlpha(0xFF000000, 1 - f));
        }
        if (mouseX != lastMx || mouseY != lastMy) { keySel = -1; lastMx = mouseX; lastMy = mouseY; }
        drawLeft(ctx, mouseX, mouseY);
        if (width >= 470) drawCard(ctx);
        drawBottom(ctx, mouseX, mouseY);
        R2D.alpha = 1;
    }

    // ------------------------------------------------------------------ lewa kolumna

    private float rowH() { return Math.max(24, Math.min(34, (height - 150) / 5.6f)); }

    private void drawLeft(DrawContext ctx, int mx, int my) {
        float x = Math.max(22, width * 0.075f);
        float rh = rowH(), rw = Math.min(250, width * 0.42f);
        boolean big = height >= 300;
        float logoH = big ? 62 : 48, gap = big ? 20 : 12;
        float total = logoH + gap + ICONS.length * rh;
        float y = Math.max(16, (height - 26 - total) / 2f);

        // ---- logo + napis VESCA CLIENT
        float la = in(150, 700);
        R2D.alpha = la;
        float ly = y + (1 - la) * 10;
        float ms = big ? 34 : 28;
        logo(ctx, x, ly, ms, 0.8f + 0.2f * sin(System.currentTimeMillis() / 700.0));
        float tx = x + ms + 12;
        float ds = (big ? 1.0f : 0.8f) * 0.62f;
        float capH = Fonts.displayCap(ds);
        float top = ly + (ms - (capH + 6 + 5.5f)) / 2f;
        display(ctx, "VESCA", tx, top, TEXT, ds);
        float vw = Fonts.width("VESCA", Font.DISPLAY, ds);
        spaced(ctx, "CLIENT", tx + vw + 7, top + capH / 2f - 3, DIM, Font.BOLD, 0.62f, 2.4f);
        Fonts.draw(ctx, "Minecraft " + SharedConstants.getGameVersion().getName() + "   •   v" + VescaClient.VERSION, tx, top + capH + 6, FAINT, Font.SEMIBOLD, 0.58f);
        R2D.alpha = 1;

        // ---- lista
        float listY = y + logoH + gap;
        int over = -1;
        for (int i = 0; i < ICONS.length; i++) if (mx >= x && mx < x + rw && my >= listY + i * rh && my < listY + (i + 1) * rh) over = i;
        hovered = over;
        int sel = keySel >= 0 ? keySel : over;
        float sa = selA.animate(sel >= 0 ? 1 : 0);
        if (sel >= 0 && selY.raw() < 0) selY.snap(listY + sel * rh);
        float sy = selY.animate(sel >= 0 ? listY + sel * rh : selY.target());
        if (sa > 0.01f && selY.raw() >= 0) {
            // biała pigułka pod zaznaczonym wierszem
            R2D.alpha = sa;
            R2D.glow(ctx, x - 16, sy - 8, rw + 32, rh + 16, R2D.withAlpha(0xFFFFFFFF, 0.12f));
            R2D.roundRectV(ctx, x, sy + 1, rw, rh - 2, (rh - 2) / 2f, 0xFFF7F7F8, 0xFFDCDCE0);
            R2D.alpha = 1;
        }
        for (int i = 0; i < ICONS.length; i++) {
            final int idx = i;
            float ry = listY + i * rh;
            float e = in(260 + i * 70L, 520);
            float h = rowAnim[i].animate(i == sel ? 1 : 0);
            // jak blisko jest pigułka (kolor tekstu przechodzi w czarny, gdy pigułka nad wierszem)
            float near = sa * Anim.clamp01(1 - Math.abs(sy - ry) / rh);
            R2D.alpha = e;
            float rx = x + (1 - e) * -18 + h * 4;
            Fonts.drawV(ctx, String.format("%02d", i + 1), rx + 14, ry + rh / 2f + 0.5f, R2D.blend(FAINT, INK, near), Font.SEMIBOLD, 0.56f);
            R2D.icon(ctx, ICONS[i], rx + 34, ry + rh / 2f - 5, 10, R2D.blend(DIM, BLACK, near));
            float lx = rx + 52;
            float lift = h * (rh >= 30 ? 4.5f : 3.5f);
            Fonts.drawV(ctx, label(i), lx, ry + rh / 2f + 0.5f - lift, R2D.blend(0xFFD4D4D8, BLACK, near), Font.BOLD, rh >= 30 ? 0.9f : 0.8f);
            if (h > 0.02f) {
                R2D.alpha = e * h;
                Fonts.drawV(ctx, desc(i), lx, ry + rh / 2f + 0.5f + (rh >= 30 ? 6.5f : 5f), INK, Font.SEMIBOLD, 0.58f);
                R2D.icon(ctx, "arrow_right", x + rw - 22 + (1 - h) * -8, ry + rh / 2f - 4.5f, 9, BLACK);
            }
            R2D.alpha = 1;
            hits.add(new Hit(x, ry, rw, rh, () -> open(idx)));
        }
        // podpowiedź klawiszy
        R2D.alpha = in(800, 600);
        float hy = listY + ICONS.length * rh + 10;
        if (hy + 10 < height - 28) {
            float kx = x + 6;
            kx += keyCap(ctx, "↑", kx, hy) + 4;
            kx += keyCap(ctx, "↓", kx, hy) + 4;
            kx += keyCap(ctx, "Enter", kx, hy) + 8;
            Fonts.drawV(ctx, t("nawigacja klawiaturą", "keyboard navigation"), kx, hy + 6, FAINT, Font.REGULAR, 0.56f);
        }
        R2D.alpha = 1;
    }

    // ------------------------------------------------------------------ karta po prawej

    private void drawCard(DrawContext ctx) {
        float ca = in(450, 700);
        if (ca <= 0) return;
        boolean pl = pl();
        boolean showNews = height >= 290;
        float cw = Math.min(196, Math.max(160, width * 0.36f)), chh = showNews ? 170 : 102;
        float cx = width - Math.max(22, width * 0.06f) - cw + (1 - ca) * 20;
        float cy = (height - 26 - chh) / 2f;
        R2D.alpha = ca;
        R2D.glow(ctx, cx - 30, cy - 30, cw + 60, chh + 60, R2D.withAlpha(0xFFFFFFFF, 0.05f));
        R2D.shadow(ctx, cx, cy + 4, cw, chh, 18, 0x99000000);
        R2D.roundRectV(ctx, cx, cy, cw, chh, 16, 0xC0141417, 0xD008080A);
        R2D.roundOutlineGrad(ctx, cx, cy, cw, chh, 16, 0.7f, 0x55FFFFFF, 0x14FFFFFF);
        R2D.gradientH(ctx, cx + 16, cy + 0.6f, (cw - 32) / 2f, 0.6f, 0x00FFFFFF, 0x50FFFFFF);
        R2D.gradientH(ctx, cx + cw / 2f, cy + 0.6f, (cw - 32) / 2f, 0.6f, 0x50FFFFFF, 0x00FFFFFF);

        float px = cx + 14, py = cy + 14;
        R2D.roundRect(ctx, px - 1.5f, py - 1.5f, 27, 27, 8, 0x66FFFFFF);
        drawHead(ctx, px, py, 24);
        LocalDateTime now = LocalDateTime.now();
        int hr = now.getHour();
        String greet = hr >= 5 && hr < 12 ? t("Dzień dobry", "Good morning") : hr >= 12 && hr < 18 ? t("Miłego dnia", "Good afternoon")
                : hr >= 18 && hr < 23 ? t("Dobry wieczór", "Good evening") : t("Nocna sesja?", "Late night session?");
        Fonts.draw(ctx, greet, px + 32, py + 3, DIM, Font.REGULAR, 0.62f);
        String name = client != null ? client.getSession().getUsername() : "Player";
        Fonts.draw(ctx, Fonts.trim(name, Font.BOLD, 0.9f, cw - 60), px + 32, py + 12, TEXT, Font.BOLD, 0.9f);
        R2D.circle(ctx, px + 23, py + 23, 3.2f, 0xFF0A0A0C);
        R2D.circle(ctx, px + 23, py + 23, 2.2f, 0xFFFFFFFF);

        float clockY = py + 38;
        String hm = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        float cs = height >= 300 ? 0.62f : 0.5f;
        display(ctx, hm, px - 1, clockY, TEXT, cs);
        float hmw = Fonts.width(hm, Font.DISPLAY, cs);
        Fonts.draw(ctx, now.format(DateTimeFormatter.ofPattern("ss")), px + hmw + 4, clockY + Fonts.displayCap(cs) - 7, DIM, Font.BOLD, 0.7f);
        Locale loc = pl ? Locale.forLanguageTag("pl-PL") : Locale.ENGLISH;
        String date = now.format(DateTimeFormatter.ofPattern(pl ? "EEEE, d MMMM" : "EEEE, MMMM d", loc));
        date = date.substring(0, 1).toUpperCase(loc) + date.substring(1);
        float dy = clockY + Fonts.displayCap(cs) + 6;
        Fonts.draw(ctx, date, px, dy, DIM, Font.SEMIBOLD, 0.66f);
        float day = (now.getHour() * 60 + now.getMinute()) / 1440f;
        float by = dy + 12;
        R2D.roundRect(ctx, px, by, cw - 28, 3, 1.5f, 0x1AFFFFFF);
        R2D.roundRectH(ctx, px, by, Math.max(3, (cw - 28) * day), 3, 1.5f, 0xFF8A8A93, 0xFFFFFFFF);
        R2D.glow(ctx, px + (cw - 28) * day - 5, by - 4, 10, 11, 0x99FFFFFF);

        if (showNews) {
            float ny = by + 14;
            R2D.rect(ctx, px, ny, cw - 28, 0.5f, 0x18FFFFFF);
            ny += 9;
            Fonts.draw(ctx, t("Nowości", "What's new"), px, ny, TEXT, Font.BOLD, 0.7f);
            String ver = "v" + VescaClient.VERSION;
            float vw = Fonts.width(ver, Font.BOLD, 0.52f) + 8;
            R2D.roundRect(ctx, px + cw - 28 - vw, ny - 1.5f, vw, 10, 5, 0xFFF4F4F5);
            Fonts.drawV(ctx, ver, px + cw - 28 - vw + 4, ny + 3.5f, BLACK, Font.BOLD, 0.52f);
            String[] news = pl
                    ? new String[]{"Biało-czarny wygląd", "Własne ekrany serwerów i światów", "Nowy ekran wczytywania"}
                    : new String[]{"Black & white look", "Custom server and world screens", "New loading screen"};
            int[] dots = {0xFFFFFFFF, 0xFFB4B4BC, 0xFF6E6E78};
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
        R2D.alpha = in(700, 600);
        float y = height - 20;
        float x = Math.max(22, width * 0.075f);
        String v = "VescaClient " + VescaClient.VERSION;
        Fonts.drawV(ctx, v, x, y + 7, DIM, Font.SEMIBOLD, 0.58f);
        Fonts.drawV(ctx, "•  Fabric  •  Minecraft " + SharedConstants.getGameVersion().getName(), x + Fonts.width(v, Font.SEMIBOLD, 0.58f) + 6, y + 7, FAINT, Font.REGULAR, 0.58f);

        String cr = "Copyright Mojang AB. Do not distribute!";
        float crw = Fonts.width(cr, Font.REGULAR, 0.56f);
        float rx = width - Math.max(22, width * 0.06f);
        boolean crh = mx >= rx - crw && mx < rx && my >= y && my < y + 14;
        Fonts.drawV(ctx, cr, rx - crw, y + 7, crh ? TEXT : FAINT, Font.REGULAR, 0.56f);
        if (crh) R2D.rect(ctx, rx - crw, y + 11.5f, crw, 0.5f, 0x66FFFFFF);
        hits.add(new Hit(rx - crw, y, crw, 14, () -> client.setScreen(new CreditsAndAttributionScreen(this))));
        float bx = rx - crw - 12;
        String[] icons = {"info", "globe_lang"};
        for (int i = 0; i < icons.length; i++) {
            float s = 16;
            bx -= s;
            boolean hv = mx >= bx && mx < bx + s && my >= y - 1 && my < y - 1 + s;
            float h = smallHover[i].animate(hv ? 1 : 0);
            R2D.roundRect(ctx, bx, y - 1, s, s, s / 2f, R2D.blend(0x14FFFFFF, 0xFFF4F4F5, h));
            R2D.roundOutline(ctx, bx, y - 1, s, s, s / 2f, 0.5f, R2D.blend(0x22FFFFFF, 0xFFFFFFFF, h));
            R2D.icon(ctx, icons[i], bx + 4, y + 3, 8, R2D.blend(DIM, BLACK, h));
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

    private void open(int i) {
        if (client == null) return;
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.4f, 0.3f));
        switch (i) {
            case 0 -> client.setScreen(new WorldsScreen(this));
            case 1 -> client.setScreen(client.options.skipMultiplayerWarning ? new ServersScreen(this) : new MultiplayerWarningScreen(this));
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
