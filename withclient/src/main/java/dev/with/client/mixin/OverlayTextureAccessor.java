package dev.with.client.mixin;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** HitColor: dostęp do tekstury czerwonego błysku przy obrażeniach. */
@Mixin(OverlayTexture.class)
public interface OverlayTextureAccessor {
    @Accessor("texture")
    NativeImageBackedTexture withclient$texture();
}
