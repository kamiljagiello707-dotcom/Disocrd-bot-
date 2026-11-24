package dev.with.client.mixin;

import dev.with.client.visual.Sky;
import dev.with.client.visual.View;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Weather (rodzaj opadów), CustomSky (własne niebo, opcjonalnie bez chmur). */
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Redirect(method = {"renderWeather", "tickRainSplashing"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/Biome;getPrecipitation(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/biome/Biome$Precipitation;"))
    private Biome.Precipitation withclient$precipitation(Biome biome, BlockPos pos) {
        return View.precipitation(biome.getPrecipitation(pos));
    }

    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void withclient$sky(Matrix4f view, Matrix4f projection, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo ci) {
        if (Sky.render(view, tickDelta, camera, thickFog)) ci.cancel();
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void withclient$clouds(MatrixStack ms, Matrix4f view, Matrix4f projection, float tickDelta, double x, double y, double z, CallbackInfo ci) {
        if (Sky.hideClouds()) ci.cancel();
    }
}
