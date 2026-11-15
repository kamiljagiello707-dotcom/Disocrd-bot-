package dev.razer.client.mixin;

import dev.razer.client.RazerClient;
import dev.razer.client.ui.MainMenu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Podmienia menu główne Minecrafta na menu RazerClient (można wyłączyć w ustawieniach). */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen razer$mainMenu(Screen screen) {
        RazerClient rc = RazerClient.get();
        if (screen instanceof TitleScreen && rc != null && rc.settings.customMenu.get()) return new MainMenu();
        return screen;
    }
}
