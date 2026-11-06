package pl.mystic.client.module.modules;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import pl.mystic.client.module.Category;
import pl.mystic.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class NoFall extends Module {
    public NoFall() { super("NoFall", "Brak obrażeń od upadku", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p.fallDistance > 2.5f && !p.isFallFlying() && p.networkHandler != null) {
            p.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
        }
    }
}
