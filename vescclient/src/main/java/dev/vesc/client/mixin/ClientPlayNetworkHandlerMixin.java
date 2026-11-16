package dev.vesc.client.mixin;

import dev.vesc.client.hud.Tps;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Odczyt czasu świata do licznika TPS w Watermarku (tylko odczyt). */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onWorldTimeUpdate", at = @At("HEAD"))
    private void vesc$tps(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
        if (MinecraftClient.getInstance().isOnThread()) Tps.onTimeUpdate(packet.getTime());
    }
}
