package pl.mysticclient.module.combat;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;
import pl.mysticclient.setting.BoolSetting;
import pl.mysticclient.setting.ModeSetting;
import pl.mysticclient.setting.NumberSetting;
import pl.mysticclient.util.FriendManager;
import pl.mysticclient.util.Render3D;
import pl.mysticclient.util.Theme;
import pl.mysticclient.util.UI;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class KillAura extends Module {
    // --- Cel
    public final ModeSetting mode = add(new ModeSetting("Mode", "Single", "Single", "Switch", "Multi"));
    public final ModeSetting priority = add(new ModeSetting("Priority", "Distance", "Distance", "Health", "Angle", "HurtTime"));
    public final NumberSetting range = add(new NumberSetting("Range", 3.0, 2.5, 6.0, 0.05));
    public final NumberSetting aimRange = add(new NumberSetting("Aim range", 4.2, 3.0, 8.0, 0.1));
    public final NumberSetting fov = add(new NumberSetting("FOV", 360, 30, 360, 5));
    public final NumberSetting switchDelay = add(new NumberSetting("Switch delay", 250, 0, 1000, 10));

    // --- Rotacje (widoczne, po stronie klienta)
    public final ModeSetting rotations = add(new ModeSetting("Rotations", "Client", "Client", "None"));
    public final ModeSetting aimPoint = add(new ModeSetting("Aim point", "Nearest", "Nearest", "Center", "Head"));
    public final NumberSetting minSpeed = add(new NumberSetting("Min turn speed", 45, 5, 180, 1));
    public final NumberSetting maxSpeed = add(new NumberSetting("Max turn speed", 80, 5, 180, 1));
    public final BoolSetting raycast = add(new BoolSetting("Raycast", true));

    // --- Atak
    public final ModeSetting timing = add(new ModeSetting("Timing", "Cooldown", "Cooldown", "CPS"));
    public final NumberSetting cooldown = add(new NumberSetting("Cooldown %", 95, 50, 100, 1));
    public final NumberSetting minCps = add(new NumberSetting("Min CPS", 9, 1, 20, 1));
    public final NumberSetting maxCps = add(new NumberSetting("Max CPS", 13, 1, 20, 1));
    public final BoolSetting smartHurt = add(new BoolSetting("Smart HurtTime", true));
    public final BoolSetting critSync = add(new BoolSetting("Crit sync", true));
    public final BoolSetting throughWalls = add(new BoolSetting("Through walls", true));
    public final NumberSetting wallRange = add(new NumberSetting("Wall range", 3.0, 1.0, 6.0, 0.05));
    public final BoolSetting autoWeapon = add(new BoolSetting("Auto weapon", true));
    public final BoolSetting ignoreFriends = add(new BoolSetting("Ignore friends", true));

    // --- Filtry
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting mobs = add(new BoolSetting("Mobs", true));
    public final BoolSetting animals = add(new BoolSetting("Animals", false));
    public final BoolSetting invisibles = add(new BoolSetting("Invisibles", false));
    public final BoolSetting teams = add(new BoolSetting("Ignore team", true));
    public final BoolSetting antiBot = add(new BoolSetting("AntiBot", true));
    public final BoolSetting onlyWeapon = add(new BoolSetting("Only weapon", false));
    public final BoolSetting pauseOnEat = add(new BoolSetting("Pause on eat", true));

    // --- Wbudowany Target ESP
    public final ModeSetting espMode = add(new ModeSetting("Target ESP", "Ring", "Ring", "Circle", "Box", "Orbit", "Pulse", "Corners", "Glow", "Off"));
    public final BoolSetting espTheme = add(new BoolSetting("ESP theme color", true));
    public final ModeSetting espColor = add(new ModeSetting("ESP color", "Red", "Red", "White", "Purple", "Blue", "Green", "Gold"));
    public final NumberSetting espAnimSpeed = add(new NumberSetting("ESP speed", 5.0, 1.0, 10.0, 0.5));

    private final Random random = new Random();
    private LivingEntity target;
    private long nextAttackAt, lastSwitchAt;
    private int switchIndex, critWait;
    private float animationProgress = 0f;

    public KillAura() {
        super("KillAura", "Atakuje cele w zasięgu, z wbudowanym Target ESP.", Category.COMBAT);
        switchDelay.visibleWhen(() -> mode.is("Switch"));
        aimPoint.visibleWhen(() -> !rotations.is("None"));
        minSpeed.visibleWhen(() -> !rotations.is("None"));
        maxSpeed.visibleWhen(() -> !rotations.is("None"));
        raycast.visibleWhen(() -> !rotations.is("None"));
        cooldown.visibleWhen(() -> timing.is("Cooldown"));
        minCps.visibleWhen(() -> timing.is("CPS"));
        maxCps.visibleWhen(() -> timing.is("CPS"));
        smartHurt.visibleWhen(() -> timing.is("CPS"));
        wallRange.visibleWhen(throughWalls::get);
        espTheme.visibleWhen(() -> !espMode.is("Off"));
        espColor.visibleWhen(() -> !espMode.is("Off") && !espTheme.get());
        espAnimSpeed.visibleWhen(() -> !espMode.is("Off") && !espMode.is("Glow"));
    }

    @Override public String getSuffix() { return mode.get(); }
    public LivingEntity getTarget() { return target; }

    @Override
    public void onDisable() {
        target = null;
        switchIndex = 0;
        critWait = 0;
    }

    // ---------------------------------------------------------------- logika

    @Override
    public void onTick() {
        if (nullCheck()) return;
        if (mc.currentScreen != null || (pauseOnEat.get() && mc.player.isUsingItem())
                || (onlyWeapon.get() && !holdingWeapon())) {
            target = null;
            return;
        }

        // wybór celu + obrót
        target = pickTarget(findTargets(aimRange.get()));
        if (target == null) return;
        if (!rotations.is("None")) {
            float[] goal = rotationsTo(aimPointOf(target));
            float[] next = turn(mc.player.getYaw(), mc.player.getPitch(), goal[0], goal[1]);
            mc.player.setYaw(next[0]);
            mc.player.setPitch(MathHelper.clamp(next[1], -90f, 90f));
        }

        // atak
        if (mc.interactionManager == null) return;
        List<LivingEntity> inRange = findTargets(range.get());
        if (!inRange.contains(target)) return;
        boolean visible = canSee(target);
        if (!rotations.is("None") && raycast.get() && !rayHits(target, visible)) return;
        if (!readyToAttack(target)) return;
        if (autoWeapon.get()) selectWeapon();

        if (mode.is("Multi")) {
            for (LivingEntity e : inRange) attack(e);
        } else {
            attack(target);
        }
        scheduleNext();
    }

    private LivingEntity pickTarget(List<LivingEntity> list) {
        if (list.isEmpty()) return null;
        if (mode.is("Switch")) {
            if (System.currentTimeMillis() - lastSwitchAt >= switchDelay.get()) {
                switchIndex++;
                lastSwitchAt = System.currentTimeMillis();
            }
            return list.get(Math.floorMod(switchIndex, list.size()));
        }
        if (target != null && list.contains(target)) return target;
        return list.get(0);
    }

    private List<LivingEntity> findTargets(double maxRange) {
        List<LivingEntity> list = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity living) || e == mc.player) continue;
            if (!living.isAlive() || living.isDead() || living.getHealth() <= 0) continue;
            if (!isValidType(living)) continue;
            if (distanceTo(living) > maxRange) continue;
            if (fov.get() < 360 && angleTo(living) > fov.get() / 2) continue;
            if (!canSee(living)) {
                if (!throughWalls.get()) continue;
                if (distanceTo(living) > Math.min(maxRange, wallRange.get() + (maxRange > range.get() ? 1.0 : 0.0))) continue;
            }
            list.add(living);
        }
        Comparator<LivingEntity> order = switch (priority.get()) {
            case "Health" -> Comparator.comparingDouble(e -> e.getHealth() + e.getAbsorptionAmount());
            case "Angle" -> Comparator.comparingDouble(this::angleTo);
            case "HurtTime" -> Comparator.<LivingEntity>comparingInt(e -> e.hurtTime).thenComparingDouble(this::distanceTo);
            default -> Comparator.comparingDouble(this::distanceTo);
        };
        list.sort(order);
        return list;
    }

    private boolean isValidType(LivingEntity e) {
        if (e.isInvisible() && !invisibles.get()) return false;
        if (teams.get() && mc.player.isTeammate(e)) return false;
        if (ignoreFriends.get() && FriendManager.isFriend(e.getName().getString())) return false;
        if (e instanceof PlayerEntity p) {
            if (!players.get() || p.isSpectator() || p.isCreative()) return false;
            return !antiBot.get() || !isBot(p);
        }
        if (e instanceof Monster) return mobs.get();
        if (e instanceof AnimalEntity) return animals.get();
        return false;
    }

    private boolean isBot(PlayerEntity p) {
        if (p.age < 20) return true;
        var handler = mc.getNetworkHandler();
        if (handler == null || handler.getPlayerListEntry(p.getUuid()) == null) return true;
        String name = p.getGameProfile().getName();
        return name == null || name.isBlank() || name.length() > 16 || !name.matches("[A-Za-z0-9_]+");
    }

    private boolean holdingWeapon() {
        ItemStack stack = mc.player.getMainHandStack();
        return stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES);
    }

    private boolean readyToAttack(LivingEntity e) {
        if (critSync.get() && waitForCrit()) return false;
        if (timing.is("Cooldown")) {
            return mc.player.getAttackCooldownProgress(0.5f) >= cooldown.get() / 100.0;
        }
        if (smartHurt.get() && e.hurtTime > 1) return false;
        return System.currentTimeMillis() >= nextAttackAt;
    }

    private boolean waitForCrit() {
        var p = mc.player;
        boolean canCrit = !p.isOnGround() && !p.isTouchingWater() && !p.isClimbing() && !p.hasVehicle()
                && !p.getAbilities().flying;
        if (!canCrit) { critWait = 0; return false; }
        if (p.fallDistance > 0 && p.getVelocity().y < 0) { critWait = 0; return false; }
        return ++critWait <= 8;
    }

    private void selectWeapon() {
        var inv = mc.player.getInventory();
        int best = inv.selectedSlot;
        double bestDmg = damageOf(inv.getStack(best));
        for (int i = 0; i < 9; i++) {
            double d = damageOf(inv.getStack(i));
            if (d > bestDmg + 0.01) { bestDmg = d; best = i; }
        }
        inv.selectedSlot = best;
    }

    public static double damageOf(ItemStack stack) {
        double[] v = {0};
        stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT)
                .applyModifiers(EquipmentSlot.MAINHAND, (attr, mod) -> {
                    if (attr.equals(EntityAttributes.ATTACK_DAMAGE)) v[0] += mod.value();
                });
        return v[0];
    }

    private void scheduleNext() {
        if (!timing.is("CPS")) return;
        double lo = Math.min(minCps.get(), maxCps.get()), hi = Math.max(minCps.get(), maxCps.get());
        double cps = lo + (hi - lo) * random.nextDouble();
        nextAttackAt = System.currentTimeMillis() + (long) Math.max(45, 1000.0 / cps);
    }

    private void attack(LivingEntity e) {
        mc.interactionManager.attackEntity(mc.player, e);
        mc.player.swingHand(Hand.MAIN_HAND);
        critWait = 0;
    }

    private double distanceTo(LivingEntity e) {
        Vec3d eye = mc.player.getEyePos();
        Box box = e.getBoundingBox();
        return eye.distanceTo(new Vec3d(
                MathHelper.clamp(eye.x, box.minX, box.maxX),
                MathHelper.clamp(eye.y, box.minY, box.maxY),
                MathHelper.clamp(eye.z, box.minZ, box.maxZ)));
    }

    private boolean canSee(LivingEntity e) {
        Vec3d eye = mc.player.getEyePos();
        Box b = e.getBoundingBox();
        Vec3d[] points = {b.getCenter(), new Vec3d(b.getCenter().x, b.maxY - 0.1, b.getCenter().z),
                new Vec3d(b.getCenter().x, b.minY + 0.1, b.getCenter().z)};
        for (Vec3d p : points) {
            var hit = mc.world.raycast(new RaycastContext(eye, p, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, mc.player));
            if (hit.getType() == HitResult.Type.MISS) return true;
        }
        return false;
    }

    private boolean rayHits(LivingEntity e, boolean visible) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d end = eye.add(Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw()).multiply(range.get() + 0.5));
        Box box = e.getBoundingBox().expand(e.getTargetingMargin() + 0.05);
        if (box.contains(eye)) return true;
        var hit = box.raycast(eye, end);
        if (hit.isEmpty()) return false;
        if (eye.distanceTo(hit.get()) > range.get()) return false;
        if (throughWalls.get() || !visible) return true;
        var block = mc.world.raycast(new RaycastContext(eye, hit.get(), RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, mc.player));
        return block.getType() == HitResult.Type.MISS;
    }

    private Vec3d aimPointOf(LivingEntity e) {
        Box b = e.getBoundingBox();
        Vec3d eye = mc.player.getEyePos();
        double w = b.getLengthX(), h = b.getLengthY();
        Vec3d c = b.getCenter();
        return switch (aimPoint.get()) {
            case "Center" -> new Vec3d(c.x, b.minY + h * 0.5, c.z);
            case "Head" -> new Vec3d(c.x, b.maxY - h * 0.12, c.z);
            default -> {
                double inset = 0.15;
                yield new Vec3d(
                        MathHelper.clamp(eye.x, b.minX + w * inset, b.maxX - w * inset),
                        MathHelper.clamp(eye.y, b.minY + h * 0.2, b.maxY - h * 0.1),
                        MathHelper.clamp(eye.z, b.minZ + w * inset, b.maxZ - w * inset));
            }
        };
    }

    private float[] rotationsTo(Vec3d point) {
        Vec3d d = point.subtract(mc.player.getEyePos());
        float yaw = (float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        return new float[]{yaw, pitch};
    }

    private double angleTo(LivingEntity e) {
        float[] rot = rotationsTo(e.getBoundingBox().getCenter());
        return Math.abs(MathHelper.wrapDegrees(rot[0] - mc.player.getYaw()));
    }

    /** Płynny obrót w stronę celu z prędkością z zakresu min–max. */
    private float[] turn(float yaw, float pitch, float goalYaw, float goalPitch) {
        float dy = MathHelper.wrapDegrees(goalYaw - yaw);
        float dp = goalPitch - pitch;
        float dist = (float) Math.hypot(dy, dp);
        if (dist < 0.05f) return new float[]{yaw, pitch};
        double lo = Math.min(minSpeed.get(), maxSpeed.get()), hi = Math.max(minSpeed.get(), maxSpeed.get());
        float speed = (float) (lo + (hi - lo) * random.nextDouble());
        float factor = Math.min(1f, speed / dist);
        if (dist < 15f) factor *= Math.max(0.4f, dist / 15f);
        return new float[]{yaw + dy * factor, MathHelper.clamp(pitch + dp * factor, -90f, 90f)};
    }

    // ---------------------------------------------------------------- Target ESP

    public int espRgb() {
        if (espTheme.get()) return Theme.accent();
        return switch (espColor.get()) {
            case "White" -> 0xFFFFFFFF;
            case "Purple" -> 0xFFA78BFA;
            case "Blue" -> 0xFF60A5FA;
            case "Green" -> 0xFF4ADE80;
            case "Gold" -> 0xFFFACC15;
            default -> 0xFFFF4D5E;
        };
    }

    /** Używane przez mixiny do trybu Glow. */
    public boolean glowTarget(Entity e) {
        return isEnabled() && espMode.is("Glow") && target != null && e == target;
    }

    @Override
    public void onRender3D(WorldRenderContext ctx) {
        if (target == null || mc.player == null || espMode.is("Off") || espMode.is("Glow")) return;

        animationProgress += UI.Frame.delta() * (float) (double) espAnimSpeed.get();
        if (animationProgress > Math.PI * 2) animationProgress -= (float) (Math.PI * 2);

        float td = ctx.tickCounter().getTickDelta(true);
        Vec3d cam = ctx.camera().getPos();
        Matrix4f m = Render3D.matrix(ctx);
        Vec3d pos = target.getLerpedPos(td);
        float w = target.getWidth(), h = target.getHeight();
        int c = espRgb();
        float t = animationProgress;

        switch (espMode.get()) {
            case "Ring" -> {
                // pierścień jeżdżący góra-dół z zanikającym ogonem
                float k = (float) (Math.sin(t) + 1) / 2f;
                double y = pos.y + h * k;
                double trail = 0.35 * (Math.cos(t) > 0 ? -1 : 1);
                double r = w * 0.8;
                BufferBuilder q = Render3D.beginQuads();
                ring(q, m, cam, pos.x, y, pos.z, r, y + trail, UI.withAlpha(c, 150), UI.withAlpha(c, 0));
                Render3D.draw(q);
                BufferBuilder l = Render3D.beginLines();
                circleLines(l, m, cam, pos.x, y, pos.z, r, c);
                Render3D.draw(l);
            }
            case "Circle" -> {
                double r = w * 0.9;
                BufferBuilder q = Render3D.beginQuads();
                disc(q, m, cam, pos.x, pos.y + 0.02, pos.z, r, UI.withAlpha(c, 45));
                Render3D.draw(q);
                BufferBuilder l = Render3D.beginLines();
                circleLines(l, m, cam, pos.x, pos.y + 0.02, pos.z, r, c);
                Render3D.draw(l);
            }
            case "Box" -> {
                Box box = target.getBoundingBox().offset(pos.subtract(target.getPos())).expand(0.08);
                BufferBuilder q = Render3D.beginQuads();
                Render3D.boxFill(q, m, cam, box, UI.withAlpha(c, 40));
                Render3D.draw(q);
                BufferBuilder l = Render3D.beginLines();
                Render3D.boxLines(l, m, cam, box, c);
                Render3D.draw(l);
            }
            case "Orbit" -> {
                // trzy kulki krążące wokół celu
                BufferBuilder q = Render3D.beginQuads();
                double r = w * 0.9;
                for (int i = 0; i < 3; i++) {
                    double a = t * 1.5 + i * (Math.PI * 2 / 3);
                    double y = pos.y + h * (0.35 + 0.25 * Math.sin(t * 2 + i));
                    double px = pos.x + Math.cos(a) * r, pz = pos.z + Math.sin(a) * r;
                    double s = 0.08;
                    Render3D.boxFill(q, m, cam, new Box(px - s, y - s, pz - s, px + s, y + s, pz + s), UI.withAlpha(c, 220));
                }
                Render3D.draw(q);
            }
            case "Pulse" -> {
                // rozchodzące się fale pod stopami
                BufferBuilder l = Render3D.beginLines();
                for (int i = 0; i < 3; i++) {
                    float p = ((t / (float) (Math.PI * 2)) + i / 3f) % 1f;
                    circleLines(l, m, cam, pos.x, pos.y + 0.02, pos.z, w * (0.3 + p * 1.2), UI.withAlpha(c, (int) (255 * (1 - p))));
                }
                Render3D.draw(l);
            }
            case "Corners" -> {
                Box b = target.getBoundingBox().offset(pos.subtract(target.getPos())).expand(0.1);
                BufferBuilder l = Render3D.beginLines();
                corners(l, m, cam, b, c);
                Render3D.draw(l);
            }
            default -> {}
        }
    }

    private static final int SEG = 48;

    private static void circleLines(BufferBuilder b, Matrix4f m, Vec3d cam, double cx, double y, double cz, double r, int color) {
        for (int i = 0; i < SEG; i++) {
            double a1 = Math.PI * 2 * i / SEG, a2 = Math.PI * 2 * (i + 1) / SEG;
            Render3D.line(b, m, cam, new Vec3d(cx + Math.cos(a1) * r, y, cz + Math.sin(a1) * r),
                    new Vec3d(cx + Math.cos(a2) * r, y, cz + Math.sin(a2) * r), color);
        }
    }

    private static void ring(BufferBuilder b, Matrix4f m, Vec3d cam, double cx, double y, double cz, double r, double y2, int c1, int c2) {
        for (int i = 0; i < SEG; i++) {
            double a1 = Math.PI * 2 * i / SEG, a2 = Math.PI * 2 * (i + 1) / SEG;
            float x1 = (float) (cx + Math.cos(a1) * r - cam.x), z1 = (float) (cz + Math.sin(a1) * r - cam.z);
            float x2 = (float) (cx + Math.cos(a2) * r - cam.x), z2 = (float) (cz + Math.sin(a2) * r - cam.z);
            float ya = (float) (y - cam.y), yb = (float) (y2 - cam.y);
            b.vertex(m, x1, ya, z1).color(c1);
            b.vertex(m, x2, ya, z2).color(c1);
            b.vertex(m, x2, yb, z2).color(c2);
            b.vertex(m, x1, yb, z1).color(c2);
        }
    }

    private static void disc(BufferBuilder b, Matrix4f m, Vec3d cam, double cx, double y, double cz, double r, int c) {
        float ccx = (float) (cx - cam.x), cy = (float) (y - cam.y), ccz = (float) (cz - cam.z);
        for (int i = 0; i < SEG; i++) {
            double a1 = Math.PI * 2 * i / SEG, a2 = Math.PI * 2 * (i + 1) / SEG;
            b.vertex(m, ccx, cy, ccz).color(c);
            b.vertex(m, (float) (cx + Math.cos(a1) * r - cam.x), cy, (float) (cz + Math.sin(a1) * r - cam.z)).color(c);
            b.vertex(m, (float) (cx + Math.cos(a2) * r - cam.x), cy, (float) (cz + Math.sin(a2) * r - cam.z)).color(c);
            b.vertex(m, ccx, cy, ccz).color(c);
        }
    }

    private static void corners(BufferBuilder b, Matrix4f m, Vec3d cam, Box box, int c) {
        double lx = box.getLengthX() * 0.3, ly = box.getLengthY() * 0.2, lz = box.getLengthZ() * 0.3;
        double[] xs = {box.minX, box.maxX}, ys = {box.minY, box.maxY}, zs = {box.minZ, box.maxZ};
        for (int xi = 0; xi < 2; xi++) for (int yi = 0; yi < 2; yi++) for (int zi = 0; zi < 2; zi++) {
            double x = xs[xi], y = ys[yi], z = zs[zi];
            double dx = xi == 0 ? lx : -lx, dy = yi == 0 ? ly : -ly, dz = zi == 0 ? lz : -lz;
            Vec3d p = new Vec3d(x, y, z);
            Render3D.line(b, m, cam, p, p.add(dx, 0, 0), c);
            Render3D.line(b, m, cam, p, p.add(0, dy, 0), c);
            Render3D.line(b, m, cam, p, p.add(0, 0, dz), c);
        }
    }
}
