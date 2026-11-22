package dev.with.client.mixin;

import dev.with.client.hud.Hud;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Przy otwartym czacie można klikać w HUD (np. przyciski Spotify). */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void withclient$hudClick(double x, double y, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == 0 && Hud.click(x, y)) cir.setReturnValue(true);
    }
}
