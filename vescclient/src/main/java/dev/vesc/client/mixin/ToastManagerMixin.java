package dev.vesc.client.mixin;

import dev.vesc.client.hud.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Toasty Minecrafta (osiągnięcia, przepisy) przesuwane pod elementy HUD w prawym górnym rogu (Watermark itd.). */
@Mixin(targets = "net.minecraft.client.toast.ToastManager$Entry")
public class ToastManagerMixin {
    @ModifyArg(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"), index = 1)
    private float vesc$offset(float y) { return y + Hud.toastOffset; }
}
