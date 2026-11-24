package dev.with.client.mixin;

import dev.with.client.visual.Tints;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** NameColor: kolor/gradient Twojego nicku na liście graczy (TAB). */
@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void withclient$name(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        Text t = Tints.name(entry, cir.getReturnValue());
        if (t != null) cir.setReturnValue(t);
    }
}
