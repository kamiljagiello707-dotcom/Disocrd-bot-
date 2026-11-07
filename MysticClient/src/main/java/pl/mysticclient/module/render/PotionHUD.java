package pl.mysticclient.module.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;
import pl.mysticclient.setting.BoolSetting;
import pl.mysticclient.util.Theme;
import pl.mysticclient.util.UI;
import pl.mysticclient.util.UI.Weight;

import java.util.ArrayList;
import java.util.List;

public class PotionHUD extends Module {
    private static final String[] ROMAN = {"", "", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};
    public final BoolSetting hideVanilla = add(new BoolSetting("Ukryj ikonki MC", true));

    public PotionHUD() {
        super("PotionHUD", "Lista aktywnych efektów mikstur z czasem.", Category.RENDER);
        hideFromArrayList();
    }

    @Override
    public void onRender2D(DrawContext ctx, float tickDelta) {
        if (nullCheck() || mc.getDebugHud().shouldShowDebugHud()) return;
        List<StatusEffectInstance> effects = new ArrayList<>(mc.player.getStatusEffects());
        if (effects.isEmpty()) return;
        float rowH = 26, w = 150;
        List<String[]> rows = new ArrayList<>();
        for (StatusEffectInstance e : effects) {
            int amp = e.getAmplifier() + 1;
            String name = e.getEffectType().value().getName().getString() + (amp < ROMAN.length ? ROMAN[amp] : " " + amp);
            String time = e.isInfinite() ? "∞" : format(e.getDuration());
            rows.add(new String[]{name, time});
            w = Math.max(w, UI.width(name, 12, Weight.MEDIUM) + UI.width(time, 11.5f, Weight.MEDIUM) + 52);
        }
        float h = 30 + rows.size() * rowH + 4;
        float x = UI.screenW() - w - 12, y = UI.screenH() - h - 70;
        ModernHud.card(x, y, w, h, 12);
        UI.textMid("Efekty", x + 12, y + 16, 11, Weight.SEMIBOLD, Theme.dim());
        UI.textRight(String.valueOf(rows.size()), x + w - 12, y + 16, 11, Weight.SEMIBOLD, Theme.dim());
        for (int i = 0; i < rows.size(); i++) {
            float ry = y + 30 + i * rowH;
            StatusEffectInstance e = effects.get(i);
            float pct = e.isInfinite() ? 1f : Math.min(1f, e.getDuration() / 1200f);
            UI.round(x + 8, ry, w - 16, rowH - 4, 7, UI.a(Theme.text(), 0.05f));
            UI.circle(x + 18, ry + (rowH - 4) / 2f, 3, Theme.text());
            UI.textMid(rows.get(i)[0], x + 28, ry + (rowH - 4) / 2f, 12, Weight.MEDIUM, Theme.text());
            UI.textRight(rows.get(i)[1], x + w - 16, ry + (rowH - 4) / 2f, 11.5f, Weight.MEDIUM, Theme.sub());
            UI.round(x + 12, ry + rowH - 6, (w - 24) * pct, 1.5f, 1, UI.a(Theme.text(), 0.35f));
        }
    }

    private static String format(int ticks) {
        int s = ticks / 20;
        return String.format("%d:%02d", s / 60, s % 60);
    }
}
