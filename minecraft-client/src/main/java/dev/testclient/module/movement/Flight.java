package dev.testclient.module.movement;

import dev.testclient.module.Category;
import dev.testclient.module.Module;
import dev.testclient.setting.BooleanSetting;
import dev.testclient.setting.NumberSetting;
import net.minecraft.util.math.Vec3d;

/**
 * Na własnym serwerze ustaw allow-flight=true w server.properties,
 * inaczej vanilla wyrzuci Cię za latanie.
 */
public class Flight extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", 1.0, 0.1, 5.0, 0.1));
    private final BooleanSetting vanilla = add(new BooleanSetting("Vanilla", false));

    public Flight() {
        super("Flight", "Latanie w survivalu", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        var p = mc.player;
        if (vanilla.get()) {
            p.getAbilities().allowFlying = true;
            p.getAbilities().flying = true;
            p.getAbilities().setFlySpeed((float) (0.05 * speed.get()));
            return;
        }

        double s = speed.get();
        double y = 0;
        if (mc.options.jumpKey.isPressed()) y += s;
        if (mc.options.sneakKey.isPressed()) y -= s;

        float forward = p.input.movementForward;
        float strafe = p.input.movementSideways;
        Vec3d horizontal = Vec3d.ZERO;
        if (forward != 0 || strafe != 0) {
            double yaw = Math.toRadians(p.getYaw());
            double len = Math.sqrt(forward * forward + strafe * strafe);
            double f = forward / len, st = strafe / len;
            horizontal = new Vec3d(
                    (st * Math.cos(yaw) - f * Math.sin(yaw)) * s,
                    0,
                    (f * Math.cos(yaw) + st * Math.sin(yaw)) * s);
        }
        p.setVelocity(horizontal.x, y, horizontal.z);
        p.fallDistance = 0;
    }

    @Override
    protected void onDisable() {
        var p = mc.player;
        if (p == null) return;
        if (!p.isCreative() && !p.isSpectator()) {
            p.getAbilities().allowFlying = false;
            p.getAbilities().flying = false;
        }
        p.getAbilities().setFlySpeed(0.05f);
    }
}
