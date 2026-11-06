package pl.firen.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import pl.firen.client.FirenClient;
import pl.firen.client.module.Category;
import pl.firen.client.module.Module;
import pl.firen.client.setting.ModeSetting;

public class Criticals extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Packet", "Packet", "Jump"));

    public Criticals() { super("Criticals", "Każdy cios jest ciosem krytycznym.", Category.COMBAT); }

    @Override public String getSuffix() { return mode.get(); }

    public static void beforeAttack(Entity target) {
        Criticals c = FirenClient.modules().get(Criticals.class);
        var p = MinecraftClient.getInstance().player;
        if (!c.isEnabled() || p == null || !(target instanceof LivingEntity)) return;
        if (!p.isOnGround() || p.isTouchingWater() || p.isInLava() || p.isClimbing() || p.hasVehicle()) return;
        if (c.mode.is("Packet")) {
            var net = p.networkHandler;
            boolean hc = p.horizontalCollision;
            net.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(p.getX(), p.getY() + 0.0625, p.getZ(), false, hc));
            net.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(p.getX(), p.getY(), p.getZ(), false, hc));
        }
    }

    @Override
    public void onTick() {
        // Jump: podskocz, gdy KillAura ma cel – cios zada się w trakcie opadania (Crit sync)
        if (!mode.is("Jump")) return;
        KillAura aura = FirenClient.modules().get(KillAura.class);
        if (aura.isEnabled() && aura.getTarget() != null && mc.player.isOnGround() && !mc.options.jumpKey.isPressed()) {
            mc.player.jump();
        }
    }
}
