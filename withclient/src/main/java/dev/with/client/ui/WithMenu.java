package dev.with.client.ui;

import dev.with.client.WithClient;
import dev.with.client.discord.DiscordRpc;
import dev.with.client.render.Fonts;
import dev.with.client.render.Fonts.Font;
import dev.with.client.render.R2D;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
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

import static dev.with.client.ui.Ui.*;

/**
 * Menu główne WithClient: panorama z cząsteczkami, po lewej logo i lista z białą pigułką
 * (mysz albo ↑ ↓ Enter), po prawej karta z profilem, zegarem, szybkim startem (ostatni serwer i świat),
 * nowościami i stanem Discorda.
 */
public class WithMenu extends Screen {
    private static boolean shownOnce;
    private final long openTime = System.currentTimeMillis();
    private final boolean firstIntro;
    private static final String[] ICONS = {"l_user", "l_users", "l_cloud", "l_settings", "l_power"};
    private final Anim selY = new Anim(-1, 18).springy(), selA = new Anim(0, 14);
    private final Anim[] rowAnim = new Anim[ICONS.length];
    private final java.util.Map<String, Anim> anims = new java.util.HashMap<>();
    private int hovered = -1, keySel = -1;
    private float lastMx = -1, lastMy = -1;

    private record Hit(float x, float y, float w, float h, Runnable action) {
        boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }
    private final List<Hit> hits = new ArrayList<>();

    public WithMenu() {
        super(Text.literal("WithClient"));
        firstIntro = !shownOnce;
        shownOnce = true;
        Loading.target = null;
        for (int i = 0; i < rowAnim.length; i++) rowAnim[i] = new Anim(0, 16);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    private Anim anim(String k) { return anims.computeIfAbsent(k, s -> new Anim(0, 16)); }

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
            case 0 -> t("Twoje światy", "Your worlds");
            case 1 -> t("Serwery i dołączanie", "Servers and joining");
            case 2 -> t("Serwery Realms", "Realms servers");
            case 3 -> t("Grafika, dźwięk, sterowanie", "Video, sound and controls");
            default -> t("Do zobaczenia!", "See you soon!");
        };
    }

    private float in(long delay, float dur) {
        float k = firstIntro ? 1f : 0.4f;
        return Anim.easeOutCubic((System.currentTimeMillis() - openTime - delay * k) / (dur * k));
    }

    // ================================================================== render

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hits.clear();
        R2D.alpha = 1; R2D.radiusMul = 1;
        MenuBg.render(ctx, this, width, height, mouseX, mouseY, delta, false, 0, true);
        if (firstIntro) {
            float f = in(0, 700);
            if (f < 1) R2D.rect(ctx, 0, 0, width, height, R2D.withAlpha(0xFF000000, 1 - f));
        }
        if (mouseX != lastMx || mouseY != lastMy) { keySel = -1; lastMx = mouseX; lastMy = mouseY; }
        drawLeft(ctx, mouseX, mouseY);
        if (width >= 470) drawCard(ctx, mouseX, mouseY);
        drawTop(ctx, mouseX, mouseY);
        drawBottom(ctx, mouseX, mouseY);
        R2D.alpha = 1;
    }

    // ------------------------------------------------------------------ lewa kolumna

    private float rowH() { return Math.max(24, Math.min(32, (height - 150) / 5.8f)); }

    private void drawLeft(DrawContext ctx, int mx, int my) {
        float x = Math.max(24, width * 0.075f);
        float rh = rowH(), rw = Math.min(240, width * 0.4f);
        boolean big = height >= 300;
        float logoH = big ? 60 : 46, gap = big ? 22 : 12;
        float total = logoH + gap + ICONS.length * rh;
        float y = Math.max(16, (height - 26 - total) / 2f);

        // ---- znak VV + napis WithClient
        float la = in(120, 760);
        R2D.alpha = la;
        float ly = y + (1 - la) * 12;
        float ms = big ? 40 : 32;
        float pulse = 0.6f + 0.4f * (float) Math.sin(System.currentTimeMillis() / 900.0);
        R2D.glow(ctx, x - ms * 0.6f, ly - ms * 0.6f, ms * 2.2f, ms * 2.2f, R2D.withAlpha(0xFFFFFFFF, 0.07f * pulse));
        R2D.icon(ctx, "with_logo", x, ly, ms, 0xFFFFFFFF);
        float tx = x + ms + 12;
        float ds = (big ? 0.95f : 0.75f) * 0.62f;
        float capH = Fonts.displayCap(ds);
        float top = ly + (ms - (capH + 7 + 6)) / 2f;
        display(ctx, "With", tx, top, TEXT, ds);
        display(ctx, "Client", tx + Fonts.width("With", Font.DISPLAY, ds), top, 0xFF8C8C96, ds);
        Fonts.draw(ctx, "Minecraft " + SharedConstants.getGameVersion().getName() + "   ·   v" + WithClient.VERSION, tx + 1, top + capH + 7, FAINT, Font.SEMIBOLD, 0.56f);
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
            R2D.alpha = sa;
            R2D.glow(ctx, x - 18, sy - 10, rw + 36, rh + 20, R2D.withAlpha(0xFFFFFFFF, 0.13f));
            R2D.roundRectV(ctx, x, sy + 1, rw, rh - 2, (rh - 2) / 2f, 0xFFF8F8F9, 0xFFDADADF);
            R2D.alpha = 1;
        }
        for (int i = 0; i < ICONS.length; i++) {
            final int idx = i;
            float ry = listY + i * rh;
            float e = in(260 + i * 70L, 520);
            float h = rowAnim[i].animate(i == sel ? 1 : 0);
            float near = sa * Anim.clamp01(1 - Math.abs(sy - ry) / rh);
            R2D.alpha = e;
            float rx = x + (1 - e) * -20 + h * 4;
            float tsz = rh - 10;
            R2D.roundRect(ctx, rx + 6, ry + 5, tsz, tsz, tsz / 2f, R2D.blend(0x14FFFFFF, 0xFF0B0B0D, near));
            R2D.icon(ctx, ICONS[i], rx + 6 + tsz / 2f - 4.5f, ry + rh / 2f - 4.5f, 9, R2D.blend(DIM, 0xFFFFFFFF, near));
            float lx = rx + 6 + tsz + 9;
            float lift = h * (rh >= 30 ? 4.5f : 3.5f);
            Fonts.drawV(ctx, label(i), lx, ry + rh / 2f + 0.5f - lift, R2D.blend(0xFFD4D4D8, BLACK, near), Font.BOLD, rh >= 30 ? 0.86f : 0.78f);
            if (h > 0.02f) {
                R2D.alpha = e * h;
                Fonts.drawV(ctx, desc(i), lx, ry + rh / 2f + 0.5f + (rh >= 30 ? 6.5f : 5f), INK, Font.SEMIBOLD, 0.56f);
                R2D.icon(ctx, "l_arrow_right", x + rw - 22 + (1 - h) * -8, ry + rh / 2f - 4.5f, 9, BLACK);
            }
            R2D.alpha = 1;
            hits.add(new Hit(x, ry, rw, rh, () -> open(idx)));
        }
        R2D.alpha = in(800, 600);
        float hy = listY + ICONS.length * rh + 10;
        if (hy + 10 < height - 28) {
            float kx = x + 6;
            kx += keyCap(ctx, "↑", kx, hy) + 4;
            kx += keyCap(ctx, "↓", kx, hy) + 4;
            kx += keyCap(ctx, "Enter", kx, hy) + 8;
            Fonts.drawV(ctx, t("nawigacja klawiaturą", "keyboard navigation"), kx, hy + 6, FAINT, Font.REGULAR, 0.54f);
        }
        R2D.alpha = 1;
    }

    // ------------------------------------------------------------------ karta po prawej

    private void drawCard(DrawContext ctx, int mx, int my) {
        float ca = in(420, 760);
        if (ca <= 0) return;
        boolean pl = pl();
        Recent.Entry srv = Recent.server(), wld = Recent.world();
        float quickH = (srv != null || wld != null) ? 20 + (srv != null ? 26 : 0) + (wld != null ? 26 : 0) : 0, newsH = 62;
        float avail = height - 26 - 40;
        boolean quick = quickH > 0 && 104 + quickH <= avail;
        boolean news = 104 + (quick ? quickH : 0) + newsH <= avail;
        float cw = Math.min(204, Math.max(168, width * 0.3f));
        float chh = 104 + (quick ? quickH : 0) + (news ? newsH : 0);
        float cx = width - Math.max(24, width * 0.065f) - cw + (1 - ca) * 24;
        float cy = Math.max(30, (height - 26 - chh) / 2f);
        R2D.alpha = ca;
        R2D.shadow(ctx, cx, cy + 5, cw, chh, 22, 0xAA000000);
        R2D.roundRectV(ctx, cx, cy, cw, chh, 16, 0xC8121215, 0xD8070709);
        R2D.roundOutlineGrad(ctx, cx, cy, cw, chh, 16, 0.7f, 0x55FFFFFF, 0x12FFFFFF);
        R2D.gradientH(ctx, cx + 16, cy + 0.6f, (cw - 32) / 2f, 0.6f, 0x00FFFFFF, 0x55FFFFFF);
        R2D.gradientH(ctx, cx + cw / 2f, cy + 0.6f, (cw - 32) / 2f, 0.6f, 0x55FFFFFF, 0x00FFFFFF);

        float px = cx + 14, py = cy + 14, iw = cw - 28;
        R2D.roundRect(ctx, px - 1.5f, py - 1.5f, 27, 27, 8, 0x55FFFFFF);
        head(ctx, px, py, 24);
        LocalDateTime now = LocalDateTime.now();
        int hr = now.getHour();
        String greet = hr >= 5 && hr < 12 ? t("Dzień dobry", "Good morning") : hr >= 12 && hr < 18 ? t("Miłego dnia", "Good afternoon")
                : hr >= 18 && hr < 23 ? t("Dobry wieczór", "Good evening") : t("Nocna sesja?", "Late night session?");
        Fonts.draw(ctx, greet, px + 32, py + 3, DIM, Font.REGULAR, 0.6f);
        String name = client != null ? client.getSession().getUsername() : "Player";
        Fonts.draw(ctx, Fonts.trim(name, Font.BOLD, 0.88f, iw - 40), px + 32, py + 12, TEXT, Font.BOLD, 0.88f);

        float clockY = py + 36;
        String hm = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        float cs = 0.56f;
        display(ctx, hm, px - 1, clockY, TEXT, cs);
        float hmw = Fonts.width(hm, Font.DISPLAY, cs);
        Fonts.draw(ctx, now.format(DateTimeFormatter.ofPattern("ss")), px + hmw + 4, clockY + Fonts.displayCap(cs) - 7, DIM, Font.BOLD, 0.66f);
        Locale loc = pl ? Locale.forLanguageTag("pl-PL") : Locale.ENGLISH;
        String date = now.format(DateTimeFormatter.ofPattern(pl ? "EEEE, d MMMM" : "EEEE, MMMM d", loc));
        date = date.substring(0, 1).toUpperCase(loc) + date.substring(1);
        float dy = clockY + Fonts.displayCap(cs) + 6;
        Fonts.draw(ctx, date, px, dy, DIM, Font.SEMIBOLD, 0.62f);
        float day = (now.getHour() * 60 + now.getMinute()) / 1440f;
        float by = dy + 12;
        R2D.roundRect(ctx, px, by, iw, 3, 1.5f, 0x1AFFFFFF);
        R2D.roundRectH(ctx, px, by, Math.max(3, iw * day), 3, 1.5f, 0xFF8A8A93, 0xFFFFFFFF);
        float y = by + 12;

        if (quick) {
            R2D.rect(ctx, px, y, iw, 0.5f, 0x18FFFFFF);
            y += 8;
            Fonts.draw(ctx, t("Szybki start", "Quick start"), px, y, TEXT, Font.BOLD, 0.66f);
            y += 12;
            if (srv != null) { y = quickRow(ctx, px, y, iw, "l_users", srv.name(), srv.id(), mx, my, () -> joinServer(srv)); }
            if (wld != null) { y = quickRow(ctx, px, y, iw, "l_globe", wld.name(), t("świat", "world"), mx, my, () -> playWorld(wld)); }
        }
        if (news) {
            R2D.rect(ctx, px, y, iw, 0.5f, 0x18FFFFFF);
            y += 8;
            Fonts.draw(ctx, t("Nowości", "What's new"), px, y, TEXT, Font.BOLD, 0.66f);
            String ver = "v" + WithClient.VERSION;
            float vw = Fonts.width(ver, Font.BOLD, 0.5f) + 8;
            R2D.roundRect(ctx, px + iw - vw, y - 1.5f, vw, 10, 5, 0xFFF4F4F5);
            Fonts.drawV(ctx, ver, px + iw - vw + 4, y + 3.5f, BLACK, Font.BOLD, 0.5f);
            String[] items = pl ? new String[]{"Target ESP i Jump Circles", "Spotify, własny hotbar, nowy HUD", "Nowe menu i status w Discordzie"}
                    : new String[]{"Target ESP and Jump Circles", "Spotify, custom hotbar, new HUD", "New menus and Discord status"};
            int[] dots = {0xFFFFFFFF, 0xFFB4B4BC, 0xFF6E6E78};
            for (int i = 0; i < items.length; i++) {
                float iy = y + 14 + i * 12;
                R2D.circle(ctx, px + 2.5f, iy + 3.5f, 2, dots[i]);
                Fonts.draw(ctx, Fonts.trim(items[i], Font.REGULAR, 0.58f, iw - 12), px + 10, iy, DIM, Font.REGULAR, 0.58f);
            }
        }
        R2D.alpha = 1;
    }

    private float quickRow(DrawContext ctx, float x, float y, float w, String icon, String name, String sub, int mx, int my, Runnable go) {
        float h = 22;
        boolean ov = mx >= x && mx < x + w && my >= y && my < y + h;
        float hv = anim("q" + icon).animate(ov ? 1 : 0);
        R2D.roundRect(ctx, x, y, w, h, 7, R2D.blend(0x0EFFFFFF, 0xFFF4F4F5, hv));
        R2D.icon(ctx, icon, x + 7, y + h / 2f - 4, 8, R2D.blend(DIM, BLACK, hv));
        Fonts.drawV(ctx, Fonts.trim(name, Font.BOLD, 0.6f, w - 70), x + 20, y + 8, R2D.blend(TEXT, BLACK, hv), Font.BOLD, 0.6f);
        Fonts.drawV(ctx, Fonts.trim(sub, Font.REGULAR, 0.48f, w - 70), x + 20, y + 15.5f, R2D.blend(FAINT, INK, hv), Font.REGULAR, 0.48f);
        R2D.icon(ctx, "l_play", x + w - 16, y + h / 2f - 4, 8, R2D.blend(TEXT, BLACK, hv));
        hits.add(new Hit(x, y, w, h, go));
        return y + h + 4;
    }

    private void joinServer(Recent.Entry e) {
        if (client == null) return;
        ServerInfo info = new ServerInfo(e.name(), e.id(), ServerInfo.ServerType.OTHER);
        Loading.target = e.name();
        ConnectScreen.connect(this, client, ServerAddress.parse(e.id()), info, false, null);
    }

    private void playWorld(Recent.Entry e) {
        if (client == null) return;
        Loading.target = e.name();
        client.createIntegratedServerLoader().start(e.id(), () -> client.setScreen(this));
    }

    // ------------------------------------------------------------------ góra / dół

    private void drawTop(DrawContext ctx, int mx, int my) {
        R2D.alpha = in(700, 600);
        float y = 10, rx = width - Math.max(24, width * 0.065f);
        // stan Discorda
        String ds = DiscordRpc.status();
        boolean ok = DiscordRpc.connected();
        String dtxt = "Discord" + (ds.isEmpty() ? "" : "  ·  " + ds);
        float dw = Fonts.width(dtxt, Font.SEMIBOLD, 0.52f) + 22;
        float dx = rx - dw;
        R2D.roundRect(ctx, dx, y, dw, 14, 7, 0x22FFFFFF);
        R2D.roundOutline(ctx, dx, y, dw, 14, 7, 0.5f, 0x22FFFFFF);
        R2D.circle(ctx, dx + 8, y + 7, 2.2f, ok ? 0xFF46D17A : 0xFF6E6E78);
        if (ok) R2D.glow(ctx, dx + 2, y + 1, 12, 12, 0x6646D17A);
        Fonts.drawV(ctx, dtxt, dx + 14, y + 7.5f, ok ? TEXT : DIM, Font.SEMIBOLD, 0.52f);
        // język i dostępność
        String[] icons = {"l_languages", "l_accessibility"};
        float bx = dx - 6;
        for (int i = 0; i < icons.length; i++) {
            float s = 14;
            bx -= s;
            boolean hv = mx >= bx && mx < bx + s && my >= y && my < y + s;
            float h = anim("top" + i).animate(hv ? 1 : 0);
            R2D.roundRect(ctx, bx, y, s, s, s / 2f, R2D.blend(0x22FFFFFF, 0xFFF4F4F5, h));
            R2D.icon(ctx, icons[i], bx + 3.5f, y + 3.5f, 7, R2D.blend(DIM, BLACK, h));
            final int which = i;
            hits.add(new Hit(bx, y, s, s, () -> {
                if (which == 0) client.setScreen(new LanguageOptionsScreen(this, client.options, client.getLanguageManager()));
                else client.setScreen(new AccessibilityOptionsScreen(this, client.options));
            }));
            bx -= 5;
        }
        R2D.alpha = 1;
    }

    private void drawBottom(DrawContext ctx, int mx, int my) {
        R2D.alpha = in(700, 600);
        float y = height - 18;
        float x = Math.max(24, width * 0.075f);
        String v = "WithClient " + WithClient.VERSION;
        Fonts.drawV(ctx, v, x, y + 6, DIM, Font.SEMIBOLD, 0.56f);
        Fonts.drawV(ctx, "·  Fabric  ·  Minecraft " + SharedConstants.getGameVersion().getName(), x + Fonts.width(v, Font.SEMIBOLD, 0.56f) + 5, y + 6, FAINT, Font.REGULAR, 0.56f);
        String cr = "Copyright Mojang AB. Do not distribute!";
        float crw = Fonts.width(cr, Font.REGULAR, 0.54f);
        float rx = width - Math.max(24, width * 0.065f);
        boolean crh = mx >= rx - crw && mx < rx && my >= y && my < y + 12;
        Fonts.drawV(ctx, cr, rx - crw, y + 6, crh ? TEXT : FAINT, Font.REGULAR, 0.54f);
        if (crh) R2D.rect(ctx, rx - crw, y + 10.5f, crw, 0.5f, 0x66FFFFFF);
        hits.add(new Hit(rx - crw, y, crw, 12, () -> client.setScreen(new CreditsAndAttributionScreen(this))));
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
