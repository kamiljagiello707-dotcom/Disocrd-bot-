package dev.with.client.visual;

import dev.with.client.render.R2D;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import java.util.Map;

/**
 * Kosmetyki 3D przyczepione do modelu Twojej postaci (widać je w F5 i w ekwipunku):
 * Hat (China hat, Crown, Top hat), Halo i Wings (Angel, Dragon, Mono). Oświetlone jak model gracza.
 */
public class CosmeticFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static final Identifier WHITE = R2D.id("textures/fx/white.png");
    private static final Identifier RING = R2D.id("textures/fx/jc_ring.png"), GLOW = R2D.id("textures/fx/jc_glow.png");
    private static final Map<String, Identifier> WINGS = Map.of(
            "Angel", R2D.id("textures/fx/wing_angel.png"), "Dragon", R2D.id("textures/fx/wing_dragon.png"), "Mono", R2D.id("textures/fx/wing_mono.png"));

    public CosmeticFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) { super(ctx); }

    public static void register() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof PlayerEntityRenderer pr) helper.register(new CosmeticFeature(pr));
        });
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity p, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (p != MinecraftClient.getInstance().player || p.isInvisible()) return;
        float time = (p.age + tickDelta) / 20f;
        if (V.on("Wings") && !p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) wings(ms, vcp, light, time);
        if (V.on("Hat")) hat(ms, vcp, light, time);
        if (V.on("Halo")) halo(ms, vcp, time);
    }

    // ------------------------------------------------------------------ pomocnicze

    private static void v(VertexConsumer vc, MatrixStack.Entry e, float x, float y, float z, int c, float a, float u, float t, int light, float nx, float ny, float nz) {
        vc.vertex(e, x, y, z).color(V.r(c), V.g(c), V.b(c), a).texture(u, t).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e, nx, ny, nz);
    }

    private static int hatColor(float offset) {
        return V.bo("Hat", "Tęcza") ? V.rainbow(1.2f, offset) : V.color("Hat", "Kolor", 0xFFFFFFFF);
    }

    // ------------------------------------------------------------------ Hat

    private void hat(MatrixStack ms, VertexConsumerProvider vcp, int light, float time) {
        ms.push();
        getContextModel().head.rotate(ms);
        float s = V.num("Hat", "Rozmiar", 1);
        ms.translate(0, -0.5f, 0);
        ms.scale(s, s, s);
        if (V.bo("Hat", "Obrót")) ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 40));
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityTranslucent(WHITE));
        MatrixStack.Entry e = ms.peek();
        switch (V.mode("Hat", "Styl", "China hat")) {
            case "Crown" -> crown(vc, e, light);
            case "Top hat" -> topHat(vc, e, light);
            default -> chinaHat(vc, e, light);
        }
        ms.pop();
    }

    private static void chinaHat(VertexConsumer vc, MatrixStack.Entry e, int light) {
        int seg = 40;
        float r = 0.62f, base = 0.07f, apex = -0.34f;
        for (int i = 0; i < seg; i++) {
            float a0 = i * MathHelper.TAU / seg, a1 = (i + 1) * MathHelper.TAU / seg;
            float x0 = MathHelper.cos(a0) * r, z0 = MathHelper.sin(a0) * r, x1 = MathHelper.cos(a1) * r, z1 = MathHelper.sin(a1) * r;
            int c0 = hatColor(i / (float) seg), c1 = hatColor((i + 1) / (float) seg), ca = hatColor(0.5f);
            float nx = MathHelper.cos((a0 + a1) / 2) * 0.5f, nz = MathHelper.sin((a0 + a1) / 2) * 0.5f;
            v(vc, e, 0, apex, 0, ca, 0.75f, 0.5f, 0.5f, light, nx, -0.85f, nz);
            v(vc, e, 0, apex, 0, ca, 0.75f, 0.5f, 0.5f, light, nx, -0.85f, nz);
            v(vc, e, x1, base, z1, c1, 0.38f, 1, 1, light, nx, -0.85f, nz);
            v(vc, e, x0, base, z0, c0, 0.38f, 0, 1, light, nx, -0.85f, nz);
            // jasny brzeg
            v(vc, e, x0, base, z0, c0, 0.95f, 0, 0, light, nx, 0, nz);
            v(vc, e, x1, base, z1, c1, 0.95f, 1, 0, light, nx, 0, nz);
            v(vc, e, x1, base + 0.022f, z1, c1, 0.95f, 1, 1, light, nx, 0, nz);
            v(vc, e, x0, base + 0.022f, z0, c0, 0.95f, 0, 1, light, nx, 0, nz);
        }
    }

    private static void crown(VertexConsumer vc, MatrixStack.Entry e, int light) {
        int seg = 8;
        float r = 0.33f, y0 = 0.04f, y1 = -0.08f, tip = -0.24f;
        for (int i = 0; i < seg; i++) {
            float a0 = i * MathHelper.TAU / seg + MathHelper.PI / 8, a1 = (i + 1) * MathHelper.TAU / seg + MathHelper.PI / 8;
            float x0 = MathHelper.cos(a0) * r, z0 = MathHelper.sin(a0) * r, x1 = MathHelper.cos(a1) * r, z1 = MathHelper.sin(a1) * r;
            float nx = MathHelper.cos((a0 + a1) / 2), nz = MathHelper.sin((a0 + a1) / 2);
            int c = hatColor(i / (float) seg);
            // obręcz
            v(vc, e, x0, y0, z0, c, 1, 0, 1, light, nx, 0, nz);
            v(vc, e, x1, y0, z1, c, 1, 1, 1, light, nx, 0, nz);
            v(vc, e, x1, y1, z1, c, 1, 1, 0, light, nx, 0, nz);
            v(vc, e, x0, y1, z0, c, 1, 0, 0, light, nx, 0, nz);
            // ząb korony
            float mx = (x0 + x1) / 2 * 1.04f, mz = (z0 + z1) / 2 * 1.04f;
            v(vc, e, x0, y1, z0, c, 1, 0, 1, light, nx, 0, nz);
            v(vc, e, x1, y1, z1, c, 1, 1, 1, light, nx, 0, nz);
            v(vc, e, mx, tip, mz, c, 1, 0.5f, 0, light, nx, 0, nz);
            v(vc, e, mx, tip, mz, c, 1, 0.5f, 0, light, nx, 0, nz);
            // klejnot
            int g = 0xFF000000 | (i % 2 == 0 ? 0x7FA8FF : 0xFF7AA8);
            float gx = (x0 + x1) / 2 * 1.06f, gz = (z0 + z1) / 2 * 1.06f, tx = -nz * 0.035f, tz = nx * 0.035f, gy = (y0 + y1) / 2;
            v(vc, e, gx - tx, gy + 0.035f, gz - tz, g, 1, 0, 1, LightmapTextureManager.MAX_LIGHT_COORDINATE, nx, 0, nz);
            v(vc, e, gx + tx, gy + 0.035f, gz + tz, g, 1, 1, 1, LightmapTextureManager.MAX_LIGHT_COORDINATE, nx, 0, nz);
            v(vc, e, gx + tx, gy - 0.035f, gz + tz, g, 1, 1, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE, nx, 0, nz);
            v(vc, e, gx - tx, gy - 0.035f, gz - tz, g, 1, 0, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE, nx, 0, nz);
        }
    }

    private static void topHat(VertexConsumer vc, MatrixStack.Entry e, int light) {
        int seg = 28, body = 0xFF18181C, band = hatColor(0);
        float r = 0.25f, brim = 0.42f, y0 = 0.02f, top = -0.46f;
        for (int i = 0; i < seg; i++) {
            float a0 = i * MathHelper.TAU / seg, a1 = (i + 1) * MathHelper.TAU / seg;
            float c0 = MathHelper.cos(a0), s0 = MathHelper.sin(a0), c1 = MathHelper.cos(a1), s1 = MathHelper.sin(a1);
            float nx = MathHelper.cos((a0 + a1) / 2), nz = MathHelper.sin((a0 + a1) / 2);
            // rondo
            v(vc, e, c0 * r, y0, s0 * r, body, 1, 0, 0, light, 0, -1, 0);
            v(vc, e, c1 * r, y0, s1 * r, body, 1, 1, 0, light, 0, -1, 0);
            v(vc, e, c1 * brim, y0, s1 * brim, body, 1, 1, 1, light, 0, -1, 0);
            v(vc, e, c0 * brim, y0, s0 * brim, body, 1, 0, 1, light, 0, -1, 0);
            // ściana (z kolorową wstążką na dole)
            float yb = y0 - 0.08f;
            v(vc, e, c0 * r, y0, s0 * r, band, 1, 0, 1, light, nx, 0, nz);
            v(vc, e, c1 * r, y0, s1 * r, band, 1, 1, 1, light, nx, 0, nz);
            v(vc, e, c1 * r, yb, s1 * r, band, 1, 1, 0, light, nx, 0, nz);
            v(vc, e, c0 * r, yb, s0 * r, band, 1, 0, 0, light, nx, 0, nz);
            v(vc, e, c0 * r, yb, s0 * r, body, 1, 0, 1, light, nx, 0, nz);
            v(vc, e, c1 * r, yb, s1 * r, body, 1, 1, 1, light, nx, 0, nz);
            v(vc, e, c1 * r, top, s1 * r, body, 1, 1, 0, light, nx, 0, nz);
            v(vc, e, c0 * r, top, s0 * r, body, 1, 0, 0, light, nx, 0, nz);
            // denko
            v(vc, e, 0, top, 0, body, 1, 0.5f, 0.5f, light, 0, -1, 0);
            v(vc, e, 0, top, 0, body, 1, 0.5f, 0.5f, light, 0, -1, 0);
            v(vc, e, c1 * r, top, s1 * r, body, 1, 1, 1, light, 0, -1, 0);
            v(vc, e, c0 * r, top, s0 * r, body, 1, 0, 1, light, 0, -1, 0);
        }
    }

    // ------------------------------------------------------------------ Halo

    private void halo(MatrixStack ms, VertexConsumerProvider vcp, float time) {
        ms.push();
        getContextModel().head.rotate(ms);
        float bob = MathHelper.sin(time * 2.2f) * 0.025f;
        // nad czapką, jeśli jest włączona
        float hat = !V.on("Hat") ? 0 : switch (V.mode("Hat", "Styl", "China hat")) { case "Top hat" -> 0.5f; case "Crown" -> 0.22f; default -> 0.36f; } * V.num("Hat", "Rozmiar", 1);
        ms.translate(0, -0.5f - 0.12f - hat - V.num("Halo", "Wysokość", 0.3f) * 0.5f + bob, 0);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 25));
        int c = V.bo("Halo", "Tęcza") ? V.rainbow(1, 0) : V.color("Halo", "Kolor", 0xFFFFFFFF);
        float size = 0.3f * V.num("Halo", "Rozmiar", 1);
        flat(ms, vcp.getBuffer(RenderLayer.getEyes(RING)), size, c, 1f);
        if (V.bo("Halo", "Poświata")) flat(ms, vcp.getBuffer(RenderLayer.getEyes(GLOW)), size * 1.35f, c, 0.45f);
        ms.pop();
    }

    /** pozioma płytka (XZ) z teksturą – mieszanie addytywne, więc kolor mnożymy przez jasność */
    private static void flat(MatrixStack ms, VertexConsumer vc, float s, int c, float k) {
        MatrixStack.Entry e = ms.peek();
        int col = 0xFF000000 | ((int) (V.r(c) * 255 * k) << 16) | ((int) (V.g(c) * 255 * k) << 8) | (int) (V.b(c) * 255 * k);
        int L = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        // warstwa "eyes" ma włączone odrzucanie tylnych ścian, więc rysujemy obie strony
        v(vc, e, -s, 0, -s, col, 1, 0, 0, L, 0, -1, 0);
        v(vc, e, s, 0, -s, col, 1, 1, 0, L, 0, -1, 0);
        v(vc, e, s, 0, s, col, 1, 1, 1, L, 0, -1, 0);
        v(vc, e, -s, 0, s, col, 1, 0, 1, L, 0, -1, 0);
        v(vc, e, -s, 0, s, col, 1, 0, 1, L, 0, 1, 0);
        v(vc, e, s, 0, s, col, 1, 1, 1, L, 0, 1, 0);
        v(vc, e, s, 0, -s, col, 1, 1, 0, L, 0, 1, 0);
        v(vc, e, -s, 0, -s, col, 1, 0, 0, L, 0, 1, 0);
    }

    // ------------------------------------------------------------------ Wings

    private void wings(MatrixStack ms, VertexConsumerProvider vcp, int light, float time) {
        Identifier tex = WINGS.getOrDefault(V.mode("Wings", "Styl", "Angel"), WINGS.get("Angel"));
        int c = V.bo("Wings", "Tęcza") ? V.rainbow(0.8f, 0) : V.color("Wings", "Kolor", 0xFFFFFFFF);
        float w = 0.95f * V.num("Wings", "Skala", 1);
        float flap = V.bo("Wings", "Animacja") ? 14 * MathHelper.sin(time * 2.6f) : 0;
        float open = 28 + flap;
        ms.push();
        getContextModel().body.rotate(ms);
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityTranslucent(tex));
        for (int s = -1; s <= 1; s += 2) {
            ms.push();
            ms.translate(s * 0.05f, 0.15f, 0.14f);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-s * open));
            MatrixStack.Entry e = ms.peek();
            float top = -0.42f * w, bottom = top + w;
            float x1 = s * w;
            float u0 = 0, u1 = 1;
            v(vc, e, 0, top, 0, c, 1, u0, 0, light, 0, 0, 1);
            v(vc, e, x1, top, 0, c, 1, u1, 0, light, 0, 0, 1);
            v(vc, e, x1, bottom, 0, c, 1, u1, 1, light, 0, 0, 1);
            v(vc, e, 0, bottom, 0, c, 1, u0, 1, light, 0, 0, 1);
            ms.pop();
        }
        ms.pop();
    }
}
