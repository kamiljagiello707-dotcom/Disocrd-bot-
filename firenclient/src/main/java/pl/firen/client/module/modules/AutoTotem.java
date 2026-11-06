package pl.firen.client.module.modules;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.NumberSetting;

public class AutoTotem extends Module {
    public final NumberSetting delay = add(new NumberSetting("Delay ticks", 1, 0, 10, 1));
    private int wait;

    public AutoTotem() { super("AutoTotem", "Trzyma totem nieśmiertelności w drugiej ręce.", Category.COMBAT); }

    @Override
    public String getSuffix() {
        if (mc.player == null) return null;
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) count += mc.player.getInventory().getStack(i).getCount();
        }
        return String.valueOf(count);
    }

    @Override
    public void onTick() {
        if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;
        if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        if (wait-- > 0) return;
        var inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (!inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) continue;
            int slot = i < 9 ? 36 + i : i; // indeks slotu w ekwipunku gracza
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, mc.player);
            wait = delay.get().intValue();
            return;
        }
    }
}
