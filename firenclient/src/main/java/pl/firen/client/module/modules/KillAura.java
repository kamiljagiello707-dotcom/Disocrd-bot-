package pl.firen.client.module.modules;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.ModeSetting;
import pl.firen.client.setting.NumberSetting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class KillAura extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Single", "Single", "Switch", "Multi"));
    public final ModeSetting priority = add(new ModeSetting("Priority", "Distance", "Distance", "Health", "Angle"));
    public final NumberSetting range = add(new NumberSetting("Range", 3.0, 2.5, 6.0, 0.1));
    public final BooleanSetting cooldown = add(new BooleanSetting("1.9 Cooldown", true));
    public final NumberSetting cps = add(new NumberSetting("CPS", 10, 1, 20, 1));
    public final BooleanSetting rotate = add(new BooleanSetting("Rotate", true));
    public final NumberSetting rotateSpeed = add(new NumberSetting("Rotate speed", 60, 10, 180, 5));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through walls", false));
    public final BooleanSetting onlyWeapon = add(new BooleanSetting("Only weapon", false));
    public final BooleanSetting pauseOnEat = add(new BooleanSetting("Pause on eat", true));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));

    private long nextAttackAt;
    private int switchIndex;
    private LivingEntity target;

    public KillAura() { super("KillAura", "Automatycznie atakuje cele w zasięgu.", Category.COMBAT); }

    @Override public String getSuffix() { return mode.get(); }

    public LivingEntity getTarget() { return target; }

    @Override
    public void onDisable() {
        target = null;
        switchIndex = 0;
    }

    @Override
    public void onTick() {
        target = null;
        if (mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (pauseOnEat.get() && mc.player.isUsingItem()) return;
        if (onlyWeapon.get() && !holdingWeapon()) return;

        List<LivingEntity> targets = findTargets();
        if (targets.isEmpty()) return;

        if (mode.is("Switch")) {
            switchIndex = Math.floorMod(switchIndex, targets.size());
            target = targets.get(switchIndex);
        } else {
            target = targets.get(0);
        }

        if (rotate.get()) faceSmooth(target);
        if (!canAttack()) return;

        if (mode.is("Multi")) {
            for (LivingEntity e : targets) attack(e);
        } else {
            attack(target);
            if (mode.is("Switch")) switchIndex++;
        }
        scheduleNext();
    }

    private List<LivingEntity> findTargets() {
        List<LivingEntity> list = new ArrayList<>();
        double r = range.get();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity living) || e == mc.player) continue;
            if (!living.isAlive() || living.isDead() || living.getHealth() <= 0) continue;
            if (!isValidType(living)) continue;
            if (distanceTo(living) > r) continue;
            if (!throughWalls.get() && !mc.player.canSee(living)) continue;
            list.add(living);
        }
        Comparator<LivingEntity> order = switch (priority.get()) {
            case "Health" -> Comparator.comparingDouble(LivingEntity::getHealth);
            case "Angle" -> Comparator.comparingDouble(this::angleTo);
            default -> Comparator.comparingDouble(this::distanceTo);
        };
        list.sort(order);
        return list;
    }

    private boolean isValidType(LivingEntity e) {
        if (e instanceof PlayerEntity p) return players.get() && !p.isSpectator() && !p.isCreative();
        if (e instanceof Monster) return mobs.get();
        if (e instanceof AnimalEntity) return animals.get();
        return false;
    }

    private boolean holdingWeapon() {
        ItemStack stack = mc.player.getMainHandStack();
        return stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES);
    }

    private boolean canAttack() {
        if (cooldown.get()) return mc.player.getAttackCooldownProgress(0.5f) >= 1f;
        return System.currentTimeMillis() >= nextAttackAt;
    }

    private void scheduleNext() {
        double base = 1000.0 / cps.get();
        double jitter = base * 0.15 * (ThreadLocalRandom.current().nextDouble() * 2 - 1);
        nextAttackAt = System.currentTimeMillis() + (long) (base + jitter);
    }

    private void attack(LivingEntity e) {
        mc.interactionManager.attackEntity(mc.player, e);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    /** Odległość od oczu gracza do najbliższego punktu hitboxa celu. */
    private double distanceTo(LivingEntity e) {
        Vec3d eye = mc.player.getEyePos();
        Box box = e.getBoundingBox();
        Vec3d closest = new Vec3d(
                MathHelper.clamp(eye.x, box.minX, box.maxX),
                MathHelper.clamp(eye.y, box.minY, box.maxY),
                MathHelper.clamp(eye.z, box.minZ, box.maxZ));
        return eye.distanceTo(closest);
    }

    private float[] rotationsTo(LivingEntity e) {
        Vec3d eye = mc.player.getEyePos();
        Box box = e.getBoundingBox();
        Vec3d aim = new Vec3d(box.getCenter().x, MathHelper.clamp(eye.y, box.minY + 0.2, box.maxY - 0.1), box.getCenter().z);
        Vec3d d = aim.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        return new float[]{yaw, pitch};
    }

    private double angleTo(LivingEntity e) {
        float[] rot = rotationsTo(e);
        return Math.abs(MathHelper.wrapDegrees(rot[0] - mc.player.getYaw()));
    }

    private void faceSmooth(LivingEntity e) {
        float[] rot = rotationsTo(e);
        float max = rotateSpeed.get().floatValue();
        float dy = MathHelper.clamp(MathHelper.wrapDegrees(rot[0] - mc.player.getYaw()), -max, max);
        float dp = MathHelper.clamp(rot[1] - mc.player.getPitch(), -max, max);
        mc.player.setYaw(mc.player.getYaw() + dy);
        mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + dp, -90f, 90f));
    }
}
