package pl.firen.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pl.firen.client.rotation.RotationManager;

/** Move fix dla skoku ze sprintem (boost liczony z rotacji serwera). */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Redirect(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    private float firen$jumpYaw(LivingEntity self) {
        if (self == MinecraftClient.getInstance().player && RotationManager.moveFix()) return RotationManager.yaw();
        return self.getYaw();
    }
}
