package dev.vesca.client.ui;

import com.google.gson.*;
import dev.vesca.client.VescaClient;
import dev.vesca.client.module.Module;
import dev.vesca.client.module.Modules;
import dev.vesca.client.render.Fonts;
import dev.vesca.client.render.Fonts.Font;
import dev.vesca.client.render.R2D;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static dev.vesca.client.ui.Ui.*;

/**
 * HUD VescaClient – elementy rysowane na ekranie w grze (Watermark, lista modułów, keybindy, ekwipunek,
 * zbroja, koordynaty, efekty, TargetHUD, CPS, FPS, ping, powiadomienia). Każdy element można włączyć
 * i ustawić jego róg ekranu na stronie "HUD" w ClickGUI. Ustawienia w config/vescaclient/hud.json.
 */
public final class Hud {
    private Hud() {}

    // kotwice: index = ay*3 + ax (ax 0 lewo,1 środek,2 prawo; ay 0 góra,1 środek,2 dół)
    public static final String[] ANCHOR_PL = {"Lewy górny", "Górny środek", "Prawy górny", "Lewy środek", "Środek", "Prawy środek", "Lewy dolny", "Dolny środek", "Prawy dolny"};
    public static final String[] ANCHOR_EN = {"Top left", "Top center", "Top right", "Mid left", "Center", "Mid right", "Bottom left", "Bottom center", "Bottom right"};

    /** definicja elementu HUD */
    public static final class El {
        public final String id, icon, pl, en, descPl, descEn;
        public final boolean defOn;
        public final int defAnchor;
        public boolean on;
        public int anchor;
        El(String id, String icon, String pl, String en, String descPl, String descEn, boolean defOn, int defAnchor) {
            this.id = id; this.icon = icon; this.pl = pl; this.en = en; this.descPl = descPl; this.descEn = descEn;
            this.defOn = defOn; this.defAnchor = defAnchor; this.on = defOn; this.anchor = defAnchor;
        }
        public String name() { return Ui.t(pl, en); }
        public String desc() { return Ui.t(descPl, descEn); }
    }

    public static final List<El> ELEMENTS = new ArrayList<>();
    private static El add(String id, String icon, String pl, String en, String dpl, String den, boolean on, int anchor) {
        El e = new El(id, icon, pl, en, dpl, den, on, anchor); ELEMENTS.add(e); return e;
    }
    static {
        add("watermark", "g_hud_watermark", "Watermark", "Watermark", "Logo, godzina i FPS w rogu", "Logo, time and FPS in the corner", true, 0);
        add("modulelist", "g_hud_list", "Lista modułów", "Module list", "Włączone moduły (ArrayList)", "Enabled modules (ArrayList)", true, 2);
        add("keybinds", "g_hud_keybinds", "Keybindy", "Keybinds", "Moduły z przypisanym klawiszem", "Modules with a bound key", true, 3);
        add("notifications", "g_hud_notify", "Powiadomienia", "Notifications", "Dymki przy włączaniu modułów", "Toasts when toggling modules", true, 8);
        add("coords", "g_hud_coords", "Koordynaty", "Coordinates", "Pozycja XYZ i kierunek", "XYZ position and facing", false, 6);
        add("fps", "g_hud_fps", "Licznik FPS", "FPS counter", "Duży licznik klatek", "Large frame counter", false, 6);
        add("cps", "g_hud_cps", "CPS", "CPS", "Kliknięcia na sekundę (LPM/PPM)", "Clicks per second (LMB/RMB)", false, 3);
        add("ping", "g_hud_ping", "Ping", "Ping", "Opóźnienie do serwera", "Latency to the server", false, 6);
        add("armor", "g_hud_armor", "Zbroja", "Armor", "Zbroja i trzymany przedmiot", "Armor and held item", false, 7);
        add("inventory", "g_hud_inventory", "Ekwipunek", "Inventory", "Podgląd ekwipunku (EQ Custom)", "Inventory preview (custom)", false, 5);
        add("effects", "g_hud_effects", "Efekty", "Effects", "Aktywne mikstury z czasem", "Active potion effects", false, 5);
        add("targethud", "g_hud_target", "TargetHUD", "TargetHUD", "Cel na który patrzysz", "The entity you look at", false, 1);
    }
    public static El el(String id) { for (El e : ELEMENTS) if (e.id.equals(id)) return e; return null; }
    public static boolean on(String id) { El e = el(id); return e != null && e.on; }

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("vescaclient").resolve("hud.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // CPS
    private static final Deque<Long> leftClicks = new ArrayDeque<>(), rightClicks = new ArrayDeque<>();
    public static void click(boolean left) { (left ? leftClicks : rightClicks).add(System.currentTimeMillis()); }
    private static int cps(Deque<Long> d) {
        long now = System.currentTimeMillis();
        d.removeIf(t -> now - t > 1000);
        return d.size();
    }

    // powiadomienia
    private record Notif(String name, boolean enabled, long start) {}
    private static final List<Notif> notifs = new ArrayList<>();
    public static void onToggle(Module m) {
        if (!on("notifications")) return;
        notifs.add(0, new Notif(m.name, m.isEnabled(), System.currentTimeMillis()));
        if (notifs.size() > 6) notifs.remove(notifs.size() - 1);
    }

    public static void init() {
        load();
        HudRenderCallback.EVENT.register((ctx, tick) -> {
            try { render(ctx); } catch (Exception ignored) {}
        });
    }

    // ================================================================== render

    private static MinecraftClient mc;
    // bieżące offsety stosu dla każdej kotwicy
    private static final float[] stackTop = new float[9], stackBot = new float[9];
    private static final List<int[]> itemPos = new ArrayList<>();
    private static final List<ItemStack> itemStacks = new ArrayList<>();

    private static void render(DrawContext ctx) {
        mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        // nie rysuj nad menu/ekranami innymi niż ClickGUI (żeby był podgląd na żywo pod ClickGUI)
        if (mc.currentScreen != null && !(mc.currentScreen instanceof ClickGui)) return;
        Theme.apply();
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        float prevA = R2D.alpha; R2D.alpha = 1;
        Arrays.fill(stackTop, 0); Arrays.fill(stackBot, 0);
        itemPos.clear(); itemStacks.clear();
        float m = 6;
        for (El e : ELEMENTS) {
            if (!e.on) continue;
            float[] sz = size(e.id);
            if (sz == null) continue;
            float w = sz[0], h = sz[1];
            int ax = e.anchor % 3, ay = e.anchor / 3;
            float x = ax == 0 ? m : ax == 1 ? (sw - w) / 2f : sw - m - w;
            float y;
            if (ay == 0) { y = m + stackTop[e.anchor]; stackTop[e.anchor] += h + 5; }
            else if (ay == 2) {
                float base = e.anchor == 7 ? 42 : 6; // dolny środek nad paskiem przedmiotów
                y = sh - base - h - stackBot[e.anchor]; stackBot[e.anchor] += h + 5;
            } else { y = (sh - h) / 2f + stackTop[e.anchor]; stackTop[e.anchor] += h + 5; }
            draw(e.id, ctx, x, y, w, h);
        }
        // przedmioty rysujemy na końcu, w czystym stanie, żeby modele nie znikały po rysowaniu kształtów
        if (!itemStacks.isEmpty()) {
            ctx.draw();
            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            net.minecraft.client.render.DiffuseLighting.enableGuiDepthLighting();
            for (int i = 0; i < itemStacks.size(); i++) {
                int[] p = itemPos.get(i);
                ctx.drawItem(itemStacks.get(i), p[0], p[1]);
                ctx.drawItemInSlot(mc.textRenderer, itemStacks.get(i), p[0], p[1]);
            }
            ctx.draw();
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        }
        R2D.alpha = prevA;
    }

    private static String key(Module mod) {
        if (mod.key < 0) return "";
        String s = InputUtil.fromKeyCode(mod.key, 0).getLocalizedText().getString();
        return s.length() > 7 ? s.substring(0, 7) : s.toUpperCase(Locale.ROOT);
    }

    // ---- rozmiary ----
    private static float[] size(String id) {
        int acc = Theme.accent();
        switch (id) {
            case "watermark" -> {
                String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                String right = time + "  " + mc.getCurrentFps() + " FPS";
                return new float[]{10 + 18 + 7 + Fonts.width("Vesca", Font.BOLD, 0.72f) + Fonts.width("Client", Font.BOLD, 0.72f) + 9 + Fonts.width(right, Font.SEMIBOLD, 0.6f) + 11, 24};
            }
            case "modulelist" -> {
                List<Module> en = enabledSorted();
                if (en.isEmpty()) return null;
                float w = 0;
                for (Module mo : en) w = Math.max(w, Fonts.width(mo.name, Font.BOLD, 0.66f) + 18);
                return new float[]{w, en.size() * 15};
            }
            case "keybinds" -> {
                List<Module> b = bound();
                if (b.isEmpty()) return null;
                float w = Fonts.width(t("Keybindy", "Keybinds"), Font.BOLD, 0.6f);
                for (Module mo : b) w = Math.max(w, Fonts.width(mo.name, Font.SEMIBOLD, 0.62f) + 24 + Fonts.width(key(mo), Font.BOLD, 0.56f));
                return new float[]{w + 22, 17 + b.size() * 12 + 7};
            }
            case "notifications" -> {
                prune();
                if (notifs.isEmpty()) return null;
                float w = 0;
                for (Notif n : notifs) w = Math.max(w, 30 + Fonts.width((n.enabled ? t("Włączono ", "Enabled ") : t("Wyłączono ", "Disabled ")) + n.name, Font.BOLD, 0.62f) + 14);
                return new float[]{w, notifs.size() * 20 - 4};
            }
            case "coords" -> { return mc.player == null ? null : new float[]{Fonts.width(coordsText(), Font.BOLD, 0.66f) + 28, 20}; }
            case "fps" -> { String s = mc.getCurrentFps() + ""; return new float[]{Fonts.width(s, Font.DISPLAY, 0.7f) + Fonts.width(" FPS", Font.BOLD, 0.6f) + 20, Fonts.displayCap(0.7f) + 14}; }
            case "cps" -> { return new float[]{Fonts.width("L " + cps(leftClicks) + "   R " + cps(rightClicks), Font.BOLD, 0.66f) + 26, 20}; }
            case "ping" -> { return new float[]{Fonts.width(pingText(), Font.BOLD, 0.66f) + 26, 20}; }
            case "armor" -> { return new float[]{5 * 20 + 4, 30}; }
            case "inventory" -> { return new float[]{9 * 18 + 12, 18 + 3 * 18 + 8}; }
            case "effects" -> {
                var fx = mc.player.getStatusEffects();
                if (fx.isEmpty()) return null;
                float w = 0;
                for (StatusEffectInstance e : fx) w = Math.max(w, Fonts.width(effectName(e), Font.SEMIBOLD, 0.62f) + 14 + Fonts.width(effectTime(e), Font.BOLD, 0.56f) + 18);
                return new float[]{w + 10, fx.size() * 14 + 10};
            }
            case "targethud" -> {
                LivingEntity tgt = target();
                return tgt == null ? null : new float[]{168, 42};
            }
        }
        return null;
    }

    // ---- rysowanie ----
    private static void draw(String id, DrawContext ctx, float x, float y, float w, float h) {
        int acc = Theme.accent();
        switch (id) {
            case "watermark" -> {
                panel(ctx, x, y, w, h);
                float ts = h - 8, tx = x + 5, ty = y + 4;
                R2D.roundRectV(ctx, tx, ty, ts, ts, 5, Theme.accentTop(), Theme.accentBottom());
                R2D.icon(ctx, "vesca_logo", tx + 3, ty + 3, ts - 6, Theme.onAccent());
                float lx = tx + ts + 7, cy = y + h / 2f;
                Fonts.drawV(ctx, "Vesca", lx, cy, 0xFFFFFFFF, Font.BOLD, 0.72f);
                lx += Fonts.width("Vesca", Font.BOLD, 0.72f);
                Fonts.drawV(ctx, "Client", lx, cy, acc, Font.BOLD, 0.72f);
                lx += Fonts.width("Client", Font.BOLD, 0.72f) + 5;
                R2D.rect(ctx, lx, y + 6, 0.6f, h - 12, 0x33FFFFFF);
                String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                Fonts.drawV(ctx, time + "  " + mc.getCurrentFps() + " FPS", lx + 6, cy, 0xFFB9B9C2, Font.SEMIBOLD, 0.6f);
            }
            case "modulelist" -> {
                List<Module> en = enabledSorted();
                float ry = y;
                long now = System.currentTimeMillis();
                for (int i = 0; i < en.size(); i++) {
                    Module mo = en.get(i);
                    float pw = Fonts.width(mo.name, Font.BOLD, 0.66f) + 18;
                    float px = x + w - pw; // prawe wyrównanie – efekt schodków
                    int a1 = R2D.blend(acc, 0xFFFFFFFF, 0.1f);
                    R2D.rect(ctx, px, ry, pw, 14, 0xC8121216);
                    R2D.rect(ctx, px + pw - 1.6f, ry, 1.6f, 14, R2D.blend(acc, a1, 0.5f + 0.5f * (float) Math.sin(now / 320.0 - i * 0.5)));
                    R2D.gradientH(ctx, px, ry, pw, 14, R2D.withAlpha(acc, 0.12f), 0x00000000);
                    Fonts.drawV(ctx, mo.name, px + 7, ry + 7, 0xFFF2F2F5, Font.BOLD, 0.66f);
                    ry += 15;
                }
            }
            case "keybinds" -> {
                List<Module> b = bound();
                panel(ctx, x, y, w, h);
                R2D.icon(ctx, "g_hud_keybinds", x + 8, y + 6, 8, acc);
                Fonts.drawV(ctx, t("Keybindy", "Keybinds"), x + 20, y + 10, 0xFFF2F2F5, Font.BOLD, 0.6f);
                R2D.rect(ctx, x + 8, y + 17, w - 16, 0.5f, 0x18FFFFFF);
                float ry = y + 23;
                for (Module mo : b) {
                    Fonts.drawV(ctx, mo.name, x + 9, ry, 0xFFC9C9D2, Font.SEMIBOLD, 0.62f);
                    String k = key(mo);
                    float kw = Fonts.width(k, Font.BOLD, 0.56f) + 8;
                    R2D.roundRect(ctx, x + w - 8 - kw, ry - 5, kw, 10, 3, R2D.withAlpha(acc, 0.2f));
                    Fonts.drawV(ctx, k, x + w - 8 - kw + 4, ry, acc, Font.BOLD, 0.56f);
                    ry += 12;
                }
            }
            case "notifications" -> {
                long now = System.currentTimeMillis();
                float ry = y;
                for (Notif n : notifs) {
                    float t = (now - n.start) / 1000f, in = Anim.easeOutCubic(t / 0.3f), out = Anim.clamp01((t - 2.2f) / 0.4f);
                    float pw = w, px = x + w - pw + (1 - in) * 30 + out * 30;
                    float prevA = R2D.alpha; R2D.alpha = (1 - out);
                    panel(ctx, px, ry, pw, 16);
                    int c = n.enabled ? acc : 0xFF6A6A74;
                    R2D.circle(ctx, px + 10, ry + 8, 2.6f, c);
                    if (n.enabled) R2D.glow(ctx, px + 4, ry + 2, 12, 12, R2D.withAlpha(acc, 0.5f));
                    Fonts.drawV(ctx, (n.enabled ? t("Włączono ", "Enabled ") : t("Wyłączono ", "Disabled ")), px + 18, ry + 8, n.enabled ? 0xFFF2F2F5 : 0xFF9A9AA4, Font.BOLD, 0.62f);
                    float nx = px + 18 + Fonts.width(n.enabled ? t("Włączono ", "Enabled ") : t("Wyłączono ", "Disabled "), Font.BOLD, 0.62f);
                    Fonts.drawV(ctx, n.name, nx, ry + 8, 0xFFFFFFFF, Font.BOLD, 0.62f);
                    R2D.roundRectH(ctx, px + 4, ry + 14, (pw - 8) * (1 - Anim.clamp01(t / 2.6f)), 1.2f, 0.6f, R2D.blend(acc, 0xFFFFFFFF, 0.2f), acc);
                    R2D.alpha = prevA;
                    ry += 20;
                }
            }
            case "coords" -> { pill(ctx, x, y, w, h, "g_hud_coords", coordsText()); }
            case "cps" -> { pill(ctx, x, y, w, h, "g_hud_cps", "L " + cps(leftClicks) + "   R " + cps(rightClicks)); }
            case "ping" -> { pill(ctx, x, y, w, h, "g_hud_ping", pingText()); }
            case "fps" -> {
                panel(ctx, x, y, w, h);
                String s = mc.getCurrentFps() + "";
                Fonts.drawDisplay(ctx, s, x + 10 + Fonts.width(s, Font.DISPLAY, 0.7f) / 2f, y + 7, 0xFFFFFFFF, 0.7f);
                Fonts.drawV(ctx, "FPS", x + 10 + Fonts.width(s, Font.DISPLAY, 0.7f) + 5, y + h / 2f, acc, Font.BOLD, 0.6f);
            }
            case "armor" -> {
                panel(ctx, x, y, w, h);
                var inv = mc.player.getInventory();
                for (int i = 0; i < 4; i++) {
                    ItemStack st = inv.getArmorStack(3 - i);
                    drawSlot(ctx, st, x + 3 + i * 20, y + 5);
                }
                drawSlot(ctx, mc.player.getMainHandStack(), x + 3 + 4 * 20, y + 5);
            }
            case "inventory" -> {
                panel(ctx, x, y, w, h);
                R2D.icon(ctx, "g_hud_inventory", x + 8, y + 6, 8, acc);
                Fonts.drawV(ctx, t("Ekwipunek", "Inventory"), x + 20, y + 10, 0xFFF2F2F5, Font.BOLD, 0.6f);
                var inv = mc.player.getInventory();
                for (int i = 9; i < 36; i++) {
                    int c = (i - 9) % 9, r = (i - 9) / 9;
                    drawSlot(ctx, inv.getStack(i), x + 6 + c * 18, y + 18 + r * 18);
                }
            }
            case "effects" -> {
                panel(ctx, x, y, w, h);
                float ry = y + 5;
                for (StatusEffectInstance e : sortedEffects()) {
                    int col = 0xFF000000 | e.getEffectType().value().getColor();
                    R2D.circle(ctx, x + 8, ry + 5.5f, 2.6f, col);
                    Fonts.drawV(ctx, effectName(e), x + 14, ry + 5.5f, 0xFFE4E4E7, Font.SEMIBOLD, 0.62f);
                    String tm = effectTime(e);
                    Fonts.drawV(ctx, tm, x + w - 6 - Fonts.width(tm, Font.BOLD, 0.56f), ry + 5.5f, 0xFFADADB6, Font.BOLD, 0.56f);
                    ry += 14;
                }
            }
            case "targethud" -> {
                LivingEntity tgt = target();
                if (tgt == null) return;
                panel(ctx, x, y, w, h);
                drawHeadEntity(ctx, tgt, x + 6, y + 6, 30);
                float tx = x + 42;
                Fonts.drawV(ctx, Fonts.trim(tgt.getName().getString(), Font.BOLD, 0.74f, w - 52), tx, y + 11, 0xFFFFFFFF, Font.BOLD, 0.74f);
                float hp = Math.max(0, tgt.getHealth()), max = Math.max(1, tgt.getMaxHealth());
                float bw = w - 48 - 6;
                R2D.roundRect(ctx, tx, y + 20, bw, 4, 2, 0x33000000);
                int hc = hp / max > 0.5f ? 0xFF46D17A : hp / max > 0.25f ? 0xFFEBC24B : 0xFFE5544B;
                R2D.roundRectH(ctx, tx, y + 20, Math.max(2, bw * hp / max), 4, 2, R2D.blend(hc, 0x000000, 0.2f), hc);
                Fonts.drawV(ctx, String.format(Locale.ROOT, "%.0f / %.0f HP", hp, max), tx, y + 32, 0xFFC9C9D2, Font.SEMIBOLD, 0.56f);
                float dist = mc.player.distanceTo(tgt);
                String d = String.format(Locale.ROOT, "%.1f m", dist);
                Fonts.drawV(ctx, d, x + w - 6 - Fonts.width(d, Font.BOLD, 0.56f), y + 32, Theme.accent(), Font.BOLD, 0.56f);
            }
        }
    }

    // ---- helpery ----

    private static void panel(DrawContext ctx, float x, float y, float w, float h) {
        R2D.shadow(ctx, x, y + 1, w, h, 8, 0x66000000);
        R2D.roundRectV(ctx, x, y, w, h, 6, 0xD216161A, 0xDC0D0D10);
        R2D.roundOutline(ctx, x, y, w, h, 6, 0.6f, 0x1EFFFFFF);
        R2D.gradientH(ctx, x + 6, y + 0.6f, (w - 12) / 2f, 0.5f, 0x00FFFFFF, 0x30FFFFFF);
        R2D.gradientH(ctx, x + w / 2f, y + 0.6f, (w - 12) / 2f, 0.5f, 0x30FFFFFF, 0x00FFFFFF);
    }

    private static void pill(DrawContext ctx, float x, float y, float w, float h, String icon, String text) {
        panel(ctx, x, y, w, h);
        R2D.icon(ctx, icon, x + 8, y + h / 2f - 4, 8, Theme.accent());
        Fonts.drawV(ctx, text, x + 20, y + h / 2f, 0xFFF2F2F5, Font.BOLD, 0.66f);
    }

    private static void drawSlot(DrawContext ctx, ItemStack st, float x, float y) {
        R2D.roundRect(ctx, x, y, 18, 18, 4, 0x24000000);
        R2D.roundOutline(ctx, x, y, 18, 18, 4, 0.5f, 0x14FFFFFF);
        if (st == null || st.isEmpty()) return;
        itemPos.add(new int[]{(int) x + 1, (int) y + 1});
        itemStacks.add(st);
    }

    private static void drawHeadEntity(DrawContext ctx, LivingEntity e, float x, float y, float s) {
        R2D.roundRect(ctx, x, y, s, s, 7, 0x30000000);
        if (e instanceof PlayerEntity p) {
            var skin = mc.getSkinProvider().getSkinTextures(p.getGameProfile());
            ctx.draw();
            net.minecraft.client.gui.PlayerSkinDrawer.draw(ctx, skin, (int) x + 3, (int) y + 3, (int) (s - 6));
            ctx.draw();
        } else {
            R2D.icon(ctx, "g_hud_target", x + s / 2f - 7, y + s / 2f - 7, 14, Theme.accent());
        }
    }

    private static List<Module> enabledSorted() {
        List<Module> l = new ArrayList<>();
        for (Module m : Modules.all()) if (m.isEnabled() && !m.name.equals("HUD")) l.add(m);
        l.sort((a, b) -> Float.compare(Fonts.width(b.name, Font.BOLD, 0.66f), Fonts.width(a.name, Font.BOLD, 0.66f)));
        return l;
    }
    private static List<Module> bound() {
        List<Module> l = new ArrayList<>();
        for (Module m : Modules.all()) if (m.key >= 0) l.add(m);
        l.sort(Comparator.comparing(m -> m.name));
        return l;
    }
    private static String coordsText() {
        Entity e = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        return String.format(Locale.ROOT, "%.0f, %.0f, %.0f", e.getX(), e.getY(), e.getZ());
    }
    private static String pingText() {
        if (mc.getNetworkHandler() != null && mc.player != null) {
            PlayerListEntry pe = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (pe != null && pe.getLatency() > 0) return pe.getLatency() + " ms";
        }
        return mc.isInSingleplayer() ? t("lokalnie", "local") : "— ms";
    }
    private static LivingEntity target() {
        if (mc.targetedEntity instanceof LivingEntity le && le.isAlive() && le != mc.player) return le;
        return null;
    }
    private static List<StatusEffectInstance> sortedEffects() {
        List<StatusEffectInstance> l = new ArrayList<>(mc.player.getStatusEffects());
        l.sort((a, b) -> Integer.compare(b.getDuration(), a.getDuration()));
        return l;
    }
    private static String effectName(StatusEffectInstance e) {
        String n = e.getEffectType().value().getName().getString();
        int amp = e.getAmplifier();
        return amp > 0 ? n + " " + (amp + 1) : n;
    }
    private static String effectTime(StatusEffectInstance e) {
        if (e.isInfinite()) return "∞";
        int s = e.getDuration() / 20;
        return s / 60 + ":" + String.format("%02d", s % 60);
    }
    private static void prune() {
        long now = System.currentTimeMillis();
        notifs.removeIf(n -> now - n.start > 2600);
    }

    // ================================================================== zapis

    public static void save() {
        JsonObject o = new JsonObject();
        for (El e : ELEMENTS) {
            JsonObject j = new JsonObject();
            j.addProperty("on", e.on);
            j.addProperty("anchor", e.anchor);
            o.add(e.id, j);
        }
        try { Files.createDirectories(FILE.getParent()); Files.writeString(FILE, GSON.toJson(o), StandardCharsets.UTF_8); } catch (Exception ignored) {}
    }

    private static void load() {
        try {
            if (!Files.exists(FILE)) return;
            JsonObject o = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
            for (El e : ELEMENTS) {
                if (!o.has(e.id)) continue;
                JsonObject j = o.getAsJsonObject(e.id);
                if (j.has("on")) e.on = j.get("on").getAsBoolean();
                if (j.has("anchor")) e.anchor = Math.max(0, Math.min(8, j.get("anchor").getAsInt()));
            }
        } catch (Exception ignored) {}
    }
}
