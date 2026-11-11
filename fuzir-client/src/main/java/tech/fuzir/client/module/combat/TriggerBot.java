package tech.fuzir.client.module.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import tech.fuzir.client.event.events.TickEvent;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.setting.NumberSetting;

public final class TriggerBot extends Module {
    private final NumberSetting cooldown = number("Cooldown", 0.95, 0.5, 1.0, 0.05);
    private final BoolSetting players = bool("Players", true);
    private final BoolSetting hostiles = bool("Hostiles", true);
    private final BoolSetting passives = bool("Passives", false);
    private final BoolSetting ignoreInvisible = bool("Ignore Invisible", true);

    public TriggerBot() {
        super("TriggerBot", "Attacks the entity under your crosshair once your swing is charged.", Category.COMBAT);
        on(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.currentScreen != null || mc.player.isUsingItem() || mc.interactionManager == null) return;
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return;
        Entity target = ((EntityHitResult) mc.crosshairTarget).getEntity();
        if (!isValid(target)) return;
        if (mc.player.getAttackCooldownProgress(0.5f) < cooldown.getFloat()) return;
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean isValid(Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity == mc.player) return false;
        if (!living.isAlive() || living.getHealth() <= 0f) return false;
        if (ignoreInvisible.isEnabled() && entity.isInvisible()) return false;
        if (entity instanceof PlayerEntity player) return players.isEnabled() && !player.isSpectator();
        if (entity instanceof Monster) return hostiles.isEnabled();
        return passives.isEnabled();
    }

    @Override
    public String getSuffix() {
        return cooldown.format();
    }
}
