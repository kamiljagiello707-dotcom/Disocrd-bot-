package pl.mystic.client.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.mystic.client.MysticClient;
import pl.mystic.client.module.modules.Zoom;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void mystic$zoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (MysticClient.modules == null) return;
        Zoom zoom = MysticClient.modules.get(Zoom.class);
        if (zoom != null && zoom.isEnabled()) cir.setReturnValue(cir.getReturnValue() / Zoom.FACTOR);
    }
}
