package dev.vesca.client.mixin;

import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.server.WorldGenerationProgressTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelLoadingScreen.class)
public interface LevelLoadingScreenAccessor {
    @Accessor("progressProvider") WorldGenerationProgressTracker vesca$progress();
}
