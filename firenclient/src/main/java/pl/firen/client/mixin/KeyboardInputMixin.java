package pl.firen.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.firen.client.rotation.RotationManager;

/**
 * Silent move fix: dobiera klawisze ruchu tak, by względem rotacji serwera gracz szedł
 * tam, gdzie patrzy kamera. Ruch pozostaje zgodny z tym, co widzi serwer.
 */
@Mixin(KeyboardInput.class)
abstract class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void firen$remap(CallbackInfo ci) {
        var player = MinecraftClient.getInstance().player;
        if (player == null || !RotationManager.moveFix()) return;
        KeyboardInput self = (KeyboardInput) (Object) this;
        float fwd = self.movementForward, side = self.movementSideways;
        if (Math.abs(fwd) < 1e-4f && Math.abs(side) < 1e-4f) return;
        float mag = Math.max(Math.abs(fwd), Math.abs(side));

        double cam = Math.toRadians(player.getYaw());
        double wx = side * Math.cos(cam) - fwd * Math.sin(cam);
        double wz = fwd * Math.cos(cam) + side * Math.sin(cam);

        double srv = Math.toRadians(RotationManager.yaw());
        float bestF = 0, bestS = 0;
        double best = -2;
        for (int f = -1; f <= 1; f++) {
            for (int s = -1; s <= 1; s++) {
                if (f == 0 && s == 0) continue;
                double cx = s * Math.cos(srv) - f * Math.sin(srv);
                double cz = f * Math.cos(srv) + s * Math.sin(srv);
                double dot = (wx * cx + wz * cz) / (Math.hypot(wx, wz) * Math.hypot(cx, cz));
                if (dot > best) { best = dot; bestF = f; bestS = s; }
            }
        }
        self.movementForward = MathHelper.clamp(bestF * mag, -1f, 1f);
        self.movementSideways = MathHelper.clamp(bestS * mag, -1f, 1f);
    }
}
