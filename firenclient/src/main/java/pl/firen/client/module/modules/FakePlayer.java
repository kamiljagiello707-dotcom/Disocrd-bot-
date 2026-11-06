package pl.firen.client.module.modules;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import pl.firen.client.FirenClient;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.BooleanSetting;

import java.util.UUID;

/** Gracz-manekin widoczny tylko u Ciebie – do testowania KillAury, AutoTrapa i ESP. */
public class FakePlayer extends Module {
    public final BooleanSetting copyInventory = add(new BooleanSetting("Copy armor", true));
    private OtherClientPlayerEntity fake;

    public FakePlayer() { super("FakePlayer", "Manekin gracza do testów (widoczny tylko u Ciebie).", Category.MISC); }

    public static boolean isFake(Entity e) {
        var m = FirenClient.modules();
        if (m == null) return false;
        FakePlayer fp = m.get(FakePlayer.class);
        return fp.fake != null && fp.fake == e;
    }

    /** Symulowany cios w manekina (bo serwer go nie zna). */
    public static void simulateHit(Entity e) {
        if (!(e instanceof OtherClientPlayerEntity p)) return;
        var me = mc.player;
        float base = 1f + (float) KillAura.damageOf(me.getMainHandStack()); // klient nie zna atrybutu z broni
        float dmg = base * Math.max(0.2f, me.getAttackCooldownProgress(0.5f));
        boolean crit = me.fallDistance > 0 && !me.isOnGround();
        if (crit) dmg *= 1.5f;
        float hp = p.getHealth() - dmg;
        p.setHealth(hp <= 0 ? 20f : hp);
        p.hurtTime = 10;
        p.maxHurtTime = 10;
    }

    @Override
    public void onEnable() {
        if (mc.world == null || mc.player == null) { setEnabledSilently(false); return; }
        fake = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.fromString("66666666-6666-4666-8666-666666666666"), "FirenBot"));
        fake.copyPositionAndRotation(mc.player);
        fake.setHeadYaw(mc.player.getHeadYaw());
        fake.setBodyYaw(mc.player.getBodyYaw());
        if (copyInventory.get()) fake.getInventory().clone(mc.player.getInventory());
        fake.setHealth(20f);
        mc.world.addEntity(fake);
    }

    @Override
    public void onDisable() {
        if (fake != null && mc.world != null) mc.world.removeEntity(fake.getId(), Entity.RemovalReason.DISCARDED);
        fake = null;
    }

    @Override
    public void onTick() {
        if (fake == null || fake.isRemoved()) setEnabled(false);
    }
}
