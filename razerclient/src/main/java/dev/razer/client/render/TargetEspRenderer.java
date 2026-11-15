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
            case "Orbit" -> drawOrbit(ms, cam, target, time, a, hurt, scale, m.trail.get());
            case "Helix" -> drawHelix(ms, cam, target, time, a, hurt, scale);
            case "Halo" -> drawHalo(ms, cam, target, time, a, hurt, scale);
            case "Sparkles" -> drawSparkles(ms, cam, target, time, a, hurt, scale);
            case "Brackets" -> markerFront(ms, cam, x, y, z, () -> drawBrackets(ms, cam, target, time, a, hurt, scale));
            case "Vortex" -> drawVortex(ms, cam, target, time, a, hurt, scale);
            case "Crystal" -> drawCrystal(ms, cam, target, time, a, hurt, scale);
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

    // ------------------------------------------------------------------ nowe tryby

    private static void dot(MatrixStack ms, Camera cam, float x, float y, float z, float size, int c) {
        ms.push();
        ms.translate(x, y, z);
        billboard(ms, cam, GLOW, size, 0, c);
        ms.pop();
    }

    /** Orbit – trzy pochylone pierścienie z krążącymi kulkami (jak atom) */
    private static void drawOrbit(MatrixStack ms, Camera cam, LivingEntity e, float time, float a, float hurt, float scale, boolean trail) {
        begin(true);
        float h = e.getHeight(), r = (e.getWidth() * 0.9f + 0.35f) * scale;
        ms.push();
        ms.translate(0, h * 0.55f, 0);
        for (int k = 0; k < 3; k++) {
            ms.push();
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(k * 120 + time * 25));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(62));
            Matrix4f mat = ms.peek().getPositionMatrix();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            int seg = 60;
            for (int i = 0; i <= seg; i++) {
                double an = Math.PI * 2 * i / seg;
                float cos = (float) Math.cos(an), sin = (float) Math.sin(an);
                int c = color((float) an + k * 2 + time, a * 0.55f, hurt);
                b.vertex(mat, cos * (r - 0.012f), 0, sin * (r - 0.012f)).color(c);
                b.vertex(mat, cos * (r + 0.012f), 0, sin * (r + 0.012f)).color(c);
            }
            BufferRenderer.drawWithGlobalProgram(b.end());
            int len = trail ? 10 : 1;
            for (int i = len - 1; i >= 0; i--) {
                float an = time * 3.2f + k * 2.1f - i * 0.09f;
                float kk = 1 - i / (float) len;
                dot(ms, cam, (float) Math.cos(an) * r, 0, (float) Math.sin(an) * r, (0.06f + 0.12f * kk) * scale, color(an, a * kk, hurt));
            }
            ms.pop();
        }
        // jądro
        dot(ms, cam, 0, 0, 0, 0.22f * scale * (1 + 0.1f * (float) Math.sin(time * 5)), color(time, a * 0.35f, hurt));
        ms.pop();
    }

    /** Helix – podwójna spirala płynąca w górę */
    private static void drawHelix(MatrixStack ms, Camera cam, LivingEntity e, float time, float a, float hurt, float scale) {
        begin(true);
        float h = e.getHeight() * 1.05f, r = (e.getWidth() * 0.75f + 0.15f) * scale;
        int n = 26;
        for (int strand = 0; strand < 2; strand++) {
            for (int i = 0; i < n; i++) {
                float t = i / (float) n;
                float an = t * (float) Math.PI * 4 + time * 2.8f + strand * (float) Math.PI;
                float pulse = 0.6f + 0.4f * (float) Math.sin(time * 4 - t * 10);
                dot(ms, cam, (float) Math.cos(an) * r, h * t, (float) Math.sin(an) * r, (0.05f + 0.07f * pulse) * scale,
                        color(t * 4 + strand * 1.5f + time, a * (0.45f + 0.55f * pulse), hurt));
            }
        }
        // szczeble DNA
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        Matrix4f mat = ms.peek().getPositionMatrix();
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < n; i += 3) {
            float t = i / (float) n, an = t * (float) Math.PI * 4 + time * 2.8f;
            int c = color(t * 4 + time, a * 0.35f, hurt);
            b.vertex(mat, (float) Math.cos(an) * r, h * t, (float) Math.sin(an) * r).color(c);
            b.vertex(mat, -(float) Math.cos(an) * r, h * t, -(float) Math.sin(an) * r).color(c);
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    /** Halo – świecąca aureola nad głową */
    private static void drawHalo(MatrixStack ms, Camera cam, LivingEntity e, float time, float a, float hurt, float scale) {
        begin(true);
        float r = 0.32f * scale, y = e.getHeight() + 0.22f + 0.04f * (float) Math.sin(time * 2.4f);
        ms.push();
        ms.translate(0, y, 0);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(8 * (float) Math.sin(time * 1.3f)));
        Matrix4f mat = ms.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        int seg = 56;
        float[][] bands = {{r - 0.09f, r - 0.025f, 0f, 0.5f}, {r - 0.025f, r + 0.025f, 1f, 1f}, {r + 0.025f, r + 0.1f, 0.5f, 0f}};
        for (float[] band : bands) {
            BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            for (int i = 0; i <= seg; i++) {
                double an = Math.PI * 2 * i / seg;
                float cos = (float) Math.cos(an), sin = (float) Math.sin(an);
                int c = color((float) an + time * 1.5f, a, hurt);
                b.vertex(mat, cos * band[0], 0, sin * band[0]).color(R2D.withAlpha(c, a * band[2]));
                b.vertex(mat, cos * band[1], 0, sin * band[1]).color(R2D.withAlpha(c, a * band[3]));
            }
            BufferRenderer.drawWithGlobalProgram(b.end());
        }
        for (int i = 0; i < 4; i++) {
            float an = time * 2 + i * (float) Math.PI / 2;
            dot(ms, cam, (float) Math.cos(an) * r, 0, (float) Math.sin(an) * r, 0.1f * scale, color(an, a, hurt));
        }
        dot(ms, cam, 0, 0, 0, 0.7f * scale, color(time, a * 0.18f, hurt));
        ms.pop();
    }

    private static final java.util.List<float[]> sparks = new java.util.ArrayList<>();
    private static long lastSpark = 0;
    private static final long SPARK_BASE = System.currentTimeMillis();

    /** Sparkles – iskierki unoszące się wokół celu */
    private static void drawSparkles(MatrixStack ms, Camera cam, LivingEntity e, float time, float a, float hurt, float scale) {
        begin(true);
        long now = System.currentTimeMillis();
        float h = e.getHeight(), r = (e.getWidth() * 0.8f + 0.2f) * scale;
        while (now - lastSpark > 16) {
            lastSpark = lastSpark == 0 || now - lastSpark > 500 ? now : lastSpark + 16;
            double an = Math.random() * Math.PI * 2;
            float rr = r * (0.6f + (float) Math.random() * 0.5f);
            sparks.add(new float[]{(float) Math.cos(an) * rr, (float) Math.random() * h, (float) Math.sin(an) * rr, now - SPARK_BASE, (float) Math.random() * 6.28f, 0.6f + (float) Math.random() * 0.8f});
        }
        sparks.removeIf(s -> now - SPARK_BASE - s[3] > 1200);
        if (sparks.size() > 140) sparks.subList(0, sparks.size() - 140).clear();
        for (float[] s : sparks) {
            float t = (now - SPARK_BASE - s[3]) / 1200f;
            float fade = (float) Math.sin(t * Math.PI);
            float tw = 0.6f + 0.4f * (float) Math.sin(time * 12 + s[4]);
            dot(ms, cam, s[0], s[1] + t * 0.6f, s[2], 0.11f * s[5] * scale * (0.6f + 0.4f * tw), color(s[4] + time, Math.min(1, a * fade * (0.7f + 0.6f * tw)), hurt));
        }
    }

    /** Brackets – narożniki celownika wokół sylwetki + pasek zdrowia */
    private static void drawBrackets(MatrixStack ms, Camera cam, LivingEntity e, float time, float a, float hurt, float scale) {
        begin(false);
        ms.push();
        ms.translate(0, e.getHeight() * 0.5f, 0);
        ms.multiply(cam.getRotation());
        float pulse = 1 + 0.05f * (float) Math.sin(time * 4) + hurt * 0.15f;
        float hw = (e.getWidth() * 0.8f + 0.2f) * scale * pulse, hh = e.getHeight() * 0.62f * scale * pulse;
        float len = Math.min(hw, hh) * 0.45f, th = 0.035f;
        Matrix4f mat = ms.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float[][] corners = {{-hw, -hh, 1, 1}, {hw, -hh, -1, 1}, {hw, hh, -1, -1}, {-hw, hh, 1, -1}};
        int ci = 0;
        for (float[] c : corners) {
            int col = color(time + ci++ * 1.5f, a, hurt);
            rectQ(b, mat, c[0], c[1], c[0] + c[2] * len, c[1] + c[3] * th, col);
            rectQ(b, mat, c[0], c[1], c[0] + c[2] * th, c[1] + c[3] * len, col);
        }
        // pasek zdrowia z boku
        float hp = Math.min(1, (e.getHealth() + e.getAbsorptionAmount()) / Math.max(1, e.getMaxHealth()));
        float bx = -hw - 0.12f;
        rectQ(b, mat, bx, -hh, bx + 0.05f, hh, R2D.withAlpha(0xFF000000, a * 0.5f));
        int hc = hp > 0.5f ? R2D.blend(0xFFFACC15, 0xFF4ADE80, (hp - 0.5f) * 2) : R2D.blend(0xFFEF4444, 0xFFFACC15, hp * 2);
        rectQ(b, mat, bx + 0.01f, -hh + 0.01f, bx + 0.04f, -hh + 0.01f + (2 * hh - 0.02f) * hp, R2D.withAlpha(hc, a));
        BufferRenderer.drawWithGlobalProgram(b.end());
        ms.pop();
    }

    private static void rectQ(BufferBuilder b, Matrix4f m, float x1, float y1, float x2, float y2, int c) {
        float ax = Math.min(x1, x2), bx = Math.max(x1, x2), ay = Math.min(y1, y2), by = Math.max(y1, y2);
        b.vertex(m, ax, ay, 0).color(c);
        b.vertex(m, ax, by, 0).color(c);
        b.vertex(m, bx, by, 0).color(c);
        b.vertex(m, bx, ay, 0).color(c);
    }

    /** Vortex – wir kropek kręcący się od stóp w górę */
    private static void drawVortex(MatrixStack ms, Camera cam, LivingEntity e, float time, float a, float hurt, float scale) {
        begin(true);
        float h = e.getHeight() * 1.15f, r = (e.getWidth() * 0.6f + 0.15f) * scale;
        int n = 44;
        for (int i = 0; i < n; i++) {
            float t = ((i / (float) n) + time * 0.25f) % 1f;
            float an = t * (float) Math.PI * 6 - time * 4 + i * 0.4f;
            float rr = r * (0.25f + 1.05f * t);
            float fade = (float) Math.sin(t * Math.PI);
            dot(ms, cam, (float) Math.cos(an) * rr, h * t, (float) Math.sin(an) * rr, (0.05f + 0.06f * t) * scale, color(an + time, a * fade, hurt));
        }
    }

    /** Crystal – obracający się kryształ nad głową */
    private static void drawCrystal(MatrixStack ms, Camera cam, LivingEntity e, float time, float a, float hurt, float scale) {
        begin(true);
        float s = 0.17f * scale, y = e.getHeight() + 0.42f + 0.06f * (float) Math.sin(time * 2.2f);
        ms.push();
        ms.translate(0, y, 0);
        dot(ms, cam, 0, 0, 0, 0.55f * scale, color(time, a * 0.25f, hurt));
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 80));
        Matrix4f mat = ms.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        float[][] ring = {{s, 0, 0}, {0, 0, s}, {-s, 0, 0}, {0, 0, -s}};
        float[] top = {0, s * 1.7f, 0}, bot = {0, -s * 1.7f, 0};
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < 4; i++) {
            float[] p1 = ring[i], p2 = ring[(i + 1) % 4];
            float shade = 0.45f + 0.35f * (i % 2);
            int c = color(time + i, a * shade, hurt), cTop = color(time + i + 1, a * (shade + 0.2f), hurt);
            b.vertex(mat, top[0], top[1], top[2]).color(cTop); b.vertex(mat, p1[0], p1[1], p1[2]).color(c); b.vertex(mat, p2[0], p2[1], p2[2]).color(c);
            b.vertex(mat, bot[0], bot[1], bot[2]).color(R2D.withAlpha(c, a * shade * 0.6f)); b.vertex(mat, p2[0], p2[1], p2[2]).color(c); b.vertex(mat, p1[0], p1[1], p1[2]).color(c);
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
        ms.pop();
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
