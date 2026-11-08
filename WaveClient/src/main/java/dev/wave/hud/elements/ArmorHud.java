package dev.wave.hud.elements;

import dev.wave.hud.HudElement;
import dev.wave.setting.BooleanSetting;
import dev.wave.setting.ModeSetting;
import dev.wave.util.ColorUtil;
import dev.wave.util.RenderUtil;
import dev.wave.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ArmorHud extends HudElement {
    public final ModeSetting layout = new ModeSetting("Layout", "", "Horizontal", "Horizontal", "Vertical");
    public final BooleanSetting hands = new BooleanSetting("Held Items", true);
    public final BooleanSetting percent = new BooleanSetting("Durability %", true);

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public ArmorHud() {
        super("Armor", "Shows your armour and its durability");
        addSettings(layout, hands, percent, background);
        background.set(false);
        setEnabledSilently(true);
    }

    @Override
    protected void defaultPosition(int sw, int sh) { x = sw / 2f + 96; y = sh - 22; }

    @Override
    public void render(DrawContext c, float delta, boolean editor) {
        List<ItemStack> stacks = new ArrayList<>();
        if (mc.player != null) {
            if (hands.isOn()) stacks.add(mc.player.getMainHandStack());
            for (EquipmentSlot s : ARMOR) stacks.add(mc.player.getEquippedStack(s));
            if (hands.isOn()) stacks.add(mc.player.getOffHandStack());
            stacks.removeIf(ItemStack::isEmpty);
        }
        if (stacks.isEmpty() && editor) {
            stacks.add(new ItemStack(Items.DIAMOND_HELMET));
            stacks.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
        }
        boolean vertical = layout.is("Vertical");
        float slot = vertical ? 18 : 20;
        float w = vertical ? (percent.isOn() ? 48 : 22) : Math.max(1, stacks.size()) * slot + 4;
        float h = vertical ? Math.max(1, stacks.size()) * slot + 4 : (percent.isOn() ? 30 : 22);
        resize(w, h);
        if (background.isOn()) RenderUtil.round(c, x, y, w, h, 3, Theme.HUD_BG);
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack st = stacks.get(i);
            int ix = (int) (vertical ? x + 3 : x + 4 + i * slot);
            int iy = (int) (vertical ? y + 3 + i * slot : y + 3);
            c.drawItem(st, ix, iy);
            c.drawStackOverlay(mc.textRenderer, st, ix, iy);
            if (percent.isOn() && st.isDamageable()) {
                float pct = 1f - st.getDamage() / (float) st.getMaxDamage();
                int col = ColorUtil.lerp(0xFFFF4040, 0xFFFFFFFF, pct);
                String t = Math.round(pct * 100) + "%";
                if (vertical) RenderUtil.text(c, t, ix + 20, iy + 4, col, true);
                else RenderUtil.textScaled(c, t, ix + 8 - RenderUtil.width(t) * 0.35f, iy + 18, 0.7f, col, true);
            }
        }
    }
}
