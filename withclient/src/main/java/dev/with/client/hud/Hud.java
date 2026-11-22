package dev.with.client.hud;

import com.google.gson.*;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.with.client.module.Category;
import dev.with.client.module.Module;
import dev.with.client.module.Modules;
import dev.with.client.module.Setting;
import dev.with.client.render.Fonts;
import dev.with.client.render.Fonts.Font;
import dev.with.client.render.R2D;
import dev.with.client.ui.Anim;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static dev.with.client.ui.Ui.t;

/**
 * HUD WithClient: elementy na ekranie (Watermark, ArrayList, Keybinds, TargetHUD, koordynaty, FPS, ping,
 * TPS, zbroja, efekty, ekwipunek, powiadomienia, prędkość, zegar, serwer, kompas, CPS, Keystrokes,
 * pasek zdrowia, sesja, pamięć, totemy). Element pokazuje się, gdy jego moduł (kategoria HUD) jest włączony.
 * Pozycje: domyślnie elementy układają się same w stosy przy krawędziach, a po przesunięciu
 * w edytorze trzymają się najbliższego rogu/krawędzi (config/withclient/hud.json).
 */
public final class Hud {
    private Hud() {}

    static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("withclient").resolve("hud.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static final int TEXT = 0xFFF4F4F5, DIM = 0xFFA1A1AA, FAINT = 0xFF6B6B75;

    // ================================================================== układ

    /** pozycja elementu: custom = przesunięty ręcznie (kotwica ax/ay: 0 lewo/góra, 1 środek, 2 prawo/dół) */
    public static final class Layout {
        public boolean custom;
        public int ax, ay;
        public float dx, dy, scale = 1;
    }

    /** grupa, w której elementy układają się same jeden pod/nad drugim */
    private record Group(int ax, int ay, float mx, float my, int dir) {}

    private static final Map<String, Group> GROUPS = Map.of(
            "TL", new Group(0, 0, 5, 5, 1), "TR", new Group(2, 0, 5, 5, 1),
            "BL", new Group(0, 2, 5, 5, -1), "BR", new Group(2, 2, 5, 5, -1),
            "TC", new Group(1, 0, 0, 5, 1), "LC", new Group(0, 1, 5, 0, 1),
            "RC", new Group(2, 1, 5, 0, 1), "BC", new Group(1, 2, 0, 58, -1));

    /** kolejność elementów = kolejność w stosie */
    static final LinkedHashMap<String, String> ELEMENTS = new LinkedHashMap<>();
    static {
        for (String[] e : new String[][]{
                {"Watermark", "TL"}, {"Keybinds", "TL"}, {"Session", "TL"},
                {"ArrayList", "TR"}, {"Potions", "RC"}, {"Spotify", "TC"}, {"Compass", "TC"}, {"Keystrokes", "LC"},
                {"HealthBar", "BC"}, {"ArmorHUD", "HB"}, {"TargetHUD", "CT"},
                {"InventoryHUD", "BR"}, {"Coordinates", "BR"}, {"TotemCounter", "BR"}, {"FPS", "BR"}, {"Ping", "BR"}, {"TPS", "BR"},
                {"Speedometer", "BR"}, {"CPS", "BR"}, {"Memory", "BR"}, {"Clock", "BR"}, {"ServerInfo", "BR"}}) ELEMENTS.put(e[0], e[1]);
    }
    static final Map<String, Layout> LAYOUT = new HashMap<>();

    public static Layout layout(String id) { return LAYOUT.computeIfAbsent(id, k -> new Layout()); }

    /** prostokąt elementu na ekranie (jednostki GUI, po skali) */
    public record Box(String id, float x, float y, float w, float h, float scale, int ax, int ay) {}

    // ================================================================== stan

    private static final Deque<Long> LEFT = new ArrayDeque<>(), RIGHT = new ArrayDeque<>();
    private static LivingEntity lastTarget, shownTarget;
    private static long lastAttack;
    private static final Anim targetA = new Anim(0, 12), hpA = new Anim(0, 9), hpTrail = new Anim(0, 2.5f), barA = new Anim(1, 9);
    private static final float[] fpsHist = new float[80], tpsHist = new float[80], speedHist = new float[80];
    private static int histPos;
    private static float speed, tpsNow = 20;
    private static long lastTimeNs = -1, lastWorldTime;
    private static final float[] tpsSamples = new float[6];
    private static int tpsCount, tpsIdx;
    private static Object handler;
    private static long sessionStart = System.currentTimeMillis();
    private static double distance, lastX = Double.NaN, lastZ;
    private static int deaths;
    private static boolean wasDead;
    private static final Map<String, Anim> AL = new HashMap<>();
    private static final Map<String, Integer> effectMax = new HashMap<>();
    private record Notif(String icon, String name, boolean on, long time) {}
    private static final List<Notif> NOTIFS = new ArrayList<>();
    private static final Map<String, Anim> KEYS = new HashMap<>();

    // kolejka przedmiotów (rysowane na końcu z czystym buforem głębi)
    private record QItem(Matrix4f m, ItemStack st, int x, int y, boolean overlay) {}
    private static final List<QItem> ITEMS = new ArrayList<>();

    // ================================================================== start

    public static void init() {
        load();
        HudRenderCallback.EVENT.register((ctx, tc) -> {
            if (mc.options.hudHidden || mc.player == null || mc.currentScreen instanceof HudEditor) return;
            renderAll(ctx, false);
            drawNotifications(ctx);
        });
        ClientTickEvents.END_CLIENT_TICK.register(Hud::tick);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && entity instanceof LivingEntity le) { lastTarget = le; lastAttack = System.currentTimeMillis(); }
            return ActionResult.PASS;
        });
    }

    private static void tick(MinecraftClient c) {
        if (c.player == null) { lastX = Double.NaN; return; }
        if (c.getNetworkHandler() != handler) {
            handler = c.getNetworkHandler();
            sessionStart = System.currentTimeMillis(); distance = 0; deaths = 0; lastTimeNs = -1; tpsCount = 0; tpsNow = 20;
        }
        double x = c.player.getX(), z = c.player.getZ();
        if (!Double.isNaN(lastX)) {
            double d = Math.hypot(x - lastX, z - lastZ);
            if (d < 20) { distance += d; speed = speed * 0.6f + (float) d * 20 * 0.4f; }
        }
        lastX = x; lastZ = z;
        boolean dead = c.player.isDead();
        if (dead && !wasDead) deaths++;
        wasDead = dead;
        histPos = (histPos + 1) % fpsHist.length;
        fpsHist[histPos] = c.getCurrentFps();
        tpsHist[histPos] = tpsNow;
        speedHist[histPos] = speed;
    }

    public static void click(boolean left) { (left ? LEFT : RIGHT).addLast(System.currentTimeMillis()); }

    private static int cps(Deque<Long> d) {
        long now = System.currentTimeMillis();
        while (!d.isEmpty() && now - d.peekFirst() > 1000) d.pollFirst();
        return d.size();
    }

    /** pakiet czasu świata (co 20 ticków serwera) → szacowane TPS */
    public static void onWorldTime(long worldTime) {
        long now = System.nanoTime();
        if (lastTimeNs > 0) {
            double secs = (now - lastTimeNs) / 1e9;
            long ticks = worldTime - lastWorldTime;
            if (secs > 0.25 && ticks > 0 && ticks < 2000) {
                tpsSamples[tpsIdx] = (float) Math.min(20, ticks / secs);
                tpsIdx = (tpsIdx + 1) % tpsSamples.length;
                tpsCount = Math.min(tpsCount + 1, tpsSamples.length);
                float s = 0; for (int i = 0; i < tpsCount; i++) s += tpsSamples[i];
                tpsNow = s / tpsCount;
            }
        }
        lastTimeNs = now; lastWorldTime = worldTime;
    }

    /** powiadomienie o przełączeniu modułu klawiszem */
    public static void notify(Module m) {
        Module n = Modules.get("Notifications");
        if (n == null || !n.isEnabled() || !bo(n, "Przełączanie modułów")) return;
        if (m.category == Category.HUD && m == n) return;
        NOTIFS.add(new Notif(m.icon, m.name, m.isEnabled(), System.currentTimeMillis()));
        while (NOTIFS.size() > 5) NOTIFS.remove(0);
        if (bo(n, "Dźwięk")) mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), m.isEnabled() ? 1.8f : 1.4f, 0.25f));
    }

    // ================================================================== ustawienia

    static Module mod(String n) { return Modules.get(n); }

    /** element Potions zastępuje ikony efektów Minecrafta */
    public static boolean hideVanillaEffects() { Module p = mod("Potions"); return p != null && p.isEnabled(); }

    /** wysokość ikon efektów Minecrafta w prawym górnym rogu (gdy są widoczne) */
    private static float vanillaEffectsHeight() {
        if (hideVanillaEffects() || mc.player == null || mc.player.getStatusEffects().isEmpty()) return 0;
        boolean good = false, bad = false;
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            if (!e.shouldShowIcon()) continue;
            if (e.getEffectType().value().isBeneficial()) good = true; else bad = true;
        }
        return (good ? 25 : 0) + (bad ? 26 : 0);
    }

    // górna krawędź stosu w prawym dolnym rogu i dolna w prawym górnym (powiadomienia omijają elementy)
    private static float brTop = -1, trBottom = -1;
    static boolean bo(Module m, String s) { return m != null && m.setting(s) instanceof Setting.Bool b && b.get(); }
    static float num(Module m, String s, float d) { return m != null && m.setting(s) instanceof Setting.Num n ? n.get().floatValue() : d; }
    static String mode(Module m, String s, String d) { return m != null && m.setting(s) instanceof Setting.Mode o ? o.get() : d; }
    static boolean has(Module m, String s, String opt) { return m != null && m.setting(s) instanceof Setting.Multi mu && mu.has(opt); }
    static int color(Module m, String s, int d) { return m != null && m.setting(s) instanceof Setting.Color c ? c.get() : d; }
    static String text(Module m, String s, String d) { return m != null && m.setting(s) instanceof Setting.Text tx ? tx.get() : d; }

    // wspólny styl (moduł HUDEditor → grupa Wygląd)
    private static String bg = "Dark";
    private static float opacity = 0.9f, radius = 6, gScale = 1;
    private static boolean outline = true, shadow = true, icons = true;
    static int accent = 0xFFFFFFFF;

    private static void readStyle() {
        Module e = mod("HUDEditor");
        bg = mode(e, "Tło", "Dark");
        opacity = num(e, "Przezroczystość", 90) / 100f;
        radius = num(e, "Zaokrąglenie", 6);
        gScale = num(e, "Skala", 1);
        outline = e == null || bo(e, "Obrys");
        shadow = e == null || bo(e, "Cień");
        icons = e == null || bo(e, "Ikony");
        accent = color(e, "Akcent", 0xFFFFFFFF);
    }

    static float globalScale() { return gScale; }

    /** HUD ma tę samą wielkość na ekranie niezależnie od skali GUI Minecrafta (na 1080p z auto-skalą jest mniejszy i czystszy) */
    static float base() {
        double gs = mc.getWindow().getScaleFactor();
        return (float) Math.max(0.5, Math.min(1.5, 2.5 / gs));
    }

    /** elementy dopasowane do paska przedmiotów Minecrafta mają jego skalę */
    private static boolean vanillaSized(String id) { return id.equals("HealthBar") || id.equals("ArmorHUD"); }

    static int onAccent() {
        float r = ((accent >> 16) & 0xFF) / 255f, g = ((accent >> 8) & 0xFF) / 255f, b = (accent & 0xFF) / 255f;
        return 0.2126f * r + 0.7152f * g + 0.0722f * b > 0.55f ? 0xFF09090B : 0xFFFFFFFF;
    }

    static void panel(DrawContext ctx, float x, float y, float w, float h) { panel(ctx, x, y, w, h, Math.min(radius, h / 2f)); }

    static void panel(DrawContext ctx, float x, float y, float w, float h, float r) {
        if (bg.equals("Clear")) return;
        if (shadow) R2D.shadow(ctx, x, y + 1.5f, w, h, 7, R2D.withAlpha(0xFF000000, 0.5f * opacity));
        boolean glass = bg.equals("Glass");
        R2D.roundRectV(ctx, x, y, w, h, r, R2D.withAlpha(glass ? 0xFF1E1E24 : 0xFF141417, (glass ? 0.55f : 0.94f) * opacity),
                R2D.withAlpha(glass ? 0xFF101014 : 0xFF0B0B0D, (glass ? 0.55f : 0.94f) * opacity));
        if (glass) R2D.roundRectV(ctx, x, y, w, h * 0.5f, r, 0x14FFFFFF, 0x00FFFFFF);
        if (outline) R2D.roundOutline(ctx, x, y, w, h, r, 0.5f, R2D.withAlpha(0xFFFFFFFF, 0.16f * Math.max(0.5f, opacity)));
    }

    /** przedmiot do narysowania na końcu (scale = rozmiar w px / 16) */
    static void item(DrawContext ctx, ItemStack st, float x, float y, float size, boolean overlay) {
        if (st == null || st.isEmpty()) return;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(size / 16f, size / 16f, 1);
        ITEMS.add(new QItem(new Matrix4f(ctx.getMatrices().peek().getPositionMatrix()), st.copy(), 0, 0, overlay));
        ctx.getMatrices().pop();
    }

    private static void flushItems(DrawContext ctx) {
        if (ITEMS.isEmpty()) return;
        ctx.draw();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (QItem q : ITEMS) {
            ctx.getMatrices().push();
            ctx.getMatrices().peek().getPositionMatrix().set(q.m());
            ctx.drawItem(q.st(), q.x(), q.y());
            if (q.overlay()) ctx.drawItemInSlot(mc.textRenderer, q.st(), q.x(), q.y());
            ctx.getMatrices().pop();
        }
        ctx.draw();
        ITEMS.clear();
    }

    // ================================================================== rysowanie wszystkiego

    private static boolean curRight, curBottom, preview;

    /** rysuje włączone elementy; zwraca ich prostokąty (dla edytora) */
    public static List<Box> renderAll(DrawContext ctx, boolean editor) {
        preview = editor;
        readStyle();
        float prevA = R2D.alpha, prevR = R2D.radiusMul;
        R2D.alpha = 1; R2D.radiusMul = 1;
        float sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        record Item(String id, Module m, float[] size, float s, float w, float h) {}
        float bs = base(), mg = Math.max(0.7f, bs), gap = 4 * mg;
        List<Item> items = new ArrayList<>();
        for (Map.Entry<String, String> en : ELEMENTS.entrySet()) {
            Module m = mod(en.getKey());
            if (m == null || !m.isEnabled()) continue;
            float s = gScale * layout(en.getKey()).scale * (vanillaSized(en.getKey()) ? 1 : bs);
            float[] size = size(en.getKey(), m);
            if (size == null) continue;
            items.add(new Item(en.getKey(), m, size, s, size[0] * s, size[1] * s));
        }
        Map<String, Box> placed = new LinkedHashMap<>();
        Map<String, Float> next = new HashMap<>();
        for (int pass = 0; pass < 2; pass++) {
            for (Item it : items) {
                Layout L = layout(it.id());
                if (L.custom != (pass == 0)) continue;
                float w = it.w(), h = it.h(), x, y; int ax, ay, dir = 1;
                if (L.custom) {
                    ax = L.ax; ay = L.ay;
                    x = ax == 0 ? L.dx : ax == 1 ? sw / 2f + L.dx - w / 2f : sw - L.dx - w;
                    y = ay == 0 ? L.dy : ay == 1 ? sh / 2f + L.dy - h / 2f : sh - L.dy - h;
                } else {
                    String gk = ELEMENTS.get(it.id());
                    if (gk.equals("HB")) { ax = 1; ay = 2; x = sw / 2f + 96; y = sh - h - 3; dir = -1; }
                    else if (gk.equals("CT")) { ax = 1; ay = 1; x = sw / 2f + 22; y = sh / 2f + 18; }
                    else {
                        Group g = GROUPS.get(gk);
                        ax = g.ax(); ay = g.ay(); dir = g.dir();
                        Float nv = next.get(gk);
                        float gx = g.mx() * mg, gy = gk.equals("BC") ? g.my() : g.my() * mg;
                        x = ax == 0 ? gx : ax == 1 ? (sw - w) / 2f : sw - gx - w;
                        if (ay == 0) y = nv != null ? nv : gy + (gk.equals("TR") ? vanillaEffectsHeight() : 0);
                        else if (ay == 2) y = (nv != null ? nv : sh - gy) - h;
                        else y = nv != null ? nv : sh / 2f - h / 2f;
                    }
                    // omijanie innych elementów w kierunku stosu
                    for (int k = 0; k < 16; k++) {
                        Box hitBox = null;
                        for (Box o : placed.values()) if (x < o.x() + o.w() + 2 && x + w > o.x() - 2 && y < o.y() + o.h() + 2 && y + h > o.y() - 2) { hitBox = o; break; }
                        if (hitBox == null) break;
                        y = dir > 0 ? hitBox.y() + hitBox.h() + gap : hitBox.y() - h - gap;
                    }
                    if (GROUPS.containsKey(gk)) next.put(gk, ay == 2 ? y - gap : y + h + gap);
                }
                x = Math.round(x * 2) / 2f; y = Math.round(y * 2) / 2f;
                placed.put(it.id(), new Box(it.id(), x, y, w, h, it.s(), ax, ay));
            }
        }
        brTop = next.containsKey("BR") ? next.get("BR") : -1;
        trBottom = next.containsKey("TR") ? next.get("TR") : -1;
        List<Box> boxes = new ArrayList<>();
        ITEMS.clear();
        CLICKS.clear();
        for (Item it : items) {
            Box b = placed.get(it.id());
            curRight = b.ax() == 2; curBottom = b.ay() == 2;
            curX = b.x(); curY = b.y(); curS = it.s();
            ctx.getMatrices().push();
            ctx.getMatrices().translate(b.x(), b.y(), 0);
            ctx.getMatrices().scale(it.s(), it.s(), 1);
            try { draw(it.id(), it.m(), ctx, it.size()[0], it.size()[1]); } catch (Exception ignored) {}
            ctx.getMatrices().pop();
            boxes.add(b);
        }
        flushItems(ctx);
        R2D.alpha = prevA; R2D.radiusMul = prevR;
        return boxes;
    }

    // ================================================================== rozmiary

    private static float[] size(String id, Module m) {
        return switch (id) {
            case "Watermark" -> watermarkSize(m);
            case "ArrayList" -> arrayList(null, m, false);
            case "Keybinds" -> keybinds(null, m);
            case "TargetHUD" -> targetSize(m);
            case "Coordinates" -> coords(null, m);
            case "FPS" -> withGraph(m, pillSize(fpsVal(), "FPS"));
            case "Ping" -> new float[]{pillSize(pingVal(), "ms")[0] + 8, 15};
            case "TPS" -> withGraph(m, pillSize(String.format(Locale.ROOT, "%.1f", tpsNow), "TPS"));
            case "Speedometer" -> withGraph(m, pillSize(speedVal(m), mode(m, "Jednostka", "bps")));
            case "CPS" -> pillSize(cps(LEFT) + " · " + cps(RIGHT), "CPS");
            case "Memory" -> memory(null, m);
            case "Clock" -> pillSize(clockVal(m), bo(m, "Data") ? LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM")) : null);
            case "ServerInfo" -> pillSize(serverVal(), bo(m, "Gracze online") ? onlineVal() : null);
            case "TotemCounter" -> totems(null, m);
            case "ArmorHUD" -> armor(null, m);
            case "Potions" -> potions(null, m);
            case "InventoryHUD" -> new float[]{9 * 17 + 8, 3 * 17 + 8};
            case "Compass" -> mode(m, "Styl", "Bar").equals("Circle") ? new float[]{44, bo(m, "Stopnie") ? 54 : 44} : new float[]{150, bo(m, "Stopnie") ? 26 : 18};
            case "Keystrokes" -> keystrokes(null, m);
            case "HealthBar" -> new float[]{182, mode(m, "Styl", "Bar").equals("Minimal") ? 10 : (bo(m, "Głód") ? 18 : 14)};
            case "Session" -> session(null, m);
            case "Spotify" -> spotify(null, m);
            default -> null;
        };
    }

    private static void draw(String id, Module m, DrawContext ctx, float w, float h) {
        switch (id) {
            case "Watermark" -> watermark(ctx, m, w, h);
            case "ArrayList" -> arrayList(ctx, m, true);
            case "Keybinds" -> keybinds(ctx, m);
            case "TargetHUD" -> target(ctx, m, w, h);
            case "Coordinates" -> coords(ctx, m);
            case "FPS" -> { pill(ctx, w, 15, "l_activity", fpsVal(), "FPS"); graph(ctx, m, w, fpsHist, 0); }
            case "Ping" -> pingPill(ctx, m, w);
            case "TPS" -> { pill(ctx, w, 15, "l_hourglass", String.format(Locale.ROOT, "%.1f", tpsNow), "TPS"); graph(ctx, m, w, tpsHist, 20); }
            case "Speedometer" -> { pill(ctx, w, 15, "l_circle_gauge", speedVal(m), mode(m, "Jednostka", "bps")); graph(ctx, m, w, speedHist, 0); }
            case "CPS" -> pill(ctx, w, h, "l_mouse_pointer_click", cps(LEFT) + " · " + cps(RIGHT), "CPS");
            case "Memory" -> memory(ctx, m);
            case "Clock" -> pill(ctx, w, h, "l_clock", clockVal(m), bo(m, "Data") ? LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM")) : null);
            case "ServerInfo" -> pill(ctx, w, h, "l_server", serverVal(), bo(m, "Gracze online") ? onlineVal() : null);
            case "TotemCounter" -> totems(ctx, m);
            case "ArmorHUD" -> armor(ctx, m);
            case "Potions" -> potions(ctx, m);
            case "InventoryHUD" -> inventory(ctx, m, w, h);
            case "Compass" -> compass(ctx, m, w, h);
            case "Keystrokes" -> keystrokes(ctx, m);
            case "HealthBar" -> healthBar(ctx, m, w, h);
            case "Session" -> session(ctx, m);
            case "Spotify" -> spotify(ctx, m);
            default -> {}
        }
    }

    // ================================================================== proste "pigułki"

    static float[] pillSize(String value, String label) {
        float w = 6 + (icons ? 11 : 0) + Fonts.width(value, Font.BOLD, 0.6f) + (label != null ? 3 + Fonts.width(label, Font.SEMIBOLD, 0.54f) : 0) + 6;
        return new float[]{w, 15};
    }

    static void pill(DrawContext ctx, float w, float h, String icon, String value, String label) {
        panel(ctx, 0, 0, w, h);
        float x = 6;
        if (icons) { R2D.icon(ctx, icon, x, h / 2f - 4, 8, accent); x += 11; }
        Fonts.drawV(ctx, value, x, h / 2f + 0.5f, TEXT, Font.BOLD, 0.6f);
        x += Fonts.width(value, Font.BOLD, 0.6f) + 3;
        if (label != null) Fonts.drawV(ctx, label, x, h / 2f + 0.5f, DIM, Font.SEMIBOLD, 0.54f);
    }

    private static float[] withGraph(Module m, float[] s) {
        if (bo(m, "Min/max")) s[0] += Fonts.width(minMax(), Font.SEMIBOLD, 0.48f) + 6;
        if (bo(m, "Wykres")) { s[0] = Math.max(s[0], 70); s[1] += 17; }
        return s;
    }

    private static String minMax() {
        float mn = Float.MAX_VALUE, mx = 0;
        for (float f : fpsHist) if (f > 0) { mn = Math.min(mn, f); mx = Math.max(mx, f); }
        if (mx == 0) mn = 0;
        return "↓" + Math.round(mn) + " ↑" + Math.round(mx);
    }

    private static void graph(DrawContext ctx, Module m, float w, float[] hist, float fixedMax) {
        if (m.name.equals("FPS") && bo(m, "Min/max")) {
            String mm = minMax();
            Fonts.drawV(ctx, mm, w - 6 - Fonts.width(mm, Font.SEMIBOLD, 0.48f), 8, FAINT, Font.SEMIBOLD, 0.48f);
        }
        if (!bo(m, "Wykres")) return;
        float gy = 17, gh = 14, gx = 0;
        panel(ctx, gx, gy, w, gh);
        float mx = fixedMax;
        if (mx <= 0) for (float f : hist) mx = Math.max(mx, f);
        if (mx <= 0) mx = 1;
        int n = hist.length;
        float[] seg = new float[n * 4]; int[] cols = new int[n]; int k = 0;
        float step = (w - 8) / (n - 1);
        for (int i = 0; i < n - 1; i++) {
            float a = hist[(histPos + 1 + i) % n], b = hist[(histPos + 2 + i) % n];
            seg[k * 4] = 4 + i * step; seg[k * 4 + 1] = gy + gh - 3 - (gh - 6) * Math.min(1, a / mx);
            seg[k * 4 + 2] = 4 + (i + 1) * step; seg[k * 4 + 3] = gy + gh - 3 - (gh - 6) * Math.min(1, b / mx);
            cols[k++] = R2D.withAlpha(accent, 0.25f + 0.65f * i / n);
        }
        R2D.lines(ctx, seg, cols, k, 0.8f);
    }

    private static String fpsVal() { return String.valueOf(mc.getCurrentFps()); }

    private static int pingMs() {
        if (mc.getNetworkHandler() != null && mc.player != null) {
            PlayerListEntry pe = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (pe != null) return pe.getLatency();
        }
        return 0;
    }
    private static String pingVal() { int p = pingMs(); return p > 0 ? String.valueOf(p) : (mc.isInSingleplayer() ? "0" : "—"); }

    private static void pingPill(DrawContext ctx, Module m, float w) {
        float h = 15;
        panel(ctx, 0, 0, w, h);
        int p = pingMs();
        int lit = p <= 0 ? 4 : p < 70 ? 4 : p < 130 ? 3 : p < 220 ? 2 : 1;
        int col = accent;
        if (bo(m, "Kolor wg wartości")) col = lit >= 4 ? 0xFF46D17A : lit == 3 ? 0xFFA3E635 : lit == 2 ? 0xFFF2C94C : 0xFFFF5C7A;
        float x = 6;
        if (bo(m, "Paski") || icons) {
            for (int i = 0; i < 4; i++) {
                float bh = 3 + i * 2;
                R2D.roundRect(ctx, x + i * 2.6f, h / 2f + 4.5f - bh, 1.7f, bh, 0.6f, i < lit ? col : 0x33FFFFFF);
            }
            x += 14;
        }
        String v = pingVal();
        Fonts.drawV(ctx, v, x, h / 2f + 0.5f, TEXT, Font.BOLD, 0.6f);
        Fonts.drawV(ctx, "ms", x + Fonts.width(v, Font.BOLD, 0.6f) + 3, h / 2f + 0.5f, DIM, Font.SEMIBOLD, 0.54f);
    }

    private static String speedVal(Module m) {
        float v = mode(m, "Jednostka", "bps").equals("km/h") ? speed * 3.6f : speed;
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static String clockVal(Module m) {
        boolean h12 = mode(m, "Format", "24h").equals("12h"), sec = bo(m, "Sekundy");
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern((h12 ? "h:mm" : "HH:mm") + (sec ? ":ss" : "") + (h12 ? " a" : ""), Locale.ENGLISH));
    }

    private static String serverVal() {
        if (mc.isInSingleplayer()) return t("Jednoosobowy", "Singleplayer");
        var e = mc.getCurrentServerEntry();
        return e != null ? e.address : t("Serwer", "Server");
    }
    private static String onlineVal() {
        int n = mc.getNetworkHandler() != null ? mc.getNetworkHandler().getPlayerList().size() : 1;
        return n + " online";
    }

    // ================================================================== Watermark

    private record Info(String icon, String value, String label) {}

    private static List<Info> infos(Module m) {
        List<Info> l = new ArrayList<>();
        if (has(m, "Info", "FPS")) l.add(new Info("l_monitor", fpsVal(), "FPS"));
        if (has(m, "Info", "Ping")) l.add(new Info("l_wifi", pingVal(), "ms"));
        if (has(m, "Info", "Serwer")) l.add(new Info("l_cloud", serverShort(), null));
        if (has(m, "Info", "TPS")) l.add(new Info("l_gauge", String.format(Locale.ROOT, "%.0f", tpsNow), "TPS"));
        if (has(m, "Info", "Nick")) l.add(new Info("l_user", mc.getSession().getUsername(), null));
        if (has(m, "Info", "Czas")) l.add(new Info("l_clock", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), null));
        return l;
    }

    private static String serverShort() {
        if (mc.isInSingleplayer()) return t("jednoosobowy", "singleplayer");
        var e = mc.getCurrentServerEntry();
        return e != null ? e.address.toLowerCase(Locale.ROOT) : "multiplayer";
    }

    private static float nameW(String name, float sc) {
        return name.equals("WithClient") ? Fonts.width("With", Font.BOLD, sc) + Fonts.width("Client", Font.SEMIBOLD, sc) : Fonts.width(name, Font.BOLD, sc);
    }
    private static void drawName(DrawContext ctx, String name, float x, float cy, float sc) {
        if (name.equals("WithClient")) {
            Fonts.drawV(ctx, "With", x, cy, TEXT, Font.BOLD, sc);
            Fonts.drawV(ctx, "Client", x + Fonts.width("With", Font.BOLD, sc), cy, DIM, Font.SEMIBOLD, sc);
        } else Fonts.drawV(ctx, name, x, cy, TEXT, Font.BOLD, sc);
    }

    private static float infoW(Info i) {
        return (icons ? 11.5f : 0) + Fonts.width(i.value(), Font.BOLD, 0.6f) + (i.label() != null ? 3 + Fonts.width(i.label(), Font.SEMIBOLD, 0.54f) : 0);
    }

    private static String infoLine(List<Info> l) {
        StringBuilder sb = new StringBuilder();
        for (Info i : l) { if (sb.length() > 0) sb.append("  ·  "); sb.append(i.value()); if (i.label() != null) sb.append(' ').append(i.label()); }
        return sb.toString();
    }

    private static float[] watermarkSize(Module m) {
        String style = mode(m, "Styl", "Stats"), name = text(m, "Tekst", "WithClient");
        List<Info> l = infos(m);
        switch (style) {
            case "Logo" -> { return new float[]{Math.max(nameW(name, 0.8f), Fonts.width(infoLine(l), Font.SEMIBOLD, 0.52f)) + 40, 30}; }
            case "Text" -> { return new float[]{nameW(name, 0.9f) + (l.isEmpty() ? 0 : 6 + Fonts.width(infoLine(l), Font.SEMIBOLD, 0.56f)) + 2, 12}; }
            case "Pill" -> {
                float w = 6 + 13 + nameW(name, 0.66f);
                if (!l.isEmpty()) w += 8 + Fonts.width(infoLine(l), Font.SEMIBOLD, 0.54f);
                return new float[]{w + 8, 16};
            }
            case "Bar" -> {
                float w = 5 + 14 + nameW(name, 0.68f) + 5;
                for (Info i : l) w += 11 + infoW(i);
                return new float[]{w + 5, 18};
            }
            default -> {
                float w = 7 + (bo(m, "Logo") ? 16 : 0);
                for (Info i : l) w += infoW(i) + 10;
                return new float[]{Math.max(30, w - 10 + 7), 18};
            }
        }
    }

    private static void watermark(DrawContext ctx, Module m, float w, float h) {
        String style = mode(m, "Styl", "Stats"), name = text(m, "Tekst", "WithClient");
        boolean back = bo(m, "Tło");
        List<Info> l = infos(m);
        switch (style) {
            case "Logo" -> {
                if (back) panel(ctx, 0, 0, w, h);
                R2D.roundRectV(ctx, 5, 4, 22, 22, 6, 0xFF26262C, 0xFF0E0E11);
                R2D.roundOutline(ctx, 5, 4, 22, 22, 6, 0.5f, 0x30FFFFFF);
                R2D.icon(ctx, "with_logo", 8, 7, 16, 0xFFFFFFFF);
                drawName(ctx, name, 33, 11, 0.8f);
                Fonts.drawV(ctx, infoLine(l), 33, 21.5f, DIM, Font.SEMIBOLD, 0.52f);
            }
            case "Text" -> {
                float sc = 0.9f;
                ctx.getMatrices().push(); ctx.getMatrices().translate(0.6f, 0.6f, 0);
                float pa = R2D.alpha; R2D.alpha = pa * 0.6f;
                Fonts.drawV(ctx, name, 0, h / 2f, 0xFF000000, Font.BOLD, sc);
                R2D.alpha = pa; ctx.getMatrices().pop();
                drawName(ctx, name, 0, h / 2f, sc);
                if (!l.isEmpty()) Fonts.drawV(ctx, infoLine(l), nameW(name, sc) + 6, h / 2f + 0.5f, DIM, Font.SEMIBOLD, 0.56f);
            }
            case "Pill" -> {
                if (back) panel(ctx, 0, 0, w, h, h / 2f);
                R2D.icon(ctx, "with_logo", 6, h / 2f - 5.5f, 11, 0xFFFFFFFF);
                drawName(ctx, name, 19, h / 2f + 0.5f, 0.66f);
                if (!l.isEmpty()) {
                    float x = 19 + nameW(name, 0.66f) + 4;
                    R2D.circle(ctx, x, h / 2f, 0.9f, 0x66FFFFFF);
                    Fonts.drawV(ctx, infoLine(l), x + 4, h / 2f + 0.5f, DIM, Font.SEMIBOLD, 0.54f);
                }
            }
            case "Bar" -> {
                if (back) panel(ctx, 0, 0, w, h);
                R2D.icon(ctx, "with_logo", 5, h / 2f - 6, 12, 0xFFFFFFFF);
                drawName(ctx, name, 19, h / 2f + 0.5f, 0.68f);
                float x = 19 + nameW(name, 0.68f) + 5;
                for (Info i : l) {
                    R2D.rect(ctx, x, h / 2f - 4.5f, 0.5f, 9, 0x2EFFFFFF);
                    x += 6;
                    x = infoDraw(ctx, i, x, h);
                    x += 5;
                }
            }
            default -> {
                if (back) panel(ctx, 0, 0, w, h);
                float x = 7;
                if (bo(m, "Logo")) { R2D.icon(ctx, "with_logo", x, h / 2f - 5.5f, 11, 0xFFFFFFFF); x += 16; }
                for (Info i : l) x = infoDraw(ctx, i, x, h) + 10;
            }
        }
    }

    private static float infoDraw(DrawContext ctx, Info i, float x, float h) {
        if (icons) { R2D.icon(ctx, i.icon(), x, h / 2f - 4, 8, accent); x += 11.5f; }
        Fonts.drawV(ctx, i.value(), x, h / 2f + 0.5f, TEXT, Font.BOLD, 0.6f);
        x += Fonts.width(i.value(), Font.BOLD, 0.6f);
        if (i.label() != null) { Fonts.drawV(ctx, i.label(), x + 3, h / 2f + 0.5f, DIM, Font.SEMIBOLD, 0.54f); x += 3 + Fonts.width(i.label(), Font.SEMIBOLD, 0.54f); }
        return x;
    }

    // ================================================================== karta "embed"

    /** karta w stylu embedu: tło, pasek akcentu z lewej, nagłówek z ikoną, tytułem i plakietką; zwraca y treści */
    private static float embed(DrawContext ctx, float w, float h, String icon, String title, String badge, boolean header) {
        panel(ctx, 0, 0, w, h, Math.min(radius + 1, 8));
        R2D.roundRectV(ctx, 3.5f, 6, 2, h - 12, 1, R2D.withAlpha(accent, 0.95f), R2D.withAlpha(accent, 0.45f));
        if (!header) return 6;
        float x = 11;
        if (icons) { R2D.icon(ctx, icon, x, 5.5f, 8, accent); x += 11.5f; }
        Fonts.drawV(ctx, title, x, 10, TEXT, Font.BOLD, 0.64f);
        if (badge != null) {
            float bw = Math.max(11, Fonts.width(badge, Font.BOLD, 0.46f) + 8);
            R2D.roundRect(ctx, w - 8 - bw, 5.5f, bw, 9, 4.5f, R2D.withAlpha(accent, 0.16f));
            Fonts.drawV(ctx, badge, w - 8 - bw + (bw - Fonts.width(badge, Font.BOLD, 0.46f)) / 2f, 10.5f, TEXT, Font.BOLD, 0.46f);
        }
        R2D.gradientH(ctx, 11, 19, (w - 19) / 2f, 0.5f, 0x22FFFFFF, 0x10FFFFFF);
        R2D.gradientH(ctx, 11 + (w - 19) / 2f, 19, (w - 19) / 2f, 0.5f, 0x10FFFFFF, 0x00FFFFFF);
        return 22;
    }

    private static float embedHeadW(String title, String badge) {
        return 11 + (icons ? 11.5f : 0) + Fonts.width(title, Font.BOLD, 0.64f) + 12 + (badge != null ? Math.max(11, Fonts.width(badge, Font.BOLD, 0.46f) + 8) : 0) + 8;
    }

    // ================================================================== ArrayList

    private static String suffix(Module m) {
        for (Setting<?> s : m.settings) if (s instanceof Setting.Mode o) return o.get();
        return null;
    }

    /** lista włączonych modułów jako karta: nagłówek "Moduły" z licznikiem, grupy kategorii, ikona, nazwa i tryb */
    private static float[] arrayEmbed(DrawContext ctx, Module al, boolean draw) {
        boolean hideVis = bo(al, "Ukryj wizualne"), suf = bo(al, "Sufiks trybu"), group = bo(al, "Grupuj kategorie"), head = bo(al, "Nagłówek");
        String anim = mode(al, "Animacja", "Slide");
        int col = color(al, "Kolor", 0xFFFFFFFF);
        boolean alpha = mode(al, "Sortowanie", "Length").equals("Alphabet");
        record Row(Module m, String suf, float a) {}
        Map<Category, List<Row>> cats = new EnumMap<>(Category.class);
        int active = 0;
        for (Module m : Modules.all()) {
            boolean show = m.isEnabled() && m.category != Category.HUD && m.category != Category.CLIENT
                    && !(hideVis && (m.category == Category.VISUALS || m.category == Category.COSMETICS));
            Anim an = AL.computeIfAbsent(m.name, k -> new Anim(show ? 1 : 0, 11));
            float a = anim.equals("None") ? (show ? 1 : 0) : an.animate(show ? 1 : 0);
            if (anim.equals("None")) an.snap(show ? 1 : 0);
            if (show) active++;
            if (a < 0.01f) continue;
            cats.computeIfAbsent(group ? m.category : Category.COMBAT, k -> new ArrayList<>()).add(new Row(m, suf ? suffix(m) : null, a));
        }
        boolean empty = cats.isEmpty();
        if (empty && !preview) return null;
        if (empty) cats.put(Category.COMBAT, new ArrayList<>(List.of(new Row(Modules.get("KillAura"), "Single", 1), new Row(Modules.get("Sprint"), "Legit", 1))));
        float rowH = 12, catH = 11;
        String title = t("Moduły", "Modules"), badge = String.valueOf(active);
        float w = head ? embedHeadW(title, badge) : 70, h = head ? 22 : 6;
        for (Map.Entry<Category, List<Row>> e : cats.entrySet()) {
            List<Row> rows = e.getValue();
            if (alpha) rows.sort(Comparator.comparing(r -> r.m().name));
            else rows.sort((p, q) -> Float.compare(Fonts.width(q.m().name, Font.SEMIBOLD, 0.58f), Fonts.width(p.m().name, Font.SEMIBOLD, 0.58f)));
            float ca = 0;
            for (Row r : rows) {
                ca = Math.max(ca, r.a());
                h += rowH * (anim.equals("Fade") ? 1 : r.a());
                w = Math.max(w, 11 + (icons ? 11.5f : 0) + Fonts.width(r.m().name, Font.SEMIBOLD, 0.58f) + (r.suf() != null ? 10 + Fonts.width(r.suf(), Font.SEMIBOLD, 0.5f) : 0) + 9);
            }
            if (group) h += catH * (anim.equals("Fade") ? 1 : ca);
        }
        h += 4;
        if (ctx == null || !draw) return new float[]{w, h};
        float y = embed(ctx, w, h, "l_layers", title, badge, head);
        for (Map.Entry<Category, List<Row>> e : cats.entrySet()) {
            List<Row> rows = e.getValue();
            float ca = 0;
            for (Row r : rows) ca = Math.max(ca, r.a());
            if (group) {
                float hh = catH * (anim.equals("Fade") ? 1 : ca);
                float pa = R2D.alpha; R2D.alpha = pa * ca;
                dev.with.client.ui.Ui.spaced(ctx, e.getKey().title().toUpperCase(Locale.ROOT), 11, y + hh / 2f - 2.2f, FAINT, Font.BOLD, 0.42f, 1.1f);
                R2D.alpha = pa;
                y += hh;
            }
            for (Row r : rows) {
                float a = r.a(), rh = rowH * (anim.equals("Fade") ? 1 : a);
                float off = anim.equals("Slide") ? (1 - a) * 10 : 0;
                float pa = R2D.alpha; R2D.alpha = pa * Math.min(1, a * 1.4f);
                float x = 11 + off;
                if (icons) { R2D.icon(ctx, r.m().icon, x, y + rh / 2f - 3.5f, 7, R2D.withAlpha(accent, 0.8f)); x += 11.5f; }
                Fonts.drawV(ctx, r.m().name, x, y + rh / 2f + 0.5f, col, Font.SEMIBOLD, 0.58f);
                if (r.suf() != null) {
                    float sw2 = Fonts.width(r.suf(), Font.SEMIBOLD, 0.5f);
                    Fonts.drawV(ctx, r.suf(), w - 9 - sw2 + off, y + rh / 2f + 0.5f, FAINT, Font.SEMIBOLD, 0.5f);
                }
                R2D.alpha = pa;
                y += rh;
            }
        }
        return null;
    }

    private static float[] arrayList(DrawContext ctx, Module al, boolean draw) {
        String style = mode(al, "Styl", "Embed");
        if (style.equals("Embed")) return arrayEmbed(ctx, al, draw);
        boolean pills = style.equals("Pills"), text = style.equals("Text");
        boolean hideVis = bo(al, "Ukryj wizualne"), suf = bo(al, "Sufiks trybu"), back = bo(al, "Tło"), bar = bo(al, "Pasek boczny") && style.equals("Classic");
        boolean ic = pills && bo(al, "Ikony") && icons;
        String anim = mode(al, "Animacja", "Slide");
        int col = color(al, "Kolor", 0xFFFFFFFF);
        float sc = 0.6f, rh = pills ? 13 : 12, gap = pills ? 2 : 0;
        record Row(Module m, String suf, float w, float a) {}
        List<Row> rows = new ArrayList<>();
        for (Module m : Modules.all()) {
            boolean show = m.isEnabled() && m.category != Category.HUD && m.category != Category.CLIENT
                    && !(hideVis && (m.category == Category.VISUALS || m.category == Category.COSMETICS));
            Anim an = AL.computeIfAbsent(m.name, k -> new Anim(show ? 1 : 0, 11));
            float a = anim.equals("None") ? (show ? 1 : 0) : an.animate(show ? 1 : 0);
            if (anim.equals("None")) an.snap(show ? 1 : 0);
            if (a < 0.01f) continue;
            String sx = suf ? suffix(m) : null;
            float w = (pills ? 6 : text ? 1 : 5) + (ic ? 10 : 0) + Fonts.width(m.name, Font.BOLD, sc) + (sx != null ? 3 + Fonts.width(sx, Font.REGULAR, 0.54f) : 0) + (pills ? 6 : text ? 1 : 5) + (bar ? 2 : 0);
            rows.add(new Row(m, sx, w, a));
        }
        if (rows.isEmpty() && preview) rows.add(new Row(Modules.get("ArrayList"), null, 12 + Fonts.width("ArrayList", Font.BOLD, sc) + (ic ? 10 : 0), 1));
        if (mode(al, "Sortowanie", "Length").equals("Alphabet")) rows.sort(Comparator.comparing(r -> r.m().name));
        else rows.sort((p, q) -> Float.compare(q.w(), p.w()));
        float maxW = 0, totalH = 0;
        for (Row r : rows) { maxW = Math.max(maxW, r.w()); totalH += (rh + gap) * (anim.equals("Fade") ? 1 : r.a()); }
        totalH = Math.max(1, totalH - gap);
        if (ctx == null || !draw) return new float[]{Math.max(maxW, 20), totalH};
        float y = curBottom ? totalH + gap : 0;
        int bgc = R2D.withAlpha(bg.equals("Glass") ? 0xFF1A1A1F : 0xFF111114, (bg.equals("Clear") ? 0 : 0.86f) * opacity);
        for (Row r : rows) {
            float a = r.a(), hh = (rh + gap) * (anim.equals("Fade") ? 1 : a);
            if (curBottom) y -= hh;
            float x = curRight ? maxW - r.w() : 0;
            float off = anim.equals("Slide") ? (1 - a) * (r.w() + 8) * (curRight ? 1 : -1) : 0;
            float pa = R2D.alpha; R2D.alpha = pa * (anim.equals("Slide") ? Math.min(1, a * 1.5f) : a);
            float rx = x + off;
            if (pills) {
                if (back) {
                    R2D.roundRect(ctx, rx, y, r.w(), rh, Math.min(radius, rh / 2f), bgc);
                    if (outline) R2D.roundOutline(ctx, rx, y, r.w(), rh, Math.min(radius, rh / 2f), 0.5f, R2D.withAlpha(0xFFFFFFFF, 0.12f));
                }
            } else if (!text && back) R2D.roundRect(ctx, rx, y, r.w(), rh, 2.5f, bgc);
            if (bar) R2D.roundRect(ctx, curRight ? rx + r.w() - 1.6f : rx, y + 2, 1.6f, rh - 4, 0.8f, accent);
            float tx = rx + (pills ? 6 : text ? 1 : 5) + (bar && !curRight ? 2 : 0);
            if (ic) { R2D.icon(ctx, r.m().icon, tx, y + rh / 2f - 3.5f, 7, R2D.withAlpha(accent, 0.85f)); tx += 10; }
            if (text) {
                float pa2 = R2D.alpha; R2D.alpha = pa2 * 0.55f;
                Fonts.drawV(ctx, r.m().name, tx + 0.6f, y + rh / 2f + 1.1f, 0xFF000000, Font.BOLD, sc);
                R2D.alpha = pa2;
            }
            Fonts.drawV(ctx, r.m().name, tx, y + rh / 2f + 0.5f, col, Font.BOLD, sc);
            if (r.suf() != null) Fonts.drawV(ctx, r.suf(), tx + Fonts.width(r.m().name, Font.BOLD, sc) + 3, y + rh / 2f + 0.5f, FAINT, Font.REGULAR, 0.54f);
            R2D.alpha = pa;
            if (!curBottom) y += hh;
        }
        return null;
    }

    // ================================================================== Keybinds

    private static String keyName(int key) {
        String s = InputUtil.fromKeyCode(key, 0).getLocalizedText().getString();
        return s.length() > 7 ? s.substring(0, 7) : s.toUpperCase(Locale.ROOT);
    }

    private static float[] keybinds(DrawContext ctx, Module kb) {
        boolean onlyOn = bo(kb, "Tylko włączone"), head = bo(kb, "Nagłówek"), pills = mode(kb, "Styl", "Embed").equals("Pills");
        int max = (int) num(kb, "Max", 10);
        List<Module> l = new ArrayList<>();
        for (Module m : Modules.all()) if (m.key >= 0 && (!onlyOn || m.isEnabled())) l.add(m);
        l.sort(Comparator.comparing((Module m) -> !m.isEnabled()).thenComparing(m -> m.name));
        if (l.size() > max) l = l.subList(0, max);
        if (mode(kb, "Styl", "Embed").equals("Embed")) {
            float rowH = 13;
            String title = "Keybinds", badge = String.valueOf(l.size());
            float w = head ? embedHeadW(title, badge) : 80;
            for (Module m : l) w = Math.max(w, 11 + 9 + Fonts.width(m.name, Font.SEMIBOLD, 0.58f) + 12 + Math.max(13, Fonts.width(keyName(m.key), Font.BOLD, 0.48f) + 8) + 8);
            if (l.isEmpty()) w = Math.max(w, 22 + Fonts.width(t("Brak klawiszy", "No keybinds"), Font.SEMIBOLD, 0.56f));
            float h = (head ? 22 : 6) + Math.max(1, l.size()) * rowH + 4;
            if (ctx == null) return new float[]{w, h};
            float y = embed(ctx, w, h, "l_keyboard", title, badge, head);
            if (l.isEmpty()) Fonts.drawV(ctx, t("Brak klawiszy", "No keybinds"), 11, y + rowH / 2f, FAINT, Font.SEMIBOLD, 0.56f);
            for (Module m : l) {
                float on = KEYS.computeIfAbsent(m.name, k -> new Anim(m.isEnabled() ? 1 : 0, 12)).animate(m.isEnabled() ? 1 : 0);
                float cy = y + rowH / 2f;
                if (on > 0.01f) R2D.glow(ctx, 9, cy - 4, 8, 8, R2D.withAlpha(accent, 0.5f * on));
                R2D.circle(ctx, 13, cy, 1.8f, R2D.blend(0x40FFFFFF, accent, on));
                Fonts.drawV(ctx, m.name, 20, cy + 0.5f, R2D.blend(FAINT, TEXT, on), Font.SEMIBOLD, 0.58f);
                String k = keyName(m.key);
                float kw = Math.max(13, Fonts.width(k, Font.BOLD, 0.48f) + 8);
                R2D.roundRect(ctx, w - 8 - kw, y + 2, kw, rowH - 4, 3, R2D.blend(0x16FFFFFF, R2D.withAlpha(accent, 0.92f), on));
                Fonts.drawV(ctx, k, w - 8 - kw + (kw - Fonts.width(k, Font.BOLD, 0.48f)) / 2f, cy + 0.5f, R2D.blend(DIM, onAccent(), on), Font.BOLD, 0.48f);
                y += rowH;
            }
            return null;
        }
        if (pills) {
            float rowH = 13, gap = 3, headH = head ? 15 + gap : 0;
            float hw = 6 + (icons ? 11 : 0) + Fonts.width("Keybinds", Font.BOLD, 0.6f) + 7;
            float w = head ? hw : 40;
            for (Module m : l) w = Math.max(w, 12 + Fonts.width(m.name, Font.SEMIBOLD, 0.58f) + gap + Math.max(13, Fonts.width(keyName(m.key), Font.BOLD, 0.5f) + 8));
            if (l.isEmpty()) w = Math.max(w, 12 + Fonts.width(t("Brak klawiszy", "No keybinds"), Font.SEMIBOLD, 0.56f));
            float h = headH + Math.max(1, l.size()) * (rowH + gap) - gap;
            if (ctx == null) return new float[]{w, h};
            if (head) {
                float hx = curRight ? w - hw : 0;
                panel(ctx, hx, 0, hw, 15);
                float x = hx + 6;
                if (icons) { R2D.icon(ctx, "l_keyboard", x, 3.5f, 8, accent); x += 11; }
                Fonts.drawV(ctx, "Keybinds", x, 8, TEXT, Font.BOLD, 0.6f);
            }
            float y = headH;
            if (l.isEmpty()) {
                float ew = 12 + Fonts.width(t("Brak klawiszy", "No keybinds"), Font.SEMIBOLD, 0.56f);
                panel(ctx, curRight ? w - ew : 0, y, ew, rowH);
                Fonts.drawV(ctx, t("Brak klawiszy", "No keybinds"), (curRight ? w - ew : 0) + 6, y + rowH / 2f + 0.5f, FAINT, Font.SEMIBOLD, 0.56f);
            }
            for (Module m : l) {
                float on = KEYS.computeIfAbsent(m.name, k -> new Anim(m.isEnabled() ? 1 : 0, 12)).animate(m.isEnabled() ? 1 : 0);
                String k = keyName(m.key);
                float nw = 12 + Fonts.width(m.name, Font.SEMIBOLD, 0.58f), kw = Math.max(13, Fonts.width(k, Font.BOLD, 0.5f) + 8);
                float x = curRight ? w - nw - gap - kw : 0;
                panel(ctx, x, y, nw, rowH);
                Fonts.drawV(ctx, m.name, x + 6, y + rowH / 2f + 0.5f, R2D.blend(FAINT, TEXT, on), Font.SEMIBOLD, 0.58f);
                panel(ctx, x + nw + gap, y, kw, rowH);
                if (on > 0.01f) R2D.roundRect(ctx, x + nw + gap, y, kw, rowH, Math.min(radius, rowH / 2f), R2D.withAlpha(accent, 0.14f * on));
                Fonts.drawV(ctx, k, x + nw + gap + (kw - Fonts.width(k, Font.BOLD, 0.5f)) / 2f, y + rowH / 2f + 0.5f, R2D.blend(FAINT, TEXT, on), Font.BOLD, 0.5f);
                y += rowH + gap;
            }
            return null;
        }
        float rowH = 12, headH = head ? 17 : 4;
        float w = 104;
        for (Module m : l) w = Math.max(w, 16 + Fonts.width(m.name, Font.SEMIBOLD, 0.58f) + 10 + Math.max(12, Fonts.width(keyName(m.key), Font.BOLD, 0.48f) + 6) + 7);
        float h = headH + Math.max(1, l.size()) * rowH + 4;
        if (ctx == null) return new float[]{w, h};
        panel(ctx, 0, 0, w, h);
        if (head) {
            R2D.icon(ctx, "l_keyboard", 6, 4.5f, 8, accent);
            Fonts.drawV(ctx, "Keybinds", 17, 9, TEXT, Font.BOLD, 0.62f);
            String c = String.valueOf(l.size());
            Fonts.drawV(ctx, c, w - 7 - Fonts.width(c, Font.BOLD, 0.5f), 9, FAINT, Font.BOLD, 0.5f);
            R2D.rect(ctx, 6, 16, w - 12, 0.5f, 0x1CFFFFFF);
        }
        float y = headH + 2;
        if (l.isEmpty()) Fonts.drawV(ctx, t("Brak klawiszy", "No keybinds"), 7, y + rowH / 2f, FAINT, Font.SEMIBOLD, 0.56f);
        for (Module m : l) {
            float on = KEYS.computeIfAbsent(m.name, k -> new Anim(m.isEnabled() ? 1 : 0, 12)).animate(m.isEnabled() ? 1 : 0);
            R2D.circle(ctx, 8.5f, y + rowH / 2f, 1.7f, R2D.blend(0x40FFFFFF, accent, on));
            Fonts.drawV(ctx, m.name, 15, y + rowH / 2f + 0.5f, R2D.blend(DIM, TEXT, on), Font.SEMIBOLD, 0.58f);
            String k = keyName(m.key);
            float kw = Math.max(12, Fonts.width(k, Font.BOLD, 0.48f) + 6);
            R2D.roundRect(ctx, w - 6 - kw, y + 1.5f, kw, rowH - 3, 3, R2D.blend(0x18FFFFFF, R2D.withAlpha(accent, 0.9f), on));
            Fonts.drawV(ctx, k, w - 6 - kw + (kw - Fonts.width(k, Font.BOLD, 0.48f)) / 2f, y + rowH / 2f + 0.5f, R2D.blend(DIM, onAccent(), on), Font.BOLD, 0.48f);
            y += rowH;
        }
        return null;
    }

    // ================================================================== TargetHUD

    private record Particle(float x, float y, float vx, float vy, long t, float size) {}
    private static final List<Particle> PARTS = new ArrayList<>();
    private static float lastHp = -1;
    private static LivingEntity lastHpEnt;
    private static final Random RNG = new Random();

    private static LivingEntity findTarget(Module m) {
        float range = num(m, "Zasięg", 8);
        long now = System.currentTimeMillis();
        if (lastTarget != null && lastTarget.isAlive() && now - lastAttack < 4000 && mc.player != null && lastTarget.distanceTo(mc.player) < range) return lastTarget;
        if (mc.targetedEntity instanceof LivingEntity le && le.isAlive() && le != mc.player) return le;
        if (preview) return mc.player;
        return null;
    }

    private static float[] targetSize(Module m) {
        return switch (mode(m, "Styl", "Modern")) {
            case "Model" -> new float[]{156, 58};
            case "Compact" -> new float[]{116, 26};
            case "Minimal" -> new float[]{108, 18};
            default -> new float[]{156, 50};
        };
    }

    private static void target(DrawContext ctx, Module m, float w, float h) {
        LivingEntity t = findTarget(m);
        if (t != null && t != shownTarget) {
            if (shownTarget == null || targetA.raw() < 0.05f) { float f = hpFrac(t); hpA.snap(f); hpTrail.snap(f); }
            shownTarget = t;
        }
        float a = preview ? 1 : targetA.animate(t != null ? 1 : 0);
        if (a < 0.01f || shownTarget == null) { if (a < 0.01f) shownTarget = null; PARTS.clear(); return; }
        LivingEntity e = shownTarget;
        long now = System.currentTimeMillis();
        float f = hpFrac(e);
        boolean animHp = bo(m, "Animacja zdrowia");
        float hp = animHp ? hpA.animate(f) : f, trail = animHp ? hpTrail.animate(f) : f;
        if (trail < hp) { hpTrail.snap(hp); trail = hp; }
        String style = mode(m, "Styl", "Modern");
        boolean bigHead = style.equals("Modern") || style.equals("Model");
        float hs = style.equals("Model") ? 44 : 34, hx = 8, hy = 8;
        // cząsteczki przy obrażeniach
        float health = e.getHealth() + e.getAbsorptionAmount();
        if (e == lastHpEnt && health < lastHp - 0.01f && bo(m, "Cząsteczki") && bigHead) {
            for (int i = 0; i < 10; i++) {
                double ang = RNG.nextDouble() * Math.PI * 2;
                float sp = 14 + RNG.nextFloat() * 22;
                PARTS.add(new Particle(hx + hs / 2f, hy + hs / 2f, (float) Math.cos(ang) * sp, (float) Math.sin(ang) * sp - 10, now, 0.8f + RNG.nextFloat() * 1.2f));
            }
        }
        lastHp = health; lastHpEnt = e;
        float pa = R2D.alpha; R2D.alpha = pa * a;
        ctx.getMatrices().push();
        float sc = 0.92f + 0.08f * a;
        ctx.getMatrices().translate(w / 2f, h / 2f, 0); ctx.getMatrices().scale(sc, sc, 1); ctx.getMatrices().translate(-w / 2f, -h / 2f, 0);
        String name = e.getName().getString();
        String hpTxt = String.format(Locale.ROOT, "%.1f", health);
        float hurt = e.hurtTime > 0 ? e.hurtTime / 10f : 0;
        switch (style) {
            case "Compact" -> {
                panel(ctx, 0, 0, w, h);
                if (bo(m, "Głowa")) head(ctx, e, 5, 5, 16, hurt);
                float x = bo(m, "Głowa") ? 26 : 6;
                Fonts.drawV(ctx, Fonts.trim(name, Font.BOLD, 0.6f, w - x - 30), x, 9, TEXT, Font.BOLD, 0.6f);
                Fonts.drawV(ctx, hpTxt, w - 6 - Fonts.width(hpTxt, Font.BOLD, 0.56f), 9, TEXT, Font.BOLD, 0.56f);
                bar(ctx, x, 17, w - x - 6, 3, hp, trail, e, false);
            }
            case "Minimal" -> {
                Fonts.drawV(ctx, Fonts.trim(name, Font.BOLD, 0.62f, w - 30), 0, 6, TEXT, Font.BOLD, 0.62f);
                Fonts.drawV(ctx, hpTxt, w - Fonts.width(hpTxt, Font.BOLD, 0.56f), 6, DIM, Font.BOLD, 0.56f);
                bar(ctx, 0, 13, w, 2.5f, hp, trail, e, false);
            }
            default -> {
                panel(ctx, 0, 0, w, h);
                float shake = hurt > 0 ? (float) Math.sin(now * 0.09) * 1.6f * hurt : 0;
                if (style.equals("Model")) model(ctx, e, hx, hy - 2, hs, h - 12, hurt);
                else if (bo(m, "Głowa")) head(ctx, e, hx + shake, hy, hs, hurt);
                boolean anyHead = style.equals("Model") || bo(m, "Głowa");
                float x = anyHead ? hx + hs + 8 : 9, right = w - 8;
                // nazwa + HP
                float hw = Fonts.width(hpTxt, Font.BOLD, 0.66f);
                Fonts.drawV(ctx, Fonts.trim(name, Font.BOLD, 0.72f, right - x - hw - 16), x, 12, TEXT, Font.BOLD, 0.72f);
                R2D.icon(ctx, "l_heart", right - hw - 10, 8, 8, R2D.blend(accent, 0xFFFF5C7A, hurt));
                Fonts.drawV(ctx, hpTxt, right - hw, 12, TEXT, Font.BOLD, 0.66f);
                // odległość + przewaga
                float ly = 23.5f;
                String dist = mc.player != null && e != mc.player ? String.format(Locale.ROOT, "%.1f m", e.distanceTo(mc.player)) : t("Ty", "You");
                Fonts.drawV(ctx, dist, x, ly, DIM, Font.SEMIBOLD, 0.5f);
                if (mc.player != null && e != mc.player) {
                    float diff = mc.player.getHealth() + mc.player.getAbsorptionAmount() - health;
                    String wl = (diff >= 0 ? "+" : "−") + String.format(Locale.ROOT, "%.1f", Math.abs(diff)) + " HP";
                    float ww = Fonts.width(wl, Font.BOLD, 0.44f) + 8, wx = x + Fonts.width(dist, Font.SEMIBOLD, 0.5f) + 5;
                    R2D.roundRect(ctx, wx, ly - 3.8f, ww, 7.6f, 3.8f, diff >= 0 ? R2D.withAlpha(accent, 0.92f) : 0x40FF5C7A);
                    Fonts.drawV(ctx, wl, wx + 4, ly + 0.4f, diff >= 0 ? onAccent() : 0xFFFF9DB0, Font.BOLD, 0.44f);
                }
                if (bo(m, "Zbroja")) {
                    EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
                    List<ItemStack> worn = new ArrayList<>();
                    for (EquipmentSlot s : slots) if (!e.getEquippedStack(s).isEmpty()) worn.add(e.getEquippedStack(s));
                    float ix = right - worn.size() * 10 + 1;
                    for (ItemStack st : worn) { item(ctx, st, ix, ly - 4.5f, 9, false); ix += 10; }
                }
                float by = h - 14;
                bar(ctx, x, by, right - x, 5, hp, trail, e, true);
            }
        }
        // cząsteczki
        PARTS.removeIf(p -> now - p.t() > 700);
        for (Particle p : PARTS) {
            float age = (now - p.t()) / 700f, tt = age * 0.7f;
            float px = p.x() + p.vx() * tt, py = p.y() + p.vy() * tt + 30 * tt * tt;
            R2D.circle(ctx, px, py, p.size() * (1 - age * 0.6f), R2D.withAlpha(R2D.blend(accent, 0xFFFF5C7A, 0.35f), (1 - age)));
        }
        ctx.getMatrices().pop();
        R2D.alpha = pa;
    }

    private static float hpFrac(LivingEntity e) { return Math.max(0, Math.min(1, e.getHealth() / Math.max(1, e.getMaxHealth()))); }

    private static void bar(DrawContext ctx, float x, float y, float w, float h, float f, float trail, LivingEntity e, boolean ticks) {
        R2D.roundRect(ctx, x, y, w, h, h / 2f, 0x22FFFFFF);
        if (trail > f) R2D.roundRect(ctx, x, y, Math.max(h, w * trail), h, h / 2f, R2D.withAlpha(accent, 0.35f));
        R2D.roundRectH(ctx, x, y, Math.max(h, w * f), h, h / 2f, R2D.blend(accent, 0xFF000000, 0.35f), accent);
        float abs = e.getAbsorptionAmount();
        if (abs > 0) {
            float af = Math.min(1, abs / Math.max(1, e.getMaxHealth()));
            R2D.roundRect(ctx, x, y + h * 0.15f, Math.max(h, w * af), h * 0.7f, h * 0.35f, 0xCCF2C94C);
        }
        if (ticks) for (int i = 1; i < 10; i++) R2D.rect(ctx, x + w * i / 10f - 0.25f, y + 0.5f, 0.5f, h - 1, 0x55000000);
    }

    private static void head(DrawContext ctx, LivingEntity e, float x, float y, float s, float hurt) {
        float r = Math.min(radius, s * 0.22f) + 1;
        R2D.roundRect(ctx, x - 1, y - 1, s + 2, s + 2, r + 1, 0x26FFFFFF);
        if (e instanceof AbstractClientPlayerEntity p) {
            var tex = p.getSkinTextures().texture();
            R2D.roundTexture(ctx, tex, x, y, s, s, r, 8 / 64f, 8 / 64f, 16 / 64f, 16 / 64f, 0xFFFFFFFF);
            R2D.roundTexture(ctx, tex, x, y, s, s, r, 40 / 64f, 8 / 64f, 48 / 64f, 16 / 64f, 0xFFFFFFFF);
            ItemStack held = e.getMainHandStack();
            if (!held.isEmpty() && s >= 30) {
                R2D.roundRect(ctx, x + s - 11, y + s - 11, 13, 13, 4, 0xEE101013);
                item(ctx, held, x + s - 10, y + s - 10, 11, false);
            }
        } else {
            R2D.roundRect(ctx, x, y, s, s, r, 0x14FFFFFF);
            SpawnEggItem egg = SpawnEggItem.forEntity(e.getType());
            if (egg != null) item(ctx, new ItemStack(egg), x + s * 0.15f, y + s * 0.15f, s * 0.7f, false);
            else R2D.icon(ctx, "l_ghost", x + s * 0.25f, y + s * 0.25f, s * 0.5f, accent);
        }
        if (hurt > 0) R2D.roundRect(ctx, x, y, s, s, r, R2D.withAlpha(0xFFFF3B3B, 0.45f * hurt));
    }

    /** obracający się model 3D celu */
    private static void model(DrawContext ctx, LivingEntity e, float x, float y, float w, float h, float hurt) {
        R2D.roundRectV(ctx, x, y, w, h, Math.min(radius + 2, 8), 0x1CFFFFFF, 0x08FFFFFF);
        ctx.draw();
        R2D.scissor(ctx, x, y, w, h);
        float by = e.bodyYaw, py = e.prevBodyYaw, yaw = e.getYaw(), pitch = e.getPitch(), hy = e.headYaw, phy = e.prevHeadYaw;
        float spin = (System.currentTimeMillis() % 9000) / 9000f * 360f;
        e.bodyYaw = 180 + spin; e.prevBodyYaw = e.bodyYaw; e.setYaw(180 + spin); e.setPitch(0); e.headYaw = e.getYaw(); e.prevHeadYaw = e.getYaw();
        try {
            float size = h * 0.8f / Math.max(e.getHeight(), e.getWidth() * 1.4f);
            org.joml.Quaternionf rot = new org.joml.Quaternionf().rotateZ((float) Math.PI);
            org.joml.Quaternionf cam = new org.joml.Quaternionf().rotateX(0);
            rot.mul(cam);
            org.joml.Vector3f tr = new org.joml.Vector3f(0, e.getHeight() / 2f + 0.0625f, 0);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            boolean hud = mc.options.hudHidden;
            mc.options.hudHidden = true; // bez etykiety z nickiem nad modelem
            try { net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(ctx, x + w / 2f, y + h / 2f, size, tr, rot, cam, e); }
            finally { mc.options.hudHidden = hud; }
        } catch (Exception ignored) {
        } finally {
            e.bodyYaw = by; e.prevBodyYaw = py; e.setYaw(yaw); e.setPitch(pitch); e.headYaw = hy; e.prevHeadYaw = phy;
        }
        ctx.draw();
        R2D.endScissor(ctx);
        if (hurt > 0) R2D.roundRect(ctx, x, y, w, h, Math.min(radius + 2, 8), R2D.withAlpha(0xFFFF3B3B, 0.3f * hurt));
    }

    // ================================================================== koordynaty

    private static float[] coords(DrawContext ctx, Module m) {
        if (mc.player == null) return new float[]{60, 15};
        var p = mc.player;
        boolean nether = mc.world != null && mc.world.getRegistryKey() == World.NETHER, over = mc.world != null && mc.world.getRegistryKey() == World.OVERWORLD;
        String facing = switch (p.getHorizontalFacing()) { case NORTH -> "N"; case SOUTH -> "S"; case WEST -> "W"; default -> "E"; };
        if (mode(m, "Styl", "Pill").equals("Pill")) {
            List<String[]> pills = new ArrayList<>();
            pills.add(new String[]{"l_map_pin", String.format(Locale.ROOT, "%.0f, %.0f, %.0f", p.getX(), p.getY(), p.getZ()), bo(m, "Kierunek") ? facing : null});
            if (bo(m, "Nether") && (nether || over)) {
                double k = nether ? 8 : 0.125;
                pills.add(new String[]{"l_flame", String.format(Locale.ROOT, "%.0f, %.0f", p.getX() * k, p.getZ() * k), nether ? t("świat", "overworld") : "nether"});
            }
            if (bo(m, "Biom") && mc.world != null)
                pills.add(new String[]{"l_trees", mc.world.getBiome(p.getBlockPos()).getKey().map(k -> pretty(k.getValue().getPath())).orElse("?"), null});
            float ph = 15, gap = 3, w = 0;
            for (String[] s : pills) w = Math.max(w, pillLineW(s));
            float h = pills.size() * (ph + gap) - gap;
            if (ctx == null) return new float[]{w, h};
            float y = 0;
            for (String[] s : pills) {
                float pw = pillLineW(s), x = curRight ? w - pw : 0;
                panel(ctx, x, y, pw, ph);
                float tx = x + 6;
                if (icons) { R2D.icon(ctx, s[0], tx, y + ph / 2f - 4, 8, accent); tx += 11.5f; }
                Fonts.drawV(ctx, s[1], tx, y + ph / 2f + 0.5f, TEXT, Font.BOLD, 0.6f);
                if (s[2] != null) Fonts.drawV(ctx, s[2], tx + Fonts.width(s[1], Font.BOLD, 0.6f) + 4, y + ph / 2f + 0.5f, DIM, Font.SEMIBOLD, 0.54f);
                y += ph + gap;
            }
            return null;
        }
        List<String[]> lines = new ArrayList<>();
        lines.add(new String[]{"l_map_pin", null, String.format(Locale.ROOT, "%.0f", p.getX()), String.format(Locale.ROOT, "%.0f", p.getY()), String.format(Locale.ROOT, "%.0f", p.getZ())});
        if (bo(m, "Nether") && (nether || over)) {
            double k = nether ? 8 : 0.125;
            lines.add(new String[]{"l_flame", nether ? t("Świat", "Overworld") : "Nether", String.format(Locale.ROOT, "%.0f, %.0f", p.getX() * k, p.getZ() * k)});
        }
        if (bo(m, "Kierunek")) {
            String dir = switch (p.getHorizontalFacing()) {
                case NORTH -> t("Północ", "North") + "  −Z"; case SOUTH -> t("Południe", "South") + "  +Z";
                case WEST -> t("Zachód", "West") + "  −X"; default -> t("Wschód", "East") + "  +X"; };
            lines.add(new String[]{"l_navigation", t("Kierunek", "Facing"), dir});
        }
        if (bo(m, "Biom") && mc.world != null) {
            String b = mc.world.getBiome(p.getBlockPos()).getKey().map(k -> pretty(k.getValue().getPath())).orElse("?");
            lines.add(new String[]{"l_trees", t("Biom", "Biome"), b});
        }
        float lh = 11, w = 0;
        for (String[] l : lines) w = Math.max(w, lineW(l));
        w += 12;
        float h = lines.size() * lh + 5;
        if (ctx == null) return new float[]{w, h};
        panel(ctx, 0, 0, w, h);
        float y = 2.5f;
        for (String[] l : lines) {
            float x = 6, cy = y + lh / 2f + 0.5f;
            if (icons) { R2D.icon(ctx, l[0], x, cy - 4, 7, accent); x += 10; }
            if (l[1] == null) {
                String[] ax = {"X", "Y", "Z"};
                for (int i = 0; i < 3; i++) {
                    Fonts.drawV(ctx, ax[i], x, cy, FAINT, Font.BOLD, 0.52f); x += Fonts.width(ax[i], Font.BOLD, 0.52f) + 2.5f;
                    Fonts.drawV(ctx, l[2 + i], x, cy, TEXT, Font.BOLD, 0.6f); x += Fonts.width(l[2 + i], Font.BOLD, 0.6f) + 6;
                }
            } else {
                Fonts.drawV(ctx, l[1], x, cy, DIM, Font.SEMIBOLD, 0.54f); x += Fonts.width(l[1], Font.SEMIBOLD, 0.54f) + 4;
                Fonts.drawV(ctx, l[2], x, cy, TEXT, Font.BOLD, 0.58f);
            }
            y += lh;
        }
        return null;
    }

    private static float pillLineW(String[] s) {
        return 6 + (icons ? 11.5f : 0) + Fonts.width(s[1], Font.BOLD, 0.6f) + (s[2] != null ? 4 + Fonts.width(s[2], Font.SEMIBOLD, 0.54f) : 0) + 7;
    }

    private static float lineW(String[] l) {
        float w = icons ? 10 : 0;
        if (l[1] == null) {
            for (int i = 0; i < 3; i++) w += Fonts.width("X", Font.BOLD, 0.52f) + 2.5f + Fonts.width(l[2 + i], Font.BOLD, 0.6f) + 6;
            return w - 6;
        }
        return w + Fonts.width(l[1], Font.SEMIBOLD, 0.54f) + 4 + Fonts.width(l[2], Font.BOLD, 0.58f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String p : s.split("_")) { if (b.length() > 0) b.append(' '); b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)); }
        return b.toString();
    }

    // ================================================================== pamięć, totemy

    private static float[] memory(DrawContext ctx, Module m) {
        Runtime r = Runtime.getRuntime();
        long used = (r.totalMemory() - r.freeMemory()) >> 20, max = r.maxMemory() >> 20;
        String v = used + " / " + max;
        float[] s = pillSize(v, "MB");
        s[0] += 30;
        if (ctx == null) return s;
        pill(ctx, s[0], s[1], "l_memory_stick", v, "MB");
        float bx = s[0] - 30, f = (float) used / Math.max(1, max);
        R2D.roundRect(ctx, bx, 6, 24, 3, 1.5f, 0x24FFFFFF);
        R2D.roundRect(ctx, bx, 6, Math.max(3, 24 * f), 3, 1.5f, f > 0.85f ? 0xFFFF5C7A : accent);
        return null;
    }

    private static float[] totems(DrawContext ctx, Module m) {
        int n = mc.player != null ? mc.player.getInventory().count(Items.TOTEM_OF_UNDYING) : 0;
        if (n == 0 && bo(m, "Ukryj przy 0") && !preview) return null;
        String v = String.valueOf(n);
        float w = 6 + 12 + 3 + Fonts.width(v, Font.BOLD, 0.62f) + 3 + Fonts.width(t("totemy", "totems"), Font.SEMIBOLD, 0.52f) + 6;
        if (ctx == null) return new float[]{w, 15};
        if (n == 0 && bo(m, "Ukryj przy 0") && !preview) return null;
        panel(ctx, 0, 0, w, 15);
        item(ctx, new ItemStack(Items.TOTEM_OF_UNDYING), 5, 1.5f, 12, false);
        Fonts.drawV(ctx, v, 21, 8, n > 0 ? TEXT : 0xFFFF5C7A, Font.BOLD, 0.62f);
        Fonts.drawV(ctx, t("totemy", "totems"), 24 + Fonts.width(v, Font.BOLD, 0.62f), 8, DIM, Font.SEMIBOLD, 0.52f);
        return null;
    }

    // ================================================================== zbroja

    private static float[] armor(DrawContext ctx, Module m) {
        boolean vert = mode(m, "Układ", "Horizontal").equals("Vertical"), pct = bo(m, "Procenty"), hand = bo(m, "Przedmiot w ręce");
        List<ItemStack> st = new ArrayList<>();
        if (mc.player != null) {
            if (hand) st.add(mc.player.getMainHandStack());
            for (int i = 3; i >= 0; i--) st.add(mc.player.getInventory().getArmorStack(i));
        }
        int n = st.size();
        float cell = 18, extra = pct ? 7 : 3;
        float w = vert ? (pct ? 44 : 26) : n * cell + 6, h = vert ? n * cell + 6 : cell + extra + 6;
        if (ctx == null) return new float[]{Math.max(w, 20), Math.max(h, 20)};
        panel(ctx, 0, 0, w, h);
        for (int i = 0; i < n; i++) {
            ItemStack s = st.get(i);
            float x = vert ? 4 : 3 + i * cell, y = vert ? 3 + i * cell : 3;
            R2D.roundRect(ctx, x + 1, y + 1, 16, 16, 3, 0x0EFFFFFF);
            if (s.isEmpty()) continue;
            item(ctx, s, x + 1, y + 1, 16, s.getCount() > 1);
            if (s.isDamageable()) {
                float f = 1 - (float) s.getDamage() / Math.max(1, s.getMaxDamage());
                int c = f > 0.5f ? accent : f > 0.2f ? 0xFFF2C94C : 0xFFFF5C7A;
                if (vert) {
                    if (pct) Fonts.drawV(ctx, Math.round(f * 100) + "%", x + 20, y + 9.5f, c, Font.BOLD, 0.5f);
                } else {
                    R2D.roundRect(ctx, x + 2, y + 17.5f, 14, 1.5f, 0.75f, 0x24FFFFFF);
                    R2D.roundRect(ctx, x + 2, y + 17.5f, Math.max(1.5f, 14 * f), 1.5f, 0.75f, c);
                    if (pct) { String p = String.valueOf(Math.round(f * 100)); Fonts.drawCentered(ctx, p, x + 9, y + 19.8f, DIM, Font.BOLD, 0.42f); }
                }
            }
        }
        return null;
    }

    // ================================================================== efekty

    private static float[] potions(DrawContext ctx, Module m) {
        List<StatusEffectInstance> l = new ArrayList<>(mc.player != null ? mc.player.getStatusEffects() : List.of());
        boolean sample = l.isEmpty() && preview;
        if (sample) {
            l.add(new StatusEffectInstance(StatusEffects.SPEED, 1800, 1));
            l.add(new StatusEffectInstance(StatusEffects.STRENGTH, 900, 0));
            l.add(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 4200, 0));
        }
        if (mode(m, "Sortowanie", "Time").equals("Name")) l.sort(Comparator.comparing(Hud::effectName));
        else l.sort((a, b) -> Integer.compare(b.getDuration(), a.getDuration()));
        boolean ic = bo(m, "Ikony"), barOn = bo(m, "Pasek czasu");
        float rh = 17, w = 96;
        for (StatusEffectInstance e : l) w = Math.max(w, (ic ? 20 : 7) + Fonts.width(effectName(e), Font.BOLD, 0.58f) + 10 + Fonts.width(effectTime(e), Font.BOLD, 0.52f) + 7);
        float h = Math.max(1, l.size()) * rh + 4;
        if (ctx == null) return l.isEmpty() ? null : new float[]{w, h};
        if (l.isEmpty()) return null;
        panel(ctx, 0, 0, w, h);
        float y = 2;
        for (StatusEffectInstance e : l) {
            String key = e.getTranslationKey();
            int mx = Math.max(effectMax.getOrDefault(key, 0), e.getDuration());
            if (!sample) effectMax.put(key, mx);
            float x = 6;
            if (ic) {
                var sprite = mc.getStatusEffectSpriteManager().getSprite(e.getEffectType());
                ctx.draw();
                RenderSystem.setShaderColor(1, 1, 1, R2D.alpha);
                ctx.getMatrices().push(); ctx.getMatrices().translate(x, y + 2.5f, 0); ctx.getMatrices().scale(11 / 18f, 11 / 18f, 1);
                ctx.drawSprite(0, 0, 0, 18, 18, sprite);
                ctx.getMatrices().pop();
                RenderSystem.setShaderColor(1, 1, 1, 1);
                x += 14;
            }
            Fonts.drawV(ctx, effectName(e), x, y + 7.5f, TEXT, Font.BOLD, 0.58f);
            String tt = effectTime(e);
            Fonts.drawV(ctx, tt, w - 7 - Fonts.width(tt, Font.BOLD, 0.52f), y + 7.5f, DIM, Font.BOLD, 0.52f);
            if (barOn && !e.isInfinite()) {
                float f = e.getDuration() / (float) Math.max(1, mx);
                R2D.roundRect(ctx, x, y + 13, w - 7 - x, 1.2f, 0.6f, 0x1CFFFFFF);
                R2D.roundRect(ctx, x, y + 13, Math.max(1.2f, (w - 7 - x) * f), 1.2f, 0.6f, R2D.withAlpha(accent, 0.8f));
            }
            y += rh;
        }
        return null;
    }

    private static String effectName(StatusEffectInstance e) {
        String n = e.getEffectType().value().getName().getString();
        int amp = e.getAmplifier();
        String[] roman = {"", " II", " III", " IV", " V", " VI"};
        return amp > 0 ? n + (amp < roman.length ? roman[amp] : " " + (amp + 1)) : n;
    }

    private static String effectTime(StatusEffectInstance e) {
        if (e.isInfinite()) return "∞";
        int s = e.getDuration() / 20;
        return s / 60 + ":" + String.format("%02d", s % 60);
    }

    // ================================================================== ekwipunek

    private static void inventory(DrawContext ctx, Module m, float w, float h) {
        if (bo(m, "Tło")) panel(ctx, 0, 0, w, h);
        for (int i = 0; i < 27; i++) {
            float x = 4 + (i % 9) * 17, y = 4 + (i / 9) * 17;
            R2D.roundRect(ctx, x, y, 16, 16, 3, 0x0EFFFFFF);
            if (mc.player != null) {
                ItemStack s = mc.player.getInventory().getStack(9 + i);
                item(ctx, s, x, y, 16, true);
            }
        }
    }

    // ================================================================== kompas

    private static void compass(DrawContext ctx, Module m, float w, float h) {
        float yaw = mc.player != null ? mc.player.getYaw() : 0;
        float heading = ((yaw % 360) + 360 + 180) % 360; // 0 = północ
        boolean deg = bo(m, "Stopnie");
        if (mode(m, "Styl", "Bar").equals("Circle")) {
            panel(ctx, 0, 0, w, w, w / 2f);
            float cx = w / 2f, cy = w / 2f, r = w / 2f - 7;
            String[] l = {"N", "E", "S", "W"};
            for (int i = 0; i < 4; i++) {
                double a = Math.toRadians(i * 90 - heading - 90);
                float px = cx + (float) Math.cos(a) * r, py = cy + (float) Math.sin(a) * r;
                Fonts.drawV(ctx, l[i], px - Fonts.width(l[i], Font.BOLD, 0.5f) / 2f, py + 0.5f, i == 0 ? accent : DIM, Font.BOLD, 0.5f);
            }
            float[] seg = {cx, cy - 7, cx - 3, cy + 4, cx - 3, cy + 4, cx, cy + 2, cx, cy + 2, cx + 3, cy + 4, cx + 3, cy + 4, cx, cy - 7};
            R2D.lines(ctx, seg, new int[]{accent, accent, accent, accent}, 4, 1f);
            if (deg) {
                String dg = Math.round(heading) + "°";
                float dw = Fonts.width(dg, Font.BOLD, 0.5f) + 8;
                panel(ctx, cx - dw / 2f, w + 2, dw, 7.5f, 3.75f);
                Fonts.drawCentered(ctx, dg, cx, w + 3.2f, TEXT, Font.BOLD, 0.5f);
            }
            return;
        }
        float bh = 18;
        panel(ctx, 0, 0, w, bh);
        R2D.scissor(ctx, 2, 0, w - 4, bh);
        float span = 140, ppd = (w - 8) / span, cx = w / 2f;
        String[] labels = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int start = (int) Math.floor((heading - span / 2) / 15) * 15;
        for (int d = start; d <= heading + span / 2 + 15; d += 15) {
            float x = cx + (d - heading) * ppd;
            float fade = 1 - Math.min(1, Math.abs(d - heading) / (span / 2));
            int dd = ((d % 360) + 360) % 360;
            float pa = R2D.alpha; R2D.alpha = pa * fade;
            if (dd % 45 == 0) {
                String lb = labels[dd / 45];
                boolean card = dd % 90 == 0;
                Fonts.drawV(ctx, lb, x - Fonts.width(lb, Font.BOLD, card ? 0.58f : 0.46f) / 2f, 10.5f, dd == 0 ? accent : card ? TEXT : DIM, Font.BOLD, card ? 0.58f : 0.46f);
                R2D.rect(ctx, x - 0.3f, 2, 0.6f, 3, 0x88FFFFFF);
            } else R2D.rect(ctx, x - 0.25f, 2, 0.5f, 2, 0x44FFFFFF);
            R2D.alpha = pa;
        }
        R2D.endScissor(ctx);
        R2D.roundRect(ctx, cx - 0.75f, 0.5f, 1.5f, 5, 0.75f, accent);
        if (deg) {
            String dg = Math.round(heading) + "°";
            float dw = Fonts.width(dg, Font.BOLD, 0.5f) + 8;
            panel(ctx, cx - dw / 2f, bh + 1, dw, 7.5f, 3.75f);
            Fonts.drawCentered(ctx, dg, cx, bh + 2.2f, TEXT, Font.BOLD, 0.5f);
        }
    }

    // ================================================================== Keystrokes

    private static float[] keystrokes(DrawContext ctx, Module m) {
        float k = num(m, "Rozmiar", 18), g = 2;
        boolean mouse = bo(m, "Mysz"), space = bo(m, "Spacja");
        float w = 3 * k + 2 * g, h = 2 * k + g + (mouse ? k * 0.85f + g : 0) + (space ? k * 0.5f + g : 0);
        if (ctx == null) return new float[]{w, h};
        KeyBinding[] row2 = {mc.options.leftKey, mc.options.backKey, mc.options.rightKey};
        key(ctx, m, "W", mc.options.forwardKey, k + g, 0, k, k, null);
        for (int i = 0; i < 3; i++) key(ctx, m, "ASD".charAt(i) + "", row2[i], i * (k + g), k + g, k, k, null);
        float y = 2 * (k + g);
        if (mouse) {
            float mw = (w - g) / 2f, mh = k * 0.85f;
            boolean c = bo(m, "CPS");
            key(ctx, m, t("LPM", "LMB"), mc.options.attackKey, 0, y, mw, mh, c ? cps(LEFT) + " CPS" : null);
            key(ctx, m, t("PPM", "RMB"), mc.options.useKey, mw + g, y, mw, mh, c ? cps(RIGHT) + " CPS" : null);
            y += mh + g;
        }
        if (space) key(ctx, m, "", mc.options.jumpKey, 0, y, w, k * 0.5f, null);
        return null;
    }

    private static void key(DrawContext ctx, Module m, String fallback, KeyBinding kb, float x, float y, float w, float h, String sub) {
        boolean down = kb.isPressed();
        float a = KEYS.computeIfAbsent("ks" + kb.getTranslationKey(), z -> new Anim(0, 20)).animate(down ? 1 : 0);
        ctx.getMatrices().push();
        float s = 1 - 0.06f * a;
        ctx.getMatrices().translate(x + w / 2f, y + h / 2f, 0); ctx.getMatrices().scale(s, s, 1); ctx.getMatrices().translate(-(x + w / 2f), -(y + h / 2f), 0);
        panel(ctx, x, y, w, h, Math.min(radius, 4));
        if (a > 0.01f) R2D.roundRect(ctx, x, y, w, h, Math.min(radius, 4), R2D.withAlpha(accent, 0.92f * a));
        int fg = R2D.blend(TEXT, onAccent(), a);
        String label = fallback.isEmpty() ? "" : (fallback.length() == 1 ? kb.getBoundKeyLocalizedText().getString().toUpperCase(Locale.ROOT) : fallback);
        if (label.length() > 3) label = label.substring(0, 3);
        if (fallback.isEmpty()) R2D.roundRect(ctx, x + w * 0.35f, y + h / 2f - 0.6f, w * 0.3f, 1.2f, 0.6f, fg);
        else if (sub == null) Fonts.drawCentered(ctx, label, x + w / 2f, y + h / 2f - Fonts.height(0.62f) / 2f + 0.5f, fg, Font.BOLD, 0.62f);
        else {
            Fonts.drawCentered(ctx, label, x + w / 2f, y + h / 2f - 6, fg, Font.BOLD, 0.56f);
            Fonts.drawCentered(ctx, sub, x + w / 2f, y + h / 2f + 1.5f, R2D.blend(DIM, onAccent(), a), Font.SEMIBOLD, 0.42f);
        }
        ctx.getMatrices().pop();
    }

    // ================================================================== pasek zdrowia

    private static void healthBar(DrawContext ctx, Module m, float w, float h) {
        if (mc.player == null) return;
        var p = mc.player;
        float hp = p.getHealth(), max = p.getMaxHealth(), abs = bo(m, "Absorpcja") ? p.getAbsorptionAmount() : 0;
        float total = max + abs;
        float f = barA.animate(hp / Math.max(1, total));
        boolean minimal = mode(m, "Styl", "Bar").equals("Minimal"), food = bo(m, "Głód"), txt = bo(m, "Tekst");
        if (minimal) {
            if (txt) {
                String v = String.format(Locale.ROOT, "%.0f", hp + abs);
                Fonts.drawV(ctx, v, w - Fonts.width(v, Font.BOLD, 0.52f), 3, TEXT, Font.BOLD, 0.52f);
            }
            float by = 7;
            R2D.roundRect(ctx, 0, by, w, 3, 1.5f, 0x40000000);
            R2D.roundRect(ctx, 0, by, w, 3, 1.5f, 0x24FFFFFF);
            R2D.roundRectH(ctx, 0, by, Math.max(3, w * f), 3, 1.5f, R2D.blend(accent, 0xFF000000, 0.3f), accent);
            if (abs > 0) R2D.roundRect(ctx, w * f, by, w * abs / total, 3, 1.5f, 0xFFB8B8C0);
            return;
        }
        panel(ctx, 0, 0, w, h);
        float x = 6;
        R2D.icon(ctx, "l_heart", x, 7 - 3.5f, 7, hp <= 6 ? 0xFFFF5C7A : accent);
        x += 11;
        String v = String.format(Locale.ROOT, "%.0f", hp + abs) , mx = "/" + Math.round(max);
        float tw = txt ? Fonts.width(v, Font.BOLD, 0.56f) + Fonts.width(mx, Font.SEMIBOLD, 0.5f) + 8 : 0;
        float bw = w - x - 6 - tw;
        R2D.roundRect(ctx, x, 5, bw, 4, 2, 0x24FFFFFF);
        R2D.roundRectH(ctx, x, 5, Math.max(4, bw * f), 4, 2, R2D.blend(accent, 0xFF000000, 0.3f), accent);
        if (abs > 0) R2D.roundRect(ctx, x + bw * f, 5, bw * abs / total, 4, 2, 0xFFB8B8C0);
        if (txt) {
            float tx = w - 6 - tw + 8;
            Fonts.drawV(ctx, v, tx, 7.5f, TEXT, Font.BOLD, 0.56f);
            Fonts.drawV(ctx, mx, tx + Fonts.width(v, Font.BOLD, 0.56f), 7.5f, DIM, Font.SEMIBOLD, 0.5f);
        }
        if (food) {
            float ff = p.getHungerManager().getFoodLevel() / 20f;
            R2D.icon(ctx, "l_drumstick", 6, 11.5f, 6, DIM);
            R2D.roundRect(ctx, x, 13, bw, 2, 1, 0x1CFFFFFF);
            R2D.roundRect(ctx, x, 13, Math.max(2, bw * ff), 2, 1, 0xFFA1A1AA);
        }
    }

    // ================================================================== sesja

    private static float[] session(DrawContext ctx, Module m) {
        List<String[]> l = new ArrayList<>();
        if (bo(m, "Czas gry")) {
            long s = (System.currentTimeMillis() - sessionStart) / 1000;
            l.add(new String[]{"l_timer", t("Czas gry", "Playtime"), s >= 3600 ? String.format("%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60) : String.format("%d:%02d", s / 60, s % 60)});
        }
        if (bo(m, "Dystans")) l.add(new String[]{"l_footprints", t("Dystans", "Distance"), distance >= 1000 ? String.format(Locale.ROOT, "%.2f km", distance / 1000) : String.format(Locale.ROOT, "%.0f m", distance)});
        if (bo(m, "Śmierci")) l.add(new String[]{"l_skull", t("Śmierci", "Deaths"), String.valueOf(deaths)});
        float head = 15, lh = 11, w = 90;
        for (String[] s : l) w = Math.max(w, (icons ? 10 : 0) + 6 + Fonts.width(s[1], Font.SEMIBOLD, 0.54f) + 12 + Fonts.width(s[2], Font.BOLD, 0.58f) + 6);
        float h = head + l.size() * lh + 4;
        if (ctx == null) return new float[]{w, h};
        panel(ctx, 0, 0, w, h);
        Fonts.drawV(ctx, t("Sesja", "Session"), 6, 8, TEXT, Font.BOLD, 0.6f);
        String srv = serverVal();
        Fonts.drawV(ctx, Fonts.trim(srv, Font.SEMIBOLD, 0.48f, w - 50), w - 6 - Fonts.width(Fonts.trim(srv, Font.SEMIBOLD, 0.48f, w - 50), Font.SEMIBOLD, 0.48f), 8, FAINT, Font.SEMIBOLD, 0.48f);
        R2D.rect(ctx, 6, 14, w - 12, 0.5f, 0x1CFFFFFF);
        float y = head + 1;
        for (String[] s : l) {
            float x = 6, cy = y + lh / 2f + 0.5f;
            if (icons) { R2D.icon(ctx, s[0], x, cy - 4, 7, accent); x += 10; }
            Fonts.drawV(ctx, s[1], x, cy, DIM, Font.SEMIBOLD, 0.54f);
            Fonts.drawV(ctx, s[2], w - 6 - Fonts.width(s[2], Font.BOLD, 0.58f), cy, TEXT, Font.BOLD, 0.58f);
            y += lh;
        }
        return null;
    }

    // ================================================================== powiadomienia

    /** pierścień postępu (frac 0..1 od góry, zgodnie z zegarem) */
    static void ring(DrawContext ctx, float cx, float cy, float r, float th, float frac, int bgc, int fg) {
        int n = 28;
        float[] seg = new float[n * 4 * 2]; int[] cols = new int[n * 2]; int k = 0;
        for (int i = 0; i < n; i++) {
            double a1 = -Math.PI / 2 + Math.PI * 2 * i / n, a2 = -Math.PI / 2 + Math.PI * 2 * (i + 1) / n;
            seg[k * 4] = cx + (float) Math.cos(a1) * r; seg[k * 4 + 1] = cy + (float) Math.sin(a1) * r;
            seg[k * 4 + 2] = cx + (float) Math.cos(a2) * r; seg[k * 4 + 3] = cy + (float) Math.sin(a2) * r;
            cols[k++] = (i + 1) <= frac * n + 0.001f ? fg : bgc;
        }
        R2D.lines(ctx, seg, cols, k, th);
    }

    /** mały przełącznik (ikona w powiadomieniu) */
    private static void miniToggle(DrawContext ctx, float x, float y, float on) {
        float w = 13, h = 7;
        R2D.roundRect(ctx, x, y, w, h, h / 2f, R2D.blend(0x00FFFFFF, R2D.withAlpha(accent, 0.95f), on));
        if (on < 0.99f) R2D.roundOutline(ctx, x, y, w, h, h / 2f, 0.6f, R2D.withAlpha(0xFFFFFFFF, 0.55f * (1 - on)));
        R2D.circle(ctx, x + h / 2f + (w - h) * on, y + h / 2f, 2.2f, R2D.blend(0xFFD4D4D8, onAccent(), on));
    }

    private static void drawNotifications(DrawContext ctx) {
        Module n = mod("Notifications");
        long now = System.currentTimeMillis();
        long life = (long) (num(n, "Czas", 2) * 1000);
        NOTIFS.removeIf(x -> now - x.time() > life + 400);
        if (n == null || !n.isEnabled() || NOTIFS.isEmpty()) return;
        readStyle();
        boolean pill = mode(n, "Styl", "Pill").equals("Pill");
        String pos = mode(n, "Pozycja", "Bottom right");
        float sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        float s = gScale * base();
        boolean top = pos.equals("Top right"), center = pos.equals("Bottom");
        float y = top ? Math.max(6, trBottom > 0 ? trBottom + 2 : 6 + vanillaEffectsHeight()) : (center ? sh - 80 : (brTop > 0 ? brTop : sh - 6));
        R2D.alpha = 1; R2D.radiusMul = 1;
        for (int i = NOTIFS.size() - 1; i >= 0; i--) {
            Notif nt = NOTIFS.get(i);
            float age = now - nt.time();
            float in = Anim.easeOutCubic(age / 300f), out = Anim.easeInCubic((age - life) / 400f);
            float e = Math.max(0, Math.min(1, in) * (1 - Math.max(0, out)));
            if (e <= 0.01f) continue;
            float lifeF = Math.min(1, age / (float) life);
            float lw, lh;
            String text = (nt.on() ? t("Włączono ", "Enabled ") : t("Wyłączono ", "Disabled ")) + nt.name();
            String sub = nt.on() ? t("włączony", "enabled") : t("wyłączony", "disabled");
            if (pill) { lw = 6 + 13 + 6 + Fonts.width(text, Font.BOLD, 0.6f) + 7 + 9 + 6; lh = 17; }
            else { lw = Math.max(Fonts.width(nt.name(), Font.BOLD, 0.62f), Fonts.width(sub, Font.SEMIBOLD, 0.5f)) + 38; lh = 24; }
            float w = lw * s, h = lh * s;
            float x = center ? (sw - w) / 2f : sw - 6 - w + (1 - in) * (w + 10);
            if (!top) y -= (h + 3) * e;
            float yy = center ? y + (1 - in) * 10 : y;
            R2D.alpha = e;
            ctx.getMatrices().push();
            ctx.getMatrices().translate(x, yy, 0);
            ctx.getMatrices().scale(s, s, 1);
            if (pill) {
                panel(ctx, 0, 0, lw, lh, Math.min(radius + 1, lh / 2f));
                float onA = nt.on() ? Math.min(1, age / 250f) : 1 - Math.min(1, age / 250f);
                miniToggle(ctx, 6, lh / 2f - 3.5f, onA);
                Fonts.drawV(ctx, text, 25, lh / 2f + 0.5f, nt.on() ? TEXT : DIM, Font.BOLD, 0.6f);
                ring(ctx, lw - 6 - 4.5f, lh / 2f, 3.6f, 1.1f, 1 - lifeF, 0x26FFFFFF, R2D.withAlpha(accent, 0.9f));
            } else {
                panel(ctx, 0, 0, lw, lh);
                R2D.roundRectV(ctx, 5, 5, 14, 14, 4.5f, nt.on() ? accent : 0xFF26262B, nt.on() ? R2D.blend(accent, 0xFF000000, 0.12f) : 0xFF1A1A1F);
                R2D.icon(ctx, nt.icon(), 8, 8, 8, nt.on() ? onAccent() : DIM);
                Fonts.draw(ctx, nt.name(), 25, 4.5f, TEXT, Font.BOLD, 0.62f);
                Fonts.draw(ctx, sub, 25, 13.5f, nt.on() ? DIM : FAINT, Font.SEMIBOLD, 0.5f);
                R2D.roundRect(ctx, 6, lh - 2, (lw - 12) * (1 - lifeF), 1, 0.5f, R2D.withAlpha(accent, 0.5f));
            }
            ctx.getMatrices().pop();
            if (top) y += (h + 3) * e;
        }
        R2D.alpha = 1;
    }

    // ================================================================== Spotify (co teraz gra)

    private record Click(float x, float y, float w, float h, Runnable r) {}
    private static final List<Click> CLICKS = new ArrayList<>();
    private static float curX, curY, curS = 1;

    private static void clickable(float x, float y, float w, float h, Runnable r) { CLICKS.add(new Click(curX + x * curS, curY + y * curS, w * curS, h * curS, r)); }

    /** kliknięcie w HUD (gdy otwarty czat) – np. przyciski Spotify */
    public static boolean click(double x, double y) {
        for (Click c : List.copyOf(CLICKS)) if (x >= c.x() && x < c.x() + c.w() && y >= c.y() && y < c.y() + c.h()) { c.r().run(); return true; }
        return false;
    }

    private static boolean mediaShown(Module m) {
        Media.ensure();
        return Media.active || preview || !bo(m, "Ukryj gdy nic nie gra");
    }

    private static String mTitle() { return Media.active ? Media.title : preview ? "Midnight City" : t("Nic nie gra", "Nothing playing"); }
    private static String mArtist() { return Media.active ? Media.artist : preview ? "M83" : "Spotify"; }
    private static String time(double s) { int x = (int) Math.max(0, s); return x / 60 + ":" + String.format("%02d", x % 60); }

    private static float[] spotify(DrawContext ctx, Module m) {
        if (!mediaShown(m)) return null;
        boolean compact = mode(m, "Styl", "Card").equals("Compact");
        boolean art = bo(m, "Okładka"), btn = bo(m, "Przyciski"), prog = bo(m, "Pasek postępu");
        String lyric = bo(m, "Tekst piosenki") ? (Media.active ? Media.lyric() : preview ? "Waiting in a car, waiting for a ride in the dark" : "") : "";
        boolean lyr = !lyric.isEmpty();
        if (compact) {
            String txt = mTitle() + "  ·  " + mArtist();
            float w = 6 + 12 + Fonts.width(txt, Font.SEMIBOLD, 0.58f) + 6 + (btn ? 14 : 0);
            w = Math.min(w, 220);
            if (ctx == null) return new float[]{w, 16};
            panel(ctx, 0, 0, w, 16);
            eq(ctx, 6, 8, Media.playing || preview);
            Fonts.drawV(ctx, Fonts.trim(txt, Font.SEMIBOLD, 0.58f, w - 24 - (btn ? 14 : 0)), 18, 8.5f, TEXT, Font.SEMIBOLD, 0.58f);
            if (btn) { R2D.icon(ctx, Media.playing ? "l_pause" : "l_play", w - 14, 4.5f, 7, accent); clickable(w - 16, 0, 16, 16, Media::playPause); }
            return null;
        }
        float w = 176, cardH = 40, h = cardH + (lyr ? 18 : 0);
        if (ctx == null) return new float[]{w, h};
        panel(ctx, 0, 0, w, cardH);
        float tx = 7;
        if (art) {
            float s = 28;
            if (Media.hasArt && Media.active) R2D.roundTexture(ctx, Media.ART, 6, 6, s, s, Math.min(radius, 6), 0, 0, 1, 1, 0xFFFFFFFF);
            else {
                R2D.roundRectV(ctx, 6, 6, s, s, Math.min(radius, 6), 0xFF2A2A31, 0xFF141418);
                R2D.icon(ctx, "l_music", 6 + s / 2f - 6, 6 + s / 2f - 6, 12, accent);
            }
            tx = 6 + s + 7;
        }
        float ctlW = btn ? 50 : 0;
        Fonts.drawV(ctx, Fonts.trim(mTitle(), Font.BOLD, 0.62f, w - tx - ctlW - 6), tx, 12, TEXT, Font.BOLD, 0.62f);
        Fonts.drawV(ctx, Fonts.trim(mArtist(), Font.SEMIBOLD, 0.5f, w - tx - ctlW - 6), tx, 21.5f, DIM, Font.SEMIBOLD, 0.5f);
        if (btn) {
            float cx = w - 8 - 14, cy = 6;
            R2D.icon(ctx, "l_skip_forward", cx + 15 - 2, cy + 3.5f, 7, DIM);
            clickable(cx + 12, cy, 12, 14, Media::next);
            R2D.roundRect(ctx, cx - 3, cy, 14, 14, 4, R2D.withAlpha(accent, 0.92f));
            R2D.icon(ctx, Media.playing || (preview && !Media.active) ? "l_pause" : "l_play", cx + 0.5f, cy + 3.5f, 7, onAccent());
            clickable(cx - 3, cy, 14, 14, Media::playPause);
            R2D.icon(ctx, "l_skip_back", cx - 16, cy + 3.5f, 7, DIM);
            clickable(cx - 19, cy, 12, 14, Media::previous);
        }
        if (prog) {
            double dur = Media.active ? Media.duration : preview ? 241 : 0, p = Media.active ? Media.pos() : preview ? 8 : 0;
            float f = dur > 0 ? (float) Math.min(1, p / dur) : 0;
            String tt = time(p);
            float bx = tx, bw = w - tx - 8 - Fonts.width(tt, Font.SEMIBOLD, 0.46f) - 5, by = 31;
            R2D.roundRect(ctx, bx, by, bw, 2.5f, 1.25f, 0x26FFFFFF);
            R2D.roundRect(ctx, bx, by, Math.max(2.5f, bw * f), 2.5f, 1.25f, accent);
            Fonts.drawV(ctx, tt, w - 8 - Fonts.width(tt, Font.SEMIBOLD, 0.46f), by + 1.5f, FAINT, Font.SEMIBOLD, 0.46f);
        }
        if (lyr) {
            float ly = cardH + 3, lw = Math.min(w, 22 + Fonts.width(lyric, Font.SEMIBOLD, 0.56f) + 8), lx = (w - lw) / 2f;
            panel(ctx, lx, ly, lw, 15);
            eq(ctx, lx + 7, ly + 7.5f, Media.playing || preview);
            Fonts.drawV(ctx, Fonts.trim(lyric, Font.SEMIBOLD, 0.56f, lw - 26), lx + 19, ly + 8, TEXT, Font.SEMIBOLD, 0.56f);
        }
        return null;
    }

    /** animowany "equalizer" (3 słupki) */
    private static void eq(DrawContext ctx, float x, float cy, boolean on) {
        float t = (System.currentTimeMillis() % 100000L) / 1000f;
        for (int i = 0; i < 3; i++) {
            float hh = on ? 2.5f + 4.5f * (0.5f + 0.5f * (float) Math.sin(t * (6 + i * 1.7f) + i * 1.3f)) : 2.5f;
            R2D.roundRect(ctx, x + i * 2.6f, cy + 3.5f - hh, 1.7f, hh, 0.8f, accent);
        }
    }

    // ================================================================== własny pasek przedmiotów

    private static final Anim hotSel = new Anim(0, 18).springy();

    /** zastępuje pasek przedmiotów Minecrafta (moduł Hotbar) */
    public static boolean hotbarEnabled() { Module m = mod("Hotbar"); return m != null && m.isEnabled(); }

    public static void drawHotbar(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tc) {
        if (!(mc.getCameraEntity() instanceof net.minecraft.entity.player.PlayerEntity p)) return;
        readStyle();
        Module m = mod("Hotbar");
        float prevA = R2D.alpha, prevR = R2D.radiusMul;
        R2D.alpha = 1; R2D.radiusMul = 1;
        ITEMS.clear();
        float sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        float x0 = sw / 2f - 91, y0 = sh - 22;
        String style = mode(m, "Styl", "Clean");
        if (style.equals("Panel")) panel(ctx, x0, y0, 182, 22, Math.min(radius + 2, 9));
        int sel = p.getInventory().selectedSlot;
        float target = x0 + 1 + sel * 20;
        if (Math.abs(hotSel.raw() - target) > 200) hotSel.snap(target);
        float sx = bo(m, "Animacja") ? hotSel.animate(target) : target;
        for (int i = 0; i < 9; i++) {
            float x = x0 + 1 + i * 20;
            if (!style.equals("Panel")) R2D.roundRect(ctx, x + 1, y0 + 1, 18, 18, 5, R2D.withAlpha(0xFF000000, 0.32f * opacity));
        }
        R2D.glow(ctx, sx - 6, y0 - 6, 32, 32, R2D.withAlpha(accent, 0.18f));
        R2D.roundRect(ctx, sx, y0, 20, 20, 6, R2D.withAlpha(accent, 0.14f));
        R2D.roundOutline(ctx, sx, y0, 20, 20, 6, 0.9f, R2D.withAlpha(accent, 0.95f));
        float tick = tc.getTickDelta(true);
        for (int i = 0; i < 9; i++) {
            ItemStack st = p.getInventory().main.get(i);
            if (st.isEmpty()) continue;
            float x = x0 + 3 + i * 20, y = y0 + 2;
            float bob = st.getBobbingAnimationTime() - tick;
            ctx.getMatrices().push();
            if (bob > 0) {
                float g = 1 + bob / 5f;
                ctx.getMatrices().translate(x + 8, y + 12, 0);
                ctx.getMatrices().scale(1 / g, (g + 1) / 2f, 1);
                ctx.getMatrices().translate(-(x + 8), -(y + 12), 0);
            }
            item(ctx, st, x, y, 16, true);
            ctx.getMatrices().pop();
            if (bo(m, "Numery slotów")) Fonts.drawV(ctx, String.valueOf(i + 1), x - 1, y + 1, FAINT, Font.BOLD, 0.4f);
        }
        ItemStack off = p.getOffHandStack();
        if (!off.isEmpty()) {
            boolean left = p.getMainArm().getOpposite() == net.minecraft.util.Arm.LEFT;
            float ox = left ? x0 - 26 : x0 + 182 + 4;
            panel(ctx, ox, y0, 22, 22, Math.min(radius + 2, 8));
            item(ctx, off, ox + 3, y0 + 3, 16, true);
        }
        flushItems(ctx);
        R2D.alpha = prevA; R2D.radiusMul = prevR;
    }

    // ================================================================== zapis układu

    public static void resetLayout(String id) { LAYOUT.remove(id); save(); }
    public static void resetAll() { LAYOUT.clear(); save(); }

    public static void save() {
        JsonObject root = new JsonObject();
        for (var e : LAYOUT.entrySet()) {
            Layout L = e.getValue();
            if (!L.custom && L.scale == 1) continue;
            JsonObject o = new JsonObject();
            o.addProperty("custom", L.custom); o.addProperty("ax", L.ax); o.addProperty("ay", L.ay);
            o.addProperty("dx", L.dx); o.addProperty("dy", L.dy); o.addProperty("scale", L.scale);
            root.add(e.getKey(), o);
        }
        try { Files.createDirectories(FILE.getParent()); Files.writeString(FILE, GSON.toJson(root), StandardCharsets.UTF_8); } catch (Exception ignored) {}
    }

    private static void load() {
        try {
            if (!Files.exists(FILE)) return;
            JsonObject root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var e : root.entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                Layout L = layout(e.getKey());
                L.custom = o.get("custom").getAsBoolean(); L.ax = o.get("ax").getAsInt(); L.ay = o.get("ay").getAsInt();
                L.dx = o.get("dx").getAsFloat(); L.dy = o.get("dy").getAsFloat(); L.scale = o.has("scale") ? o.get("scale").getAsFloat() : 1;
            }
        } catch (Exception ignored) {}
    }
}
