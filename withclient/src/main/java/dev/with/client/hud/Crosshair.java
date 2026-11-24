package dev.with.client.hud;

import dev.with.client.render.Fonts;
import dev.with.client.render.Fonts.Font;
import dev.with.client.render.R2D;
import dev.with.client.ui.Anim;
import dev.with.client.visual.Effects;
import dev.with.client.visual.V;
import dev.with.client.visual.View;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.GameMode;

import java.util.Locale;

/** Własny celownik (Crosshair) z hitmarkerem i wskaźnikiem ataku oraz pigułka powiększenia (Zoom). */
final class Crosshair {
    private Crosshair() {}

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Anim gapA = new Anim(0, 10), zoomA = new Anim(0, 10), targetA = new Anim(0, 12);

    /** true = celownik z gry ma się nie rysować */
    static boolean draw(DrawContext ctx, RenderTickCounter tc) {
        if (!V.on("Crosshair")) return false;
        if (!mc.options.getPerspective().isFirstPerson()) return true;
        if (mc.interactionManager != null && mc.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR) return false;
        if (mc.getDebugHud().shouldShowDebugHud() && !mc.player.hasReducedDebugInfo() && !mc.options.getReducedDebugInfo().getValue()) return false;

        String style = V.mode("Crosshair", "Styl", "Cross");
        float size = V.num("Crosshair", "Rozmiar", 4), gap = V.num("Crosshair", "Przerwa", 2), th = V.num("Crosshair", "Grubość", 1);
        int col = V.color("Crosshair", "Kolor", 0xFFFFFFFF);
        boolean outline = V.bo("Crosshair", "Obrys");
        float tgt = targetA.animate(V.bo("Crosshair", "Kolor na celu") && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY
                && mc.targetedEntity instanceof LivingEntity ? 1 : 0);
        col = R2D.blend(col, V.color("Crosshair", "Kolor celu", 0xFFFF5C7A) | 0xFF000000, tgt);
        float dyn = 0;
        if (V.bo("Crosshair", "Dynamiczny") && mc.player != null) {
            double v = mc.player.getVelocity().horizontalLength() * 20;
            dyn = (float) Math.min(5, v * 0.45) + (mc.player.isOnGround() ? 0 : 1.5f) + (1 - mc.player.getAttackCooldownProgress(0)) * 2.5f;
        }
        float g = gap + gapA.animate(dyn);
        float b = Hud.base();
        float cx = ctx.getScaledWindowWidth() / 2f, cy = ctx.getScaledWindowHeight() / 2f;
        var m = ctx.getMatrices();
        m.push();
        m.translate(cx, cy, 0);
        m.scale(b, b, 1);
        float o = outline ? 0.75f : 0;
        int oc = R2D.withAlpha(0xFF000000, 0.55f * V.a(col));
        switch (style) {
            case "Dot" -> {
                if (o > 0) R2D.circle(ctx, 0, 0, th * 1.4f + o, oc);
                R2D.circle(ctx, 0, 0, th * 1.4f, col);
            }
            case "Circle" -> {
                float r = g + size * 0.6f;
                if (o > 0) R2D.roundOutline(ctx, -r - o, -r - o, (r + o) * 2, (r + o) * 2, r + o, th + o * 2, oc);
                R2D.roundOutline(ctx, -r, -r, r * 2, r * 2, r, th, col);
            }
            case "Square" -> {
                float r = g + size * 0.5f;
                if (o > 0) R2D.roundOutline(ctx, -r - o, -r - o, (r + o) * 2, (r + o) * 2, 1, th + o * 2, oc);
                R2D.roundOutline(ctx, -r, -r, r * 2, r * 2, 0.6f, th, col);
            }
            case "Chevron" -> {
                m.push();
                m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45));
                arm(ctx, -th / 2, g * 0.7f, th, size, col, oc, o);
                arm(ctx, g * 0.7f, -th / 2, size, th, col, oc, o);
                m.pop();
            }
            default -> {
                boolean t = style.equals("T");
                arm(ctx, -th / 2, g, th, size, col, oc, o);                // dół
                if (!t) arm(ctx, -th / 2, -g - size, th, size, col, oc, o); // góra
                arm(ctx, -g - size, -th / 2, size, th, col, oc, o);        // lewo
                arm(ctx, g, -th / 2, size, th, col, oc, o);                // prawo
            }
        }
        if (V.bo("Crosshair", "Kropka") && !style.equals("Dot")) {
            if (o > 0) R2D.circle(ctx, 0, 0, th * 0.8f + o, oc);
            R2D.circle(ctx, 0, 0, th * 0.8f, col);
        }
        // hitmarker: krótki X po uderzeniu
        long since = System.currentTimeMillis() - Effects.hitTime();
        if (V.bo("Crosshair", "Hitmarker") && since < 320) {
            float k = since / 320f;
            float a = 1 - k * k;
            float d = g + 3 + k * 3, len = 3.5f;
            int hc = R2D.withAlpha(col, a), ho = R2D.withAlpha(0xFF000000, 0.5f * a);
            m.push();
            m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45));
            arm(ctx, -th / 2, d, th, len, hc, ho, o);
            arm(ctx, -th / 2, -d - len, th, len, hc, ho, o);
            arm(ctx, -d - len, -th / 2, len, th, hc, ho, o);
            arm(ctx, d, -th / 2, len, th, hc, ho, o);
            m.pop();
        }
        // wskaźnik ataku (jak w grze, tylko cieńszy)
        if (V.bo("Crosshair", "Wskaźnik ataku") && mc.player != null) {
            float p = mc.player.getAttackCooldownProgress(0);
            if (p < 1) {
                float w = 14, y = g + size + 4;
                R2D.roundRect(ctx, -w / 2 - 0.5f, y - 0.5f, w + 1, 2.5f, 1.25f, 0x66000000);
                R2D.roundRect(ctx, -w / 2, y, w * p, 1.5f, 0.75f, col);
            }
        }
        m.pop();
        return true;
    }

    private static void arm(DrawContext ctx, float x, float y, float w, float h, int col, int oc, float o) {
        if (o > 0) R2D.roundRect(ctx, x - o, y - o, w + o * 2, h + o * 2, Math.min(w, h) / 2f + o, oc);
        R2D.roundRect(ctx, x, y, w, h, Math.min(w, h) / 2f, col);
    }

    /** pigułka z aktualnym przybliżeniem pod celownikiem */
    static void zoom(DrawContext ctx) {
        float z = View.zoom();
        float a = zoomA.animate(z > 1.04f && V.on("Zoom") && V.bo("Zoom", "Wskaźnik") ? 1 : 0);
        if (a < 0.01f) return;
        float b = Hud.base() * Hud.globalScale();
        String s = String.format(Locale.ROOT, "%.1fx", z);
        float tw = Fonts.width(s, Font.BOLD, 0.62f);
        float w = tw + 25, h = 15;
        var m = ctx.getMatrices();
        m.push();
        m.translate(ctx.getScaledWindowWidth() / 2f, ctx.getScaledWindowHeight() / 2f + 26 * b + (1 - a) * 6, 0);
        m.scale(b, b, 1);
        R2D.alpha = a;
        Hud.capsule(ctx, -w / 2, 0, w, h, h / 2f);
        R2D.icon(ctx, "l_zoom_in", -w / 2 + 6, 3, 9, R2D.col(0xFFFFFFFF));
        Fonts.draw(ctx, s, -w / 2 + 18, 3.6f, R2D.col(0xFFFFFFFF), Font.BOLD, 0.62f);
        R2D.alpha = 1;
        m.pop();
    }
}
