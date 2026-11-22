package dev.with.client.mixin;

import dev.with.client.ui.Loading;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ekrany wczytywania Minecrafta (łączenie, teren, przygotowywanie świata, zapisywanie) rysujemy po swojemu. */
@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "renderWithTooltip", at = @At("HEAD"), cancellable = true)
    private void withclient$loading(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen s = (Screen) (Object) this;
        if (Loading.handles(s)) {
            Loading.render(ctx, s, mouseX, mouseY);
            ci.cancel();
        }
    }
}
