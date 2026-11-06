package pl.mystic.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.mystic.client.MysticClient;
import pl.mystic.client.module.modules.Fullbright;

@Mixin(SimpleOption.class)
public class SimpleOptionMixin<T> {
    @SuppressWarnings("unchecked")
    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    private void mystic$fullbright(CallbackInfoReturnable<T> cir) {
        if (MysticClient.modules == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null || (Object) this != mc.options.getGamma()) return;
        Fullbright fb = MysticClient.modules.get(Fullbright.class);
        if (fb != null && fb.isEnabled()) cir.setReturnValue((T) (Object) 16.0);
    }
}
