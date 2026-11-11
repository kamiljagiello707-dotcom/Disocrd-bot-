package tech.fuzir.client.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.fuzir.client.FuzirClient;
import tech.fuzir.client.event.events.KeyEvent;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void fuzir$onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (window != client.getWindow().getHandle()) return;
        KeyEvent event = FuzirClient.get().getEventBus().post(new KeyEvent(key, scancode, action, modifiers));
        if (event.isCancelled()) ci.cancel();
    }
}
