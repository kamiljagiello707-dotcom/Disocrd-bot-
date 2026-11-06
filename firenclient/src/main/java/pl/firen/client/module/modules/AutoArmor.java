package pl.firen.client.module.modules;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.NumberSetting;

/** Zakłada najlepszą zbroję z ekwipunku. */
public class AutoArmor extends Module {
    public final NumberSetting delay = add(new NumberSetting("Delay ticks", 2, 0, 10, 1));
    private int wait;
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public AutoArmor() { super("AutoArmor", "Automatycznie zakłada najlepszą zbroję.", Category.PLAYER); }

    private static double value(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) return 0;
        var eq = stack.get(DataComponentTypes.EQUIPPABLE);
        if (eq == null || eq.slot() != slot) return -1;
        double[] v = {0};
        stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT)
                .applyModifiers(slot, (attr, mod) -> {
                    if (attr.equals(EntityAttributes.ARMOR)) v[0] += mod.value();
                    else if (attr.equals(EntityAttributes.ARMOR_TOUGHNESS)) v[0] += mod.value() * 0.5;
                });
        return v[0] + stack.getEnchantments().getSize() * 0.1;
    }

    @Override
    public void onTick() {
        if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;
        if (wait-- > 0) return;
        var inv = mc.player.getInventory();
        int sync = mc.player.playerScreenHandler.syncId;
        for (int a = 0; a < 4; a++) {
            EquipmentSlot slot = SLOTS[a];
            int armorSlotId = 5 + a;
            ItemStack worn = mc.player.getEquippedStack(slot);
            double best = value(worn, slot);
            int bestInv = -1;
            for (int i = 0; i < 36; i++) {
                double v = value(inv.getStack(i), slot);
                if (v > best + 0.01) { best = v; bestInv = i; }
            }
            if (bestInv < 0) continue;
            int invSlotId = bestInv < 9 ? 36 + bestInv : bestInv;
            if (!worn.isEmpty()) {
                // zdejmij gorszą część (do ekwipunku), nową założymy w kolejnym ticku
                mc.interactionManager.clickSlot(sync, armorSlotId, 0, SlotActionType.QUICK_MOVE, mc.player);
            } else {
                mc.interactionManager.clickSlot(sync, invSlotId, 0, SlotActionType.QUICK_MOVE, mc.player);
            }
            wait = delay.get().intValue();
            return;
        }
    }
}
