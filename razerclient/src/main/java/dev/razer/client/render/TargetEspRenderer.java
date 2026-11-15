package dev.razer.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.razer.client.RazerClient;
import dev.razer.client.module.Ambient;
import dev.razer.client.module.TargetEsp;
import dev.razer.client.ui.Anim;
import dev.razer.client.ui.Theme;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Efekty wizualne w świecie: Target ESP (Mix, Duchy, Pulse, Krąg, Pierścień, Znacznik) oraz Ambient.
 * Wszystko z testem głębi – nic nie jest widoczne przez ściany.
 */
public final class TargetEspRenderer {
    private static final Identifier MARKER = Identifier.of("razerclient", "textures/ui/target_marker.png");
    private static final Identifier GLOW = Identifier.of("razerclient", "textures/ui/glow.png");
    private static final Identifier PULSE = Identifier.of("razerclient", "textures/ui/pulse.png");
    private static final Identifier GLOW_ADD = Identifier.of("razerclient", "textures/ui/glow_add.png");
    private static final Identifier PULSE_ADD = Identifier.of("razerclient", "textures/ui/pulse_add.png");
    private static LivingEntity target, attacked;
    private static long attackTime;
    private static final Anim appear = new Anim(0, 10);

    private TargetEspRenderer() {}

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && entity instanceof LivingEntity le) { attacked = le; attackTime = System.currentTimeMillis(); }
            if (world.isClient && entity instanceof LivingEntity le && le.isAlive()
                    && RazerClient.get().modules.get("HitSound") instanceof dev.razer.client.module.HitSound hs && hs.isEnabled()
                    && (!hs.playersOnly.get() || entity instanceof PlayerEntity)) hs.play();
            return ActionResult.PASS;
        });
        WorldRenderEvents.AFTER_ENTITIES.register(TargetEspRenderer::render);
    }

    private static boolean valid(LivingEntity e, TargetEsp m) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return e != null && e.isAlive() && e != mc.player && !e.isInvisible() && (!m.playersOnly.get() || e instanceof PlayerEntity)
                && mc.player != null && e.squaredDistanceTo(mc.player) < 64 * 64;
    }

    private static LivingEntity pick(TargetEsp m) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (valid(attacked, m) && System.currentTimeMillis() - attackTime < 4000) return attacked;
        Entity e = mc.targetedEntity;
        if (e instanceof LivingEntity le && valid(le, m)) return le;
        return null;
    }

    // ------------------------------------------------------------------ stan GL

    private static void begin(boolean additive) {
        RenderSystem.enableBlend();
        if (additive) RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        else RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
    }

    private static void end() {
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void render(WorldRenderContext ctx) {
        RazerClient rc = RazerClient.get();
        Camera cam = ctx.camera();
        MatrixStack ms = ctx.matrixStack() != null ? ctx.matrixStack() : new MatrixStack();
        float delta = ctx.tickCounter().getTickDelta(true);

        Ambient amb = (Ambient) rc.modules.get("Ambient");
        if (amb != null && amb.isEnabled()) renderAmbient(ms, cam, amb);

        TargetEsp m = (TargetEsp) rc.modules.get("TargetESP");
        if (m == null || !m.isEnabled()) { appear.snap(0); return; }
        LivingEntity t = pick(m);
        if (t != null) target = t;
        float a = appear.animate(t != null ? 1 : 0);
        if (target == null || a < 0.01f) return;
        if (!target.isAlive()) { target = null; return; }

        Vec3d cp = cam.getPos();
        double x = MathHelper.lerp(delta, target.prevX, target.getX()) - cp.x;
        double y = MathHelper.lerp(delta, target.prevY, target.getY()) - cp.y;
        double z = MathHelper.lerp(delta, target.prevZ, target.getZ()) - cp.z;
        float time = (System.currentTimeMillis() % 1_000_000L) / 1000f * m.speed.get().floatValue();
        float hurt = target.hurtTime > 0 ? target.hurtTime / 10f : 0;
        float scale = m.size.get().floatValue() * Anim.easeOutBack(a);

        ms.push();
        ms.translate(x, y, z);
        switch (m.mode.get()) {
            case "Ring" -> drawRing(ms, target, time, a, hurt, scale, m.trail.get());
            case "Marker" -> markerFront(ms, cam, x, y, z, () -> drawMarker(ms, cam, target, time, a, hurt, scale));
            case "Circle" -> drawGroundCircle(ms, target, time, a, hurt, scale);
            case "Pulse" -> markerFront(ms, cam, x, y, z, () -> drawPulse(ms, cam, target, time, a, hurt, scale));
            case "Ghosts" -> drawGhosts(ms, cam, target, time, a, hurt, scale, m.ghosts.get().intValue(), m.trail.get());
            default -> {
                drawRing(ms, target, time, a, hurt, scale, m.trail.get());
                markerFront(ms, cam, x, y, z, () -> drawMarker(ms, cam, target, time, a, hurt, scale));
            }
        }
        ms.pop();
        end();
    }

    /** przesuwa znacznik lekko w stronę kamery, żeby nie chował się w modelu */
    private static void markerFront(MatrixStack ms, Camera cam, double x, double y, double z, Runnable r) {
        Vec3d toCam = new Vec3d(-x, -(y + target.getHeight() * 0.55), -z).normalize().multiply(target.getWidth() * 0.75);
        ms.push();
        ms.translate(toCam.x, toCam.y, toCam.z);
        r.run();
        ms.pop();
    }

    private static int color(float phase, float alpha, float hurt) {
        int c = Theme.accentPreset() == Theme.Accent.MONO ? R2D.blend(0xFFFFFFFF, 0xFFB8B8C0, 0.5f + 0.5f * (float) Math.sin(phase)) : Theme.accentWave(phase);
        c = R2D.blend(c, 0xFFFF3B3B, hurt);
        return R2D.withAlpha(c, alpha);
    }

    /** kwadrat zwrócony do kamery w aktualnym punkcie macierzy */
    private static void billboard(MatrixStack ms, Camera cam, Identifier tex, float size, float rot, int c) {
        // miękkie tekstury rysujemy addytywnie (ONE, ONE) z jasnością w RGB – shader MC odrzuca piksele z alfą < 0.1
        boolean soft = tex == GLOW || tex == PULSE;
        if (soft) {
            tex = tex == GLOW ? GLOW_ADD : PULSE_ADD;
            float a = ((c >>> 24) & 0xFF) / 255f;
            c = 0xFF000000 | ((int) (((c >> 16) & 0xFF) * a) << 16) | ((int) (((c >> 8) & 0xFF) * a) << 8) | (int) ((c & 0xFF) * a);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
        }
        ms.push();
        ms.multiply(cam.getRotation());
        if (rot != 0) ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rot));
        ms.scale(size, size, size);
        Matrix4f mat = ms.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, tex);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        b.vertex(mat, -1, -1, 0).texture(0, 1).color(c);
        b.vertex(mat, 1, -1, 0).texture(1, 1).color(c);
        b.vertex(mat, 1, 1, 0).texture(1, 0).color(c);
        b.vertex(mat, -1, 1, 0).texture(0, 0).color(c);
        BufferRenderer.drawWithGlobalProgram(b.end());
        ms.pop();
        if (soft) RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
    }

    // ------------------------------------------------------------------ Pierścień

    private static void drawRing(MatrixStack ms, LivingEntity e, float time, float a, float hurt, float scale, boolean trail) {
        begin(false);
        float h = e.getHeight();
        float r = (e.getWidth() * 0.75f + 0.1f) * scale;
        float wave = (float) Math.sin(time * 2.2f);
        float ry = h * (0.5f + 0.47f * wave);
        float dir = (float) Math.cos(time * 2.2f);
        float trailLen = trail ? 0.45f * h * Math.abs(dir) + 0.05f : 0;
        int seg = 48;
        Matrix4f mat = ms.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        if (trailLen > 0) {
            BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            float ty = ry - Math.signum(dir) * trailLen;
            for (int i = 0; i <= seg; i++) {
                double an = Math.PI * 2 * i / seg;
                float cx = (float) Math.cos(an) * r, cz = (float) Math.sin(an) * r;
                int c = color((float) an + time, 0.55f * a, hurt);
                b.vertex(mat, cx, ry, cz).color(c);
                b.vertex(mat, cx, ty, cz).color(R2D.withAlpha(c, 0));
            }
            BufferRenderer.drawWithGlobalProgram(b.end());
        }
        float th = 0.035f;
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= seg; i++) {
            double an = Math.PI * 2 * i / seg;
            float cx = (float) Math.cos(an) * r, cz = (float) Math.sin(an) * r;
            int c = color((float) an + time, a, hurt);
            b.vertex(mat, cx, ry - th, cz).color(c);
            b.vertex(mat, cx, ry + th, cz).color(c);
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    // ------------------------------------------------------------------ Znacznik

    private static void drawMarker(MatrixStack ms, Camera cam, LivingEntity e, float time, float a, float hurt, float scale) {
        begin(false);
        ms.push();
        ms.translate(0, e.getHeight() * 0.55f, 0);
        float pulse = 1 + 0.06f * (float) Math.sin(time * 4) + hurt * 0.25f;
        billboard(ms, cam, MARKER, e.getHeight() * 0.55f * scale * pulse, time * 90f, color(time, a, hurt));
        ms.pop();
    }

    // ------------------------------------------------------------------ Krąg (pod nogami)

    private static void drawGroundCircle(MatrixStack ms, LivingEntity e, float time, float a, float hurt, float scale) {
        begin(true);
        float r = (e.getWidth() * 0.85f + 0.15f) * scale * (1 + 0.04f * (float) Math.sin(time * 3));
        float yy = 0.03f;
        int seg = 64;
        Matrix4f mat = ms.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        float[][] bands = { // promień wewn., promień zewn., alfa wewn., alfa zewn.
                {r - 0.22f, r - 0.05f, 0f, 0.35f},
                {r - 0.05f, r + 0.05f, 1f, 1f},
                {r + 0.05f, r + 0.32f, 0.45f, 0f},
        };
        for (float[] band : bands) {
            BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            for (int i = 0; i <= seg; i++) {
                double an = Math.PI * 2 * i / seg;
                float cos = (float) Math.cos(an), sin = (float) Math.sin(an);
                int c = color((float) an + time * 2, a, hurt);
                b.vertex(mat, cos * band[0], yy, sin * band[0]).color(R2D.withAlpha(c, a * band[2]));
                b.vertex(mat, cos * band[1], yy, sin * band[1]).color(R2D.withAlpha(c, a * band[3]));
            }
            BufferRenderer.drawWithGlobalProgram(b.end());
        }
        // fala rozchodząca się od środka
        float wt = (time * 0.8f) % 1f;
        float wr = r * (0.3f + 0.9f * wt);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= seg; i++) {
            double an = Math.PI * 2 * i / seg;
            float cos = (float) Math.cos(an), sin = (float) Math.sin(an);
            int c = color((float) an, a * 0.5f * (1 - wt), hurt);
            b.vertex(mat, cos * wr, yy, sin * wr).color(c);
            b.vertex(mat, cos * (wr + 0.06f), yy, sin * (wr + 0.06f)).color(R2D.withAlpha(c, 0));
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    // ------------------------------------------------------------------ Pulse

    private static void drawPulse(MatrixStack ms, Camera cam, LivingEntity e, float time, float a, float hurt, float scale) {
        begin(true);
        ms.push();
        ms.translate(0, e.getHeight() * 0.55f, 0);
        float beat = (float) Math.pow(Math.abs(Math.sin(time * 2.4f)), 3);
        float s = e.getHeight() * 0.22f * scale * (0.85f + 0.25f * beat + hurt * 0.3f);
        billboard(ms, cam, GLOW, s * 2.2f, 0, color(time, a * (0.18f + 0.22f * beat), hurt));
        billboard(ms, cam, PULSE, s, time * 40f, color(time, a, hurt));
        // pierścień rozchodzący się z każdym "uderzeniem"
        float wt = (time * 2.4f / (float) Math.PI) % 1f;
        billboard(ms, cam, PULSE, s * (1 + wt * 0.9f), time * 40f, color(time + 1, a * 0.5f * (1 - wt), hurt));
        ms.pop();
    }

    // ------------------------------------------------------------------ Duchy (komety)

    private static void drawGhosts(MatrixStack ms, Camera cam, LivingEntity e, float time, float a, float hurt, float scale, int count, boolean trail) {
        begin(true);
        float h = e.getHeight();
        float r = (e.getWidth() * 0.9f + 0.25f) * scale;
        int len = trail ? 18 : 1;
        for (int g = 0; g < count; g++) {
            float off = (float) (Math.PI * 2 * g / count);
            for (int i = len - 1; i >= 0; i--) {
                float tt = time * 2.6f - i * 0.055f;
                float an = tt + off;
                float px = (float) Math.cos(an) * r, pz = (float) Math.sin(an) * r;
                float py = h * (0.5f + 0.32f * (float) Math.sin(tt * 1.3f + off * 1.7f));
                float k = 1 - i / (float) len;
                ms.push();
                ms.translate(px, py, pz);
                billboard(ms, cam, GLOW, (0.09f + 0.24f * k) * scale, 0, color(an, a * k * k, hurt));
                ms.pop();
            }
        }
    }

    // ------------------------------------------------------------------ Ambient

    private static float[][] orbs = new float[0][];
    private static final Random RNG = new Random();
    private static long lastAmbient = System.nanoTime();

    private static void respawn(float[] o, Vec3d c, float range, boolean anywhere) {
        o[0] = (float) (c.x + (RNG.nextFloat() * 2 - 1) * range);
        o[1] = (float) (c.y + (anywhere ? (RNG.nextFloat() * 2 - 1) * range * 0.6f : -range * 0.4f));
        o[2] = (float) (c.z + (RNG.nextFloat() * 2 - 1) * range);
        o[3] = 0.15f + RNG.nextFloat() * 0.35f;   // prędkość w górę
        o[4] = RNG.nextFloat() * 6.28f;           // faza
        o[5] = 0;                                 // wiek
        o[6] = 6 + RNG.nextFloat() * 8;           // życie
        o[7] = 0.6f + RNG.nextFloat() * 0.8f;     // rozmiar
    }

    private static void renderAmbient(MatrixStack ms, Camera cam, Ambient m) {
        Vec3d cp = cam.getPos();
        int n = m.count.get().intValue();
        float range = m.range.get().floatValue();
        if (orbs.length != n) {
            orbs = new float[n][8];
            for (float[] o : orbs) { respawn(o, cp, range, true); o[5] = RNG.nextFloat() * o[6]; }
        }
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastAmbient) / 1e9f);
        lastAmbient = now;
        begin(true);
        float time = (System.currentTimeMillis() % 1_000_000L) / 1000f;
        for (float[] o : orbs) {
            o[5] += dt;
            o[1] += o[3] * dt;
            o[0] += (float) Math.sin(time * 0.6f + o[4]) * 0.15f * dt;
            double dx = o[0] - cp.x, dy = o[1] - cp.y, dz = o[2] - cp.z;
            if (o[5] > o[6] || dx * dx + dz * dz > range * range * 1.6 || Math.abs(dy) > range) respawn(o, cp, range, false);
            float life = o[5] / o[6];
            float fade = Math.min(1, Math.min(life * 4, (1 - life) * 4));
            float tw = 0.7f + 0.3f * (float) Math.sin(time * 2 + o[4]);
            ms.push();
            ms.translate(o[0] - cp.x, o[1] - cp.y, o[2] - cp.z);
            billboard(ms, cam, GLOW, 0.35f * o[7] * m.size.get().floatValue(), 0, color(o[4], 0.8f * fade * tw, 0));
            ms.pop();
        }
        end();
    }
}
