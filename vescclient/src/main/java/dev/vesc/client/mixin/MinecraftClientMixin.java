package dev.vesc.client.mixin;

import dev.vesc.client.VescClient;
import dev.vesc.client.ui.MainMenu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Podmienia menu główne Minecrafta na menu VescClient (można wyłączyć w ustawieniach). */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen vesc$mainMenu(Screen screen) {
        VescClient rc = VescClient.get();
        if (screen instanceof TitleScreen && rc != null && rc.settings.customMenu.get()) return new MainMenu();
        return screen;
    }
}
