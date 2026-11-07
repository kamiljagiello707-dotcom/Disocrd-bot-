package pl.mysticclient.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.mysticclient.module.ModuleManager;
import pl.mysticclient.module.render.PotionHUD;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    /** Ukrywa waniliowe ikonki efektów w prawym górnym rogu, gdy działa PotionHUD. */
    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void mystic$hideEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        PotionHUD hud = ModuleManager.get(PotionHUD.class);
        if (hud != null && hud.isEnabled() && hud.hideVanilla.get()) ci.cancel();
    }
}
