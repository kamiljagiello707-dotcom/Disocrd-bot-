package tech.fuzir.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.Direction;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.module.ModuleManager;
import tech.fuzir.client.module.impl.*;
import tech.fuzir.client.util.Anim;
import tech.fuzir.client.util.Canvas;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Hud {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final Map<String, Anim> KEYS = new HashMap<>();
    private static final int PANEL = 0xD20C0C0C, BORDER = 0xFF222222, MUTED = 0xFF8A8A8A;

    private Hud() {
    }

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        if (mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud() || mc.player == null) return;
        float u = mc.getWindow().getFramebufferHeight() / 540f * ModuleManager.get(ClickGui.class).hudScale.f();
        Canvas cv = Canvas.begin(ctx, u);
        float y = 6;
        Watermark wm = ModuleManager.get(Watermark.class);
        if (wm.isEnabled()) y = watermark(cv, wm, y) + 4;
        Info info = ModuleManager.get(Info.class);
        if (info.isEnabled()) y = info(cv, info, y) + 4;
        Keystrokes ks = ModuleManager.get(Keystrokes.class);
        if (ks.isEnabled()) keystrokes(cv, ks, y + 2);
        moduleList(cv, ModuleManager.get(ModuleList.class));
        Coordinates co = ModuleManager.get(Coordinates.class);
        if (co.isEnabled()) coordinates(cv, co);
        Notifications.render(cv);
        cv.end();
    }

    private static float watermark(Canvas cv, Watermark wm, float y) {
        float x = 6, h = 18;
        boolean mini = wm.style.is("Minimalny");
        List<String> parts = new ArrayList<>();
        if (!mini) {
            if (wm.fps.on()) parts.add(mc.getCurrentFps() + " fps");
            if (wm.time.on()) parts.add(LocalTime.now().format(TIME));
        }
        float w = 26 + cv.textWidthBold("FUZIR", 1);
        for (String p : parts) w += 9 + cv.textWidth(p, 1);
        w += 6;
        cv.roundBorder(x, y, w, h, 6, PANEL, BORDER);
        cv.round(x + 4, y + 4, 10, 10, 3, 0xFFFFFFFF);
        cv.textBold("F", x + 6.5f, y + 5, 0xFF000000, 0.5f);
        float tx = x + 19;
        cv.textBold("FUZIR", tx, y + 5, 0xFFFFFFFF, 1);
        tx += cv.textWidthBold("FUZIR", 1);
        for (String p : parts) {
            cv.rect(tx + 4, y + 5, 1f / cv.u * Math.max(1, Math.round(cv.u / 2)), 8, 0xFF3A3A3A);
            cv.text(p, tx + 9, y + 5, MUTED, 1);
            tx += 9 + cv.textWidth(p, 1);
        }
        return y + h;
    }

    private static float info(Canvas cv, Info info, float y) {
        List<String[]> rows = new ArrayList<>();
        if (info.ping.on() && mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) rows.add(new String[]{"Ping", e.getLatency() + " ms"});
        }
        if (info.speed.on()) {
            double dx = mc.player.getX() - mc.player.prevX, dz = mc.player.getZ() - mc.player.prevZ;
            rows.add(new String[]{"Prędkość", String.format(Locale.ROOT, "%.1f b/s", Math.sqrt(dx * dx + dz * dz) * 20).replace('.', ',')});
        }
        if (info.server.on()) {
            ServerInfo s = mc.getCurrentServerEntry();
            rows.add(new String[]{"Serwer", s == null ? "Świat lokalny" : s.address});
        }
        if (rows.isEmpty()) return y - 4;
        float w = 0;
        for (String[] r : rows) w = Math.max(w, cv.textWidth(r[0], 1) + 10 + cv.textWidth(r[1], 1));
        w += 14;
        float h = rows.size() * 11 + 7;
        cv.roundBorder(6, y, w, h, 6, PANEL, BORDER);
        float ry = y + 5;
        for (String[] r : rows) {
            cv.text(r[0], 13, ry, MUTED, 1);
            cv.text(r[1], 6 + w - 7 - cv.textWidth(r[1], 1), ry, 0xFFFFFFFF, 1);
            ry += 11;
        }
        return y + h;
    }

    private static void key(Canvas cv, String id, String label, float x, float y, float w, float h, boolean down) {
        float a = KEYS.computeIfAbsent(id, k -> new Anim(0)).to(down ? 1 : 0, 22);
        cv.roundBorder(x, y, w, h, 5, Canvas.mix(PANEL, 0xFFFFFFFF, a), Canvas.mix(BORDER, 0xFFFFFFFF, a));
        if (!label.isEmpty()) {
            float tw = cv.textWidth(label, 1);
            cv.text(label, x + w / 2 - tw / 2 + 0.5f, y + h / 2 - 3.5f, Canvas.mix(0xFFFFFFFF, 0xFF000000, a), 1);
        } else {
            cv.round(x + w / 2 - 9, y + h / 2 - 0.75f, 18, 1.5f, 0.75f, Canvas.mix(0xFFFFFFFF, 0xFF000000, a));
        }
    }

    private static void keystrokes(Canvas cv, Keystrokes ks, float y) {
        float s = 22, g = 2, x = 6;
        KeyBinding[] k = {mc.options.forwardKey, mc.options.leftKey, mc.options.backKey, mc.options.rightKey};
        key(cv, "w", "W", x + s + g, y, s, s, k[0].isPressed());
        key(cv, "a", "A", x, y + s + g, s, s, k[1].isPressed());
        key(cv, "s", "S", x + s + g, y + s + g, s, s, k[2].isPressed());
        key(cv, "d", "D", x + (s + g) * 2, y + s + g, s, s, k[3].isPressed());
        float ry = y + (s + g) * 2, full = s * 3 + g * 2;
        if (ks.mouse.on()) {
            float half = (full - g) / 2;
            key(cv, "lmb", "LPM", x, ry, half, 16, mc.options.attackKey.isPressed());
            key(cv, "rmb", "PPM", x + half + g, ry, half, 16, mc.options.useKey.isPressed());
            ry += 16 + g;
        }
        if (ks.space.on()) key(cv, "space", "", x, ry, full, 11, mc.options.jumpKey.isPressed());
    }

    private static void moduleList(Canvas cv, ModuleList ml) {
        List<Module> mods = new ArrayList<>();
        for (Module m : ModuleManager.all()) {
            if (!m.showInList()) continue;
            float a = m.listAnim.to(m.isEnabled() && ml.isEnabled() ? 1 : 0, 13);
            if (a > 0.005f) mods.add(m);
        }
        mods.sort(Comparator.comparingDouble((Module m) -> -label(cv, m, ml)));
        float y = 6, right = cv.width() - 6, rowH = 13;
        for (Module m : mods) {
            float a = Anim.easeOutCubic(m.listAnim.get());
            float w = label(cv, m, ml) + 12;
            float x = right - w + (1 - a) * (w + 10);
            float prev = cv.alpha;
            cv.alpha = Math.min(1, a * 1.2f);
            if (ml.background.on()) cv.rect(x, y, w, rowH * a + 0.01f, 0xB00B0B0B);
            if (ml.bar.on()) cv.rect(right - 1.5f + (1 - a) * (w + 10), y, 1.5f, rowH * a + 0.01f, 0xFFFFFFFF);
            cv.text(m.name, x + 5, y + 3, 0xFFFFFFFF, 1);
            String sfx = m.suffix();
            if (sfx != null && ml.suffixes.on()) cv.text(sfx, x + 5 + cv.textWidth(m.name + " ", 1), y + 3, MUTED, 1);
            cv.alpha = prev;
            y += rowH * a;
        }
    }

    private static float label(Canvas cv, Module m, ModuleList ml) {
        String s = m.suffix();
        return cv.textWidth(s != null && ml.suffixes.on() ? m.name + " " + s : m.name, 1);
    }

    private static void coordinates(Canvas cv, Coordinates co) {
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"XYZ", fmt(x) + ", " + fmt(y) + ", " + fmt(z)});
        if (co.nether.on() && mc.world != null) {
            boolean inNether = mc.world.getRegistryKey() == net.minecraft.world.World.NETHER;
            double k = inNether ? 8 : 1 / 8.0;
            rows.add(new String[]{inNether ? "Świat" : "Nether", fmt(x * k) + ", " + fmt(z * k)});
        }
        if (co.direction.on()) {
            Direction d = mc.player.getHorizontalFacing();
            String name = switch (d) {
                case NORTH -> "Północ (-Z)";
                case SOUTH -> "Południe (+Z)";
                case EAST -> "Wschód (+X)";
                case WEST -> "Zachód (-X)";
                default -> d.asString();
            };
            rows.add(new String[]{"Kierunek", name});
        }
        float w = 0;
        for (String[] r : rows) w = Math.max(w, cv.textWidth(r[0], 1) + 8 + cv.textWidth(r[1], 1));
        w += 14;
        float h = rows.size() * 11 + 7;
        float bottom = cv.height() - 6 - (mc.currentScreen instanceof ChatScreen ? 18 : 0);
        float top = bottom - h;
        cv.roundBorder(6, top, w, h, 6, PANEL, BORDER);
        float ry = top + 5;
        for (String[] r : rows) {
            cv.text(r[0], 13, ry, MUTED, 1);
            cv.text(r[1], 13 + cv.textWidth(r[0], 1) + 8, ry, 0xFFFFFFFF, 1);
            ry += 11;
        }
    }

    private static String fmt(double v) {
        return String.valueOf((int) Math.floor(v));
    }
}
