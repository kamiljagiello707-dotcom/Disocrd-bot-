package pl.mysticclient.module.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;
import pl.mysticclient.setting.BoolSetting;
import pl.mysticclient.setting.NumberSetting;
import pl.mysticclient.util.FriendManager;

public class TriggerBot extends Module {
    private final NumberSetting cooldown = add(new NumberSetting("Cooldown", 1.0, 0.5, 1.0, 0.05));
    private final BoolSetting players = add(new BoolSetting("Players", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", true));
    private final BoolSetting ignoreFriends = add(new BoolSetting("Ignore friends", true));

    public TriggerBot() {
        super("TriggerBot", "Atakuje cel pod celownikiem, gdy miecz jest naładowany.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (nullCheck() || mc.currentScreen != null || mc.player.isUsingItem()) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;
        Entity e = hit.getEntity();
        if (!(e instanceof LivingEntity living) || !living.isAlive()) return;
        if (e instanceof PlayerEntity p) {
            if (!players.get()) return;
            if (ignoreFriends.get() && FriendManager.isFriend(p.getGameProfile().getName())) return;
        } else if (!mobs.get()) return;
        if (mc.player.getAttackCooldownProgress(0.5f) < cooldown.getFloat()) return;
        mc.interactionManager.attackEntity(mc.player, e);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
