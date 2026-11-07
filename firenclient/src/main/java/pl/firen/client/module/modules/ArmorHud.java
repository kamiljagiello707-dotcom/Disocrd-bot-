package pl.firen.client.module.modules;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import pl.firen.client.gui.Theme;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.render.Fonts;
import pl.firen.client.render.Render2D;
import pl.firen.client.setting.NumberSetting;

/** Pokazuje założoną zbroję i jej wytrzymałość. */
public class ArmorHud extends Module {
    public final NumberSetting x = add(new NumberSetting("X", 200, 0, 600, 1));
    public final NumberSetting y = add(new NumberSetting("Y", 300, 0, 400, 1));

    public ArmorHud() { super("ArmorHUD", "Założona zbroja i jej wytrzymałość.", Category.RENDER); }

    @Override
    public void onRenderHud(DrawContext ctx, float s) {
        if (!isEnabled() || mc.player == null || mc.options.hudHidden) return;
        var armor = mc.player.getInventory().armor;
        boolean any = false;
        for (ItemStack st : armor) if (!st.isEmpty()) any = true;
        ItemStack main = mc.player.getMainHandStack();
        if (!any && main.isEmpty()) return;

        float ox = x.get().floatValue(), oy = y.get().floatValue();
        float slot = 20, gap = 4;
        ItemStack[] items = { armor.get(3), armor.get(2), armor.get(1), armor.get(0), main };
        int count = 0; for (ItemStack it : items) if (!it.isEmpty()) count++;
        if (count == 0) return;
        float w = count * (slot + gap) - gap + 10, h = slot + 20;
        Render2D.round(ctx, ox, oy, w, h, 8, 0xC00C0C0F);
        Render2D.outline(ctx, ox, oy, w, h, 8, 0.5f, Theme.accentAlpha(0x35));
        float cx = ox + 5;
        for (ItemStack it : items) {
            if (it.isEmpty()) continue;
            ctx.drawItem(it, (int) (cx + 2), (int) (oy + 4));
            ctx.drawStackOverlay(mc.textRenderer, it, (int) (cx + 2), (int) (oy + 4));
            if (it.isDamageable()) {
                float dur = 1f - (float) it.getDamage() / it.getMaxDamage();
                int c = dur > 0.5f ? 0xFF3DDC97 : dur > 0.25f ? 0xFFE9C46A : 0xFFFF5C6A;
                Render2D.round(ctx, cx + 2, oy + slot + 6, slot - 4, 2.5f, 1f, 0xFF26262C);
                Render2D.round(ctx, cx + 2, oy + slot + 6, (slot - 4) * dur, 2.5f, 1f, c);
            }
            cx += slot + gap;
        }
    }
}
