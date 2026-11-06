package pl.firen.client.module.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
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
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.rotation.RotationManager;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.ModeSetting;
import pl.firen.client.setting.NumberSetting;
import pl.firen.client.util.Anim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * KillAura: ciche, "ludzkie" rotacje z move fixem, sprawdzanie trafienia promieniem,
 * inteligentne wyczucie ciosu (cooldown / CPS / hurtTime / krytyki), AntiBot i Target ESP.
 */
public class KillAura extends Module {
    // --- Cel
    public final ModeSetting mode = add(new ModeSetting("Mode", "Single", "Single", "Switch", "Multi"));
    public final ModeSetting priority = add(new ModeSetting("Priority", "Distance", "Distance", "Health", "Angle", "HurtTime"));
    public final NumberSetting range = add(new NumberSetting("Range", 3.0, 2.5, 6.0, 0.05));
    public final NumberSetting aimRange = add(new NumberSetting("Aim range", 4.2, 3.0, 8.0, 0.1));
    public final NumberSetting fov = add(new NumberSetting("FOV", 360, 30, 360, 5));
    public final NumberSetting switchDelay = add(new NumberSetting("Switch delay", 250, 0, 1000, 10));

    // --- Rotacje
    public final ModeSetting rotations = add(new ModeSetting("Rotations", "Silent", "Silent", "Client", "None"));
    public final ModeSetting aimPoint = add(new ModeSetting("Aim point", "Nearest", "Nearest", "Center", "Head"));
    public final NumberSetting minSpeed = add(new NumberSetting("Min turn speed", 45, 5, 180, 1));
    public final NumberSetting maxSpeed = add(new NumberSetting("Max turn speed", 80, 5, 180, 1));
    public final BooleanSetting humanize = add(new BooleanSetting("Humanize", true));
    public final BooleanSetting gcdFix = add(new BooleanSetting("Sensitivity fix", true));
    public final BooleanSetting moveFix = add(new BooleanSetting("Move fix", true));
    public final BooleanSetting raycast = add(new BooleanSetting("Raycast", true));

    // --- Atak
    public final ModeSetting timing = add(new ModeSetting("Timing", "Cooldown", "Cooldown", "CPS"));
    public final NumberSetting cooldown = add(new NumberSetting("Cooldown %", 95, 50, 100, 1));
    public final NumberSetting minCps = add(new NumberSetting("Min CPS", 9, 1, 20, 1));
    public final NumberSetting maxCps = add(new NumberSetting("Max CPS", 13, 1, 20, 1));
    public final BooleanSetting smartHurt = add(new BooleanSetting("Smart HurtTime", true));
    public final BooleanSetting critSync = add(new BooleanSetting("Crit sync", true));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through walls", false));

    // --- Filtry
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));
    public final BooleanSetting invisibles = add(new BooleanSetting("Invisibles", false));
    public final BooleanSetting teams = add(new BooleanSetting("Ignore team", true));
    public final BooleanSetting antiBot = add(new BooleanSetting("AntiBot", true));
    public final BooleanSetting onlyWeapon = add(new BooleanSetting("Only weapon", false));
    public final BooleanSetting pauseOnEat = add(new BooleanSetting("Pause on eat", true));

    // --- Wygląd
    public final BooleanSetting targetEsp = add(new BooleanSetting("Target ESP", true));

    private final Random random = new Random();
    private LivingEntity target, espTarget;
    private long nextAttackAt, lastSwitchAt;
    private int switchIndex, critWait;
    private double driftX, driftY, driftZ;
    private float espAnim;

    public KillAura() {
        super("KillAura", "Ciche rotacje, raycast, krytyki i Target ESP.", Category.COMBAT);
        switchDelay.visibleWhen(() -> mode.is("Switch"));
        aimPoint.visibleWhen(() -> !rotations.is("None"));
        minSpeed.visibleWhen(() -> !rotations.is("None"));
        maxSpeed.visibleWhen(() -> !rotations.is("None"));
        humanize.visibleWhen(() -> !rotations.is("None"));
        gcdFix.visibleWhen(() -> !rotations.is("None"));
        moveFix.visibleWhen(() -> rotations.is("Silent"));
        raycast.visibleWhen(() -> !rotations.is("None"));
        cooldown.visibleWhen(() -> timing.is("Cooldown"));
        minCps.visibleWhen(() -> timing.is("CPS"));
        maxCps.visibleWhen(() -> timing.is("CPS"));
        smartHurt.visibleWhen(() -> timing.is("CPS"));
    }

    @Override public String getSuffix() { return mode.get(); }

    public LivingEntity getTarget() { return target; }

    @Override
    public void onDisable() {
        target = null;
        switchIndex = 0;
        critWait = 0;
    }

    // ------------------------------------------------------------ wybór celu + rotacje (przed pakietem ruchu)

    @Override
    public void onPreTick() {
        if (mc.currentScreen != null || (pauseOnEat.get() && mc.player.isUsingItem())
                || (onlyWeapon.get() && !holdingWeapon())) {
            target = null;
            return;
        }

        List<LivingEntity> candidates = findTargets(aimRange.get());
        target = pickTarget(candidates);
        if (target == null || rotations.is("None")) return;

        float[] goal = rotationsTo(aimPointOf(target));
        float curYaw = rotations.is("Silent") ? RotationManager.currentYaw() : mc.player.getYaw();
        float curPitch = rotations.is("Silent") ? RotationManager.currentPitch() : mc.player.getPitch();
        float[] next = turn(curYaw, curPitch, goal[0], goal[1]);

        if (rotations.is("Silent")) {
            RotationManager.request(next[0], next[1], moveFix.get(), (float) (double) maxSpeed.get());
        } else {
            mc.player.setYaw(next[0]);
            mc.player.setPitch(MathHelper.clamp(next[1], -90f, 90f));
        }
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
        // Single / Multi: trzymaj obecny cel, dopóki jest poprawny (brak "skakania" po celach)
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
            if (!throughWalls.get() && !canSee(living)) continue;
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
        if (e instanceof PlayerEntity p) {
            if (!players.get() || p.isSpectator() || p.isCreative()) return false;
            return !antiBot.get() || !isBot(p);
        }
        if (e instanceof Monster) return mobs.get();
        if (e instanceof AnimalEntity) return animals.get();
        return false;
    }

    /** Prosty AntiBot: gracz musi być na liście graczy (TAB) i mieć poprawny nick. */
    private boolean isBot(PlayerEntity p) {
        var handler = mc.getNetworkHandler();
        if (handler == null || handler.getPlayerListEntry(p.getUuid()) == null) return true;
        String name = p.getGameProfile().getName();
        return name == null || name.isBlank() || name.length() > 16 || !name.matches("[A-Za-z0-9_]+");
    }

    private boolean holdingWeapon() {
        ItemStack stack = mc.player.getMainHandStack();
        return stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES);
    }

    // ------------------------------------------------------------ atak (po ruchu, rotacja jest już na serwerze)

    @Override
    public void onTick() {
        if (target == null || mc.interactionManager == null || mc.currentScreen != null) return;

        List<LivingEntity> inRange = findTargets(range.get());
        if (!inRange.contains(target)) return;
        if (!rotations.is("None") && raycast.get() && !rayHits(target)) return;
        if (!readyToAttack(target)) return;

        if (mode.is("Multi")) {
            for (LivingEntity e : inRange) attack(e);
        } else {
            attack(target);
        }
        scheduleNext();
    }

    private boolean readyToAttack(LivingEntity e) {
        if (critSync.get() && waitForCrit()) return false;
        if (timing.is("Cooldown")) {
            return mc.player.getAttackCooldownProgress(0.5f) >= cooldown.get() / 100.0;
        }
        if (smartHurt.get() && e.hurtTime > 1) return false;
        return System.currentTimeMillis() >= nextAttackAt;
    }

    /** Czekaj na fazę opadania skoku, żeby cios był krytyczny (max ~8 ticków). */
    private boolean waitForCrit() {
        var p = mc.player;
        boolean canCrit = !p.isOnGround() && !p.isTouchingWater() && !p.isClimbing() && !p.hasVehicle()
                && !p.getAbilities().flying;
        if (!canCrit) { critWait = 0; return false; }
        if (p.fallDistance > 0 && p.getVelocity().y < 0) { critWait = 0; return false; }
        return ++critWait <= 8;
    }

    private void scheduleNext() {
        if (!timing.is("CPS")) return;
        double lo = Math.min(minCps.get(), maxCps.get()), hi = Math.max(minCps.get(), maxCps.get());
        double cps = lo + (hi - lo) * random.nextDouble();
        double delay = 1000.0 / cps + random.nextGaussian() * 12.0;
        nextAttackAt = System.currentTimeMillis() + (long) Math.max(45, delay);
    }

    private void attack(LivingEntity e) {
        mc.interactionManager.attackEntity(mc.player, e);
        mc.player.swingHand(Hand.MAIN_HAND);
        critWait = 0;
    }

    // ------------------------------------------------------------ geometria

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

    /** Czy promień z rotacji wysłanej do serwera trafia w hitbox celu (a nie w ścianę). */
    private boolean rayHits(LivingEntity e) {
        float yaw = rotations.is("Silent") ? RotationManager.sentYaw : mc.player.getYaw();
        float pitch = rotations.is("Silent") ? RotationManager.sentPitch : mc.player.getPitch();
        Vec3d eye = mc.player.getEyePos();
        Vec3d end = eye.add(Vec3d.fromPolar(pitch, yaw).multiply(range.get() + 0.5));
        Box box = e.getBoundingBox().expand(e.getTargetingMargin() + 0.05);
        if (box.contains(eye)) return true;
        var hit = box.raycast(eye, end);
        if (hit.isEmpty()) return false;
        if (eye.distanceTo(hit.get()) > range.get()) return false;
        if (throughWalls.get()) return true;
        var block = mc.world.raycast(new RaycastContext(eye, hit.get(), RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, mc.player));
        return block.getType() == HitResult.Type.MISS;
    }

    private Vec3d aimPointOf(LivingEntity e) {
        Box b = e.getBoundingBox();
        Vec3d eye = mc.player.getEyePos();
        double w = b.getLengthX(), h = b.getLengthY();
        if (humanize.get()) {
            // powolny "dryf" punktu celowania, jak u człowieka
            driftX = MathHelper.clamp(driftX + random.nextGaussian() * 0.02, -0.25, 0.25);
            driftY = MathHelper.clamp(driftY + random.nextGaussian() * 0.02, -0.25, 0.25);
            driftZ = MathHelper.clamp(driftZ + random.nextGaussian() * 0.02, -0.25, 0.25);
        } else {
            driftX = driftY = driftZ = 0;
        }
        double cx = b.getCenter().x + driftX * w, cz = b.getCenter().z + driftZ * w;
        return switch (aimPoint.get()) {
            case "Center" -> new Vec3d(cx, b.minY + h * (0.5 + driftY * 0.5), cz);
            case "Head" -> new Vec3d(cx, b.maxY - h * (0.12 + Math.abs(driftY) * 0.2), cz);
            default -> {
                double inset = 0.15;
                double x = MathHelper.clamp(eye.x, b.minX + w * inset, b.maxX - w * inset) + driftX * w * 0.4;
                double y = MathHelper.clamp(eye.y, b.minY + h * 0.2, b.maxY - h * 0.1) + driftY * h * 0.3;
                double z = MathHelper.clamp(eye.z, b.minZ + w * inset, b.maxZ - w * inset) + driftZ * w * 0.4;
                yield new Vec3d(x, y, z);
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

    /** Płynny obrót: zmienna prędkość, zwalnianie przy celu, mikro-drgania i dopasowanie do czułości myszy. */
    private float[] turn(float yaw, float pitch, float goalYaw, float goalPitch) {
        float dy = MathHelper.wrapDegrees(goalYaw - yaw);
        float dp = goalPitch - pitch;
        float dist = (float) Math.hypot(dy, dp);
        if (dist < 0.05f) return new float[]{yaw, pitch};

        double lo = Math.min(minSpeed.get(), maxSpeed.get()), hi = Math.max(minSpeed.get(), maxSpeed.get());
        float speed = (float) (lo + (hi - lo) * random.nextDouble());
        float factor = Math.min(1f, speed / dist);
        if (humanize.get()) {
            // wygładzanie przy małych odległościach (bez "przyklejania" w jednym ticku)
            factor *= (float) (0.62 + random.nextDouble() * 0.33);
            if (dist < 3f) factor = Math.max(factor, 0.55f);
        }
        float stepYaw = dy * factor, stepPitch = dp * factor;
        if (humanize.get()) {
            stepYaw += (float) (random.nextGaussian() * 0.18);
            stepPitch += (float) (random.nextGaussian() * 0.12);
        }
        if (gcdFix.get()) {
            double f = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
            double gcd = f * f * f * 8.0 * 0.15;
            stepYaw = (float) (Math.round(stepYaw / gcd) * gcd);
            stepPitch = (float) (Math.round(stepPitch / gcd) * gcd);
        }
        return new float[]{yaw + stepYaw, MathHelper.clamp(pitch + stepPitch, -90f, 90f)};
    }

    // ------------------------------------------------------------ Target ESP (animowany pierścień)

    @Override
    public void onRenderWorld(WorldRenderContext ctx) {
        float dt = 1f / 60f;
        LivingEntity t = isEnabled() && targetEsp.get() ? target : null;
        if (t != null) espTarget = t;
        espAnim = Anim.approach(espAnim, t != null ? 1f : 0f, dt, 10f);
        if (espAnim < 0.01f || espTarget == null || ctx.matrixStack() == null) return;

        float delta = ctx.tickCounter().getTickDelta(true);
        Vec3d pos = espTarget.getLerpedPos(delta).subtract(ctx.camera().getPos());
        double radius = espTarget.getWidth() * 0.85;
        double height = espTarget.getHeight();
        double time = (System.currentTimeMillis() % 1800) / 1800.0 * Math.PI * 2;
        double wave = Math.sin(time) * 0.5 + 0.5;
        double tail = Math.cos(time) > 0 ? -0.32 : 0.32; // smuga za ruchem pierścienia

        MatrixStack ms = ctx.matrixStack();
        ms.push();
        ms.translate(pos.x, pos.y + wave * height, pos.z);
        Matrix4f mat = ms.peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        int a = (int) (170 * espAnim);
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 64; i++) {
            double ang = i / 64.0 * Math.PI * 2;
            float x = (float) (Math.cos(ang) * radius), z = (float) (Math.sin(ang) * radius);
            bb.vertex(mat, x, 0, z).color(255, 255, 255, a);
            bb.vertex(mat, x, (float) tail, z).color(255, 255, 255, 0);
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());

        // cienka jasna linia na krawędzi pierścienia
        BufferBuilder line = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 64; i++) {
            double ang = i / 64.0 * Math.PI * 2;
            float x = (float) (Math.cos(ang) * radius), z = (float) (Math.sin(ang) * radius);
            line.vertex(mat, x, 0.012f, z).color(255, 255, 255, (int) (255 * espAnim));
            line.vertex(mat, x, -0.012f, z).color(255, 255, 255, (int) (255 * espAnim));
        }
        BufferRenderer.drawWithGlobalProgram(line.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ms.pop();
    }
}
