package dev.with.client.mixin;

import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** dostęp do rozmycia tła ekranu (tło menu WithClient) */
@Mixin(Screen.class)
public interface ScreenAccessor {
    @Invoker("applyBlur") void withclient$applyBlur(float delta);
}
