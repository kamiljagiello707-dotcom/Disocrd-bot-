package pl.firen.client.module.modules;

import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.ModeSetting;
import pl.firen.client.setting.NumberSetting;

public class Flight extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Motion", "Motion", "Vanilla"));
    public final NumberSetting speed = add(new NumberSetting("Speed", 1.0, 0.1, 5.0, 0.1));
    public final BooleanSetting antiKick = add(new BooleanSetting("Anti kick", true));
    private int ticks;

    public Flight() { super("Flight", "Pozwala latać (Spacja w górę, Shift w dół).", Category.MOVEMENT); }

    @Override public String getSuffix() { return mode.get(); }

    @Override
    public void onTick() {
        var p = mc.player;
        ticks++;
        if (mode.is("Vanilla")) {
            p.getAbilities().allowFlying = true;
            p.getAbilities().flying = true;
            p.getAbilities().setFlySpeed((float) (0.05 * speed.get()));
        } else {
            float fwd = p.input.movementForward, side = p.input.movementSideways;
            double yaw = Math.toRadians(p.getYaw());
            double sp = speed.get();
            double dx = (side * Math.cos(yaw) - fwd * Math.sin(yaw)) * sp;
            double dz = (fwd * Math.cos(yaw) + side * Math.sin(yaw)) * sp;
            double dy = mc.options.jumpKey.isPressed() ? sp * 0.6 : mc.options.sneakKey.isPressed() ? -sp * 0.6 : 0;
            if (antiKick.get() && ticks % 40 == 0 && dy == 0) dy = -0.04;
            p.setVelocity(dx, dy, dz);
            p.fallDistance = 0;
        }
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;
        var a = mc.player.getAbilities();
        if (!mc.player.isCreative() && !mc.player.isSpectator()) {
            a.flying = false;
            a.allowFlying = false;
        }
        a.setFlySpeed(0.05f);
    }
}
