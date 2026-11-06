package pl.firen.client.module.modules;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import pl.firen.client.gui.Theme;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.render.Fonts;
import pl.firen.client.render.Project;
import pl.firen.client.render.Render2D;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.NumberSetting;
import pl.firen.client.util.Targets;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Nametags extends Module {
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", false));
    public final BooleanSetting health = add(new BooleanSetting("Health", true));
    public final BooleanSetting distance = add(new BooleanSetting("Distance", true));
    public final NumberSetting scale = add(new NumberSetting("Scale", 1.0, 0.5, 2.0, 0.05));
    public final NumberSetting range = add(new NumberSetting("Range", 128, 16, 256, 8));

    public Nametags() { super("Nametags", "Nowoczesne nazwy z życiem i dystansem.", Category.RENDER); }

    @Override
    public void onRenderHud(DrawContext ctx, float s) {
        if (!isEnabled()) return;
        float delta = mc.getRenderTickCounter().getTickDelta(true);
        List<Entity> list = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) {
            if (!Targets.matches(e, true, mobs.get(), false)) continue;
            if (mc.player.distanceTo(e) > range.get()) continue;
            list.add(e);
        }
        list.sort(Comparator.comparingDouble(e -> -mc.player.distanceTo(e))); // dalsze najpierw
        for (Entity e : list) {
            Vec3d top = e.getLerpedPos(delta).add(0, e.getHeight() + 0.45, 0);
            float[] p = Project.toScreen(top);
            if (p == null) continue;
            float x = p[0] / s, y = p[1] / s;
            float dist = mc.player.distanceTo(e);
            float sc = (float) (scale.get() * Math.max(0.7, Math.min(1.15, 1.25 - dist / 60.0)));

            String name = e.getName().getString();
            String hp = "";
            int hpColor = Theme.TEXT;
            if (health.get() && e instanceof LivingEntity l) {
                float h = l.getHealth() + l.getAbsorptionAmount();
                hp = String.format("%.1f", h);
                float pct = l.getHealth() / Math.max(1, l.getMaxHealth());
                hpColor = pct > 0.5f ? 0xFF3DDC97 : pct > 0.25f ? 0xFFFFC34D : 0xFFFF4D5E;
            }
            String dst = distance.get() ? String.format("%.0fm", dist) : "";
            float w = Fonts.width(name, Fonts.SEMIBOLD) + 14 + (hp.isEmpty() ? 0 : Fonts.width(hp, Fonts.SEMIBOLD) + 8)
                    + (dst.isEmpty() ? 0 : Fonts.width(dst, Fonts.SMALL) + 8);
            float h = 17;

            var m = ctx.getMatrices();
            m.push();
            m.translate(x, y, 0);
            m.scale(sc, sc, 1);
            float bx = -w / 2f, by = -h;
            Render2D.shadow(ctx, bx, by + 1, w, h, 6, 6, 0x70000000);
            Render2D.round(ctx, bx, by, w, h, 6, 0xD90C0C0F);
            Render2D.round(ctx, bx, by + h - 1.5f, w, 1.5f, 0.75f, e instanceof PlayerEntity ? Theme.accentAlpha(0xCC) : 0x80FF4D5E);
            float tx = bx + 7;
            Fonts.draw(ctx, name, Fonts.SEMIBOLD, tx, by + 5, Theme.TEXT);
            tx += Fonts.width(name, Fonts.SEMIBOLD) + 8;
            if (!hp.isEmpty()) {
                Fonts.draw(ctx, hp, Fonts.SEMIBOLD, tx, by + 5, hpColor);
                tx += Fonts.width(hp, Fonts.SEMIBOLD) + 8;
            }
            if (!dst.isEmpty()) Fonts.draw(ctx, dst, Fonts.SMALL, tx, by + 6, Theme.TEXT_3);
            m.pop();
        }
    }
}
