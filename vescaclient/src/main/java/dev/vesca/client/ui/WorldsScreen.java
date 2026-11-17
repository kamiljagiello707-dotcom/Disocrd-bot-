package dev.vesca.client.ui;

import dev.vesca.client.render.Fonts;
import dev.vesca.client.render.Fonts.Font;
import dev.vesca.client.render.R2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.EditWorldScreen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static dev.vesca.client.ui.Ui.*;

/** Wybór świata VescaClient: lista światów z ikonami, gra, tworzenie, edycja i usuwanie. */
public class WorldsScreen extends VescaScreen {
    private List<LevelSummary> worlds = List.of();
    private boolean loading = true;
    private final Map<String, Identifier> icons = new HashMap<>();

    public WorldsScreen(Screen parent) { super(parent, Ui.t("Tryb jednoosobowy", "Singleplayer")); }

    @Override
    protected void init() { load(); }

    @SuppressWarnings("unchecked")
    private void load() {
        loading = true;
        try {
            LevelStorage ls = mc.getLevelStorage();
            ls.loadSummaries(ls.getLevelList()).thenAcceptAsync(list -> {
                worlds = new ArrayList<>((List<LevelSummary>) list);
                loading = false;
                if (selected >= worlds.size()) selected = -1;
            }, mc);
        } catch (Exception e) {
            loading = false;
            worlds = List.of();
        }
    }

    @Override
    protected void back() {
        for (Identifier id : icons.values()) if (id != null) mc.getTextureManager().destroyTexture(id);
        icons.clear();
        super.back();
    }

    // ------------------------------------------------------------------ lista

    @Override protected String subtitle() {
        if (loading) return t("Wczytywanie listy światów…", "Loading worlds…");
        int n = worlds.size();
        return n == 0 ? t("Utwórz swój pierwszy świat", "Create your first world")
                : t(n + (n == 1 ? " świat" : n < 5 ? " światy" : " światów") + " • wybierz i graj", n + (n == 1 ? " world" : " worlds") + " • pick one and play");
    }
    @Override protected String emptyText() { return loading ? t("Wczytywanie…", "Loading…") : t("Brak światów – kliknij „Nowy świat”", "No worlds yet – click \"Create\""); }
    @Override protected String emptyIcon() { return "world"; }
    @Override protected int rowCount() { return worlds.size(); }
    @Override protected String rowSearch(int i) { LevelSummary s = worlds.get(i); return s.getDisplayName() + " " + s.getName(); }
    @Override protected float rowHeight() { return 42; }
    @Override protected String hint() { return t("wybierz • dwuklik uruchamia świat", "select • double-click plays"); }

    private Identifier icon(LevelSummary s) {
        if (icons.containsKey(s.getName())) return icons.get(s.getName());
        Identifier id = null;
        try {
            Path p = s.getIconPath();
            if (p != null && Files.isRegularFile(p)) {
                try (InputStream in = Files.newInputStream(p)) {
                    NativeImage img = NativeImage.read(in);
                    id = Identifier.of("vescaclient", "worlds/" + Integer.toHexString(s.getName().hashCode()));
                    mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
                }
            }
        } catch (Exception ignored) { id = null; }
        icons.put(s.getName(), id);
        return id;
    }

    @Override
    protected void drawRow(DrawContext ctx, int i, float x, float y, float w, float h, float hover, boolean sel) {
        LevelSummary s = worlds.get(i);
        float is = h - 12;
        Identifier tex = icon(s);
        if (tex != null) {
            R2D.roundTexture(ctx, tex, x + 10, y + 6, is, is, is * 0.24f, 0, 0, 1, 1, 0xFFFFFFFF);
            R2D.roundOutline(ctx, x + 10, y + 6, is, is, is * 0.24f, 0.6f, 0x2AFFFFFF);
        } else monogram(ctx, x + 10, y + 6, is, s.getDisplayName());

        float tx = x + 10 + is + 10, tw = w - (tx - x) - 100;
        Fonts.draw(ctx, Fonts.trim(s.getDisplayName(), Font.BOLD, 0.82f, tw), tx, y + 7, TEXT, Font.BOLD, 0.82f);
        String details = s.getDetails().getString();
        Fonts.draw(ctx, Fonts.trim(details, Font.REGULAR, 0.6f, tw), tx, y + 19, DIM, Font.REGULAR, 0.6f);
        Fonts.draw(ctx, Fonts.trim(s.getName(), Font.REGULAR, 0.56f, tw), tx, y + 29, FAINT, Font.REGULAR, 0.56f);

        // prawa strona: data + wersja / tag Hardcore
        float rx = x + w - 12;
        long lp = s.getLastPlayed();
        if (lp > 0) {
            String date = DateTimeFormatter.ofPattern(pl() ? "d MMM yyyy, HH:mm" : "MMM d, yyyy, HH:mm", pl() ? Locale.forLanguageTag("pl-PL") : Locale.ENGLISH)
                    .format(Instant.ofEpochMilli(lp).atZone(ZoneId.systemDefault()));
            Fonts.draw(ctx, date, rx - Fonts.width(date, Font.SEMIBOLD, 0.6f), y + 8, DIM, Font.SEMIBOLD, 0.6f);
        }
        String tag = s.isHardcore() ? "HARDCORE" : s.getVersion().getString();
        float tw2 = Fonts.width(tag, Font.BOLD, 0.52f) + 10;
        boolean hc = s.isHardcore();
        R2D.roundRect(ctx, rx - tw2, y + 22, tw2, 11, 5.5f, hc ? 0xFFF4F4F5 : 0x1CFFFFFF);
        Fonts.drawV(ctx, tag, rx - tw2 + 5, y + 27.5f, hc ? BLACK : DIM, Font.BOLD, 0.52f);
        if (!s.isSelectable() || s.isLocked()) R2D.roundRect(ctx, x, y, w, h, 11, 0x66000000);
    }

    // ------------------------------------------------------------------ akcje

    @Override protected void activate(int i) { play(worlds.get(i)); }

    private void play(LevelSummary s) {
        if (s == null) return;
        Loading.target = s.getDisplayName();
        mc.createIntegratedServerLoader().start(s.getName(), () -> mc.setScreen(this));
    }

    private void edit() {
        if (!hasSelection()) return;
        LevelSummary s = worlds.get(selected);
        try {
            LevelStorage.Session session = mc.getLevelStorage().createSession(s.getName());
            mc.setScreen(EditWorldScreen.create(mc, session, edited -> {
                session.tryClose();
                if (edited) icons.remove(s.getName());
                load();
                mc.setScreen(this);
            }));
        } catch (Exception e) {
            SystemToast.addWorldAccessFailureToast(mc, s.getName());
            load();
        }
    }

    private void delete() {
        if (!hasSelection()) return;
        LevelSummary s = worlds.get(selected);
        mc.setScreen(new ConfirmScreen(ok -> {
            if (ok) {
                try (LevelStorage.Session session = mc.getLevelStorage().createSessionWithoutSymlinkCheck(s.getName())) {
                    session.deleteSessionLock();
                } catch (Exception e) {
                    SystemToast.addWorldDeleteFailureToast(mc, s.getName());
                }
                Identifier id = icons.remove(s.getName());
                if (id != null) mc.getTextureManager().destroyTexture(id);
                selected = -1;
                load();
            }
            mc.setScreen(this);
        }, Text.translatable("selectWorld.deleteQuestion"), Text.translatable("selectWorld.deleteWarning", s.getDisplayName()),
                Text.translatable("selectWorld.deleteButton"), ScreenTexts.CANCEL));
    }

    @Override protected void rightClick(int i) { edit(); }

    @Override
    protected List<Btn> buttons() {
        boolean sel = hasSelection();
        return List.of(
                new Btn("play", "arrow_right", t("Graj", "Play"), true, sel, () -> play(worlds.get(selected))),
                new Btn("create", "plus", t("Nowy świat", "Create"), false, true, () -> CreateWorldScreen.create(mc, this)),
                new Btn("edit", "pen", t("Edytuj", "Edit"), false, sel, this::edit),
                new Btn("delete", "trash", t("Usuń", "Delete"), false, sel, this::delete),
                new Btn("refresh", "refresh", t("Odśwież", "Refresh"), false, true, this::load)
        );
    }
}
