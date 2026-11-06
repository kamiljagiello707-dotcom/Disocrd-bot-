package pl.firen.client.module.modules;

import net.minecraft.util.hit.HitResult;
import pl.firen.client.mixin.MinecraftClientAccessor;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.NumberSetting;

import java.util.Random;

public class AutoClicker extends Module {
    public final NumberSetting minCps = add(new NumberSetting("Min CPS", 9, 1, 20, 1));
    public final NumberSetting maxCps = add(new NumberSetting("Max CPS", 13, 1, 20, 1));
    public final BooleanSetting left = add(new BooleanSetting("Left click", true));
    public final BooleanSetting right = add(new BooleanSetting("Right click", false));
    public final BooleanSetting ignoreBlocks = add(new BooleanSetting("Ignore blocks", true));

    private final Random random = new Random();
    private long nextLeft, nextRight;

    public AutoClicker() { super("AutoClicker", "Klika za Ciebie, gdy trzymasz przycisk myszy.", Category.COMBAT); }

    private long delay() {
        double lo = Math.min(minCps.get(), maxCps.get()), hi = Math.max(minCps.get(), maxCps.get());
        double cps = lo + (hi - lo) * random.nextDouble();
        return (long) Math.max(40, 1000.0 / cps + random.nextGaussian() * 10);
    }

    @Override
    public void onTick() {
        if (mc.currentScreen != null) return;
        long now = System.currentTimeMillis();
        MinecraftClientAccessor acc = (MinecraftClientAccessor) mc;
        if (left.get() && mc.options.attackKey.isPressed() && now >= nextLeft) {
            boolean onBlock = mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK;
            if (!(ignoreBlocks.get() && onBlock)) {
                acc.firen$doAttack();
                nextLeft = now + delay();
            }
        }
        if (right.get() && mc.options.useKey.isPressed() && now >= nextRight && !mc.player.isUsingItem()) {
            acc.firen$doItemUse();
            nextRight = now + delay();
        }
    }
}
