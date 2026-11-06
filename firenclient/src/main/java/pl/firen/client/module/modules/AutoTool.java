package pl.firen.client.module.modules;

import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.BooleanSetting;

public class AutoTool extends Module {
    public final BooleanSetting switchBack = add(new BooleanSetting("Switch back", true));
    private int previous = -1;

    public AutoTool() { super("AutoTool", "Wybiera najlepsze narzędzie do kopanego bloku.", Category.PLAYER); }

    @Override
    public void onTick() {
        var inv = mc.player.getInventory();
        boolean mining = mc.options.attackKey.isPressed() && mc.crosshairTarget != null
                && mc.crosshairTarget.getType() == HitResult.Type.BLOCK;
        if (!mining) {
            if (switchBack.get() && previous >= 0) inv.selectedSlot = previous;
            previous = -1;
            return;
        }
        BlockState state = mc.world.getBlockState(((BlockHitResult) mc.crosshairTarget).getBlockPos());
        int best = inv.selectedSlot;
        float bestSpeed = inv.getStack(best).getMiningSpeedMultiplier(state);
        for (int i = 0; i < 9; i++) {
            float sp = inv.getStack(i).getMiningSpeedMultiplier(state);
            if (sp > bestSpeed + 0.01f) { bestSpeed = sp; best = i; }
        }
        if (best != inv.selectedSlot) {
            if (previous < 0) previous = inv.selectedSlot;
            inv.selectedSlot = best;
        }
    }
}
