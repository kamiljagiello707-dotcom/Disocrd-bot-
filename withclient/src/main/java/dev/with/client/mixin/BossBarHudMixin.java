package dev.with.client.mixin;

import dev.with.client.visual.V;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NoRender: paski bossów. */
@Mixin(BossBarHud.class)
public class BossBarHudMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void withclient$bossbar(DrawContext ctx, CallbackInfo ci) {
        if (V.hide("NoRender", "Bossbar")) ci.cancel();
    }
}
