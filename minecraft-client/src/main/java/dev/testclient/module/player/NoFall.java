package dev.testclient.module.player;

import dev.testclient.module.Category;
import dev.testclient.module.Module;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class NoFall extends Module {
    public NoFall() {
        super("NoFall", "Brak obrażeń od upadku", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc.player.fallDistance > 2.5f && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
            mc.player.fallDistance = 0;
        }
    }
}
