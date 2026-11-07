package pl.firen.client.module.modules;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.text.Text;
import pl.firen.client.gui.Theme;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.render.Fonts;
import pl.firen.client.render.Render2D;
import pl.firen.client.setting.NumberSetting;

import java.util.ArrayList;
import java.util.List;

/** Lista aktywnych efektów mikstur na HUD. */
public class PotionHud extends Module {
    public final NumberSetting x = add(new NumberSetting("X", 14, 0, 400, 1));
    public final NumberSetting y = add(new NumberSetting("Y", 300, 0, 400, 1));

    public PotionHud() { super("PotionHUD", "Aktywne efekty mikstur na ekranie.", Category.RENDER); }

    @Override
    public void onRenderHud(DrawContext ctx, float s) {
        if (!isEnabled() || mc.player == null || mc.options.hudHidden) return;
        List<StatusEffectInstance> fx = new ArrayList<>(mc.player.getStatusEffects());
        if (fx.isEmpty()) return;
        float ox = x.get().floatValue(), oy = y.get().floatValue();
        float rowH = 16, maxW = 0;
        String[] lines = new String[fx.size()]; String[] times = new String[fx.size()];
        for (int i = 0; i < fx.size(); i++) {
            StatusEffectInstance e = fx.get(i);
            String name = Text.translatable(e.getEffectType().value().getTranslationKey()).getString();
            int amp = e.getAmplifier();
            lines[i] = name + (amp > 0 ? " " + (amp + 1) : "");
            times[i] = e.isInfinite() ? "∞" : StatusEffectUtil.getDurationText(e, 1f, mc.world == null ? 20f : mc.world.getTickManager().getTickRate()).getString();
            maxW = Math.max(maxW, Fonts.width(lines[i], Fonts.SEMIBOLD) + Fonts.width(times[i], Fonts.REGULAR) + 22);
        }
        float h = fx.size() * rowH + 8;
        Render2D.round(ctx, ox, oy, maxW, h, 8, 0xC00C0C0F);
        Render2D.outline(ctx, ox, oy, maxW, h, 8, 0.5f, Theme.accentAlpha(0x35));
        for (int i = 0; i < fx.size(); i++) {
            float ry = oy + 5 + i * rowH;
            Render2D.round(ctx, ox + 6, ry + 2, 2f, 9, 1f, Theme.accent());
            Fonts.draw(ctx, lines[i], Fonts.SEMIBOLD, ox + 12, ry, Theme.TEXT);
            Fonts.drawRight(ctx, times[i], Fonts.REGULAR, ox + maxW - 8, ry, Theme.TEXT_3);
        }
    }
}
