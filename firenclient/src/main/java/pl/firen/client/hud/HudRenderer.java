package pl.firen.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import pl.firen.client.FirenClient;
import pl.firen.client.gui.Theme;
import pl.firen.client.module.Module;
import pl.firen.client.module.modules.Hud;
import pl.firen.client.module.modules.KillAura;
import pl.firen.client.render.Fonts;
import pl.firen.client.render.Render2D;
import pl.firen.client.util.Anim;

import java.util.Comparator;
import java.util.List;

/** Nowoczesny HUD: watermark, lista modułów, współrzędne i Target HUD. */
public final class HudRenderer {
    private long lastFrame = System.nanoTime();
    private float targetAnim, healthAnim = -1;
    private LivingEntity lastTarget;

    public void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastFrame) / 1e9f);
        lastFrame = now;

        Hud hud = FirenClient.modules().get(Hud.class);
        if (!hud.isEnabled() || mc.options.hudHidden || mc.player == null) return;

        double factor = mc.getWindow().getScaleFactor();
        float s = (float) ((mc.getWindow().getFramebufferWidth() >= 1280 ? 2 : 1) / factor);
        float vw = ctx.getScaledWindowWidth() / s, vh = ctx.getScaledWindowHeight() / s;

        var m = ctx.getMatrices();
        m.push();
        m.translate(0, 0, 5000); // nad warstwami vanilla HUD (każda podnosi Z o 200)
        m.scale(s, s, 1f);
        Render2D.alpha = 1f;

        FirenClient.modules().renderHud(ctx, s); // 2D ESP, Nametags (pod resztą HUD)
        Render2D.alpha = 1f;

        if (hud.watermark.get()) watermark(ctx, mc, hud);
        if (hud.arrayList.get()) arrayList(ctx, hud, vw, dt);
        if (hud.info.get()) coords(ctx, mc, vh);
        targetHud(ctx, vw, vh, dt);

        m.pop();
    }

    private void watermark(DrawContext ctx, MinecraftClient mc, Hud hud) {
        float x = 8, y = 8, h = 24;
        String fps = mc.getCurrentFps() + " fps";
        String user = mc.getSession().getUsername();
        float w = 30 + Fonts.width("FirenClient", Fonts.SEMIBOLD) + 22 + 22
                + (hud.info.get() ? Fonts.width(fps, Fonts.REGULAR) + 18 : 0)
                + Fonts.width(user, Fonts.REGULAR) + 2;
        Render2D.shadow(ctx, x, y + 2, w, h, 9, 10, 0x80000000);
        Render2D.gradientV(ctx, x, y, w, h, 9, 0xE6141418, 0xE60A0A0D);
        Render2D.outline(ctx, x, y, w, h, 9, 0.5f, 0x1CFFFFFF);
        Render2D.gradientV(ctx, x + 5, y + 5, 14, 14, 5, Theme.accent(), Theme.accent2());
        Fonts.drawCentered(ctx, Fonts.I_FIRE, Fonts.ICONS, x + 12, y + 8.5f, Theme.onAccent());
        float tx = x + 25;
        Fonts.draw(ctx, "FirenClient", Fonts.SEMIBOLD, tx, y + 8, Theme.TEXT);
        tx += Fonts.width("FirenClient", Fonts.SEMIBOLD) + 4;
        Render2D.round(ctx, tx, y + 7, 17, 10, 4, Theme.accentAlpha(0x38));
        Fonts.drawCentered(ctx, "V2", Fonts.SMALL, tx + 8.5f, y + 9, Theme.accent());
        tx += 26;
        if (hud.info.get()) {
            Render2D.circle(ctx, tx, y + 12, 1.2f, 0x60FFFFFF);
            tx += 8;
            Fonts.draw(ctx, fps, Fonts.REGULAR, tx, y + 8, Theme.TEXT_2);
            tx += Fonts.width(fps, Fonts.REGULAR) + 9;
        }
        Render2D.circle(ctx, tx, y + 12, 1.2f, 0x60FFFFFF);
        Fonts.draw(ctx, user, Fonts.REGULAR, tx + 8, y + 8, Theme.TEXT_2);
    }

    private void arrayList(DrawContext ctx, Hud hud, float vw, float dt) {
        List<Module> mods = FirenClient.modules().all().stream()
                .peek(md -> md.arrayAnim = Anim.approach(md.arrayAnim, md.isEnabled() && md != hud ? 1f : 0f, dt, 14f))
                .filter(md -> md.arrayAnim > 0.01f)
                .sorted(Comparator.comparingDouble((Module md) -> -labelWidth(md)))
                .toList();
        float y = 8;
        for (Module md : mods) {
            float t = Anim.easeOutCubic(md.arrayAnim);
            float w = labelWidth(md) + 16, h = 16;
            float x = vw - 8 - w + (1 - t) * (w + 12);
            Render2D.alpha = t;
            Render2D.shadow(ctx, x, y + 1, w, h, 6, 6, 0x60000000);
            Render2D.round(ctx, x, y, w, h, 6, 0xD90C0C0F);
            Render2D.round(ctx, x + w - 3, y + 4, 1.5f, h - 8, 0.75f, Theme.accent());
            Render2D.shadow(ctx, x + w - 3, y + 4, 1.5f, h - 8, 0.75f, 4, Theme.accentAlpha(0x70));
            Fonts.draw(ctx, md.getName(), Fonts.SEMIBOLD, x + 7, y + 4.5f, Theme.TEXT);
            if (md.getSuffix() != null) {
                Fonts.draw(ctx, md.getSuffix(), Fonts.REGULAR, x + 7 + Fonts.width(md.getName(), Fonts.SEMIBOLD) + 4, y + 4.5f, Theme.TEXT_3);
            }
            y += (h + 3) * t;
        }
        Render2D.alpha = 1f;
    }

    private static float labelWidth(Module md) {
        float w = Fonts.width(md.getName(), Fonts.SEMIBOLD);
        if (md.getSuffix() != null) w += 4 + Fonts.width(md.getSuffix(), Fonts.REGULAR);
        return w;
    }

    private void coords(DrawContext ctx, MinecraftClient mc, float vh) {
        String xyz = String.format("%.0f  %.0f  %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
        float w = Fonts.width(xyz, Fonts.REGULAR) + 34, h = 18, x = 8, y = vh - 8 - h;
        Render2D.round(ctx, x, y, w, h, 7, 0xCC0C0C0F);
        Render2D.outline(ctx, x, y, w, h, 7, 0.5f, 0x16FFFFFF);
        Fonts.draw(ctx, "XYZ", Fonts.SMALL, x + 7, y + 6, Theme.TEXT_3);
        Fonts.draw(ctx, xyz, Fonts.REGULAR, x + 27, y + 5, Theme.TEXT);
    }

    private void targetHud(DrawContext ctx, float vw, float vh, float dt) {
        KillAura aura = FirenClient.modules().get(KillAura.class);
        LivingEntity target = aura.isEnabled() ? aura.getTarget() : null;
        if (target != null) lastTarget = target;
        targetAnim = Anim.approach(targetAnim, target != null ? 1f : 0f, dt, 12f);
        if (targetAnim < 0.01f || lastTarget == null) { healthAnim = -1; return; }

        float hp = Math.max(0, lastTarget.getHealth()), max = Math.max(1, lastTarget.getMaxHealth());
        float pct = Math.min(1, hp / max);
        healthAnim = healthAnim < 0 ? pct : Anim.approach(healthAnim, pct, dt, 8f);

        float w = 150, h = 44, x = vw / 2f + 18, y = vh / 2f + 18;
        float t = Anim.easeOutCubic(targetAnim);
        Render2D.alpha = t;
        var m = ctx.getMatrices();
        m.push();
        m.translate(x + w / 2f, y + h / 2f, 0);
        m.scale(0.9f + 0.1f * t, 0.9f + 0.1f * t, 1);
        m.translate(-(x + w / 2f), -(y + h / 2f), 0);

        Render2D.shadow(ctx, x, y + 3, w, h, 12, 12, 0x90000000);
        Render2D.gradientV(ctx, x, y, w, h, 12, 0xEB141418, 0xEB0A0A0D);
        Render2D.outline(ctx, x, y, w, h, 12, 0.5f, 0x1EFFFFFF);
        Render2D.gradientV(ctx, x + 8, y + 8, 28, 28, 9, Theme.accent(), Theme.accent2());
        String initial = lastTarget.getName().getString().isEmpty() ? "?" : lastTarget.getName().getString().substring(0, 1).toUpperCase();
        Fonts.drawCentered(ctx, initial, Fonts.TITLE, x + 22, y + 15, Theme.onAccent());

        String name = Fonts.trim(lastTarget.getName().getString(), Fonts.SEMIBOLD, w - 90);
        Fonts.draw(ctx, name, Fonts.SEMIBOLD, x + 44, y + 9, Theme.TEXT);
        String hpText = String.format("%.1f HP", hp);
        Fonts.drawRight(ctx, hpText, Fonts.SMALL, x + w - 10, y + 10, Theme.TEXT_2);
        float bx = x + 44, by = y + 27, bw = w - 54;
        Render2D.round(ctx, bx, by, bw, 6, 3, 0xFF26262C);
        Render2D.gradientH(ctx, bx, by, Math.max(6, bw * healthAnim), 6, 3, Theme.accent2(), Theme.accent());

        m.pop();
        Render2D.alpha = 1f;
    }
}
