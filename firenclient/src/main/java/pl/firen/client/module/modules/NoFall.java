package pl.firen.client.module.modules;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;

public class NoFall extends Module {
    public NoFall() { super("NoFall", "Brak obrażeń od upadku.", Category.MOVEMENT); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p.fallDistance > 2.5f && !p.isGliding() && !p.isCreative()) {
            p.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, p.horizontalCollision));
            p.fallDistance = 0;
        }
    }
}
