package dev.razer.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.razer.client.RazerClient;
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

/** Rysuje efekty Target ESP w świecie (pierścień ze smugą + obracający się znacznik). */
public final class TargetEspRenderer {
    private static final Identifier MARKER = Identifier.of("razerclient", "textures/ui/target_marker.png");
    private static LivingEntity target, attacked;
    private static long attackTime;
    private static final Anim appear = new Anim(0, 10);
    private static LivingEntity shown;

    private TargetEspRenderer() {}

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && entity instanceof LivingEntity le) { attacked = le; attackTime = System.currentTimeMillis(); }
            return ActionResult.PASS;
        });
        WorldRenderEvents.AFTER_ENTITIES.register(TargetEspRenderer::render);
    }

    private static TargetEsp module() {
        return (TargetEsp) RazerClient.get().modules.get("TargetESP");
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

    private static void render(WorldRenderContext ctx) {
        TargetEsp m = module();
        if (m == null || !m.isEnabled()) { shown = null; appear.snap(0); return; }
        LivingEntity t = pick(m);
        if (t != null) target = t;
        float a = appear.animate(t != null ? 1 : 0);
        if (target == null || a < 0.01f) return;
        if (!target.isAlive()) { target = null; return; }

        Camera cam = ctx.camera();
        float delta = ctx.tickCounter().getTickDelta(true);
        Vec3d cp = cam.getPos();
        double x = MathHelper.lerp(delta, target.prevX, target.getX()) - cp.x;
        double y = MathHelper.lerp(delta, target.prevY, target.getY()) - cp.y;
        double z = MathHelper.lerp(delta, target.prevZ, target.getZ()) - cp.z;
        float time = (System.currentTimeMillis() % 1_000_000L) / 1000f * m.speed.get().floatValue();
        float hurt = target.hurtTime > 0 ? target.hurtTime / 10f : 0;

        MatrixStack ms = ctx.matrixStack() != null ? ctx.matrixStack() : new MatrixStack();
        ms.push();
        ms.translate(x, y, z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        float scale = m.size.get().floatValue() * Anim.easeOutBack(a);
        if (m.ring()) drawRing(ms, target, time, a, hurt, scale, m.trail.get());
        if (m.marker()) {
            // przesunięcie znacznika w stronę kamery, żeby nie chował się w modelu (nadal z testem głębi)
            Vec3d toCam = new Vec3d(-x, -(y + target.getHeight() * 0.55), -z).normalize().multiply(target.getWidth() * 0.75);
            ms.push();
            ms.translate(toCam.x, toCam.y, toCam.z);
            drawMarker(ms, cam, target, time, a, hurt, scale);
            ms.pop();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ms.pop();
    }

    private static int color(float phase, float alpha, float hurt) {
        int c = Theme.accentPreset() == Theme.Accent.MONO ? R2D.blend(0xFFFFFFFF, 0xFFB8B8C0, 0.5f + 0.5f * (float) Math.sin(phase)) : Theme.accentWave(phase);
        c = R2D.blend(c, 0xFFFF3B3B, hurt);
        return R2D.withAlpha(c, alpha);
    }

    /** pierścień płynący w górę i w dół po sylwetce + zanikająca smuga */
    private static void drawRing(MatrixStack ms, LivingEntity e, float time, float a, float hurt, float scale, boolean trail) {
        float h = e.getHeight();
        float r = (e.getWidth() * 0.75f + 0.1f) * scale;
        float wave = (float) Math.sin(time * 2.2f);
        float ry = h * (0.5f + 0.47f * wave);
        float dir = (float) Math.cos(time * 2.2f); // kierunek ruchu – smuga ciągnie się za pierścieniem
        float trailLen = trail ? 0.45f * h * Math.abs(dir) + 0.05f : 0;
        int seg = 48;
        Matrix4f mat = ms.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        // smuga (pas od pierścienia w stronę, z której przyszedł)
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
        // sam pierścień (gruba wstęga)
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
        // delikatny cień na ziemi
        BufferBuilder g = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        int gc = color(time, 0.22f * a, hurt);
        g.vertex(mat, 0, 0.02f, 0).color(gc);
        for (int i = 0; i <= seg; i++) {
            double an = Math.PI * 2 * i / seg;
            g.vertex(mat, (float) Math.cos(an) * r * 1.1f, 0.02f, (float) Math.sin(an) * r * 1.1f).color(R2D.withAlpha(gc, 0));
        }
        BufferRenderer.drawWithGlobalProgram(g.end());
    }

    /** obracający się znacznik zwrócony do kamery */
    private static void drawMarker(MatrixStack ms, Camera cam, LivingEntity e, float time, float a, float hurt, float scale) {
        ms.push();
        ms.translate(0, e.getHeight() * 0.55f, 0);
        ms.multiply(cam.getRotation());
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(time * 90f));
        float pulse = 1 + 0.06f * (float) Math.sin(time * 4) + hurt * 0.25f;
        float s = e.getHeight() * 0.55f * scale * pulse;
        ms.scale(s, s, s);
        Matrix4f mat = ms.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, MARKER);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        int c1 = color(time, a, hurt), c2 = color(time + 1.5f, a, hurt), c3 = color(time + 3f, a, hurt), c4 = color(time + 4.5f, a, hurt);
        b.vertex(mat, -1, -1, 0).texture(0, 1).color(c1);
        b.vertex(mat, 1, -1, 0).texture(1, 1).color(c2);
        b.vertex(mat, 1, 1, 0).texture(1, 0).color(c3);
        b.vertex(mat, -1, 1, 0).texture(0, 0).color(c4);
        BufferRenderer.drawWithGlobalProgram(b.end());
        ms.pop();
    }
}
