package tech.fuzir.client.module.movement;

import tech.fuzir.client.event.events.TickEvent;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.ModeSetting;

public final class Sprint extends Module {
    private final ModeSetting mode = mode("Mode", "Legit", "Legit", "Omni");

    public Sprint() {
        super("Sprint", "Keeps you sprinting automatically.", Category.MOVEMENT);
        on(TickEvent.class, e -> tick());
    }

    private void tick() {
        var player = mc.player;
        if (player.isSneaking() || player.isUsingItem() || player.horizontalCollision || player.isTouchingWater()) return;
        boolean canSprintFood = player.getHungerManager().getFoodLevel() > 6 || player.getAbilities().allowFlying;
        if (!canSprintFood) return;
        float forward = player.input.movementForward;
        float sideways = player.input.movementSideways;
        boolean moving = mode.is("Omni") ? (forward != 0f || sideways != 0f) : forward > 0f;
        if (moving && !player.isSprinting()) player.setSprinting(true);
    }

    @Override
    public String getSuffix() {
        return mode.get();
    }
}
