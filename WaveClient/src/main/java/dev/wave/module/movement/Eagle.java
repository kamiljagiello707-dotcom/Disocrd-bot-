package dev.wave.module.movement;

import dev.wave.module.Category;
import dev.wave.module.Module;

public class Eagle extends Module {
    public Eagle() { super("Eagle", "Sneaks automatically at block edges (safe bridging)", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        boolean edge = mc.player.isOnGround()
                && mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset(0, -0.6, 0).expand(-0.25, 0, -0.25));
        mc.options.sneakKey.setPressed(edge || isPhysicallyPressed());
    }

    private boolean isPhysicallyPressed() {
        return net.minecraft.client.util.InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                net.minecraft.client.util.InputUtil.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey()).getCode());
    }

    @Override
    public void onDisable() { mc.options.sneakKey.setPressed(false); }
}
