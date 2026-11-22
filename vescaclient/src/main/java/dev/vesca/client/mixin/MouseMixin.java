package dev.vesca.client.mixin;

import dev.vesca.client.ui.Hud;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Liczy kliknięcia LPM/PPM dla elementu HUD "CPS". */
@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void vesca$click(long window, int button, int action, int mods, CallbackInfo ci) {
        if (action == 1) {
            if (button == 0) Hud.click(true);
            else if (button == 1) Hud.click(false);
        }
    }
}
