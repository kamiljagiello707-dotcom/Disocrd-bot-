package dev.razer.client.mixin;

import dev.razer.client.render.SmoothTextLayer;
import net.minecraft.client.font.TextRenderLayerSet;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Czcionki RazerClient dostają wygładzone (liniowe) warstwy tekstu. */
@Mixin(TextRenderLayerSet.class)
public class TextRenderLayerSetMixin {
    @Inject(method = "ofIntensity", at = @At("HEAD"), cancellable = true)
    private static void razer$smooth(Identifier id, CallbackInfoReturnable<TextRenderLayerSet> cir) {
        if (id.getNamespace().equals("razerclient")) cir.setReturnValue(SmoothTextLayer.of(id));
    }
}
