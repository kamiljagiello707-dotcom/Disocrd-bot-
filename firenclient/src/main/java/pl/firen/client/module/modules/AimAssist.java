package pl.firen.client.module.modules;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.NumberSetting;
import pl.firen.client.util.Friends;
import pl.firen.client.util.Targets;

/** Delikatnie dociąga celownik do najbliższego celu (widoczne, "legit"). */
public class AimAssist extends Module {
    public final NumberSetting range = add(new NumberSetting("Range", 4.5, 2.0, 8.0, 0.1));
    public final NumberSetting fov = add(new NumberSetting("FOV", 70, 10, 180, 5));
    public final NumberSetting strength = add(new NumberSetting("Strength", 35, 5, 100, 1));
    public final BooleanSetting onClick = add(new BooleanSetting("Only on click", true));
    public final BooleanSetting vertical = add(new BooleanSetting("Vertical", false));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", false));

    public AimAssist() { super("AimAssist", "Płynnie pomaga celować w najbliższy cel.", Category.COMBAT); }

    @Override
    public void onTick() {
        if (mc.currentScreen != null || (onClick.get() && !mc.options.attackKey.isPressed())) return;
        LivingEntity best = null;
        double bestAngle = fov.get() / 2;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity l) || !Targets.matches(e, players.get(), mobs.get(), false)) continue;
            if (Friends.is(e.getName().getString()) || mc.player.distanceTo(e) > range.get() || !mc.player.canSee(e)) continue;
            double a = Math.abs(MathHelper.wrapDegrees(yawTo(l.getBoundingBox().getCenter()) - mc.player.getYaw()));
            if (a < bestAngle) { bestAngle = a; best = l; }
        }
        if (best == null) return;
        Vec3d c = best.getBoundingBox().getCenter();
        float k = strength.get().floatValue() / 100f * 0.35f;
        mc.player.setYaw(mc.player.getYaw() + MathHelper.wrapDegrees(yawTo(c) - mc.player.getYaw()) * k);
        if (vertical.get()) {
            Vec3d d = c.subtract(mc.player.getEyePos());
            float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
            mc.player.setPitch(mc.player.getPitch() + (pitch - mc.player.getPitch()) * k);
        }
    }

    private float yawTo(Vec3d p) {
        Vec3d d = p.subtract(mc.player.getEyePos());
        return (float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90f;
    }
}
