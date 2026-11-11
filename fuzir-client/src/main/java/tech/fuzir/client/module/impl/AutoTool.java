package tech.fuzir.client.module.impl;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.util.Icons;

public class AutoTool extends Module {
    private final BoolSetting back = add(new BoolSetting("Wróć do slotu", true));
    private final BoolSetting protect = add(new BoolSetting("Chroń zużyte narzędzia", true));
    private int previous = -1;

    public AutoTool() {
        super("AutoTool", "Bierze najlepsze narzędzie do kopanego bloku", Category.PLAYER, Icons.PICKAXE);
    }

    @Override
    public void onTick() {
        boolean mining = mc.options.attackKey.isPressed() && mc.crosshairTarget instanceof BlockHitResult;
        if (!mining) {
            if (previous >= 0 && back.on()) mc.player.getInventory().selectedSlot = previous;
            previous = -1;
            return;
        }
        BlockState state = mc.world.getBlockState(((BlockHitResult) mc.crosshairTarget).getBlockPos());
        if (state.isAir()) return;
        int best = mc.player.getInventory().selectedSlot;
        float bestSpeed = speed(mc.player.getInventory().getStack(best), state);
        for (int i = 0; i < 9; i++) {
            float s = speed(mc.player.getInventory().getStack(i), state);
            if (s > bestSpeed) {
                bestSpeed = s;
                best = i;
            }
        }
        if (best != mc.player.getInventory().selectedSlot) {
            if (previous < 0) previous = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = best;
        }
    }

    private float speed(ItemStack stack, BlockState state) {
        if (stack.isEmpty()) return 1f;
        // narzędzie z 1-2 punktami wytrzymałości zostawiamy, żeby się nie zepsuło
        if (protect.on() && stack.isDamageable() && stack.getMaxDamage() - stack.getDamage() <= 2) return 0f;
        return stack.getMiningSpeedMultiplier(state);
    }

    @Override
    public void onDisable() {
        previous = -1;
    }
}
