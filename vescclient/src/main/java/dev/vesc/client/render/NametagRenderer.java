package dev.vesc.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.vesc.client.VescClient;
import dev.vesc.client.module.Nametags;
import dev.vesc.client.render.Fonts.Font;
import dev.vesc.client.ui.Theme;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

/** Rysuje nametagi VescClient nad graczami (z testem głębi). */
public final class NametagRenderer {
    private NametagRenderer() {}

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(NametagRenderer::render);
    }

    public static boolean active() {
        VescClient rc = VescClient.get();
        return rc != null && rc.modules != null && rc.modules.get("Nametags") instanceof Nametags n && n.isEnabled();
    }

    private static void render(WorldRenderContext wctx) {
        if (!active()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || mc.options.hudHidden) return;
        Nametags m = (Nametags) VescClient.get().modules.get("Nametags");
        Camera cam = wctx.camera();
        Vec3d cp = cam.getPos();
        float delta = wctx.tickCounter().getTickDelta(true);
        MatrixStack ms = wctx.matrixStack() != null ? wctx.matrixStack() : new MatrixStack();

        for (PlayerEntity p : mc.world.getPlayers()) {
            boolean self = p == mc.player;
            if (self && (!m.self.get() || mc.options.getPerspective().isFirstPerson())) continue;
            if (p.isInvisible() || p.isSpectator()) continue;
            double dist = p.distanceTo(mc.player);
            if (!self && dist > 64) continue;
            double x = MathHelper.lerp(delta, p.prevX, p.getX()) - cp.x;
            double y = MathHelper.lerp(delta, p.prevY, p.getY()) - cp.y + p.getHeight() + 0.45;
            double z = MathHelper.lerp(delta, p.prevZ, p.getZ()) - cp.z;

            ms.push();
            ms.translate(x, y, z);
            ms.multiply(cam.getRotation());
            float s = 0.02f * m.scale.get().floatValue() * (float) Math.max(1, Math.min(2.2, dist / 9.0));
            ms.scale(s, -s, s);

            DrawContext ctx = new DrawContext(mc, mc.getBufferBuilders().getEntityVertexConsumers());
            ctx.getMatrices().multiplyPositionMatrix(ms.peek().getPositionMatrix());
            R2D.worldDepth = true;
            R2D.alpha = 1;
            drawTag(ctx, p, m, dist);
            ctx.draw();
            R2D.worldDepth = false;
            ms.pop();
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    private static void drawTag(DrawContext ctx, PlayerEntity p, Nametags m, double dist) {
        VescClient rc = VescClient.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        String name = p.getName().getString();
        boolean friend = rc.friends.is(name);
        float hp = p.getHealth() + p.getAbsorptionAmount(), max = Math.max(1, p.getMaxHealth());

        String hpS = String.format(Locale.ROOT, "%.1f", hp);
        int ping = -1;
        if (mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
            if (e != null) ping = e.getLatency();
        }
        int armor = p.getArmor();

        // szerokość
        float pad = 4, headS = 12, gap = 4, ts = 0.9f, ps = 0.62f;
        float w = pad;
        if (m.head.get()) w += headS + gap;
        float nameW = Fonts.width(name, Font.BOLD, ts);
        w += nameW + (friend ? 9 : 0);
        float hpW = Fonts.width(hpS, Font.BOLD, ps) + 13;
        if (m.health.get()) w += gap + hpW;
        String extra = "";
        if (m.armor.get() && armor > 0) extra += armor + "";
        float armW = m.armor.get() && armor > 0 ? Fonts.width(String.valueOf(armor), Font.BOLD, ps) + 13 : 0;
        if (armW > 0) w += gap + armW;
        String pingS = ping >= 0 ? ping + "ms" : null;
        float pingW = m.ping.get() && pingS != null ? Fonts.width(pingS, Font.SEMIBOLD, ps) + 4 : 0;
        if (pingW > 0) w += gap + pingW;
        String distS = String.format(Locale.ROOT, "%.0fm", dist);
        float distW = m.distance.get() ? Fonts.width(distS, Font.SEMIBOLD, ps) + 4 : 0;
        if (distW > 0) w += gap + distW;
        w += pad;
        float h = 18;
        float x = -w / 2f, y = -h;

        // tło
        String bg = m.background.get();
        int ac = Theme.accentWave(0);
        if (!bg.equals("None")) {
            int col = bg.equals("Glass") ? R2D.withAlpha(Theme.hudBg(), 0.55f) : Theme.hudBg();
            R2D.roundRectV(ctx, x, y, w, h, h / 2f, R2D.withAlpha(R2D.blend(col, Theme.text(), 0.07f), ((col >>> 24) & 0xFF) / 255f), col);
            R2D.roundOutline(ctx, x, y, w, h, h / 2f, 0.6f, friend ? ac : R2D.blend(Theme.stroke(), ac, 0.25f));
        }
        float cx = x + pad, cy = y + h / 2f;
        if (m.head.get() && p instanceof AbstractClientPlayerEntity ap) {
            Identifier skin = ap.getSkinTextures().texture();
            R2D.roundTexture(ctx, skin, cx, cy - headS / 2f, headS, headS, 4, 8 / 64f, 8 / 64f, 16 / 64f, 16 / 64f, 0xFFFFFFFF);
            R2D.roundTexture(ctx, skin, cx - 0.4f, cy - headS / 2f - 0.4f, headS + 0.8f, headS + 0.8f, 4.2f, 40 / 64f, 8 / 64f, 48 / 64f, 16 / 64f, 0xFFFFFFFF);
            if (p.hurtTime > 0) R2D.roundRect(ctx, cx, cy - headS / 2f, headS, headS, 4, R2D.withAlpha(0xFFFF3B3B, p.hurtTime / 10f * 0.5f));
            cx += headS + gap;
        }
        Fonts.drawV(ctx, name, cx, cy + 0.6f, friend ? ac : 0xFFFFFFFF, Font.BOLD, ts);
        cx += nameW;
        if (friend) { R2D.icon(ctx, "users", cx + 2, cy - 3, 6, ac); cx += 9; }
        if (m.health.get()) {
            cx += gap;
            int hc = hp / max > 0.5f ? R2D.blend(0xFFFACC15, 0xFF4ADE80, (hp / max - 0.5f) * 2) : R2D.blend(0xFFEF4444, 0xFFFACC15, hp / max * 2);
            R2D.roundRect(ctx, cx, cy - 5, hpW, 10, 5, R2D.withAlpha(hc, 0.2f));
            R2D.icon(ctx, "heart", cx + 3, cy - 3, 6, hc);
            Fonts.drawV(ctx, hpS, cx + 10.5f, cy + 0.5f, hc, Font.BOLD, ps);
            cx += hpW;
        }
        if (armW > 0) {
            cx += gap;
            R2D.roundRect(ctx, cx, cy - 5, armW, 10, 5, R2D.withAlpha(0xFF93C5FD, 0.18f));
            R2D.icon(ctx, "shield", cx + 3, cy - 3, 6, 0xFF93C5FD);
            Fonts.drawV(ctx, String.valueOf(armor), cx + 10.5f, cy + 0.5f, 0xFF93C5FD, Font.BOLD, ps);
            cx += armW;
        }
        if (pingW > 0) {
            cx += gap;
            int pc = ping < 80 ? 0xFF4ADE80 : ping < 160 ? 0xFFFACC15 : 0xFFEF4444;
            Fonts.drawV(ctx, pingS, cx + 2, cy + 0.5f, pc, Font.SEMIBOLD, ps);
            cx += pingW;
        }
        if (distW > 0) {
            cx += gap;
            Fonts.drawV(ctx, distS, cx + 2, cy + 0.5f, Theme.textDim(), Font.SEMIBOLD, ps);
        }
        // pasek zdrowia pod spodem
        if (m.healthBar.get()) {
            float bw = w - h, bx = x + h / 2f, by = y + h + 2.5f;
            R2D.roundRect(ctx, bx, by, bw, 2.2f, 1.1f, R2D.withAlpha(Theme.hudBg(), 0.9f));
            float pct = Math.min(1, hp / max);
            R2D.roundRect(ctx, bx, by, Math.max(2.2f, bw * pct), 2.2f, 1.1f, ac);
        }
    }
}
