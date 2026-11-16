package dev.vesc.client.mixin;

import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** dostęp do vanilla applySwingOffset (gdy SwordAnimations nie działa dla danego przedmiotu) */
@Mixin(HeldItemRenderer.class)
public interface HeldItemRendererInvoker {
    @Invoker("applySwingOffset")
    void vesc$applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress);
}
