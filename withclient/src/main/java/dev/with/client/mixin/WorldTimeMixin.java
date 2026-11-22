package dev.with.client.mixin;

import dev.with.client.hud.Hud;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Czas świata z serwera (co sekundę) – z odstępów między pakietami liczymy TPS dla elementu HUD "TPS". */
@Mixin(ClientPlayNetworkHandler.class)
public class WorldTimeMixin {
    @Inject(method = "onWorldTimeUpdate", at = @At("TAIL"))
    private void withclient$time(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
        Hud.onWorldTime(packet.getTime());
    }
}
