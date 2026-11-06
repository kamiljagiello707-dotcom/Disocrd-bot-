package pl.firen.client.mixin;

import net.minecraft.client.render.RenderTickCounter;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.firen.client.module.modules.Timer;

/** Timer: przyspiesza/zwalnia liczbę ticków gry. */
@Mixin(RenderTickCounter.Dynamic.class)
abstract class RenderTickCounterMixin {
    @Shadow private float lastFrameDuration;

    @Inject(method = "beginRenderTick(J)I", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/render/RenderTickCounter$Dynamic;lastFrameDuration:F",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER, ordinal = 0))
    private void firen$timer(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        lastFrameDuration *= Timer.multiplier();
    }
}
