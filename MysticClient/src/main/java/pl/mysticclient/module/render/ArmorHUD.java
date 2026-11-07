package pl.mysticclient.module.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;
import pl.mysticclient.util.Theme;
import pl.mysticclient.util.UI;
import pl.mysticclient.util.UI.Weight;

public class ArmorHUD extends Module {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};

    public ArmorHUD() {
        super("ArmorHUD", "Pokazuje założoną zbroję i jej wytrzymałość.", Category.RENDER);
        hideFromArrayList();
    }

    @Override
    public void onRender2D(DrawContext ctx, float tickDelta) {
        if (nullCheck() || mc.getDebugHud().shouldShowDebugHud()) return;
        int count = 0;
        for (EquipmentSlot s : SLOTS) if (!mc.player.getEquippedStack(s).isEmpty()) count++;
        if (count == 0) return;
        float cell = 36, w = count * cell + 8, h = 46;
        float x = UI.screenW() - w - 12, y = UI.screenH() - h - 12;
        ModernHud.card(x, y, w, h, 12);
        float ix = x + 4;
        for (EquipmentSlot s : SLOTS) {
            ItemStack stack = mc.player.getEquippedStack(s);
            if (stack.isEmpty()) continue;
            var ms = ctx.getMatrices();
            ms.push();
            ms.translate(ix + cell / 2f - 12, y + 6, 0);
            ms.scale(1.5f, 1.5f, 1f);
            ctx.drawItem(stack, 0, 0);
            ms.pop();
            if (stack.isDamageable()) {
                float pct = 1f - stack.getDamage() / (float) stack.getMaxDamage();
                UI.round(ix + 6, y + h - 9, cell - 12, 3, 1.5f, UI.a(Theme.text(), 0.15f));
                UI.round(ix + 6, y + h - 9, Math.max(3, (cell - 12) * pct), 3, 1.5f, Theme.text());
            } else if (stack.getCount() > 1) {
                UI.textCenter(String.valueOf(stack.getCount()), ix + cell / 2f, y + h - 8, 10, Weight.SEMIBOLD, Theme.text());
            }
            ix += cell;
        }
    }
}
