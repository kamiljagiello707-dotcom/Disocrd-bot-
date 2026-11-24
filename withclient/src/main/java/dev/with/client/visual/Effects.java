package dev.with.client.visual;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.with.client.render.Fonts;
import dev.with.client.render.R2D;
import dev.with.client.render.WorldFx;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

/**
 * Efekty kosmetyczne w świecie (tylko u Ciebie, z testem głębi – nic przez ściany):
 * Trails, HitParticles, HitSound, KillEffect, DamageNumbers, Breadcrumbs, Atmosphere, BlockOutline.
 */
public final class Effects {
    private Effects() {}

    private static final Identifier DOT = R2D.id("textures/fx/dot.png");
    private static final Map<Integer, Long> HITS = new HashMap<>();
    private static final Map<Integer, Float> HEALTH = new HashMap<>();
    private static final Set<Integer> KILLED = new HashSet<>();
    private record Num(double x, double y, double z, String text, int color, long t, float dx, float dz) {}
    private static final List<Num> NUMS = new ArrayList<>();
    private static final ArrayDeque<Vec3d> CRUMBS = new ArrayDeque<>();
    private static final Random RND = new Random();
    private static int fakeId = -2_000_000;
    private static long hitTime;

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && player == MinecraftClient.getInstance().player) onAttack(entity);
            return ActionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(Effects::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(Effects::render);
        WorldRenderEvents.BLOCK_OUTLINE.register(Effects::outline);
    }

    /** czas ostatniego uderzenia (hitmarker celownika) */
    public static long hitTime() { return hitTime; }

    // ------------------------------------------------------------------ uderzenie

    private static void onAttack(Entity e) {
        long now = System.currentTimeMillis();
        hitTime = now;
        if (e instanceof LivingEntity le) {
            HITS.put(e.getId(), now);
            HEALTH.putIfAbsent(e.getId(), le.getHealth());
        }
        hitSound();
        hitParticles(e);
    }

    private static void play(SoundEvent ev, float pitch, float volume) {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(ev, pitch, volume));
    }

    private static void hitSound() {
        if (!V.on("HitSound")) return;
        SoundEvent ev = switch (V.mode("HitSound", "Dźwięk", "Ding")) {
            case "Pop" -> SoundEvents.ENTITY_ITEM_PICKUP;
            case "Crit" -> SoundEvents.ENTITY_PLAYER_ATTACK_CRIT;
            case "Bell" -> SoundEvents.BLOCK_NOTE_BLOCK_BELL.value();
            case "Click" -> SoundEvents.UI_BUTTON_CLICK.value();
            case "Bass" -> SoundEvents.BLOCK_NOTE_BLOCK_BASS.value();
            case "Chime" -> SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
            case "Pling" -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
            default -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
        };
        float pitch = V.num("HitSound", "Wysokość", 1);
        if (V.bo("HitSound", "Losowa wysokość")) pitch *= 0.88f + RND.nextFloat() * 0.24f;
        play(ev, pitch, V.num("HitSound", "Głośność", 80) / 100f);
    }

    private static ParticleEffect particle(String type, int color) {
        return switch (type) {
            case "Hearts" -> ParticleTypes.HEART;
            case "Crits" -> ParticleTypes.CRIT;
            case "Magic" -> ParticleTypes.ENCHANTED_HIT;
            case "Blood" -> new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.getDefaultState());
            case "Flame" -> ParticleTypes.FLAME;
            case "Soul" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "Totem" -> ParticleTypes.TOTEM_OF_UNDYING;
            case "Glow" -> ParticleTypes.GLOW;
            case "Snow" -> ParticleTypes.SNOWFLAKE;
            case "Cherry" -> ParticleTypes.CHERRY_LEAVES;
            case "Notes" -> ParticleTypes.NOTE;
            case "Dust" -> new DustParticleEffect(new Vector3f(V.r(color), V.g(color), V.b(color)), 1.1f);
            default -> ParticleTypes.END_ROD; // Stars / Sparkle
        };
    }

    private static void spawn(ParticleEffect p, double x, double y, double z, double vx, double vy, double vz) {
        MinecraftClient.getInstance().particleManager.addParticle(p, x, y, z, vx, vy, vz);
    }

    private static void hitParticles(Entity e) {
        if (!V.on("HitParticles")) return;
        int n = (int) V.num("HitParticles", "Ilość", 6);
        boolean phys = V.bo("HitParticles", "Fizyka");
        String type = V.mode("HitParticles", "Typ", "Stars");
        Box b = e.getBoundingBox();
        for (int i = 0; i < n; i++) {
            ParticleEffect p = particle(type, V.col("HitParticles", 0xFFFFFFFF, i * 0.07f));
            double x = MathHelper.lerp(RND.nextDouble(), b.minX, b.maxX), y = MathHelper.lerp(0.3 + RND.nextDouble() * 0.6, b.minY, b.maxY), z = MathHelper.lerp(RND.nextDouble(), b.minZ, b.maxZ);
            double s = phys ? 0.28 : 0.04;
            double vx = (RND.nextDouble() - 0.5) * s, vy = phys ? RND.nextDouble() * 0.22 : 0.02, vz = (RND.nextDouble() - 0.5) * s;
            if (type.equals("Notes")) vx = RND.nextInt(25) / 24.0;
            spawn(p, x, y, z, vx, vy, vz);
        }
    }

    // ------------------------------------------------------------------ tick

    private static void tick(MinecraftClient mc) {
        ClientWorld w = mc.world;
        if (mc.player == null || w == null) { HITS.clear(); HEALTH.clear(); KILLED.clear(); NUMS.clear(); CRUMBS.clear(); atm = null; return; }
        long now = System.currentTimeMillis();
        trails(mc);
        crumbs(mc);

        // obrażenia i zabójstwa: tylko cele, które niedawno uderzyłeś (albo wszystkie w pobliżu dla DamageNumbers)
        HITS.entrySet().removeIf(en -> now - en.getValue() > 5000);
        boolean all = V.on("DamageNumbers") && !V.bo("DamageNumbers", "Tylko moje uderzenia");
        Set<Integer> seen = new HashSet<>();
        if (all) {
            for (Entity e : w.getEntities())
                if (e instanceof LivingEntity le && le != mc.player && le.squaredDistanceTo(mc.player) < 32 * 32) { seen.add(e.getId()); health(le, now); }
        }
        for (Integer id : new ArrayList<>(HITS.keySet())) {
            if (seen.contains(id)) continue;
            Entity e = w.getEntityById(id);
            if (e instanceof LivingEntity le) { seen.add(id); health(le, now); }
        }
        HEALTH.keySet().retainAll(seen);
        if (KILLED.size() > 64) KILLED.clear();
        NUMS.removeIf(n -> now - n.t() > 1300);
    }

    private static void health(LivingEntity le, long now) {
        int id = le.getId();
        float hp = le.getHealth();
        Float prev = HEALTH.put(id, hp);
        if (prev != null && hp < prev - 0.05f && V.on("DamageNumbers")) addNumber(le, prev - hp, now);
        if (HITS.containsKey(id) && (le.isDead() || hp <= 0) && KILLED.add(id)) killEffect(le);
    }

    private static void addNumber(LivingEntity e, float dmg, long now) {
        String s = dmg >= 10 ? String.format(Locale.ROOT, "%.0f", dmg) : String.format(Locale.ROOT, "%.1f", dmg);
        if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
        int c = V.color("DamageNumbers", "Kolor", 0xFFFFFFFF);
        if (V.bo("DamageNumbers", "Kolor wg obrażeń")) c = dmg >= 6 ? 0xFFFF5C7A : dmg >= 3 ? 0xFFFFC857 : 0xFFFFFFFF;
        float a = RND.nextFloat() * 6.28f;
        NUMS.add(new Num(e.getX() + Math.cos(a) * e.getWidth() * 0.6, e.getY() + e.getHeight() * (0.65 + RND.nextFloat() * 0.3), e.getZ() + Math.sin(a) * e.getWidth() * 0.6,
                "-" + s, c, now, (float) Math.cos(a) * 0.35f, (float) Math.sin(a) * 0.35f));
        while (NUMS.size() > 40) NUMS.remove(0);
    }

    private static void killEffect(LivingEntity e) {
        if (!V.on("KillEffect")) return;
        if (V.bo("KillEffect", "Tylko gracze") && !(e instanceof PlayerEntity)) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld w = mc.world;
        if (w == null) return;
        double x = e.getX(), y = e.getY(), z = e.getZ();
        boolean sound = V.bo("KillEffect", "Dźwięk");
        switch (V.mode("KillEffect", "Efekt", "Lightning")) {
            case "Souls" -> {
                for (int i = 0; i < 26; i++) spawn(i % 3 == 0 ? ParticleTypes.SCULK_SOUL : ParticleTypes.SOUL, x + RND.nextGaussian() * 0.3, y + RND.nextDouble() * e.getHeight(), z + RND.nextGaussian() * 0.3, RND.nextGaussian() * 0.02, 0.06 + RND.nextDouble() * 0.1, RND.nextGaussian() * 0.02);
                if (sound) w.playSound(x, y, z, SoundEvents.PARTICLE_SOUL_ESCAPE.value(), SoundCategory.PLAYERS, 1.4f, 0.8f, false);
            }
            case "Explosion" -> {
                for (int i = 0; i < 4; i++) spawn(ParticleTypes.EXPLOSION, x + RND.nextGaussian() * 0.5, y + 0.5 + RND.nextDouble(), z + RND.nextGaussian() * 0.5, 0, 0, 0);
                for (int i = 0; i < 20; i++) spawn(ParticleTypes.SMOKE, x, y + 0.6, z, RND.nextGaussian() * 0.12, RND.nextDouble() * 0.15, RND.nextGaussian() * 0.12);
                if (sound) w.playSound(x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 0.8f, 1.2f, false);
            }
            case "Blood" -> {
                ParticleEffect p = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.getDefaultState());
                for (int i = 0; i < 70; i++) spawn(p, x + RND.nextGaussian() * 0.25, y + RND.nextDouble() * e.getHeight(), z + RND.nextGaussian() * 0.25, RND.nextGaussian() * 0.2, RND.nextDouble() * 0.3, RND.nextGaussian() * 0.2);
                if (sound) w.playSound(x, y, z, SoundEvents.BLOCK_HONEY_BLOCK_BREAK, SoundCategory.PLAYERS, 1f, 0.7f, false);
            }
            case "Firework" -> {
                for (int i = 0; i < 90; i++) {
                    Vec3d d = new Vec3d(RND.nextGaussian(), RND.nextGaussian(), RND.nextGaussian()).normalize().multiply(0.22);
                    spawn(ParticleTypes.FIREWORK, x, y + e.getHeight() * 0.7, z, d.x, d.y, d.z);
                }
                if (sound) w.playSound(x, y, z, SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 1.2f, 1f, false);
            }
            case "Totem" -> {
                for (int i = 0; i < 70; i++) spawn(ParticleTypes.TOTEM_OF_UNDYING, x, y + e.getHeight() * 0.6, z, RND.nextGaussian() * 0.35, RND.nextDouble() * 0.6, RND.nextGaussian() * 0.35);
                if (sound) w.playSound(x, y, z, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.8f, 1.3f, false);
            }
            default -> { // Lightning: piorun tylko u Ciebie (bez obrażeń i ognia)
                LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w);
                if (bolt == null) return;
                bolt.setCosmetic(true);
                bolt.refreshPositionAfterTeleport(x, y, z);
                bolt.setId(fakeId--);
                w.addEntity(bolt);
            }
        }
    }

    private static void trails(MinecraftClient mc) {
        if (!V.on("Trails")) return;
        var p = mc.player;
        boolean moving = p.getVelocity().horizontalLengthSquared() > 0.0006 || !p.isOnGround();
        if (V.bo("Trails", "Tylko w ruchu") && !moving) return;
        String type = V.mode("Trails", "Cząsteczki", "Sparkle");
        int n = (int) V.num("Trails", "Ilość", 3);
        if (type.equals("Hearts") && RND.nextInt(3) != 0) return;
        for (int i = 0; i < n; i++) {
            int c = V.col("Trails", 0xFFFFFFFF, (p.age + i) * 0.02f);
            ParticleEffect pe = particle(type, c);
            double a = RND.nextDouble() * Math.PI * 2, r = 0.15 + RND.nextDouble() * 0.25;
            double y = type.equals("Dust") || type.equals("Snow") ? 0.1 + RND.nextDouble() * 0.3 : 0.2 + RND.nextDouble() * 1.2;
            double vx = (RND.nextDouble() - 0.5) * 0.02, vy = type.equals("Flame") || type.equals("Soul") ? 0.01 : 0.0, vz = (RND.nextDouble() - 0.5) * 0.02;
            if (type.equals("Notes")) vx = ((p.age / 2 + i) % 25) / 24.0;
            spawn(pe, p.getX() + Math.cos(a) * r, p.getY() + y, p.getZ() + Math.sin(a) * r, vx, vy, vz);
        }
    }

    private static void crumbs(MinecraftClient mc) {
        if (!V.on("Breadcrumbs")) { CRUMBS.clear(); return; }
        Vec3d pos = mc.player.getPos().add(0, 0.06, 0);
        if (CRUMBS.isEmpty() || CRUMBS.peekLast().squaredDistanceTo(pos) > 0.01) CRUMBS.addLast(pos);
        int max = (int) V.num("Breadcrumbs", "Długość", 200);
        while (CRUMBS.size() > max) CRUMBS.removeFirst();
    }

    // ------------------------------------------------------------------ render

    private static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        boolean crumbs = V.on("Breadcrumbs") && CRUMBS.size() > 1, atmos = V.on("Atmosphere"), nums = V.on("DamageNumbers") && !NUMS.isEmpty();
        if (!crumbs && !atmos && !nums) { if (!atmos) atm = null; return; }
        MatrixStack ms = ctx.matrixStack() != null ? ctx.matrixStack() : new MatrixStack();
        Camera cam = ctx.camera();
        float delta = ctx.tickCounter().getTickDelta(true);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        try {
            if (crumbs) renderCrumbs(ms, cam, delta);
            if (atmos) renderAtmosphere(ms, cam);
            else atm = null;
        } finally {
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
        if (nums) renderNumbers(ms, cam);
    }

    private static void renderCrumbs(MatrixStack ms, Camera cam, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<Vec3d> pts = new ArrayList<>(CRUMBS);
        pts.add(mc.player.getLerpedPos(delta).add(0, 0.06, 0));
        Vec3d cp = cam.getPos();
        float w = V.num("Breadcrumbs", "Grubość", 1.5f) * 0.03f;
        boolean fade = V.bo("Breadcrumbs", "Zanikanie");
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        Matrix4f m = ms.peek().getPositionMatrix();
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int n = pts.size();
        for (int i = 0; i < n - 1; i++) {
            Vec3d a = pts.get(i), b = pts.get(i + 1);
            Vec3d dir = b.subtract(a);
            if (dir.lengthSquared() < 1e-6) continue;
            Vec3d sa = dir.crossProduct(cp.subtract(a)).normalize().multiply(w), sb = dir.crossProduct(cp.subtract(b)).normalize().multiply(w);
            float fa = fade ? i / (float) n : 1, fb = fade ? (i + 1) / (float) n : 1;
            int ca = V.col("Breadcrumbs", 0xFFFFFFFF, i * 0.01f), cb = V.col("Breadcrumbs", 0xFFFFFFFF, (i + 1) * 0.01f);
            float aa = V.a(ca) * fa * 0.85f, ab = V.a(cb) * fb * 0.85f;
            vtx(bb, m, a.add(sa), cp, ca, aa); vtx(bb, m, a.subtract(sa), cp, ca, aa);
            vtx(bb, m, b.subtract(sb), cp, cb, ab); vtx(bb, m, b.add(sb), cp, cb, ab);
        }
        BuiltBuffer built = bb.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
    }

    private static void vtx(BufferBuilder bb, Matrix4f m, Vec3d p, Vec3d cp, int c, float a) {
        bb.vertex(m, (float) (p.x - cp.x), (float) (p.y - cp.y), (float) (p.z - cp.z)).color(V.r(c), V.g(c), V.b(c), a);
    }

    private static void renderNumbers(MatrixStack ms, Camera cam) {
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();
        Vec3d cp = cam.getPos();
        TextRenderer tr = mc.textRenderer;
        VertexConsumerProvider.Immediate imm = mc.getBufferBuilders().getEntityVertexConsumers();
        String style = V.mode("DamageNumbers", "Styl", "Pop");
        float scale = V.num("DamageNumbers", "Skala", 1);
        boolean shadow = V.bo("DamageNumbers", "Cień");
        for (Num n : NUMS) {
            float age = (now - n.t()) / 1000f;
            float life = Math.min(1, age / 1.2f);
            float fade = life < 0.7f ? 1 : 1 - (life - 0.7f) / 0.3f;
            if (fade <= 0.03f) continue;
            float pop = style.equals("Pop") ? 1 + 0.6f * (float) Math.exp(-age * 9) * (float) Math.cos(age * 18) : 1;
            float rise = style.equals("Minimal") ? age * 0.25f : age * 0.75f - age * age * 0.25f;
            float drift = style.equals("Float") ? age : 0;
            ms.push();
            ms.translate(n.x() - cp.x + n.dx() * drift, n.y() - cp.y + rise, n.z() - cp.z + n.dz() * drift);
            ms.multiply(cam.getRotation());
            float s = 0.028f * scale * pop;
            ms.scale(s, -s, s);
            Text t = Fonts.text(n.text(), Fonts.Font.BOLD);
            float w = tr.getWidth(t);
            int a = Math.max(8, (int) (fade * 255));
            tr.draw(t, -w / 2f, 0, (a << 24) | (n.color() & 0xFFFFFF), shadow, ms.peek().getPositionMatrix(), imm, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            ms.pop();
        }
        imm.draw();
    }

    // ------------------------------------------------------------------ Atmosphere

    private static float[][] atm;
    private static String atmStyle = "";
    private static long atmLast;

    private static void renderAtmosphere(MatrixStack ms, Camera cam) {
        String st = V.mode("Atmosphere", "Styl", "Fireflies");
        int count = (int) V.num("Atmosphere", "Ilość", 80);
        float range = V.num("Atmosphere", "Zasięg", 16);
        Vec3d cp = cam.getPos();
        long now = System.nanoTime();
        float dt = atmLast == 0 ? 0 : Math.min(0.1f, (now - atmLast) / 1e9f);
        atmLast = now;
        if (atm == null || atm.length != count || !st.equals(atmStyle)) {
            atm = new float[count][];
            atmStyle = st;
            for (int i = 0; i < count; i++) atm[i] = spawnAtm(st, cp, range, true);
        }
        float time = (System.currentTimeMillis() % 1_000_000L) / 1000f;
        float size = V.num("Atmosphere", "Rozmiar", 1);
        String cm = V.mode("Atmosphere", "Kolor tryb", "Styl");
        int base = switch (st) { case "Snow" -> 0xFFFFFFFF; case "Stars" -> 0xFFE6ECFF; case "Sakura" -> 0xFFFFB3D4; case "Embers" -> 0xFFFF8A3C; default -> 0xFFD6FF6A; };
        Quaternionf rot = cam.getRotation();
        Vector3f right = rot.transform(new Vector3f(1, 0, 0)), up = rot.transform(new Vector3f(0, 1, 0));
        WorldFx.bind(DOT);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        Matrix4f m = ms.peek().getPositionMatrix();
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int i = 0; i < atm.length; i++) {
            float[] p = atm[i]; // x,y,z, vx,vy,vz, phase, age, life, size
            p[7] += dt;
            switch (st) {
                case "Fireflies" -> {
                    p[3] += (RND.nextFloat() - 0.5f) * dt * 1.6f; p[4] += (RND.nextFloat() - 0.5f) * dt * 0.8f; p[5] += (RND.nextFloat() - 0.5f) * dt * 1.6f;
                    float sp = (float) Math.sqrt(p[3] * p[3] + p[4] * p[4] + p[5] * p[5]);
                    if (sp > 0.7f) { p[3] *= 0.7f / sp; p[4] *= 0.7f / sp; p[5] *= 0.7f / sp; }
                }
                case "Snow", "Sakura" -> { p[3] = MathHelper.sin(time * 0.9f + p[6]) * (st.equals("Sakura") ? 0.6f : 0.25f); p[5] = MathHelper.cos(time * 0.7f + p[6]) * (st.equals("Sakura") ? 0.4f : 0.15f); }
                default -> {}
            }
            p[0] += p[3] * dt; p[1] += p[4] * dt; p[2] += p[5] * dt;
            double dx = p[0] - cp.x, dy = p[1] - cp.y, dz = p[2] - cp.z;
            if (dx * dx + dz * dz > range * range * 1.2 || dy < -range * 0.5 || dy > range * 0.9 || (p[8] > 0 && p[7] > p[8])) { atm[i] = spawnAtm(st, cp, range, false); continue; }
            float a = 1;
            if (p[8] > 0) a = Math.min(1, Math.min(p[7] / 1.2f, (p[8] - p[7]) / 1.2f));
            a *= switch (st) {
                case "Fireflies" -> 0.25f + 0.75f * (float) Math.pow(Math.max(0, MathHelper.sin(time * 1.7f + p[6])), 2);
                case "Stars" -> 0.45f + 0.55f * (0.5f + 0.5f * MathHelper.sin(time * 3.1f + p[6] * 4));
                case "Embers" -> 0.6f + 0.4f * MathHelper.sin(time * 9 + p[6] * 3);
                default -> 0.9f;
            };
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            a *= (float) MathHelper.clamp((dist - 0.8) / 1.5, 0, 1) * (float) MathHelper.clamp((range - Math.sqrt(dx * dx + dz * dz)) / 3, 0, 1);
            if (a < 0.02f) continue;
            int c = cm.equals("Rainbow") ? V.rainbow(1, i * 0.013f) : cm.equals("Static") ? V.color("Atmosphere", "Kolor", 0xFFFFFFFF) : base;
            float s = p[9] * size;
            float r = V.r(c) * a, g = V.g(c) * a, b = V.b(c) * a;
            float x = (float) dx, y = (float) dy, z = (float) dz;
            float rx = right.x * s, ry = right.y * s, rz = right.z * s, ux = up.x * s, uy = up.y * s, uz = up.z * s;
            bb.vertex(m, x - rx - ux, y - ry - uy, z - rz - uz).texture(0, 1).color(r, g, b, 1f);
            bb.vertex(m, x + rx - ux, y + ry - uy, z + rz - uz).texture(1, 1).color(r, g, b, 1f);
            bb.vertex(m, x + rx + ux, y + ry + uy, z + rz + uz).texture(1, 0).color(r, g, b, 1f);
            bb.vertex(m, x - rx + ux, y - ry + uy, z - rz + uz).texture(0, 0).color(r, g, b, 1f);
        }
        BuiltBuffer built = bb.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
        RenderSystem.defaultBlendFunc();
    }

    private static float[] spawnAtm(String st, Vec3d cp, float range, boolean init) {
        double a = RND.nextDouble() * Math.PI * 2, r = Math.sqrt(RND.nextDouble()) * range;
        float x = (float) (cp.x + Math.cos(a) * r), z = (float) (cp.z + Math.sin(a) * r);
        float y = switch (st) {
            case "Snow", "Sakura" -> (float) (cp.y + (init ? -range * 0.4 + RND.nextDouble() * range * 1.2 : range * 0.8));
            case "Embers" -> (float) (cp.y + (init ? -range * 0.4 + RND.nextDouble() * range : -range * 0.45));
            case "Stars" -> (float) (cp.y + RND.nextDouble() * range * 0.8);
            default -> (float) (cp.y - range * 0.3 + RND.nextDouble() * range * 0.8);
        };
        float vy = switch (st) { case "Snow" -> -0.8f - RND.nextFloat() * 0.6f; case "Sakura" -> -0.5f - RND.nextFloat() * 0.3f; case "Embers" -> 0.4f + RND.nextFloat() * 0.5f; case "Stars" -> 0.02f; default -> 0; };
        float life = switch (st) { case "Fireflies" -> 6 + RND.nextFloat() * 8; case "Stars" -> 4 + RND.nextFloat() * 6; case "Embers" -> 5 + RND.nextFloat() * 5; default -> 0; };
        float size = switch (st) { case "Snow" -> 0.1f + RND.nextFloat() * 0.08f; case "Sakura" -> 0.13f + RND.nextFloat() * 0.08f; case "Stars" -> 0.12f + RND.nextFloat() * 0.1f; case "Embers" -> 0.08f + RND.nextFloat() * 0.06f; default -> 0.16f + RND.nextFloat() * 0.1f; };
        return new float[]{x, y, z, 0, vy, 0, RND.nextFloat() * 6.28f, init && life > 0 ? RND.nextFloat() * life : 0, life, size};
    }

    // ------------------------------------------------------------------ BlockOutline

    private static Box shown;
    private static long outlineLast;

    private static boolean outline(WorldRenderContext ctx, WorldRenderContext.BlockOutlineContext bo) {
        if (!V.on("BlockOutline")) return true;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return true;
        VoxelShape shape = bo.blockState().getOutlineShape(mc.world, bo.blockPos(), ShapeContext.of(bo.entity()));
        if (shape.isEmpty()) return false;
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - outlineLast) / 1e9f);
        boolean fresh = now - outlineLast > 150_000_000L;
        outlineLast = now;
        Box target = shape.getBoundingBox().offset(bo.blockPos());
        boolean anim = V.bo("BlockOutline", "Animacja");
        if (shown == null || fresh || !anim) shown = target;
        else {
            float k = 1 - (float) Math.exp(-dt * 22);
            shown = new Box(MathHelper.lerp(k, shown.minX, target.minX), MathHelper.lerp(k, shown.minY, target.minY), MathHelper.lerp(k, shown.minZ, target.minZ),
                    MathHelper.lerp(k, shown.maxX, target.maxX), MathHelper.lerp(k, shown.maxY, target.maxY), MathHelper.lerp(k, shown.maxZ, target.maxZ));
        }
        boolean moving = anim && !shown.equals(target);
        MatrixStack ms = ctx.matrixStack() != null ? ctx.matrixStack() : new MatrixStack();
        ms.push();
        ms.scale(0.99975586f, 0.99975586f, 0.99975586f);
        double cx = bo.cameraX(), cy = bo.cameraY(), cz = bo.cameraZ();
        String style = V.mode("BlockOutline", "Styl", "Both");
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        try {
            List<Box> boxes = moving ? List.of(shown) : shape.getBoundingBoxes().stream().map(b -> b.offset(bo.blockPos())).toList();
            if (!style.equals("Outline")) fill(ms, boxes, cx, cy, cz);
            if (!style.equals("Fill")) {
                if (moving || boxes.size() > 6) edges(ms, List.of(moving ? shown : target), cx, cy, cz);
                else {
                    List<double[]> e = new ArrayList<>();
                    shape.forEachEdge((x1, y1, z1, x2, y2, z2) -> e.add(new double[]{x1 + bo.blockPos().getX(), y1 + bo.blockPos().getY(), z1 + bo.blockPos().getZ(), x2 + bo.blockPos().getX(), y2 + bo.blockPos().getY(), z2 + bo.blockPos().getZ()}));
                    lines(ms, e, cx, cy, cz);
                }
            }
        } finally {
            RenderSystem.lineWidth(1f);
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            ms.pop();
        }
        return false;
    }

    private static int outlineColor(String set, int def, double x, double y, double z) {
        int c = V.color("BlockOutline", set, def);
        if (!V.bo("BlockOutline", "Tęcza")) return c;
        return (V.rainbow(1.2f, (float) (x + y + z) * 0.08f) & 0xFFFFFF) | (c & 0xFF000000);
    }

    private static void fill(MatrixStack ms, List<Box> boxes, double cx, double cy, double cz) {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        Matrix4f m = ms.peek().getPositionMatrix();
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (Box b0 : boxes) {
            Box b = b0.expand(0.002);
            double[][] f = {
                    {b.minX, b.minY, b.minZ, b.maxX, b.minY, b.minZ, b.maxX, b.minY, b.maxZ, b.minX, b.minY, b.maxZ},
                    {b.minX, b.maxY, b.minZ, b.minX, b.maxY, b.maxZ, b.maxX, b.maxY, b.maxZ, b.maxX, b.maxY, b.minZ},
                    {b.minX, b.minY, b.minZ, b.minX, b.maxY, b.minZ, b.maxX, b.maxY, b.minZ, b.maxX, b.minY, b.minZ},
                    {b.minX, b.minY, b.maxZ, b.maxX, b.minY, b.maxZ, b.maxX, b.maxY, b.maxZ, b.minX, b.maxY, b.maxZ},
                    {b.minX, b.minY, b.minZ, b.minX, b.minY, b.maxZ, b.minX, b.maxY, b.maxZ, b.minX, b.maxY, b.minZ},
                    {b.maxX, b.minY, b.minZ, b.maxX, b.maxY, b.minZ, b.maxX, b.maxY, b.maxZ, b.maxX, b.minY, b.maxZ}};
            for (double[] q : f)
                for (int i = 0; i < 4; i++) {
                    int c = outlineColor("Wypełnienie", 0x22FFFFFF, q[i * 3], q[i * 3 + 1], q[i * 3 + 2]);
                    bb.vertex(m, (float) (q[i * 3] - cx), (float) (q[i * 3 + 1] - cy), (float) (q[i * 3 + 2] - cz)).color(V.r(c), V.g(c), V.b(c), V.a(c));
                }
        }
        BuiltBuffer built = bb.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
    }

    private static void edges(MatrixStack ms, List<Box> boxes, double cx, double cy, double cz) {
        List<double[]> e = new ArrayList<>();
        for (Box b : boxes) {
            double[] xs = {b.minX, b.maxX}, ys = {b.minY, b.maxY}, zs = {b.minZ, b.maxZ};
            for (double y : ys) for (double z : zs) e.add(new double[]{b.minX, y, z, b.maxX, y, z});
            for (double x : xs) for (double z : zs) e.add(new double[]{x, b.minY, z, x, b.maxY, z});
            for (double x : xs) for (double y : ys) e.add(new double[]{x, y, b.minZ, x, y, b.maxZ});
        }
        lines(ms, e, cx, cy, cz);
    }

    private static void lines(MatrixStack ms, List<double[]> edges, double cx, double cy, double cz) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float w = V.num("BlockOutline", "Grubość", 2) * Math.max(1f, mc.getWindow().getFramebufferWidth() / 1920f) * 1.25f;
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
        RenderSystem.lineWidth(w);
        MatrixStack.Entry entry = ms.peek();
        Matrix4f m = entry.getPositionMatrix();
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        for (double[] e : edges) {
            float nx = (float) (e[3] - e[0]), ny = (float) (e[4] - e[1]), nz = (float) (e[5] - e[2]);
            float len = MathHelper.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1e-5f) continue;
            nx /= len; ny /= len; nz /= len;
            int c1 = outlineColor("Kolor", 0xFFFFFFFF, e[0], e[1], e[2]), c2 = outlineColor("Kolor", 0xFFFFFFFF, e[3], e[4], e[5]);
            bb.vertex(m, (float) (e[0] - cx), (float) (e[1] - cy), (float) (e[2] - cz)).color(V.r(c1), V.g(c1), V.b(c1), V.a(c1)).normal(entry, nx, ny, nz);
            bb.vertex(m, (float) (e[3] - cx), (float) (e[4] - cy), (float) (e[5] - cz)).color(V.r(c2), V.g(c2), V.b(c2), V.a(c2)).normal(entry, nx, ny, nz);
        }
        BuiltBuffer built = bb.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
    }
}
