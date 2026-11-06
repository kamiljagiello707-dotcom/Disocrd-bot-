package pl.firen.client.module.modules;

import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.NumberSetting;

public class ChestStealer extends Module {
    public final NumberSetting delay = add(new NumberSetting("Delay ms", 80, 0, 500, 10));
    public final BooleanSetting autoClose = add(new BooleanSetting("Auto close", true));
    private long next;

    public ChestStealer() { super("ChestStealer", "Szybko zabiera przedmioty ze skrzyń.", Category.PLAYER); }

    @Override
    public void onTick() {
        if (!(mc.currentScreen instanceof GenericContainerScreen) && !(mc.currentScreen instanceof ShulkerBoxScreen)) return;
        ScreenHandler handler = mc.player.currentScreenHandler;
        int containerSlots = handler.slots.size() - 36;
        long now = System.currentTimeMillis();
        if (now < next) return;
        for (int i = 0; i < containerSlots; i++) {
            if (!handler.getSlot(i).hasStack()) continue;
            mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
            next = now + delay.get().longValue();
            if (delay.get() > 0) return;
        }
        boolean empty = true;
        for (int i = 0; i < containerSlots; i++) if (handler.getSlot(i).hasStack()) { empty = false; break; }
        if (empty && autoClose.get()) mc.player.closeHandledScreen();
    }
}
