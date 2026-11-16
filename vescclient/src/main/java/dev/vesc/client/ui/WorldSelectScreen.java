package dev.vesc.client.ui;

import dev.vesc.client.lang.Lang;
import dev.vesc.client.render.Fonts;
import dev.vesc.client.render.Fonts.Font;
import dev.vesc.client.render.R2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Wybór świata singleplayer w stylu VescClient: lista światów, gra, tworzenie nowego. */
public class WorldSelectScreen extends VescScreen {
    private List<LevelSummary> worlds = List.of();
    private boolean loading = true;

    public WorldSelectScreen(Screen parent) { super(parent, "Singleplayer"); }

    @Override protected String subtitle() { return Lang.pl() ? "Wybierz świat do gry" : "Pick a world to play"; }
    @Override protected String subtitleEmpty() { return loading ? (Lang.pl() ? "Wczytywanie..." : "Loading...") : (Lang.pl() ? "Brak światów – utwórz nowy" : "No worlds yet – create one"); }

    @Override
    protected void onOpen() { load(); }

    @SuppressWarnings("unchecked")
    private void load() {
        loading = true;
        try {
            LevelStorage ls = mc.getLevelStorage();
            ls.loadSummaries(ls.getLevelList()).thenAcceptAsync(list -> {
                worlds = (List<LevelSummary>) list;
                loading = false;
            }, (java.util.concurrent.Executor) mc);
        } catch (Exception e) { loading = false; worlds = List.of(); }
    }

    @Override protected float rowHeight() { return 40; }
    @Override protected int rowCount() { return worlds.size(); }

    @Override
    protected void drawRow(DrawContext ctx, int i, float x, float y, float w, float h, boolean hovered, boolean sel) {
        LevelSummary s = worlds.get(i);
        int accent = accent();
        float bg = anim("row" + i).animate(hovered || sel ? 1 : 0);
        R2D.roundRect(ctx, x, y, w, h, 11, R2D.blend(0x14FFFFFF, 0x24FFFFFF, bg));
        R2D.roundOutline(ctx, x, y, w, h, 11, 0.5f, sel ? R2D.withAlpha(accent, 0.8f) : R2D.withAlpha(0xFFFFFFFF, 0.06f + 0.08f * bg));
        if (sel) R2D.roundRect(ctx, x + 2, y + h / 2f - 8, 2.5f, 16, 1.25f, accent);

        monogram(ctx, x + 7, y + 7, h - 14, s.getDisplayName(), accent);
        float tx = x + h, tw = w - h - 100;
        Fonts.drawV(ctx, Fonts.trim(s.getDisplayName(), Font.BOLD, 0.84f, tw), tx, y + 13, 0xFFF4F3F8, Font.BOLD, 0.84f);
        String mode = s.getGameMode().getSimpleTranslatableName().getString();
        if (s.isHardcore()) mode = "Hardcore";
        String meta = s.getName() + "  •  " + mode;
        Fonts.drawV(ctx, Fonts.trim(meta, Font.REGULAR, 0.62f, tw), tx, y + 26, 0xFF7E7E8A, Font.REGULAR, 0.62f);

        // prawy: data + wersja
        long lp = s.getLastPlayed();
        if (lp > 0) {
            String date = DateTimeFormatter.ofPattern(Lang.pl() ? "d MMM yyyy" : "MMM d, yyyy", Lang.pl() ? Locale.forLanguageTag("pl-PL") : Locale.ENGLISH)
                    .format(Instant.ofEpochMilli(lp).atZone(ZoneId.systemDefault()));
            Fonts.drawV(ctx, date, x + w - 12 - Fonts.width(date, Font.SEMIBOLD, 0.6f), y + 14, 0xFFADADB6, Font.SEMIBOLD, 0.6f);
        }
        String ver = s.getVersion().getString();
        Fonts.drawV(ctx, ver, x + w - 12 - Fonts.width(ver, Font.REGULAR, 0.56f), y + 27, 0xFF60606A, Font.REGULAR, 0.56f);
        if (!s.isSelectable()) R2D.roundRect(ctx, x, y, w, h, 11, R2D.withAlpha(0xFF000000, 0.35f));
    }

    @Override
    protected void onRow(int i, int button, boolean dbl) {
        if (dbl) play(worlds.get(i));
    }

    private void play(LevelSummary s) {
        if (s == null || !s.isSelectable()) return;
        mc.createIntegratedServerLoader().start(s.getName(), () -> mc.setScreen(this));
    }

    @Override
    protected void drawFooter(DrawContext ctx, float x, float y, float w) {
        float g = 7, bw = (w - g * 2) / 3f, h = 26;
        footBtn(ctx, "play", x, y, bw, h, "player", Lang.pl() ? "Graj" : "Play", true, false,
                () -> { if (selected >= 0 && selected < rowCount()) play(worlds.get(selected)); });
        footBtn(ctx, "create", x + (bw + g), y, bw, h, "plus", Lang.pl() ? "Nowy świat" : "Create", false, false,
                () -> CreateWorldScreen.create(mc, this));
        footBtn(ctx, "refresh", x + (bw + g) * 2, y, bw, h, "refresh", Lang.pl() ? "Odśwież" : "Refresh", false, false, this::load);
    }
}
