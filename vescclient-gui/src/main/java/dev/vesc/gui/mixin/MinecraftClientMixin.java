package dev.vesc.gui.mixin;

import dev.vesc.gui.ui.VescMenu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Podmienia menu główne Minecrafta na menu VescClient GUI. */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen vescgui$mainMenu(Screen screen) {
        if (screen instanceof TitleScreen) return new VescMenu();
        return screen;
    }
}
