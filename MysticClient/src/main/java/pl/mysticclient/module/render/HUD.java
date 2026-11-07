package pl.mysticclient.module.render;

import net.minecraft.client.gui.DrawContext;
import pl.mysticclient.MysticClient;
import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;
import pl.mysticclient.module.ModuleManager;
import pl.mysticclient.setting.BoolSetting;
import pl.mysticclient.util.Theme;
import pl.mysticclient.util.UI;
import pl.mysticclient.util.UI.Icons;
import pl.mysticclient.util.UI.Weight;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class HUD extends Module {
    private final BoolSetting watermark = add(new BoolSetting("Watermark", true));
    private final BoolSetting arrayList = add(new BoolSetting("ArrayList", true));
    private final BoolSetting showFps = add(new BoolSetting("FPS", true)).visibleWhen(watermark::get);
    private final BoolSetting showName = add(new BoolSetting("Nick", true)).visibleWhen(watermark::get);
    private final BoolSetting showTime = add(new BoolSetting("Godzina", true)).visibleWhen(watermark::get);

    /** dolna krawędź ArrayList (dp) – powiadomienia pojawiają się pod nią */
    public static float arrayListBottom = 0;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    public HUD() {
        super("HUD", "Watermark i lista włączonych modułów.", Category.RENDER);
        hideFromArrayList();
    }

    @Override
    public void onRender2D(DrawContext ctx, float tickDelta) {
        if (mc.getDebugHud().shouldShowDebugHud()) { arrayListBottom = 0; return; }
        if (watermark.get()) drawWatermark();
        if (arrayList.get()) drawArrayList(); else arrayListBottom = 0;
    }

    private void drawWatermark() {
        float x = 12, y = 12, h = 34;
        float size = 13;
        String name = MysticClient.NAME;
        float nameW = UI.width(name, size, Weight.SEMIBOLD);

        java.util.List<String> parts = new java.util.ArrayList<>();
        if (showFps.get()) parts.add(mc.getCurrentFps() + " fps");
        if (showName.get() && mc.player != null) parts.add(mc.player.getGameProfile().getName());
        if (showTime.get()) parts.add(LocalTime.now().format(TIME));

        float w = 6 + 22 + 9 + nameW + 12;
        float[] pw = new float[parts.size()];
        for (int i = 0; i < parts.size(); i++) { pw[i] = UI.width(parts.get(i), 12, Weight.MEDIUM); w += 24 + pw[i]; }
        w -= 2;

        ModernHud.card(x, y, w, h, 11);
        UI.round(x + 6, y + 6, 22, 22, 7, Theme.accent());
        UI.icon(Icons.SPARKLES, x + 17, y + 17, 13, Theme.onAccent());
        float tx = x + 6 + 22 + 9;
        UI.textMid(name, tx, y + h / 2f, size, Weight.SEMIBOLD, Theme.text());
        tx += nameW + 10;
        for (int i = 0; i < parts.size(); i++) {
            UI.rect(tx, y + 11, 1, h - 22, UI.a(Theme.text(), 0.18f));
            tx += 12;
            UI.textMid(parts.get(i), tx, y + h / 2f, 12, Weight.MEDIUM, Theme.sub());
            tx += pw[i] + 12;
        }
    }

    private void drawArrayList() {
        float sw = UI.screenW();
        List<Module> mods = ModuleManager.getModules().stream()
                .filter(Module::showInArrayList)
                .peek(m -> m.arrayAnim = UI.approach(m.arrayAnim, m.isEnabled() ? 1f : 0f, 12f))
                .filter(m -> m.arrayAnim > 0.01f)
                .sorted(Comparator.comparingDouble(HUD::entryWidth).reversed())
                .toList();

        float y = 12, h = 24;
        for (Module m : mods) {
            float a = m.arrayAnim;
            float w = entryWidth(m) + 24;
            float x = sw - 12 - w + (1f - a) * (w + 16);
            float prevAlpha = UI.alpha;
            UI.alpha = a;
            UI.round(x, y, w, h - 2, 8, Theme.glass());
            UI.outline(x, y, w, h - 2, 8, 1, Theme.glassBorder());
            UI.round(x + w - 5, y + 6, 2, h - 14, 1, Theme.accent());
            UI.textMid(m.getName(), x + 10, y + (h - 2) / 2f, 12.5f, Weight.MEDIUM, Theme.text());
            String suffix = m.getSuffix();
            if (!suffix.isEmpty()) {
                UI.textMid(suffix, x + 10 + UI.width(m.getName(), 12.5f, Weight.MEDIUM) + 6, y + (h - 2) / 2f, 12.5f, Weight.REGULAR, Theme.dim());
            }
            UI.alpha = prevAlpha;
            y += h * a;
        }
        arrayListBottom = y;
    }

    private static float entryWidth(Module m) {
        String s = m.getSuffix();
        return UI.width(m.getName(), 12.5f, Weight.MEDIUM) + (s.isEmpty() ? 0 : UI.width(s, 12.5f) + 6);
    }
}
