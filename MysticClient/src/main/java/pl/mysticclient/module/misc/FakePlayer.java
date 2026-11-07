package pl.mysticclient.module.misc;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import pl.mysticclient.module.Category;
import pl.mysticclient.module.Module;

import java.util.UUID;

public class FakePlayer extends Module {
    private static final int ID = -2137;
    private OtherClientPlayerEntity fake;

    public FakePlayer() {
        super("FakePlayer", "Tworzy kopię Twojej postaci (tylko u Ciebie) – do testów.", Category.MISC);
    }

    @Override
    public void onEnable() {
        if (nullCheck()) { setEnabled(false, false); return; }
        fake = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.randomUUID(), "MysticBot"));
        fake.copyPositionAndRotation(mc.player);
        fake.setHeadYaw(mc.player.getHeadYaw());
        fake.setBodyYaw(mc.player.getBodyYaw());
        fake.getInventory().clone(mc.player.getInventory());
        fake.setId(ID);
        mc.world.addEntity(fake);
    }

    @Override
    public void onDisable() {
        if (fake != null && mc.world != null) mc.world.removeEntity(ID, Entity.RemovalReason.DISCARDED);
        fake = null;
    }

    @Override
    public void onTick() {
        if (mc.world == null && fake != null) setEnabled(false, false);
    }
}
