package pl.firen.client.module.modules;

import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.BooleanSetting;
import pl.firen.client.setting.NumberSetting;
import pl.firen.client.util.Targets;

import java.util.concurrent.ThreadLocalRandom;

public class TriggerBot extends Module {
    public final NumberSetting cooldown = add(new NumberSetting("Cooldown %", 95, 50, 100, 1));
    public final NumberSetting minDelay = add(new NumberSetting("Min delay ms", 20, 0, 300, 5));
    public final NumberSetting maxDelay = add(new NumberSetting("Max delay ms", 70, 0, 300, 5));
    public final BooleanSetting players = add(new BooleanSetting("Players", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));

    private Entity lastSeen;
    private long attackAt;

    public TriggerBot() { super("TriggerBot", "Atakuje, gdy celownik jest na celu.", Category.COMBAT); }

    @Override
    public void onTick() {
        if (mc.currentScreen != null || mc.interactionManager == null) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) { lastSeen = null; return; }
        Entity e = hit.getEntity();
        if (!Targets.matches(e, players.get(), mobs.get(), animals.get())) { lastSeen = null; return; }
        long now = System.currentTimeMillis();
        if (e != lastSeen) {
            lastSeen = e;
            double lo = Math.min(minDelay.get(), maxDelay.get()), hi = Math.max(minDelay.get(), maxDelay.get());
            attackAt = now + (long) (lo + (hi - lo) * ThreadLocalRandom.current().nextDouble());
        }
        if (now < attackAt || mc.player.getAttackCooldownProgress(0.5f) < cooldown.get() / 100.0) return;
        mc.interactionManager.attackEntity(mc.player, e);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
