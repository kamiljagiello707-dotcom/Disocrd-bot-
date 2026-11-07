package pl.mysticclient.module.player;

import net.minecraft.component.DataComponentTypes;
import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;
import pl.mysticclient.setting.NumberSetting;

public class AutoEat extends Module {
    private final NumberSetting hunger = add(new NumberSetting("Hunger", 14, 1, 19, 1));
    private boolean eating;
    private int prevSlot = -1;

    public AutoEat() {
        super("AutoEat", "Je automatycznie, gdy jesteś głodny.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        int food = mc.player.getHungerManager().getFoodLevel();
        if (eating) {
            if (food >= 20 || !mc.player.getMainHandStack().contains(DataComponentTypes.FOOD)) stop();
            else mc.options.useKey.setPressed(true);
            return;
        }
        if (food > hunger.getInt() || mc.currentScreen != null) return;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).contains(DataComponentTypes.FOOD)) {
                prevSlot = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = i;
                eating = true;
                mc.options.useKey.setPressed(true);
                return;
            }
        }
    }

    private void stop() {
        eating = false;
        mc.options.useKey.setPressed(false);
        if (prevSlot >= 0 && mc.player != null) mc.player.getInventory().selectedSlot = prevSlot;
        prevSlot = -1;
    }

    @Override
    public void onDisable() {
        if (eating) stop();
    }
}
