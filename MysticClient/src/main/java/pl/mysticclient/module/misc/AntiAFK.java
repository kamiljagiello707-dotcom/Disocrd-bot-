package pl.mysticclient.module.misc;

import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;
import pl.mysticclient.setting.BoolSetting;
import pl.mysticclient.setting.NumberSetting;

public class AntiAFK extends Module {
    private final NumberSetting interval = add(new NumberSetting("Interval (s)", 15, 3, 120, 1));
    private final BoolSetting jump = add(new BoolSetting("Jump", true));
    private final BoolSetting rotate = add(new BoolSetting("Rotate", true));
    private final BoolSetting swing = add(new BoolSetting("Swing", false));
    private int ticks;

    public AntiAFK() {
        super("AntiAFK", "Rusza postacią co jakiś czas, żeby nie wyrzuciło za AFK.", Category.MISC);
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        if (++ticks < interval.getInt() * 20) return;
        ticks = 0;
        if (jump.get() && mc.player.isOnGround()) mc.player.jump();
        if (rotate.get()) mc.player.setYaw(mc.player.getYaw() + (float) (Math.random() * 60 - 30));
        if (swing.get()) mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
    }
}
