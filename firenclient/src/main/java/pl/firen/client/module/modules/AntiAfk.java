package pl.firen.client.module.modules;

import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.NumberSetting;

public class AntiAfk extends Module {
    public final NumberSetting interval = add(new NumberSetting("Interval s", 20, 5, 120, 5));
    private int ticks;

    public AntiAfk() { super("AntiAFK", "Co jakiś czas skacze i się obraca, żeby nie wyrzuciło za AFK.", Category.MISC); }

    @Override
    public void onTick() {
        if (++ticks < interval.get() * 20) return;
        ticks = 0;
        if (mc.player.isOnGround()) mc.player.jump();
        mc.player.setYaw(mc.player.getYaw() + 90f);
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
    }
}
