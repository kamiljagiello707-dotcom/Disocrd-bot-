package dev.vesca.client.mixin;

import dev.vesca.client.ui.ServersScreen;
import dev.vesca.client.ui.VescaMenu;
import dev.vesca.client.ui.WorldsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Podmienia ekrany Minecrafta na ekrany VescaClient: menu główne, listę serwerów i listę światów
 * (także gdy gra sama do nich wraca, np. po rozłączeniu z serwerem).
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen vesca$screens(Screen screen) {
        if (screen instanceof TitleScreen) return new VescaMenu();
        if (screen instanceof MultiplayerScreen) return new ServersScreen(new VescaMenu());
        if (screen instanceof SelectWorldScreen) return new WorldsScreen(new VescaMenu());
        return screen;
    }
}
