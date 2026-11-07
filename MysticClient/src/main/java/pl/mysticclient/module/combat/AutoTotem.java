package pl.mysticclient.module.combat;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;
import pl.mysticclient.setting.NumberSetting;

public class AutoTotem extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 2, 0, 20, 1));
    private int timer;

    public AutoTotem() {
        super("AutoTotem", "Automatycznie wkłada totem do drugiej ręki.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;
        if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) { timer = 0; return; }
        if (timer++ < delay.getInt()) return;
        timer = 0;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                int slot = i < 9 ? 36 + i : i;
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, mc.player);
                return;
            }
        }
    }
}
