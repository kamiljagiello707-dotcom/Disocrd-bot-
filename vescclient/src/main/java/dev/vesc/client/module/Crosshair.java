package dev.vesc.client.module;

import dev.vesc.client.render.R2D;
import dev.vesc.client.setting.BoolSetting;
import dev.vesc.client.setting.ColorSetting;
import dev.vesc.client.setting.ModeSetting;
import dev.vesc.client.setting.NumberSetting;
import dev.vesc.client.ui.Theme;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.RotationAxis;

/** Własny celownik: kilka stylów, dynamiczny rozrzut, hit marker, pasek cooldownu. Zastępuje celownik vanilla. */
public class Crosshair extends Module {
    public final ModeSetting style = new ModeSetting("Styl", "Style", "Cross", "Cross + Dot", "Dot", "Circle", "T-Shape");
    public final NumberSetting size = new NumberSetting("Rozmiar", "Size", 4, 1, 12, 0.5);
    public final NumberSetting gap = new NumberSetting("Przerwa", "Gap", 2, 0, 8, 0.5);
    public final NumberSetting thickness = new NumberSetting("Grubość", "Thickness", 1, 0.5, 3, 0.25);
    public final BoolSetting outline = new BoolSetting("Obrys", "Outline", true);
    public final BoolSetting dynamic = new BoolSetting("Dynamiczny", "Dynamic", true);
    public final BoolSetting hitMarker = new BoolSetting("Hit marker", "Hit Marker", true);
    public final BoolSetting cooldown = new BoolSetting("Cooldown ataku", "Attack Cooldown", true);
    public final BoolSetting targetColor = new BoolSetting("Kolor przy celu", "Target Color", true);
    public final ModeSetting colorMode = new ModeSetting("Kolor", "Color", "Accent", "White", "Custom");
    public final ColorSetting customColor = new ColorSetting("Własny kolor", "Custom Color", 0xFF4D8BFF);
    public final NumberSetting opacity = new NumberSetting("Krycie", "Opacity", 100, 20, 100, 5);

    private static long lastHit;
    private static float spread, targetAnim;
    private static long lastFrame = System.nanoTime();

    public Crosshair() {
        super("Crosshair", Category.CAMERA, "target", "Własny, nowoczesny celownik", "Custom modern crosshair");
        add(style, size, gap, thickness, outline, dynamic, hitMarker, cooldown, targetColor, colorMode, customColor, opacity);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && player == MinecraftClient.getInstance().player) lastHit = System.currentTimeMillis();
            return ActionResult.PASS;
        });
    }

    /** @return true gdy celownik vanilla ma zostać ukryty */
    public static boolean render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!(dev.vesc.client.VescClient.get().modules.get("Crosshair") instanceof Crosshair c) || !c.isEnabled()) return false;
        if (!mc.options.getPerspective().isFirstPerson() || mc.getDebugHud().shouldShowDebugHud() || mc.player == null) return true;
        c.draw(ctx, mc);
        return true;
    }

    private void draw(DrawContext ctx, MinecraftClient mc) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        float k = 1 - (float) Math.exp(-12 * dt);

        double vx = mc.player.getVelocity().x, vz = mc.player.getVelocity().z;
        float move = (float) Math.sqrt(vx * vx + vz * vz) * 18 + (mc.player.isOnGround() ? 0 : 1.5f);
        spread += ((dynamic.get() ? Math.min(4f, move) : 0) - spread) * k;
        boolean onTarget = mc.targetedEntity != null && targetColor.get();
        targetAnim += ((onTarget ? 1 : 0) - targetAnim) * k;

        int base = switch (colorMode.get()) {
            case "White" -> 0xFFFFFFFF;
            case "Custom" -> customColor.get();
            default -> Theme.accentWave(0);
        };
        int col = R2D.withAlpha(R2D.blend(base, 0xFFFF4D5E, targetAnim), (float) (opacity.get() / 100.0));
        int ol = R2D.withAlpha(0xFF000000, 0.55f * (float) (opacity.get() / 100.0));

        float cx = ctx.getScaledWindowWidth() / 2f, cy = ctx.getScaledWindowHeight() / 2f;
        float s = size.get().floatValue(), g = gap.get().floatValue() + spread, t = thickness.get().floatValue();
        float o = outline.get() ? 0.5f : 0;
        String st = style.get();

        if (st.equals("Cross") || st.equals("Cross + Dot") || st.equals("T-Shape")) {
            bar(ctx, cx - g - s, cy - t / 2, s, t, o, col, ol);
            bar(ctx, cx + g, cy - t / 2, s, t, o, col, ol);
            bar(ctx, cx - t / 2, cy + g, t, s, o, col, ol);
            if (!st.equals("T-Shape")) bar(ctx, cx - t / 2, cy - g - s, t, s, o, col, ol);
        }
        if (st.equals("Dot") || st.equals("Cross + Dot")) {
            float r = Math.max(0.75f, t * (st.equals("Dot") ? 1.4f : 0.8f));
            if (o > 0) R2D.circle(ctx, cx, cy, r + o, ol);
            R2D.circle(ctx, cx, cy, r, col);
        }
        if (st.equals("Circle")) {
            float r = s + g;
            if (o > 0) R2D.roundOutline(ctx, cx - r - o, cy - r - o, (r + o) * 2, (r + o) * 2, r + o, t + o * 2, ol);
            R2D.roundOutline(ctx, cx - r, cy - r, r * 2, r * 2, r, t, col);
            R2D.circle(ctx, cx, cy, Math.max(0.6f, t * 0.6f), col);
        }

        // hit marker – krótki „X” po trafieniu
        float hm = 1 - (System.currentTimeMillis() - lastHit) / 280f;
        if (hitMarker.get() && hm > 0) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate(cx, cy, 0);
            ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45));
            int hc = R2D.withAlpha(0xFFFFFFFF, hm);
            float hg = 3 + g + (1 - hm) * 2, hs = 3.5f;
            bar(ctx, -hg - hs, -0.5f, hs, 1, 0, hc, 0);
            bar(ctx, hg, -0.5f, hs, 1, 0, hc, 0);
            bar(ctx, -0.5f, -hg - hs, 1, hs, 0, hc, 0);
            bar(ctx, -0.5f, hg, 1, hs, 0, hc, 0);
            ctx.getMatrices().pop();
        }

        // cooldown ataku – cienki pasek pod celownikiem
        float cd = mc.player.getAttackCooldownProgress(0);
        if (cooldown.get() && cd < 1) {
            float w = 16, y = cy + g + s + 4;
            R2D.roundRect(ctx, cx - w / 2, y, w, 2, 1, R2D.withAlpha(0xFF000000, 0.45f));
            R2D.roundRect(ctx, cx - w / 2, y, w * cd, 2, 1, col);
        }
    }

    private static void bar(DrawContext ctx, float x, float y, float w, float h, float o, int col, int ol) {
        if (o > 0) R2D.roundRect(ctx, x - o, y - o, w + o * 2, h + o * 2, Math.min(w, h) / 2 + o, ol);
        R2D.roundRect(ctx, x, y, w, h, Math.min(w, h) / 2, col);
    }
}
