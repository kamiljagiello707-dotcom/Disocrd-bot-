package dev.wave.module.player;

import dev.wave.module.Category;
import dev.wave.module.Module;
import dev.wave.setting.BooleanSetting;
import dev.wave.setting.NumberSetting;

public class AntiAFK extends Module {
    public final NumberSetting interval = new NumberSetting("Interval", "Seconds between actions", 15, 2, 120, 1, "s");
    public final BooleanSetting jump = new BooleanSetting("Jump", true);
    public final BooleanSetting rotate = new BooleanSetting("Rotate", true);
    public final BooleanSetting swing = new BooleanSetting("Swing", false);

    private int ticks;

    public AntiAFK() {
        super("AntiAFK", "Prevents getting kicked for being idle", Category.PLAYER);
        addSettings(interval, jump, rotate, swing);
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        if (++ticks < interval.get() * 20) return;
        ticks = 0;
        if (jump.isOn() && mc.player.isOnGround()) mc.player.jump();
        if (rotate.isOn()) mc.player.setYaw(mc.player.getYaw() + (float) (Math.random() * 60 - 30));
        if (swing.isOn()) mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
    }
}
