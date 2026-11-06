package pl.firen.client.module.modules;

import net.minecraft.util.math.Vec3d;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.ModeSetting;
import pl.firen.client.setting.NumberSetting;

public class Speed extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Strafe", "Strafe", "Ground"));
    public final NumberSetting speed = add(new NumberSetting("Speed", 1.25, 1.0, 3.0, 0.05));

    public Speed() { super("Speed", "Szybsze poruszanie się (Strafe = bhop).", Category.MOVEMENT); }

    @Override public String getSuffix() { return mode.get(); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p.isTouchingWater() || p.isInLava() || p.isClimbing() || p.hasVehicle() || p.getAbilities().flying) return;
        float fwd = p.input.movementForward, side = p.input.movementSideways;
        if (fwd == 0 && side == 0) return;

        double base = 0.2873 * speed.get();
        if (mode.is("Strafe") && p.isOnGround()) p.jump();

        double yaw = Math.toRadians(p.getYaw());
        double len = Math.sqrt(fwd * fwd + side * side);
        double f = fwd / len, s = side / len;
        double dx = s * Math.cos(yaw) - f * Math.sin(yaw);
        double dz = f * Math.cos(yaw) + s * Math.sin(yaw);
        Vec3d v = p.getVelocity();
        double current = Math.hypot(v.x, v.z);
        double target = mode.is("Ground") ? (p.isOnGround() ? base : current) : Math.max(current, base * (p.isOnGround() ? 1.0 : 0.95));
        p.setVelocity(dx * target, v.y, dz * target);
    }
}
