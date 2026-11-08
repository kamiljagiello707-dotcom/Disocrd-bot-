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
    public final ModeSetting color = new ModeSetting("Color", "Text colour", "Wave", "Wave", "White", "Accent", "Rainbow");
    public final ModeSetting sidebar = new ModeSetting("Sidebar", "", "Outside", "Outside", "Inside", "None");
    public final BooleanSetting suffix = new BooleanSetting("Suffix", "Show module info", true);
    public final BooleanSetting lowercase = new BooleanSetting("Lowercase", false);
    public final NumberSetting spacing = new NumberSetting("Line Height", "", 11, 9, 16, 1, "px");
    public final NumberSetting bgOpacity = new NumberSetting("BG Opacity", "", 0.55, 0, 1, 0.05, "").visible(background::isOn);

    public ArrayListHud() {
        super("ArrayList", "List of enabled modules");
        addSettings(color, sidebar, suffix, lowercase, spacing, background, bgOpacity);
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

    @Override
    public void render(DrawContext c, float delta, boolean editor) {
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
            RenderUtil.text(c, s, lx + 3, cy + (lh - 8) / 2f, col, true);
            RenderUtil.globalAlpha = 1f;
            cy += h;
            index++;
        }
    }
}
