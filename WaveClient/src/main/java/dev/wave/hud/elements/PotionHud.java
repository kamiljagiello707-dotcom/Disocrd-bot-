package dev.wave.hud.elements;

import dev.wave.hud.HudElement;
import dev.wave.setting.BooleanSetting;
import dev.wave.util.RenderUtil;
import dev.wave.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.ArrayList;
import java.util.List;

public class PotionHud extends HudElement {
    public final BooleanSetting colored = new BooleanSetting("Effect Colors", "Use the potion colour for the dot", true);

    public PotionHud() {
        super("Potions", "Active potion effects");
        addSettings(colored, background);
        setEnabledSilently(true);
    }

    @Override
    protected void defaultPosition(int sw, int sh) { x = sw - 100; y = sh / 2f - 60; }

    private static String roman(int n) {
        String[] r = {"", "", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};
        return n < r.length ? r[n] : " " + n;
    }

    private static String duration(StatusEffectInstance e) {
        if (e.isInfinite()) return "∞";
        int s = e.getDuration() / 20;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    @Override
    public void render(DrawContext c, float delta, boolean editor) {
        List<String[]> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        if (mc.player != null) {
            for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                lines.add(new String[]{e.getEffectType().value().getName().getString() + roman(e.getAmplifier() + 1), duration(e)});
                colors.add(0xFF000000 | e.getEffectType().value().getColor());
            }
        }
        if (lines.isEmpty()) {
            if (!editor) { resize(width, height); return; }
            lines.add(new String[]{"Speed II", "1:30"});
            colors.add(0xFF7CAFC6);
        }
        float w = 0;
        for (String[] l : lines) w = Math.max(w, RenderUtil.width(l[0] + "  " + l[1]));
        w += 18;
        float h = lines.size() * 11 + 5;
        resize(w, h);
        if (background.isOn()) RenderUtil.round(c, x, y, w, h, 3, Theme.HUD_BG);
        float ly = y + 3;
        for (int i = 0; i < lines.size(); i++) {
            int dot = colored.isOn() ? colors.get(i) : Theme.accent();
            RenderUtil.circle(c, x + 6, ly + 4.5f, 2.2f, dot);
            RenderUtil.text(c, lines.get(i)[0], x + 12, ly + 1, Theme.TEXT, true);
            String d = lines.get(i)[1];
            RenderUtil.text(c, d, x + w - 4 - RenderUtil.width(d), ly + 1, Theme.TEXT_DIM, true);
            ly += 11;
        }
    }
}
