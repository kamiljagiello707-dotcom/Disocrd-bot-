package dev.vesc.client.module.utility;

import dev.vesc.client.module.Category;
import dev.vesc.client.module.Module;

import com.mojang.authlib.GameProfile;
import dev.vesc.client.setting.BoolSetting;
import dev.vesc.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;

import java.util.UUID;

/** Klient-side manekin gracza do testowania TargetESP, nametagów, HUD-a itd. Serwer go nie widzi. */
public class FakePlayer extends Module {
    private static final int ID = -421337;
    private OtherClientPlayerEntity dummy;

    public FakePlayer() {
        super("FakePlayer", Category.UTILITY, "i_userx", "Kopia gracza do testów (tylko u Ciebie)", "Client-side test dummy");
        place(Category.UTILITY, "Tools");
        add(new BoolSetting("Copy Inventory", "Copy Inventory", true), new BoolSetting("Copy Skin", "Copy Skin", true),
                new NumberSetting("Health", "Health", 20, 1, 20, 1));
    }

    @Override protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) { setEnabledSilent(false); return; }
        boolean skin = bool("Copy Skin");
        net.minecraft.client.util.SkinTextures tex = mc.player.getSkinTextures();
        dummy = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.randomUUID(), "FakePlayer")) {
            @Override public net.minecraft.client.util.SkinTextures getSkinTextures() { return skin ? tex : super.getSkinTextures(); }
        };
        dummy.copyPositionAndRotation(mc.player);
        dummy.setHeadYaw(mc.player.getHeadYaw());
        dummy.setBodyYaw(mc.player.getBodyYaw());
        if (bool("Copy Inventory")) dummy.getInventory().clone(mc.player.getInventory());
        dummy.setHealth((float) num("Health"));
        dummy.setId(ID);
        mc.world.addEntity(dummy);
    }

    @Override protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null && dummy != null) mc.world.removeEntity(ID, Entity.RemovalReason.DISCARDED);
        dummy = null;
    }

    /** po zmianie świata manekin znika – wyłącz moduł */
    public void tick(MinecraftClient mc) {
        if (isEnabled() && (dummy == null || mc.world == null || dummy.getWorld() != mc.world)) { dummy = null; setEnabledSilent(false); }
    }
}
