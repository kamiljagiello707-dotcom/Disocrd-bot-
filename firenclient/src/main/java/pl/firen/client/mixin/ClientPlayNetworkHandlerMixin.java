package pl.firen.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.firen.client.module.modules.Velocity;

/** Velocity: skaluje zmianę prędkości gracza z odrzutu (cios, eksplozja). */
@Mixin(ClientPlayNetworkHandler.class)
abstract class ClientPlayNetworkHandlerMixin {
    @Unique private Vec3d firen$before;

    @Inject(method = "onEntityVelocityUpdate", at = @At("HEAD"))
    private void firen$velHead(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        firen$before = mc.isOnThread() && mc.player != null && packet.getEntityId() == mc.player.getId()
                ? mc.player.getVelocity() : null;
    }

    @Inject(method = "onEntityVelocityUpdate", at = @At("TAIL"))
    private void firen$velTail(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        if (firen$before != null) Velocity.adjust(firen$before);
        firen$before = null;
    }

    @Inject(method = "onExplosion", at = @At("HEAD"))
    private void firen$expHead(ExplosionS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        firen$before = mc.isOnThread() && mc.player != null ? mc.player.getVelocity() : null;
    }

    @Inject(method = "onExplosion", at = @At("TAIL"))
    private void firen$expTail(ExplosionS2CPacket packet, CallbackInfo ci) {
        if (firen$before != null) Velocity.adjust(firen$before);
        firen$before = null;
    }
}
