package pl.firen.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.firen.client.render.OutlineManager;
import pl.firen.client.rotation.RotationManager;

/** Move fix: ruch liczony względem rotacji wysyłanej do serwera. */
@Mixin(Entity.class)
abstract class EntityMixin {
    @ModifyArg(method = "updateVelocity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"),
            index = 2)
    private float firen$moveFixYaw(float yaw) {
        if ((Object) this == MinecraftClient.getInstance().player && RotationManager.moveFix()) return RotationManager.yaw();
        return yaw;
    }

    /** Kolor poświaty (Glow ESP / Target ESP). */
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void firen$glowColor(CallbackInfoReturnable<Integer> cir) {
        int id = ((Entity) (Object) this).getId();
        if (OutlineManager.has(id)) cir.setReturnValue(OutlineManager.color(id));
    }
}
