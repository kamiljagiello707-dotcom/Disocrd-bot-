package dev.wave.hud.elements;

import dev.wave.WaveClient;
import dev.wave.hud.HudElement;
import dev.wave.module.Module;
import dev.wave.setting.BooleanSetting;
import dev.wave.setting.ModeSetting;
import dev.wave.setting.NumberSetting;
import dev.wave.util.ColorUtil;
import dev.wave.util.Easing;
import dev.wave.util.RenderUtil;
import dev.wave.util.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ArrayListHud extends HudElement {
    public final ModeSetting style = new ModeSetting("Style", "Card: icon header 'Enabled:' with the list below", "Card", "Card", "Classic");
    public final BooleanSetting icons = new BooleanSetting("Icons", "Module icons next to names", true);
    public final ModeSetting color = new ModeSetting("Color", "Text colour", "Wave", "Wave", "White", "Accent", "Rainbow");
    public final ModeSetting sidebar = new ModeSetting("Sidebar", "", "Outside", "Outside", "Inside", "None").visible(() -> style.is("Classic"));
    public final BooleanSetting suffix = new BooleanSetting("Suffix", "Show module info", true);
    public final BooleanSetting lowercase = new BooleanSetting("Lowercase", false);
    public final NumberSetting spacing = new NumberSetting("Line Height", "", 11, 9, 16, 1, "px");
    public final NumberSetting bgOpacity = new NumberSetting("BG Opacity", "", 0.55, 0, 1, 0.05, "").visible(background::isOn);

    public ArrayListHud() {
        super("ArrayList", "List of enabled modules");
        addSettings(style, icons, color, sidebar, suffix, lowercase, spacing, background, bgOpacity);
        setEnabledSilently(true);
    }

    @Override
    protected void defaultPosition(int sw, int sh) { width = 80; x = sw - width - 2; y = 2; }

    private String label(Module m) {
        String s = m.getName();
        String sfx = suffix.isOn() ? m.getSuffix() : null;
        if (sfx != null && !sfx.isEmpty()) s += " §7" + sfx;
        return lowercase.isOn() ? s.toLowerCase() : s;
    }

    private int colorFor(int index) {
        switch (color.get()) {
            case "White": return 0xFFFFFFFF;
            case "Accent": return Theme.accent(index * 150L);
            case "Rainbow": return ColorUtil.rainbow(index * 150L, 0.6f, 1f, 1f);
            default: return ColorUtil.monoWave(index * 220L, 1f);
        }
    }

    private final dev.wave.util.Animation cardWidth = new dev.wave.util.Animation(14, 90);
    private final dev.wave.util.Animation cardHeight = new dev.wave.util.Animation(14, 20);

    private List<Module> collect() {
        List<Module> shown = new ArrayList<>();
        for (Module m : WaveClient.INSTANCE.modules.getModules()) {
            m.listAnimation.setTarget(m.isEnabled() && m.drawn.isOn() ? 1 : 0);
            m.listAnimation.update();
            if (m.listAnimation.get() > 0.01f) shown.add(m);
        }
        shown.sort(Comparator.comparingInt((Module m) -> RenderUtil.width(label(m))).reversed());
        return shown;
    }

    private void renderCard(DrawContext c, boolean editor) {
        List<Module> shown = collect();
        float rowH = spacing.getFloat() + 1, header = 20, pad = 6;
        float iconW = icons.isOn() ? 12 : 0;
        long count = shown.stream().filter(Module::isEnabled).count();
        String title = "\u00a7lEnabled:";
        float targetW = RenderUtil.width(title) + 22 + RenderUtil.width(String.valueOf(count)) + 18;
        float rows = 0;
        for (Module m : shown) {
            targetW = Math.max(targetW, RenderUtil.width(label(m)) + iconW + pad * 2 + 4);
            rows += rowH * Easing.outCubic(m.listAnimation.get());
        }
        cardWidth.setTarget(targetW);
        cardHeight.setTarget(header + (shown.isEmpty() ? 0 : rows + 5));
        float w = cardWidth.update();
        float h = shown.isEmpty() ? header : header + rows + 5;
        cardHeight.update();
        resize(w, h);

        int bg = ColorUtil.withAlpha(0x080808, (int) (Math.max(0.35, bgOpacity.get() + 0.25) * 255));
        if (background.isOn()) {
            RenderUtil.round(c, x, y, w, h, 6, bg);
            RenderUtil.roundOutline(c, x, y, w, h, 6, 0.5f, 0x22FFFFFF);
        }
        // header: icon tile + "Enabled:" + counter
        RenderUtil.round(c, x + 4, y + 4, 12, 12, 3.5f, Theme.accent());
        dev.wave.util.Icons.draw(c, "power", x + 6, y + 6, 8, Theme.onAccent());
        RenderUtil.text(c, title, x + 21, y + 6, Theme.TEXT, true);
        String cnt = String.valueOf(count);
        float cw = RenderUtil.width(cnt) + 6;
        RenderUtil.round(c, x + w - cw - 4, y + 5, cw, 10, 3, 0xFF1E1E1E);
        RenderUtil.text(c, cnt, x + w - cw - 1, y + 6, Theme.TEXT_DIM, false);
        if (shown.isEmpty()) return;
        RenderUtil.rect(c, x + 5, y + header - 0.5f, w - 10, 0.5f, 0x26FFFFFF);

        boolean right = rightAligned();
        float cy = y + header + 3;
        int index = 0;
        RenderUtil.scissor(c, x, y + header, w, h - header);
        for (Module m : shown) {
            float anim = Easing.outCubic(m.listAnimation.get());
            String s = label(m);
            int col = colorFor(index);
            float tw = RenderUtil.width(s) + iconW;
            float slide = (1 - anim) * 14 * (right ? 1 : -1);
            float lx = right ? x + w - pad - tw + slide : x + pad + slide;
            float prev = RenderUtil.globalAlpha;
            RenderUtil.globalAlpha = prev * anim;
            if (icons.isOn()) dev.wave.util.Icons.draw(c, m.getIcon(), lx, cy + (rowH - 8) / 2f - 0.5f, 8, col);
            RenderUtil.text(c, s, lx + iconW, cy + (rowH - 8) / 2f, col, true);
            if (sidebar.is("Inside")) RenderUtil.rect(c, right ? x + w - 2 : x + 1, cy + 1, 1, rowH * anim - 2, col);
            RenderUtil.globalAlpha = prev;
            cy += rowH * anim;
            index++;
        }
        RenderUtil.endScissor(c);
    }

    @Override
    public void render(DrawContext c, float delta, boolean editor) {
        if (style.is("Card")) { renderCard(c, editor); return; }
        List<Module> shown = new ArrayList<>();
        for (Module m : WaveClient.INSTANCE.modules.getModules()) {
            m.listAnimation.setTarget(m.isEnabled() && m.drawn.isOn() ? 1 : 0);
            m.listAnimation.update();
            if (m.listAnimation.get() > 0.01f) shown.add(m);
        }
        shown.sort(Comparator.comparingInt((Module m) -> RenderUtil.width(label(m))).reversed());

        float lh = spacing.getFloat();
        boolean right = rightAligned();
        float maxW = 0, totalH = 0;
        for (Module m : shown) {
            maxW = Math.max(maxW, RenderUtil.width(label(m)) + 6);
            totalH += lh * Easing.outCubic(m.listAnimation.get());
        }
        if (shown.isEmpty() && editor) {
            maxW = RenderUtil.width("ArrayList") + 6;
            totalH = lh;
            RenderUtil.text(c, "ArrayList", right ? x + width - maxW + 3 : x + 3, y + 2, Theme.TEXT_DIM, true);
        }
        resize(Math.max(maxW, 20), Math.max(totalH, lh));

        float cy = y;
        int index = 0;
        int bg = ColorUtil.withAlpha(0x000000, (int) (bgOpacity.get() * 255));
        for (Module m : shown) {
            float anim = Easing.outCubic(m.listAnimation.get());
            String s = label(m);
            float tw = RenderUtil.width(s) + 6;
            float slide = (1 - anim) * (tw + 6);
            float lx = right ? x + width - tw + slide : x - slide;
            int col = colorFor(index);
            float h = lh * anim;
            RenderUtil.globalAlpha = anim;
            if (background.isOn()) RenderUtil.rect(c, lx, cy, tw, h, bg);
            if (sidebar.is("Outside")) RenderUtil.rect(c, right ? lx + tw : lx - 1.5f, cy, 1.5f, h, col);
            else if (sidebar.is("Inside")) RenderUtil.rect(c, right ? lx + tw - 1.5f : lx, cy, 1.5f, h, col);
            if (icons.isOn()) dev.wave.util.Icons.draw(c, m.getIcon(), right ? lx - 10 : lx + tw + 2, cy + (lh - 8) / 2f, 8, col);
            RenderUtil.text(c, s, lx + 3, cy + (lh - 8) / 2f, col, true);
            RenderUtil.globalAlpha = 1f;
            cy += h;
            index++;
        }
    }
}
