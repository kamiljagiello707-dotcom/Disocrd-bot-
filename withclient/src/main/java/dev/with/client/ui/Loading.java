package dev.with.client.ui;

import dev.with.client.mixin.ConnectScreenAccessor;
import dev.with.client.mixin.DisconnectedScreenAccessor;
import dev.with.client.mixin.LevelLoadingScreenAccessor;
import dev.with.client.mixin.ProgressScreenAccessor;
import dev.with.client.render.Fonts;
import dev.with.client.render.Fonts.Font;
import dev.with.client.render.R2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.server.WorldGenerationProgressTracker;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.Random;

import static dev.with.client.ui.Ui.*;

/**
 * Ekran wczytywania WithClient – zastępuje wygląd ekranów Minecrafta podczas łączenia z serwerem,
 * pobierania terenu, przygotowywania świata i zapisywania: logo, obracający się pierścień, napis "Wczytywanie…",
 * status, pasek postępu i wskazówka. Przyciski ekranu (np. "Anuluj") dalej działają – rysujemy je po swojemu.
 */
public final class Loading {
    private Loading() {}

    /** nazwa serwera / świata, do którego wchodzimy (ustawiana przez listy serwerów i światów) */
    public static String target;

    private static Screen last;
    private static long since;
    private static int tip;
    private static final Random RND = new Random();
    private static final Anim progress = new Anim(0, 6);

    public static boolean handles(Screen s) {
        return s instanceof ConnectScreen || s instanceof DownloadingTerrainScreen || s instanceof LevelLoadingScreen
                || s instanceof MessageScreen || s instanceof ProgressScreen || s instanceof DisconnectedScreen;
    }

    private static String[] tips() {
        return pl() ? new String[]{
                "Kliknij dwukrotnie serwer albo świat, żeby od razu wejść",
                "Prawy Shift w grze otwiera ClickGUI WithClient",
                "W menu działają strzałki ↑ ↓ i Enter",
                "Zacznij pisać na liście serwerów, żeby ją przeszukać",
                "Prawy przycisk myszy na serwerze otwiera edycję",
                "Esc na liście wraca do menu głównego"
        } : new String[]{
                "Double-click a server or world to jump right in",
                "Right Shift in game opens the WithClient ClickGUI",
                "Arrow keys ↑ ↓ and Enter work in the menus",
                "Start typing on the server list to search it",
                "Right-click a server to edit it",
                "Press Esc on a list to go back to the main menu"
        };
    }

    public static void render(DrawContext ctx, Screen s, int mouseX, int mouseY) {
        if (s != last) {
            // nowa sesja wczytywania (kolejne ekrany jednego wejścia nie resetują animacji, jeśli idą szybko po sobie)
            if (last == null || System.currentTimeMillis() - since > 4000 || !handles(last)) { since = System.currentTimeMillis(); tip = RND.nextInt(6); progress.snap(0); }
            last = s;
        }
        int w = s.width, h = s.height;
        float t = MenuBg.time();
        R2D.radiusMul = 1;
        MenuBg.render(ctx, s, w, h, mouseX, mouseY, net.minecraft.client.MinecraftClient.getInstance().getRenderTickCounter().getLastFrameDuration(), true, 0.45f, false);
        float in = Anim.easeOutCubic((System.currentTimeMillis() - since) / 500f);
        R2D.alpha = in;
        if (s instanceof DisconnectedScreen ds) { renderError(ctx, ds, mouseX, mouseY, in); return; }

        // ---- status i postęp zależnie od ekranu
        String head = t("Wczytywanie", "Loading");
        String status;
        float prog = -1;
        if (s instanceof ConnectScreen) {
            Text st = ((ConnectScreenAccessor) s).withclient$status();
            status = st != null ? st.getString() : t("Łączenie z serwerem", "Connecting to the server");
        } else if (s instanceof DownloadingTerrainScreen) {
            status = t("Pobieranie terenu", "Loading terrain");
        } else if (s instanceof LevelLoadingScreen) {
            WorldGenerationProgressTracker tr = ((LevelLoadingScreenAccessor) s).withclient$progress();
            prog = tr != null ? Math.max(0, Math.min(100, tr.getProgressPercentage())) / 100f : 0;
            status = t("Przygotowywanie świata", "Preparing the world");
        } else if (s instanceof ProgressScreen ps) {
            ProgressScreenAccessor a = (ProgressScreenAccessor) ps;
            Text tt = a.withclient$title(), task = a.withclient$task();
            status = (tt != null ? tt.getString() : "") + (task != null && !task.getString().isEmpty() ? " – " + task.getString() : "");
            if (a.withclient$progress() > 0) prog = a.withclient$progress() / 100f;
        } else {
            status = s.getTitle().getString();
        }
        String low = status.toLowerCase(Locale.ROOT);
        if (low.contains("zapis") || low.contains("saving")) head = t("Zapisywanie", "Saving");
        if (status.isBlank()) status = t("Proszę czekać", "Please wait");

        // ---- ułożenie: blok nad przyciskami ekranu (np. "Anuluj")
        float btnTop = h;
        for (Element e : s.children()) if (e instanceof ButtonWidget b && b.visible) btnTop = Math.min(btnTop, b.getY());
        boolean small = h < 280;
        float ring = small ? 30 : 38, logoS = small ? 36 : 46;
        float blockH = ring * 2 + 14 + 10 + 16 + 14 + 10 + 14 + 4;
        float area = Math.min(btnTop - 12, h - 30);
        float cy0 = Math.max(10, (area - blockH) / 2f) + (1 - in) * 8;
        float cx = w / 2f, cy = cy0 + ring;

        // pierścień-spinner wokół logo
        R2D.glow(ctx, cx - ring * 1.6f, cy - ring * 1.6f, ring * 3.2f, ring * 3.2f, R2D.withAlpha(0xFFFFFFFF, 0.06f));
        R2D.roundOutline(ctx, cx - ring, cy - ring, ring * 2, ring * 2, ring, 0.8f, 0x16FFFFFF);
        if (prog >= 0) {
            // postęp jako łuk pod wirującą kometą
            float p = progress.animate(prog);
            int dots = (int) (p * 120);
            for (int i = 0; i < dots; i++) {
                double a = -Math.PI / 2 + i / 120.0 * Math.PI * 2;
                R2D.circle(ctx, cx + (float) Math.cos(a) * ring, cy + (float) Math.sin(a) * ring, 1f, 0x88FFFFFF);
            }
        }
        double a0 = t * 4.2;
        for (int i = 0; i < 34; i++) {
            double a = a0 - i * 0.075;
            float k = 1 - i / 34f;
            R2D.circle(ctx, cx + (float) Math.cos(a) * ring, cy + (float) Math.sin(a) * ring, 0.5f + 0.8f * k, R2D.withAlpha(0xFFFFFFFF, k));
        }
        R2D.glow(ctx, cx + (float) Math.cos(a0) * ring - 6, cy + (float) Math.sin(a0) * ring - 6, 12, 12, 0x99FFFFFF);
        float pulse = 1 + 0.04f * (float) Math.sin(t * 3);
        float ls = logoS * pulse;
        logo(ctx, cx - ls / 2f, cy - ls / 2f, ls, 0.7f + 0.3f * (float) Math.sin(t * 3));

        // VESCA CLIENT
        float y = cy + ring + 14;
        float sw = spacedWidth("WITH CLIENT", Font.BOLD, 0.66f, 2.6f);
        spaced(ctx, "WITH CLIENT", cx - sw / 2f, y, DIM, Font.BOLD, 0.66f, 2.6f);
        y += 16;

        // Wczytywanie + kropki (stała szerokość, żeby napis nie skakał)
        float hs = small ? 1.05f : 1.25f;
        float hw = Fonts.width(head, Font.BOLD, hs), dw = Fonts.width("...", Font.BOLD, hs);
        float hx = cx - (hw + dw) / 2f;
        Fonts.draw(ctx, head, hx, y, TEXT, Font.BOLD, hs);
        int n = (int) (t * 2.5f) % 4;
        Fonts.draw(ctx, ".".repeat(n), hx + hw, y, TEXT, Font.BOLD, hs);
        y += Fonts.height(hs) + 6;

        // status + nazwa celu
        String line = target != null && !target.isBlank() && !(s instanceof ProgressScreen) ? status + "  •  " + target : status;
        line = Fonts.trim(line, Font.REGULAR, 0.7f, w - 40);
        Fonts.drawCentered(ctx, line, cx, y, DIM, Font.REGULAR, 0.7f);
        y += 14;

        // pasek postępu
        float bw = Math.min(200, w - 60), bx = cx - bw / 2f;
        R2D.roundRect(ctx, bx, y, bw, 3, 1.5f, 0x1CFFFFFF);
        if (prog >= 0) {
            float p = progress.animate(prog);
            R2D.roundRectH(ctx, bx, y, Math.max(3, bw * p), 3, 1.5f, 0xFF9A9AA2, 0xFFFFFFFF);
            R2D.glow(ctx, bx + bw * p - 6, y - 5, 12, 13, 0x88FFFFFF);
            String pc = Math.round(p * 100) + "%";
            Fonts.draw(ctx, pc, bx + bw + 6, y - 2, TEXT, Font.BOLD, 0.6f);
        } else {
            float seg = bw * 0.28f;
            float sx = bx + (bw - seg) * (0.5f + 0.5f * (float) Math.sin(t * 2.2f));
            R2D.roundRectH(ctx, sx, y, seg, 3, 1.5f, 0x00FFFFFF, 0xFFFFFFFF);
            R2D.glow(ctx, sx + seg - 6, y - 5, 12, 13, 0x77FFFFFF);
        }

        buttons(ctx, s, mouseX, mouseY, false);

        // wskazówka na dole
        String tipText = t("Wskazówka: ", "Tip: ") + tips()[tip % tips().length];
        float tipY = h - 16;
        Fonts.drawCentered(ctx, Fonts.trim(tipText, Font.REGULAR, 0.6f, w - 40), cx, tipY, FAINT, Font.REGULAR, 0.6f);
        R2D.alpha = 1;
    }

    /** przyciski ekranu (np. Anuluj) w stylu WithClient – kliknięcia obsługuje dalej sam przycisk */
    private static void buttons(DrawContext ctx, Screen s, int mouseX, int mouseY, boolean firstPrimary) {
        boolean first = true;
        for (Element e : s.children()) {
            if (!(e instanceof ButtonWidget b) || !b.visible) continue;
            boolean hv = mouseX >= b.getX() && mouseX < b.getX() + b.getWidth() && mouseY >= b.getY() && mouseY < b.getY() + b.getHeight();
            button(ctx, b.getX() + 20, b.getY(), b.getWidth() - 40, b.getHeight(), null, b.getMessage().getString(), firstPrimary && first, hv || b.isFocused() ? 1 : 0, b.active);
            first = false;
        }
    }

    /** ekran błędu połączenia / rozłączenia: logo, tytuł, powód i przyciski */
    private static void renderError(DrawContext ctx, DisconnectedScreen s, int mouseX, int mouseY, float in) {
        int w = s.width, h = s.height;
        float btnTop = h;
        for (Element e : s.children()) if (e instanceof ButtonWidget b && b.visible) btnTop = Math.min(btnTop, b.getY());
        String title = s.getTitle().getString();
        Text reasonText = ((DisconnectedScreenAccessor) s).withclient$info().reason();
        java.util.List<String> lines = Fonts.wrap(reasonText != null ? reasonText.getString().replace("\n", " ") : "", Font.REGULAR, 0.72f, Math.min(320, w - 60));
        if (lines.size() > 4) lines = lines.subList(0, 4);
        float logoS = 40;
        float blockH = logoS + 14 + 10 + 14 + 12 + 8 + lines.size() * 11;
        float top = Math.max(10, Math.min(btnTop - 16 - blockH, (h - blockH) / 2f - 20)) + (1 - in) * 8;
        float cx = w / 2f;
        R2D.glow(ctx, cx - 70, top - 30, 140, 120, R2D.withAlpha(0xFFFFFFFF, 0.05f));
        logo(ctx, cx - logoS / 2f, top, logoS, 0.5f);
        // mały znaczek "!" w rogu logo
        R2D.circle(ctx, cx + logoS / 2f - 2, top + 2, 6.5f, 0xFF0A0A0C);
        R2D.circle(ctx, cx + logoS / 2f - 2, top + 2, 5.2f, 0xFFF4F4F5);
        Fonts.drawCentered(ctx, "!", cx + logoS / 2f - 2, top + 2 - Fonts.height(0.62f) / 2f + 0.5f, BLACK, Font.BOLD, 0.62f);
        float y = top + logoS + 14;
        float sw = spacedWidth("WITH CLIENT", Font.BOLD, 0.66f, 2.6f);
        spaced(ctx, "WITH CLIENT", cx - sw / 2f, y, DIM, Font.BOLD, 0.66f, 2.6f);
        y += 16;
        Fonts.drawCentered(ctx, Fonts.trim(title, Font.BOLD, 1.1f, w - 40), cx, y, TEXT, Font.BOLD, 1.1f);
        y += Fonts.height(1.1f) + 8;
        for (String l : lines) { Fonts.drawCentered(ctx, l, cx, y, DIM, Font.REGULAR, 0.72f); y += 11; }
        buttons(ctx, s, mouseX, mouseY, true);
        R2D.alpha = 1;
    }
}
