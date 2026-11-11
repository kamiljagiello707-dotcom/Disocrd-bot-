package tech.fuzir.client.module.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.fuzir.client.event.events.TickEvent;
import tech.fuzir.client.module.Category;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.setting.BoolSetting;
import tech.fuzir.client.setting.ModeSetting;
import tech.fuzir.client.setting.NumberSetting;

import java.util.Comparator;

/**
 * Automatically attacks the best target in range once the attack cooldown is charged.
 */
public final class KillAura extends Module {
    private final NumberSetting range = number("Range", 3.5, 2.0, 6.0, 0.1, "m");
    private final NumberSetting cooldown = number("Cooldown", 0.95, 0.5, 1.0, 0.05);
    private final ModeSetting priority = mode("Priority", "Distance", "Distance", "Health", "Angle");
    private final ModeSetting rotation = mode("Rotation", "Silent", "Silent", "Client", "None");
    private final BoolSetting throughWalls = bool("Through Walls", false);
    private final BoolSetting players = bool("Players", true);
    private final BoolSetting hostiles = bool("Hostiles", true);
    private final BoolSetting passives = bool("Passives", false);
    private final BoolSetting ignoreInvisible = bool("Ignore Invisible", true);
    private final BoolSetting pauseWhileUsing = bool("Pause While Using", true);

    private LivingEntity target;

    public KillAura() {
        super("KillAura", "Attacks the best target around you when your swing is charged.", Category.COMBAT);
        on(TickEvent.class, e -> tick());
    }

    @Override
    protected void onDisable() {
        target = null;
    }

    private void tick() {
        target = null;
        if (mc.currentScreen != null || mc.interactionManager == null) return;
        if (pauseWhileUsing.isEnabled() && mc.player.isUsingItem()) return;

        target = findTarget();
        if (target == null) return;
        if (mc.player.getAttackCooldownProgress(0.5f) < cooldown.getFloat()) return;

        float[] angles = anglesTo(aimPoint(target));
        if (rotation.is("Client")) {
            mc.player.setYaw(angles[0]);
            mc.player.setPitch(angles[1]);
        } else if (rotation.is("Silent") && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(
                    new PlayerMoveC2SPacket.LookAndOnGround(angles[0], angles[1], mc.player.isOnGround()));
        }

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private LivingEntity findTarget() {
        Vec3d eyes = mc.player.getEyePos();
        double maxSq = range.get() * range.get();
        Comparator<LivingEntity> order = switch (priority.get()) {
            case "Health" -> Comparator.comparingDouble(e -> e.getHealth() + e.getAbsorptionAmount());
            case "Angle" -> Comparator.comparingDouble(this::angleDistance);
            default -> Comparator.comparingDouble(e -> closestPoint(e.getBoundingBox(), eyes).squaredDistanceTo(eyes));
        };
        LivingEntity best = null;
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || !isValid(living)) continue;
            if (closestPoint(living.getBoundingBox(), eyes).squaredDistanceTo(eyes) > maxSq) continue;
            if (!throughWalls.isEnabled() && !mc.player.canSee(living)) continue;
            if (best == null || order.compare(living, best) < 0) best = living;
        }
        return best;
    }

    private boolean isValid(LivingEntity entity) {
        if (entity == mc.player || !entity.isAlive() || entity.getHealth() <= 0f) return false;
        if (ignoreInvisible.isEnabled() && entity.isInvisible()) return false;
        if (entity instanceof PlayerEntity player) return players.isEnabled() && !player.isSpectator() && !player.isCreative();
        if (entity instanceof Monster) return hostiles.isEnabled();
        return passives.isEnabled();
    }

    private static Vec3d closestPoint(Box box, Vec3d point) {
        return new Vec3d(
                MathHelper.clamp(point.x, box.minX, box.maxX),
                MathHelper.clamp(point.y, box.minY, box.maxY),
                MathHelper.clamp(point.z, box.minZ, box.maxZ));
    }

    /** Aims at the point of the hitbox closest to the eyes, kept slightly inside the box vertically. */
    private Vec3d aimPoint(LivingEntity entity) {
        Box box = entity.getBoundingBox();
        Vec3d eyes = mc.player.getEyePos();
        double y = MathHelper.clamp(eyes.y, box.minY + entity.getHeight() * 0.2, box.maxY - entity.getHeight() * 0.1);
        return new Vec3d(box.getCenter().x, y, box.getCenter().z);
    }

    private float[] anglesTo(Vec3d point) {
        Vec3d diff = point.subtract(mc.player.getEyePos());
        double horizontal = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, horizontal));
        // Keep the yaw continuous with the current one to avoid 360° flips.
        yaw = mc.player.getYaw() + MathHelper.wrapDegrees(yaw - mc.player.getYaw());
        return new float[]{yaw, MathHelper.clamp(pitch, -90f, 90f)};
    }

    private double angleDistance(LivingEntity entity) {
        float[] angles = anglesTo(aimPoint(entity));
        float yaw = MathHelper.wrapDegrees(angles[0] - mc.player.getYaw());
        float pitch = angles[1] - mc.player.getPitch();
        return Math.sqrt(yaw * yaw + pitch * pitch);
    }

    public LivingEntity getTarget() {
        return target;
    }

    @Override
    public String getSuffix() {
        return priority.get();
    }
}
