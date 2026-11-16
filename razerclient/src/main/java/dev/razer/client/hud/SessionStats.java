package dev.razer.client.hud;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;

/** Statystyki sesji: zabójstwa (gracz, którego ostatnio uderzyłeś, zginął w ciągu 4 s), śmierci, seria. */
public class SessionStats {
    public int kills, deaths, streak, bestStreak;
    private PlayerEntity lastHit;
    private long lastHitTime;
    private boolean wasDead;

    public SessionStats() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && entity instanceof PlayerEntity p && p != player) { lastHit = p; lastHitTime = System.currentTimeMillis(); }
            return ActionResult.PASS;
        });
    }

    public void tick(MinecraftClient mc) {
        if (mc.player == null) { lastHit = null; return; }
        boolean dead = mc.player.isDead();
        if (dead && !wasDead) { deaths++; streak = 0; }
        wasDead = dead;
        if (lastHit != null) {
            if (System.currentTimeMillis() - lastHitTime > 4000) lastHit = null;
            else if (lastHit.isDead() || lastHit.getHealth() <= 0) {
                kills++; streak++; bestStreak = Math.max(bestStreak, streak);
                lastHit = null;
            }
        }
    }

    public String kd() { return String.format(java.util.Locale.ROOT, "%.2f", deaths == 0 ? kills : kills / (float) deaths); }
}
