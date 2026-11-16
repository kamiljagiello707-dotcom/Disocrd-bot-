package dev.vesc.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.vesc.client.VescClient;
import dev.vesc.client.module.cosmetics.ChinaHat;
import dev.vesc.client.module.cosmetics.JumpCircles;
import dev.vesc.client.ui.Anim;
import dev.vesc.client.ui.Theme;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Kosmetyka w świecie: ChinaHat i JumpCircles (z testem głębi – nic przez ściany). */
public final class CosmeticsRenderer {
    private record Circle(double x, double y, double z, long start) {}
    private static final List<Circle> circles = new ArrayList<>();
    private static boolean wasOnGround = true;

    private CosmeticsRenderer() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) { circles.clear(); return; }
            boolean on = mc.player.isOnGround();
            JumpCircles jc = (JumpCircles) VescClient.get().modules.get("JumpCircles");
            if (jc != null && jc.isEnabled() && wasOnGround && !on && mc.player.getVelocity().y > 0.2)
                circles.add(new Circle(mc.player.getX(), mc.player.getY(), mc.player.getZ(), System.currentTimeMillis()));
            wasOnGround = on;
            if (circles.size() > 20) circles.remove(0);
        });
        WorldRenderEvents.AFTER_ENTITIES.register(CosmeticsRenderer::render);
    }

    private static Integer override;

    private static int color(float phase, float alpha) {
        if (override != null) return R2D.withAlpha(R2D.blend(override, 0xFFFFFFFF, 0.18f * (0.5f + 0.5f * (float) Math.sin(phase))), alpha);
        int c = Theme.accentPreset() == Theme.Accent.MONO ? R2D.blend(0xFFFFFFFF, 0xFFB8B8C0, 0.5f + 0.5f * (float) Math.sin(phase)) : Theme.accentWave(phase);
        return R2D.withAlpha(c, alpha);
    }

    private static void begin(boolean additive) {
        RenderSystem.enableBlend();
        if (additive) RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        else RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
    }

    private static void end() {
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        VescClient rc = VescClient.get();
        MatrixStack ms = ctx.matrixStack() != null ? ctx.matrixStack() : new MatrixStack();
        Vec3d cp = ctx.camera().getPos();
        float delta = ctx.tickCounter().getTickDelta(true);
        float time = (System.currentTimeMillis() % 1_000_000L) / 1000f;

        // ---------------- JumpCircles
        JumpCircles jc = (JumpCircles) rc.modules.get("JumpCircles");
        if (jc != null && jc.isEnabled() && !circles.isEmpty()) {
            override = jc.custom();
            begin(true);
            long now = System.currentTimeMillis();
            float life = jc.time.get().floatValue() * 1000f;
            circles.removeIf(c -> now - c.start > life);
            for (Circle c : circles) {
                float t = (now - c.start) / life;
                float r = 0.15f + jc.size.get().floatValue() * Anim.easeOutCubic(t);
                float a = (1 - t) * (1 - t);
                ms.push();
                ms.translate(c.x - cp.x, c.y - cp.y + 0.03, c.z - cp.z);
                Matrix4f m = ms.peek().getPositionMatrix();
                int seg = 56;
                float[][] bands = {{r - 0.18f, r - 0.03f, 0f, 0.4f}, {r - 0.03f, r + 0.03f, 1f, 1f}, {r + 0.03f, r + 0.22f, 0.5f, 0f}};
                for (float[] b : bands) {
                    BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
                    for (int i = 0; i <= seg; i++) {
                        double an = Math.PI * 2 * i / seg;
                        float cos = (float) Math.cos(an), sin = (float) Math.sin(an);
                        int col = color((float) an + time * 2, 1);
                        bb.vertex(m, cos * Math.max(0, b[0]), 0, sin * Math.max(0, b[0])).color(R2D.withAlpha(col, a * b[2]));
                        bb.vertex(m, cos * b[1], 0, sin * b[1]).color(R2D.withAlpha(col, a * b[3]));
                    }
                    BufferRenderer.drawWithGlobalProgram(bb.end());
                }
                ms.pop();
            }
            end();
        }

        // ---------------- ChinaHat
        ChinaHat hat = (ChinaHat) rc.modules.get("ChinaHat");
        if (hat != null && hat.isEnabled()) {
            override = hat.custom();
            for (PlayerEntity p : mc.world.getPlayers()) {
                boolean self = p == mc.player;
                if (self && mc.options.getPerspective().isFirstPerson()) continue;
                if (!self && !(hat.friends.get() && rc.friends.is(p.getName().getString()))) continue;
                if (p.isInvisible()) continue;
                double x = MathHelper.lerp(delta, p.prevX, p.getX()) - cp.x;
                double y = MathHelper.lerp(delta, p.prevY, p.getY()) - cp.y;
                double z = MathHelper.lerp(delta, p.prevZ, p.getZ()) - cp.z;
                drawHat(ms, x, y + p.getHeight() + (p.isSneaking() ? -0.02 : 0.03), z, hat, time);
            }
        }
    }

    private static void drawHat(MatrixStack ms, double x, double y, double z, ChinaHat hat, float time) {
        float r = hat.size.get().floatValue(), h = hat.height.get().floatValue();
        float spin = hat.spin.get() ? time * 1.2f : 0;
        int seg = 48;
        ms.push();
        ms.translate(x, y, z);
        Matrix4f m = ms.peek().getPositionMatrix();
        begin(false);
        // stożek
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        b.vertex(m, 0, h, 0).color(color(spin, 0.75f));
        for (int i = 0; i <= seg; i++) {
            double an = Math.PI * 2 * i / seg;
            b.vertex(m, (float) Math.cos(an) * r, 0, (float) Math.sin(an) * r).color(color((float) an + spin, 0.38f));
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
        end();
        // świecąca krawędź
        begin(true);
        BufferBuilder e = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= seg; i++) {
            double an = Math.PI * 2 * i / seg;
            float cos = (float) Math.cos(an), sin = (float) Math.sin(an);
            int c = color((float) an + spin, 1);
            e.vertex(m, cos * (r - 0.03f), 0.01f, sin * (r - 0.03f)).color(R2D.withAlpha(c, 0.9f));
            e.vertex(m, cos * (r + 0.05f), -0.005f, sin * (r + 0.05f)).color(R2D.withAlpha(c, 0f));
        }
        BufferRenderer.drawWithGlobalProgram(e.end());
        end();
        ms.pop();
    }
}
