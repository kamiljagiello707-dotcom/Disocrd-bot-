package dev.vesca.client.ui;

import dev.vesca.client.render.Fonts;
import dev.vesca.client.render.Fonts.Font;
import dev.vesca.client.render.R2D;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static dev.vesca.client.ui.Ui.*;

/**
 * Powitanie "Hello VescaClient" na środku ekranu – tylko przy PIERWSZYM wejściu na dany serwer.
 * Odwiedzone serwery są zapisywane w config/vescaclient/seen_servers.txt, więc po restarcie gry
 * powitanie na tym samym serwerze już się nie pokaże.
 */
public final class Welcome {
    private Welcome() {}

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("vescaclient").resolve("seen_servers.txt");
    private static final float DURATION = 4.8f;
    private static Set<String> seen;
    /** serwer, który czeka na powitanie (pokaże się, gdy zniknie ekran wczytywania) */
    private static String pending;
    private static long start = -1;

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> onJoin(client)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> { pending = null; start = -1; }));
        HudRenderCallback.EVENT.register((ctx, tick) -> render(ctx));
    }

    private static void onJoin(MinecraftClient mc) {
        if (mc.isInSingleplayer()) return;
        ServerInfo info = mc.getCurrentServerEntry();
        if (info == null || info.address == null || info.address.isBlank()) return;
        if (firstVisit(info.address)) show(info.name != null && !info.name.isBlank() ? info.name : info.address);
    }

    /** true, jeśli to pierwsza wizyta na tym adresie (i od razu zapisuje go jako odwiedzony) */
    public static boolean firstVisit(String address) {
        load();
        if (!seen.add(normalize(address))) return false;
        save();
        return true;
    }

    /** pokazuje powitanie (np. do podglądu) */
    public static void show(String serverName) { pending = serverName; start = -1; }

    static String normalize(String a) {
        a = a.trim().toLowerCase(Locale.ROOT);
        if (a.endsWith(".")) a = a.substring(0, a.length() - 1);
        if (a.endsWith(":25565")) a = a.substring(0, a.length() - 6);
        return a;
    }

    private static void load() {
        if (seen != null) return;
        seen = new HashSet<>();
        try {
            if (Files.exists(FILE)) for (String l : Files.readAllLines(FILE, StandardCharsets.UTF_8)) if (!l.isBlank()) seen.add(l.trim());
        } catch (IOException ignored) {}
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.write(FILE, seen.stream().sorted().toList(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }

    // ------------------------------------------------------------------ rysowanie

    private static void render(DrawContext ctx) {
        if (pending == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (start < 0) {
            // czekamy, aż gracz naprawdę zobaczy świat (bez ekranu wczytywania)
            if (mc.currentScreen != null || mc.player == null) return;
            start = System.currentTimeMillis();
        }
        float t = (System.currentTimeMillis() - start) / 1000f;
        if (t > DURATION) { pending = null; start = -1; return; }
        if (mc.options.hudHidden) return;

        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        float in = Anim.easeOutCubic(t / 0.6f);
        float out = Anim.easeInCubic(Anim.clamp01((t - (DURATION - 0.9f)) / 0.9f));
        float alpha = in * (1 - out);
        float cx = w / 2f, cy = h / 2f - 60 - out * 10;
        float prev = R2D.alpha;
        R2D.alpha = alpha;

        // tekst: "Hello" (szary) + "VescaClient" (biały) w jednej linii, litery wjeżdżają po kolei
        String hello = "Hello ", brand = "VescaClient", full = hello + brand;
        float ds = Math.min(0.72f, (w - 60) / Math.max(1, Fonts.width(full, Font.DISPLAY, 1f)));
        float cap = Fonts.displayCap(ds);
        float tw = Fonts.width(full, Font.DISPLAY, ds);


        // logo nad napisem – "wyskakuje"
        float lp = Anim.easeOutBack(Anim.clamp01(t / 0.5f));
        float ls = 26 * lp;
        if (ls > 1) logo(ctx, cx - ls / 2f, cy - 34 - ls / 2f + 13, ls, 0.6f);

        float x = cx - tw / 2f;
        for (int i = 0; i < full.length(); i++) {
            String ch = String.valueOf(full.charAt(i));
            float cw = Fonts.width(ch, Font.DISPLAY, ds);
            float lt = Anim.clamp01((t - 0.25f - i * 0.045f) / 0.35f);
            if (lt > 0 && !ch.isBlank()) {
                float e = Anim.easeOutCubic(lt);
                float pa = R2D.alpha;
                R2D.alpha = pa * e;
                int col = i < hello.length() ? 0xFFC4C4CC : 0xFFFFFFFF;
                // cień pod literą – czytelność na jasnym świecie
                display(ctx, ch, x + 1.2f, cy + (1 - e) * 6 + 1.2f, 0x99000000, ds);
                display(ctx, ch, x, cy + (1 - e) * 6, col, ds);
                R2D.alpha = pa;
            }
            x += cw;
        }
        // przelatujący błysk po napisie
        float sh = (t - 1.25f) / 0.8f;
        if (sh > 0 && sh < 1) {
            R2D.scissor(ctx, cx - tw / 2f - 4, cy - 4, tw + 8, cap + 8);
            float gx = cx - tw / 2f - 30 + (tw + 60) * Anim.easeOutCubic(sh);
            R2D.glow(ctx, gx - 22, cy - 12, 44, cap + 24, R2D.withAlpha(0xFFFFFFFF, 0.35f * (float) Math.sin(Math.PI * sh)));
            R2D.endScissor(ctx);
        }

        // linia pod napisem rozjeżdżająca się od środka
        float ly = cy + cap + 9;
        float lw = (tw + 24) * Anim.easeOutCubic(Anim.clamp01((t - 0.7f) / 0.7f));
        if (lw > 1) {
            R2D.gradientH(ctx, cx - lw / 2f, ly + 0.8f, lw / 2f, 0.8f, 0x00000000, 0x66000000);
            R2D.gradientH(ctx, cx, ly + 0.8f, lw / 2f, 0.8f, 0x66000000, 0x00000000);
            R2D.gradientH(ctx, cx - lw / 2f, ly, lw / 2f, 0.8f, 0x00FFFFFF, 0xCCFFFFFF);
            R2D.gradientH(ctx, cx, ly, lw / 2f, 0.8f, 0xCCFFFFFF, 0x00FFFFFF);
            R2D.glow(ctx, cx - 8, ly - 5, 16, 11, 0x66FFFFFF);
        }

        // podpis z nazwą serwera
        float sa = Anim.easeOutCubic(Anim.clamp01((t - 1.0f) / 0.5f));
        if (sa > 0) {
            float pa = R2D.alpha;
            R2D.alpha = pa * sa;
            String sub = t("Pierwszy raz na ", "First time on ") + pending;
            sub = Fonts.trim(sub, Font.SEMIBOLD, 0.72f, w - 40);
            Fonts.drawCentered(ctx, sub, cx + 0.8f, ly + 8.8f + (1 - sa) * 4, 0x99000000, Font.SEMIBOLD, 0.72f);
            Fonts.drawCentered(ctx, sub, cx, ly + 8 + (1 - sa) * 4, 0xFFDCDCE0, Font.SEMIBOLD, 0.72f);
            R2D.alpha = pa;
        }
        R2D.alpha = prev;
    }
}
