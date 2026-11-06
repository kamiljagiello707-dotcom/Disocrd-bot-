package pl.mystic.client.module.modules;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import pl.mystic.client.module.Category;
import pl.mystic.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class AutoTotem extends Module {
    private int delay;

    public AutoTotem() { super("AutoTotem", "Automatycznie wkłada totem do drugiej ręki", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (delay > 0) { delay--; return; }
        if (p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;

        var inv = p.getInventory();
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                int slot = i < 9 ? i + 36 : i; // indeks slotu w ekwipunku gracza
                mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, p);
                delay = 3;
                return;
            }
        }
    }
}
