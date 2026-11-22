package dev.with.client.mixin;

import dev.with.client.render.SmoothTextLayer;
import net.minecraft.client.font.TextRenderLayerSet;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Czcionki WithClient dostają wygładzone (liniowe) warstwy tekstu. */
@Mixin(TextRenderLayerSet.class)
public class TextRenderLayerSetMixin {
    @Inject(method = "ofIntensity", at = @At("HEAD"), cancellable = true)
    private static void vesc$smooth(Identifier id, CallbackInfoReturnable<TextRenderLayerSet> cir) {
        if (id.getNamespace().equals("withclient")) cir.setReturnValue(SmoothTextLayer.of(id));
    }
}
