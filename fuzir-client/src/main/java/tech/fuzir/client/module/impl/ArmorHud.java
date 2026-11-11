package tech.fuzir.client.module.impl;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.math.MatrixStack;
import tech.fuzir.client.hud.Hud;
import tech.fuzir.client.module.HudModule;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.util.Canvas;
import tech.fuzir.client.util.Canvas.F;
import tech.fuzir.client.util.Icons;

import java.util.ArrayList;
import java.util.List;

public class ArmorHud extends HudModule {
    public final BoolSetting hand = add(new BoolSetting("Przedmiot w ręce", true));
    public final BoolSetting percent = add(new BoolSetting("Procent wytrzymałości", true));
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final float ROW = 20, PAD = 6;

    public ArmorHud() {
        super("Zbroja", "Założona zbroja i jej wytrzymałość", Icons.SHIELD, 1f, 0.55f);
    }

    private List<ItemStack> items() {
        List<ItemStack> out = new ArrayList<>();
        if (mc.player == null) return out;
        for (EquipmentSlot s : SLOTS) {
            ItemStack st = mc.player.getEquippedStack(s);
            if (!st.isEmpty()) out.add(st);
        }
        if (hand.on() && !mc.player.getMainHandStack().isEmpty()) out.add(mc.player.getMainHandStack());
        return out;
    }

    @Override
    public float[] size(Canvas cv) {
        List<ItemStack> items = items();
        if (items.isEmpty()) return Hud.editing() ? new float[]{80, ROW + PAD * 2} : null;
        return new float[]{percent.on() ? 74 : 44, items.size() * ROW + PAD * 2 - 2};
    }

    @Override
    public void draw(Canvas cv, float x, float y, float w, float h, boolean right) {
        Hud.panel(cv, x, y, w, h, 8);
        List<ItemStack> items = items();
        if (items.isEmpty()) {
            cv.textCenter("Zbroja", x + w / 2, y + h / 2, Hud.DIM, F.SMALL);
            return;
        }
        float ry = y + PAD;
        for (ItemStack st : items) {
            // przedmiot rysowany przez grę, przeskalowany do jednostek HUD
            cv.ctx.draw();
            MatrixStack m = cv.ctx.getMatrices();
            m.push();
            m.translate((x + PAD) * cv.u, ry * cv.u, 0);
            float k = cv.u * 16f / 16f;
            m.scale(k, k, 1);
            cv.ctx.drawItem(st, 0, 0);
            m.pop();
            if (st.isDamageable()) {
                float left = 1f - st.getDamage() / (float) st.getMaxDamage();
                float bx = x + PAD + 20, bw = w - PAD * 2 - 20;
                int col = Canvas.mix(0xFFFF5A5A, Hud.ACCENT, Math.min(1, left * 1.6f));
                if (percent.on()) cv.textMid(Math.round(left * 100) + "%", bx, ry + 6, Hud.TEXT, F.SMALL);
                cv.round(bx, ry + 12, bw, 2.5f, 1.25f, 0x22FFFFFF);
                cv.round(bx, ry + 12, Math.max(2.5f, bw * left), 2.5f, 1.25f, col);
            } else if (st.getCount() > 1) {
                cv.textMid("x" + st.getCount(), x + PAD + 20, ry + 8, Hud.TEXT, F.REGULAR);
            }
            ry += ROW;
        }
    }
}
